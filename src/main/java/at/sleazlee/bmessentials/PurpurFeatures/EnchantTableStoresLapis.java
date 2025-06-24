package at.sleazlee.bmessentials.PurpurFeatures;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Allows Enchanting Tables to store lapis lazuli placed inside them.
 * When the GUI is closed, any lapis in the table is saved into the block's
 * persistent data container and restored the next time it is opened.
 * Breaking the table will drop any stored lapis.
 */
public class EnchantTableStoresLapis implements Listener {

    private final NamespacedKey lapisKey;

    public EnchantTableStoresLapis(JavaPlugin plugin) {
        this.lapisKey = new NamespacedKey(plugin, "stored-lapis");
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getView().getType() != InventoryType.ENCHANTING) {
            return;
        }
        Inventory inv = event.getInventory();
        Location loc = inv.getLocation();
        if (loc == null) {
            return; // opened via command or not tied to a block
        }
        Block block = loc.getBlock();
        if (!(block.getState() instanceof TileState state)) {
            return;
        }
        PersistentDataContainer container = state.getPersistentDataContainer();
        Integer amount = container.get(lapisKey, PersistentDataType.INTEGER);
        if (amount != null && amount > 0) {
            ItemStack existing = inv.getItem(1);
            if (existing == null || existing.getType() == Material.AIR) {
                inv.setItem(1, new ItemStack(Material.LAPIS_LAZULI, amount));
            } else if (existing.getType() == Material.LAPIS_LAZULI) {
                existing.setAmount(existing.getAmount() + amount);
            } else {
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.LAPIS_LAZULI, amount));
            }
            container.remove(lapisKey);
            state.update();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getType() != InventoryType.ENCHANTING) {
            return;
        }
        Inventory inv = event.getInventory();
        Location loc = inv.getLocation();
        if (loc == null) {
            return;
        }
        ItemStack lapis = inv.getItem(1);
        int amount = (lapis != null && lapis.getType() == Material.LAPIS_LAZULI) ? lapis.getAmount() : 0;
        if (amount > 0) {
            inv.setItem(1, null); // prevent drops on close
        }
        Block block = loc.getBlock();
        if (block.getState() instanceof TileState state) {
            state.getPersistentDataContainer().set(lapisKey, PersistentDataType.INTEGER, amount);
            state.update();
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.ENCHANTING_TABLE) {
            return;
        }
        if (block.getState() instanceof TileState state) {
            PersistentDataContainer container = state.getPersistentDataContainer();
            Integer amount = container.get(lapisKey, PersistentDataType.INTEGER);
            if (amount != null && amount > 0) {
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.LAPIS_LAZULI, amount));
                container.remove(lapisKey);
                state.update();
            }
        }
    }
}