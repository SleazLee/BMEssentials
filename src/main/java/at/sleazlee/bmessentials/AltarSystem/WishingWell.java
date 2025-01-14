package at.sleazlee.bmessentials.AltarSystem;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import org.bukkit.*;
import org.bukkit.entity.Player;

import static at.sleazlee.bmessentials.art.Art.createDustOptions;

/**
 * Controls the animation for the "Wishing Well" altar.
 */
public class WishingWell {

    private static boolean altarActivated = false;

    /**
     * Call this when the altar starts its animation sequence.
     */
    public static void activateWishingWellAltar() {
        altarActivated = true;
    }

    /**
     * Call this when the altar animation is complete.
     */
    public static void deactivateWishingWellAltar() {
        altarActivated = false;
    }

    /**
     * The main method that plays the Wishing Well altar animation.
     *
     * @param plugin             Your main plugin instance.
     * @param altarBlockLocation The block location of the altar (X=210, Y=71, Z=309).
     */
    public static void playWishingWellAnimation(BMEssentials plugin, Location altarBlockLocation) {
        // Mark altar as active
        activateWishingWellAltar();

        // ------------------------------------------------------------------------------------
        // 1) Define key points and overall timing
        // ------------------------------------------------------------------------------------
        World world = altarBlockLocation.getWorld();

        // Adjust to the "center" of the block so it looks nicer and slightly above the block
        Location altarCenter = altarBlockLocation.clone().add(0.5, 1.0, 0.5);

        // The top of the spiral will be 1.5 blocks above the altar
        double spiralHeight = 1.5;

        // The well center
        Location wellCenter = new Location(world, 207.5, 72, 312.5);

        // Define how many ticks we want each phase to take
        int spiralDurationTicks = 60;   // ~3 seconds at 20tps
        int travelDurationTicks = 40;   // ~2 seconds
        // This totals ~5 seconds for the spark to reach the well.

        // ------------------------------------------------------------------------------------
        // 2) Spiral the spark upward with color shifting from yellow to red
        // ------------------------------------------------------------------------------------
        // Define start and end colors for the spark
        Color startColor = Color.fromRGB(0xFF, 0xEA, 0x29); // #ffea29 (yellow)
        Color endColor = Color.fromRGB(0xFF, 0x38, 0x38);   // #ff3838 (red)

        for (int i = 0; i <= spiralDurationTicks; i++) {
            final int step = i;
            Scheduler.runLater(() -> {
                if (!altarActivated) return;

                // Angle to revolve the spark
                double angle = 2 * Math.PI * step / 15;  // Adjust revolutions as needed
                double radius = 0.4;                    // Spiral radius

                // Calculate offsets
                double dx = radius * Math.cos(angle);
                double dz = radius * Math.sin(angle);

                // Gradually move up to 1.5 blocks over the spiralDuration
                double dy = (spiralHeight / spiralDurationTicks) * step;

                // Current location of the spark
                double x = altarCenter.getX() + dx;
                double y = altarCenter.getY() + dy;
                double z = altarCenter.getZ() + dz;

                // Interpolate color based on the current step
                Color currentColor = interpolateColor(startColor, endColor, (double) step / spiralDurationTicks);
                String hexColor = String.format("#%02X%02X%02X", currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue());

                // Spawn a dust particle with the interpolated color
                world.spawnParticle(
                        Particle.DUST,
                        x, y, z,
                        0, 0, 0, 0,
                        createDustOptions(hexColor)
                );
            }, step);
        }

        // ------------------------------------------------------------------------------------
        // 3) Move spark from top of spiral to the well with continued color shifting
        // ------------------------------------------------------------------------------------
        Scheduler.runLater(() -> {
            // The spark’s start location is the last point in the spiral
            final Location sparkStart = altarCenter.clone().add(0, spiralHeight, 0);

            for (int i = 0; i <= travelDurationTicks; i++) {
                final int step = i;
                Scheduler.runLater(() -> {
                    if (!altarActivated) return;

                    // Linear interpolation from sparkStart -> wellCenter
                    double fraction = (double) step / travelDurationTicks;

                    double x = sparkStart.getX() + (wellCenter.getX() - sparkStart.getX()) * fraction;
                    double y = sparkStart.getY() + (wellCenter.getY() - sparkStart.getY()) * fraction;
                    double z = sparkStart.getZ() + (wellCenter.getZ() - sparkStart.getZ()) * fraction;

                    // Continue color shifting from yellow to red during travel
                    Color currentColor = interpolateColor(startColor, endColor, (double) step / travelDurationTicks);
                    String hexColor = String.format("#%02X%02X%02X", currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue());

                    // Spawn a dust particle with the interpolated color
                    world.spawnParticle(
                            Particle.DUST,
                            x, y, z,
                            0, 0, 0, 0,
                            createDustOptions(hexColor)
                    );
                }, step);
            }

        }, spiralDurationTicks);  // start after the spiral finishes

        // ------------------------------------------------------------------------------------
        // 4) Play a jingle once spark arrives at the well
        // ------------------------------------------------------------------------------------
        Scheduler.runLater(() -> {
            if (!altarActivated) return;

            // Play a jingle sound at the well center
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.playSound(wellCenter, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.AMBIENT, 1.0f, 1.0f);
            }
        }, spiralDurationTicks + travelDurationTicks);

        // ------------------------------------------------------------------------------------
        // 5) Purple mist comes out of The Well, then “strikes” the altar
        // ------------------------------------------------------------------------------------
        // We’ll do ~1 second (20 ticks) of purple mist rising from the well.
        Scheduler.runLater(() -> {
            if (!altarActivated) return;

            for (int i = 0; i <= 20; i++) {
                final int step = i;
                Scheduler.runLater(() -> {
                    // Slowly move upward from wellCenter
                    double y = wellCenter.getY() + 0.3 + (0.02 * step);
                    world.spawnParticle(
                            Particle.DUST,
                            wellCenter.getX(),
                            y,
                            wellCenter.getZ(),
                            0, 0, 0, 0,
                            createDustOptions("#A020F0") // purple color
                    );
                }, step);
            }
        }, spiralDurationTicks + travelDurationTicks + 5L);
        // (start a small offset (5 ticks) after spark arrival to make it look smooth)

        // After the mist forms, we quickly shoot a burst from the well to the altar
        // then spawn the reward item (the diamond placeholder).
        Scheduler.runLater(() -> {
            if (!altarActivated) return;

            // "Strike" effect: a quick line of purple from well -> altar
            // We'll do ~10 small steps over 10 ticks
            int strikeSteps = 10;
            for (int i = 0; i <= strikeSteps; i++) {
                final int step = i;
                Scheduler.runLater(() -> {
                    double fraction = (double) step / strikeSteps;
                    double x = wellCenter.getX() + (altarCenter.getX() - wellCenter.getX()) * fraction;
                    double y = wellCenter.getY() + (altarCenter.getY() - wellCenter.getY()) * fraction;
                    double z = wellCenter.getZ() + (altarCenter.getZ() - wellCenter.getZ()) * fraction;

                    world.spawnParticle(
                            Particle.DUST,
                            x, y, z,
                            0, 0, 0, 0,
                            createDustOptions("#A020F0")
                    );
                }, step);
            }
        }, spiralDurationTicks + travelDurationTicks + 25L);

        // ------------------------------------------------------------------------------------
        // 6) Spawn the “prize” (the diamond placeholder) at the altar with enhanced water droplet sphere
        // ------------------------------------------------------------------------------------
        Scheduler.runLater(() -> {
            if (!altarActivated) return;

            // Play a small “poof” or explosion sound
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.playSound(altarCenter, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.AMBIENT, 0.2f, 1.0f);
            }

            // Define the prize location slightly above the altar center
            Location prizeLocation = altarBlockLocation.clone().add(0.5, 1.3, 0.5);

            // Show the item animation at the altar
            AltarManager.showItemAnimation(plugin, prizeLocation, world);

            // Create a dense sphere of water droplet particles around the prize
            createWaterDropletSphere(world, prizeLocation, 0.5, 40, 1L);

            // Deactivate the altar so ambient (if any) returns to default
            deactivateWishingWellAltar();
        }, spiralDurationTicks + travelDurationTicks + 35L);
    }

    /**
     * Interpolates between two colors based on the given fraction.
     *
     * @param startColor The starting color.
     * @param endColor   The ending color.
     * @param fraction   The fraction between 0.0 and 1.0.
     * @return The interpolated color.
     */
    private static Color interpolateColor(Color startColor, Color endColor, double fraction) {
        int red = (int) (startColor.getRed() + (endColor.getRed() - startColor.getRed()) * fraction);
        int green = (int) (startColor.getGreen() + (endColor.getGreen() - startColor.getGreen()) * fraction);
        int blue = (int) (startColor.getBlue() + (endColor.getBlue() - startColor.getBlue()) * fraction);
        return Color.fromRGB(red, green, blue);
    }

    /**
     * Creates a dense sphere of water droplet particles around a given location.
     *
     * @param world             The world where particles are spawned.
     * @param center            The center location of the sphere.
     * @param radius            The radius of the sphere.
     * @param totalPoints       The total number of water droplets to spawn.
     * @param delayBetweenPoints The delay in ticks between spawning each layer of particles.
     */
    private static void createWaterDropletSphere(World world, Location center, double radius, int totalPoints, long delayBetweenPoints) {
        // Using the Fibonacci sphere algorithm for even distribution
        for (int i = 0; i < totalPoints; i++) {
            final int index = i;
            Scheduler.runLater(() -> {
                if (!altarActivated) return;

                double phi = Math.acos(1 - 2.0 * (index + 0.5) / totalPoints);
                double theta = Math.PI * (1 + Math.sqrt(5)) * index; // Golden angle

                double x = radius * Math.sin(phi) * Math.cos(theta);
                double y = radius * Math.sin(phi) * Math.sin(theta);
                double z = radius * Math.cos(phi);

                Location particleLocation = center.clone().add(x, y, z);

                world.spawnParticle(
                        Particle.DRIPPING_WATER,
                        particleLocation,
                        1,
                        0, 0, 0,
                        0.0
                );
            }, index * delayBetweenPoints);
        }
    }
}