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

        // Play a sound
        Location location = player.getLocation();
        player.getWorld().playSound(location, Sound.BLOCK_NOTE_BLOCK_BIT, 0.3f, 1f);

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

            if (!player.hasPermission("drop2inventory.use") && !player.hasPermission("autoadd.firstuse")) {

                // First time running command.

                // Dispatch commands from the console using Bukkit.dispatchCommand:
                Scheduler.run(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set autoadd.firstuse true"));
                Scheduler.run(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set drop2inventory.use true"));

                if (player.hasPermission("drop2inventory.autocondense")) {
                    books.openBook(player, "autoaddtruetrue");
                } else {
                    books.openBook(player, "autoaddtruefalse");
                }

                return true;


            } else if (!player.hasPermission("drop2inventory.use") && player.hasPermission("autoadd.firstuse")) {

                if (player.hasPermission("drop2inventory.autocondense")) {
                    books.openBook(player, "autoaddfalsetrue");
                } else {
                    books.openBook(player, "autoaddfalsefalse");
                }

                return true;

            } else if (player.hasPermission("drop2inventory.use") && player.hasPermission("autoadd.firstuse")) {

                if (player.hasPermission("drop2inventory.autocondense")) {
                    books.openBook(player, "autoaddtruetrue");
                } else {
                    books.openBook(player, "autoaddtruefalse");
                }
                return true;

            } else {

                Scheduler.run(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set autoadd.firstuse true"));
                Scheduler.run(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set drop2inventory.use true"));

                if (player.hasPermission("drop2inventory.autocondense")) {
                    books.openBook(player, "autoaddtruetrue");
                } else {
                    books.openBook(player, "autoaddtruefalse");
                }

                return true;

            }

            // Step 3: Run logic for (/autoadd toggle <type>)
        } else if (args[0].equalsIgnoreCase("toggle")) {

            // Step 4: Run logic for only (/autoadd toggle)
            if (args.length == 1) {

                if (player.hasPermission("drop2inventory.use")) {
                    Scheduler.run(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set drop2inventory.use false"));
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<gold><bold>AutoAdd </bold></gold><gray>You have toggled automatic sorting <color:#ff3300>off</color:#ff3300>!</gray>"));

                } else {
                    Scheduler.run(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set drop2inventory.use true"));
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<gold><bold>AutoAdd </bold></gold><gray>You have toggled automatic sorting <green>on</green>!</gray>"));
                }

            } else if (args[1].equals("true")) {

                if (player.hasPermission("drop2inventory.use")) {
                    Scheduler.run(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set drop2inventory.use false"));
                    if (player.hasPermission("drop2inventory.autocondense")) {
                        books.openBook(player, "autoaddfalsetrue");
                    } else {
                        books.openBook(player, "autoaddfalsefalse");
                    }

                } else {
                    Scheduler.run(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set drop2inventory.use true"));
                    if (player.hasPermission("drop2inventory.autocondense")) {
                        books.openBook(player, "autoaddtruetrue");
                    } else {
                        books.openBook(player, "autoaddtruefalse");
                    }
                }

                // Step 5: Run logic for (/autoadd toggle "type")
            } if (args.length == 2) {

                if (args[1].equalsIgnoreCase("condense")) {// Placeholder for "/autoadd toggle condense" logic
                    if (player.hasPermission("drop2inventory.autocondense")) {
                        Scheduler.run(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set drop2inventory.autocondense false"));
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<gold><bold>AutoAdd </bold></gold><gray>You have toggled Auto Condense <color:#ff3300>off</color:#ff3300>!</gray>"));
                    } else {
                        Scheduler.run(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set drop2inventory.autocondense true"));
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<gold><bold>AutoAdd </bold></gold><gray>You have toggled Auto Condense <green>on</green>!</gray>"));
                    }
                }

            } else if (args[2].equals("true")) {

                if (player.hasPermission("drop2inventory.autocondense")) {
                    Scheduler.run(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set drop2inventory.autocondense false"));
                    if (player.hasPermission("drop2inventory.use")) {
                        books.openBook(player, "autoaddtruefalse");
                    } else {
                        books.openBook(player, "autoaddfalsefalse");
                    }

                } else {
                    Scheduler.run(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " permission set drop2inventory.autocondense true"));
                    if (player.hasPermission("drop2inventory.use")) {
                        books.openBook(player, "autoaddtruetrue");
                    } else {
                        books.openBook(player, "autoaddfalsetrue");
                    }
                }

            }
            return true;

        } else {
            // error AA101
            player.sendMessage("Report this to SleazLee. Error AA101");
            return true;
        }

    }

    /**
     * Checks if the player has the specified permission.
     *
     * @param player     The player to check.
     * @param permission The permission node to check, e.g. "drop2inventory.use".
     * @return true if the player has the permission, false otherwise.
     */
    public static boolean hasPermission(Player player, String permission) {
        return player.hasPermission(permission);
    }

}

