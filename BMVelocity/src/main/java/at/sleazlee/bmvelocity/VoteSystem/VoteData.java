package at.sleazlee.bmvelocity.VoteSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents the proxy-side vote data stored in the database.
 */
public class VoteData {
    private final UUID uuid;
    private int currentStreak;
    private long lastVote;
    private List<QueuedReward> pendingRewards;
    private int lifetimeVotes;

    public VoteData(UUID uuid, int currentStreak, long lastVote, List<QueuedReward> pendingRewards, int lifetimeVotes) {
        this.uuid = uuid;
        this.currentStreak = currentStreak;
        this.lastVote = lastVote;
        this.pendingRewards = pendingRewards == null ? new ArrayList<>() : new ArrayList<>(pendingRewards);
        this.lifetimeVotes = lifetimeVotes;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public long getLastVote() {
        return lastVote;
    }

    public void setLastVote(long lastVote) {
        this.lastVote = lastVote;
    }

    public List<QueuedReward> getPendingRewards() {
        return pendingRewards;
    }

    public void setPendingRewards(List<QueuedReward> pendingRewards) {
        this.pendingRewards = pendingRewards == null ? new ArrayList<>() : new ArrayList<>(pendingRewards);
    }

    public int getLifetimeVotes() {
        return lifetimeVotes;
    }

    public void setLifetimeVotes(int lifetimeVotes) {
        this.lifetimeVotes = lifetimeVotes;
    }
}
