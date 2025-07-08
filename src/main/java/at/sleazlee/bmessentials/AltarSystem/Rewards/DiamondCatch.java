package at.sleazlee.bmessentials.AltarSystem.Rewards;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class DiamondCatch {

	private final BMEssentials plugin;

	public DiamondCatch(BMEssentials plugin) {
		this.plugin = plugin;
	}

	/**
	 * Attempts to reward the given player with diamonds. The chance of receiving a reward is determined by
	 * the "TotalChance" value in the config.
	 *
	 * @param player The player to receive the diamond reward.
	 */
	public void diamondcatch(Player player) {
		// First, check if the random chance passes
		double chanceValue = Math.random();
		double totalChance = plugin.getConfig().getDouble("Systems.SpawnSystems.DiamondCatch.TotalChance");
		if (chanceValue >= totalChance) {
			// Chance failed; no reward is given.
			return;
		}

		// Generate new random values for reward selection
		double randomValue = Math.random();
		double randomValueTwo = Math.random();
		int rndNumber = 0;

		double tenToFifty = plugin.getConfig().getDouble("Systems.SpawnSystems.DiamondCatch.TenToFifty");
		double threeToTen = tenToFifty + plugin.getConfig().getDouble("Systems.SpawnSystems.DiamondCatch.ThreeToTen");
		double oneToThree = threeToTen + plugin.getConfig().getDouble("Systems.SpawnSystems.DiamondCatch.OneToThree");

		if (randomValue <= tenToFifty) {
			rndNumber = (int) ((Math.random() * 40) + 10);
			if (randomValueTwo <= 0.50) {
				player.sendMessage("§b§lBM §6You hear a splash§f and see a diamond geyser of §6"
						+ rndNumber + " §fdiamonds erupting from the Wishing-Well. §aYou're rich!!");
			} else {
				player.sendMessage("§b§lBM §dIncredible! §fThe Wishing-Well has revealed an avalanche of §6"
						+ rndNumber + " §fshimmering §bdiamonds§f for you!");
			}
		} else if (randomValue <= threeToTen) {
			rndNumber = (int) ((Math.random() * 7) + 3);
			if (randomValueTwo <= 0.33) {
				player.sendMessage("§b§lBM §eOh, how lucky! §fThe Wishing-Well has gifted you §6"
						+ rndNumber + " §fsparkling §bdiamonds§f.");
			} else if (randomValueTwo <= 0.66) {
				player.sendMessage("§b§lBM §eA pleasant surprise §fawaits you as the Wishing-Well offers you §6"
						+ rndNumber + " §fprecious §bdiamonds§f!");
			} else {
				player.sendMessage("§b§lBM §eWell done! §fThe Wishing-Well graces you with a cluster of §6"
						+ rndNumber + " §eradiant diamonds.");
			}
		} else if (randomValue <= oneToThree) {
			rndNumber = (int) ((Math.random() * 2) + 1);
			if (randomValueTwo <= 0.50) {
				player.sendMessage("§b§lBM §aGreat! §fThe Wishing-Well has rewarded your patience with §6"
						+ rndNumber + " §agleaming diamonds.");
			} else {
				player.sendMessage("§b§lBM §aA gift from the depths! §fThe Wishing-Well has bestowed upon you §6"
						+ rndNumber + " §fbrilliant diamonds.");
			}
		} else {
			rndNumber = 1;
			player.sendMessage("§b§lBM §dWell, well, well! §fThe Wishing-Well has given you a shiny diamond!");
		}

		// Dispatch the command to give the player the diamonds
		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		String commandBuilder = "si give diamonds " + rndNumber + " " + player.getName() + " true";
		Scheduler.run(() -> Bukkit.dispatchCommand(console, commandBuilder));
	}
}
