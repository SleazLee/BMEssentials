package at.sleazlee.bmessentials.PlayerData;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.TextUtils.TextCenter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;

/**
 * Listens for player join events.
 * When a player joins for the first time, inserts their data into the database.
 */
public class PlayerJoinListener implements Listener {

    private BMEssentials plugin;

    /**
     * Constructs a PlayerJoinListener with a reference to the main plugin.
     *
     * @param plugin the main plugin instance
     */
    public PlayerJoinListener(BMEssentials plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the PlayerJoinEvent.
     * Checks if the player is new and stores their UUID, join date, and centered name.
     *
     * @param event the player join event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        PlayerDatabaseManager dbManager = plugin.getPlayerDataDBManager();

        // Check if the player data is already stored
        if (!dbManager.hasPlayerData(uuid)) {
            long joinDate = System.currentTimeMillis(); // Current timestamp
            dbManager.insertPlayerData(uuid, joinDate); // Insert data into the database
        }
    }
}
