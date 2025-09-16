package at.sleazlee.bmessentials.rankup;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

import net.milkbowl.vault2.economy.Economy;  // We still use VaultUnlocked for economy

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.math.BigDecimal;
import java.util.*;

import static org.bukkit.Bukkit.getServer;

/**
 * RankUpManager that uses LuckPerms directly for permission/group changes,
 * while still using VaultUnlocked for economy.
 */
public class RankUpManager implements CommandExecutor {
    private final JavaPlugin plugin;

    // VaultUnlocked Economy
    private final Economy economy;

    // LuckPerms API reference
    private LuckPerms luckPerms;

    private final ConfigurationLoader configLoader;
    private final MessageHandler messageHandler;
    private final Map<String, Rank> ranks;

    public RankUpManager(JavaPlugin plugin) {
        this.plugin = plugin;

        // SET UP ECONOMY via VaultUnlocked
        this.economy = setupVaultUnlockedEconomy();
        if (this.economy == null || !this.economy.isEnabled()) {
            plugin.getLogger().severe("VaultUnlocked Economy not found or not enabled! Disabling RankUpManager.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            throw new IllegalStateException("VaultUnlocked Economy not found!");
        }

        // SET UP LuckPerms (directly)
        if (!setupLuckPerms()) {
            plugin.getLogger().severe("LuckPerms not found or not loaded! Disabling RankUpManager.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            throw new IllegalStateException("LuckPerms not found!");
        }

        // Load rank configuration
        this.configLoader = new ConfigurationLoader(plugin, economy);
        this.messageHandler = new MessageHandler();
        this.ranks = configLoader.loadRanks();

        // Register /rankup command
        plugin.getCommand("rankup").setExecutor(this);

        getServer().getConsoleSender().sendMessage(
                ChatColor.GRAY + "    Discovered " +
                        ChatColor.DARK_GREEN + ranks.size() +
                        ChatColor.GRAY + " Ranks!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("bmessentials.rankup.use")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        String currentRankKey = getCurrentRank(player);
        if (currentRankKey == null) {
            player.sendMessage(ChatColor.RED + "Your current rank could not be determined via LuckPerms.");
            plugin.getLogger().warning("Player " + player.getName() + " has no primary group in LuckPerms?");
            return true;
        }

        Rank currentRank = ranks.get(currentRankKey);
        if (currentRank == null) {
            player.sendMessage(ChatColor.RED + "Your current rank is invalid.");
            plugin.getLogger().warning("Invalid current rank for " + player.getName() + ": " + currentRankKey);
            return true;
        }

        // Next rank
        String nextRankKey = currentRank.getNextRank();
        if (nextRankKey == null || nextRankKey.equalsIgnoreCase("none")) {
            player.sendMessage(ChatColor.RED + "You have reached the highest rank.");
            return true;
        }

        Rank nextRank = ranks.get(nextRankKey);
        if (nextRank == null) {
            player.sendMessage(ChatColor.RED + "The next rank is not configured properly.");
            plugin.getLogger().warning("Next rank not found: " + nextRankKey);
            return true;
        }

        // Check Requirements
        List<String> unmet = new ArrayList<>();
        for (Requirement req : currentRank.getRequirements()) {
            if (!req.isMet(player)) {
                unmet.add(req.getDenyMessage());
            }
        }
        if (!unmet.isEmpty()) {
            String deny = currentRank.getDenyMessage();
            if (deny != null && !deny.isEmpty()) {
                player.sendMessage(messageHandler.formatMessage(deny, player));
            }
            return true;
        }

        // Perform rank up
        performRankUp(player, currentRank, nextRank);
        return true;
    }

    /**
     * Executes the rank-up process, validating costs and updating the player's group.
     *
     * @param player      The player ranking up.
     * @param currentRank The player's current rank.
     * @param nextRank    The rank the player is moving to.
     */
    private void performRankUp(Player player, Rank currentRank, Rank nextRank) {
        // Calculate total cost from all EconomyRequirements
        double totalCost = 0.0;
        for (Requirement req : currentRank.getRequirements()) {
            if (req instanceof EconomyRequirement) {
                EconomyRequirement econReq = (EconomyRequirement) req;
                totalCost += econReq.getRequiredBalance();
            }
        }

        if (totalCost > 0) {
            // Check balance using VaultUnlocked's economy
            if (!economy.has(plugin.getName(), player.getUniqueId(), BigDecimal.valueOf(totalCost))) {
                player.sendMessage(ChatColor.RED + "You do not have enough money to rank up.");
                return;
            }
        }

        // Update LuckPerms rank
        setPlayerRankLuckPerms(player, currentRank.getName(), nextRank.getName());

        // Send messages
        sendRankUpMessages(player, currentRank);
    }

    /**
     * Sends personalized and broadcast messages after a successful rank-up.
     *
     * @param player      The player who ranked up.
     * @param currentRank The rank the player just left.
     */
    private void sendRankUpMessages(Player player, Rank currentRank) {
        String personalMsg = currentRank.getPersonalMessage();
        if (!personalMsg.isEmpty()) {
            player.sendMessage(messageHandler.formatMessage(personalMsg, player));
        }

        String broadcastMsg = currentRank.getBroadcastMessage();
        if (!broadcastMsg.isEmpty()) {
            String formatted = messageHandler.formatMessage(broadcastMsg, player);
            plugin.getServer().broadcastMessage(formatted);
        }
    }

    /**
     * Use LuckPerms to get the player's current primary group (rank).
     */
    private String getCurrentRank(Player player) {
        // Load or get the LuckPerms user
        User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
        // Primary group typically indicates the rank name in LuckPerms
        return user.getPrimaryGroup();
    }

    /**
     * Use LuckPerms to set the player's new primary group (rank).
     */
    private void setPlayerRankLuckPerms(Player player, String oldRank, String newRank) {
        User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);

        // 1) Check if the "newRank" group exists
        var group = luckPerms.getGroupManager().getGroup(newRank);
        if (group == null) {
            plugin.getLogger().warning("LuckPerms group does not exist: " + newRank);
            player.sendMessage(ChatColor.RED + "Cannot rank up; group '" + newRank + "' does not exist in LuckPerms.");
            return;
        }

        // 2) Add the user to that group if they're not already
        //    We'll create an InheritanceNode for that group.
        var node = net.luckperms.api.node.types.InheritanceNode.builder(group).build();
        user.data().add(node);

        // 3) Now we can set the user's primary group
        user.setPrimaryGroup(newRank);

        // 4) Save changes back to LuckPerms
        luckPerms.getUserManager().saveUser(user);

        plugin.getLogger().info("Set primary group for " + player.getName() + " to " + newRank);
    }


    /**
     * Grabs the LuckPerms API, returns false if not available.
     */
    private boolean setupLuckPerms() {
        try {
            this.luckPerms = LuckPermsProvider.get();
            return true;
        } catch (IllegalStateException e) {
            // Thrown if LuckPerms isn't loaded or hooking is done too early
            plugin.getLogger().severe("LuckPermsProvider not available: " + e.getMessage());
            return false;
        }
    }

    /**
     * Finds a VaultUnlocked economy from ServiceManager or returns null if not found.
     */
    private Economy setupVaultUnlockedEconomy() {
        RegisteredServiceProvider<Economy> rsp =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().severe("No VaultUnlocked economy provider found!");
            return null;
        }
        return rsp.getProvider();
    }
}
