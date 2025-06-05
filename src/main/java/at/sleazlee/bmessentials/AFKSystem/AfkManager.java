package at.sleazlee.bmessentials.AFKSystem;

import at.sleazlee.bmessentials.vot.VoteManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages AFK state and player activity.
 */
public class AfkManager {

    private static final AfkManager instance = new AfkManager();
    private final Map<UUID, PlayerActivity> activityMap = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastAfkCommandTime = new ConcurrentHashMap<>();
    // Cooldown time in milliseconds (7 minutes)
    private static final long AFK_MESSAGE_COOLDOWN = 7 * 60 * 1000; // 420,000ms

    /**
     * Returns the singleton instance of AfkManager.
     *
     * @return the AfkManager instance.
     */
    public static AfkManager getInstance() {
        return instance;
    }

    /**
     * Retrieves the PlayerActivity for a given player, creating a new record if necessary.
     *
     * @param player the player.
     * @return the PlayerActivity for the player.
     */
    private PlayerActivity getActivity(Player player) {
        return activityMap.computeIfAbsent(player.getUniqueId(),
                uuid -> new PlayerActivity(System.currentTimeMillis(), player.getLocation()));
    }

    /**
     * Movement is considered significant only when the player's position changes
     * by a threshold amount; head rotation is ignored.
     *
     * @param player      the player.
     * @param newLocation the player's current location.
     * @return true if the player's AFK status changed from true to false.
     */
    public boolean updateActivity(Player player, org.bukkit.Location newLocation) {
        PlayerActivity activity = getActivity(player);
        boolean significant = MovementUtils.isSignificantMovement(activity.getLastLocation(), newLocation);
        if (significant) {
            activity.setLastActiveTime(System.currentTimeMillis());
            activity.setLastLocation(newLocation);
            if (activity.isAfk()) {
                activity.setAfk(false);
                return true; // Status changed from AFK to active.
            }
        }
        return false;
    }

    /**
     * Manually toggles the AFK status for a player via command.
     *
     * @param player the player.
     * @return the new AFK state (true if now AFK, false if active).
     */
    public boolean toggleAfk(Player player) {
        PlayerActivity activity = getActivity(player);
        boolean newState = !activity.isAfk();
        activity.setAfk(newState);
        activity.setLastActiveTime(System.currentTimeMillis());
        activity.setLastLocation(player.getLocation());
        return newState;
    }

    /**
     * Checks all players for inactivity and marks them as AFK if they've been inactive
     * for at least timeoutMillis. This is performed automatically and does not broadcast a message.
     *
     * @param currentTime   the current system time in milliseconds.
     * @param timeoutMillis the inactivity threshold in milliseconds.
     */
    public void checkForInactivity(long currentTime, long timeoutMillis) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerActivity activity = getActivity(player);
            // Get the current location for comparison.
            Location currentLocation = player.getLocation();

            // Check if the player has moved significantly since last check.
            boolean hasMoved = MovementUtils.isSignificantMovement(activity.getLastLocation(), currentLocation);

            if (hasMoved) {
                // Update the last active time and location.
                activity.setLastActiveTime(currentTime);
                activity.setLastLocation(currentLocation);

                // If the player was marked as AFK, mark them active.
                if (activity.isAfk()) {
                    activity.setAfk(false);
                    // Notify the vote system that the player is active again.
                    VoteManager.getInstance().handlePlayerJoin(player);

                    // Broadcast "no longer AFK" if applicable.
                    if (activity.hasBroadcastedAfkMessage()) {
                        String message = "<italic><gray>" + player.getName() + " is no longer AFK</gray></italic>";
                        Bukkit.broadcast(MiniMessage.miniMessage().deserialize(message));
                    }
                    // Reset broadcast flag for the next AFK session.
                    activity.setBroadcastedAfkMessage(false);
                }
            } else {
                // No significant movement detected. Check if the inactivity threshold has been reached.
                if (!activity.isAfk() && (currentTime - activity.getLastActiveTime() >= timeoutMillis)) {
                    activity.setAfk(true);
                    // Notify the vote system that the player is now AFK.
                    VoteManager.getInstance().handlePlayerAfk(player);
                    // Auto AFK should not trigger a "no longer AFK" broadcast unless the player
                    // manually set themselves AFK again.
                    activity.setBroadcastedAfkMessage(false);
                }
            }
        }
    }



    /**
     * Checks if a player is marked as AFK.
     *
     * @param player the player.
     * @return true if the player is AFK, false otherwise.
     */
    public boolean isAfk(Player player) {
        return getActivity(player).isAfk();
    }

    /**
     * Checks if the player can send an AFK message (7-minute cooldown).
     *
     * @param player The player using /afk
     * @return true if the message can be sent, false if on cooldown.
     */
    public boolean canSendAfkMessage(Player player) {
        long currentTime = System.currentTimeMillis();
        long lastTime = lastAfkCommandTime.getOrDefault(player.getUniqueId(), 0L);

        return (currentTime - lastTime) >= AFK_MESSAGE_COOLDOWN;
    }

    /**
     * Updates the last AFK message time when a player successfully sends the message.
     *
     * @param player The player using /afk
     */
    public void updateLastAfkMessageTime(Player player) {
        lastAfkCommandTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void setBroadcastedAfk(Player player, boolean broadcasted) {
        getActivity(player).setBroadcastedAfkMessage(broadcasted);
    }

    public boolean hasBroadcastedAfk(Player player) {
        return getActivity(player).hasBroadcastedAfkMessage();
    }

    public void forceActive(Player player) {
        PlayerActivity activity = getActivity(player);
        // Update last active time and clear AFK unconditionally
        activity.setLastActiveTime(System.currentTimeMillis());
        activity.setLastLocation(player.getLocation());  // optional, if you still want to track location
        if (activity.isAfk()) {
            activity.setAfk(false);
            activity.setBroadcastedAfkMessage(false);
        }
    }


}
