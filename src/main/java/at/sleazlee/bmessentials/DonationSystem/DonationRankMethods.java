package at.sleazlee.bmessentials.DonationSystem;

import at.sleazlee.bmessentials.BMEssentials;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class DonationRankMethods implements Listener {

	private final BMEssentials plugin;

	public DonationRankMethods(BMEssentials plugin) {
		this.plugin = plugin;
	}

	/**
	 * Called when a player donates.
	 *
	 * @param plugin     The plugin instance.
	 * @param playerName The name of the target player.
	 * @param rankChain  The donation rank chain.
	 */
	public static void playerDonated(BMEssentials plugin, String playerName, String rankChain) {
		// Get the online player
		Player player = Bukkit.getPlayerExact(playerName);
		if (player == null) {
			plugin.getLogger().warning("Player " + playerName + " is not online.");
			return;
		}

		// Retrieve LuckPerms instance
		LuckPerms luckPerms;
		try {
			luckPerms = LuckPermsProvider.get();
		} catch (IllegalStateException e) {
			plugin.getLogger().severe("LuckPerms not available: " + e.getMessage());
			return;
		}

		// Get LuckPerms user from the player
		User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
		if (user == null) {
			plugin.getLogger().warning("Could not retrieve LuckPerms user for " + playerName);
			player.sendMessage(ChatColor.RED + "Your rank could not be determined.");
			return;
		}

		// Retrieve player's current rank via LuckPerms (primary group)
		String currentRank = user.getPrimaryGroup();

		// Determine the ranking from the current rank using SearchRank (assumed to exist)
		int ranking = SearchRank.searchRank(currentRank, "Ranking");

		// Build the new rank based on the donation chain
		String newRank = "[" + ranking + "][" + rankChain + "]";

		// Check if the new rank exists in LuckPerms
		if (luckPerms.getGroupManager().getGroup(newRank) == null) {
			plugin.getLogger().warning("LuckPerms group does not exist: " + newRank);
			player.sendMessage(ChatColor.RED + "Cannot update your rank; group '" + newRank + "' does not exist.");
			return;
		}

		// Update LuckPerms: add the group node and set the new primary group
		InheritanceNode node = InheritanceNode.builder(newRank).build();
		user.data().add(node);
		user.setPrimaryGroup(newRank);
		luckPerms.getUserManager().saveUser(user);

		// Log the rank update
		plugin.getLogger().info("Updated donation rank for " + playerName + " from " + currentRank + " to " + newRank);
	}
}
