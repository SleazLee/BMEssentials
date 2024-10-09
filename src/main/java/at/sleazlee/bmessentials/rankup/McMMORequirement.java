package at.sleazlee.bmessentials.rankup;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Requirement that checks if a player has the required MCMMO power level.
 */
public class McMMORequirement implements Requirement {
    private final int requiredPowerLevel;

    /**
     * Constructs an McMMORequirement.
     *
     * @param requiredPowerLevel The required MCMMO power level.
     */
    public McMMORequirement(int requiredPowerLevel) {
        this.requiredPowerLevel = requiredPowerLevel;
    }

    @Override
    public boolean isMet(Player player) {
        String powerLevelStr = PlaceholderAPI.setPlaceholders(player, "%mcmmo_power_level%");
        try {
            int powerLevel = Integer.parseInt(powerLevelStr);
            return powerLevel >= requiredPowerLevel;
        } catch (NumberFormatException e) {
            player.getServer().getLogger().warning("Could not parse MCMMO power level for player " + player.getName() + ": " + powerLevelStr);
            return false;
        }
    }

    @Override
    public String getDenyMessage() {
        return "&c&lRanks &cYou need a MCMMO power level of &6" + requiredPowerLevel + "&c to rank up.";
    }
}
