package at.sleazlee.bmvelocity;

import at.sleazlee.bmvelocity.VoteSystem.QueuedReward;
import at.sleazlee.bmvelocity.VoteSystem.VoteData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Manages the SQLite database connections and operations.
 * Handles punishment data, vote data and pending donation queues.
 */
public class DatabaseManager {
    private static final Gson GSON = new Gson();
    private static final Type REWARD_LIST_TYPE = new TypeToken<List<QueuedReward>>() {
    }.getType();
    private static final String DEFAULT_PENDING_STATE = "'{\"rewards\":[],\"lastIncrement\":0,\"votesTowardNext\":0,\"windowStart\":0}'";

    private HikariDataSource dataSource;
    private final BMVelocity plugin;

    /**
     * Constructs a new DatabaseManager instance.
     *
     * @param plugin the BMVelocity plugin instance.
     */
    public DatabaseManager(BMVelocity plugin) {
        this.plugin = plugin;

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found", e);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:playerdata.db");
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(config);
    }

    /**
     * Obtains a database connection.
     *
     * @return a Connection object.
     * @throws SQLException if a database access error occurs.
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Asynchronously sets a string value in the specified table.
     */
    public void asyncSetString(String tableName, String keyColumnName, String uuidValue, String targetColumn, String value) {
        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO " + tableName + " (" + keyColumnName + ", " + targetColumn + ") " +
                                 "VALUES (?, ?) ON CONFLICT(" + keyColumnName + ") DO UPDATE SET " + targetColumn + " = ?")) {
                statement.setString(1, uuidValue);
                statement.setString(2, value);
                statement.setString(3, value);
                statement.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).schedule();
    }

    /**
     * Asynchronously retrieves a string value from the specified table.
     */
    public void asyncGetString(String tableName, String keyColumnName, String uuidValue, String targetColumn, Consumer<String> callback) {
        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT " + targetColumn + " FROM " + tableName + " WHERE " + keyColumnName + " = ?")) {
                statement.setString(1, uuidValue);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    String result = resultSet.getString(targetColumn);
                    plugin.getServer().getScheduler().buildTask(plugin, () -> callback.accept(result)).schedule();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).schedule();
    }

    /**
     * Closes the database connection pool.
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /**
     * Creates the punishments table if it does not already exist.
     */
    public void createPunishmentsTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS punishments (" +
                "uuid TEXT PRIMARY KEY," +
                "spam INTEGER DEFAULT 0," +
                "language INTEGER DEFAULT 0," +
                "harassment INTEGER DEFAULT 0," +
                "toxicity INTEGER DEFAULT 0," +
                "advertising INTEGER DEFAULT 0," +
                "greifing INTEGER DEFAULT 0," +
                "bullying INTEGER DEFAULT 0," +
                "scam INTEGER DEFAULT 0," +
                "hacking INTEGER DEFAULT 0" +
                ");";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        }
    }

    /**
     * Creates the vote_data table (and migrates legacy pending_votes entries) if necessary.
     */
    public void createVoteDataTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS vote_data (" +
                "uuid TEXT PRIMARY KEY," +
                "current_streak INTEGER NOT NULL DEFAULT 0," +
                "last_vote INTEGER NOT NULL DEFAULT 0," +
                "pending_rewards TEXT NOT NULL DEFAULT " + DEFAULT_PENDING_STATE + "," +
                "lifetime_votes INTEGER NOT NULL DEFAULT 0" +
                ");";
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
            migrateLegacyPendingVotes(connection);
        }
    }

    /**
     * Adds a player to the punishments table if they are missing.
     */
    public void addPlayerToPunishments(String playerUUID) throws SQLException {
        String sql = "INSERT INTO punishments (uuid) VALUES (?) ON CONFLICT(uuid) DO NOTHING;";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUUID);
            statement.execute();
        }
    }

    /**
     * Loads a player's vote data (creating a default row if needed).
     */
    public VoteData loadOrCreateVoteData(UUID uuid) throws SQLException {
        try (Connection connection = getConnection()) {
            VoteData existing = loadVoteData(connection, uuid);
            if (existing != null) {
                return existing;
            }

            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO vote_data (uuid, current_streak, last_vote, pending_rewards, lifetime_votes) " +
                            "VALUES (?, 0, 0, " + DEFAULT_PENDING_STATE + ", 0)")) {
                insert.setString(1, uuid.toString());
                insert.execute();
            }
            return new VoteData(uuid, 0, 0L, 0L, new ArrayList<>(), 0, 0, 0L);
        }
    }

    /**
     * Persists the supplied vote data back to the database.
     */
    public void saveVoteData(VoteData data) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE vote_data SET current_streak = ?, last_vote = ?, pending_rewards = ?, lifetime_votes = ? WHERE uuid = ?")) {
            statement.setInt(1, data.getCurrentStreak());
            statement.setLong(2, data.getLastVote());
            statement.setString(3, serializePendingState(data.getPendingRewards(), data.getLastStreakIncrement(),
                    data.getVotesSinceIncrement(), data.getStreakWindowStart()));
            statement.setInt(4, data.getLifetimeVotes());
            statement.setString(5, data.getUuid().toString());
            statement.executeUpdate();
        }
    }

    /**
     * Updates only the pending reward queue for the player.
     */
    public void updatePendingRewards(VoteData data) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE vote_data SET pending_rewards = ? WHERE uuid = ?")) {
            statement.setString(1, serializePendingState(data.getPendingRewards(), data.getLastStreakIncrement(),
                    data.getVotesSinceIncrement(), data.getStreakWindowStart()));
            statement.setString(2, data.getUuid().toString());
            statement.executeUpdate();
        }
    }

    // -----------------------------------------------------------------
    // Pending donations helpers (unchanged from previous implementation)
    // -----------------------------------------------------------------

    public void createPendingDonationsTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS pending_donations (" +
                "uuid TEXT NOT NULL, " +
                "command TEXT NOT NULL, " +
                "PRIMARY KEY (uuid, command)" +
                ");";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.execute();
        }
    }

    /**
     * Asynchronously adds a donation entry (player UUID and command) to the pending_donations table.
     */
    public void asyncAddPendingDonation(String uuid, String command) {
        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            try (Connection connection = getConnection();
                 PreparedStatement ps = connection.prepareStatement(
                         "INSERT OR IGNORE INTO pending_donations (uuid, command) VALUES (?, ?)")) {
                ps.setString(1, formatUUID(uuid));
                ps.setString(2, command);
                ps.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).schedule();
    }

    /**
     * Asynchronously removes a donation entry for the given player UUID and command.
     */
    public void asyncRemovePendingDonation(String uuid, String command) {
        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            try (Connection connection = getConnection();
                 PreparedStatement ps = connection.prepareStatement(
                         "DELETE FROM pending_donations WHERE uuid = ? AND command = ?")) {
                ps.setString(1, formatUUID(uuid));
                ps.setString(2, command);
                ps.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).schedule();
    }

    /**
     * Asynchronously retrieves all pending donation entries for a given player UUID.
     */
    public void asyncGetPendingDonations(String uuid, Consumer<List<DonationEntry>> callback) {
        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            List<DonationEntry> list = new ArrayList<>();
            try (Connection connection = getConnection();
                 PreparedStatement ps = connection.prepareStatement(
                         "SELECT command FROM pending_donations WHERE uuid = ?")) {
                ps.setString(1, formatUUID(uuid));
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String command = rs.getString("command");
                    list.add(new DonationEntry(uuid, command));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            plugin.getServer().getScheduler().buildTask(plugin, () -> callback.accept(list)).schedule();
        }).schedule();
    }

    /**
     * Simple holder for queued donation commands.
     */
    public static class DonationEntry {
        public final String uuid;
        public final String command;

        public DonationEntry(String uuid, String command) {
            this.uuid = uuid;
            this.command = command;
        }
    }

    // -----------------------------------------------------------------
    // Private helper methods
    // -----------------------------------------------------------------

    private VoteData loadVoteData(Connection connection, UUID uuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT current_streak, last_vote, pending_rewards, lifetime_votes FROM vote_data WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                int currentStreak = rs.getInt("current_streak");
                long lastVote = rs.getLong("last_vote");
                String pending = rs.getString("pending_rewards");
                int lifetimeVotes = rs.getInt("lifetime_votes");
                PendingState state = deserializePendingState(pending);
                return new VoteData(uuid, currentStreak, lastVote, state.lastIncrement, state.rewards, lifetimeVotes,
                        state.votesTowardNext, state.windowStart);
            }
        }
    }

    private void migrateLegacyPendingVotes(Connection connection) {
        try {
            if (!tableExists(connection, "pending_votes")) {
                return;
            }

            try (PreparedStatement select = connection.prepareStatement("SELECT uuid FROM pending_votes");
                 ResultSet rs = select.executeQuery()) {
                while (rs.next()) {
                    String uuid = rs.getString("uuid");
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO vote_data (uuid, current_streak, last_vote, pending_rewards, lifetime_votes) " +
                                    "VALUES (?, 0, 0, " + DEFAULT_PENDING_STATE + ", 0) ON CONFLICT(uuid) DO NOTHING")) {
                        insert.setString(1, uuid);
                        insert.execute();
                    }
                }
            }

            try (Statement drop = connection.createStatement()) {
                drop.execute("DROP TABLE IF EXISTS pending_votes");
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Failed to migrate legacy pending votes table", e);
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }

    private String serializePendingState(List<QueuedReward> rewards, long lastIncrement,
                                         int votesTowardNext, long windowStart) {
        PendingState state = new PendingState();
        state.rewards = rewards == null ? new ArrayList<>() : new ArrayList<>(rewards);
        state.lastIncrement = lastIncrement;
        state.votesTowardNext = votesTowardNext;
        state.windowStart = windowStart;
        return GSON.toJson(state);
    }

    private PendingState deserializePendingState(String json) {
        PendingState state = new PendingState();
        state.rewards = new ArrayList<>();
        state.lastIncrement = 0L;
        state.votesTowardNext = 0;
        state.windowStart = 0L;

        if (json == null || json.isBlank()) {
            return state;
        }

        String trimmed = json.trim();
        try {
            if (trimmed.startsWith("{")) {
                PendingState parsed = GSON.fromJson(trimmed, PendingState.class);
                if (parsed != null) {
                    if (parsed.rewards != null) {
                        state.rewards.addAll(parsed.rewards);
                    }
                    if (parsed.lastIncrement != null) {
                        state.lastIncrement = parsed.lastIncrement;
                    }
                    if (parsed.votesTowardNext != null) {
                        state.votesTowardNext = parsed.votesTowardNext;
                    }
                    if (parsed.windowStart != null) {
                        state.windowStart = parsed.windowStart;
                    }
                }
            } else if (trimmed.startsWith("[")) {
                List<QueuedReward> rewards = GSON.fromJson(trimmed, REWARD_LIST_TYPE);
                if (rewards != null) {
                    state.rewards.addAll(rewards);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warn("Failed to parse pending reward payload '{}': {}", json, e.getMessage());
        }
        return state;
    }

    private static class PendingState {
        List<QueuedReward> rewards;
        Long lastIncrement;
        Integer votesTowardNext;
        Long windowStart;
    }

    /**
     * Formats a UUID string into the standard hyphenated format.
     */
    private String formatUUID(String uuid) {
        if (uuid.contains("-") && uuid.length() == 36) {
            return uuid.toLowerCase();
        }
        String normalized = uuid.replace("-", "").toLowerCase();
        if (normalized.length() != 32) {
            throw new IllegalArgumentException("UUID must have 32 hex digits when normalized");
        }
        return normalized.substring(0, 8) + "-" +
                normalized.substring(8, 12) + "-" +
                normalized.substring(12, 16) + "-" +
                normalized.substring(16, 20) + "-" +
                normalized.substring(20);
    }
}
