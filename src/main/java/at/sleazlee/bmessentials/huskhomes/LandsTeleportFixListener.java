package at.sleazlee.bmessentials.huskhomes;

import me.angeschossen.lands.api.events.land.spawn.LandSpawnTeleportEvent;
import me.angeschossen.lands.api.land.Land;
import me.angeschossen.lands.api.player.LandPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class LandsTeleportFixListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLandSpawnTeleport(LandSpawnTeleportEvent event) {
        // Skip processing if the event is already canceled.
        if (event.isCancelled()) {
            return;
        }

        // Cancel the Lands teleport to prevent its default execution.
        event.setCancelled(true);

        // Retrieve the player from the Lands event.
        LandPlayer landPlayer = event.getLandPlayer();
        if (landPlayer == null) {
            return;
        }
        Player player = landPlayer.getPlayer();
        if (player == null) {
            return;
        }

        // Get the destination (spawn) from the Land.
        Land land = event.getLand();
        Location spawn = land.getSpawn(); // Using the Lands API to get spawn location.
        if (spawn == null) {
            player.sendMessage("This land has no spawn defined!");
            return;
        }

        // Extract coordinates and orientation.
        double x = spawn.getX();
        double y = spawn.getY();
        double z = spawn.getZ();
        float yaw = spawn.getYaw();
        float pitch = spawn.getPitch();

        // Get world name and server name (server name may be fixed or come from configuration).
        String worldName = spawn.getWorld().getName();
        String serverName = player.getServer().getName();

        // Use the HuskHomes API hook to perform a timed teleport.
        HuskHomesAPIHook.timedTeleportPlayer(player, x, y, z, yaw, pitch, worldName, serverName);
    }
}