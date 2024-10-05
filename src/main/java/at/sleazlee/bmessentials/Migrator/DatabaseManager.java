package at.sleazlee.bmessentials.Migrator;

import at.sleazlee.bmessentials.BMEssentials;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.io.File;

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
            // Correct usage: Use plugin.getDataFolder() to get the data folder location.
            File dbFile = new File(plugin.getDataFolder(), "inventories.db");
            // Ensure the data folder exists
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTables();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates the required tables in the database if they do not exist.
     *
     * @throws SQLException if a database access error occurs
     */
    private void createTables() throws SQLException {
        String createInventoriesTable = "CREATE TABLE IF NOT EXISTS inventories (" +
                "player_uuid TEXT PRIMARY KEY," +
                "inventory_data TEXT," +
                "ender_chest_data TEXT" +
                ");";

        String createStatsTable = "CREATE TABLE IF NOT EXISTS stats (" +
                "player_uuid TEXT PRIMARY KEY," +
                "stats_data TEXT" +
                ");";

        String createAchievementsTable = "CREATE TABLE IF NOT EXISTS achievements (" +
                "player_uuid TEXT PRIMARY KEY," +
                "achievements_data TEXT" +
                ");";

        try (PreparedStatement ps1 = connection.prepareStatement(createInventoriesTable);
             PreparedStatement ps2 = connection.prepareStatement(createStatsTable);
             PreparedStatement ps3 = connection.prepareStatement(createAchievementsTable)) {
            ps1.execute();
            ps2.execute();
            ps3.execute();
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
            e.printStackTrace();
        }
    }
}
