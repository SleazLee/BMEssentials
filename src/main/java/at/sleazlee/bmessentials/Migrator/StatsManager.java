package at.sleazlee.bmessentials.Migrator;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
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
                if (stat.getType() == Statistic.Type.UNTYPED) {
                    int value = player.getStatistic(stat);
                    config.set(stat.name(), value);
                } else if (stat.getType() == Statistic.Type.ENTITY) {
                    for (EntityType entity : EntityType.values()) {
                        if (entity.isAlive() && entity.isSpawnable()) {
                            int value = player.getStatistic(stat, entity);
                            if (value != 0) {
                                config.set(stat.name() + "." + entity.name(), value);
                            }
                        }
                    }
                } else if (stat.getType() == Statistic.Type.BLOCK || stat.getType() == Statistic.Type.ITEM) {
                    for (Material material : Material.values()) {
                        if (material.isItem() || material.isBlock()) {
                            int value = player.getStatistic(stat, material);
                            if (value != 0) {
                                config.set(stat.name() + "." + material.name(), value);
                            }
                        }
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid combinations
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

        for (String key : config.getKeys(true)) {
            try {
                if (key.contains(".")) {
                    String[] parts = key.split("\\.");
                    Statistic stat = Statistic.valueOf(parts[0]);
                    String typeName = parts[1];
                    int value = config.getInt(key);

                    if (stat.getType() == Statistic.Type.ENTITY) {
                        EntityType entity = EntityType.valueOf(typeName);
                        player.setStatistic(stat, entity, value);
                    } else if (stat.getType() == Statistic.Type.BLOCK || stat.getType() == Statistic.Type.ITEM) {
                        Material material = Material.valueOf(typeName);
                        player.setStatistic(stat, material, value);
                    }
                } else {
                    Statistic stat = Statistic.valueOf(key);
                    int value = config.getInt(key);
                    player.setStatistic(stat, value);
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid stats
            }
        }
    }
}