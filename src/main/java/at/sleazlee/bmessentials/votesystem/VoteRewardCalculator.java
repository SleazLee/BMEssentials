package at.sleazlee.bmessentials.votesystem;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Encapsulates the balance logic for streak-aware vote rewards.
 * <p>
 * The calculator keeps the math in one place so both the reward distribution and the in-code
 * documentation stay consistent. A streak is considered preserved when the previous vote happened
 * within a grace window; otherwise it resets to day one. Token chances interpolate between the day-one
 * mix and the day-30 mix (55/30/15 shifting toward 35/40/25), while vote points guarantee at least one
 * point and slowly increase the odds for bonus points until day 30+ effectively guarantees two or more.
 * The calculator also increments a lifetime vote counter so PlaceholderAPI can expose the total number
 * of votes a player has ever cast. Beginning with this revision, streak progress only advances once a
 * player records four successful votes and at least 18 hours have elapsed since the previous streak
 * increment. This mirrors the four major listing sites the network encourages players to vote on while
 * still providing a safety buffer shorter than a full day. Extra votes beyond the daily four no longer
 * allow multiple streak jumps; they simply contribute to token/VotePoint rolls.
 * </p>
 */
public class VoteRewardCalculator {

    private static final Duration STREAK_GRACE = Duration.ofHours(36);
    private static final Duration STREAK_INCREMENT_INTERVAL = Duration.ofHours(18);
    public static final int VOTES_PER_INCREMENT = 4;
    private static final int STREAK_CAP = 30;

    /**
     * Holds the outcome of a reward calculation.
     */
    public record Reward(int effectiveStreak,
                         int previousStreak,
                         int currentStreak,
                         int bestStreak,
                         int votesTowardsNextIncrement,
                         boolean streakIncremented,
                         Instant lastVoteAt,
                         Instant lastStreakIncrementAt,
                         Token token,
                         int votePoints,
                         int totalVotes) {
    }

    /**
     * Represents the sanitized view of a player's streak progress without applying a new vote.
     */
    public record StreakSnapshot(int effectiveStreak,
                                 int currentStreak,
                                 int votesTowardsNextIncrement,
                                 int votesRequired,
                                 Duration timeUntilNextEligible) {
        public int votesRemaining() {
            return Math.max(0, votesRequired - votesTowardsNextIncrement);
        }
    }

    /**
     * Represents the interpolated odds for each token type at a specific streak value.
     */
    public record TokenOdds(double healing, double wishing, double obelisk) {
    }

    /**
     * The available altar token rewards.
     */
    public enum Token {
        HEALING("healingsprings"),
        WISHING("wishingwell"),
        OBELISK("obelisk");

        private final String keyCommandId;

        Token(String keyCommandId) {
            this.keyCommandId = keyCommandId;
        }

        public String getKeyCommandId() {
            return keyCommandId;
        }
    }

    /**
     * Compute the reward using the supplied streak data.
     *
     * @param currentStreak the stored current streak (0 when absent)
     * @param bestStreak    the stored best streak
     * @param lastVoteAt    the timestamp of the last vote (null if never)
     * @param lastStreakIncrementAt the timestamp of the last streak increment (null if never)
     * @param totalVotes    the stored lifetime vote counter
     * @param votesTowardsNextIncrement votes collected since the last streak increment (capped at 4)
     * @param now           the current time
     * @return a reward bundle containing the streak counters, token roll and vote points
     */
    public Reward calculate(int currentStreak,
                            int bestStreak,
                            Instant lastVoteAt,
                            Instant lastStreakIncrementAt,
                            int votesTowardsNextIncrement,
                            int totalVotes,
                            Instant now) {
        SanitizedState state = sanitizeState(currentStreak, bestStreak, lastVoteAt, lastStreakIncrementAt, votesTowardsNextIncrement, now);

        int progressBefore = state.progress();
        int progressAfter = Math.min(VOTES_PER_INCREMENT, progressBefore + 1);

        int updatedStreak = state.currentStreak();
        int previousStreak = state.currentStreak();
        Instant updatedIncrementInstant = state.lastIncrementAt();
        boolean streakIncremented = false;

        if (progressAfter >= VOTES_PER_INCREMENT) {
            boolean canIncrement = updatedIncrementInstant == null ||
                    Duration.between(updatedIncrementInstant, now).compareTo(STREAK_INCREMENT_INTERVAL) >= 0;
            if (canIncrement) {
                updatedStreak = Math.min(STREAK_CAP, updatedStreak + 1);
                updatedIncrementInstant = now;
                streakIncremented = true;
                progressAfter = progressBefore >= VOTES_PER_INCREMENT ? 1 : 0;
            } else {
                progressAfter = VOTES_PER_INCREMENT;
            }
        }

        int updatedBest = Math.max(state.bestStreak(), updatedStreak);
        int effectiveStreak = determineEffectiveStreak(updatedStreak);

        Token token = rollToken(effectiveStreak);
        int votePoints = rollVotePoints(effectiveStreak);
        int newTotalVotes = incrementTotalVotes(totalVotes);
        return new Reward(effectiveStreak,
                previousStreak,
                updatedStreak,
                updatedBest,
                progressAfter,
                streakIncremented,
                now,
                updatedIncrementInstant,
                token,
                votePoints,
                newTotalVotes);
    }

    /**
     * Produces a view of the current streak progress without applying an additional vote.
     */
    public StreakSnapshot preview(int currentStreak,
                                  int bestStreak,
                                  Instant lastVoteAt,
                                  Instant lastStreakIncrementAt,
                                  int votesTowardsNextIncrement,
                                  Instant now) {
        SanitizedState state = sanitizeState(currentStreak, bestStreak, lastVoteAt, lastStreakIncrementAt, votesTowardsNextIncrement, now);
        int effective = determineEffectiveStreak(state.currentStreak());
        Duration untilNext = computeCooldown(now, state.lastIncrementAt());
        return new StreakSnapshot(effective, state.currentStreak(), state.progress(), VOTES_PER_INCREMENT, untilNext);
    }

    /**
     * Returns the token odds for a given streak without rolling.
     */
    public TokenOdds describeOddsForStreak(int streak) {
        double progress = streakProgress(streak);
        double healingWeight = lerp(0.55, 0.35, progress);
        double wishingWeight = lerp(0.30, 0.40, progress);
        double obeliskWeight = lerp(0.15, 0.25, progress);
        return new TokenOdds(healingWeight, wishingWeight, obeliskWeight);
    }

    private static double streakProgress(int streak) {
        if (streak <= 0) {
            return 0;
        }
        int capped = Math.min(streak, STREAK_CAP);
        return (double) (capped - 1) / (STREAK_CAP - 1);
    }

    private static Token rollToken(int streak) {
        TokenOdds odds = describeOddsForStreak(streak);
        double roll = ThreadLocalRandom.current().nextDouble();
        if (roll < odds.healing) {
            return Token.HEALING;
        }
        roll -= odds.healing;
        if (roll < odds.wishing) {
            return Token.WISHING;
        }
        return Token.OBELISK;
    }

    private static double lerp(double start, double end, double t) {
        return start + (end - start) * t;
    }

    private static int rollVotePoints(int streak) {
        int base = streak >= STREAK_CAP ? 2 : 1;
        int total = base;

        double bonusOneChance = Math.min(1.0, Math.max(0.0, (streak - 1) / 29.0));
        if (ThreadLocalRandom.current().nextDouble() < bonusOneChance) {
            total += 1;
        }

        double bonusTwoChance = Math.max(0.0, (streak - 10) / 40.0);
        if (ThreadLocalRandom.current().nextDouble() < bonusTwoChance) {
            total += 2;
        }

        return total;
    }

    private static int incrementTotalVotes(int currentTotal) {
        if (currentTotal >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return currentTotal + 1;
    }

    private static SanitizedState sanitizeState(int currentStreak,
                                                int bestStreak,
                                                Instant lastVoteAt,
                                                Instant lastIncrementAt,
                                                int votesTowardsNextIncrement,
                                                Instant now) {
        int sanitizedCurrent = clamp(currentStreak, 0, STREAK_CAP);
        int sanitizedBest = clamp(bestStreak, 0, STREAK_CAP);
        int sanitizedProgress = clamp(votesTowardsNextIncrement, 0, VOTES_PER_INCREMENT);
        Instant sanitizedIncrementInstant = lastIncrementAt;

        if (lastVoteAt != null) {
            Duration sinceLastVote = Duration.between(lastVoteAt, now);
            if (sinceLastVote.compareTo(STREAK_GRACE) > 0) {
                sanitizedCurrent = 0;
                sanitizedProgress = 0;
                sanitizedIncrementInstant = null;
            }
        }

        return new SanitizedState(sanitizedCurrent, sanitizedBest, sanitizedProgress, sanitizedIncrementInstant);
    }

    private static int determineEffectiveStreak(int storedCurrent) {
        if (storedCurrent > 0) {
            return storedCurrent;
        }
        return 1;
    }

    private static Duration computeCooldown(Instant now, Instant lastIncrementAt) {
        if (lastIncrementAt == null) {
            return Duration.ZERO;
        }
        Duration elapsed = Duration.between(lastIncrementAt, now);
        Duration remaining = STREAK_INCREMENT_INTERVAL.minus(elapsed);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record SanitizedState(int currentStreak,
                                  int bestStreak,
                                  int progress,
                                  Instant lastIncrementAt) {
    }
}
