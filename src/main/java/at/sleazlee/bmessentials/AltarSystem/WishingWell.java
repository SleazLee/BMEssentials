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
     * @param plugin          Your main plugin instance.
     * @param altarBlockLocation The block location of the altar (X=210, Y=71, Z=309).
     */
    public static void playWishingWellAnimation(BMEssentials plugin, Location altarBlockLocation) {
        // Mark altar as active (if you want an ambient effect like HealingSprings).
        activateWishingWellAltar();

        // ------------------------------------------------------------------------------------
        // 1) Define key points and overall timing
        // ------------------------------------------------------------------------------------
        // Altar block is at  (210, 71, 309). We'll assume altarBlockLocation is already that.
        // Well center is at  (207, 71, 312).
        // We’ll spawn our spark a bit more "centered" on the block, so add 0.5 to X/Z.
        // Then we’ll animate a small spiral going upward 1.5 blocks over ~3 seconds,
        // and afterwards have it quickly move to the well in ~2 seconds.

        // Some location references:
        World world = altarBlockLocation.getWorld();

        // Adjust to the "center" of the block so it looks nicer
        Location altarCenter = altarBlockLocation.clone().add(0.5, 1, 0.5);

        // The top of the spiral will be 1.5 blocks above the altar
        double spiralHeight = 1.5;

        // The well center
        Location wellCenter = new Location(world, 207.5, 72, 312.5);

        // Define how many ticks we want each phase to take
        int spiralDurationTicks = 60;   // ~3 seconds at 20tps
        int travelDurationTicks = 40;   // ~2 seconds
        // This totals ~5 seconds for the spark to reach the well.

        // ------------------------------------------------------------------------------------
        // 2) Spiral the spark upward
        // ------------------------------------------------------------------------------------
        // We'll schedule 60 small tasks (one per tick) to create the spiral effect.
        for (int i = 0; i <= spiralDurationTicks; i++) {
            final int step = i;
            Scheduler.runLater(() -> {
                if (!altarActivated) return;

                // Angle to revolve the spark
                double angle = 2 * Math.PI * step / 15;  // more revolutions if you like
                double radius = 0.4;                    // how wide the spiral is

                // Calculate offsets
                double dx = radius * Math.cos(angle);
                double dz = radius * Math.sin(angle);

                // Gradually move up to 1.5 blocks over the spiralDuration
                double dy = (spiralHeight / spiralDurationTicks) * step;

                // Current location of the spark
                double x = altarCenter.getX() + dx;
                double y = altarCenter.getY() + dy;
                double z = altarCenter.getZ() + dz;

                // Spawn a yellow dust particle (you could also use Particle.END_ROD or similar)
                world.spawnParticle(
                        Particle.DUST,
                        x, y, z,
                        0, 0, 0, 0,
                        createDustOptions("#FFFF55")  // bright yellow
                );
            }, step);
        }

        // ------------------------------------------------------------------------------------
        // 3) Move spark from top of spiral to the well
        // ------------------------------------------------------------------------------------
        // After the spiral is done, in the next 40 ticks (2 seconds), we move the spark to the well.
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

                    // Spawn a yellow dust or magic particle
                    world.spawnParticle(
                            Particle.DUST,
                            x, y, z,
                            0, 0, 0, 0,
                            createDustOptions("#FFFF55")
                    );
                }, step);
            }

        }, spiralDurationTicks);  // start after the spiral finishes

        // ------------------------------------------------------------------------------------
        // 4) Play a jingle once spark arrives at the well
        // ------------------------------------------------------------------------------------
        Scheduler.runLater(() -> {
            if (!altarActivated) return;

            // Simple "level up" sound or choose another jingle
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
                    double y = (wellCenter.getY() + 1) + (altarCenter.getY() - wellCenter.getY()) * fraction;
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
        // 6) Spawn the “prize” (the diamond placeholder) at the altar
        // ------------------------------------------------------------------------------------
        Scheduler.runLater(() -> {
            if (!altarActivated) return;

            // "Poof" at altar, spawn the placeholder item
            world.spawnParticle(Particle.LARGE_SMOKE, altarCenter, 10, 0.2, 0.2, 0.2, 0.01);
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                // A small “poof” or explosion sound
                onlinePlayer.playSound(altarCenter, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.AMBIENT, 0.2f, 1.0f);
            }

            // Finally show the item animation at the altar
            Location aboveAltarCenter = altarBlockLocation.clone().add(0, 1, 0);
            AltarManager.showItemAnimation(plugin, aboveAltarCenter, world);

            // Deactivate the altar so ambient (if any) returns to default
            deactivateWishingWellAltar();
        }, spiralDurationTicks + travelDurationTicks + 35L);
    }
}
