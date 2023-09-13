package at.sleazlee.bmessentials.SpawnSystems;

import at.sleazlee.bmessentials.BMEssentials;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class DiamondCatch implements CommandExecutor {

	private final BMEssentials plugin;

	public DiamondCatch(BMEssentials plugin) {
		this.plugin = plugin;
	}


	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender instanceof ConsoleCommandSender) {
			if (args.length > 0) {
				String playerName = args[0];
				Player player = Bukkit.getPlayer(playerName);
				double randomValue = Math.random();
				double totalChance = plugin.getConfig().getDouble("systems.spawnsystems.diamondcatch.totalchance");

				if (player != null) {
					if (randomValue < totalChance) {
						diamondcatch(player, playerName);
					}
				} else {
					sender.sendMessage("§c§lBM §cPlayer not found or not online.");
				}
			} else {
				sender.sendMessage("§c§lBM §cInvalid Syntax! §7Try §b/diamondcatch <player>§7.");
			}
			return true;
		} else {
			// Inform the player that this command can only be executed by the console
			sender.sendMessage("§c§lBM §cThis command can only be executed by the console.");

			return true;
		}
	}

	public void diamondcatch(Player player, String playerName) {
		double randomValue = Math.random();
		double randomValueTwo = Math.random();
		int rndNumber = 0;
		double tenToFifty = plugin.getConfig().getDouble("systems.spawnsystems.diamondcatch.tentofifty");
		double threeToTen = (tenToFifty + plugin.getConfig().getDouble("systems.spawnsystems.diamondcatch.threetoten"));
		double oneToThree = (threeToTen + plugin.getConfig().getDouble("systems.spawnsystems.onetothree.onehundredfiftypercentchance"));

		if (randomValue <= tenToFifty) {
			rndNumber = (int) ((Math.random() * 40) + 10);
			if (randomValueTwo <= 0.50) {
				player.sendMessage("§b§lBM §6You hear a splash§f and see a diamond geyser of §6" + rndNumber + " §fdiamonds erupting from the Wishing-Well. §aYou're rich!!");
			} else {
				player.sendMessage("§b§lBM §dIncredible! §fThe Wishing-Well has revealed an avalanche of §6" + rndNumber + " §fshimmering §bdiamonds§f for you!");
			}
		} else if (randomValue <= threeToTen) {
			rndNumber = (int) ((Math.random() * 7) + 3);
			if (randomValueTwo <= 0.33) {
				player.sendMessage("§b§lBM §eOh, how lucky! §fThe Wishing-Well has gifted you §6" + rndNumber + " §fsparkling §bdiamonds§f.");
			} else if (randomValueTwo <= 0.66){
				player.sendMessage("§b§lBM §eA pleasant surprise §fawaits you as the Wishing-Well offers you §6" + rndNumber + " §fprecious §bdiamonds§f!");
			} else {
				player.sendMessage("§b§lBM §eWell done! §fThe Wishing-Well graces you with a cluster of §6" + rndNumber + " §eradiant diamonds.");
			}
		} else if (randomValue <= oneToThree) {
			rndNumber = (int) ((Math.random() * 2) + 1);
			if (randomValueTwo <= 0.50) {
				player.sendMessage("§b§lBM §aGreat! §fThe Wishing-Well has rewarded your patience with §6" + rndNumber + " §agleaming diamonds.");
			} else {
				player.sendMessage("§b§lBM §aA gift from the depths! §fThe Wishing-Well has bestowed upon you §6" + rndNumber + " §fbrilliant diamonds.");
			}
		} else {
			rndNumber = 1;
			player.sendMessage("§b§lBM §dWell, well, well! §fThe Wishing-Well has given you a shiny diamond!");
		}

		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		String CommandBuilder = "si give diamonds " + rndNumber + " " + playerName + " true";
		Bukkit.dispatchCommand(console, CommandBuilder);
	}
}

