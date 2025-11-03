package at.sleazlee.bmvelocity.VoteSystem;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Calculates streak-based altar token odds and vote point rewards.
 */
public class RewardCalculator {
    private static final double HEALING_START = 0.55;
    private static final double WISHING_START = 0.30;
    private static final double OBELISK_START = 0.15;

    private static final double HEALING_END = 0.35;
    private static final double WISHING_END = 0.40;
    private static final double OBELISK_END = 0.25;

    private static final int STREAK_CAP = 30;

    public QueuedReward createReward(int currentStreak, int lifetimeVotes, boolean streakIncremented) {
        String token = rollToken(currentStreak);
        int votePoints = rollVotePoints(currentStreak);
        return QueuedReward.altar(token, votePoints, currentStreak, lifetimeVotes, streakIncremented);
    }

    private String rollToken(int streak) {
        int clamped = Math.min(Math.max(streak, 1), STREAK_CAP);
        double progress = (clamped - 1) / 29.0; // 0 at day 1, 1 at day 30

        double healing = interpolate(HEALING_START, HEALING_END, progress);
        double wishing = interpolate(WISHING_START, WISHING_END, progress);
        double obelisk = interpolate(OBELISK_START, OBELISK_END, progress);

        double roll = ThreadLocalRandom.current().nextDouble();
        if (roll < healing) {
            return "HEALING";
        }
        if (roll < healing + wishing) {
            return "WISHING";
        }
        return "OBELISK";
    }

    private double interpolate(double start, double end, double progress) {
        return start + progress * (end - start);
    }

    private int rollVotePoints(int streak) {
        if (streak >= STREAK_CAP) {
            return 2;
        }
        int points = 1;
        if (streak > 1) {
            double chance = (streak - 1) / 29.0;
            if (ThreadLocalRandom.current().nextDouble() < chance) {
                points++;
            }
        }
        return points;
    }
}
