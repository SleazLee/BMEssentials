package at.sleazlee.bmessentials.Migrator.Commands;

import at.sleazlee.bmessentials.Migrator.DatabaseManager;
import at.sleazlee.bmessentials.Migrator.InventorySerializer;
import at.sleazlee.bmessentials.Migrator.MigratorManager;
import at.sleazlee.bmessentials.Migrator.StatsManager;
import at.sleazlee.bmessentials.Scheduler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;
import java.util.List;

/**
 * Handles the /migrateinventories admin command.
 */
public class MigrateInventoriesCommand implements CommandExecutor {

    private MigratorManager migratorManager;

    /**
     * Constructs the command executor.
     *
     * @param migratorManager the migrator manager instance
     */
    public MigrateInventoriesCommand(MigratorManager migratorManager) {
        this.migratorManager = migratorManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("inventorymigrator.admin")) {
            sender.sendMessage("You do not have permission to execute this command.");
            return true;
        }

        // Use the custom Scheduler
        Scheduler.run(() -> {

            File playerDataFolder = new File(migratorManager.getPlugin().getConfig().getString("playerdata-path"));
            if (!playerDataFolder.exists() || !playerDataFolder.isDirectory()) {
                sender.sendMessage("Player data folder not found.");
                return;
            }

            InventorySerializer serializer = migratorManager.getInventorySerializer();
            StatsManager statsManager = migratorManager.getStatsManager();
            DatabaseManager dbManager = migratorManager.getDatabaseManager();

            try (Connection conn = dbManager.getConnection()) {

                File[] files = playerDataFolder.listFiles((dir, name) -> name.endsWith(".dat"));
                if (files == null) {
                    sender.sendMessage("No player data files found.");
                    return;
                }

                for (File file : files) {
                    // Load player data
                    UUID playerUUID = UUID.fromString(file.getName().replace(".dat", ""));
                    YamlConfiguration dataConfig = YamlConfiguration.loadConfiguration(file);

                    // Serialize inventory
                    List<?> inventoryList = dataConfig.getList("Inventory");
                    ItemStack[] inventory = inventoryList.toArray(new ItemStack[0]);
                    String inventoryData = serializer.serializeInventory(inventory);

                    // Serialize ender chest
                    List<?> enderChestList = dataConfig.getList("EnderItems");
                    ItemStack[] enderChest = enderChestList.toArray(new ItemStack[0]);
                    String enderChestData = serializer.serializeInventory(enderChest);

                    // Insert into database
                    String insertSQL = "INSERT OR REPLACE INTO inventories (player_uuid, inventory_data, ender_chest_data) VALUES (?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(insertSQL)) {
                        ps.setString(1, playerUUID.toString());
                        ps.setString(2, inventoryData);
                        ps.setString(3, enderChestData);
                        ps.executeUpdate();
                    }

                    // Delete the player data file
                    file.delete();
                }

                sender.sendMessage("Migration completed successfully.");

            } catch (Exception e) {
                e.printStackTrace();
                sender.sendMessage("An error occurred during migration.");
            }
        });

        return true;
    }
}
