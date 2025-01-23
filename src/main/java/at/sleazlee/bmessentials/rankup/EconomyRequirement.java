package at.sleazlee.bmessentials.rankup;

import net.milkbowl.vault2.economy.Economy;
import org.bukkit.entity.Player;
import java.math.BigDecimal;

/**
 * Represents an economy requirement for ranking up, checking if a player
 * has enough of a specific currency (default: Dollars).
 */
public class EconomyRequirement implements Requirement {
    private final double requiredBalance;
    private final Economy economy;
    private final String pluginName;

    /**
     * Constructs a new EconomyRequirement.
     *
     * @param pluginName       The plugin name registered with VaultUnlocked.
     * @param requiredBalance  The amount of currency required to rank up.
     * @param economy          The VaultUnlocked economy instance.
     */
    public EconomyRequirement(String pluginName, double requiredBalance, Economy economy) {
        this.pluginName = pluginName;
        this.requiredBalance = requiredBalance;
        this.economy = economy;
    }

    /**
     * Checks if the player meets the economy requirement.
     *
     * @param player The player to check.
     * @return True if the player has sufficient funds, false otherwise.
     */
    @Override
    public boolean isMet(Player player) {
        return economy.has(pluginName, player.getUniqueId(), BigDecimal.valueOf(requiredBalance));
    }

    /**
     * Gets the deny message for insufficient funds.
     *
     * @return Formatted deny message.
     */
    @Override
    public String getDenyMessage() {
        return "&c&lRanks &cYou need at least &a$" +
                String.format("%.2f", requiredBalance) + "&c to rank up.";
    }

    /**
     * Retrieves the required balance for this requirement.
     *
     * @return The amount of currency needed.
     */
    public double getRequiredBalance() {
        return requiredBalance;
    }
}