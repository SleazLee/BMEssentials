package at.sleazlee.bmessentials.wild;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.ArrayList;

/**
 * The WildTabCompleter class provides tab completion for the /wild command.
 */
public class WildTabCompleter implements TabCompleter {

    private final WildData wildData; // Reference to WildData for available versions.

    /**
     * Constructs a WildTabCompleter object.
     *
     * @param wildData The WildData instance containing version information.
     */
    public WildTabCompleter(WildData wildData) {
        this.wildData = wildData;
    }

    /**
     * Handles tab completion for the /wild command.
     *
     * @param sender  The command sender.
     * @param command The command object.
     * @param alias   The alias used.
     * @param args    Command arguments.
     * @return A list of possible completions.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Provide a list of versions and 'all' as suggestions.
            List<String> completions = new ArrayList<>(wildData.getVersions());
            completions.add("all");
            return completions;
        }
        return null;
    }
}
