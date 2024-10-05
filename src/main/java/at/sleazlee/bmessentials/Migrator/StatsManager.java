package at.sleazlee.bmessentials.Migrator;

import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Manages the serialization and application of player statistics.
 */
public class StatsManager {

    /**
     * Serializes a player's statistics into a Base64-encoded string.
     *
     * @param player the player
     * @return the serialized statistics
     */
    public String serializeStats(Player player) {
        YamlConfiguration config = new YamlConfiguration();
        for (Statistic stat : Statistic.values()) {
            try {
                int value = player.getStatistic(stat);
                config.set(stat.name(), value);
            } catch (Exception ignored) {
                // Ignore stats that require additional parameters
            }
        }
        String yamlString = config.saveToString();
        return Base64.getEncoder().encodeToString(yamlString.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Applies serialized statistics to a player.
     *
     * @param player the player
     * @param data   the serialized statistics
     */
    public void applyStats(Player player, String data) {
        String yamlString = new String(Base64.getDecoder().decode(data), StandardCharsets.UTF_8);
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yamlString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (String key : config.getKeys(false)) {
            Statistic stat = Statistic.valueOf(key);
            int value = config.getInt(key);
            try {
                player.setStatistic(stat, value);
            } catch (Exception ignored) {
                // Ignore stats that require additional parameters
            }
        }
    }
}