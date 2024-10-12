package at.sleazlee.bmessentials.rankup;

import java.util.List;

/**
 * Represents a single rank with its requirements and associated messages.
 */
public class Rank {
    private final String name;
    private final String nextRank;
    private final double cost;
    private final List<Requirement> requirements;
    private final String personalMessage;
    private final String broadcastMessage;
    private final String denyMessage;

    /**
     * Constructs a new Rank.
     *
     * @param name             The name/key of the rank.
     * @param nextRank         The next rank's name/key.
     * @param cost             The cost to rank up.
     * @param requirements     The list of requirements for this rank.
     * @param personalMessage  The personal message to send upon successful rank up.
     * @param broadcastMessage The broadcast message to send upon successful rank up.
     * @param denyMessage      The message to send if requirements are not met.
     */
    public Rank(String name, String nextRank, double cost, List<Requirement> requirements, String personalMessage, String broadcastMessage, String denyMessage) {
        this.name = name;
        this.nextRank = nextRank;
        this.cost = cost;
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

    /**
     * Gets the cost to rank up.
     *
     * @return The cost in the server's economy.
     */
    public double getCost() {
        return cost;
    }

    public List<Requirement> getRequirements() {
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