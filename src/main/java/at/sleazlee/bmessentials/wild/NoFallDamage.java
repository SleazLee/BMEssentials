package at.sleazlee.bmessentials.wild;

import at.sleazlee.bmessentials.Scheduler;
import lombok.Getter;
import net.william278.huskhomes.event.TeleportEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The NoFallDamage class handles preventing fall damage for players who have just teleported.
 */
public class NoFallDamage implements Listener {

    @Getter
    private static final List<UUID> fallDisabled = new ArrayList<>(); // List of player UUIDs with fall damage disabled.
    private final Plugin plugin; // Reference to the main plugin.

    /**
     * Constructs a NoFallDamage object.
     *
     * @param plugin The main plugin instance.
     */
    public NoFallDamage(Plugin plugin) {
        this.plugin = plugin;
    }

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
