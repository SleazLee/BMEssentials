package at.sleazlee.bmessentials.rankup;

import org.bukkit.entity.Player;

/**
 * Interface representing a requirement for rank-up.
 */
public interface Requirement {
    /**
     * Checks if the requirement is met for the given player.
     *
     * @param player The player to check.
     * @return True if met, false otherwise.
     */
    boolean isMet(Player player);

    /**
     * Provides a deny message if the requirement is not met.
     *
     * @return The deny message.
     */
    String getDenyMessage();
}
