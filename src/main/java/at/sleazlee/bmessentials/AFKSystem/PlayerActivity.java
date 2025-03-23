package at.sleazlee.bmessentials.AFKSystem;

import org.bukkit.Location;

/**
 * Represents a player's activity data for AFK detection.
 */
public class PlayerActivity {

    private long lastActiveTime;
    private Location lastLocation;
    private boolean afk;
    private boolean broadcastedAfkMessage;

    /**
     * Constructs a new PlayerActivity.
     *
     * @param lastActiveTime the timestamp of the last activity.
     * @param lastLocation   the last known location.
     */
    public PlayerActivity(long lastActiveTime, Location lastLocation) {
        this.lastActiveTime = lastActiveTime;
        this.lastLocation = lastLocation;
        this.afk = false;
    }

    public long getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(long lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(Location lastLocation) {
        this.lastLocation = lastLocation;
    }

    public boolean isAfk() {
        return afk;
    }

    public void setAfk(boolean afk) {
        this.afk = afk;
    }

    public boolean hasBroadcastedAfkMessage() {
        return broadcastedAfkMessage;
    }

    public void setBroadcastedAfkMessage(boolean broadcasted) {
        this.broadcastedAfkMessage = broadcasted;
    }
}
