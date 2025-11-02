package at.sleazlee.bmvelocity;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Manages the SQLite database connections and operations.
 * Handles both punishment data and pending vote data.
 */
public class DatabaseManager {
	private HikariDataSource dataSource;
	private final BMVelocity plugin;

	/**
	 * Constructs a new DatabaseManager instance.
	 *
	 * @param plugin the BMVelocity plugin instance.
	 */
	public DatabaseManager(BMVelocity plugin) {
		this.plugin = plugin;

		// Explicitly load the SQLite JDBC driver
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
	 *
	 * @param tableName    the table name.
	 * @param keyColumnName the key column name.
	 * @param uuidValue    the UUID value.
	 * @param targetColumn the target column name.
	 * @param value        the value to set.
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
	 *
	 * @param tableName    the table name.
	 * @param keyColumnName the key column name.
	 * @param uuidValue    the UUID value.
	 * @param targetColumn the target column name.
	 * @param callback     a callback to consume the result.
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
	 *
	 * @throws SQLException if a database access error occurs.
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
	 * Creates the pending_votes table if it does not already exist.
	 * This table stores the number of pending votes for each player's UUID.
	 *
	 * @throws SQLException if a database access error occurs.
	 */
	public void createPendingVotesTable() throws SQLException {
		String sql = "CREATE TABLE IF NOT EXISTS pending_votes (" +
				"uuid TEXT PRIMARY KEY, " +
				"votes INTEGER DEFAULT 0" +
				");";
		try (Connection connection = getConnection();
			 PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.execute();
		}
	}

	/**
	 * Adds a player to the punishments table.
	 *
	 * @param playerUUID the player's UUID.
	 * @throws SQLException if a database access error occurs.
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
	 * Asynchronously increments the pending vote count for a player.
	 *
	 * @param uuid   the player's UUID.
	 * @param amount the number of votes to add.
	 */
	public void incrementVoteCount(UUID uuid, int amount) {
		plugin.getServer().getScheduler().buildTask(plugin, () -> {
			try (Connection connection = getConnection();
				 PreparedStatement ps = connection.prepareStatement(
						 "INSERT INTO pending_votes (uuid, votes) VALUES (?, ?) " +
								 "ON CONFLICT(uuid) DO UPDATE SET votes = votes + ?")) {
				ps.setString(1, uuid.toString());
				ps.setInt(2, amount);
				ps.setInt(3, amount);
				ps.execute();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}).schedule();
	}

	/**
	 * Asynchronously sets the pending vote count for a player.
	 *
	 * @param uuid  the player's UUID.
	 * @param count the vote count to set.
	 */
	public void setVoteCount(UUID uuid, int count) {
		plugin.getServer().getScheduler().buildTask(plugin, () -> {
			try (Connection connection = getConnection();
				 PreparedStatement ps = connection.prepareStatement(
						 "INSERT INTO pending_votes (uuid, votes) VALUES (?, ?) " +
								 "ON CONFLICT(uuid) DO UPDATE SET votes = ?")) {
				ps.setString(1, uuid.toString());
				ps.setInt(2, count);
				ps.setInt(3, count);
				ps.execute();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}).schedule();
	}

	/**
	 * Asynchronously retrieves the pending vote count for a player.
	 *
	 * @param uuid     the player's UUID.
	 * @param callback a callback that accepts the vote count.
	 */
	public void getVoteCount(UUID uuid, Consumer<Integer> callback) {
		plugin.getServer().getScheduler().buildTask(plugin, () -> {
			int voteCount = 0;
			try (Connection connection = getConnection();
				 PreparedStatement ps = connection.prepareStatement(
						 "SELECT votes FROM pending_votes WHERE uuid = ?")) {
				ps.setString(1, uuid.toString());
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					voteCount = rs.getInt("votes");
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			int finalCount = voteCount;
			plugin.getServer().getScheduler().buildTask(plugin, () -> callback.accept(finalCount)).schedule();
		}).schedule();
	}

	// Creates the pending_donations table if it does not already exist.
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
	 * A simple helper class that converts all hyphen removed UUID's to the normal format
	 */
	public static class DonationEntry {
		public final String uuid;
		public final String command;

		public DonationEntry(String uuid, String command) {
			this.uuid = uuid;
			this.command = command;
		}
	}

	/**
	 * Formats a UUID string into the standard hyphenated format.
	 * If the UUID is already in standard format (36 characters with hyphens), returns it in lowercase.
	 * Otherwise, if it's a 32-character string, adds the hyphens.
	 */
	private String formatUUID(String uuid) {
		// If it already contains hyphens and is 36 characters long, assume it's already formatted.
		if (uuid.contains("-") && uuid.length() == 36) {
			return uuid.toLowerCase();
		}
		// Otherwise, remove any hyphens and reformat
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