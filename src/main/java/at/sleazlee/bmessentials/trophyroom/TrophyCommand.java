package at.sleazlee.bmessentials.trophyroom;

import at.sleazlee.bmessentials.BMEssentials;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * The TrophyCommand class handles the /trophy command, allowing players to manage their trophy rooms.
 */
public class TrophyCommand implements CommandExecutor, TabCompleter {

    private final BMEssentials plugin;
    private final TrophyDatabase database;
    private final TrophyMenu menu;

    private final NamespacedKey trophyKey;

    /**
     * Constructor for the TrophyCommand class.
     *
     * @param plugin   The JavaPlugin instance.
     * @param database The Database instance.
     * @param menu     The Menu instance.
     */
    public TrophyCommand(BMEssentials plugin, TrophyDatabase database, TrophyMenu menu) {
        this.plugin = plugin;
        this.database = database;
        this.menu = menu;
        this.trophyKey = new NamespacedKey(plugin, "trophy_item");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can execute this command.");
            return true;
        }

        Player player = (Player) sender;

        // Check if the player has admin permission
        boolean hasAdminPerm = player.hasPermission("trophyroom.admin");

        // No arguments: Open player's own trophy room
        if (args.length == 0) {
            menu.openMenu(player, player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // Handle admin commands
        if (hasAdminPerm && (subCommand.equals("create") || subCommand.equals("get"))) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /trophy " + subCommand + " <name>");
                return true;
            }
            String trophyName = args[1];

            if (subCommand.equals("create")) {
                createTrophy(player, trophyName);
            } else if (subCommand.equals("get")) {
                getTrophy(player, trophyName);
            }
            return true;
        }

        // If the player does not have admin permission and tried to use admin commands
        if (!hasAdminPerm && (subCommand.equals("create") || subCommand.equals("get"))) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        // Attempt to open a player's trophy room (online or offline)
        String targetName = args[0];
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);

        // Check if the player has joined the server before
        if (targetPlayer.hasPlayedBefore() || targetPlayer.isOnline()) {
            menu.openMenu(player, targetPlayer);
            return true;
        } else {
            player.sendMessage(ChatColor.RED + "Player '" + targetName + "' not found.");
            return true;
        }
    }



    /**
     * Creates a trophy from the item in the player's main hand.
     *
     * @param player     The player executing the command.
     * @param trophyName The name of the trophy.
     */
    private void createTrophy(Player player, String trophyName) {
        // Get the item in the player's main hand
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand == null || itemInHand.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "You must hold an item in your main hand to create a trophy.");
            return;
        }

        // Add the custom NBT tag to the item
        ItemMeta meta = itemInHand.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(trophyKey, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            itemInHand.setItemMeta(meta);
        }

        // Serialize the item and store it in the database
        try {
            String serializedItem = ItemBuilder.itemStackToBase64(itemInHand);
            database.addTrophy(trophyName, serializedItem);
            player.sendMessage(ChatColor.GREEN + "Trophy '" + trophyName + "' has been created.");
        } catch (IOException e) {
            player.sendMessage(ChatColor.RED + "An error occurred while creating the trophy.");
            plugin.getLogger().log(Level.SEVERE, "Error serializing trophy item", e);
        }
    }

    /**
     * Gives the player a copy of the specified trophy.
     *
     * @param player     The player executing the command.
     * @param trophyName The name of the trophy.
     */
    private void getTrophy(Player player, String trophyName) {
        String serializedItem = database.getItem(trophyName);
        if (serializedItem == null) {
            player.sendMessage(ChatColor.RED + "Trophy '" + trophyName + "' does not exist.");
            return;
        }

        try {
            ItemStack item = ItemBuilder.itemStackFromBase64(serializedItem);
            player.getInventory().addItem(item);
            player.sendMessage(ChatColor.GREEN + "You have received the trophy '" + trophyName + "'.");
        } catch (IOException | ClassNotFoundException e) {
            player.sendMessage(ChatColor.RED + "An error occurred while retrieving the trophy.");
            plugin.getLogger().log(Level.SEVERE, "Error deserializing trophy item", e);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return completions;
        }

        Player player = (Player) sender;
        boolean isOp = player.isOp();

        if (args.length == 1) {
            String input = args[0].toLowerCase();

            if (isOp) {
                // OP can tab-complete "create" and "get"
                if ("create".startsWith(input)) {
                    completions.add("create");
                }
                if ("get".startsWith(input)) {
                    completions.add("get");
                }
            }

            // Add online player names that start with the input
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getName().toLowerCase().startsWith(input)) {
                    completions.add(onlinePlayer.getName());
                }
            }

            // If input is empty, add all options
            if (input.isEmpty()) {
                if (isOp) {
                    completions.add("create");
                    completions.add("get");
                }
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    completions.add(onlinePlayer.getName());
                }
            }
        } else if (args.length == 2 && isOp && (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("get"))) {
            // Tab-complete trophy names from the database
            String input = args[1].toLowerCase();
            List<String> trophyNames = database.getAllTrophyNames();

            for (String name : trophyNames) {
                if (name.toLowerCase().startsWith(input)) {
                    completions.add(name);
                }
            }
        }

        return completions;
    }
}
