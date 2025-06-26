package at.sleazlee.bmessentials.wild;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.Set;
import java.util.function.Consumer;
import at.sleazlee.bmessentials.Scheduler;
import java.util.logging.Logger;

/**
 * Handles storage of pre-generated wild teleport locations using SQLite.
 */
public class WildLocationsDatabase {

    private final JavaPlugin plugin;
    private final Logger logger;
    private Connection connection;

    public WildLocationsDatabase(JavaPlugin plugin, WildData wildData) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        connect();
        createTables(wildData.getVersions());
    }

    private void connect() {
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                logger.severe("Could not create plugin data folder for database");
            }
            File dbFile = new File(plugin.getDataFolder(), "WildLocations.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
        } catch (SQLException e) {
            logger.severe("Could not connect to WildLocations database: " + e.getMessage());
        }
    }

    private void createTables(Set<String> versions) {
        for (String version : versions) {
            createTable(version);
        }
    }

    private void createTable(String version) {
        String table = sanitize(version);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS " + table + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "x INTEGER," +
                    "z INTEGER" +
                    ")");
        } catch (SQLException e) {
            logger.severe("Error creating table " + table + ": " + e.getMessage());
        }
    }

    /**
     * Sanitize a bound/version name for use as a SQLite table. All characters
     * other than letters, numbers and underscore are replaced with an
     * underscore. If the resulting name does not begin with a letter or
     * underscore, a 'v' prefix is added to produce a valid identifier.
     */
    private String sanitize(String version) {
        String table = version.replaceAll("[^a-zA-Z0-9_]", "_");
        if (!table.matches("^[A-Za-z_].*")) {
            table = "v" + table;
        }
        return table;
    }

    public void insertLocation(String version, int x, int z) {
        createTable(version);
        String table = sanitize(version);
        String sql = "INSERT INTO " + table + " (x, z) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, x);
            pstmt.setInt(2, z);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Error inserting location into " + table + ": " + e.getMessage());
        }
    }

    /**
     * Asynchronously insert a new location and optionally run a callback on the
     * main thread once the insert completes.
     *
     * @param version  the bound/version name
     * @param x        x-coordinate
     * @param z        z-coordinate
     * @param callback optional task to run on the main server thread when done
     */
    public void insertLocationAsync(String version, int x, int z, Runnable callback) {
        Scheduler.runAsync(() -> {
            insertLocation(version, x, z);
            if (callback != null) {
                Scheduler.run(callback);
            }
        });
    }

    /**
     * Asynchronously insert a new location without a callback.
     */
    public void insertLocationAsync(String version, int x, int z) {
        insertLocationAsync(version, x, z, null);
    }

    public int[] getAndRotateLocation(String version) {
        createTable(version);
        String table = sanitize(version);
        String select = "SELECT id,x,z FROM " + table + " ORDER BY id ASC LIMIT 1";
        try (PreparedStatement stmt = connection.prepareStatement(select)) {
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                return null;
            }
            int id = rs.getInt("id");
            int x = rs.getInt("x");
            int z = rs.getInt("z");
            rs.close();

            connection.setAutoCommit(false);
            try {
                try (PreparedStatement del = connection.prepareStatement("DELETE FROM " + table + " WHERE id = ?")) {
                    del.setInt(1, id);
                    del.executeUpdate();
                }
                try (PreparedStatement ins = connection.prepareStatement("INSERT INTO " + table + " (x,z) VALUES (?,?)")) {
                    ins.setInt(1, x);
                    ins.setInt(2, z);
                    ins.executeUpdate();
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
            return new int[]{x, z};
        } catch (SQLException e) {
            logger.severe("Error rotating location in " + table + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Retrieve and rotate a location asynchronously.
     * The callback is executed on the main server thread.
     *
     * @param version  the bound/version name
     * @param callback consumer receiving the x/z coordinate pair or null
     */
    public void getAndRotateLocationAsync(String version, Consumer<int[]> callback) {
        Scheduler.runAsync(() -> {
            int[] coords = getAndRotateLocation(version);
            Scheduler.run(() -> callback.accept(coords));
        });
    }

    public int getLocationCount(String version) {
        String table = sanitize(version);
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM " + table);
            int count = rs.getInt("cnt");
            rs.close();
            return count;
        } catch (SQLException e) {
            logger.severe("Error counting locations for " + table + ": " + e.getMessage());
            return 0;
        }
    }

    /**
     * Asynchronously count locations.
     */
    public void getLocationCountAsync(String version, Consumer<Integer> callback) {
        Scheduler.runAsync(() -> {
            int count = getLocationCount(version);
            Scheduler.run(() -> callback.accept(count));
        });
    }

    /**
     * Remove all stored locations for the specified version.
     *
     * @param version the bound/version name
     */
    public void purgeLocations(String version) {
        String table = sanitize(version);
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM " + table);
        } catch (SQLException e) {
            logger.severe("Error purging locations for " + table + ": " + e.getMessage());
        }
    }

    /**
     * Purge all locations for a version asynchronously.
     */
    public void purgeLocationsAsync(String version, Runnable callback) {
        Scheduler.runAsync(() -> {
            purgeLocations(version);
            if (callback != null) {
                Scheduler.run(callback);
            }
        });
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.severe("Error closing WildLocations database: " + e.getMessage());
        }
    }
}
