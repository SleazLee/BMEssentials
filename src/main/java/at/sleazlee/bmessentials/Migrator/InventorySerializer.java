package at.sleazlee.bmessentials.Migrator;

import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.YamlConfiguration;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * Provides methods to serialize and deserialize player inventories.
 */
public class InventorySerializer {

    /**
     * Serializes an array of ItemStacks into a Base64-encoded string.
     *
     * @param items the array of ItemStacks
     * @return the serialized string
     */
    public String serializeInventory(ItemStack[] items) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("items", items);
        String yamlString = config.saveToString();
        return Base64.getEncoder().encodeToString(yamlString.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Deserializes a Base64-encoded string into an array of ItemStacks.
     *
     * @param data the serialized string
     * @return the array of ItemStacks
     */
    public ItemStack[] deserializeInventory(String data) {
        String yamlString = new String(Base64.getDecoder().decode(data), StandardCharsets.UTF_8);
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yamlString);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<ItemStack> list = (List<ItemStack>) config.get("items");
        return list.toArray(new ItemStack[0]);
    }
}
