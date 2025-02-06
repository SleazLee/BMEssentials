package at.sleazlee.bmessentials.AltarSystem;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Random;

import static at.sleazlee.bmessentials.art.Art.createDustOptions;

/**
 * Controls the animation sequence for the "Wishing Well" Altar.
 */
public class WishingWell {

    /** Whether this altar is currently in an "active" animation state. */
    private static boolean altarActivated = false;

    /** Used for random color generation in the ambient effect. */
    private static final Random random = new Random();

    // Some sample colors used in effects
    private static final Color SPARK_COLOR_START = Color.fromRGB(0xFF, 0xEA, 0x29); // Bright yellow
    private static final Color SPARK_COLOR_END   = Color.fromRGB(0xFF, 0xD7, 0x00); // Gold

    /**
     * Plays the primary animation sequence for the Wishing Well.
     * Called by AltarManager upon a valid token activation.
     *
     * @param plugin            Main plugin instance.
     * @param altarBlockLocation The location of the block that was clicked.
     * @param displayType       The Material to display in the final floating animation.
     */
    public static void playWishingWellAnimation(BMEssentials plugin, Player player, Location altarBlockLocation, Material displayType) {
        altarActivated = true;

        World world = altarBlockLocation.getWorld();
        if (world == null) {
            return;
        }

        Location altarCenter = altarBlockLocation.clone().add(0.5, 1.0, 0.5);
        Location wellCenter = new Location(world, 207.5, 71.5, 312.5);

        // Animation timings (in ticks)
        int spiralDuration = 60;   // 3 seconds
        int travelDuration = 30;   // 1.5 seconds
        int mistDuration   = 20;   // 1 second
        int strikeDuration = 10;   // 0.5 seconds

        // 1) Spiral rising from the altar
        for (int i = 0; i <= spiralDuration; i++) {
            final int step = i;
            Scheduler.runLater(() -> {
                if (!altarActivated) return;

                double progress = easeOutQuad((double) step / spiralDuration);
                double y = altarCenter.getY() + 1.5 * progress;
                double radius = 0.5 * (1 - progress * 0.3);
                double angle = 4 * Math.PI * progress; // 2 rotations

                double x = altarCenter.getX() + radius * Math.cos(angle);
                double z = altarCenter.getZ() + radius * Math.sin(angle);

                Color color = interpolateColor(SPARK_COLOR_START, SPARK_COLOR_END, progress);
                spawnColoredParticle(world, x, y, z, color);

                // Extra spark every 3 steps
                if (step % 3 == 0) {
                    spawnColoredParticle(world, x, y + 0.1, z, color);
                }
            }, step);
        }

        // 2) Accelerated travel (arc) to the well
        Scheduler.runLater(() -> {
            Location start = altarCenter.clone().add(0, 1.5, 0);
            Vector controlPoint = calculateArcControlPoint(start, wellCenter, 2.0);

            for (int i = 0; i <= travelDuration; i++) {
                final int step = i;
                Scheduler.runLater(() -> {
                    if (!altarActivated) return;

                    double t = (double) step / travelDuration;
                    t = easeInQuad(t); // accelerate
                    Location pos = calculateBezierPoint(start, controlPoint.toLocation(world), wellCenter, t);

                    Color travelColor = interpolateColor(SPARK_COLOR_END, Color.PURPLE, t);
                    spawnColoredParticle(world, pos.getX(), pos.getY(), pos.getZ(), travelColor);

                    // Slight motion blur every 2 steps
                    if (step % 2 == 0) {
                        world.spawnParticle(Particle.FIREWORK, pos, 1, 0, 0, 0, 0.05);
                    }
                }, step);
            }
        }, spiralDuration);

        // 3) Harmonic jingle at the well
        Scheduler.runLater(() -> {
            if (!altarActivated) return;
            playHarmonicSound(wellCenter, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.5f, 0.8f);
            playHarmonicSound(wellCenter, Sound.BLOCK_NOTE_BLOCK_BELL, 1.2f, 1.0f);
        }, spiralDuration + travelDuration);

        // 4) Purple mist vortex around the well
        Scheduler.runLater(() -> {
            for (int i = 0; i < mistDuration; i++) {
                final int step = i;
                Scheduler.runLater(() -> {
                    if (!altarActivated) return;

                    double radius = 1.0 - (double) step / mistDuration;
                    double angle = 2 * Math.PI * step / 5;
                    double y = wellCenter.getY() + 0.5 + (0.1 * step);

                    for (int j = 0; j < 3; j++) {
                        double x = wellCenter.getX() + radius * Math.cos(angle + j * Math.PI / 2);
                        double z = wellCenter.getZ() + radius * Math.sin(angle + j * Math.PI / 2);
                        world.spawnParticle(Particle.WITCH, x, y, z, 2, 0.1, 0.1, 0.1, 0);
                    }
                }, step);
            }
        }, spiralDuration + travelDuration + 5);

        // 5) Lightning strike from the well back to altar
        Scheduler.runLater(() -> {
            Vector direction = wellCenter.toVector().subtract(altarCenter.toVector()).normalize();
            for (int i = 0; i < strikeDuration; i++) {
                final int step = i;
                Scheduler.runLater(() -> {
                    if (!altarActivated) return;

                    double t = (double) step / strikeDuration;
                    Location pos = wellCenter.clone().add(direction.multiply(t * 3));

                    world.spawnParticle(Particle.ELECTRIC_SPARK, pos, 2, 0.1, 0.1, 0.1, 0);
                    world.spawnParticle(Particle.DRAGON_BREATH, pos, 1, 0, 0, 0, 0.1);
                }, step);
            }
        }, spiralDuration + travelDuration + mistDuration + 5);

        // 6) Final reveal: Show the reward item
        Scheduler.runLater(() -> {
            if (!altarActivated) return;

            Location prizeLocation = altarCenter.clone().add(0, 0.6, 0);
            AltarManager.showItemAnimation(plugin, player, prizeLocation, world, displayType);

            // Extra visuals
            world.spawnParticle(Particle.FALLING_WATER, prizeLocation, 100, 0.5, 0.5, 0.5, 0.5);
            world.spawnParticle(Particle.END_ROD, prizeLocation, 30, 0.3, 0.3, 0.3, 0.1);

            // Sounds
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(prizeLocation, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                p.playSound(prizeLocation, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.7f);
            }
        }, spiralDuration + travelDuration + mistDuration + strikeDuration + 10);

        // Reactivate ambient effect after the full sequence
        Scheduler.runLater(() -> altarActivated = false,
                spiralDuration + travelDuration + mistDuration + strikeDuration + 90
        );
    }

    /**
     * Computes a control point for a simple arc between two locations.
     *
     * @param start  The starting location.
     * @param end    The ending location.
     * @param height How much additional height to add in the middle.
     * @return A Vector representing the control point.
     */
    private static Vector calculateArcControlPoint(Location start, Location end, double height) {
        Vector startVec = start.toVector();
        Vector endVec = end.toVector();
        Vector mid = startVec.clone().add(endVec.clone().subtract(startVec).multiply(0.5));
        return mid.add(new Vector(0, height, 0));
    }

    /**
     * Computes a point on a 2D Quadratic Bezier curve B(t) = (1-t)^2 * p0 + 2(1-t)t * p1 + t^2 * p2.
     *
     * @param p0 Start point (Location).
     * @param p1 Control point (Location).
     * @param p2 End point (Location).
     * @param t  A value from 0.0 to 1.0.
     * @return A new Location representing the interpolated point.
     */
    private static Location calculateBezierPoint(Location p0, Location p1, Location p2, double t) {
        double u = 1 - t;
        double tt = t * t;
        double uu = u * u;

        return p0.clone().multiply(uu)
                .add(p1.clone().multiply(2 * u * t))
                .add(p2.clone().multiply(tt));
    }

    /**
     * Plays two pitches of the given sound for a pleasant "harmony" effect.
     *
     * @param loc       The location to play the sound.
     * @param sound     The Sound enum.
     * @param volume    Volume of the base sound.
     * @param basePitch The base pitch.
     */
    private static void playHarmonicSound(Location loc, Sound sound, float volume, float basePitch) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(loc, sound, volume, basePitch);
            p.playSound(loc, sound, volume * 0.8f, basePitch * 1.2f);
        }
    }

    /**
     * Spawns a single colored dust particle at the given coordinates.
     *
     * @param world The world to spawn in.
     * @param x     The X coordinate.
     * @param y     The Y coordinate.
     * @param z     The Z coordinate.
     * @param color The color to use, converted to hex code.
     */
    private static void spawnColoredParticle(World world, double x, double y, double z, Color color) {
        String hex = String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
        world.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0, createDustOptions(hex));
    }

    /**
     * Eases out with a quadratic curve: 1 - (1 - t)^2
     *
     * @param t A value from 0 to 1.
     * @return The eased value.
     */
    private static double easeOutQuad(double t) {
        return 1 - (1 - t) * (1 - t);
    }

    /**
     * Eases in with a quadratic curve: t^2
     *
     * @param t A value from 0 to 1.
     * @return The eased value.
     */
    private static double easeInQuad(double t) {
        return t * t;
    }

    /**
     * Interpolates between two Colors in RGB space.
     *
     * @param start Starting color.
     * @param end   Ending color.
     * @param t     Value from 0.0 to 1.0
     * @return The interpolated Color.
     */
    private static Color interpolateColor(Color start, Color end, double t) {
        int r = (int) (start.getRed()   + (end.getRed()   - start.getRed())   * t);
        int g = (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * t);
        int b = (int) (start.getBlue()  + (end.getBlue()  - start.getBlue())  * t);
        return Color.fromRGB(r, g, b);
    }

    /**
     * Ambient effect around the Wishing Well location when not active.
     * Spawns random-colored DUST particles in a small radius.
     *
     * @param plugin Main plugin instance.
     */
    public static void startWishingWellAmbient(BMEssentials plugin) {
        Scheduler.runTimer(() -> {
            if (altarActivated) return;

            Location altarBlockLocation = new Location(plugin.getServer().getWorld("world"), 210.5, 71, 309.5);
            World world = altarBlockLocation.getWorld();
            if (world == null) return;

            // Random RGB
            int r = random.nextInt(256);
            int g = random.nextInt(256);
            int b = random.nextInt(256);

            Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(r, g, b), 1.0F);
            world.spawnParticle(
                    Particle.DUST,
                    altarBlockLocation,
                    15, // count
                    0.9, 0.3, 0.9, // offsets
                    0.01,
                    dustOptions
            );

        }, 0L, 15L);
    }
}