package at.sleazlee.bmessentials.DonationSystem;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.bmefunctions.DatabaseManager;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;

public class DonationRankMethods implements Listener {

	private final BMEssentials plugin;

	public DonationRankMethods(BMEssentials plugin) {
		this.plugin = plugin;
	}

	private static Permission permission;

	public static boolean setupPermissions() {
		RegisteredServiceProvider<Permission> permissionProvider = Bukkit.getServicesManager().getRegistration(Permission.class);
		if (permissionProvider != null) {
			permission = permissionProvider.getProvider();
		}
		return permission != null;
	}

	/**
	 * Called when a player donates.
	 *
	 * @param plugin     The plugin instance.
	 * @param playerName The name of the target player.
	 * @param rankChain  The donation rank chain.
	 */
	public static void playerDonated(BMEssentials plugin, String playerName, String rankChain) {
		// Retrieve player's current rank via the database
		String uuid = Bukkit.getOfflinePlayer(playerName).getUniqueId().toString();
		String currentRank = DatabaseManager.getPlayerRank(uuid);

		// Determine the ranking from the current rank
		int ranking = SearchRank.searchRank(currentRank, "Ranking");

		// Build the new rank
		String newRank = "[" + ranking + "][" + rankChain + "]";
		String commandCreator = "lp user " + playerName + " parent set " + newRank;

		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		Bukkit.dispatchCommand(console, commandCreator);

		// Use the plugin logger instead of System.out.println
		plugin.getLogger().info("BMLands " + currentRank + " " + ranking + " " + newRank);
	}
}