package at.sleazlee.bmessentials.bmefunctions;

import at.sleazlee.bmessentials.BMEssentials;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BMECommandExecutor implements CommandExecutor {
	private final BMEssentials plugin;

	public BMECommandExecutor(BMEssentials plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = (Player) sender;
		if (command.getName().equalsIgnoreCase("bme")) {
			if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {

				// Bukkit provided config.yml reload method.
				plugin.reloadConfig();
				//My created reload method for <filename>.yml.
				//plugin.reloadMessagesFile();  // reload messages.yml

				player.sendMessage("§b§lBM §bBMEssentials configuration reloaded!");
				return true;
			} else {
				// Handle invalid case
				player.sendMessage("§c§lBM §cPlease provide more arguments!");
			}
		} else {
			//other commands

		}
		return false;
	}

}