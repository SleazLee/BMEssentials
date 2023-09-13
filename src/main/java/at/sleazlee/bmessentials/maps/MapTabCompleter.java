package at.sleazlee.bmessentials.maps;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class MapTabCompleter implements TabCompleter {

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 1) {
			List<String> completions = new ArrayList<>();
			completions.add("WildA");
			completions.add("WildB");
			completions.add("WildC");
			completions.add("WildD");
			return completions;
		}
		return null;
	}
}
