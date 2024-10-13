package at.sleazlee.bmessentials.rankup;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Manages rank-up functionalities and command execution.
 */
public class RankUpManager implements CommandExecutor {
    private final JavaPlugin plugin;
    public final Economy economy; // Made public for ConfigurationLoader access
    private final Permission permission;
    private final ConfigurationLoader configLoader;
    private final MessageHandler messageHandler;
    private final Map<String, Rank> ranks;

    /**
     * Initializes the RankUpManager with the provided plugin and economy instances.
     *
     * @param plugin  The main plugin instance.
     * @param economy The economy instance from the main class.
     */
    public RankUpManager(JavaPlugin plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.permission = setupPermissions();
        if (this.economy == null) {
            plugin.getLogger().severe("Economy plugin not found! Disabling RankUpManager.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            throw new IllegalStateException("Economy plugin not found!");
        }
        if (this.permission == null) {
            plugin.getLogger().severe("Permission plugin not found! Disabling RankUpManager.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            throw new IllegalStateException("Permission plugin not found!");
        }
        this.configLoader = new ConfigurationLoader(plugin, this.economy); // Pass economy if needed
        this.messageHandler = new MessageHandler();
        this.ranks = configLoader.loadRanks();
        plugin.getCommand("rankup").setExecutor(this);
        plugin.getLogger().info("Discovered " + ranks.size() + " ranks in the config!");
    }

    /**
     * Handles the /rankup command.
     *
     * @param sender  The command sender.
     * @param command The command being executed.
     * @param label   The command label.
     * @param args    Command arguments.
     * @return True if the command was handled, false otherwise.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Ensure the command is run by a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        // Check for necessary permission
        if (!player.hasPermission("rankup.use")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        // Retrieve current rank
        String currentRankKey = getCurrentRank(player);
        if (currentRankKey == null) {
            player.sendMessage(ChatColor.RED + "Your current rank could not be determined.");
            plugin.getLogger().warning("Player " + player.getName() + " has no primary group.");
            return true;
        }

        Rank currentRank = ranks.get(currentRankKey);
        if (currentRank == null) {
            player.sendMessage(ChatColor.RED + "Your current rank is invalid.");
            plugin.getLogger().warning("Invalid current rank for player " + player.getName() + ": " + currentRankKey);
            return true;
        }

        // Retrieve next rank
        String nextRankKey = currentRank.getNextRank();
        if (nextRankKey == null || nextRankKey.equalsIgnoreCase("none")) {
            player.sendMessage(ChatColor.RED + "You have reached the highest rank.");
            return true;
        }

        Rank nextRank = ranks.get(nextRankKey);
        if (nextRank == null) {
            player.sendMessage(ChatColor.RED + "The next rank is not configured properly.");
            plugin.getLogger().warning("Next rank not found in config: " + nextRankKey);
            return true;
        }

        // Check requirements
        List<String> unmetRequirements = new ArrayList<>();
        for (Requirement requirement : currentRank.getRequirements()) {
            if (!requirement.isMet(player)) {
                unmetRequirements.add(requirement.getDenyMessage());
            }
        }

        if (!unmetRequirements.isEmpty()) {
            // Send specific requirements' deny messages
            for (String denyMsg : unmetRequirements) {
                String formattedDenyMsg = messageHandler.formatMessage(denyMsg, player);
                player.sendMessage(formattedDenyMsg);
            }

            // Send the general rank deny message
            String rankDenyMsg = currentRank.getDenyMessage();
            if (rankDenyMsg != null && !rankDenyMsg.isEmpty()) {
                String formattedRankDenyMsg = messageHandler.formatMessage(rankDenyMsg, player);
                player.sendMessage(formattedRankDenyMsg);
            }

            return true;
        }

        // Perform rank up
        performRankUp(player, currentRank, nextRank);

        return true;
    }

    /**
     * Performs the rank-up process for the player.
     *
     * @param player      The player to rank up.
     * @param currentRank The player's current rank.
     * @param nextRank    The rank to assign to the player.
     */
    private void performRankUp(Player player, Rank currentRank, Rank nextRank) {
        // Deduct economy cost if applicable
        double cost = currentRank.getCost();
        if (cost > 0) {
            if (!economy.has(player, cost)) {
                player.sendMessage(ChatColor.RED + "You do not have enough money to rank up.");
                return;
            }
            boolean success = economy.withdrawPlayer(player, cost).transactionSuccess();
            if (!success) {
                player.sendMessage(ChatColor.RED + "Failed to deduct the required balance for rank up.");
                plugin.getLogger().warning("Economy withdrawal failed for player " + player.getName() + " for rank " + nextRank.getName());
                return;
            }
        }

        // Set the player's new rank
        setPlayerRank(player, currentRank.getName(), nextRank.getName());

        // Send personal message
        String personalMessage = currentRank.getPersonalMessage();
        if (personalMessage != null && !personalMessage.isEmpty()) {
            personalMessage = messageHandler.formatMessage(personalMessage, player);
            player.sendMessage(personalMessage);
        }

        // Broadcast message
        String broadcastMessage = currentRank.getBroadcastMessage();
        if (broadcastMessage != null && !broadcastMessage.isEmpty()) {
            broadcastMessage = messageHandler.formatMessage(broadcastMessage, player);
            plugin.getServer().broadcastMessage(broadcastMessage);
        }

        plugin.getLogger().info("Player " + player.getName() + " ranked up from " + currentRank.getName() + " to " + nextRank.getName());
    }

    /**
     * Sets the player's rank using the permission plugin.
     *
     * @param player      The player to set the rank for.
     * @param currentRank The player's current rank.
     * @param nextRank    The rank to set.
     */
    private void setPlayerRank(Player player, String currentRank, String nextRank) {
        permission.playerRemoveGroup(null, player, currentRank);
        permission.playerAddGroup(null, player, nextRank);
    }

    /**
     * Retrieves the player's current primary group using Vault permissions.
     *
     * @param player The player whose rank is to be retrieved.
     * @return The current rank key as a string, or null if unable to determine.
     */
    private String getCurrentRank(Player player) {
        return permission.getPrimaryGroup(player);
    }

    /**
     * Sets up the Permission service via Vault (LuckPerms in this case).
     *
     * @return The Permission instance, or null if not found.
     */
    private Permission setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = plugin.getServer().getServicesManager().getRegistration(Permission.class);
        if (rsp == null) {
            plugin.getLogger().severe("No Permission plugin found! Ensure Vault and LuckPerms are installed.");
            return null;
        }
        return rsp.getProvider();
    }
}