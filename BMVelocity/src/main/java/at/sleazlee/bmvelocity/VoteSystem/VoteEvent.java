package at.sleazlee.bmvelocity.VoteSystem;

import at.sleazlee.bmvelocity.BMVelocity;
import at.sleazlee.bmvelocity.util.UUIDTools;
import com.velocitypowered.api.event.Subscribe;
import com.vexsoftware.votifier.velocity.event.VotifierEvent;

import java.util.UUID;

/**
 * Listens for Votifier events and hands them to the VoteSystem for processing.
 */
public class VoteEvent {
    private final BMVelocity plugin;

    public VoteEvent(BMVelocity plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onVotifierEvent(VotifierEvent event) {
        String playerName = event.getVote().getUsername();
        String serviceName = event.getVote().getServiceName();
        String serviceAcronym = mapServiceName(serviceName);

        String message = "<light_purple><bold>Vote</light_purple> <aqua>" + playerName +
                "<gray> just voted on <green>" + serviceAcronym +
                "<gray>! Use <red>/vote</red><gray> to earn rewards.";
        plugin.getServer().getAllPlayers().forEach(player ->
                player.sendMessage(plugin.getMiniMessage().deserialize(message))
        );

        UUID uuid = plugin.getServer().getPlayer(playerName)
                .map(player -> {
                    UUID playerUuid = player.getUniqueId();
                    UUIDTools.cacheFloodgatePlayer(playerName, playerUuid);
                    return playerUuid;
                })
                .orElseGet(() -> UUIDTools.getUUID(plugin, playerName));

        if (uuid == null) {
            plugin.getLogger().warn("Vote received for {} but UUID could not be resolved", playerName);
            return;
        }

        plugin.getVoteSystem().handleIncomingVote(uuid, playerName);
    }

    private String mapServiceName(String serviceName) {
        return switch (serviceName) {
            case "PlanetMinecraft.com" -> "PlanetMinecraft";
            case "Minecraft-Server-List.com" -> "Minecraft-Server-List";
            case "MinecraftServers.org" -> "MinecraftServers";
            case "Minecraft-Server.net" -> "Minecraft-Server";
            case "Minecraft.Buzz" -> "MinecraftBuzz";
            case "minestatus.net test vote" -> "MineStatus";
            default -> serviceName;
        };
    }
}
