package at.sleazlee.bmessentials.PlayerData;

import at.sleazlee.bmessentials.BMEssentials;
import java.io.File;
import java.sql.*;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manages player data storage and retrieval in an SQLite database.
 * Handles database connections, table creation, and CRUD (Create, Read, Update, Delete) operations
 * for player-related information including join dates and currency balances.
 */
public class PlayerDatabaseManager {
    private BMEssentials plugin;
    private Connection connection;

    /**
     * Constructs a new PlayerDatabaseManager instance.
     *
     * @param plugin The main plugin instance for accessing configuration and logger
     */
    public PlayerDatabaseManager(BMEssentials plugin) {
        this.plugin = plugin;
    }

    /**
     * Initializes the database connection and ensures proper table structure.
     * <p>
     * Performs the following operations:
     * 1. Creates plugin data directory if missing
     * 2. Loads SQLite JDBC driver
     * 3. Establishes database connection
     * 4. Creates playerdata table with required columns
     */
    public void initializeDatabase() {
        try {
            // Ensure data directory exists for database storage
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();  // Create directory if missing
            }

            // Load SQLite JDBC driver class
            Class.forName("org.sqlite.JDBC");

            // Create connection to SQLite database file
            connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + new File(plugin.getDataFolder(), "playerdata.db"));

            /* Create the playerdata table with columns:
               - uuid: Unique player identifier (Primary Key)
               - joinDate: Timestamp of first join
               - dollars: Player's dollar balance (default 0.0)
               - votepoints: Player's vote points (default 0.0)
            */
            try (PreparedStatement statement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS playerdata (" +
                            "uuid TEXT PRIMARY KEY," +          // Primary key ensures unique players
                            "joinDate INTEGER," +               // Stored as milliseconds since epoch
                            "dollars REAL DEFAULT 0.0," +       // REAL type for decimal values
                            "votepoints REAL DEFAULT 0.0" +     // Default values set for new players
                            ")")) {
                statement.executeUpdate();
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Safely closes the database connection if it exists and is open.
     * Should be called when the plugin is disabled to release resources.
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed successfully");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error closing database connection: " + e.getMessage());
        }
    }

    /**
     * Checks if a player exists in the database.
     *
     * @param uuid The player's unique identifier
     * @return true if the player exists, false otherwise
     */
    public boolean hasPlayerData(String uuid) {
        // Using try-with-resources to auto-close resources
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM playerdata WHERE uuid = ?")) {  // '1' is optimized for existence check
            statement.setString(1, uuid);  // Set first parameter to UUID
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();  // Returns true if at least one row exists
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error checking player existence: " + e.getMessage());
            return false;
        }
    }

    /**
     * Inserts new player data into the database.
     *
     * @param uuid     The player's unique identifier
     * @param joinDate The timestamp of the player's first join (milliseconds since epoch)
     */
    public void insertPlayerData(String uuid, long joinDate) {
        // Parameterized query prevents SQL injection
        String sql = "INSERT INTO playerdata (uuid, joinDate) VALUES (?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid);      // Set UUID parameter
            statement.setLong(2, joinDate);    // Set joinDate parameter
            statement.executeUpdate();         // Execute the insert operation
        } catch (SQLException e) {
            plugin.getLogger().severe("Error inserting player data: " + e.getMessage());
        }
    }

    /**
     * Retrieves a player's join date from the database.
     *
     * @param uuid The player's unique identifier
     * @return Join date as milliseconds since epoch, or -1 if not found
     */
    public long getJoinDate(String uuid) {
        String sql = "SELECT joinDate FROM playerdata WHERE uuid = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("joinDate");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error retrieving join date: " + e.getMessage());
        }
        return -1;  // Return -1 as error indicator
    }

    /**
     * Retrieves a player's dollar balance.
     *
     * @param uuid The player's unique identifier
     * @return Current dollar balance, 0.0 if not found
     */
    public double getDollars(String uuid) {
        return getCurrencyBalance(uuid, "dollars");
    }

    /**
     * Updates a player's dollar balance.
     *
     * @param uuid   The player's unique identifier
     * @param amount New balance amount
     */
    public void setDollars(String uuid, double amount) {
        updateCurrencyBalance(uuid, "dollars", amount);
    }

    /**
     * Retrieves a player's vote points balance.
     *
     * @param uuid The player's unique identifier
     * @return Current vote points balance, 0.0 if not found
     */
    public double getVotePoints(String uuid) {
        return getCurrencyBalance(uuid, "votepoints");
    }

    /**
     * Updates a player's vote points balance.
     *
     * @param uuid   The player's unique identifier
     * @param amount New balance amount
     */
    public void setVotePoints(String uuid, double amount) {
        updateCurrencyBalance(uuid, "votepoints", amount);
    }

    /**
     * Generic method to retrieve currency balances.
     *
     * @param uuid           Player's unique identifier
     * @param currencyColumn Name of the currency column to query
     * @return Current balance value
     */
    private double getCurrencyBalance(String uuid, String currencyColumn) {
        String sql = "SELECT " + currencyColumn + " FROM playerdata WHERE uuid = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(currencyColumn);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error retrieving " + currencyColumn + ": " + e.getMessage());
        }
        return 0.0;
    }

    /**
     * Generic method to update currency balances.
     *
     * @param uuid           Player's unique identifier
     * @param currencyColumn Name of the currency column to update
     * @param amount         New balance value
     */
    private void updateCurrencyBalance(String uuid, String currencyColumn, double amount) {
        String sql = "UPDATE playerdata SET " + currencyColumn + " = ? WHERE uuid = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDouble(1, amount);   // Set balance value
            statement.setString(2, uuid);     // Set UUID parameter
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error updating " + currencyColumn + ": " + e.getMessage());
        }
    }

    /**
     * Retrieves top balances for a specified currency.
     *
     * @param currencyColumn Which currency to check (dollars/votepoints)
     * @param limit          Maximum number of results to return
     * @return List of entries containing UUIDs and balances, sorted descending
     */
    public List<Map.Entry<String, Double>> getTopBalances(String currencyColumn, int limit) {
        List<Map.Entry<String, Double>> results = new ArrayList<>();
        String sql = "SELECT uuid, " + currencyColumn + " FROM playerdata ORDER BY " + currencyColumn + " DESC LIMIT ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);  // Set LIMIT parameter
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String uuid = rs.getString("uuid");
                    double balance = rs.getDouble(currencyColumn);
                    // Store results as simple key-value pairs
                    results.add(new AbstractMap.SimpleEntry<>(uuid, balance));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error retrieving top balances: " + e.getMessage());
        }
        return results;
    }

    /**
     * Provides direct access to the database connection.
     * Use with caution - prefer using built-in methods when possible.
     *
     * @return The active database connection
     */
    public Connection getConnection() {
        return connection;
    }
}