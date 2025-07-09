package at.sleazlee.bmessentials.Help.Abilities;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Help.HelpBooks;
import at.sleazlee.bmessentials.Scheduler;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BestToolCommand implements CommandExecutor {

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

        // Play a sound
        Location location = player.getLocation();
        player.getWorld().playSound(location, Sound.BLOCK_NOTE_BLOCK_BIT, 0.3f, 1f);


        // Step 1: Check if the player has the proper rank.
        if (!player.hasPermission("settings.besttool.allow")) {
            // Is not allowed.
            if (player.hasPermission("ranking.blockminer") || player.hasPermission("ranking.super")) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<#F62525><bold>BestTool</bold><red> You need to be at least rank <aqua><bold>[5]</bold><red> to use Chest Sorting!<white> Check out <click:run_command:'/ranks'><hover:show_text:'<aqua>Click to view your current rank chain!</aqua>'><aqua>/ranks</aqua></hover></click><white>."
                ));
                return true;
            } else if (player.hasPermission("ranking.ultra") || player.hasPermission("ranking.premium")) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<#F62525><bold>BestTool</bold><red> You need to be at least rank <aqua><bold>[6]</bold><red> to use Chest Sorting!<white> Check out <click:run_command:'/ranks'><hover:show_text:'<aqua>Click to view your current rank chain!</aqua>'><aqua>/ranks</aqua></hover></click><white>."
                ));
                return true;
            } else if (player.hasPermission("ranking.plus")) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<#F62525><bold>BestTool</bold><red> You need to be at least rank <aqua><bold>[7]</bold><red> to use Chest Sorting!<white> Check out <click:run_command:'/ranks'><hover:show_text:'<aqua>Click to view your current rank chain!</aqua>'><aqua>/ranks</aqua></hover></click><white>."
                ));
                return true;
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<#F62525><bold>BestTool</bold><red> You need to be at least rank <aqua><bold>[7]</bold><red> to use Chest Sorting!<white> Check out <click:run_command:'/ranks'><hover:show_text:'<aqua>Click to view your current rank chain!</aqua>'><aqua>/ranks</aqua></hover></click><white>."
                ));
                return true;
            }


            // Step 2: Run logic for No arguments (/besttool)
        } else if (args.length == 0) {

            if (!player.hasPermission("besttool.use") && !player.hasPermission("besttool.firstuse")) {

                // First time running command.

                // Dispatch commands from the console using Bukkit.dispatchCommand:
                Scheduler.run(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttool.firstuse true"));
                Scheduler.run(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttool.use true"));

                if (player.hasPermission("besttool.refill")) {
                    books.openBook(player, "besttooltruetrue");
                } else {
                    books.openBook(player, "besttooltruefalse");
                }
                return true;


            } else if (!player.hasPermission("besttool.use") && player.hasPermission("besttool.firstuse")) {

                if (player.hasPermission("besttool.refill")) {
                    books.openBook(player, "besttoolfalsetrue");
                } else {
                    books.openBook(player, "besttoolfalsefalse");
                }

                return true;

            } else if (player.hasPermission("besttool.use") && player.hasPermission("besttool.firstuse")) {

                if (player.hasPermission("besttool.refill")) {
                    books.openBook(player, "besttooltruetrue");
                } else {
                    books.openBook(player, "besttooltruefalse");
                }
                return true;

            } else {

                Scheduler.run(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttool.firstuse true"));
                Scheduler.run(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttool.use true"));

                if (player.hasPermission("besttool.refill")) {
                    books.openBook(player, "besttooltruetrue");
                } else {
                    books.openBook(player, "besttooltruefalse");
                }
                return true;

            }

            // Step 3: Run logic for (/besttool toggle <type>)
        } else if (args[0].equalsIgnoreCase("toggle")) {

            // Step 4: Run logic for only (/besttool toggle)
            if (args.length == 1) {

                if (player.hasPermission("besttool.use")) {
                    Scheduler.run(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttool.use false"));
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<gold><bold>BestTool </bold></gold><gray>You have toggled automatic sorting <color:#ff3300>off</color:#ff3300>!</gray>"));

                } else {
                    Scheduler.run(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttool.use true"));
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<gold><bold>BestTool </bold></gold><gray>You have toggled automatic sorting <green>on</green>!</gray>"));
                }

            } else if (args[1].equals("true")) {

                if (player.hasPermission("besttool.use")) {
                    Scheduler.run(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttool.use false"));
                    if (player.hasPermission("besttool.refill")) {
                        books.openBook(player, "besttoolfalsetrue");
                    } else {
                        books.openBook(player, "besttoolfalsefalse");
                    }

                } else {
                    Scheduler.run(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttool.use true"));
                    if (player.hasPermission("besttool.refill")) {
                        books.openBook(player, "besttooltruetrue");
                    } else {
                        books.openBook(player, "besttooltruefalse");
                    }
                }

                // Step 5: Run logic for (/besttool toggle "type")
            } if (args.length == 2) {

                if (args[1].equalsIgnoreCase("refill")) {// Placeholder for "/besttool toggle refill" logic
                    if (player.hasPermission("besttool.refill")) {
                        Scheduler.run(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttool.refill false"));
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<gold><bold>BestTool </bold></gold><gray>You have toggled Item Refilling <color:#ff3300>off</color:#ff3300>!</gray>"));
                    } else {
                        Scheduler.run(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttool.refill true"));
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<gold><bold>BestTool </bold></gold><gray>You have toggled Item Refilling <green>on</green>!</gray>"));
                    }
                }

            } else if (args[2].equals("true")) {

                if (player.hasPermission("besttool.refill")) {
                    Scheduler.run(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttool.refill false"));
                    if (player.hasPermission("besttool.use")) {
                        books.openBook(player, "besttooltruefalse");
                    } else {
                        books.openBook(player, "besttoolfalsefalse");
                    }

                } else {
                    Scheduler.run(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set besttool.refill true"));
                    if (player.hasPermission("besttool.use")) {
                        books.openBook(player, "besttooltruetrue");
                    } else {
                        books.openBook(player, "besttoolfalsetrue");
                    }
                }

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
     * @param permission The permission node to check, e.g. "besttool.use".
     * @return true if the player has the permission, false otherwise.
     */
    public static boolean hasPermission(Player player, String permission) {
        return player.hasPermission(permission);
    }

}