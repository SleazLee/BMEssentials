package at.sleazlee.bmessentials.Migrator.Commands;

import at.sleazlee.bmessentials.Migrator.DatabaseManager;
import at.sleazlee.bmessentials.Migrator.InventorySerializer;
import at.sleazlee.bmessentials.Migrator.MigratorManager;
import de.tr7zw.nbtapi.*;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

        String playerDataPath = migratorManager.getPlugin().getConfig().getString("playerdata-path");
        File playerDataFolder = new File(playerDataPath);
        if (!playerDataFolder.exists() || !playerDataFolder.isDirectory()) {
            sender.sendMessage("Player data folder not found.");
            return true;
        }

        InventorySerializer serializer = migratorManager.getInventorySerializer();
        DatabaseManager dbManager = migratorManager.getDatabaseManager();

        try (Connection conn = dbManager.getConnection()) {

            File[] files = playerDataFolder.listFiles((dir, name) -> name.endsWith(".dat"));
            if (files == null || files.length == 0) {
                sender.sendMessage("No player data files found.");
                return true;
            }

            for (File file : files) {
                try {
                    // Load player data
                    UUID playerUUID = UUID.fromString(file.getName().replace(".dat", ""));
                    NBTFile nbtFile = new NBTFile(file);

                    // Read inventory
                    NBTCompoundList inventoryList = nbtFile.getCompoundList("Inventory");
                    ItemStack[] inventory = convertNBTTagListToItemStackArray(inventoryList);

                    // Read ender chest
                    NBTCompoundList enderChestList = nbtFile.getCompoundList("EnderItems");
                    ItemStack[] enderChest = convertNBTTagListToItemStackArray(enderChestList);

                    // Serialize inventories
                    String inventoryData = serializer.serializeInventory(inventory);
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
                } catch (Exception e) {
                    e.printStackTrace();
                    sender.sendMessage("An error occurred while processing player file: " + file.getName());
                }
            }

            sender.sendMessage("Migration completed successfully.");

        } catch (Exception e) {
            e.printStackTrace();
            sender.sendMessage("An error occurred during migration.");
        }

        return true;
    }

    /**
     * Converts an NBTCompoundList to an array of ItemStacks.
     *
     * @param nbtList the NBTCompoundList containing the item data
     * @return an array of ItemStacks
     */
    private ItemStack[] convertNBTTagListToItemStackArray(NBTCompoundList nbtList) {
        List<ItemStack> items = new ArrayList<>();
        if (nbtList != null) {
            for (NBTListCompound itemCompound : nbtList) {
                try {
                    // Get item data
                    String id = itemCompound.getString("id");
                    int count = itemCompound.getByte("Count");

                    // Convert to Material
                    Material material = Material.matchMaterial(id.replace("minecraft:", ""));
                    if (material == null || material == Material.AIR) {
                        continue; // Skip invalid items
                    }

                    // Create ItemStack
                    ItemStack itemStack = new ItemStack(material, count);

                    // Get the tag compound for item meta
                    if (itemCompound.hasKey("tag")) {
                        NBTCompound tagCompound = itemCompound.getCompound("tag");
                        NBTItem nbtItem = new NBTItem(itemStack);
                        nbtItem.mergeCompound(tagCompound);
                        itemStack = nbtItem.getItem();
                    }

                    items.add(itemStack);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return items.toArray(new ItemStack[0]);
    }
}