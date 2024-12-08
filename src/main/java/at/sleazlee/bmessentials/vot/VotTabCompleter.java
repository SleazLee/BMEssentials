package at.sleazlee.bmessentials.vot;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;


import java.util.ArrayList;
import java.util.List;

public class VotTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            list.add("Day");
            list.add("Night");
            list.add("Clear");
            list.add("Rain");
            list.add("Thunder");
            return list;
        }

        return null;
    }
}

