package at.sleazlee.bmessentials.DonationSystem;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.VTell.VTellCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import static at.sleazlee.bmessentials.DonationSystem.DonationRankMethods.playerDonated;
import static at.sleazlee.bmessentials.TextUtils.TextCenter.center;
import static at.sleazlee.bmessentials.TextUtils.TextCenter.fullLineStrike;

public class DonationCommand implements CommandExecutor {


	private final BMEssentials plugin;

	public DonationCommand(BMEssentials plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		if (sender instanceof Player) {
			sender.sendMessage(ChatColor.RED + "This command can only be run from the console.");
			return true;
		}

		if (args.length <= 1) {
			return false;
		}

		String playerName = args[0];
		String donationPackage = args[1].toLowerCase();

		switch (donationPackage) {
			case "freepotatoes":
				buyFreePotatoes(playerName);
				break;
			case "blockminerblast":
				buyBlockminerBlast(playerName);
				break;
			case "lousybrick":
				buyLousyBrick(playerName);
				break;
			case "plus":
				buyPlusRank(playerName);
				break;
			case "premium":
				buyPremiumRank(playerName);
				break;
			case "ultra":
				buyUltraRank(playerName);
				break;
			case "super":
				buySuperRank(playerName);
				break;
			case "blockminer":
				buyBlockminerRank(playerName);
				break;
			default:
		}
		return true;
	}

	public void buyFreePotatoes(String playerName) {
		Player player = Bukkit.getPlayer(playerName);
		String displayName = player.getDisplayName();

		VTellCommand.broadcastMessage("<aqua><bold>Shop</bold><white> <red>" + displayName + "<white>, just purchased <gold><bold>Free Potatoes</bold><white> from the webstore.");
		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		String commandBuilder = "si give hotpotato 32 " + playerName;
		Bukkit.dispatchCommand(console, commandBuilder);
	}

	public void buyBlockminerBlast(String playerName) {
		Player player = Bukkit.getPlayer(playerName);
		String displayName = player.getDisplayName();

		VTellCommand.broadcastMessage("<aqua><bold>Shop</bold><white> <red>" + displayName + "<white>, just purchased <dark_aqua><bold>Blockminer Blast</bold><white> from the webstore.");
		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		String commandBuilder = "si give blockminerblast 1 " + playerName;
		Bukkit.dispatchCommand(console, commandBuilder);
	}

	public void buyLousyBrick(String playerName) {
		Player player = Bukkit.getPlayer(playerName);
		String displayName = player.getDisplayName();

		VTellCommand.broadcastMessage("<aqua><bold>Shop</bold><white> <red>" + displayName + "<white>, just purchased <color:#B5593F><bold>A Brick</bold><white> from the webstore.");
		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		String commandBuilder = "si give lousybrick 1 " + playerName;
		Bukkit.dispatchCommand(console, commandBuilder);
	}

	public void buyPlusRank(String playerName) {
		Player player = Bukkit.getPlayer(playerName);
		String displayName = player.getDisplayName();

		String line1 = "<dark_gray>[<green><bold>PLUS CELEBRATION</bold><dark_gray>]";
		String line3 = "<gray>Nice! <aqua>" + displayName + "<gray> has obtained <dark_gray>[<green>Plus<dark_gray>] <gray>rank!";
		String line4 = "<gray>Your <underlined>great</underlined><gray> generosity will help our server!";

		VTellCommand.broadcastMessage(center(line1, "dark_gray") + "<newline><newline>" + center(line3) + "<newline>" + center(line4) + "<newline><newline>" + fullLineStrike("dark_gray"));
		playerDonated(plugin, playerName, "plus");
	}

	public void buyPremiumRank(String playerName) {
		Player player = Bukkit.getPlayer(playerName);
		String displayName = player.getDisplayName();

		String line1 = "<dark_gray>[<aqua><bold>PREMIUM CELEBRATION</bold><dark_gray>]";
		String line3 = "<gray>Wow! <aqua>" + displayName + "<gray> has acquired <dark_gray>[<aqua>Premium<dark_gray>] <gray>rank!";
		String line4 = "<gray>Your <underlined>honorable</underlined><gray> generosity will benefit our server!";

		VTellCommand.broadcastMessage(center(line1, "dark_gray") + "<newline><newline>" + center(line3) + "<newline>" + center(line4) + "<newline><newline>" + fullLineStrike("dark_gray"));

		playerDonated(plugin, playerName, "premium");
	}

	public void buyUltraRank(String playerName) {
		Player player = Bukkit.getPlayer(playerName);
		String displayName = player.getDisplayName();

		String line1 = "<dark_gray>[<blue><bold>ULTRA CELEBRATION</bold><dark_gray>]";
		String line3 = "<gray>Woah! <aqua>" + displayName + "<gray> has reached <dark_gray>[<blue>Ultra<dark_gray>] <gray>rank!";
		String line4 = "<gray>Your <underlined>noble</underlined><gray> generosity will support our server!";

		VTellCommand.broadcastMessage(center(line1, "dark_gray") + "<newline><newline>" + center(line3) + "<newline>" + center(line4) + "<newline><newline>" + fullLineStrike("dark_gray"));

		playerDonated(plugin, playerName, "ultra");
	}

	public void buySuperRank(String playerName) {
		Player player = Bukkit.getPlayer(playerName);
		String displayName = player.getDisplayName();

		String line1 = "<dark_gray>[<light_purple><bold>SUPER CELEBRATION</bold><dark_gray>]";
		String line3 = "<gray>Incredible! <aqua>" + displayName + "<gray> has attained <dark_gray>[<light_purple>Super<dark_gray>] <gray>rank!";
		String line4 = "<gray>Your <underlined>serious</underlined><gray> generosity will advance our server!";

		VTellCommand.broadcastMessage(center(line1, "dark_gray") + "<newline><newline>" + center(line3) + "<newline>" + center(line4) + "<newline><newline>" + fullLineStrike("dark_gray"));

		playerDonated(plugin, playerName, "super");
	}

	public void buyBlockminerRank(String playerName) {
		Player player = Bukkit.getPlayer(playerName);
		String displayName = player.getDisplayName();

		String line1 = "<dark_gray>[<gradient:#43d1d1:#4d5a5a><bold>BLOCKMINER CELEBRATION</bold></gradient><dark_gray>]";
		String line3 = "<gray>Unbelievable! <aqua>" + displayName + "<gray> is now <gradient:#43d1d1:#4e5b5b>[BLOCKMINER]</gradient>&f <gray>rank!";
		String line4 = "<gray>Your <underlined>grand</underlined><gray> generosity will evolve our server!";

		VTellCommand.broadcastMessage(center(line1, "dark_gray") + "<newline><newline>" + center(line3) + "<newline>" + center(line4) + "<newline><newline>" + fullLineStrike("dark_gray"));

		playerDonated(plugin, playerName, "blockminer");
	}

}