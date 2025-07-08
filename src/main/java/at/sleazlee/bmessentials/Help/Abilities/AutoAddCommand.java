package at.sleazlee.bmessentials.Help.Abilities;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Help.HelpBooks;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AutoAddCommand implements CommandExecutor {

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
        if (!player.hasPermission("settings.autoadd.allow")) {
            // Is not allowed.
            if (player.hasPermission("ranking.blockminer") || player.hasPermission("ranking.super")) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<#F62525><bold>AutoAdd</bold><red> You need to be at least rank <aqua><bold>[5]</bold><red> to use Chest Sorting!<white> Check out <click:run_command:'/ranks'><hover:show_text:'<aqua>Click to view your current rank chain!</aqua>'><aqua>/ranks</aqua></hover></click><white>."
                ));
                return true;
            } else if (player.hasPermission("ranking.ultra") || player.hasPermission("ranking.premium")) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<#F62525><bold>AutoAdd</bold><red> You need to be at least rank <aqua><bold>[6]</bold><red> to use Chest Sorting!<white> Check out <click:run_command:'/ranks'><hover:show_text:'<aqua>Click to view your current rank chain!</aqua>'><aqua>/ranks</aqua></hover></click><white>."
                ));
                return true;
            } else if (player.hasPermission("ranking.plus")) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<#F62525><bold>AutoAdd</bold><red> You need to be at least rank <aqua><bold>[7]</bold><red> to use Chest Sorting!<white> Check out <click:run_command:'/ranks'><hover:show_text:'<aqua>Click to view your current rank chain!</aqua>'><aqua>/ranks</aqua></hover></click><white>."
                ));
                return true;
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<#F62525><bold>AutoAdd</bold><red> You need to be at least rank <aqua><bold>[7]</bold><red> to use Chest Sorting!<white> Check out <click:run_command:'/ranks'><hover:show_text:'<aqua>Click to view your current rank chain!</aqua>'><aqua>/ranks</aqua></hover></click><white>."
                ));
                return true;
            }


            // Step 2: Run logic for No arguments (/autoadd)
        } else if (args.length == 0) {

            if (!player.hasPermission("autoadd.use") && !player.hasPermission("autoadd.firstuse")) {

                // First time running command.

                // Dispatch commands from the console using Bukkit.dispatchCommand:
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set autoadd.firstuse true");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set autoadd.use true");
                books.openBook(player, "autoaddsettingstrue");
                return true;


            } else if (!player.hasPermission("autoadd.use") && player.hasPermission("autoadd.firstuse")) {

                books.openBook(player, "autoaddsettingsfalse");

                return true;

            } else if (player.hasPermission("autoadd.use") && player.hasPermission("autoadd.firstuse")) {

                books.openBook(player, "autoaddsettingstrue");
                return true;

            } else {

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set autoadd.firstuse true");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set autoadd.use true");
                books.openBook(player, "besttooltruefalse");
                return true;

            }

            // Step 3: Run logic for (/autoadd toggle <type>)
        } else if (args[0].equalsIgnoreCase("toggle")) {

            // Step 4: Run logic for only (/autoadd toggle)
            if (args.length == 1) {

                if (player.hasPermission("autoadd.use")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set autoadd.use false");
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<gold><bold>AutoAdd </bold></gold><gray>You have toggled automatic sorting <color:#ff3300>off</color:#ff3300>!</gray>"));

                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set autoadd.use true");
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<gold><bold>AutoAdd </bold></gold><gray>You have toggled automatic sorting <green>on</green>!</gray>"));
                }

            } else if (args[1].equals("true")) {

                if (player.hasPermission("autoadd.use")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set autoadd.use false");
                    if (player.hasPermission("autoadd.refill")) {
                        books.openBook(player, "besttoolfalsetrue");
                    } else {
                        books.openBook(player, "besttoolfalsefalse");
                    }

                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set autoadd.use true");
                    if (player.hasPermission("autoadd.refill")) {
                        books.openBook(player, "besttooltruetrue");
                    } else {
                        books.openBook(player, "besttooltruefalse");
                    }
                }

                // Step 5: Run logic for (/autoadd toggle "type")
            } else {
                String subCommand = args[1].toLowerCase();
                toggleAutoAdd(player, playerName, subCommand);

            }
            return true;

        } else {
            // error BT101
            player.sendMessage("Report this to SleazLee. Error BT101");
            return true;
        }

    }

    /**
     * Checks if the player has the specified permission.
     *
     * @param player     The player to check.
     * @param permission The permission node to check, e.g. "autoadd.use".
     * @return true if the player has the permission, false otherwise.
     */
    public static boolean hasPermission(Player player, String permission) {
        return player.hasPermission(permission);
    }

    public void toggleAutoAdd(Player player, String playerName, String subCommand) {
        if (subCommand.equals("refill")) {// Placeholder for "/autoadd toggle refill" logic
            if (player.hasPermission("autoadd.refill")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set autoadd.refill false");
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set autoadd.refill true");
            }
        } else {
            player.sendMessage("Unknown subcommand for toggle.");
        }
    }

}

