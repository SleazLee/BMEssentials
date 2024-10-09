package at.sleazlee.bmessentials.rankup;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Requirement that checks if a player has the required economy balance.
 */
public class EconomyRequirement implements Requirement {
    private final double requiredBalance;
    private final Economy economy;

    /**
     * Constructs an EconomyRequirement.
     *
     * @param requiredBalance The required balance in dollars.
     * @param economy         The Economy instance from Vault.
     */
    public EconomyRequirement(double requiredBalance, Economy economy) {
        this.requiredBalance = requiredBalance;
        this.economy = economy;
    }

    @Override
    public boolean isMet(Player player) {
        return economy.has(player, requiredBalance);
    }

    @Override
    public String getDenyMessage() {
        return "&c&lRanks &cYou need at least &a$" + String.format("%.2f", requiredBalance) + "&c to rank up.";
    }
}
