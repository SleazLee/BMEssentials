package at.sleazlee.bmessentials.votesystem;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TestVoteTabCompleter implements TabCompleter {

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 1) {
			List<String> list = new ArrayList<>();
			for (Player player : Bukkit.getOnlinePlayers()) {
				list.add(player.getName());
			}
			return list;
		}
		return null;
	}
}