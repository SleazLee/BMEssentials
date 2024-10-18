package at.sleazlee.bmessentials.Migrator;

import at.sleazlee.bmessentials.BMEssentials;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.io.File;
import java.util.logging.Level;

/**
 * Manages the database connection and operations for the migrator system.
 */
public class DatabaseManager {

    private Connection connection;
    private BMEssentials plugin;

    /**
     * Initializes the database manager.
     *
     * @param plugin the main plugin instance
     */
    public DatabaseManager(BMEssentials plugin) {
        this.plugin = plugin;
        initializeDatabase();
    }

    /**
     * Initializes the SQLite database and creates necessary tables.
     */
    private void initializeDatabase() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "inventories.db");
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTables();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not initialize database", e);
        }
    }

    /**
     * Creates the required tables in the database if they do not exist.
     */
    private void createTables() {
        String createInventoriesTable = "CREATE TABLE IF NOT EXISTS inventories (" +
                "player_uuid TEXT PRIMARY KEY," +
                "inventory_data TEXT," +
                "ender_chest_data TEXT" +
                ");";

        String createStatsTable = "CREATE TABLE IF NOT EXISTS stats (" +
                "player_uuid TEXT PRIMARY KEY," +
                "stats_data TEXT" +
                ");";

        String createAdvancementsTable = "CREATE TABLE IF NOT EXISTS advancements (" +
                "player_uuid TEXT PRIMARY KEY," +
                "advancements_data TEXT" +
                ");";

        try (PreparedStatement ps1 = connection.prepareStatement(createInventoriesTable);
             PreparedStatement ps2 = connection.prepareStatement(createStatsTable);
             PreparedStatement ps3 = connection.prepareStatement(createAdvancementsTable)) {
            ps1.execute();
            ps2.execute();
            ps3.execute();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not create tables", e);
        }
    }

    /**
     * Gets the database connection.
     *
     * @return the database connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Closes the database connection.
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not close database connection", e);
        }
    }
}