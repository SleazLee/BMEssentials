package at.sleazlee.bmessentials.rankup;

import java.util.List;

/**
 * Represents a single rank with its requirements and messages.
 */
public class Rank {
    private final String name;
    private final String nextRank;
    private final List<RankUpManager.Requirement> requirements;
    private final String personalMessage;
    private final String broadcastMessage;
    private final String denyMessage;

    /**
     * Constructs a new Rank.
     *
     * @param name             The name/key of the rank.
     * @param nextRank         The next rank's name/key.
     * @param requirements     The list of requirements for this rank.
     * @param personalMessage  The personal message to send upon successful rank up.
     * @param broadcastMessage The broadcast message to send upon successful rank up.
     * @param denyMessage      The message to send if requirements are not met.
     */
    public Rank(String name, String nextRank, List<RankUpManager.Requirement> requirements, String personalMessage, String broadcastMessage, String denyMessage) {
        this.name = name;
        this.nextRank = nextRank;
        this.requirements = requirements;
        this.personalMessage = personalMessage;
        this.broadcastMessage = broadcastMessage;
        this.denyMessage = denyMessage;
    }

    public String getName() {
        return name;
    }

    public String getNextRank() {
        return nextRank;
    }

    public List<RankUpManager.Requirement> getRequirements() {
        return requirements;
    }

    public String getPersonalMessage() {
        return personalMessage;
    }

    public String getBroadcastMessage() {
        return broadcastMessage;
    }

    public String getDenyMessage() {
        return denyMessage;
    }
}
