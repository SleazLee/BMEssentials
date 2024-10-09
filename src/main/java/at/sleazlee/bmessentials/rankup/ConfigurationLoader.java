package at.sleazlee.bmessentials.rankup;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Handles loading and parsing the ranks configuration.
 */
public class ConfigurationLoader {
    private final JavaPlugin plugin;
    private final FileConfiguration config;

    /**
     * Constructs a ConfigurationLoader and loads the configuration.
     *
     * @param plugin The main plugin instance.
     */
    public ConfigurationLoader(JavaPlugin plugin) {
        this.plugin = plugin;
        this.plugin.saveDefaultConfig(); // Ensure default config is saved
        this.config = plugin.getConfig();
    }

    /**
     * Loads all ranks from the configuration.
     *
     * @return A map of rank keys to Rank objects.
     */
    public Map<String, Rank> loadRanks() {
        Map<String, Rank> ranks = new HashMap<>();
        if (config.isConfigurationSection("ranks")) {
            ConfigurationSection ranksSection = config.getConfigurationSection("ranks");
            for (String rankKey : ranksSection.getKeys(false)) {
                Rank rank = parseRank(rankKey, ranksSection);
                if (rank != null) {
                    ranks.put(rank.getName(), rank);
                }
            }
        } else {
            plugin.getLogger().severe("No 'ranks' section found in config.");
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

        String nextRank = rankSection.getString("next_rank", "none");
        double balance = rankSection.getDouble("requirements.balance", 0);
        int mcmmoPowerLevel = rankSection.getInt("requirements.mcmmo_power_level", 0);
        String playtime = rankSection.getString("requirements.playtime", "");

        String personalMessage = rankSection.getString("messages.personal", "");
        String broadcastMessage = rankSection.getString("messages.broadcast", "");
        String denyMessage = rankSection.getString("messages.deny", "");

        List<Requirement> requirements = new ArrayList<>();

        // Playtime Requirement
        if (!playtime.isEmpty()) {
            requirements.add(new PlaytimeRequirement(playtime));
        }

        // Economy Requirement
        if (balance > 0) {
            requirements.add(new EconomyRequirement(balance, getEconomy()));
        }

        // MCMMO Requirement
        if (mcmmoPowerLevel > 0) {
            requirements.add(new McMMORequirement(mcmmoPowerLevel));
        }

        return new Rank(rankKey, nextRank, requirements, personalMessage, broadcastMessage, denyMessage);
    }

    /**
     * Retrieves the Economy instance from the main plugin's RankUpManager.
     *
     * @return The Economy instance.
     */
    private Economy getEconomy() {
        return ((RankUpManager) plugin.getPlugin(RankUpManager.class)).economy;
    }
}
