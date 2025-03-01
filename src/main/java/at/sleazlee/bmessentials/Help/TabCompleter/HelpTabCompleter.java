package at.sleazlee.bmessentials.Help.TabCompleter;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides tab completion suggestions for the /help command.
 */
public class HelpTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        List<String> completions = new ArrayList<>();

        // /help <subcommand>
        if (args.length == 1) {
            // Suggest main subcommands
            completions.add("claiming");
            completions.add("money");
            completions.add("ranks");
            completions.add("voting");
            completions.add("commands");
            completions.add("settings");
            completions.add("abilities");

            return filterResults(args[0], completions);
        }

        // /help commands <subcommand>
        if (args.length == 2 && args[0].equalsIgnoreCase("commands")) {
            completions.add("abilities");
            completions.add("basics");
            completions.add("arm");
            completions.add("chat");
            completions.add("chestshop");
            completions.add("fun");
            completions.add("lands");
            completions.add("mcmmo");
            completions.add("settings");
            completions.add("teleportation");
            completions.add("trophies");
            completions.add("unlocks");
            completions.add("vip");
            return filterResults(args[1], completions);
        }


        // If we get here, no suggestions:
        return Collections.emptyList();
    }

    /**
     * Filters the given list of completions to only include those that start with the provided argument.
     */
    private List<String> filterResults(String arg, List<String> completions) {
        List<String> filtered = new ArrayList<>();
        for (String completion : completions) {
            if (completion.toLowerCase().startsWith(arg.toLowerCase())) {
                filtered.add(completion);
            }
        }
        return filtered;
    }
}
