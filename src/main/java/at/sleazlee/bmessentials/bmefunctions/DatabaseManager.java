package at.sleazlee.bmessentials.bmefunctions;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
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

public class DatabaseManager {
	private static HikariDataSource dataSource;

	public DatabaseManager(BMEssentials plugin) {
		// Retrieve values from the config.yml
		String host = plugin.getConfig().getString("MySQL.host");
		int port = plugin.getConfig().getInt("MySQL.port");
		String user = plugin.getConfig().getString("MySQL.user");
		String password = plugin.getConfig().getString("MySQL.password");
		String database = plugin.getConfig().getString("MySQL.database");

		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
		config.setUsername(user);
		config.setPassword(password);
		config.setMaximumPoolSize(10); // Adjust as needed
		dataSource = new HikariDataSource(config);
	}

	public static Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	public static void asyncSetString(BMEssentials plugin, String tableName, String keyColumnName, String uuidValue, String targetColumn, String value) {
                Scheduler.runAsync(() -> {
			try (Connection connection = getConnection();
				 PreparedStatement statement = connection.prepareStatement(
						 "INSERT INTO " + tableName + " (" + keyColumnName + ", " + targetColumn + ") VALUES (?, ?) ON DUPLICATE KEY UPDATE " + targetColumn + " = ?")) {
				statement.setString(1, uuidValue);
				statement.setString(2, value);
				statement.setString(3, value);
				statement.execute();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});
	}

	public static void asyncGetString(BMEssentials plugin, String tableName, String keyColumnName, String uuidValue, String targetColumn, Consumer<String> callback) {
                Scheduler.runAsync(() -> {
			try (Connection connection = getConnection();
				 PreparedStatement statement = connection.prepareStatement(
						 "SELECT " + targetColumn + " FROM " + tableName + " WHERE " + keyColumnName + " = ?")) {
				statement.setString(1, uuidValue);
				ResultSet resultSet = statement.executeQuery();
				if (resultSet.next()) {
					String result = resultSet.getString(targetColumn);
					Scheduler.run(() -> callback.accept(result));
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});
	}

	public void close() {
		if (dataSource != null) {
			dataSource.close();
		}
	}

	// NonCommon Methods

	// Create Tables for the Banning/Muting System:
	public void createDatabaseTables() throws SQLException {
		String createPunishmentsTable = "CREATE TABLE IF NOT EXISTS punishments ("
				+ "uuid VARCHAR(36) PRIMARY KEY,"
				+ "spam INT DEFAULT 0,"
				+ "language INT DEFAULT 0,"
				+ "harassment INT DEFAULT 0,"
				+ "toxicity INT DEFAULT 0,"
				+ "advertising INT DEFAULT 0,"
				+ "greifing INT DEFAULT 0,"
				+ "bullying INT DEFAULT 0,"
				+ "scam INT DEFAULT 0,"
				+ "hacking INT DEFAULT 0"
				+ ");";
		String createBonusPlayersTable = "CREATE TABLE IF NOT EXISTS bonus_players ("
				+ "UUID VARCHAR(36) NOT NULL PRIMARY KEY,"
				+ "CurrentTown VARCHAR(16) NOT NULL,"
				+ "Rank VARCHAR(16) NOT NULL,"
				+ "RoleInTown VARCHAR(10) NOT NULL"
				+ ");";
		String createBonusTownsTable = "CREATE TABLE IF NOT EXISTS bonus_towns ("
				+ "TownName VARCHAR(16) NOT NULL PRIMARY KEY,"
				+ "CurrentFullBonus INT NOT NULL,"
				+ "MayorUUID VARCHAR(36) NOT NULL"
				+ ");";
		try (Connection connection = getConnection();
			 PreparedStatement statement = connection.prepareStatement(createPunishmentsTable)) {
			statement.execute();
		}
		try (Connection connection = getConnection();
			 PreparedStatement statement = connection.prepareStatement(createBonusPlayersTable)) {
			statement.execute();
		}
		try (Connection connection = getConnection();
			 PreparedStatement statement = connection.prepareStatement(createBonusTownsTable)) {
			statement.execute();
		}
	}

	// Start of Punishment Methods

	public static void addPlayerToPunishments(String playerUUID) throws SQLException {
		String insertPlayerSQL = "INSERT INTO punishments (uuid) VALUES (?) ON DUPLICATE KEY UPDATE uuid=uuid;";
		try (Connection connection = getConnection();
			 PreparedStatement statement = connection.prepareStatement(insertPlayerSQL)) {
			statement.setString(1, playerUUID);
			statement.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}


	// Start of Town Bonus Methods


	/**
	 * Adds a town to the database.
	 *
	 * @param town  the name of the town the player is associated with
	 * @param uuid the player object of the town's mayor.
	 */
	public static void addATownToTheDB(String uuid, int fullBonus, String town) throws SQLException {
		String insertPlayerSQL = "INSERT INTO bonus_towns (TownName, CurrentFullBonus, MayorUUID) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE TownName=TownName;";
		try (Connection connection = getConnection();
			 PreparedStatement statement = connection.prepareStatement(insertPlayerSQL)) {
			statement.setString(1, town);
			statement.setInt(2, fullBonus);
			statement.setString(3, uuid);
			statement.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Adds a player to the players table with the given UUID, town, rank, and role.
	 *
	 * @param uuid        The UUID of the player.
	 * @param currentTown The name of the player's current town.
	 * @param rank        The rank of the player.
	 * @param roleInTown  The role of the player within the town.
	 */
	public static void addPlayerToTheBonusDB(String uuid, String currentTown, String rank, String roleInTown) {
		final String sql = "INSERT INTO bonus_players (UUID, CurrentTown, Rank, RoleInTown) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE UUID=UUID;";
		try (Connection connection = getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, uuid);
			statement.setString(2, currentTown);
			statement.setString(3, rank);
			statement.setString(4, roleInTown);
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Removes a player from the players table using their UUID.
	 *
	 * @param uuid The UUID of the player to remove.
	 */
	public static void removePlayerFromTheBonusDB(String uuid) {
		final String sql = "DELETE FROM bonus_players WHERE UUID = ?";
		try (Connection connection = getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, uuid);
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Retrieves a list of UUIDs for players in a specific town.
	 *
	 * @param townName The name of the town to search for players.
	 * @return A list of UUIDs for players in the specified town.
	 */
	public List<UUID> getPlayersInTown(String townName) {
		List<UUID> playerUUIDs = new ArrayList<>();
		final String sql = "SELECT UUID FROM bonus_players WHERE CurrentTown = ?";
		try (Connection connection = getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, townName);
			ResultSet resultSet = statement.executeQuery();
			while (resultSet.next()) {
				UUID uuid = UUID.fromString(resultSet.getString("UUID"));
				playerUUIDs.add(uuid);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return playerUUIDs;
	}

	/**
	 * Removes a town from the towns table using the town name.
	 *
	 * @param townName The name of the town to remove.
	 */
	public static void removeTown(String townName) {
		final String sql = "DELETE FROM bonus_towns WHERE TownName = ?";
		try (Connection connection = getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, townName);
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Updates the 'CurrentTown' for all players in a town to a new town name.
	 *
	 * @param oldTownName The name of the town to be updated.
	 * @param newTownName The new town name to be set for the players.
	 */
	public static void updateTownNameForPlayers(String oldTownName, String newTownName) {
		final String sql = "UPDATE bonus_players SET CurrentTown = ? WHERE CurrentTown = ?";
		try (Connection connection = getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, newTownName);
			statement.setString(2, oldTownName);
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Deletes all players in the players table that are in a specific town.
	 *
	 * @param townName The name of the town to delete players from.
	 */
	public static void removePlayersInTown(String townName) {
		final String sql = "DELETE FROM bonus_players WHERE CurrentTown = ?";
		try (Connection connection = getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, townName);
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Updates the MayorUUID of a town to a new player's UUID.
	 *
	 * @param townName   The name of the town for which the mayor should be updated.
	 * @param uuid The UUID of the new mayor.
	 */
	public static void updateTownMayor(String townName, String uuid) {
		final String sql = "UPDATE bonus_towns SET MayorUUID = ? WHERE TownName = ?";
		try (Connection connection = getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, uuid);
			statement.setString(2, townName);
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gets the town role of a player.
	 *
	 * @param uuid the uuid of the player.
	 * @return The town role of the player.
	 */
	public static String getPlayersRole(String uuid) {
		final String sql = "SELECT RoleInTown FROM bonus_players WHERE UUID = ?";
		try (Connection connection = getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, uuid);
			ResultSet resultSet = statement.executeQuery();
			if (resultSet.next()) {
				return resultSet.getString("RoleInTown");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return "default";
	}

	/**
	 * Sets the town role of a player.
	 *
	 * @param uuid the uuid of the player.
	 * @param newRole the new role in the town for the player.
	 */
	public static void setPlayersRole(String uuid, String newRole) {
		final String sql = "UPDATE bonus_players SET RoleInTown = ? WHERE UUID = ?";
		try (Connection connection = getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, newRole);
			statement.setString(2, uuid);
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gets the town of a player.
	 *
	 * @param uuid the uuid of the player.
	 * @return The town of the player.
	 */
	public static String getPlayersTown(String uuid) {
		final String sql = "SELECT CurrentTown FROM bonus_players WHERE UUID = ?";
		try (Connection connection = getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, uuid);
			ResultSet resultSet = statement.executeQuery();
			if (resultSet.next()) {
				return resultSet.getString("CurrentTown");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return "default";
	}

	/**
	 * Sets the town of the player.
	 *
	 * @param uuid the uuid of the player.
	 * @param townName the town of the player.
	 */
	public static void updateTownName(String uuid, String townName) {
		final String sql = "UPDATE bonus_towns SET TownName = ? WHERE MayorUUID = ?";
		try (Connection connection = getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, townName);
			statement.setString(2, uuid);
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gets the rank of a player.
	 *
	 * @param uuid the uuid of the player.
	 * @return The rank of the player.
	 */
	public static String getPlayerRank(String uuid) {
		final String sql = "SELECT Rank FROM bonus_players WHERE UUID = ?";
		try (Connection connection = getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, uuid);
			ResultSet resultSet = statement.executeQuery();
			if (resultSet.next()) {
				return resultSet.getString("Rank");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return "default";
	}

	/**
	 * Sets the rank of the player.
	 *
	 * @param uuid the uuid of the player.
	 * @param rank the rank of the player.
	 */
	public static void setPlayerRank(String uuid, String rank) {
		final String sql = "UPDATE bonus_players SET Rank = ? WHERE UUID = ?";
		try (Connection connection = getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, rank);
			statement.setString(2, uuid);
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	/**
	 * Get the full bonus for a town.
	 *
	 * @param townName The name of the town.
	 * @return The full bonus value.
	 */
	public static int getFullBonus(String townName) {
		final String sql = "SELECT CurrentFullBonus FROM bonus_towns WHERE TownName = ?";
		try (Connection connection = getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, townName);
			ResultSet resultSet = statement.executeQuery();
			if (resultSet.next()) {
				return resultSet.getInt("CurrentFullBonus");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * Set the full bonus for a town.
	 *
	 * @param townName The name of the town.
	 * @param fullBonus The full bonus value.
	 */
	public static void setFullBonus(String townName, int fullBonus) {
		final String sql = "UPDATE bonus_towns SET CurrentFullBonus = ? WHERE TownName = ?";
		try (Connection connection = getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, fullBonus);
			statement.setString(2, townName);
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}

