package at.sleazlee.bmessentials.Migrator;

import at.sleazlee.bmessentials.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Represents the virtual chest GUI for players to retrieve their migrated items.
 */
public class VirtualChestGUI implements InventoryHolder {

    private MigratorManager migratorManager;
    private Player player;
    private Inventory inventory;

    /**
     * Constructs the virtual chest GUI.
     *
     * @param migratorManager the migrator manager instance
     * @param player          the player
     */
    public VirtualChestGUI(MigratorManager migratorManager, Player player) {
        this.migratorManager = migratorManager;
        this.player = player;
    }

    /**
     * Loads the player's migrated inventory from the database.
     */
    private void loadInventory() {
        try (Connection conn = migratorManager.getDatabaseManager().getConnection()) {
            String query = "SELECT inventory_data, ender_chest_data FROM inventories WHERE player_uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, player.getUniqueId().toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String inventoryData = rs.getString("inventory_data");
                    String enderChestData = rs.getString("ender_chest_data");
                    ItemStack[] inventoryItems = migratorManager.getInventorySerializer().deserializeInventory(inventoryData);
                    ItemStack[] enderChestItems = migratorManager.getInventorySerializer().deserializeInventory(enderChestData);

                    // Combine inventory and ender chest items
                    ItemStack[] combinedItems = combineItems(inventoryItems, enderChestItems);

                    // Determine inventory size (multiple of 9, up to 54)
                    int size = ((combinedItems.length - 1) / 9 + 1) * 9;
                    if (size > 54) size = 54;

                    inventory = Bukkit.createInventory(this, size, "Migrated Inventory");
                    inventory.setContents(combinedItems);
                } else {
                    player.sendMessage("No migrated inventory found.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage("An error occurred while loading your migrated inventory.");
        }
    }

    /**
     * Combines two arrays of ItemStacks.
     *
     * @param inventoryItems  the inventory items
     * @param enderChestItems the ender chest items
     * @return the combined array
     */
    private ItemStack[] combineItems(ItemStack[] inventoryItems, ItemStack[] enderChestItems) {
        ItemStack[] combined = new ItemStack[inventoryItems.length + enderChestItems.length];
        System.arraycopy(inventoryItems, 0, combined, 0, inventoryItems.length);
        System.arraycopy(enderChestItems, 0, combined, inventoryItems.length, enderChestItems.length);
        return combined;
    }

    /**
     * Opens the virtual chest inventory for the player.
     */
    public void openInventory() {
        loadInventory();
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Saves the remaining items back to the database.
     */
    public void saveInventory() {
        try (Connection conn = migratorManager.getDatabaseManager().getConnection()) {
            String updateSQL = "UPDATE inventories SET inventory_data = ? WHERE player_uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateSQL)) {
                String inventoryData = migratorManager.getInventorySerializer().serializeInventory(inventory.getContents());
                ps.setString(1, inventoryData);
                ps.setString(2, player.getUniqueId().toString());
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage("An error occurred while saving your migrated inventory.");
        }
    }
}