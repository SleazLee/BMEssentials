package at.sleazlee.bmessentials.VTell;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.crypto.AESEncryptor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

public class VTellCommand implements CommandExecutor {

	private static BMEssentials plugin;
	private final MiniMessage miniMessage = MiniMessage.miniMessage();
	private final AESEncryptor aes;

	public VTellCommand(BMEssentials plugin) {
		VTellCommand.plugin = plugin;
		// grab the shared AESEncryptor instance
		this.aes = plugin.getAes();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		// Only console may run /vtell
		if (!(sender instanceof Player) && command.getName().equalsIgnoreCase("vtell")) {
			if (args.length == 0) {
				return false;
			}

			// Build the raw UTF‑8 message
			StringBuilder builder = new StringBuilder();
			for (String s : args) {
				builder.append(s).append(" ");
			}
			byte[] raw = builder.toString().trim().getBytes(StandardCharsets.UTF_8);

			// Encrypt it
			byte[] encrypted;
			try {
				encrypted = aes.encrypt(raw);
			} catch (Exception e) {
				plugin.getLogger().severe("Failed to encrypt VTell payload: " + e.getMessage());
				return true; // swallow so console isn’t spammed
			}

			// Send via any online player proxy‑bridge
			Collection<? extends Player> players = Bukkit.getOnlinePlayers();
			if (!players.isEmpty()) {
				players.iterator()
						.next()
						.sendPluginMessage(plugin, "bmessentials:vtell", encrypted);
			}
			return true;
		}

		sender.sendMessage(miniMessage.deserialize("<red>This command can only be run from the console.</red>"));
		return true;
	}

	/**
	 * Broadcast a message to the proxy (e.g. for auto‑announcements).
	 */
	public static void broadcastMessage(String message) {
		byte[] raw = message.getBytes(StandardCharsets.UTF_8);
		byte[] encrypted;
		try {
			encrypted = plugin.getAes().encrypt(raw);
		} catch (Exception e) {
			plugin.getLogger().severe("Failed to encrypt broadcast payload: " + e.getMessage());
			return;
		}

		Collection<? extends Player> players = Bukkit.getOnlinePlayers();
		if (!players.isEmpty()) {
			players.iterator()
					.next()
					.sendPluginMessage(plugin, "bmessentials:vtell", encrypted);
		}
	}
}
