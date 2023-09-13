package at.sleazlee.bmessentials.LandsBonus;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.bmefunctions.DatabaseManager;
import net.milkbowl.vault.permission.Permission;
import net.william278.husktowns.events.MemberJoinEvent;
import net.william278.husktowns.events.MemberLeaveEvent;
import net.william278.husktowns.events.TownCreateEvent;
import net.william278.husktowns.events.TownDisbandEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.sql.SQLException;
import java.util.UUID;

public class BonusMethods implements Listener {

	private final BMEssentials plugin;

	public BonusMethods(BMEssentials plugin) {
		this.plugin = plugin;
	}

	private static Permission permission;

	public static boolean setupPermissions() {
		RegisteredServiceProvider<Permission> permissionProvider = Bukkit.getServicesManager().getRegistration(Permission.class);
		if (permissionProvider != null) {
			permission = permissionProvider.getProvider();
		}
		return permission != null;
	}

	@EventHandler
	public void onTownCreate(TownCreateEvent event) {
		Player player = event.getPlayer();
		String rank = getPlayerPrimaryGroup(player);
		String town = event.getTownName();
		String role = "Mayor";
		String uuid = player.getUniqueId().toString();
		int totalChunks = SearchRank.searchRank(rank, "TotalChunks");
		int memberSlots = SearchRank.searchRank(rank, "MemberSlots");

		DatabaseManager.addPlayerToTheBonusDB(uuid, town, rank, role);
		try {
			DatabaseManager.addATownToTheDB(uuid, totalChunks, town);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			String commandCreator = "admintown bonus town " + town + " set claims 0" + totalChunks;
			runCommandAsOp(commandCreator);
		}, 30L); // 40 ticks (20 ticks per second, so 40 ticks 	= 2 seconds)

		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			String commandCreator = "admintown bonus town " + town + " set members " + memberSlots;
			runCommandAsOp(commandCreator);
		}, 50L); // 40 ticks (20 ticks per second, so 40 ticks = 2 seconds)
	}

	/**
	 * Handles the MemberJoinEvent and updates the player's information in the database.
	 *
	 * @param event The MemberJoinEvent.
	 */
	@EventHandler
	public void onMemberJoin(MemberJoinEvent event) {
		// get player info (BonusChunks)
		String uuid = event.getUser().getUuid().toString();
		Player player = Bukkit.getPlayer(UUID.fromString(uuid));
		String rank = BonusMethods.getPlayerPrimaryGroup(player);
		String role = "Resident";

		// get the towns current LandBonus
		String town = event.getTown().getName();
		int landBonus = DatabaseManager.getFullBonus(town);
		int totalChunks = SearchRank.searchRank(rank, "BonusChunks");

		// add the players BonusChunks to the total LandBonus
		int newLandBonus = landBonus + totalChunks;
		DatabaseManager.setFullBonus(town, newLandBonus);

		// run LandBonus command as console
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			String commandCreator = "admintown bonus town " + town + " set claims " + newLandBonus;
			runCommandAsOp(commandCreator);
		}, 40L); // 40 ticks (20 ticks per second, so 40 ticks = 2 seconds)

		// Update players info (Land Name and Role)
		DatabaseManager.addPlayerToTheBonusDB(uuid, town, rank, role);
	}

	/**
	 * Handles the MemberLeaveEvent and updates the player's information in the database.
	 *
	 * @param event The MemberJoinEvent.
	 */
	@EventHandler
	public void MemberLeaveEvent(MemberLeaveEvent event) {
		// get the towns current LandBonus
		String uuid = event.getMember().user().getUuid().toString();
		String role = DatabaseManager.getPlayersRole(uuid);

		if (!role.equals("Mayor")) {

			String town = event.getTown().getName();
			int landBonus = DatabaseManager.getFullBonus(town);
			int totalChunks = SearchRank.searchRank(DatabaseManager.getPlayerRank(uuid), "BonusChunks");

			// remove the players BonusChunks to the total LandBonus
			int newLandBonus = (landBonus - totalChunks);
			DatabaseManager.setFullBonus(town, newLandBonus);

			// run LandBonus command as console
			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				String commandCreator = "admintown bonus town " + town + " set claims " + newLandBonus;
				runCommandAsOp(commandCreator);
			}, 40L); // 40 ticks (20 ticks per second, so 40 ticks = 2 seconds)

			// Update players info (Land Name and Role)
			DatabaseManager.removePlayerFromTheBonusDB(uuid);
		}
	}

	/** Handles when a Town Mayor kicks a player.
	 *
	 */
	public static void memberWasKicked(BMEssentials plugin, String playerName, String townName) {
		String uuid = Bukkit.getOfflinePlayer(playerName).getUniqueId().toString();

		int landBonus = DatabaseManager.getFullBonus(townName);
		int totalChunks = SearchRank.searchRank(DatabaseManager.getPlayerRank(uuid), "BonusChunks");

		// remove the players BonusChunks to the total LandBonus
		int newLandBonus = (landBonus - totalChunks);
		DatabaseManager.setFullBonus(townName, newLandBonus);

		// run LandBonus command as console
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			String commandCreator = "admintown bonus town " + townName + " set claims " + newLandBonus;
			runCommandAsOp(commandCreator);
		}, 40L); // 40 ticks (20 ticks per second, so 40 ticks = 2 seconds)

		// Update players info (Land Name and Role)
		DatabaseManager.removePlayerFromTheBonusDB(uuid);
	}


	/** Handles what happens when an Owner Transfers the Ownership of their town.
	 *
	 */
	public static void transferOwnership(BMEssentials plugin, String townName, String oldOwner, String newOwner) {

		// This Runs for the Old Owner (Subtract)
		String oldOwnerUUID = Bukkit.getOfflinePlayer(oldOwner).getUniqueId().toString();
		String playersRole = DatabaseManager.getPlayersRole(oldOwnerUUID);

		if (playersRole.equals("Mayor")) {

			String newOwnerUUID = Bukkit.getOfflinePlayer(newOwner).getUniqueId().toString();
			int totalTownBonus = DatabaseManager.getFullBonus(townName);
			int oldTotalChunks = SearchRank.searchRank(DatabaseManager.getPlayerRank(oldOwnerUUID), "TotalChunks");
			int newTotalChunks = SearchRank.searchRank(DatabaseManager.getPlayerRank(newOwnerUUID), "TotalChunks");
			int oldBonusChunks = SearchRank.searchRank(DatabaseManager.getPlayerRank(oldOwnerUUID), "BonusChunks");
			int newBonusChunks = SearchRank.searchRank(DatabaseManager.getPlayerRank(newOwnerUUID), "BonusChunks");
			int newMemberSlots = SearchRank.searchRank(DatabaseManager.getPlayerRank(newOwnerUUID), "MemberSlots");

			// Deduct The OldOwners FullBonus and add OldOwners bonusChunks
			int newLandBonus = ((totalTownBonus - (oldTotalChunks - oldBonusChunks)) + (newTotalChunks - newBonusChunks));
			DatabaseManager.setPlayersRole(oldOwnerUUID, "Resident");
			DatabaseManager.setPlayersRole(newOwnerUUID, "Mayor");
			DatabaseManager.updateTownMayor(newOwnerUUID, townName);
			DatabaseManager.setFullBonus(townName, newLandBonus);


			// run LandBonus command as console (2 seconds)
			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				String commandCreator = "admintown bonus town " + townName + " set claims " + newLandBonus;
				runCommandAsOp(commandCreator);
			}, 20L); // 40 ticks (20 ticks per second, so 40 ticks 	= 2 seconds)

			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				String commandCreator = "admintown bonus town " + townName + " set members " + newMemberSlots;
				runCommandAsOp(commandCreator);
			}, 40L); // 40 ticks (20 ticks per second, so 40 ticks = 2 seconds)
		}
	}

	/** Handles what happens when Town is Disbanded.
	 *
	 */
	@EventHandler
	public void TownDisbandEvent(TownDisbandEvent event) {
		// get the town name
		String town = event.getTown().getName();
		// remove the town from the DB and also remove all players who are in that town from the player db
		DatabaseManager.removePlayersInTown(town);
		DatabaseManager.removeTown(town);
	}

	/** Handles a Town's name change.
	 *
	 */
	public static void townNameChange(BMEssentials plugin, String playerName, String newTownName) {

		// from PlayerName get uuid and get the townName associated with that uuid
		String uuid = Bukkit.getOfflinePlayer(playerName).getUniqueId().toString();
		String playersRole = DatabaseManager.getPlayersRole(uuid);
		if (playersRole.equals("Mayor")) {

			String oldTownName = DatabaseManager.getPlayersTown(uuid);
			if (!oldTownName.equals(newTownName)) {
				// update the NewTownName for the Owner
				DatabaseManager.updateTownName(uuid, newTownName);
				// cycle through all players in the OldTownName and update it to the NewTownName
				DatabaseManager.updateTownNameForPlayers(oldTownName, newTownName);
			}
		}
	}

	/** Handles what happens when a Player Ranks up.
	 *
	 */
	public static void playerRankUp(BMEssentials plugin, String playerName, String newRank) {

		// Input PlayerName, OldRank, NewRank
		String uuid = Bukkit.getOfflinePlayer(playerName).getUniqueId().toString();
		String role = DatabaseManager.getPlayersRole(uuid);

		// if Mayor
		if (role.equals("Mayor")) {
			String oldRank = DatabaseManager.getPlayerRank(uuid);
			String town = DatabaseManager.getPlayersTown(uuid);
			int oldTotalBonusChunks = DatabaseManager.getFullBonus(town);
			// Take the OldRanks TotalChunks and minus it from the NewRanks TotalChunks
			int oldTotalChunks = SearchRank.searchRank(oldRank, "TotalChunks");
			int newTotalChunks = SearchRank.searchRank(newRank, "TotalChunks");
			int newMemberSlots = SearchRank.searchRank(newRank, "MemberSlots");

			int totalBonusChunks = ((oldTotalBonusChunks - oldTotalChunks) + newTotalChunks);
			DatabaseManager.setFullBonus(town, totalBonusChunks);
			// update the players rank
			DatabaseManager.setPlayerRank(uuid, newRank);

			// add this value to the Towns FullBonus

			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				String commandCreator = "admintown bonus town " + town + " set claims " + totalBonusChunks;
				runCommandAsOp(commandCreator);
			}, 30L); // 40 ticks (20 ticks per second, so 40 ticks 	= 2 seconds)

			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				String commandCreator = "admintown bonus town " + town + " set members " + newMemberSlots;
				runCommandAsOp(commandCreator);
			}, 50L); // 40 ticks (20 ticks per second, so 40 ticks = 2 seconds)
		}

		else if (role.equals("Resident") || role.equals("Trustee")) {
			String oldRank = DatabaseManager.getPlayerRank(uuid);
			String town = DatabaseManager.getPlayersTown(uuid);
			int oldTotalBonusChunks = DatabaseManager.getFullBonus(town);
			// Take the OldRanks BonusChunks and minus it from the NewRanks BonusChunks
			int oldBonusChunks = SearchRank.searchRank(oldRank, "BonusChunks");
			int newBonusChunks = SearchRank.searchRank(newRank, "BonusChunks");

			int totalBonusChunks = ((oldTotalBonusChunks - oldBonusChunks) + newBonusChunks);
			DatabaseManager.setFullBonus(town, totalBonusChunks);
			// update the players rank
			DatabaseManager.setPlayerRank(uuid, newRank);

			// add this value to the Towns FullBonus
			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				String commandCreator = "admintown bonus town " + town + " set claims " + totalBonusChunks;
				runCommandAsOp(commandCreator);
			}, 30L); // 40 ticks (20 ticks per second, so 40 ticks 	= 2 seconds)
		}
	}

	/** Runs a command as op through console.
	 *
	 * @param command 	A command you want to run in console.
	 */
	public static void runCommandAsOp(String command) {
		Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command);
	}

	public static String getPlayerPrimaryGroup(Player player) {
		if (permission == null) {
			throw new IllegalStateException("Vault not found");
		}
		return permission.getPrimaryGroup(player);
	}


	/**
	 * Method for when a player donates
	 * @param plugin
	 * @param playerName    The name of the target player.
	 * @param rankChain    The rank chain of the donation rank.
	 */
	public static void playerDonated(BMEssentials plugin, String playerName, String rankChain) {

		//Find players current rank
		String uuid = Bukkit.getOfflinePlayer(playerName).getUniqueId().toString();
		String currentRank = DatabaseManager.getPlayerRank(uuid);

		//Get ranking
		int Ranking = SearchRank.searchRank(currentRank, "Ranking");

		//Build New Rank
		String newRank = "[" + Ranking + "][" + rankChain + "]";

		//rank player up
		String commandCreator = "lp user " + playerName + " parent set " + newRank;
		runCommandAsOp(commandCreator);

		// Send it off to playerRankUp()
		playerRankUp(plugin, playerName, newRank);

		System.out.println("§b§lBMLands " + currentRank + " " + Ranking + " " + newRank); // Debugging statement

	}
}
