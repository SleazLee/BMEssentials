package at.sleazlee.bmvelocity.VoteSystem;

import at.sleazlee.bmvelocity.BMVelocity;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class VoteSystem {

    private final BMVelocity plugin;

    public VoteSystem(BMVelocity plugin) {
        this.plugin = plugin;
    }

    /**
     * Listens for player login. Checks pending votes.
     */
    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        String serverName = event.getPlayer().getCurrentServer()
                .map(conn -> conn.getServerInfo().getName())
                .orElse("");
        checkPendingVotesAndSend(uuid, serverName);
    }

    /**
     * Listens for server switch. Checks pending votes.
     */
    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        String serverName = event.getServer().getServerInfo().getName();
        checkPendingVotesAndSend(uuid, serverName);
    }

    /**
     * Checks if a player has pending votes and if they are on the "spawn" server.
     */
    private void checkPendingVotesAndSend(UUID uuid, String serverName) {
        plugin.getDatabaseManager().getVoteCount(uuid, count -> {
            if (count > 0 && serverName.equalsIgnoreCase("spawn")) {
                // schedule a small delay, then send the votes message to spigot
                plugin.getServer().getScheduler().buildTask(plugin, () -> {
                    sendVotesMessageToSpigot(uuid, count);
                    // Reset pending votes
                    plugin.getDatabaseManager().setVoteCount(uuid, 0);
                }).delay(5, TimeUnit.SECONDS).schedule();
            }
        });
    }

    /**
     * Called externally to send a plugin message
     * about votes to the Spigot backend.
     */
    public void sendVotesMessageToSpigot(UUID uuid, int voteCount) {
        // This example uses a method from "BMVelocity" to get the server & logger
        plugin.getServer().getPlayer(uuid).ifPresent(player -> {
            player.getCurrentServer().ifPresent(connection -> {
                boolean success = false;
                try {
                    success = connection.sendPluginMessage(
                            MinecraftChannelIdentifier.create("bmessentials", "vote"),
                            plugin.getAes().encrypt(buildVotePayload(uuid, voteCount))
                    );
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                plugin.getLogger().info("[VoteSystem] Sent plugin message for {}: {}", player.getUsername(), success);
            });
        });
    }

    /**
     * Builds the vote payload as a byte array.
     */
    private byte[] buildVotePayload(UUID uuid, int count) {
        String msg = uuid.toString() + ";" + count;
        return msg.getBytes();
    }
}
