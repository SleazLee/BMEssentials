package at.sleazlee.bmvelocity.PunishSystem;

import at.sleazlee.bmvelocity.BMVelocity;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import java.nio.charset.StandardCharsets;

public class MuteCommandSender {

	/**
	 * Sends a mute command to the Spigot backend, encrypting the payload with AES‑GCM.
	 */
	public static void sendMuteCommand(BMVelocity plugin, Player player, String muteCommandBuilder) {
		// 1) Turn the command into bytes
		byte[] raw = muteCommandBuilder.getBytes(StandardCharsets.UTF_8);

		// 2) Encrypt (prepends IV + GCM tag)
		byte[] encrypted;
		try {
			encrypted = plugin.getAes().encrypt(raw);
		} catch (Exception e) {
			plugin.getLogger().error("Failed to encrypt mute command, aborting send", e);
			return;
		}

		// 3) Send over our secured channel
		MinecraftChannelIdentifier channel = MinecraftChannelIdentifier.create("bmessentials", "mute");
		player.getCurrentServer()
				.map(ServerConnection::getServer)
				.ifPresent(conn -> conn.sendPluginMessage(channel, encrypted));
	}
}
