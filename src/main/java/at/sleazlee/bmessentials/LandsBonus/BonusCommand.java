package at.sleazlee.bmessentials.LandsBonus;

import at.sleazlee.bmessentials.BMEssentials;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BonusCommand implements CommandExecutor {

	private final BMEssentials plugin;

	public BonusCommand(BMEssentials plugin) {
		this.plugin = plugin;
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (label.equalsIgnoreCase("bmlands")) {
			if (!(sender instanceof Player)) {
				if (args.length >= 1) {
					String subCommand = args[0];

					if (subCommand.equalsIgnoreCase("namechange") && args.length == 3) {
						String playerName = args[1];
						String newTownName = args[2];
						BonusMethods.townNameChange(plugin, playerName, newTownName);
						return true;

					} else if (subCommand.equalsIgnoreCase("rankup") && args.length == 3) {
						String playerName = args[1];
						String newRank = args[2];
						BonusMethods.playerRankUp(plugin, playerName, newRank);
						return true;

					} else if (subCommand.equalsIgnoreCase("purchased") && args.length == 3) {
						String playerName = args[1];
						String rankChain = args[2];
						BonusMethods.playerDonated(plugin, playerName, rankChain);
						return true;

					} else if (subCommand.equalsIgnoreCase("kicked") && args.length == 3) {
						String playerName = args[1];
						String townName = args[2];
						BonusMethods.memberWasKicked(plugin, playerName, townName);
						return true;

					} else if (subCommand.equalsIgnoreCase("transfer") && args.length == 4) {
						String townName = args[1];
						String playerName = args[2];
						String newOwner = args[3];
						BonusMethods.transferOwnership(plugin, townName, playerName, newOwner);
						return true;

					} else {

						sender.sendMessage("§c§lBM §cInvalid command usage.");
						return true;

					}
				} else {
					sender.sendMessage("§c§lBM §cPlease provide a valid subcommand.");
					return true;
				}
			} else {
				sender.sendMessage("§c§lBM §cThis command can only be used by the console.");
				return true;
			}
		}
		return false;
	}

}
