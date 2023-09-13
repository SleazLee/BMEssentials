package at.sleazlee.bmessentials.huskhomes;

import net.william278.huskhomes.api.HuskHomesAPI;
import net.william278.huskhomes.position.Position;
import net.william278.huskhomes.position.World;
import net.william278.huskhomes.teleport.TeleportationException;
import net.william278.huskhomes.user.OnlineUser;
import org.bukkit.entity.Player;

import java.util.UUID;

public class HuskHomesAPIHook {

    public static void teleportPlayer(Player player, double x, double y, double z, float yaw, float pitch, String worldName, String serverName) {
        HuskHomesAPI huskHomesAPI = HuskHomesAPI.getInstance();
        OnlineUser onlineUser = huskHomesAPI.adaptUser(player);

        // The TeleportBuilder accepts a class that (extends/is a) Position. This can be a Home, Warp or constructed Position.
        // --> Note that the World object needs the name and UID of the world.
        // --> The UID will be used if the world can't be found by name. You can just pass it a random UUID if you don't have it.
        Position position = Position.at(
                x, y, z, yaw, pitch,
                World.from(worldName, UUID.randomUUID()), serverName
        );

        // To construct a teleport, get a TeleportBuilder with #teleportBuilder
        try {
            huskHomesAPI.teleportBuilder()
                    .teleporter(onlineUser) // The person being teleported
                    .target(position) // The target position
                    .toTimedTeleport()
                    .execute(); // #execute() can throw a TeleportationException
        } catch(
                TeleportationException e) {
            e.printStackTrace(); // This exception will contain the reason why the teleport failed, so you can handle it gracefully.
        }
    }
}