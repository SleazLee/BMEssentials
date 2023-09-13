package at.sleazlee.bmessentials.wild;

import lombok.Getter;
import net.william278.huskhomes.event.TeleportEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NoFallDamage implements Listener {
    @Getter
    private static List<UUID> fallDisabled = new ArrayList<>();
    private final Plugin plugin;

    public NoFallDamage(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onTeleport(TeleportEvent event) {
        var teleport = event.getTeleport();
        var uuid = Bukkit.getOfflinePlayer(teleport.getTeleporter().getUsername()).getUniqueId();
        Player player = Bukkit.getPlayer(uuid);

        Location location = player.getLocation();
        double y = location.getY();
        if (player.isOnline()) {
            fallDisabled.add(uuid);

            // Adjust the delay based on how long you expect the fall to last
            // Here I set it to 20 seconds as an example
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                fallDisabled.remove(uuid);
            }, 240L);  // 400 ticks = 20 seconds
        }
    }

    // Enable if people are dying on server join.

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        fallDisabled.add(event.getPlayer().getUniqueId());

        // You could add the same delay here, or adjust it based on how long you expect the rest of the fall to last
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            fallDisabled.remove(event.getPlayer().getUniqueId());
        }, 240L);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if(e.getEntity() instanceof Player p) {
            if(fallDisabled.contains(p.getUniqueId()) && e.getCause() == EntityDamageEvent.DamageCause.FALL)
                e.setCancelled(true);
        }
    }
}