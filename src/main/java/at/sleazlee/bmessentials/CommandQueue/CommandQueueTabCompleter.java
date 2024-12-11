package at.sleazlee.bmessentials.CommandQueue;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides tab completion suggestions for the /commandqueue command.
 */
public class CommandQueueTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // If the sender does not have permission (e.g., not op), return no completions
        if (!sender.isOp()) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        // /commandqueue <subcommand>
        if (args.length == 1) {
            // Suggest main subcommands
            completions.add("run");
            completions.add("reload");
            completions.add("clean");
            return filterResults(args[0], completions);
        }

        // /commandqueue run <player|console> <delay>
        if (args.length == 2 && args[0].equalsIgnoreCase("run")) {
            completions.add("player");
            completions.add("console");
            return filterResults(args[1], completions);
        }

        // /commandqueue run console <delay> or /commandqueue run player <delay>
        // For delay, we can just return an empty list (or suggest a few example numbers)
        if (args.length == 3 && args[0].equalsIgnoreCase("run")) {
            // You can provide no suggestions or some example delays.
            // Example: suggest a few common delays
            completions.add("1");
            completions.add("3");
            completions.add("5");
            return filterResults(args[2], completions);
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
