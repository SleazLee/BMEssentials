package at.sleazlee.bmessentials.AFKSystem;

import org.bukkit.Location;

/**
 * Utility class for determining if a player's movement is significant.
 */
public class MovementUtils {

    private static final double DISTANCE_THRESHOLD = 1.0; // in blocks
    private static final float ANGLE_THRESHOLD = 10.0f;     // in degrees

    /**
     * Determines whether the movement or orientation change between two locations is significant.
     *
     * @param from the previous location.
     * @param to   the current location.
     * @return true if the movement is significant, false otherwise.
     */
    public static boolean isSignificantMovement(Location from, Location to) {
        if (from == null || to == null) {
            return true;
        }
        if (from.getWorld() != to.getWorld()) {
            return true;
        }

        double distance = from.distance(to);
        float yawDiff = Math.abs(from.getYaw() - to.getYaw());
        float pitchDiff = Math.abs(from.getPitch() - to.getPitch());
        if (yawDiff > 180) {
            yawDiff = 360 - yawDiff;
        }
        return (distance > DISTANCE_THRESHOLD) || (yawDiff > ANGLE_THRESHOLD) || (pitchDiff > ANGLE_THRESHOLD);
    }
}
