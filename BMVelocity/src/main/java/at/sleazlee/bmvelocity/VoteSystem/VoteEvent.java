package at.sleazlee.bmvelocity.VoteSystem;

import at.sleazlee.bmvelocity.BMVelocity;
import at.sleazlee.bmvelocity.util.UUIDTools;
import com.velocitypowered.api.event.Subscribe;
import com.vexsoftware.votifier.velocity.event.VotifierEvent;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Listens for Votifier events and handles vote logic.
 * <p>
 * When a vote comes in, the vote is broadcast to all players.
 * Then, if the voting player is online:
 * <ul>
 *   <li>If they are on the "spawn" server, a plugin message is sent to the Spigot server (after a 5-second delay) to give the reward.</li>
 *   <li>If they are not on "spawn", their pending vote count is incremented in the database.</li>
 * </ul>
 * If the player is offline, their pending vote count is incremented.
 * When a player later joins, the BMVelocity main class checks the database for pending votes
 * and sends a plugin message to Spigot the appropriate number of times.
 * </p>
 */
public class VoteEvent {
	private final BMVelocity plugin;

	/**
	 * Constructs a new VoteEvent listener.
	 *
	 * @param plugin the BMVelocity plugin instance.
	 */
	public VoteEvent(BMVelocity plugin) {
		this.plugin = plugin;
	}

	/**
	 * Handles Votifier events.
	 * <p>
	 * This method broadcasts the vote message, then checks if the player is online.
	 * If the player is online and on the "spawn" server, it schedules a plugin message to Spigot
	 * after a 5-second delay. Otherwise (if the player is online but not on spawn or offline),
	 * it increments the pending vote count in the database.
	 * </p>
	 *
	 * @param event the Votifier event.
	 */
	@Subscribe
	public void onVotifierEvent(VotifierEvent event) {
		String playerName = event.getVote().getUsername();
		String serviceName = event.getVote().getServiceName();
		String serviceAcronym = serviceName;

		switch (serviceName) {
			case "PlanetMinecraft.com":
				serviceAcronym = "PlanetMinecraft";
				break;
			case "Minecraft-Server-List.com":
				serviceAcronym = "Minecraft-Server-List";
				break;
			case "MinecraftServers.org":
				serviceAcronym = "MinecraftServers";
				break;
			case "Minecraft-Server.net":
				serviceAcronym = "Minecraft-Server";
				break;
			case "Minecraft.Buzz":
				serviceAcronym = "MinecraftBuzz";
				break;
			case "minestatus.net test vote":
				serviceAcronym = "MineStatus";
				break;
		}

		String message = "<light_purple><bold>Vote</light_purple> <aqua>" + playerName +
				"<gray> just voted on <green>" + serviceAcronym +
				"<gray>! Use <red>/vote</red><gray> to earn rewards.";
		plugin.getServer().getAllPlayers().forEach(player ->
				player.sendMessage(plugin.getMiniMessage().deserialize(message))
		);

		// If the player is online, schedule a vote message
		plugin.getServer().getPlayer(playerName).ifPresentOrElse(player -> {
			String currentServer = player.getCurrentServer()
					.map(s -> s.getServerInfo().getName())
					.orElse("none");
			if (currentServer.equalsIgnoreCase("spawn")) {
				plugin.getServer().getScheduler().buildTask(plugin, () -> {
					// Here's the fix: we call the method in VoteSystem:
					plugin.getVoteSystem().sendVotesMessageToSpigot(player.getUniqueId(), 1);
				}).delay(5, TimeUnit.SECONDS).schedule();
			} else {
				// increment vote count if they're on a different server
				plugin.getDatabaseManager().incrementVoteCount(player.getUniqueId(), 1);
			}
		}, () -> {
			// Offline case: try to get offline player's UUID.
                        UUID offlineUuid = UUIDTools.getUUID(plugin, playerName);
			if (offlineUuid != null) {
				plugin.getLogger().info("VoteEvent: Player " + playerName + " is offline. Incrementing vote count.");
				plugin.getDatabaseManager().incrementVoteCount(offlineUuid, 1);
			}
		});
	}

}