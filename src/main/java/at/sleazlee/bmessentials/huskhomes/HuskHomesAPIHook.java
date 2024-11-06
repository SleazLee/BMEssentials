package at.sleazlee.bmessentials.huskhomes;

import net.william278.huskhomes.api.HuskHomesAPI;
import net.william278.huskhomes.position.Position;
import net.william278.huskhomes.position.World;
import net.william278.huskhomes.teleport.TeleportationException;
import net.william278.huskhomes.user.OnlineUser;
import org.bukkit.entity.Player;

import java.util.UUID;

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

    public static void timedTeleportPlayer(Player player, double x, double y, double z, float yaw, float pitch, String worldName, String serverName) {
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
        } catch (TeleportationException e) {
            e.printStackTrace();
            // Optionally, send a message to the player about the failure
            e.displayMessage(onlineUser);
        }
    }
}
