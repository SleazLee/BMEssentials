package at.sleazlee.bmvelocity.util;

import at.sleazlee.bmvelocity.BMVelocity;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import org.geysermc.floodgate.api.FloodgateApi;

/**
 * Listens for player logins and caches Floodgate player UUIDs so that lookups
 * can succeed even when the player is offline.
 */
public class FloodgatePlayerListener {

    private final BMVelocity plugin;

    public FloodgatePlayerListener(BMVelocity plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        try {
            FloodgateApi api = FloodgateApi.getInstance();
            if (api != null && api.isFloodgatePlayer(event.getPlayer().getUniqueId())) {
                UUIDTools.cacheFloodgatePlayer(event.getPlayer().getUsername(), event.getPlayer().getUniqueId());
            }
        } catch (Throwable throwable) {
            plugin.getLogger().warn("Unable to cache Floodgate data for {}", event.getPlayer().getUsername(), throwable);
        }
    }
}
