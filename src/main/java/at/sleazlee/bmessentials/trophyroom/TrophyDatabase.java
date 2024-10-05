package at.sleazlee.bmessentials.trophyroom;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A class for handling interactions with the SQLite database for the TrophyRoom plugin.
 * This class manages the Players and Trophies tables and provides methods for data manipulation.
 */
public class TrophyDatabase {

    private final JavaPlugin plugin;
    private Connection connection;

    /**
     * Constructor to initialize the database.
     *
     * @param plugin the JavaPlugin instance to access the plugin folder.
     */
    public TrophyDatabase(JavaPlugin plugin) {
        this.plugin = plugin;
        connect();
        createTables();
        alterPlayersTable();
    }

    /**
     * Establishes a connection to the SQLite database. The database file is located in the plugin's folder.
     * If the database file doesn't exist, it will be created.
     */
    private void connect() {
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            // Path to the .db file in the plugin's folder
            String url = "jdbc:sqlite:" + plugin.getDataFolder() + "/TrophyRooms.db";
            connection = DriverManager.getConnection(url);
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not connect to SQLite database: " + e.getMessage());
        }
    }

    /**
     * Alters the Players table to add the TrophyCount column if it doesn't exist.
     */
    private void alterPlayersTable() {
        try (Statement stmt = connection.createStatement()) {
            // Check if the TrophyCount column exists
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(Players);");
            boolean hasTrophyCount = false;
            while (rs.next()) {
                String columnName = rs.getString("name");
                if ("TrophyCount".equalsIgnoreCase(columnName)) {
                    hasTrophyCount = true;
                    break;
                }
            }
            rs.close();

            // If not, add the TrophyCount column
            if (!hasTrophyCount) {
                stmt.execute("ALTER TABLE Players ADD COLUMN TrophyCount INTEGER DEFAULT 0;");
                plugin.getLogger().info("TrophyCount column added to Players table.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error altering Players table: " + e.getMessage());
        }
    }


    /**
     * Creates the necessary tables (Players and Trophies) in the database if they don't already exist.
     */
    private void createTables() {
        try (Statement stmt = connection.createStatement()) {
            // Create Players table
            stmt.execute("CREATE TABLE IF NOT EXISTS Players (" +
                    "UUID TEXT PRIMARY KEY, " +
                    "Contents TEXT" +
                    ");");

            // Create Trophies table
            stmt.execute("CREATE TABLE IF NOT EXISTS Trophies (" +
                    "Name TEXT PRIMARY KEY, " +
                    "Item TEXT" +
                    ");");

        } catch (SQLException e) {
            plugin.getLogger().severe("Error creating tables: " + e.getMessage());
        }
    }

    /**
     * Sets the trophy count for a player.
     *
     * @param uuid  The UUID of the player.
     * @param count The trophy count to set.
     */
    public void setTrophyCount(UUID uuid, int count) {
        String sql = "UPDATE Players SET TrophyCount = ? WHERE UUID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, count);
            pstmt.setString(2, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error setting trophy count: " + e.getMessage());
        }
    }

    /**
     * Gets the trophy count for a player.
     *
     * @param uuid The UUID of the player.
     * @return The trophy count.
     */
    public int getTrophyCount(UUID uuid) {
        String sql = "SELECT TrophyCount FROM Players WHERE UUID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("TrophyCount");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting trophy count: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Retrieves a list of all trophy names from the Trophies table.
     *
     * @return A list of trophy names.
     */
    public List<String> getAllTrophyNames() {
        List<String> trophyNames = new ArrayList<>();
        String sql = "SELECT Name FROM Trophies";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                trophyNames.add(rs.getString("Name"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error fetching trophy names: " + e.getMessage());
        }
        return trophyNames;
    }

    /**
     * Retrieves the "Contents" field for a player from the Players table.
     *
     * @param uuid the UUID of the player.
     * @return the contents associated with the player.
     */
    public String getContents(UUID uuid) {
        String sql = "SELECT Contents FROM Players WHERE UUID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("Contents");
            } else {
                // Player does not exist in the database; return empty string
                return "";
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error fetching contents: " + e.getMessage());
        }
        return "";
    }


    /**
     * Updates the "Contents" field for a player in the Players table.
     *
     * @param uuid     the UUID of the player.
     * @param contents the new contents to be set.
     */
    public void setContents(UUID uuid, String contents) {
        String sql = "INSERT INTO Players (UUID, Contents) VALUES (?, ?) " +
                "ON CONFLICT(UUID) DO UPDATE SET Contents = excluded.Contents;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, contents);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error updating contents: " + e.getMessage());
        }
    }

    /**
     * Retrieves the "Item" field for a trophy from the Trophies table.
     *
     * @param name the name of the trophy.
     * @return the item associated with the trophy.
     */
    public String getItem(String name) {
        String sql = "SELECT Item FROM Trophies WHERE Name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("Item");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error fetching item: " + e.getMessage());
        }
        return null;
    }

    /**
     * Updates the "Item" field for a trophy in the Trophies table.
     *
     * @param name the name of the trophy.
     * @param item the new item to be set.
     */
    public void setItem(String name, String item) {
        String sql = "UPDATE Trophies SET Item = ? WHERE Name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, item);
            pstmt.setString(2, name);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error updating item: " + e.getMessage());
        }
    }

    /**
     * Adds a new trophy to the Trophies table. If a trophy with the same name exists, it will be replaced.
     *
     * @param name the name of the trophy.
     * @param item the serialized item associated with the trophy.
     */
    public void addTrophy(String name, String item) {
        String sql = "INSERT OR REPLACE INTO Trophies (Name, Item) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, item);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error adding trophy: " + e.getMessage());
        }
    }

    /**
     * Removes a trophy from the Trophies table.
     *
     * @param name the name of the trophy to remove.
     */
    public void removeTrophy(String name) {
        String sql = "DELETE FROM Trophies WHERE Name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error removing trophy: " + e.getMessage());
        }
    }

    /**
     * Closes the connection to the SQLite database to prevent memory leaks.
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error closing database connection: " + e.getMessage());
        }
    }
}
