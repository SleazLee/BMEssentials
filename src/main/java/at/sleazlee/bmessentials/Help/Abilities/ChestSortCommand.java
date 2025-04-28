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
        String playerName = player.getName();
        HelpBooks books = BMEssentials.getInstance().getBooks();


        // Step 1: Check if the player has the proper rank.
        if (!player.hasPermission("settings.chestsort.allow")) {
            // Is not allowed.
            if (player.hasPermission("ranking.blockminer") || player.hasPermission("ranking.super")) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<#F62525><bold>ChestSort</bold><red> You need to be at least rank <aqua><bold>[0]</bold><red> to use Chest Sorting!<white> Check out <click:run_command:'/ranks'><hover:show_text:'<aqua>Click to view your current rank chain!</aqua>'><aqua>/ranks</aqua></hover></click><white>."
                ));
                return true;
            } else if (player.hasPermission("ranking.ultra") || player.hasPermission("ranking.premium")) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<#F62525><bold>ChestSort</bold><red> You need to be at least rank <aqua><bold>[1]</bold><red> to use Chest Sorting!<white> Check out <click:run_command:'/ranks'><hover:show_text:'<aqua>Click to view your current rank chain!</aqua>'><aqua>/ranks</aqua></hover></click><white>."
                ));
                return true;
            } else if (player.hasPermission("ranking.plus")) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<#F62525><bold>ChestSort</bold><red> You need to be at least rank <aqua><bold>[2]</bold><red> to use Chest Sorting!<white> Check out <click:run_command:'/ranks'><hover:show_text:'<aqua>Click to view your current rank chain!</aqua>'><aqua>/ranks</aqua></hover></click><white>."
                ));
                return true;
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<#F62525><bold>ChestSort</bold><red> You need to be at least rank <aqua><bold>[2]</bold><red> to use Chest Sorting!<white> Check out <click:run_command:'/ranks'><hover:show_text:'<aqua>Click to view your current rank chain!</aqua>'><aqua>/ranks</aqua></hover></click><white>."
                ));
                return true;
            }


            // Step 2: Run logic for No arguments (/chestsort)
        } else if (args.length == 0) {

            if (!player.hasPermission("chestsort.use") && !player.hasPermission("chestsort.firstuse")) {

                // First time running command.

                // Dispatch commands from the console using Bukkit.dispatchCommand:
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.hotkey.middleclick false");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.hotkey.shiftclick false");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.hotkey.shiftrightclick false");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.firstuse true");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.use true");
                books.openBook(player, "chestsortsettingstrue");
                return true;


            } else if (!player.hasPermission("chestsort.use") && player.hasPermission("chestsort.firstuse")) {

                books.openBook(player, "chestsortsettingsfalse");

                return true;

            } else if (player.hasPermission("chestsort.use") && player.hasPermission("chestsort.firstuse")) {

                books.openBook(player, "chestsortsettingstrue");
                return true;

            } else {

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.hotkey.middleclick false");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.hotkey.shiftclick false");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.hotkey.shiftrightclick false");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.firstuse true");
                books.openBook(player, "chestsortsettingstrue");
                return true;

            }

            // Step 3: Run logic for (/chestsort toggle <type>)
        } else if (args[0].equalsIgnoreCase("toggle")) {

            // Step 4: Run logic for only (/chestsort toggle)
            if (args.length == 1) {

                if (player.hasPermission("chestsort.use")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.use false");
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<gold><bold>ChestSort </bold></gold><gray>You have toggled automatic sorting <color:#ff3300>off</color:#ff3300>!</gray>"));

                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.use true");
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<gold><bold>ChestSort </bold></gold><gray>You have toggled automatic sorting <green>on</green>!</gray>"));
                }

            } else if (args[1].equals("true")) {

                if (player.hasPermission("chestsort.use")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.use false");
                    books.openBook(player, "chestsortsettingsfalse");

                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.use true");
                    books.openBook(player, "chestsortsettingstrue");
                }

            // Step 5: Run logic for (/chestsort toggle "type")
            } else {

                String subCommand = args[1].toLowerCase();
                toggleChestSortHotkey(player, playerName, subCommand);

            }
            return true;


            // Step 6: Run logic for (/chestsort hotkeys)
        } else if (args[0].equalsIgnoreCase("hotkeys")) {

            // If only "toggle" is present, leave a placeholder for future toggle logic
            boolean doubleclick = player.hasPermission("chestsort.hotkey.doubleclick");
            boolean shiftclick = player.hasPermission("chestsort.hotkey.shiftclick");
            boolean middleclick = player.hasPermission("chestsort.hotkey.middleclick");
            boolean shiftrightclick = player.hasPermission("chestsort.hotkey.shiftrightclick");

            // Step 4: Run logic for only (/chestsort hotkeys)
            if (args.length == 1) {

                books.openBook(player, "chestsorthotkeys" + doubleclick + shiftclick + middleclick + shiftrightclick);

                // Step 5: Run logic for (/chestsort hotkeys "type")
            } else {

                String subCommand = args[1].toLowerCase();
                boolean toogled = toggleChestSortHotkey(player, playerName, subCommand);

                switch (subCommand) {
                    case "doubleclick":
                        books.openBook(player, "chestsorthotkeys" + toogled + shiftclick + middleclick + shiftrightclick);
                        break;
                    case "shiftclick":
                        books.openBook(player, "chestsorthotkeys" + doubleclick + toogled + middleclick + shiftrightclick);
                        break;
                    case "middleclick":
                        books.openBook(player, "chestsorthotkeys" + doubleclick + shiftclick + toogled + shiftrightclick);
                        break;
                    case "shiftrightclick":
                        books.openBook(player, "chestsorthotkeys" + doubleclick + shiftclick + middleclick + toogled);
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
     * @param permission The permission node to check, e.g. "chestsort.use".
     * @return true if the player has the permission, false otherwise.
     */
    public static boolean hasPermission(Player player, String permission) {
        return player.hasPermission(permission);
    }


    public boolean toggleChestSortHotkey(Player player, String playerName, String subCommand) {
        switch (subCommand) {
            case "doubleclick":
                // Placeholder for "/chestsort toggle DoubleClick" logic
                if (player.hasPermission("chestsort.hotkey.doubleclick")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.hotkey.doubleclick false");
                    return false;
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.hotkey.doubleclick true");
                    return true;
                }
            case "shiftclick":
                // Placeholder for "/chestsort toggle ShiftClick" logic
                if (player.hasPermission("chestsort.hotkey.shiftclick")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.hotkey.shiftclick false");
                    return false;
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.hotkey.shiftclick true");
                    return true;
                }
            case "middleclick":
                // Placeholder for "/chestsort toggle MiddleClick" logic
                if (player.hasPermission("chestsort.hotkey.middleclick")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.hotkey.middleclick false");
                    return false;
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.hotkey.middleclick true");
                    return true;
                }
            case "shiftrightclick":
                // Placeholder for "/chestsort toggle ShiftRightClick" logic
                if (player.hasPermission("chestsort.hotkey.shiftrightclick")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.hotkey.shiftrightclick false");
                    return false;
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set chestsort.hotkey.shiftrightclick true");
                    return true;
                }
            default:
                player.sendMessage("Unknown subcommand for toggle.");
                break;
        }
        return false;
    }

}

