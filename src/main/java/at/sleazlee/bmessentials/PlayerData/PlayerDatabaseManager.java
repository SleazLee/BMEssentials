package at.sleazlee.bmessentials.PlayerData;

import at.sleazlee.bmessentials.BMEssentials;

import java.io.File;
import java.sql.*;

/**
 * Manages database operations such as connecting, querying, and inserting data.
 * Uses SQLite to store player data locally.
 */
public class PlayerDatabaseManager {

    private BMEssentials plugin;
    private Connection connection;

    /**
     * Constructs a DatabaseManager with a reference to the main plugin.
     *
     * @param plugin the main plugin instance
     */
    public PlayerDatabaseManager(BMEssentials plugin) {
        this.plugin = plugin;
    }

    /**
     * Initializes the SQLite database.
     * Creates the database file and the player data table if they don't exist.
     */
    public void initializeDatabase() {
        try {
            // Ensure the plugin's data folder exists
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            // Load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            // Establish a connection to the SQLite database file
            connection = DriverManager.getConnection("jdbc:sqlite:" + new File(plugin.getDataFolder(), "playerdata.db"));

            // Create the playerdata table if it doesn't exist
            PreparedStatement statement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS playerdata (" +
                            "uuid TEXT PRIMARY KEY," +
                            "joinDate INTEGER," +
                            "centeredName TEXT" +
                            ")"
            );
            statement.executeUpdate();
            statement.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes the database connection when the plugin is disabled.
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

    /**
     * Checks if the player data exists in the database.
     *
     * @param uuid the UUID of the player
     * @return true if the player data exists, false otherwise
     */
    public boolean hasPlayerData(String uuid) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM playerdata WHERE uuid = ?");
            statement.setString(1, uuid);
            ResultSet rs = statement.executeQuery();
            boolean hasData = rs.next();
            rs.close();
            statement.close();
            return hasData;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Inserts new player data into the database.
     *
     * @param uuid         the UUID of the player
     * @param joinDate     the timestamp of the player's first join
     * @param centeredName the centered name of the player
     */
    public void insertPlayerData(String uuid, long joinDate, String centeredName) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO playerdata (uuid, joinDate, centeredName) VALUES (?, ?, ?)"
            );
            statement.setString(1, uuid);
            statement.setLong(2, joinDate);
            statement.setString(3, centeredName);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the join date of a player from the database.
     *
     * @param uuid the UUID of the player
     * @return the join date as a timestamp, or -1 if not found
     */
    public long getJoinDate(String uuid) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT joinDate FROM playerdata WHERE uuid = ?");
            statement.setString(1, uuid);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                long joinDate = rs.getLong("joinDate");
                rs.close();
                statement.close();
                return joinDate;
            }
            rs.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Retrieves the centered name of a player from the database.
     *
     * @param uuid the UUID of the player
     * @return the centered name as a string, or null if not found
     */
    public String getCenteredName(String uuid) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT centeredName FROM playerdata WHERE uuid = ?");
            statement.setString(1, uuid);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                String centeredName = rs.getString("centeredName");
                rs.close();
                statement.close();
                return centeredName;
            }
            rs.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
