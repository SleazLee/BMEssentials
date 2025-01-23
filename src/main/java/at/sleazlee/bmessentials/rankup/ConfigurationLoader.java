package at.sleazlee.bmessentials.rankup;

import net.milkbowl.vault2.economy.Economy; // <-- VaultUnlocked import
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Handles loading and parsing the ranks configuration using VaultUnlocked.
 */
public class ConfigurationLoader {
    private final JavaPlugin plugin;
    private final Economy economy; // This is now VaultUnlocked's Economy
    private final File configFile;
    private FileConfiguration config;

    /**
     * Constructs a ConfigurationLoader and loads the configuration.
     *
     * @param plugin  The main plugin instance.
     * @param economy The VaultUnlocked Economy instance.
     */
    public ConfigurationLoader(JavaPlugin plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;  // net.milkbowl.vault2.economy.Economy
        this.configFile = new File(plugin.getDataFolder(), "ranks.yml");
        loadConfig();
    }

    /**
     * Loads the configuration file, saving the default if it doesn't exist.
     */
    private void loadConfig() {
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource("ranks.yml", false);
        }
        config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
        try {
            config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().severe("Failed to load ranks.yml: " + e.getMessage());
        }
    }

    /**
     * Loads and parses all ranks from the configuration.
     *
     * @return A map of rank keys to Rank objects.
     */
    public Map<String, Rank> loadRanks() {
        Map<String, Rank> ranks = new HashMap<>();
        if (config.isConfigurationSection("ranks")) {
            ConfigurationSection ranksSection = config.getConfigurationSection("ranks");
            for (String rankChainKey : ranksSection.getKeys(false)) {
                ConfigurationSection rankChainSection = ranksSection.getConfigurationSection(rankChainKey);
                if (rankChainSection == null) continue;

                for (String rankKey : rankChainSection.getKeys(false)) {
                    Rank rank = parseRank(rankKey, rankChainSection);
                    if (rank != null) {
                        ranks.put(rank.getName(), rank);
                    }
                }
            }
        } else {
            plugin.getLogger().severe("No 'ranks' section found in ranks.yml.");
        }
        return ranks;
    }


    /**
     * Parses a rank configuration section into a Rank object.
     *
     * @param rankKey        The key of the rank in the configuration.
     * @param ranksSection   The ConfigurationSection containing rank definitions.
     * @return A configured Rank object, or null if parsing fails.
     */
    private Rank parseRank(String rankKey, ConfigurationSection ranksSection) {
        ConfigurationSection rankSection = ranksSection.getConfigurationSection(rankKey);
        if (rankSection == null) {
            plugin.getLogger().warning("Invalid rank section: " + rankKey);
            return null;
        }

        String name = rankSection.getString("name", rankKey);
        String nextRank = rankSection.getString("next_rank", null);

        // Load requirements
        List<Requirement> requirements = new ArrayList<>();
        ConfigurationSection requirementsSection = rankSection.getConfigurationSection("requirements");
        if (requirementsSection != null) {
            // Economy requirement
            double balanceReq = requirementsSection.getDouble("balance", 0);
            if (balanceReq > 0) {
                requirements.add(new EconomyRequirement(
                        plugin.getName(),
                        balanceReq,
                        economy
                ));
            }

            // MCMMO Power Level Requirement
            int mcmmoPowerLevel = requirementsSection.getInt("mcmmo_power_level", 0);
            if (mcmmoPowerLevel > 0) {
                requirements.add(new McMMORequirement(mcmmoPowerLevel));
            }

            // Playtime Requirement
            String playtimeStr = requirementsSection.getString("playtime", null);
            if (playtimeStr != null && !playtimeStr.isEmpty()) {
                requirements.add(new PlaytimeRequirement(plugin, playtimeStr));
            }
        }

        // Load messages
        ConfigurationSection messagesSection = rankSection.getConfigurationSection("messages");
        String personalMsg = messagesSection != null ? messagesSection.getString("personal", "") : "";
        String broadcastMsg = messagesSection != null ? messagesSection.getString("broadcast", "") : "";
        String denyMsg = messagesSection != null ? messagesSection.getString("deny", "") : "";

        return new Rank(name, nextRank, requirements, personalMsg, broadcastMsg, denyMsg);
    }

}