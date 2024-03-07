package at.sleazlee.bmessentials.trophyroom.db;

import at.sleazlee.bmessentials.trophyroom.data.Trophy;
import at.sleazlee.bmessentials.trophyroom.util.ItemStackJson;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Database {
    private static Database database;
    private final String dbPath;
    private Connection connection;

    public Database(String pluginFolder) {
        // Set the path to the SQLite database file
        this.dbPath = pluginFolder + "/trophies.db";
        database = this;

        try {
            Class.forName("org.sqlite.JDBC"); // Use SQLite driver
            this.connect();
        } catch (SQLException | ClassNotFoundException var8) {
            System.err.println("There was a problem with the SQLite database! Maybe something is wrong with the setup.");
            var8.printStackTrace();
        }

        try {
            this.createTables();
            this.insertDefaults();
        } catch (SQLException var7) {
            System.err.println("There was an error creating the tables");
            var7.printStackTrace();
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Create the 'trophies' table
            stmt.execute("CREATE TABLE IF NOT EXISTS trophies (id VARCHAR(256) NOT NULL, item TEXT, PRIMARY KEY (id))");

            // Create the 'players' table
            stmt.execute("CREATE TABLE IF NOT EXISTS players (uuid VARCHAR(32) NOT NULL, name VARCHAR(256) NOT NULL, PRIMARY KEY (uuid))");

            // Create the 'playerstrophies' table
            stmt.execute("CREATE TABLE IF NOT EXISTS playerstrophies (id VARCHAR(256) NOT NULL, uuid VARCHAR(32) NOT NULL, slot INT NOT NULL, FOREIGN KEY (id) REFERENCES trophies(id), FOREIGN KEY (uuid) REFERENCES players(uuid))");

            // Create the 'settings' table
            stmt.execute("CREATE TABLE IF NOT EXISTS settings (name VARCHAR(256) NOT NULL, value TEXT NOT NULL, PRIMARY KEY (name))");

            // Create the 'messages' table
            stmt.execute("CREATE TABLE IF NOT EXISTS messages (name VARCHAR(256) NOT NULL, value TEXT NOT NULL, PRIMARY KEY (name))");
        }
    }


    private void insertDefaults() throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("INSERT OR IGNORE INTO settings (name, value) VALUES (?, ?)")) {
            // Insert default settings
            ps.setString(1, "menuTitle");
            ps.setString(2, "{player}'s Trophy Room");
            ps.execute();
        }

        try (PreparedStatement ps = connection.prepareStatement("INSERT OR IGNORE INTO messages (name, value) VALUES (?, ?)")) {
            // Insert default messages
            ps.setString(1, "NO_PERMISSIONS");
            ps.setString(2, "&cYou don't have permission to do this.");
            ps.execute();

            ps.setString(1, "TROPHY_NOT_VALID");
            ps.setString(2, "&c{trophy} is not a valid trophy");
            ps.execute();

            ps.setString(1, "PLAYER_NOT_ONLINE");
            ps.setString(2, "&c{player} is not online");
            ps.execute();

            ps.setString(1, "NO_INVENTORY_ROOM");
            ps.setString(2, "&cThere is no room in the inventory");
            ps.execute();

            ps.setString(1, "PLAYER_RECEIVED_TROPHY");
            ps.setString(2, "&7{player}§a received &7{trophy} &atrophy");
            ps.execute();

            ps.setString(1, "YOU_RECEIVED_TROPHY");
            ps.setString(2, "&7You have received &a{trophy} &7trophy!");
            ps.execute();

            ps.setString(1, "ALREADY_OWN_TROPHY");
            ps.setString(2, "&cYou already have &7{trophy}&c in your room");
            ps.execute();

            ps.setString(1, "TROPHY_ALREADY_EXISTS");
            ps.setString(2, "&cAnother trophy with id &7{trophy} &calready exists!");
            ps.execute();
        }
    }


    private void connect() throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    public String getUUID(String name) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT uuid FROM players WHERE name=?")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString(1) : null;
        }
    }

    public String getName(String uuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT name FROM players WHERE uuid=?")) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException var4) {
            var4.printStackTrace();
        }
        return "";
    }


    public List<String> getNames() throws SQLException {
        List<String> names = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT name FROM players")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                names.add(rs.getString(1));
            }
        }
        return names;
    }


    public List<String> getTrophyIds() throws SQLException {
        List<String> ids = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT id FROM trophies")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ids.add(rs.getString(1));
            }
        }
        return ids;
    }


    public Map<String, Trophy> getTrophies() throws SQLException, JsonProcessingException {
        Map<String, Trophy> trophies = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT id, item FROM trophies")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                trophies.put(rs.getString(1), new Trophy(rs.getString(1), ItemStackJson.fromJSON(rs.getString(2))));
            }
        }
        return trophies;
    }


    public Trophy getTrophy(String id) throws SQLException, JsonProcessingException {
        Trophy trophy = null;
        try (PreparedStatement ps = connection.prepareStatement("SELECT id, item FROM trophies WHERE id=?")) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                trophy = new Trophy(rs.getString(1), ItemStackJson.fromJSON(rs.getString(2)));
            }
        }
        return trophy;
    }


    private List<String> getPlayerUUIDs() throws SQLException {
        List<String> uuids = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT uuid FROM players")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                uuids.add(rs.getString(1));
            }
        }
        return uuids;
    }


    public Map<String, Integer> getPlayerTrophies(String uuid) throws SQLException {
        Map<String, Integer> trophies = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT id, slot FROM playerstrophies WHERE uuid=?")) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                trophies.put(rs.getString("id"), rs.getInt("slot"));
            }
        }
        return trophies;
    }


    public void insertPlayerTrophy(String uuid, String id, int slot) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO playerstrophies (id, uuid, slot) VALUES (?, ?, ?)")) {
            ps.setString(1, id);
            ps.setString(2, uuid);
            ps.setInt(3, slot);
            ps.execute();
        }
    }


    public void removePlayerTrophy(String uuid, String id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM playerstrophies WHERE uuid=? AND id=?")) {
            ps.setString(1, uuid);
            ps.setString(2, id);
            ps.execute();
        }
    }


    public String getSetting(String name) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT value FROM settings WHERE name=?")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString(1) : null;
        }
    }


    public String getMessage(String name) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT value FROM messages WHERE name=?")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString(1) : null;
        }
    }


    public void insertPlayer(String uuid, String name) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("INSERT OR IGNORE INTO players (uuid, name) VALUES (?, ?)")) {
            ps.setString(1, uuid);
            ps.setString(2, name);
            ps.execute();
        }

        try (PreparedStatement ps = connection.prepareStatement("UPDATE players SET name=? WHERE uuid=?")) {
            ps.setString(1, name);
            ps.setString(2, uuid);
            ps.execute();
        }
    }


    public void insertTrophy(String id, @NotNull Trophy trophy) throws SQLException, JsonProcessingException {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO trophies (id, item) VALUES (?, ?)")) {
            ps.setString(1, id);
            ps.setString(2, ItemStackJson.toJson(trophy.getItem()));
            ps.execute();
        }
    }


    public static Database getDatabase() {
        return database;
    }
}
