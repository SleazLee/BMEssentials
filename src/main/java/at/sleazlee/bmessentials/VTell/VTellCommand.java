package at.sleazlee.bmessentials.VTell;

import at.sleazlee.bmessentials.BMEssentials;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;

public class VTellCommand implements CommandExecutor {

	private static BMEssentials plugin;
	private final MiniMessage miniMessage = MiniMessage.miniMessage();

	public VTellCommand(BMEssentials plugin) {
		VTellCommand.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		// This command is only to be run from the console.
		if (command.getName().equalsIgnoreCase("vtell")) {
			if (sender instanceof Player) {
				sender.sendMessage(miniMessage.deserialize("<red>This command can only be run from the console.</red>"));
				return true;
			}
			if (args.length == 0) {
				return false;
			}
			// Build the message from the command arguments.
			StringBuilder builder = new StringBuilder();
			for (String s : args) {
				builder.append(s).append(" ");
			}

			// Create a byte stream to hold the message.
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(b);

			try {
				out.writeUTF(builder.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}

			// Send the plugin message on channel "vtell" using an arbitrary online player.
			Collection<? extends Player> players = Bukkit.getOnlinePlayers();
			if (!players.isEmpty()) {
				players.iterator().next().sendPluginMessage(plugin, "bmessentials:vtell", b.toByteArray());
			}
			return true;
		}
		return false;
	}

	/**
	 * Broadcast a message to the proxy.
	 *
	 * @param message The message to broadcast.
	 */
	public static void broadcastMessage(String message) {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(b);

		try {
			out.writeUTF(message);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Collection<? extends Player> players = Bukkit.getOnlinePlayers();
		if (!players.isEmpty()) {
			players.iterator().next().sendPluginMessage(plugin, "bmessentials:vtell", b.toByteArray());
		}
	}
}