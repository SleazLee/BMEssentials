package at.sleazlee.bmessentials.wild;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.ArrayList;

/**
 * Provides tab-completion for the /wild command:
 *
 * <p>Completion suggests all known versions plus "all".</p>
 */
public class WildTabCompleter implements TabCompleter {

    private final WildData wildData;

    /**
     * Constructs a WildTabCompleter with references to the plugin's WildData.
     *
     * @param wildData The WildData containing version ring info
     */
    public WildTabCompleter(WildData wildData) {
        this.wildData = wildData;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Only handle the first argument
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(wildData.getVersions());
            completions.add("all");
            return completions;
        }
        return null;
    }
}