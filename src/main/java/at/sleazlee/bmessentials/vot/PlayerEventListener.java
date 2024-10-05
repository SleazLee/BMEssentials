package at.sleazlee.bmessentials.vot;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens for player join and quit events to adjust the vote counts.
 */
public class PlayerEventListener implements Listener {

    private final VoteManager voteManager = VoteManager.getInstance();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        voteManager.handlePlayerJoin(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        voteManager.handlePlayerQuit(event.getPlayer());
    }
}

