package at.sleazlee.bmessentials.Help.TabCompleter;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides tab completion suggestions for the /command command.
 */
public class RanksTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        List<String> completions = new ArrayList<>();

        // /help <subcommand>
        if (args.length == 1) {
            // Suggest main subcommands
            completions.add("Default");
            completions.add("Plus");
            completions.add("Premium");
            completions.add("Ultra");
            completions.add("Super");
            completions.add("Blockminer");
            completions.add("Donor");
            completions.add("Current");

            return filterResults(args[0], completions);
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
