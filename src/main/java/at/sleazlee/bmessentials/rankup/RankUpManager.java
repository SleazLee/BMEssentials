package at.sleazlee.bmessentials.rankup;

import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

/**
 * Manages rank-up functionalities and command execution.
 */
public class RankUpManager implements CommandExecutor {
    private JavaPlugin plugin;
    private Economy economy;
    private FileConfiguration ranksConfig;
    private File ranksFile;

    public RankUpManager(JavaPlugin plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
        initializeConfig();
        plugin.getCommand("rankup").setExecutor(this);
    }

    private void initializeConfig() {
        ranksFile = new File(plugin.getDataFolder(), "ranks.yml");
        if (!ranksFile.exists()) {
            ranksFile.getParentFile().mkdirs();
            plugin.saveResource("ranks.yml", false);
        }
        ranksConfig = YamlConfiguration.loadConfiguration(ranksFile);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("rankup.use")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        String currentRank = getCurrentRank(player);
        if (currentRank == null) {
            player.sendMessage(ChatColor.RED + "Your current rank could not be determined.");
            return true;
        }

        String nextRank = ranksConfig.getString("ranks." + currentRank + ".next_rank");
        if (nextRank == null) {
            player.sendMessage(ChatColor.RED + "You have reached the highest rank.");
            return true;
        }

        double cost = ranksConfig.getDouble("ranks." + currentRank + ".requirements.balance");
        int requiredPowerLevel = ranksConfig.getInt("ranks." + currentRank + ".requirements.mcmmo_power_level");

        if (!checkEconomyCondition(player, cost) || !checkMcMMOCondition(player, requiredPowerLevel)) {
            String denyMessage = ranksConfig.getString("ranks." + currentRank + ".messages.deny");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', denyMessage));
            return true;
        }

        economy.withdrawPlayer(player, cost);
        setPlayerRank(player, nextRank);
        String personalMessage = ranksConfig.getString("ranks." + currentRank + ".messages.personal");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', personalMessage));

        String broadcastMessage = ranksConfig.getString("ranks." + currentRank + ".messages.broadcast").replace("%player%", player.getName());
        plugin.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', broadcastMessage));

        return true;
    }

    private String getCurrentRank(Player player) {
        Permission perms = getPermissions();
        if (perms == null) {
            plugin.getLogger().severe("Permissions system is not available!");
            return null;
        }
        return perms.getPrimaryGroup(player);
    }

    private boolean checkEconomyCondition(Player player, double cost) {
        return economy.has(player, cost);
    }

    private boolean checkMcMMOCondition(Player player, int requiredLevel) {
        String powerLevelStr = PlaceholderAPI.setPlaceholders(player, "%mcmmo_power_level%");
        try {
            int powerLevel = Integer.parseInt(powerLevelStr);
            return powerLevel >= requiredLevel;
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Could not parse mcMMO power level: " + powerLevelStr);
            return false;
        }
    }

    private void setPlayerRank(Player player, String nextRank) {
        Permission perms = getPermissions();
        if (perms == null) {
            plugin.getLogger().severe("Permissions system is not available!");
            return;
        }
        String currentRank = getCurrentRank(player);
        if (currentRank != null && !currentRank.isEmpty()) {
            perms.playerRemoveGroup(null, player, currentRank);
        }
        perms.playerAddGroup(null, player, nextRank);
    }

    private Permission getPermissions() {
        RegisteredServiceProvider<Permission> rsp = plugin.getServer().getServicesManager().getRegistration(Permission.class);
        if (rsp == null) {
            return null;
        }
        return rsp.getProvider();
    }
}
