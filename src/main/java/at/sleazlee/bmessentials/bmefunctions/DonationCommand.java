package at.sleazlee.bmessentials.bmefunctions;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.VTell.VTellCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

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

		VTellCommand.broadcastMessage("&b&lShop&f &c" + displayName + "&f, just purchased &6&lFree Potatoes&f from the webstore.");
		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		String commandBuilder = "si give hotpotato 32 " + playerName;
		Bukkit.dispatchCommand(console, commandBuilder);
	}

	public void buyBlockminerBlast(String playerName) {
		Player player = Bukkit.getPlayer(playerName);
		String displayName = player.getDisplayName();

		VTellCommand.broadcastMessage("&b&lShop&f &c" + displayName + "&f, just purchased &3&lBlockminer Blast&f from the webstore.");
		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		String commandBuilder = "si give blockminerblast 1 " + playerName;
		Bukkit.dispatchCommand(console, commandBuilder);
	}

	public void buyLousyBrick(String playerName) {
		Player player = Bukkit.getPlayer(playerName);
		String displayName = player.getDisplayName();

		VTellCommand.broadcastMessage("&b&lShop&f &c" + displayName + "&f, just purchased #B5593F&lA Brick&f from the webstore.");
		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		String commandBuilder = "si give lousybrick 1 " + playerName;
		Bukkit.dispatchCommand(console, commandBuilder);
	}

	public void buyPlusRank(String playerName) {
		Player player = Bukkit.getPlayer(playerName);
		String displayName = player.getDisplayName();

		VTellCommand.broadcastMessage("&8&m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &8 [&a&lPLUS CELEBRATION&8] &8&m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m<newline><newline>&7 &7 &7 &7 &7 &7 &7 &7 &7 &7 &7 &7 Nice! &b" + displayName + "&7 has obtained &8[&aPlus&8] &7rank!<newline>&7 &7 &7 &7 &7 &7 &7 &7 &7 &7 Your &ngreat&7 generosity will help our server!<newline><newline>&8&m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m ");
		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		String commandBuilder = "bmlands purchased " + playerName + " plus";
		Bukkit.dispatchCommand(console, commandBuilder);
	}

	public void buyPremiumRank(String playerName) {
		Player player = Bukkit.getPlayer(playerName);
		String displayName = player.getDisplayName();

		VTellCommand.broadcastMessage("&8&m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &8 [&b&lPREMIUM CELEBRATION&8] &8&m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m <newline><newline>&7 &7 &7 &7 &7 &7 &7 &7 Wow! &b$arg1&7 has acquired &8[&bPremium&8] &7rank!<newline>&7 &7 &7 &7 Your &nhonorable&7 generosity will benefit our server!<newline><newline>&8&m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m ");
		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		String commandBuilder = "bmlands purchased " + playerName + " premium";
		Bukkit.dispatchCommand(console, commandBuilder);
	}

	public void buyUltraRank(String playerName) {
		Player player = Bukkit.getPlayer(playerName);
		String displayName = player.getDisplayName();

		VTellCommand.broadcastMessage("&8&m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &8 [&9&lULTRA CELEBRATION&8] &8&m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m <newline><newline>&7 &7 &7 &7 &7 &7 &7 &7 &7 &7 &7 Woah! &b$arg1&7 has reached &8[&9Ultra&8] &7rank!<newline>&7 &7 &7 &7 &7 &7 &7 &7 Your &nnoble&7 generosity will support our server!<newline><newline>&8&m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m ");
		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		String commandBuilder = "bmlands purchased " + playerName + " ultra";
		Bukkit.dispatchCommand(console, commandBuilder);
	}

	public void buySuperRank(String playerName) {
		Player player = Bukkit.getPlayer(playerName);
		String displayName = player.getDisplayName();

		VTellCommand.broadcastMessage("&8&m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &8 [&d&lSUPER CELEBRATION&8] &8&m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m <newline><newline>&7 &7 &7 &7 &7 &7 &7 Incredible! &b$arg1&7 has attained &8[&dSuper&8] &7rank!<newline>&7 &7 &7 &7 &7 &7 Your &nserious&7 generosity will advance our server!<newline><newline>&8&m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m ");
		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		String commandBuilder = "bmlands purchased " + playerName + " super";
		Bukkit.dispatchCommand(console, commandBuilder);
	}

	public void buyBlockminerRank(String playerName) {
		Player player = Bukkit.getPlayer(playerName);
		String displayName = player.getDisplayName();

		VTellCommand.broadcastMessage("&8&m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &8 [#43d1d1&lB#46cbcb&lL#48c5c5&lO#4abfbf&lC#4cb9b9&lK#4eb3b3&lM#4fadae&lI#50a8a8&lN#51a2a2&lE#529c9c&lR #529191&lC#528b8b&lE#528586&lL#528080&lE#527a7a&lB#517575&lR#516f6f&lA#506a6a&lT#4f6565&lI#4e5f5f&lO#4d5a5a&lN&8] &8&m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m <newline><newline>&7 &7 &7 Unbelievable! &b$arg1&7 is now #43d1d1[#48c5c6B#4cbabbL#4fafb0O#51a4a5C#52999aK#538f8fM#538484I#527a7aN#516f6fE#506565R#4e5b5b]&f &7rank!<newline>&7 &7 &7 &7 Your &ngrand&7 generosity will evolve our server!<newline><newline>&8&m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m &m ");
		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		String commandBuilder = "bmlands purchased " + playerName + " blockminer";
		Bukkit.dispatchCommand(console, commandBuilder);
	}


}