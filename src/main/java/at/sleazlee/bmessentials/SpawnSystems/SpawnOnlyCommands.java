package at.sleazlee.bmessentials.SpawnSystems;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnOnlyCommands implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender instanceof Player) {

			Player player = (Player) sender;

			if (label.equalsIgnoreCase("vot") || label.equalsIgnoreCase("voting")) {

				player.sendMessage("§e§lVot §cYou can only do this in the Wild Districts! §fSee §e/wild§f.");

				return true;
			}
		}
		return false;
	}
}
