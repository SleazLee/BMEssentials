package at.sleazlee.bmessentials.huskhomes;

import at.sleazlee.bmessentials.BMEssentials;
import net.william278.huskhomes.api.HuskHomesAPI;
import net.william278.huskhomes.position.Position;
import net.william278.huskhomes.position.World;
import net.william278.huskhomes.teleport.TeleportationException;
import net.william278.huskhomes.user.OnlineUser;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HuskHomesAPIHook {

    public static void instantTeleportPlayer(Player player, double x, double y, double z, float yaw, float pitch, String worldName, String serverName) {
        HuskHomesAPI huskHomesAPI = HuskHomesAPI.getInstance();
        OnlineUser onlineUser = huskHomesAPI.adaptUser(player);

        Position position = Position.at(
                x, y, z, yaw, pitch,
                World.from(worldName, UUID.randomUUID()), serverName
        );

        // Perform instant teleport
        huskHomesAPI.teleportBuilder()
                .teleporter(onlineUser) // The person being teleported
                .target(position)
                .toTeleport()
                .execute();
    }

    public static boolean timedTeleportPlayer(Player player, double x, double y, double z, float yaw, float pitch, String worldName, String serverName) {
        HuskHomesAPI huskHomesAPI = HuskHomesAPI.getInstance();
        OnlineUser onlineUser = huskHomesAPI.adaptUser(player);

        Position position = Position.at(
                x, y, z, yaw, pitch,
                World.from(worldName, UUID.randomUUID()), serverName
        );

        try {
            // Perform timed teleport
            huskHomesAPI.teleportBuilder()
                    .teleporter(onlineUser)
                    .target(position)
                    .toTimedTeleport()
                    .execute();
            return true;
        } catch (TeleportationException e) {
            TeleportationException.Type type = e.getType();
            Level level = switch (type) {
                case WARMUP_ALREADY_MOVING, ALREADY_WARMING_UP -> Level.FINE;
                default -> Level.WARNING;
            };

            Logger logger = BMEssentials.getInstance().getLogger();
            if (logger.isLoggable(level)) {
                logger.log(
                        level,
                        "Failed to start HuskHomes timed teleport for {0}: {1}",
                        new Object[]{player.getName(), type.name()}
                );
            }

            // Let HuskHomes notify the player about what went wrong.
            e.displayMessage(onlineUser);
            return false;
        }
    }

    public static void warpPlayer(Player player, String warpName) {
        HuskHomesAPI huskHomesAPI = HuskHomesAPI.getInstance();
        OnlineUser onlineUser = huskHomesAPI.adaptUser(player);

        huskHomesAPI.getWarp(warpName).thenAccept(optionalWarp -> optionalWarp.ifPresent(warp -> {
            try {
                huskHomesAPI.teleportBuilder()
                        .teleporter(onlineUser)
                        .target(warp)
                        .toTeleport()
                        .execute();
            } catch (TeleportationException e) {
                e.printStackTrace();
                e.displayMessage(onlineUser);
            }
        }));
    }

}