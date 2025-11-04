package at.sleazlee.bmvelocity.VoteSystem;

/**
 * Represents a pre-determined reward that should be executed on the Spigot server.
 */
public record QueuedReward(String type, String token, int vp, int streak, int lifetime, boolean streakIncremented) {
    public static final String TYPE_ALTAR = "ALTAR";

    public static QueuedReward altar(String token, int votePoints, int streak, int lifetimeVotes, boolean streakIncremented) {
        return new QueuedReward(TYPE_ALTAR, token, votePoints, streak, lifetimeVotes, streakIncremented);
    }
}
