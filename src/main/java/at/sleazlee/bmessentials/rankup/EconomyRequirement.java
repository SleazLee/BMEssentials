package at.sleazlee.bmessentials.rankup;

import net.milkbowl.vault2.economy.Economy; // <-- VaultUnlocked
import org.bukkit.entity.Player;

import java.math.BigDecimal;

/**
 * Requirement that checks if a player has the required economy balance using VaultUnlocked.
 */
public class EconomyRequirement implements Requirement {
    private final double requiredBalance;
    private final Economy economy;         // VaultUnlocked economy
    private final String pluginName;       // e.g. "BMEssentials"

    /**
     * Constructs an EconomyRequirement.
     *
     * @param pluginName      The name of your plugin, used in VaultUnlocked calls.
     * @param requiredBalance The required balance in dollars.
     * @param economy         The VaultUnlocked Economy instance.
     */
    public EconomyRequirement(String pluginName, double requiredBalance, Economy economy) {
        this.pluginName = pluginName;
        this.requiredBalance = requiredBalance;
        this.economy = economy;
    }

    @Override
    public boolean isMet(Player player) {
        // Using new economy.has(pluginName, UUID, BigDecimal)
        return economy.has(pluginName, player.getUniqueId(), BigDecimal.valueOf(requiredBalance));
        // Or if you want to force “Dollars” currency:
        // return economy.has(pluginName, player.getUniqueId(), "world", "Dollars", BigDecimal.valueOf(requiredBalance));
    }

    @Override
    public String getDenyMessage() {
        return "&c&lRanks &cYou need at least &a$"
                + String.format("%.2f", requiredBalance)
                + "&c to rank up.";
    }
}