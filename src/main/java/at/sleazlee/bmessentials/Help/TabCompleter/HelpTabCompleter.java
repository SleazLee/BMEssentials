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
            completions.add("Claiming");
            completions.add("Money");
            completions.add("Ranks");
            completions.add("Voting");
            completions.add("Commands");
            completions.add("Settings");
            completions.add("Abilities");

            return filterResults(args[0], completions);
        }

        // /help commands <subcommand>
        if (args.length == 2 && args[0].equalsIgnoreCase("commands")) {
            completions.add("Abilities");
            completions.add("Basics");
            completions.add("Bms");
            completions.add("Chat");
            completions.add("Chestshop");
            completions.add("Fun");
            completions.add("Lands");
            completions.add("Mcmmo");
            completions.add("Settings");
            completions.add("Teleportation");
            completions.add("Trophies");
            completions.add("Unlocks");
            completions.add("Vip");
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
