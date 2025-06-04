package at.sleazlee.bmessentials.Help.Abilities;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Help.HelpBooks;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BestToolsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Ensure that only players execute this command
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        String playerName = player.getName();
        HelpBooks books = BMEssentials.getInstance().getBooks();


        // Step 1: Check if the player has the proper rank.
        if (!player.hasPermission("settings.besttools.allow")) {
            // Is not allowed.
            if (player.hasPermission("ranking.blockminer") || player.hasPermission("ranking.super")) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<#F62525><bold>BestTools</bold><red> You need to be at least rank <aqua><bold>[5]</bold><red> to use Chest Sorting!<white> Check out <click:run_command:'/ranks'><hover:show_text:'<aqua>Click to view your current rank chain!</aqua>'><aqua>/ranks</aqua></hover></click><white>."
                ));
                return true;
            } else if (player.hasPermission("ranking.ultra") || player.hasPermission("ranking.premium")) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<#F62525><bold>BestTools</bold><red> You need to be at least rank <aqua><bold>[6]</bold><red> to use Chest Sorting!<white> Check out <click:run_command:'/ranks'><hover:show_text:'<aqua>Click to view your current rank chain!</aqua>'><aqua>/ranks</aqua></hover></click><white>."
                ));
                return true;
            } else if (player.hasPermission("ranking.plus")) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<#F62525><bold>BestTools</bold><red> You need to be at least rank <aqua><bold>[7]</bold><red> to use Chest Sorting!<white> Check out <click:run_command:'/ranks'><hover:show_text:'<aqua>Click to view your current rank chain!</aqua>'><aqua>/ranks</aqua></hover></click><white>."
                ));
                return true;
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<#F62525><bold>BestTools</bold><red> You need to be at least rank <aqua><bold>[7]</bold><red> to use Chest Sorting!<white> Check out <click:run_command:'/ranks'><hover:show_text:'<aqua>Click to view your current rank chain!</aqua>'><aqua>/ranks</aqua></hover></click><white>."
                ));
                return true;
            }


            // Step 2: Run logic for No arguments (/besttools)







            






























        } else if (args.length == 0) {

            if (!player.hasPermission("besttools.use") && !player.hasPermission("besttools.firstuse")) {

                // First time running command.

                // Dispatch commands from the console using Bukkit.dispatchCommand:
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttools.hotkey.middleclick false");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttools.hotkey.shiftclick false");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttools.hotkey.shiftrightclick false");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttools.firstuse true");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttools.use true");
                books.openBook(player, "besttoolssettingstrue");
                return true;


            } else if (!player.hasPermission("besttools.use") && player.hasPermission("besttools.firstuse")) {

                books.openBook(player, "besttoolssettingsfalse");

                return true;

            } else if (player.hasPermission("besttools.use") && player.hasPermission("besttools.firstuse")) {

                books.openBook(player, "besttoolssettingstrue");
                return true;

            } else {

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttools.hotkey.middleclick false");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttools.hotkey.shiftclick false");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttools.hotkey.shiftrightclick false");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttools.firstuse true");
                books.openBook(player, "besttoolssettingstrue");
                return true;

            }

            // Step 3: Run logic for (/besttools toggle <type>)
        } else if (args[0].equalsIgnoreCase("toggle")) {

            // Step 4: Run logic for only (/besttools toggle)
            if (args.length == 1) {

                if (player.hasPermission("besttools.use")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttools.use false");
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<gold><bold>BestTools </bold></gold><gray>You have toggled automatic sorting <color:#ff3300>off</color:#ff3300>!</gray>"));

                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttools.use true");
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<gold><bold>BestTools </bold></gold><gray>You have toggled automatic sorting <green>on</green>!</gray>"));
                }

            } else if (args[1].equals("true")) {

                if (player.hasPermission("besttools.use")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttools.use false");
                    books.openBook(player, "besttoolssettingsfalse");

                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttools.use true");
                    books.openBook(player, "besttoolssettingstrue");
                }

                // Step 5: Run logic for (/besttools toggle "type")
            } else {

                String subCommand = args[1].toLowerCase();
                toggleBestToolsHotkey(player, playerName, subCommand);

            }
            return true;


            // Step 6: Run logic for (/besttools hotkeys)
        } else if (args[0].equalsIgnoreCase("hotkeys")) {

            // If only "toggle" is present, leave a placeholder for future toggle logic
            boolean doubleclick = player.hasPermission("besttools.hotkey.doubleclick");
            boolean shiftclick = player.hasPermission("besttools.hotkey.shiftclick");
            boolean middleclick = player.hasPermission("besttools.hotkey.middleclick");
            boolean shiftrightclick = player.hasPermission("besttools.hotkey.shiftrightclick");

            // Step 4: Run logic for only (/besttools hotkeys)
            if (args.length == 1) {

                books.openBook(player, "besttoolshotkeys" + doubleclick + shiftclick + middleclick + shiftrightclick);

                // Step 5: Run logic for (/besttools hotkeys "type")
            } else {

                String subCommand = args[1].toLowerCase();
                boolean toogled = toggleBestToolsHotkey(player, playerName, subCommand);

                switch (subCommand) {
                    case "doubleclick":
                        books.openBook(player, "besttoolshotkeys" + toogled + shiftclick + middleclick + shiftrightclick);
                        break;
                    case "shiftclick":
                        books.openBook(player, "besttoolshotkeys" + doubleclick + toogled + middleclick + shiftrightclick);
                        break;
                    case "middleclick":
                        books.openBook(player, "besttoolshotkeys" + doubleclick + shiftclick + toogled + shiftrightclick);
                        break;
                    case "shiftrightclick":
                        books.openBook(player, "besttoolshotkeys" + doubleclick + shiftclick + middleclick + toogled);
                        break;
                    default:
                        player.sendMessage("Unknown subcommand for toggle.");
                        break;
                }

            }
            return true;
        } else {
            // error CS101
            player.sendMessage("Report this to SleazLee. Error CS101");
            return true;
        }

    }

    /**
     * Checks if the player has the specified permission.
     *
     * @param player     The player to check.
     * @param permission The permission node to check, e.g. "besttools.use".
     * @return true if the player has the permission, false otherwise.
     */
    public static boolean hasPermission(Player player, String permission) {
        return player.hasPermission(permission);
    }


    public boolean toggleBestToolsHotkey(Player player, String playerName, String subCommand) {
        switch (subCommand) {
            case "doubleclick":
                // Placeholder for "/besttools toggle DoubleClick" logic
                if (player.hasPermission("besttools.hotkey.doubleclick")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttools.hotkey.doubleclick false");
                    return false;
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttools.hotkey.doubleclick true");
                    return true;
                }
            case "shiftclick":
                // Placeholder for "/besttools toggle ShiftClick" logic
                if (player.hasPermission("besttools.hotkey.shiftclick")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttools.hotkey.shiftclick false");
                    return false;
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttools.hotkey.shiftclick true");
                    return true;
                }
            case "middleclick":
                // Placeholder for "/besttools toggle MiddleClick" logic
                if (player.hasPermission("besttools.hotkey.middleclick")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttools.hotkey.middleclick false");
                    return false;
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttools.hotkey.middleclick true");
                    return true;
                }
            case "shiftrightclick":
                // Placeholder for "/besttools toggle ShiftRightClick" logic
                if (player.hasPermission("besttools.hotkey.shiftrightclick")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttools.hotkey.shiftrightclick false");
                    return false;
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttools.hotkey.shiftrightclick true");
                    return true;
                }
            default:
                player.sendMessage("Unknown subcommand for toggle.");
                break;
        }
        return false;
    }

}

