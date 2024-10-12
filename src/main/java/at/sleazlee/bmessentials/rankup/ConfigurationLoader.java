package at.sleazlee.bmessentials.rankup;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Handles loading and parsing the ranks configuration.
 */
public class ConfigurationLoader {
    private final JavaPlugin plugin;
    private final Economy economy;
    private final File configFile;
    private FileConfiguration config;

    /**
     * Constructs a ConfigurationLoader and loads the configuration.
     *
     * @param plugin  The main plugin instance.
     * @param economy The Economy instance from Vault.
     */
    public ConfigurationLoader(JavaPlugin plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
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
     * Parses a single rank from the configuration.
     *
     * @param rankKey      The key of the rank to parse.
     * @param ranksSection The configuration section containing ranks.
     * @return A Rank object, or null if parsing failed.
     */
    private Rank parseRank(String rankKey, ConfigurationSection ranksSection) {
        ConfigurationSection rankSection = ranksSection.getConfigurationSection(rankKey);
        if (rankSection == null) {
            plugin.getLogger().warning("Rank section not found for key: " + rankKey);
            return null;
        }

        String name = rankSection.getString("name", rankKey);
        String nextRank = rankSection.getString("next_rank", null);
        double cost = rankSection.getDouble("cost", 0);

        // Requirements
        List<Requirement> requirements = new ArrayList<>();
        ConfigurationSection requirementsSection = rankSection.getConfigurationSection("requirements");
        if (requirementsSection != null) {
            // MCMMO Power Level Requirement
            int mcmmoPowerLevel = requirementsSection.getInt("mcmmo_power_level", 0);
            if (mcmmoPowerLevel > 0) {
                requirements.add(new McMMORequirement(mcmmoPowerLevel));
            }

            // Economy Balance Requirement
            double balanceRequirement = requirementsSection.getDouble("balance", 0);
            if (balanceRequirement > 0) {
                requirements.add(new EconomyRequirement(balanceRequirement, economy));
            }

            // Playtime Requirement
            String playtimeStr = requirementsSection.getString("playtime", null);
            if (playtimeStr != null && !playtimeStr.isEmpty()) {
                requirements.add(new PlaytimeRequirement(plugin, playtimeStr));
            }

            // Add additional requirements as needed
        }

        // Messages
        ConfigurationSection messagesSection = rankSection.getConfigurationSection("messages");
        String personalMessage = "";
        String broadcastMessage = "";
        String denyMessage = "";
        if (messagesSection != null) {
            personalMessage = messagesSection.getString("personal", "");
            broadcastMessage = messagesSection.getString("broadcast", "");
            denyMessage = messagesSection.getString("deny", "");
        }

        // Create Rank object
        Rank rank = new Rank(
                name,
                nextRank,
                cost,
                requirements,
                personalMessage,
                broadcastMessage,
                denyMessage
        );

        return rank;
    }
}