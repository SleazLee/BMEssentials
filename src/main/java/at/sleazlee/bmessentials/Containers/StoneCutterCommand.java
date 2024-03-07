package at.sleazlee.bmessentials.Containers;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StoneCutterCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by a player!");
            return true;
        }

        Player player = (Player) sender;

        // Check for the permission
        if (!player.hasPermission("bm.containers.stonecutter")) {

            if(player.hasPermission("ranks.blockminer")) {
                sender.sendMessage("&4&lBM &cYou need to be at least rank &b&l[6]&c to use this ability!&f Check you &b/ranks&f.");

            } else if (player.hasPermission("ranks.super")) {
                sender.sendMessage("&4&lBM &cYou need to be at least rank &b&l[6]&c to use this ability!&f Check you &b/ranks&f.");

            } else if (player.hasPermission("ranks.ultra")) {
                sender.sendMessage("&4&lBM &cYou need to be at least rank &b&l[7]&c to use this ability!&f Check you &b/ranks&f.");

            } else if (player.hasPermission("ranks.premium")) {
                sender.sendMessage("&4&lBM &cYou need to be at least rank &b&l[7]&c to use this ability!&f Check you &b/ranks&f.");

            } else if (player.hasPermission("ranks.plus")) {
                sender.sendMessage("&4&lBM &cYou need to be at least rank &b&l[8]&c to use this ability!&f Check you &b/ranks&f.");

            } else {
                sender.sendMessage("&4&lBM &cYou need to be at least rank &b&l[8]&c to use this ability!&f Check you &b/ranks&f.");
            }

            return true;
        }
        Location location = player.getLocation();

        // Play a sound
        player.getWorld().playSound(location, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);

        player.openStonecutter(location, true);

        return true;
    }
}