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
	 * Broadcast an encrypted message to the Velocity proxy.
	 *
	 * @param playerName The player name to include in the payload.
	 */
	public static void sendPluginMessage(String playerName) {
		BMEssentials plugin = BMEssentials.getInstance();
		plugin.getLogger().info("Trying to send an encrypted Plugin Message");

		// 1) Serialize your payload
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(baos);
		try {
			out.writeUTF(playerName);
		} catch (IOException e) {
			plugin.getLogger().severe("Failed to serialize payload");
			return;
		}

		byte[] raw = baos.toByteArray();
		byte[] cipher;
		// 2) Encrypt it
		try {
			cipher = plugin.getAes().encrypt(raw);
		} catch (Exception ex) {
			plugin.getLogger().severe("Failed to encrypt autoban payload");
			return;
		}

		// 3) Send via any online player
		Collection<? extends Player> players = Bukkit.getOnlinePlayers();
		if (!players.isEmpty()) {
			Player sender = players.iterator().next();
			sender.sendPluginMessage(plugin, "bmessentials:autoban", cipher);
			plugin.getLogger().info("Sent encrypted Plugin Message");
		} else {
			plugin.getLogger().severe("No online players to relay plugin message!");
		}
	}
}
