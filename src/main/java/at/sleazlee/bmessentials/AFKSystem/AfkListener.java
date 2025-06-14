package at.sleazlee.bmessentials.AFKSystem;

import at.sleazlee.bmessentials.vot.VoteManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener to detect player activity events and update AFK status.
 * <p>
 * When a player who is marked as AFK sends a chat message or a command (excluding any /afk commands),
 * they are forced out of AFK mode. Additionally, if the "no longer AFK" message has not yet been broadcast
 * during the current AFK session, it will be broadcast to notify others, and then flagged to prevent duplicate broadcasts.
 * </p>
 */
public class AfkListener implements Listener {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    /**
     * Handles asynchronous player chat events.
     * <p>
     * If the player was AFK, they are forced active and the vote system is updated.
     * The "no longer AFK" message is then broadcast only if it hasn't already been broadcast for the current session.
     * </p>
     *
     * @param event The asynchronous player chat event.
     */
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        boolean wasAfk = AfkManager.getInstance().isAfk(player);
        boolean alreadyBroadcast = AfkManager.getInstance().hasBroadcastedAfk(player);

        // If the player was AFK, force them to become active and update vote status.
        if (wasAfk) {
            AfkManager.getInstance().forceActive(player);
            VoteManager.getInstance().handlePlayerJoin(player);

            // Broadcast "no longer AFK" message only if an "AFK" message was sent.
            if (alreadyBroadcast) {
                String message = "<italic><gray>" + player.getName() + " is no longer AFK</gray></italic>";
                Bukkit.broadcast(miniMessage.deserialize(message));
                // Reset flag so future automatic AFK sessions don't broadcast.
                AfkManager.getInstance().setBroadcastedAfk(player, false);
            }
        }
    }

    /**
     * Handles player command preprocess events.
     * <p>
     * If the player is AFK and sends a command (other than any /afk command), they are forced active
     * and the vote system is updated. The "no longer AFK" message is then broadcast only if it hasn't been broadcast already.
     * </p>
     *
     * @param event The player command preprocess event.
     */
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String cmd = event.getMessage().toLowerCase();

        // Skip processing if the command is /afk or its variants.
        if (cmd.startsWith("/afk")) {
            return;
        }

        boolean wasAfk = AfkManager.getInstance().isAfk(player);
        boolean alreadyBroadcast = AfkManager.getInstance().hasBroadcastedAfk(player);

        // If the player was AFK, force them to become active and update vote status.
        if (wasAfk) {
            AfkManager.getInstance().forceActive(player);
            VoteManager.getInstance().handlePlayerJoin(player);

            // Broadcast "no longer AFK" message only if an "AFK" message was sent.
            if (alreadyBroadcast) {
                String message = "<italic><gray>" + player.getName() + " is no longer AFK</gray></italic>";
                Bukkit.broadcast(miniMessage.deserialize(message));
                // Reset flag so future automatic AFK sessions don't broadcast.
                AfkManager.getInstance().setBroadcastedAfk(player, false);
            }
        }
    }

    /**
     * Cleans up AFK data when a player leaves the server.
     *
     * @param event the player quit event
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        AfkManager.getInstance().removePlayer(event.getPlayer());
    }
}
