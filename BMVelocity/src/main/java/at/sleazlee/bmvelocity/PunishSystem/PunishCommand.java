package at.sleazlee.bmvelocity.PunishSystem;

import at.sleazlee.bmvelocity.BMVelocity;
import at.sleazlee.bmvelocity.util.UUIDTools;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import at.sleazlee.bmvelocity.PunishSystem.MuteCommandSender;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class PunishCommand implements SimpleCommand {

	private final BMVelocity plugin;
	private final MiniMessage miniMessage = MiniMessage.miniMessage();

	// List of reasons supported.
	private static final List<String> REASONS = Arrays.asList(
			"spam", "language", "harassment", "toxicity", "advertising", "muteevasion",
			"greifing", "bullying", "scam", "hacking", "duplicating", "banevasion"
	);

	public PunishCommand(BMVelocity plugin) {
		this.plugin = plugin;
	}

	@Override
	public void execute(Invocation invocation) {
		CommandSource sender = invocation.source();
		String[] args = invocation.arguments();

		// Only players may use this command.
		if (!(sender instanceof Player)) {
			sender.sendMessage(miniMessage.deserialize("<red>Only players can use this command!</red>"));
			return;
		}
		Player player = (Player) sender;

		if (args.length < 2) {
			player.sendMessage(miniMessage.deserialize("<red>Usage: /punish <player> <reason></red>"));
			return;
		}

		String commandSender = player.getUsername();
		String targetPlayerName = args[0];
		String reason = args[1].toLowerCase();

		// Determine which offenses require ban or mute permissions.
		List<String> banPermissionsRequired = Arrays.asList("spam", "language", "harassment", "toxicity", "advertising", "muteevasion");
		List<String> mutePermissionsRequired = Arrays.asList("greifing", "bullying", "scam", "hacking", "duplicating", "banevasion");

		if (banPermissionsRequired.contains(reason)) {
			if (!player.hasPermission("bm.staff.ban")) {
				player.sendMessage(miniMessage.deserialize("<red>You don't have permission to issue this punishment.</red>"));
				return;
			}
			punishmentSelector(player, commandSender, targetPlayerName, reason);
		} else if (mutePermissionsRequired.contains(reason)) {
			if (!player.hasPermission("bm.staff.mute")) {
				player.sendMessage(miniMessage.deserialize("<red>You don't have permission to issue this punishment.</red>"));
				return;
			}
			punishmentSelector(player, commandSender, targetPlayerName, reason);
		} else {
			player.sendMessage(miniMessage.deserialize("<red>Invalid reason.</red>"));
		}
	}

	@Override
	public List<String> suggest(Invocation invocation) {
		List<String> suggestions = new ArrayList<>();
		String[] args = invocation.arguments();
		if (args.length == 1) {
			// Suggest online player names.
			plugin.getServer().getAllPlayers().forEach(p -> {
				if (p.getUsername().toLowerCase().startsWith(args[0].toLowerCase())) {
					suggestions.add(p.getUsername());
				}
			});
		} else if (args.length == 2) {
			for (String r : REASONS) {
				if (r.startsWith(args[1].toLowerCase())) {
					suggestions.add(r);
				}
			}
		}
		return suggestions;
	}

	// Helper: Determines which punishment method to call based on the reason.
	private void punishmentSelector(Player sender, String commandSender, String playerName, String reason) {
                UUID playerUUID = UUIDTools.getUUID(plugin, playerName);
		if (playerUUID == null) {
			sender.sendMessage(miniMessage.deserialize("<red>Could not determine UUID for " + playerName + ".</red>"));
			return;
		}
		String uuid = playerUUID.toString();
		try {
			plugin.getDatabaseManager().addPlayerToPunishments(uuid);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		switch (reason) {
			case "spam":
				spamMute(sender, commandSender, playerName, uuid);
				break;
			case "language":
				languageMute(sender, commandSender, playerName, uuid);
				break;
			case "harassment":
				harassmentMute(sender, commandSender, playerName, uuid);
				break;
			case "toxicity":
				toxicityMute(sender, commandSender, playerName, uuid);
				break;
			case "advertising":
				advertisingMute(sender, commandSender, playerName, uuid);
				break;
			case "muteevasion":
				evasionMute(sender, commandSender, playerName, uuid);
				break;
			case "greifing":
				greifingBan(sender, commandSender, playerName, uuid);
				break;
			case "bullying":
				bullyingBan(sender, commandSender, playerName, uuid);
				break;
			case "scam":
				scamBan(sender, commandSender, playerName, uuid);
				break;
			case "hacking":
				hackingBan(sender, commandSender, playerName, uuid);
				break;
			case "duplicating":
				duplicatingBan(sender, commandSender, playerName, uuid);
				break;
			case "banevasion":
				evasionBan(sender, commandSender, playerName, uuid);
				break;
			default:
				sender.sendMessage(miniMessage.deserialize("<red>Unknown reason.</red>"));
		}
	}

	// Helper: Send a formatted message to a target player.
	private void sendPunishMessage(String playerName, String message) {
		plugin.getServer().getPlayer(playerName).ifPresent(p -> p.sendMessage(miniMessage.deserialize(message)));
	}

        // Helper: For mute-type offenses, dispatch a plugin message to the target server.
        private void sendMuteCommand(Player player, String muteCommand) {
                MuteCommandSender.sendMuteCommand(plugin, player, muteCommand);
        }

	// === Offense-specific methods below ===

	// Spam Offense
	public void spamMute(Player sender, String commandSender, String playerName, String uuid) {
		plugin.getDatabaseManager().asyncGetString("punishments", "uuid", uuid, "spam", result -> {
			int spamValue = Integer.parseInt(result);
			spamValue++;
			String muteCommand;
			String time = "";
			if (spamValue == 1) {
                muteCommand = "";
                sendPunishMessage(playerName, "<gray>[<red>Warning</red>] <red>You have been flagged for spamming. Continued offenses will result in a mute.</red>");
				sender.sendMessage(miniMessage.deserialize("<red>The player " + playerName + " has been issued a warning for spamming.</red>"));
			} else if (spamValue == 2) {
				sendPunishMessage(playerName, "<red>You have been muted for <yellow>1 day</yellow> due to repeated spamming.</red>");
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been muted for 1 day due to spamming.</red>"));
				time = "1d";
				muteCommand = "mute:mute player " + playerName + " " + time;
			} else if (spamValue == 3) {
				sendPunishMessage(playerName, "<red>You have been muted for <yellow>2 days</yellow> due to continued spamming.</red>");
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been muted for 2 days due to spamming.</red>"));
				time = "2d";
				muteCommand = "mute:mute player " + playerName + " " + time;
			} else {
				sendPunishMessage(playerName, "<red>You have been muted for <yellow>3 days</yellow> due to multiple spamming offenses.</red>");
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been muted for 3 days due to spamming.</red>"));
				time = "3d";
				muteCommand = "mute:mute player " + playerName + " " + time;
			}
			if (!muteCommand.isEmpty()) {
				plugin.getServer().getPlayer(playerName).ifPresent(p -> sendMuteCommand(p, muteCommand));
			}
			plugin.getDatabaseManager().asyncSetString("punishments", "uuid", uuid, "spam", String.valueOf(spamValue));
		});
	}

	// Language Offense
	public void languageMute(Player sender, String commandSender, String playerName, String uuid) {
		plugin.getDatabaseManager().asyncGetString("punishments", "uuid", uuid, "language", result -> {
			int languageValue = Integer.parseInt(result);
			languageValue++;
			String muteCommand;
			String time = "";
			if (languageValue == 1) {
                muteCommand = "";
                sendPunishMessage(playerName, "<gray>[<red>Warning</red>] <red>Offensive or vulgar language is not tolerated. Continued offenses will result in a mute.</red>");
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been issued a warning for offensive language.</red>"));
			} else if (languageValue == 2) {
				sendPunishMessage(playerName, "<red>You have been muted for <yellow>1 day</yellow> due to repeated offensive language.</red>");
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been muted for 1 day for offensive language.</red>"));
				time = "1d";
				muteCommand = "mute:mute player " + playerName + " " + time;
			} else if (languageValue == 3) {
				sendPunishMessage(playerName, "<red>You have been muted for <yellow>2 days</yellow> due to continued offensive language.</red>");
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been muted for 2 days for offensive language.</red>"));
				time = "2d";
				muteCommand = "mute:mute player " + playerName + " " + time;
			} else {
				sendPunishMessage(playerName, "<red>You have been muted for <yellow>1 week</yellow> due to multiple offenses of offensive language.</red>");
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been muted for 1 week for offensive language.</red>"));
				time = "7d";
				muteCommand = "mute:mute player " + playerName + " " + time;
			}
			if (!muteCommand.isEmpty()) {
				plugin.getServer().getPlayer(playerName).ifPresent(p -> sendMuteCommand(p, muteCommand));
			}
			plugin.getDatabaseManager().asyncSetString("punishments", "uuid", uuid, "language", String.valueOf(languageValue));
		});
	}

	// Harassment Offense
	public void harassmentMute(Player sender, String commandSender, String playerName, String uuid) {
		plugin.getDatabaseManager().asyncGetString("punishments", "uuid", uuid, "harassment", result -> {
			int harassmentValue = Integer.parseInt(result);
			harassmentValue++;
			String muteCommand;
			String time = "";
			if (harassmentValue == 1) {
                muteCommand = "";
                sendPunishMessage(playerName, "<gray>[<red>Warning</red>] <red>Harassment is not tolerated. Continued offenses will result in a mute.</red>");
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been issued a warning for harassment.</red>"));
			} else if (harassmentValue == 2) {
				sendPunishMessage(playerName, "<red>You have been muted for <yellow>1 day</yellow> due to repeated harassment.</red>");
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been muted for 1 day for harassment.</red>"));
				time = "1d";
				muteCommand = "mute:mute player " + playerName + " " + time;
			} else if (harassmentValue == 3) {
				sendPunishMessage(playerName, "<red>You have been muted for <yellow>2 days</yellow> due to continued harassment.</red>");
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been muted for 2 days for harassment.</red>"));
				time = "2d";
				muteCommand = "mute:mute player " + playerName + " " + time;
			} else {
				sendPunishMessage(playerName, "<red>You have been muted for <yellow>1 week</yellow> due to multiple harassment offenses.</red>");
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been muted for 1 week for harassment.</red>"));
				time = "7d";
				muteCommand = "mute:mute player " + playerName + " " + time;
			}
			if (!muteCommand.isEmpty()) {
				plugin.getServer().getPlayer(playerName).ifPresent(p -> sendMuteCommand(p, muteCommand));
			}
			plugin.getDatabaseManager().asyncSetString("punishments", "uuid", uuid, "harassment", String.valueOf(harassmentValue));
		});
	}

	// Toxicity Offense
	public void toxicityMute(Player sender, String commandSender, String playerName, String uuid) {
		plugin.getDatabaseManager().asyncGetString("punishments", "uuid", uuid, "toxicity", result -> {
			int toxicityValue = Integer.parseInt(result);
			toxicityValue++;
			String muteCommand;
			String time = "";
			if (toxicityValue == 1) {
                muteCommand = "";
                sendPunishMessage(playerName, "<gray>[<red>Warning</red>] <red>Toxic behavior is not tolerated. Continued offenses will result in a mute.</red>");
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been issued a warning for toxicity.</red>"));
			} else if (toxicityValue == 2) {
				sendPunishMessage(playerName, "<red>You have been muted for <yellow>3 hours</yellow> due to repeated toxic behavior.</red>");
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been muted for 3 hours for toxicity.</red>"));
				time = "3h";
				muteCommand = "mute:mute player " + playerName + " " + time;
			} else if (toxicityValue == 3) {
				sendPunishMessage(playerName, "<red>You have been muted for <yellow>1 day</yellow> due to continued toxic behavior.</red>");
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been muted for 1 day for toxicity.</red>"));
				time = "1d";
				muteCommand = "mute:mute player " + playerName + " " + time;
			} else {
				sendPunishMessage(playerName, "<red>You have been muted for <yellow>1 week</yellow> due to multiple toxic offenses.</red>");
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been muted for 1 week for toxicity.</red>"));
				time = "7d";
				muteCommand = "mute:mute player " + playerName + " " + time;
			}
			if (!muteCommand.isEmpty()) {
				plugin.getServer().getPlayer(playerName).ifPresent(p -> sendMuteCommand(p, muteCommand));
			}
			plugin.getDatabaseManager().asyncSetString("punishments", "uuid", uuid, "toxicity", String.valueOf(toxicityValue));
		});
	}

	// Advertising Offense
	public void advertisingMute(Player sender, String commandSender, String playerName, String uuid) {
		plugin.getDatabaseManager().asyncGetString("punishments", "uuid", uuid, "advertising", result -> {
			int advertisingValue = Integer.parseInt(result);
			advertisingValue++;
			String muteCommand;
			String time = "";
			if (advertisingValue == 1) {
                muteCommand = "";
                sendPunishMessage(playerName, "<gray>[<red>Warning</red>] <red>Advertising is not allowed. Continued offenses will result in a mute.</red>");
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been issued a warning for advertising.</red>"));
			} else if (advertisingValue == 2) {
				sendPunishMessage(playerName, "<red>You have been muted for <yellow>3 days</yellow> due to repeated advertising.</red>");
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been muted for 3 days for advertising.</red>"));
				time = "3d";
				muteCommand = "mute:mute player " + playerName + " " + time;
			} else {
				sendPunishMessage(playerName, "<red>You have been muted for <yellow>1 week</yellow> due to multiple advertising offenses.</red>");
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been muted for 1 week for advertising.</red>"));
				time = "7d";
				muteCommand = "mute:mute player " + playerName + " " + time;
			}
			if (!muteCommand.isEmpty()) {
				plugin.getServer().getPlayer(playerName).ifPresent(p -> sendMuteCommand(p, muteCommand));
			}
			plugin.getDatabaseManager().asyncSetString("punishments", "uuid", uuid, "advertising", String.valueOf(advertisingValue));
		});
	}

	// Mute Evasion Offense
	public void evasionMute(Player sender, String commandSender, String playerName, String uuid) {
		// For mute evasion, we immediately unmute then schedule a new mute.
		String unmuteCommand = "mute:mute player " + playerName + " off";
		String time = "7d";
		String muteCommand = "mute:mute player " + playerName + " " + time;
		sender.sendMessage(miniMessage.deserialize("<red>Player " + playerName + " has been muted for 1 week due to mute evasion.</red>"));
		// Execute the unmute command immediately.
		plugin.getServer().getCommandManager().executeAsync(plugin.getServer().getConsoleCommandSource(), unmuteCommand);
		// Schedule the mute command after a short delay.
		plugin.getServer().getScheduler().buildTask(plugin, () -> {
			sendPunishMessage(playerName, "<red>You have been muted for 1 week due to attempting to evade a mute.</red>");
			plugin.getServer().getPlayer(playerName).ifPresent(p -> sendMuteCommand(p, muteCommand));
		}).delay(2, java.util.concurrent.TimeUnit.SECONDS).schedule();
	}

	// Greifing Offense (Banning)
	public void greifingBan(Player sender, String commandSender, String playerName, String uuid) {
		plugin.getDatabaseManager().asyncGetString("punishments", "uuid", uuid, "greifing", result -> {
			int greifingValue = Integer.parseInt(result);
			greifingValue++;
			String banCommand;
			String time;
			if (greifingValue == 1) {
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been banned for 3 days for griefing claimed areas.</red>"));
				time = "3d";
				banCommand = "tempbanip " + playerName + " " + time + " Greifing";
			} else if (greifingValue == 2) {
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been banned for 1 week for griefing claimed areas.</red>"));
				time = "7d";
				banCommand = "tempbanip " + playerName + " " + time + " Greifing";
			} else if (greifingValue == 3) {
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been banned for 2 weeks for griefing claimed areas.</red>"));
				time = "14d";
				banCommand = "tempbanip " + playerName + " " + time + " Greifing";
			} else {
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been banned for 1 month for griefing claimed areas.</red>"));
				time = "30d";
				banCommand = "tempbanip " + playerName + " " + time + " Greifing";
			}
			plugin.getServer().getCommandManager().executeAsync(plugin.getServer().getConsoleCommandSource(), banCommand);
			plugin.getDatabaseManager().asyncSetString("punishments", "uuid", uuid, "greifing", String.valueOf(greifingValue));
		});
	}

	// Bullying Offense (Banning)
	public void bullyingBan(Player sender, String commandSender, String playerName, String uuid) {
		plugin.getDatabaseManager().asyncGetString("punishments", "uuid", uuid, "bullying", result -> {
			int bullyingValue = Integer.parseInt(result);
			bullyingValue++;
			String banCommand = "";
			String time = "";
			if (bullyingValue == 1) {
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been issued a warning for harassing players/admins.</red>"));
				sendPunishMessage(playerName, "<gray>[<red>Warning</red>] <red>Harassment of players or admins is not tolerated. Continued offenses will result in a ban.</red>");
			} else if (bullyingValue == 2) {
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been banned for 3 days for harassing players/admins.</red>"));
				time = "3d";
				banCommand = "tempbanip " + playerName + " " + time + " Bullying";
			} else {
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been banned for 1 week for harassing players/admins.</red>"));
				time = "7d";
				banCommand = "tempbanip " + playerName + " " + time + " Bullying";
			}
			if (!banCommand.isEmpty()) {
				plugin.getServer().getCommandManager().executeAsync(plugin.getServer().getConsoleCommandSource(), banCommand);
			}
			plugin.getDatabaseManager().asyncSetString("punishments", "uuid", uuid, "bullying", String.valueOf(bullyingValue));
		});
	}

	// Scam Offense (Banning)
	public void scamBan(Player sender, String commandSender, String playerName, String uuid) {
		plugin.getDatabaseManager().asyncGetString("punishments", "uuid", uuid, "scam", result -> {
			int scamValue = Integer.parseInt(result);
			scamValue++;
			String banCommand = "";
			String time = "";
			if (scamValue == 1) {
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been issued a warning for scamming/blackmailing.</red>"));
				sendPunishMessage(playerName, "<gray>[<red>Warning</red>] <red>Scamming or blackmailing is not tolerated. Continued offenses will result in a ban.</red>");
			} else if (scamValue == 2) {
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been banned for 3 days for scamming/blackmailing.</red>"));
				time = "3d";
				banCommand = "tempbanip " + playerName + " " + time + " Scam";
			} else {
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been banned for 1 week for scamming/blackmailing.</red>"));
				time = "7d";
				banCommand = "tempbanip " + playerName + " " + time + " Scam";
			}
			if (!banCommand.isEmpty()) {
				plugin.getServer().getCommandManager().executeAsync(plugin.getServer().getConsoleCommandSource(), banCommand);
			}
			plugin.getDatabaseManager().asyncSetString("punishments", "uuid", uuid, "scam", String.valueOf(scamValue));
		});
	}

	// Hacking Offense (Banning)
	public void hackingBan(Player sender, String commandSender, String playerName, String uuid) {
		plugin.getDatabaseManager().asyncGetString("punishments", "uuid", uuid, "hacking", result -> {
			int hackingValue = Integer.parseInt(result);
			hackingValue++;
			String banCommand;
			String time;
			if (hackingValue == 1) {
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been banned for 1 week due to hacking.</red>"));
				time = "7d";
				banCommand = "tempbanip " + playerName + " " + time + " Hacking";
			} else if (hackingValue == 2) {
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been banned for 2 weeks due to hacking.</red>"));
				time = "14d";
				banCommand = "tempbanip " + playerName + " " + time + " Hacking";
			} else {
				sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been banned for 1 month due to hacking.</red>"));
				time = "30d";
				banCommand = "tempbanip " + playerName + " " + time + " Hacking";
			}
			plugin.getServer().getCommandManager().executeAsync(plugin.getServer().getConsoleCommandSource(), banCommand);
			plugin.getDatabaseManager().asyncSetString("punishments", "uuid", uuid, "hacking", String.valueOf(hackingValue));
		});
	}

	// Duplicating Items Offense (Banning)
	public void duplicatingBan(Player sender, String commandSender, String playerName, String uuid) {
		sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been banned for 1 month for duplicating items.</red>"));
		String time = "30d";
		String banCommand = "tempbanip " + playerName + " " + time + " Duplicating Items";
		plugin.getServer().getCommandManager().executeAsync(plugin.getServer().getConsoleCommandSource(), banCommand);
	}

	// Ban Evasion Offense (Banning)
	public void evasionBan(Player sender, String commandSender, String playerName, String uuid) {
		sender.sendMessage(miniMessage.deserialize("<red>" + playerName + " has been banned for 1 month due to ban evasion.</red>"));
		String time = "30d";
		String banCommand = "tempbanip " + playerName + " " + time + " Ban Evasion";
		plugin.getServer().getCommandManager().executeAsync(plugin.getServer().getConsoleCommandSource(), banCommand);
	}
}