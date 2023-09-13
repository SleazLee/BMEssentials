package at.sleazlee.bmessentials.bungeetell;

import at.sleazlee.bmessentials.BMEssentials;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;

public class BungeeTellCommand implements CommandExecutor {
	private static BMEssentials plugin;

	public BungeeTellCommand(BMEssentials plugin) {
		BungeeTellCommand.plugin = plugin;

	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equalsIgnoreCase("bungeetell")) {
			if (sender instanceof Player) {
				sender.sendMessage(ChatColor.RED + "This command can only be run from the console.");
				return true;
			}

			if (args.length == 0) {
				return false;
			}

			// Create the message from the command arguments
			StringBuilder builder = new StringBuilder();
			for (String s : args) {
				builder.append(s).append(" ");
			}

			// Create a byte stream to hold the message
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(b);

			try {
				out.writeUTF(builder.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}

			// Send the plugin message to BungeeCord from an arbitrary player
			Collection<? extends Player> players = Bukkit.getOnlinePlayers();
			if (!players.isEmpty()) {
				players.iterator().next().sendPluginMessage(plugin, "bmessentials:bungeetell", b.toByteArray());
			}

			return true;
		}

		return false;
	}

	/**
	 * Broadcast a message to the BungeeCord server.
	 *
	 * @param message The message to be broadcasted.
	 */
	public static void broadcastMessage(String message) {
		// Create a byte stream to hold the message
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(b);

		try {
			out.writeUTF(message);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Send the plugin message to BungeeCord from an arbitrary player
		Collection<? extends Player> players = Bukkit.getOnlinePlayers();
		if (!players.isEmpty()) {
			players.iterator().next().sendPluginMessage(plugin, "bungeetell:broadcast", b.toByteArray());
		}
	}

}