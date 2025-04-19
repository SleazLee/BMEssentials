package at.sleazlee.bmessentials.Help.Abilities;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Help.HelpBooks;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChestSortCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Ensure that only players execute this command
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;

        // Step 1: Check player's permissions "chestsort.use" and "chestsort.firstuse"
        if (!player.hasPermission("chestsort.use") && !player.hasPermission("chestsort.firstuse")) {
            // Is not allowed.
            if (player.hasPermission("ranking.blockminer") || player.hasPermission("ranking.super")) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<#F62525><bold>ChestSort</bold><red> You need to be at least rank <aqua><bold>[0]</bold><red> to use Chest Sorting!<white> Check out <click:run_command:\"/ranks\"><hover:show_text:\"<aqua>Click to view your current rank chain!</aqua>\"><aqua>/ranks</aqua></hover></click><white>."
                ));
                return true;
            } else if (player.hasPermission("ranking.ultra") || player.hasPermission("ranking.premium")) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<#F62525><bold>ChestSort</bold><red> You need to be at least rank <aqua><bold>[1]</bold><red> to use Chest Sorting!<white> Check out <click:run_command:\"/ranks\"><hover:show_text:\"<aqua>Click to view your current rank chain!</aqua>\"><aqua>/ranks</aqua></hover></click><white>."
                ));
                return true;
            } else if (player.hasPermission("ranking.plus")) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<#F62525><bold>ChestSort</bold><red> You need to be at least rank <aqua><bold>[2]</bold><red> to use Chest Sorting!<white> Check out <click:run_command:\"/ranks\"><hover:show_text:\"<aqua>Click to view your current rank chain!</aqua>\"><aqua>/ranks</aqua></hover></click><white>."
                ));
                return true;
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<#F62525><bold>ChestSort</bold><red> You need to be at least rank <aqua><bold>[2]</bold><red> to use Chest Sorting!<white> Check out <click:run_command:\"/ranks\"><hover:show_text:\"<aqua>Click to view your current rank chain!</aqua>\"><aqua>/ranks</aqua></hover></click><white>."
                ));
                return true;
            }
        } else if (player.hasPermission("chestsort.use") && !player.hasPermission("chestsort.firstuse")) {
            // First time running command.
            String playerName = player.getName();
            player.sendMessage("Setting up first-use permissions for " + playerName + " (Placeholder: executing LP commands).");
            // Dispatch commands from the console using Bukkit.dispatchCommand:
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.hotkey.middleclick false");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.hotkey.shiftclick false");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.hotkey.shiftrightclick false");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.firstuse");

            return true;
        } else if (!player.hasPermission("chestsort.use") && player.hasPermission("chestsort.firstuse")){
            if (args.length >= 2) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<#F62525><bold>ChestSort</bold><red> You need to toggle Chest Sorting in able to toggle its abilities! <white>Try <click:run_command:\"/chestsort toggle\"><hover:show_text:\"<gold>Click to toggle ChestSorting!</gold>\"><gold>/chestort toggle</gold></hover></click><white>."
                ));
            }
            return true;
        }



        // Check if there are any arguments
        if (args.length > 0) {
            String playerName = player.getName();
            // Check for the "toggle" subcommand
            if (args[0].equalsIgnoreCase("toggle")) {
                // If only "toggle" is present, leave a placeholder for future toggle logic
                if (args.length == 1) {
                    player.sendMessage("Toggling ChestSort. (Placeholder for future logic)");
                    if (player.hasPermission("chestsort.use")) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.use false");
                    } else {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.use true");
                    }
                    return true;
                }
                // Check for subsub commands:
                if (args.length >= 2) {
                    String subCommand = args[1].toLowerCase();
                    switch (subCommand) {
                        case "doubleclick":
                            // Placeholder for "/chestsort toggle DoubleClick" logic
                            player.sendMessage("DoubleClick toggle executed. (Placeholder for future logic)");
                            if (player.hasPermission("chestsort.hotkey.doubleclick")) {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.hotkey.doubleclick false");
                            } else {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.hotkey.doubleclick true");
                            }
                            break;
                        case "shiftclick":
                            // Placeholder for "/chestsort toggle ShiftClick" logic
                            player.sendMessage("ShiftClick toggle executed. (Placeholder for future logic)");
                            if (player.hasPermission("chestsort.hotkey.shiftclick")) {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.hotkey.shiftclick false");
                            } else {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.hotkey.shiftclick true");
                            }
                            break;
                        case "middleclick":
                            // Placeholder for "/chestsort toggle MiddleClick" logic
                            player.sendMessage("MiddleClick toggle executed. (Placeholder for future logic)");
                            if (player.hasPermission("chestsort.hotkey.middleclick")) {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.hotkey.middleclick false");
                            } else {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.hotkey.middleclick true");
                            }
                            break;
                        case "shiftrightclick":
                            // Placeholder for "/chestsort toggle ShiftRightClick" logic
                            player.sendMessage("ShiftRightClick toggle executed. (Placeholder for future logic)");
                            if (player.hasPermission("chestsort.hotkey.shiftrightclick")) {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.hotkey.shiftrightclick false");
                            } else {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.hotkey.shiftrightclick true");
                            }
                            break;
                        default:
                            player.sendMessage("Unknown subcommand for toggle.");
                            break;
                    }
                    return true;
                }
            }
        } else {
            // No arguments provided:
            HelpBooks books = BMEssentials.getInstance().getBooks();
            if (player.hasPermission("chestsort.use")) {
                books.openBook(player, "chestsortsettingsEnabled");
            } else {
                books.openBook(player, "chestsortsettingsDisabled");
            }
            return true;
        }
        return false;
    }

    /**
     * Checks if the player has the specified permission.
     *
     * @param player     The player to check.
     * @param permission The permission node to check, e.g. "chestsort.use".
     * @return true if the player has the permission, false otherwise.
     */
    public static boolean hasPermission(Player player, String permission) {
        return player.hasPermission(permission);
    }


}

