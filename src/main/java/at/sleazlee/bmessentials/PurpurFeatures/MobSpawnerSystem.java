package at.sleazlee.bmessentials.PurpurFeatures;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class MobSpawnerSystem implements Listener {

    // Key used to store the mob type in the spawner item’s PersistentDataContainer
    private final NamespacedKey spawnerKey;

    public MobSpawnerSystem(JavaPlugin plugin) {
        this.spawnerKey = new NamespacedKey(plugin, "spawner-type");
    }

    // Handle breaking spawners with a silk touch enchanted pickaxe.
    @EventHandler
    public void onSpawnerBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.SPAWNER) {
            return;
        }
        // Check if the player is using a tool with Silk Touch
        if (event.getPlayer().getInventory().getItemInMainHand().containsEnchantment(Enchantment.SILK_TOUCH)) {
            event.setDropItems(false); // Prevent normal drops

            CreatureSpawner spawner = (CreatureSpawner) block.getState();
            EntityType spawnedType = spawner.getSpawnedType();

            // Remove the spawner block
            block.setType(Material.AIR);

            // Create a new spawner item with custom data
            ItemStack spawnerItem = new ItemStack(Material.SPAWNER, 1);
            ItemMeta meta = spawnerItem.getItemMeta();
            meta.setDisplayName("Spawner: " + spawnedType.name());
            // Store the mob type using the persistent data container
            meta.getPersistentDataContainer().set(spawnerKey, PersistentDataType.STRING, spawnedType.name());
            spawnerItem.setItemMeta(meta);

            // Drop the custom spawner item in the world
            block.getWorld().dropItemNaturally(block.getLocation(), spawnerItem);
        }
    }

    // Handle placing spawner items, restoring the mob type.
    @EventHandler
    public void onSpawnerPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType() != Material.SPAWNER) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Retrieve the stored mob type from the item
        String mobTypeName = meta.getPersistentDataContainer().get(spawnerKey, PersistentDataType.STRING);
        if (mobTypeName == null) return;

        Block block = event.getBlockPlaced();
        if (block.getType() == Material.SPAWNER) {
            CreatureSpawner spawner = (CreatureSpawner) block.getState();
            try {
                EntityType type = EntityType.valueOf(mobTypeName);
                spawner.setSpawnedType(type);
                spawner.update();
            } catch (IllegalArgumentException e) {
                // Log the error or inform the player if needed.
                event.getPlayer().sendMessage("Error: Invalid mob type stored in spawner item: " + mobTypeName);
            }
        }
    }
}
