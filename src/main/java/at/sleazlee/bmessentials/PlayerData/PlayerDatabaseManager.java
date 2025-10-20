package at.sleazlee.bmessentials.PlayerData;

import at.sleazlee.bmessentials.BMEssentials;
import java.io.File;
import java.sql.*;
import java.time.Instant;
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
    private static final String TABLE_NAME = "playerdata";
    private static final String VOTE_TABLE_NAME = "player_vote_data";
    private static final String COLUMN_STREAK_CURRENT = "vote_streak_current";
    private static final String COLUMN_STREAK_BEST = "vote_streak_best";
    private static final String COLUMN_LAST_VOTE_AT = "vote_last_timestamp";
    private static final String COLUMN_TOTAL_VOTES = "vote_total";
    private static final String COLUMN_VOTE_PROGRESS = "vote_cycle_progress";
    private static final String COLUMN_LAST_STREAK_INCREMENT = "vote_streak_last_increment";

    private BMEssentials plugin;
    private Connection connection;
    private final Object dbLock = new Object();

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
                    "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                            "uuid TEXT PRIMARY KEY," +
                            "joinDate INTEGER," +
                            "dollars REAL DEFAULT 0.0," +
                            "votepoints REAL DEFAULT 0.0" +
                            ")")) {
                statement.executeUpdate();
            }

            /* Create the vote data table with streak and lifetime statistics. Keeping vote metadata in a
               dedicated table makes it easier to evolve the streak system without touching the legacy
               player currency schema. */
            try (PreparedStatement statement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + VOTE_TABLE_NAME + " (" +
                            "uuid TEXT PRIMARY KEY," +
                            COLUMN_STREAK_CURRENT + " INTEGER DEFAULT 0," +
                            COLUMN_STREAK_BEST + " INTEGER DEFAULT 0," +
                            COLUMN_LAST_VOTE_AT + " INTEGER DEFAULT 0," +
                            COLUMN_TOTAL_VOTES + " INTEGER DEFAULT 0," +
                            COLUMN_VOTE_PROGRESS + " INTEGER DEFAULT 0," +
                            COLUMN_LAST_STREAK_INCREMENT + " INTEGER DEFAULT 0" +
                            ")")) {
                statement.executeUpdate();
            }

            ensureColumn(VOTE_TABLE_NAME, COLUMN_STREAK_CURRENT, "INTEGER", "0");
            ensureColumn(VOTE_TABLE_NAME, COLUMN_STREAK_BEST, "INTEGER", "0");
            ensureColumn(VOTE_TABLE_NAME, COLUMN_LAST_VOTE_AT, "INTEGER", "0");
            ensureColumn(VOTE_TABLE_NAME, COLUMN_TOTAL_VOTES, "INTEGER", "0");
            ensureColumn(VOTE_TABLE_NAME, COLUMN_VOTE_PROGRESS, "INTEGER", "0");
            ensureColumn(VOTE_TABLE_NAME, COLUMN_LAST_STREAK_INCREMENT, "INTEGER", "0");

            migrateLegacyVoteColumns();

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void ensureColumn(String table, String column, String type, String defaultValue) throws SQLException {
        if (columnExists(table, column)) {
            return;
        }

        StringBuilder sql = new StringBuilder("ALTER TABLE ").append(table)
                .append(" ADD COLUMN ").append(column).append(' ').append(type);
        if (defaultValue != null) {
            sql.append(" DEFAULT ").append(defaultValue);
        }

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql.toString());
        }

        if (defaultValue != null) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("UPDATE " + table +
                        " SET " + column + " = " + defaultValue +
                        " WHERE " + column + " IS NULL");
            }
        }
    }

    private boolean columnExists(String table, String column) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                String name = rs.getString("name");
                if (name != null && name.equalsIgnoreCase(column)) {
                    return true;
                }
            }
        }
        return false;
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
                "SELECT 1 FROM " + TABLE_NAME + " WHERE uuid = ?")) {  // '1' is optimized for existence check
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
        String sql = "INSERT INTO " + TABLE_NAME + " (uuid, joinDate) VALUES (?, ?)";

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
        String sql = "SELECT joinDate FROM " + TABLE_NAME + " WHERE uuid = ?";

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
        String sql = "SELECT " + currencyColumn + " FROM " + TABLE_NAME + " WHERE uuid = ?";

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
        String sql = "UPDATE " + TABLE_NAME + " SET " + currencyColumn + " = ? WHERE uuid = ?";

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
        String sql = "SELECT uuid, " + currencyColumn + " FROM " + TABLE_NAME + " ORDER BY " + currencyColumn + " DESC LIMIT ?";

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

    /**
     * Holds vote streak metadata for a player. The streak values are stored alongside the
     * player's total vote count so the vote system can gradually ramp up rewards while still
     * reporting lifetime votes for PlaceholderAPI. The progress counter tracks how many votes
     * have been recorded since the last streak increment (capped at four).
     */
    public record VoteData(int currentStreak, int bestStreak, long lastVoteAtMillis, int totalVotes,
                           int votesTowardsNextIncrement, long lastStreakIncrementMillis) {
        public Instant lastVoteAt() {
            return lastVoteAtMillis > 0 ? Instant.ofEpochMilli(lastVoteAtMillis) : null;
        }

        public Instant lastStreakIncrementAt() {
            return lastStreakIncrementMillis > 0 ? Instant.ofEpochMilli(lastStreakIncrementMillis) : null;
        }
    }

    /**
     * Retrieves the current vote metadata for the supplied player, creating a default row if needed.
     * This method is synchronized so it can be safely invoked from async vote handlers.
     */
    public VoteData getVoteData(String uuid) {
        synchronized (dbLock) {
            ensurePlayerRow(uuid);
            ensureVoteRow(uuid);
            String sql = "SELECT " + COLUMN_STREAK_CURRENT + ", " + COLUMN_STREAK_BEST + ", " + COLUMN_LAST_VOTE_AT + ", " + COLUMN_TOTAL_VOTES +
                    ", " + COLUMN_VOTE_PROGRESS + ", " + COLUMN_LAST_STREAK_INCREMENT +
                    " FROM " + VOTE_TABLE_NAME + " WHERE uuid = ?";

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        int current = rs.getInt(COLUMN_STREAK_CURRENT);
                        int best = rs.getInt(COLUMN_STREAK_BEST);
                        long lastVote = rs.getLong(COLUMN_LAST_VOTE_AT);
                        if (rs.wasNull()) {
                            lastVote = 0;
                        }
                        int total = rs.getInt(COLUMN_TOTAL_VOTES);
                        int cycleProgress = rs.getInt(COLUMN_VOTE_PROGRESS);
                        long lastIncrement = rs.getLong(COLUMN_LAST_STREAK_INCREMENT);
                        if (rs.wasNull()) {
                            lastIncrement = 0;
                        }
                        return new VoteData(current, best, lastVote, total, cycleProgress, lastIncrement);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error retrieving vote metadata: " + e.getMessage());
            }

            return new VoteData(0, 0, 0, 0, 0, 0);
        }
    }

    /**
     * Updates the stored vote metadata for the supplied player.
     */
    public void updateVoteData(String uuid,
                               int currentStreak,
                               int bestStreak,
                               long lastVoteAtMillis,
                               int totalVotes,
                               int votesTowardsNextIncrement,
                               long lastStreakIncrementMillis) {
        synchronized (dbLock) {
            ensurePlayerRow(uuid);
            ensureVoteRow(uuid);
            String sql = "UPDATE " + VOTE_TABLE_NAME +
                    " SET " + COLUMN_STREAK_CURRENT + " = ?, " +
                    COLUMN_STREAK_BEST + " = ?, " +
                    COLUMN_LAST_VOTE_AT + " = ?, " +
                    COLUMN_TOTAL_VOTES + " = ?, " +
                    COLUMN_VOTE_PROGRESS + " = ?, " +
                    COLUMN_LAST_STREAK_INCREMENT + " = ? WHERE uuid = ?";

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, currentStreak);
                statement.setInt(2, bestStreak);
                statement.setLong(3, lastVoteAtMillis);
                statement.setInt(4, totalVotes);
                statement.setInt(5, votesTowardsNextIncrement);
                statement.setLong(6, lastStreakIncrementMillis);
                statement.setString(7, uuid);
                statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error updating vote metadata: " + e.getMessage());
            }
        }
    }

    /**
     * Convenience method for PlaceholderAPI usage – reads only the lifetime vote count.
     */
    public int getTotalVotes(String uuid) {
        return getVoteData(uuid).totalVotes();
    }

    /**
     * Convenience accessor for PlaceholderAPI.
     */
    public int getCurrentStreak(String uuid) {
        return getVoteData(uuid).currentStreak();
    }

    /**
     * Convenience accessor for PlaceholderAPI.
     */
    public int getBestStreak(String uuid) {
        return getVoteData(uuid).bestStreak();
    }

    private void ensurePlayerRow(String uuid) {
        String sql = "INSERT OR IGNORE INTO " + TABLE_NAME + " (uuid, joinDate) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid);
            statement.setLong(2, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error ensuring player row: " + e.getMessage());
        }
    }

    private void ensureVoteRow(String uuid) {
        String sql = "INSERT OR IGNORE INTO " + VOTE_TABLE_NAME + " (uuid) VALUES (?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid);
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error ensuring vote data row: " + e.getMessage());
        }
    }

    private void migrateLegacyVoteColumns() {
        try {
            if (!columnExists(TABLE_NAME, COLUMN_STREAK_CURRENT)
                    || !columnExists(TABLE_NAME, COLUMN_STREAK_BEST)
                    || !columnExists(TABLE_NAME, COLUMN_LAST_VOTE_AT)
                    || !columnExists(TABLE_NAME, COLUMN_TOTAL_VOTES)
                    || !columnExists(TABLE_NAME, COLUMN_VOTE_PROGRESS)
                    || !columnExists(TABLE_NAME, COLUMN_LAST_STREAK_INCREMENT)) {
                return;
            }

            String sql = "INSERT OR IGNORE INTO " + VOTE_TABLE_NAME + " (uuid, " +
                    COLUMN_STREAK_CURRENT + ", " + COLUMN_STREAK_BEST + ", " + COLUMN_LAST_VOTE_AT + ", " +
                    COLUMN_TOTAL_VOTES + ", " + COLUMN_VOTE_PROGRESS + ", " + COLUMN_LAST_STREAK_INCREMENT + ") " +
                    "SELECT uuid, " +
                    "COALESCE(" + COLUMN_STREAK_CURRENT + ", 0), " +
                    "COALESCE(" + COLUMN_STREAK_BEST + ", 0), " +
                    "COALESCE(" + COLUMN_LAST_VOTE_AT + ", 0), " +
                    "COALESCE(" + COLUMN_TOTAL_VOTES + ", 0), " +
                    "COALESCE(" + COLUMN_VOTE_PROGRESS + ", 0), " +
                    "COALESCE(" + COLUMN_LAST_STREAK_INCREMENT + ", 0) " +
                    "FROM " + TABLE_NAME;

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(sql);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to migrate legacy vote columns: " + e.getMessage());
        }
    }
}
