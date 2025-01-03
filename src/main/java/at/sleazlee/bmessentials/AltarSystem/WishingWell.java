package at.sleazlee.bmessentials.AltarSystem;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import org.bukkit.*;
import org.bukkit.Location;

public class WishingWell {

    private static final int STEP_1_DURATION_TICKS = 80; // 4 seconds (Step 1: Spiral duration)
    private static final int STEP_2_BALL_FORM_DURATION = 40; // 2 seconds to form the ball at the top (50 ticks)
    private static final double SPIRAL_RADIUS = 0.5; // The radius of the spiral
    private static final double HEIGHT_CHANGE = 3.0; // Height of the spiral (3 blocks)
    private static final int PARTICLE_COUNT = 60; // Number of particles in the spiral
    private static final double SPIRAL_ROTATIONS = 2.0; // Number of full rotations in the spiral

    private static boolean animationInProgress = false; // Prevents reactivating while already running

    /**
     * Plays the Wishing Well animation starting from Step 1 (Spiraling particles).
     *
     * @param plugin       The plugin instance.
     * @param altarLocation The location of the altar being activated.
     */
    public static void playWishingWellAnimation(BMEssentials plugin, Location altarLocation) {
        // Ensure we don't start multiple animations simultaneously
        if (animationInProgress) return;
        animationInProgress = true;

        World world = altarLocation.getWorld();
        Location startLocation = altarLocation.clone().add(0.5, 0.0, 0.5); // Centered above the Altar
        Location stopLocation = altarLocation.clone().add(0.5, 3.0, 0.5); // The ball location (3 blocks above)

        // Step 1: Create a rising spiral of particles
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            final int currentStep = i;

            // Schedule each particle's position at the appropriate time
            Scheduler.runLater(() -> {
                // Calculate the progress of the particle (0 to 1)
                double progress = (double) currentStep / PARTICLE_COUNT;

                // Calculate the height (starts at well level, rises to 3 blocks)
                double currentHeight = progress * HEIGHT_CHANGE;

                // Calculate the angle of rotation for the spiral (0 to 2π)
                double angle = progress * SPIRAL_ROTATIONS * 2 * Math.PI;

                // Calculate x and z offsets for the spiral
                double offsetX = SPIRAL_RADIUS * Math.cos(angle);
                double offsetZ = SPIRAL_RADIUS * Math.sin(angle);

                // Calculate the particle position
                Location particleLocation = startLocation.clone().add(offsetX, currentHeight, offsetZ);

                // Spawn the particle (end_rod)
                world.spawnParticle(Particle.END_ROD, particleLocation, 2, 0, 0, 0, 0);

                // Optional: Play sound effect when particles start
                if (currentStep == 0) {
                    world.playSound(startLocation, Sound.BLOCK_END_PORTAL_FRAME_FILL, 1f, 1f);
                }
            }, (STEP_1_DURATION_TICKS / PARTICLE_COUNT) * currentStep);
        }

        // Schedule transition to Step 2 (Pause and form magic ball at the top)
        Scheduler.runLater(() -> pauseAndFormBall(plugin, stopLocation), STEP_1_DURATION_TICKS);
    }

    /**
     * Step 2: Pauses at the top and forms a glowing ball of particles.
     *
     * @param plugin       The plugin instance.
     * @param ballLocation The location where the ball forms (3 blocks above the altar).
     */
    private static void pauseAndFormBall(BMEssentials plugin, Location ballLocation) {
        World world = ballLocation.getWorld();

        // Gradually form the glowing ball over a few seconds
        for (int i = 0; i < STEP_2_BALL_FORM_DURATION; i++) {
            final int currentStep = i;

            Scheduler.runLater(() -> {
                // Generate particles in a shrinking spherical pattern
                double radius = 0.3; // Slightly larger radius for the forming ball
                int particleDensity = 10; // More particles for denser appearance

                for (double theta = 0; theta <= Math.PI; theta += Math.PI / particleDensity) {
                    for (double phi = 0; phi <= 2 * Math.PI; phi += Math.PI / particleDensity) {
                        double x = radius * Math.sin(theta) * Math.cos(phi);
                        double y = radius * Math.sin(theta) * Math.sin(phi);
                        double z = radius * Math.cos(theta);
                        Location particleLocation = ballLocation.clone().add(x, y, z);
                        world.spawnParticle(Particle.END_ROD, particleLocation, 1, 0, 0, 0, 0);
                    }
                }

                // Optional: Add sound during ball formation
                if (currentStep == STEP_2_BALL_FORM_DURATION / 2) {
                    world.playSound(ballLocation, Sound.BLOCK_BEACON_AMBIENT, 1f, 1f);
                }
            }, currentStep * 2); // 2 ticks between each iteration
        }

        // Schedule transition to Step 3 (Shoot into well)
        Scheduler.runLater(() -> shootIntoWell(plugin, ballLocation), STEP_2_BALL_FORM_DURATION);
    }

    /**
     * Step 3: Shoots the particle ball into the center of the Wishing Well.
     *
     * @param plugin       The plugin instance.
     * @param startLocation The starting location (where the ball is formed).
     */
    private static void shootIntoWell(BMEssentials plugin, Location startLocation) {
        World world = startLocation.getWorld();
        Location wellCenter = startLocation.clone().add(-3, -3, 0); // Example well target (adjust as needed)
        int steps = 20; // Smooth steps for the particle beam

        for (int i = 0; i <= steps; i++) {
            final int currentStep = i;

            Scheduler.runLater(() -> {
                // Calculate the position of the particle along the trajectory
                double progress = (double) currentStep / steps;
                double x = startLocation.getX() + (wellCenter.getX() - startLocation.getX()) * progress;
                double y = startLocation.getY() + (wellCenter.getY() - startLocation.getY()) * progress;
                double z = startLocation.getZ() + (wellCenter.getZ() - startLocation.getZ()) * progress;

                Location particleLocation = new Location(world, x, y, z);

                // Spawn particles to simulate the shooting effect
                world.spawnParticle(Particle.FLASH, particleLocation, 5, 0.01, 0.01, 0.01, 0.02);

                // Play sound effect when the particle reaches the well
                if (currentStep == steps) {
                    world.playSound(wellCenter, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.8f);
                }
            }, currentStep * 2); // Spread out steps over 40 ticks (smooth beam)
        }
    }
}