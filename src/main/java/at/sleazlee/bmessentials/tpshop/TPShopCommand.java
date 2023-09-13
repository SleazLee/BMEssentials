package at.sleazlee.bmessentials.tpshop;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class TPShopCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // Creates a set for 001 to 092.
            Set<String> validArgs = new HashSet<>();
            for (int i = 1; i <= 92; i++) {
                validArgs.add(String.format("%03d", i));
            }

            // Then, in your command execution logic
            if (args.length > 0) {
                String shopPlot = args[0];
                if (validArgs.contains(shopPlot)) {
                    // Your logic here
                    Bukkit.getServer().dispatchCommand(player, "warp " + shopPlot);
                } else {
                    // Handle invalid case
                    player.sendMessage("§b§lBMS§7 You need the shops plot number! Try §b/tpshop §8<§b###§8>§7.");
                }
            }

            return true;
        }

        return false;
    }
}
