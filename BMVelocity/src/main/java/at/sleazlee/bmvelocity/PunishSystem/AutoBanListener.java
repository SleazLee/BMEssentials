package at.sleazlee.bmvelocity.PunishSystem;

import at.sleazlee.bmvelocity.BMVelocity;
import at.sleazlee.bmvelocity.crypto.AESEncryptor;
import at.sleazlee.bmvelocity.util.UUIDTools;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

public class AutoBanListener {

	private final BMVelocity plugin;
	private final AESEncryptor aes;
	private final MiniMessage miniMessage = MiniMessage.miniMessage();

	public AutoBanListener(BMVelocity plugin) {
		this.plugin = plugin;
		this.aes = plugin.getAes();
	}

	@Subscribe
	public void onPluginMessage(PluginMessageEvent event) {
                // 1) Only handle our autoban channel
                if (!"bmessentials:autoban".equals(event.getIdentifier().getId())) {
                        return;
                }

                // We've handled the message, don't let Velocity forward it any further.
                event.setResult(PluginMessageEvent.ForwardResult.handled());

		// 2) Never trust the client to originate these
		if (event.getSource() instanceof Player) {
			return;
		}

		// 3) Decrypt the incoming payload
		byte[] cipher = event.getData();
		byte[] plain;
		try {
			plain = aes.decrypt(cipher);
		} catch (Exception e) {
			plugin.getLogger().error("🔐 Failed to decrypt autoban message, dropping.", e);
			return;
		}

		// 4) Parse the decrypted data
		try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(plain))) {
			String playerName = in.readUTF();
                        UUID playerUUID = UUIDTools.getUUID(plugin, playerName);
			if (playerUUID == null) {
				return;
			}
			String uuid = playerUUID.toString();

			// 5) Add to punishments table
			try {
				plugin.getDatabaseManager().addPlayerToPunishments(uuid);
			} catch (SQLException ex) {
				plugin.getLogger().error("Error adding autoban record", ex);
			}

			// 6) Execute the ban
			autoHackingBan(playerName, uuid);

		} catch (IOException e) {
			plugin.getLogger().error("Error reading decrypted autoban payload", e);
		}
	}

	// Hacking Offense: Increase the hacking count and dispatch an appropriate ban command.
	private void autoHackingBan(String playerName, String uuid) {
		if (uuid != null) {
			plugin.getDatabaseManager().asyncGetString("punishments", "uuid", uuid, "hacking", result -> {
				int hackingValue = Integer.parseInt(result);
				hackingValue++;

				String banCommand;
				String time;
				if (hackingValue == 1) {
					time = "7d";
					banCommand = "tempbanip " + playerName + " " + time + " Hacking";
				} else if (hackingValue == 2) {
					time = "14d";
					banCommand = "tempbanip " + playerName + " " + time + " Hacking";
				} else {
					time = "30d";
					banCommand = "tempbanip " + playerName + " " + time + " Hacking";
				}
				// Execute the ban command as the console.
				plugin.getServer().getCommandManager().executeAsync(plugin.getServer().getConsoleCommandSource(), banCommand);

				// Update the hacking count in the database.
				plugin.getDatabaseManager().asyncSetString("punishments", "uuid", uuid, "hacking", String.valueOf(hackingValue));
			});
		}
	}
}
