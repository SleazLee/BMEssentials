package at.sleazlee.bmessentials.rankup;

import java.util.List;

/**
 * Represents a rank with its requirements and messages.
 */
public class Rank {
    private final String name;
    private final String nextRank;
    private final List<Requirement> requirements;
    private final String personalMessage;
    private final String broadcastMessage;
    private final String denyMessage;

    /**
     * Constructs a Rank.
     *
     * @param name            The rank's display name.
     * @param nextRank        The next rank in the progression (or null).
     * @param requirements    List of requirements to rank up.
     * @param personalMessage Message sent privately to the player.
     * @param broadcastMessage Message broadcasted to the server.
     * @param denyMessage     Message shown if requirements aren't met.
     */
    public Rank(String name, String nextRank, List<Requirement> requirements,
                String personalMessage, String broadcastMessage, String denyMessage) {
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