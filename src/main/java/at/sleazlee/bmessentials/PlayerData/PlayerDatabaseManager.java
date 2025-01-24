package at.sleazlee.bmessentials.PlayerData;

import at.sleazlee.bmessentials.BMEssentials;

import java.io.File;
import java.sql.*;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlayerDatabaseManager {
    private BMEssentials plugin;
    private Connection connection;

    public PlayerDatabaseManager(BMEssentials plugin) {
        this.plugin = plugin;
    }

    public void initializeDatabase() {
        try {
            // Ensure the plugin's data folder exists
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            // Load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            // Establish a connection to the SQLite database file
            connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + new File(plugin.getDataFolder(), "playerdata.db"));

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

            // Add currency columns if they don’t exist (SQLite allows IF NOT EXISTS)
            try (PreparedStatement stmtDollars = connection.prepareStatement(
                    "ALTER TABLE playerdata ADD COLUMN dollars REAL DEFAULT 0.0")) {
                stmtDollars.executeUpdate();
            } catch (SQLException ignored) {
                // Column likely exists; ignore error
            }
            try (PreparedStatement stmtVP = connection.prepareStatement(
                    "ALTER TABLE playerdata ADD COLUMN votepoints REAL DEFAULT 0.0")) {
                stmtVP.executeUpdate();
            } catch (SQLException ignored) {
                // Column likely exists; ignore error
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean hasPlayerData(String uuid) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM playerdata WHERE uuid = ?")) {
            statement.setString(1, uuid);
            ResultSet rs = statement.executeQuery();
            boolean hasData = rs.next();
            rs.close();
            return hasData;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void insertPlayerData(String uuid, long joinDate, String centeredName) {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO playerdata (uuid, joinDate, centeredName) VALUES (?, ?, ?)")) {
            statement.setString(1, uuid);
            statement.setLong(2, joinDate);
            statement.setString(3, centeredName);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public long getJoinDate(String uuid) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT joinDate FROM playerdata WHERE uuid = ?")) {
            statement.setString(1, uuid);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                long joinDate = rs.getLong("joinDate");
                rs.close();
                return joinDate;
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public String getCenteredName(String uuid) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT centeredName FROM playerdata WHERE uuid = ?")) {
            statement.setString(1, uuid);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                String centeredName = rs.getString("centeredName");
                rs.close();
                return centeredName;
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get the player's Dollars balance.
     */
    public double getDollars(String uuid) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT dollars FROM playerdata WHERE uuid = ?")) {
            statement.setString(1, uuid);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                double balance = rs.getDouble("dollars");
                rs.close();
                return balance;
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    /**
     * Set the player's Dollars balance.
     */
    public void setDollars(String uuid, double amount) {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE playerdata SET dollars = ? WHERE uuid = ?")) {
            statement.setDouble(1, amount);
            statement.setString(2, uuid);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the player's VotePoints balance.
     */
    public double getVotePoints(String uuid) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT votepoints FROM playerdata WHERE uuid = ?")) {
            statement.setString(1, uuid);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                double balance = rs.getDouble("votepoints");
                rs.close();
                return balance;
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    /**
     * Set the player's VotePoints balance.
     */
    public void setVotePoints(String uuid, double amount) {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE playerdata SET votepoints = ? WHERE uuid = ?")) {
            statement.setDouble(1, amount);
            statement.setString(2, uuid);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get top balances with UUIDs
     * @return List of entries with UUIDs and balances
     */
    public List<Map.Entry<String, Double>> getTopBalances(String currencyColumn, int limit) {
        List<Map.Entry<String, Double>> results = new ArrayList<>();
        String sql = "SELECT uuid, " + currencyColumn + " as bal " +
                "FROM playerdata ORDER BY bal DESC LIMIT ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                String uuid = rs.getString("uuid");
                double balance = rs.getDouble("bal");
                results.add(new AbstractMap.SimpleEntry<>(uuid, balance));
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    public Connection getConnection() {
        return connection;
    }
}