package at.sleazlee.bmessentials.wild;

import at.sleazlee.bmessentials.Scheduler;
import lombok.Getter;
import net.william278.huskhomes.event.TeleportEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The NoFallDamage class handles preventing fall damage for players who have just teleported.
 */
public class NoFallDamage implements Listener {

    @Getter
    private static final List<UUID> fallDisabled = new ArrayList<>(); // List of player UUIDs with fall damage disabled.


    /**
     * Event handler that triggers when a player teleports.
     * Adds the player to the fallDisabled list to prevent fall damage.
     *
     * @param event The teleport event.
     */
    @EventHandler
    public void onTeleport(TeleportEvent event) {
        var teleport = event.getTeleport();
        var uuid = Bukkit.getOfflinePlayer(teleport.getTeleporter().getUsername()).getUniqueId();
        Player player = Bukkit.getPlayer(uuid);

        if (player != null && player.isOnline()) {
            fallDisabled.add(uuid);

            // Schedule a task to remove the player from the fallDisabled list after a delay.
            Scheduler.runLater(() -> fallDisabled.remove(uuid), 240L); // 240 ticks = 12 seconds
        }
    }

    // New join event handler for rejoining players
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        // Add player to the fall damage disabled list upon join
        fallDisabled.add(uuid);
        // Remove their immunity after a delay (240 ticks = 12 seconds) to avoid permanent immunity
        Scheduler.runLater(() -> fallDisabled.remove(uuid), 240L);
    }

    /**
     * Event handler that triggers when an entity takes damage.
     * Cancels fall damage for players in the fallDisabled list.
     *
     * @param e The damage event.
     */
    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p) {
            // Check if the player has fall damage disabled and the damage cause is fall.
            if (fallDisabled.contains(p.getUniqueId()) && e.getCause() == EntityDamageEvent.DamageCause.FALL) {
                e.setCancelled(true); // Cancel the fall damage.
            }
        }
    }
}
