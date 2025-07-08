package at.sleazlee.bmessentials.Help.Commands;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Help.HelpBooks;
import at.sleazlee.bmessentials.Help.HelpText;
import at.sleazlee.bmessentials.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HelpCommand implements CommandExecutor {


    // Get the instance of the Help book and text systems
    HelpBooks books = BMEssentials.getInstance().getBooks();
    HelpText commands = new HelpText(BMEssentials.getInstance());

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
            books.openBook(player, "help");

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

            switch (args[1].toLowerCase()) {
                case "abilities" -> commands.sendCommandInfo(player, "abilities");
                case "basics" -> commands.sendCommandInfo(player, "basics");
                case "bms" -> commands.sendCommandInfo(player, "arm");
                case "chat" -> commands.sendCommandInfo(player, "communication");
                case "chestshop" -> commands.sendCommandInfo(player, "quickshop");
                case "fun" -> commands.sendCommandInfo(player, "fun");
                case "lands" -> commands.sendCommandInfo(player, "lands1");
                case "mcmmo" -> commands.sendCommandInfo(player, "mcmmo");
                case "settings" -> commands.sendCommandInfo(player, "settings");
                case "teleportation" -> commands.sendCommandInfo(player, "teleportation");
                case "trophies" -> commands.sendCommandInfo(player, "trophies");
                case "unlocks" -> commands.sendCommandInfo(player, "unlocks");
                case "vip" -> commands.sendCommandInfo(player, "vip");
                default -> {
                    player.sendMessage("Unknown command. Try /help commands for a list.");
                    return true;
                }
            }
        return true;
        }

        // If more than 2 arguments or something else, default message
        player.sendMessage("Usage: /help <subcommand>");
        return true;
    }
}

