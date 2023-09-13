package at.sleazlee.bmessentials.Punish;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class UnMuteCommand implements CommandExecutor {


	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (label.equalsIgnoreCase("unmute")) {
			if ((sender instanceof Player)) {
				Player player = (Player) sender;
				if (!player.hasPermission("bm.staff.mute")) {
					sender.sendMessage("§c§lBM §cAccess Denied.");
				} else {
					String playerName = args[0];

					ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
					String CommandBuilder = "mute:mute player " + playerName + " off";
					Bukkit.dispatchCommand(console, CommandBuilder);

					sender.sendMessage("§c§lBM §fIf §e" + playerName + " §fwas muted, they are now §aunmuted§f!");
				}

			} else {
				sender.sendMessage("§c§lBM §cAccess Denied.");
			}
			return true;
		}
		return false;
	}
}
