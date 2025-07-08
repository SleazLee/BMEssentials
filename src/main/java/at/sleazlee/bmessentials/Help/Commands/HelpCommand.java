package at.sleazlee.bmessentials.Help.Commands;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Help.HelpBooks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HelpCommand implements CommandExecutor {

    // Get the instance of the Help book system
    HelpBooks books = BMEssentials.getInstance().getBooks();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Make sure only players can run this (optional).
        // If you want console support, remove this check.
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use /help in this example.");
            return true;
        }

        Player player = (Player) sender;

        // If no subcommand: /help
        if (args.length == 0) {
            Bukkit.dispatchCommand(player, "book help");

            // Play a sound
            Location location = player.getLocation();
            player.getWorld().playSound(location, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
            return true;
        }

        // /help <subcommand>
        if (args.length == 1) {

            // Play a sound
            Location location = player.getLocation();
            player.getWorld().playSound(location, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);

            // Example: /help claiming => we run "/book claiming"
            if (args[0].equalsIgnoreCase("claiming")) {
                books.openBook(player, "claiming");
                return true;
            }
            else if (args[0].equalsIgnoreCase("lands")) {
                books.openBook(player, "claiming");
                return true;
            }
            else if (args[0].equalsIgnoreCase("money")) {
                books.openBook(player, "money");
                return true;
            }
            else if (args[0].equalsIgnoreCase("ranks")) {
                books.openBook(player, "rankhelp");
                return true;
            }
            else if (args[0].equalsIgnoreCase("voting")) {
                books.openBook(player, "votehelp");
                return true;
            }
            else if (args[0].equalsIgnoreCase("commands")) {
                // If player just types "/help commands" with no second argument
                player.sendMessage("Usage: /help commands <sub-subcommand>");
                return true;
            }
            else if (args[0].equalsIgnoreCase("settings")) {
                books.openBook(player, "settings");
                return true;
            }
            else if (args[0].equalsIgnoreCase("abilities")) {
                books.openBook(player, "abilities");
                return true;
            }
        }

        // /help commands <sub-subcommand>
        // This covers: /help commands abilities, /help commands chat, etc.
        // You can adjust the logic as needed.
        if (args.length == 2 && args[0].equalsIgnoreCase("commands")) {

            if (args[1].equalsIgnoreCase("abilities")) {
                Bukkit.dispatchCommand(player, "command abilities");
                return true;
            }
            else if (args[1].equalsIgnoreCase("basics")) {
                Bukkit.dispatchCommand(player, "command basics");
                return true;
            }
            else if (args[1].equalsIgnoreCase("bms")) {
                Bukkit.dispatchCommand(player, "command bms");
                return true;
            }
            else if (args[1].equalsIgnoreCase("chat")) {
                Bukkit.dispatchCommand(player, "command chat");
                return true;
            }
            else if (args[1].equalsIgnoreCase("chestshop")) {
                Bukkit.dispatchCommand(player, "command chestshop");
                return true;
            }
            else if (args[1].equalsIgnoreCase("fun")) {
                Bukkit.dispatchCommand(player, "command fun");
                return true;
            }
            else if (args[1].equalsIgnoreCase("lands")) {
                Bukkit.dispatchCommand(player, "command lands");
                return true;
            }
            else if (args[1].equalsIgnoreCase("mcmmo")) {
                Bukkit.dispatchCommand(player, "command mcmmo");
                return true;
            }
            else if (args[1].equalsIgnoreCase("settings")) {
                Bukkit.dispatchCommand(player, "command settings");
                return true;
            }
            else if (args[1].equalsIgnoreCase("teleportation")) {
                Bukkit.dispatchCommand(player, "command teleportation");
                return true;
            }
            else if (args[1].equalsIgnoreCase("trophies")) {
                Bukkit.dispatchCommand(player, "command trophies");
                return true;
            }
            else if (args[1].equalsIgnoreCase("unlocks")) {
                Bukkit.dispatchCommand(player, "command unlocks");
                return true;
            }
            else if (args[1].equalsIgnoreCase("vip")) {
                Bukkit.dispatchCommand(player, "command vip");
                return true;
            }
            else {
                // Unrecognized sub-subcommand
                player.sendMessage("Unknown command. Try /help commands for a list.");
                return true;
            }
        }

        // If more than 2 arguments or something else, default message
        player.sendMessage("Usage: /help <subcommand>");
        return true;
    }
}

