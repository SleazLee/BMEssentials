package at.sleazlee.bmessentials.trophyroom;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;

public class TrophyTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Check if the command is "/trophy"
        if (command.getName().equalsIgnoreCase("trophy")) {
            // If there's no additional input, return all online player names
            if (args.length == 1) {
                List<String> playerNames = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    playerNames.add(player.getName());
                }
                return playerNames;
            }
        }
        // Return an empty list if the conditions are not met
        return new ArrayList<>();
    }
}