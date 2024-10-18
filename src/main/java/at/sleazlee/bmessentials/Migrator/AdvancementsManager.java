package at.sleazlee.bmessentials.Migrator;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.configuration.file.YamlConfiguration;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;

/**
 * Manages the serialization and application of player advancements.
 */
public class AdvancementsManager {

    /**
     * Serializes a player's advancements into a Base64-encoded string.
     *
     * @param player the player
     * @return the serialized advancements
     */
    public String serializeAdvancements(Player player) {
        YamlConfiguration config = new YamlConfiguration();
        Iterator<Advancement> advancements = Bukkit.advancementIterator();

        while (advancements.hasNext()) {
            Advancement advancement = advancements.next();
            AdvancementProgress progress = player.getAdvancementProgress(advancement);
            if (progress.isDone()) {
                config.set(advancement.getKey().toString(), true);
            }
        }
        String yamlString = config.saveToString();
        return Base64.getEncoder().encodeToString(yamlString.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Applies serialized advancements to a player.
     *
     * @param player the player
     * @param data   the serialized advancements
     */
    public void applyAdvancements(Player player, String data) {
        String yamlString = new String(Base64.getDecoder().decode(data), StandardCharsets.UTF_8);
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yamlString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (String key : config.getKeys(false)) {
            if (config.getBoolean(key)) {
                NamespacedKey namespacedKey = NamespacedKey.fromString(key);
                Advancement advancement = Bukkit.getAdvancement(namespacedKey);
                if (advancement != null) {
                    AdvancementProgress progress = player.getAdvancementProgress(advancement);
                    for (String criteria : progress.getRemainingCriteria()) {
                        progress.awardCriteria(criteria);
                    }
                }
            }
        }
    }
}