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
        this.inventory = Bukkit.createInventory(this, 54, "Migrated Inventory");

        loadInventory();
    }

    /**
     * Loads the player's migrated inventory from the database.
     */
    private void loadInventory() {
        Scheduler.run(() -> {
            try (Connection conn = migratorManager.getDatabaseManager().getConnection()) {
                String query = "SELECT inventory_data FROM inventories WHERE player_uuid = ?";
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setString(1, player.getUniqueId().toString());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        String inventoryData = rs.getString("inventory_data");
                        ItemStack[] items = migratorManager.getInventorySerializer().deserializeInventory(inventoryData);
                        Scheduler.run(() -> inventory.setContents(items));
                    } else {
                        Scheduler.run(() -> player.sendMessage("No migrated inventory found."));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Scheduler.run(() -> player.sendMessage("An error occurred while loading your migrated inventory."));
            }
        });
    }

    /**
     * Opens the virtual chest inventory for the player.
     */
    public void openInventory() {
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
        Scheduler.run(() -> {
            // Save the remaining items back to the database
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
                Scheduler.run(() -> player.sendMessage("An error occurred while saving your migrated inventory."));
            }
        });
    }
}
