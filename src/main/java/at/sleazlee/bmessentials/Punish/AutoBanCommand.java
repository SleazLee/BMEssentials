package at.sleazlee.bmessentials.Punish;

import at.sleazlee.bmessentials.BMEssentials;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;

public class AutoBanCommand implements CommandExecutor {
	private static BMEssentials plugin;

	public AutoBanCommand(BMEssentials plugin) {
		AutoBanCommand.plugin = plugin;

	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (label.equalsIgnoreCase("autoban")) {
			if (!(sender instanceof Player)) {
				String playerName = args[0];
				sendPluginMessage(playerName);

			} else {
				sender.sendMessage("§c§lBM §cAccess Denied.");
			}
			return true;
		}
		return false;
	}

	/**
	 * Broadcast a message to the BungeeCord server.
	 *
	 * @param playerName The Player that will be banned.
	 */
	public static void sendPluginMessage(String playerName) {
		// Debug message
		plugin.getLogger().info("Trying to send a Plugin Message");

		// Create a byte stream to hold the message
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(b);

		try {
			out.writeUTF(playerName);

		} catch (IOException e) {
			e.printStackTrace();
		}

		// Send the plugin message to BungeeCord from an arbitrary player
		Collection<? extends Player> players = Bukkit.getOnlinePlayers();
		if (!players.isEmpty()) {
			players.iterator().next().sendPluginMessage(plugin, "bmessentials:autoban", b.toByteArray());
			// Debug message
			plugin.getLogger().info("Sent a Plugin Message");
		}
	}
}
