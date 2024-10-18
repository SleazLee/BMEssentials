package at.sleazlee.bmessentials.Migrator;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Migrator.Commands.MigrateInventoriesCommand;
import at.sleazlee.bmessentials.Migrator.Commands.ResetMigrationCommand;
import at.sleazlee.bmessentials.Migrator.Commands.RetrieveInventoryCommand;
import at.sleazlee.bmessentials.Migrator.Listeners.InventoryClickListener;
import org.bukkit.plugin.PluginManager;

/**
 * Manages the initialization and shutdown of the Migrator system components.
 */
public class MigratorManager {

    private BMEssentials plugin;
    private DatabaseManager databaseManager;
    private InventorySerializer inventorySerializer;
    private StatsManager statsManager;
    private AdvancementsManager advancementsManager;

    /**
     * Initializes the migrator system components.
     *
     * @param plugin the main plugin instance
     */
    public MigratorManager(BMEssentials plugin) {
        this.plugin = plugin;

        // Initialize core components
        databaseManager = new DatabaseManager(plugin);
        inventorySerializer = new InventorySerializer();
        statsManager = new StatsManager();
        advancementsManager = new AdvancementsManager();

        // Register commands
        plugin.getCommand("migrateinventories").setExecutor(new MigrateInventoriesCommand(this));
        plugin.getCommand("resetmigration").setExecutor(new ResetMigrationCommand(this));
        plugin.getCommand("retrieveinventory").setExecutor(new RetrieveInventoryCommand(this));

        // Register event listeners
        PluginManager pm = plugin.getServer().getPluginManager();
        pm.registerEvents(new InventoryClickListener(this), plugin);
    }

    /**
     * Performs any necessary shutdown procedures for the migrator system.
     */
    public void shutdown() {
        databaseManager.closeConnection();
    }

    // Getters for core components
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public InventorySerializer getInventorySerializer() {
        return inventorySerializer;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public AdvancementsManager getAdvancementsManager() {
        return advancementsManager;
    }

    /**
     * Gets the main plugin instance.
     *
     * @return the plugin instance
     */
    public BMEssentials getPlugin() {
        return plugin;
    }
}
