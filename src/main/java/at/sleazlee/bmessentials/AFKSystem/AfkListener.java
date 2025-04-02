package at.sleazlee.bmessentials.AFKSystem;

import at.sleazlee.bmessentials.vot.VoteManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Listener to detect player activity events and update AFK status.
 * Chat and command usage instantly take a player out of AFK.
 */
public class AfkListener implements Listener {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        boolean wasAfk = AfkManager.getInstance().isAfk(player);
        boolean hadBroadcastedAfk = AfkManager.getInstance().hasBroadcastedAfk(player);

        // Checks if the player was AFK
        if (wasAfk) {
            // Force them out of AFK
            AfkManager.getInstance().forceActive(player);
            VoteManager.getInstance().handlePlayerJoin(player);
        }

        // Only broadcast “no longer AFK” if they were AFK & had broadcast it
        if (hadBroadcastedAfk) {
            String message = "<italic><gray>" + player.getName() + " is no longer AFK</gray></italic>";
            Bukkit.broadcast(MiniMessage.miniMessage().deserialize(message));
            // Reset their broadcasted state so if they go AFK again, it can be announced again later
            AfkManager.getInstance().setBroadcastedAfk(player, true);
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String cmd = event.getMessage().toLowerCase();

        // If the command is /afk (or an alias like /afk2, /afk?), skip forcing them active.
        // This check ensures that we don’t un-AFK them before the /afk command toggles them to AFK.
        if (cmd.startsWith("/afk")) {
            return;
        }

        boolean wasAfk = AfkManager.getInstance().isAfk(player);
        boolean hadBroadcastedAfk = AfkManager.getInstance().hasBroadcastedAfk(player);

        // Checks if the player was AFK
        if (wasAfk) {
            // Force them out of AFK
            AfkManager.getInstance().forceActive(player);
            VoteManager.getInstance().handlePlayerJoin(player);
        }

        // Only broadcast “no longer AFK” if they were AFK & had broadcast it
        if (hadBroadcastedAfk) {
            String message = "<italic><gray>" + player.getName() + " is no longer AFK</gray></italic>";
            Bukkit.broadcast(MiniMessage.miniMessage().deserialize(message));
            // Reset their broadcasted state so if they go AFK again, it can be announced again later
            AfkManager.getInstance().setBroadcastedAfk(player, true);
        }
    }
}
