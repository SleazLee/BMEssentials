package at.sleazlee.bmessentials.Help.Commands;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Help.HelpBooks;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RanksCommand implements CommandExecutor {

    // Get the instance of the Help book system
    HelpBooks books = BMEssentials.getInstance().getBooks();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Make sure only players can run this (optional).
        // If you want console support, remove this check.
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use /ranks in this example.");
            return true;
        }

        Player player = (Player) sender;

        // If no subcommand: /help
        if (args.length == 0) {
            findRank(player);
            // Play a sound
            Location location = player.getLocation();
            player.getWorld().playSound(location, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
            return true;
        }

        // /ranks <subcommand>
        if (args.length == 1) {

            // Play a sound
            Location location = player.getLocation();
            player.getWorld().playSound(location, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);

            // Example: /ranks plus => we run "/book plusrank"
            if (args[0].equalsIgnoreCase("default")) {
                books.openBook(player, "defaultrank");
                return true;
            }
            else if (args[0].equalsIgnoreCase("plus")) {
                books.openBook(player, "plusrank");
                return true;
            }
            else if (args[0].equalsIgnoreCase("premium")) {
                books.openBook(player, "premiumrank");
                return true;
            }
            else if (args[0].equalsIgnoreCase("ultra")) {
                books.openBook(player, "ultrarank");
                return true;
            }
            else if (args[0].equalsIgnoreCase("super")) {
                books.openBook(player, "superrank");
                return true;
            }
            else if (args[0].equalsIgnoreCase("blockminer")) {
                books.openBook(player, "blockminerrank");
                return true;
            }
            else if (args[0].equalsIgnoreCase("donor")) {
                books.openBook(player, "donorranks");
                return true;
            }
            }else if (args[0].equalsIgnoreCase("current")) {
                findRank(player);
                return true;
            }

        // If more than 2 arguments or something else, default message
        player.sendMessage("Usage: /ranks <subcommand>");
        return true;
    }

    /**
     * Opens a rank book for the specified player based on their rank.
     *
     * @param player   The player who will receive the book.
     */
    public void findRank(Player player) {
        if(player.hasPermission("ranks.blockminer")) {
            books.openBook(player, "blockminerrank");
        } else if (player.hasPermission("ranks.super")) {
            books.openBook(player, "superrank");
        } else if (player.hasPermission("ranks.ultra")) {
            books.openBook(player, "ultrarank");
        } else if (player.hasPermission("ranks.premium")) {
            books.openBook(player, "premiumrank");
        } else if (player.hasPermission("ranks.plus")) {
            books.openBook(player, "plusrank");
        } else {
            books.openBook(player, "defaultrank");
        }
    }
}

