package at.sleazlee.bmessentials.rankup;

import java.util.List;

/**
 * Represents a single rank with its requirements and associated messages.
 */
public class Rank {
    private final String name;
    private final String nextRank;
    private final double balance; // Added balance field
    private final List<Requirement> requirements;
    private final String personalMessage;
    private final String broadcastMessage;
    private final String denyMessage;

    /**
     * Constructs a new Rank.
     *
     * @param name             The name/key of the rank.
     * @param nextRank         The next rank's name/key.
     * @param balance          The economy balance requirement.
     * @param requirements     The list of requirements for this rank.
     * @param personalMessage  The personal message to send upon successful rank up.
     * @param broadcastMessage The broadcast message to send upon successful rank up.
     * @param denyMessage      The message to send if requirements are not met.
     */
    public Rank(String name, String nextRank, double balance, List<Requirement> requirements, String personalMessage, String broadcastMessage, String denyMessage) {
        this.name = name;
        this.nextRank = nextRank;
        this.balance = balance;
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
     * Gets the economy balance requirement for this rank.
     *
     * @return The required balance in the server's economy.
     */
    public double getBalance() {
        return balance;
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