package at.sleazlee.bmessentials.vot;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens for player join and quit events to adjust the vote counts.
 */
public class PlayerEventListener implements Listener {

    /**
     * Handles player join events.
     *
     * @param event the PlayerJoinEvent
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        VoteManager.getInstance().handlePlayerJoin(event.getPlayer());
    }

    /**
     * Handles player quit events.
     *
     * @param event the PlayerQuitEvent
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        VoteManager.getInstance().handlePlayerQuit(event.getPlayer());
    }
}
