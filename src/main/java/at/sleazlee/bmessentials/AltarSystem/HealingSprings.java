package at.sleazlee.bmessentials.AltarSystem;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import org.bukkit.*;
import org.bukkit.entity.Player;

import static at.sleazlee.bmessentials.art.Art.createDustOptions;

public class HealingSprings {

    private static boolean altarActivated = false;
    private String activeHexColor = "#32CA65";
    private static double theta = 0; // Class-level variable to keep track of the current angle


    public static void startHealingSpringsAmbient(BMEssentials plugin) {
        Scheduler.runTimer(() -> {
            if (!altarActivated) {
                spawnParticleAtAltar(Particle.FIREWORKS_SPARK, false);
            } else {
                spawnParticleAtAltar(Particle.FIREWORKS_SPARK, true);
            }
        }, 0L, 3L); // Reduced delay for faster animation
    }

    private static void spawnParticleAtAltar(Particle particle, boolean active) {
        World world = Bukkit.getWorld("hub");
        double x = -246.5; // Centered on the block
        double y = 61.8;
        double z = -107.5; // Centered on the block
        double radius = 0.8;

        double dx = radius * Math.sin(theta);
        double dz = radius * Math.cos(theta);

        if (active) {
            world.spawnParticle(particle, x + dx, (y - 0.7), z + dz, 0, 0, -0.02, 0, 1);
        } else {
            // For FIREWORKS_SPARK, add a downward velocity
            world.spawnParticle(particle, x + dx, y, z + dz, 0, 0, -0.02, 0, 1);
        }

        // Increment theta by 2*PI/12 to move to the next point in the circle
        theta += 2 * Math.PI / 18;
        if (theta >= 2 * Math.PI) {
            theta = 0; // Reset theta when a full circle is completed
        }
    }

    // Call this method when the altar is activated
    public static void activateHealingSpringsAltar() {
        altarActivated = true;
    }

    // Call this method when the altar activation is done
    public static void deactivateHealingSpringsAltar() {
        altarActivated = false;
    }

    static void playHealingSpringsAnimation(BMEssentials plugin, Location location) {
        World world = location.getWorld();
        Location center = new Location(world, -246.5, 63, -107.5);

        // Step 1: Activate the Healing Springs Altar
        activateHealingSpringsAltar();

        // Step 2: Create a beam of particles from the Lectern to the Spore Blossom
        for (int i = 0; i <= 10; i++) {
            final int finalI = i;
            Scheduler.runLater(() -> {
                double y = center.getY() - 1.0 + (0.15 * finalI);
                world.spawnParticle(Particle.REDSTONE, center.getX(), y, center.getZ(), 0, 0, 0, 0, createDustOptions("#32CA65"));
            }, i * 2L);
        }

        // Step 3: Play initial ambient sounds
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.playSound(center, Sound.BLOCK_BEACON_AMBIENT, SoundCategory.AMBIENT, 0.8f, 0.7f);
            onlinePlayer.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.AMBIENT, 2.0f, 1.0f);
        }

        // Step 4: Play additional ambient sounds after a short delay
        Scheduler.runLater(() -> {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                Location adjustedCenter = center.clone().add(0.0, 0.4, 0.0);
                onlinePlayer.playSound(adjustedCenter, Sound.BLOCK_AMETHYST_CLUSTER_FALL, SoundCategory.AMBIENT, 2.0f, 0.3f);
            }
        }, 20L);

        // Step 5: Create particles that come down from the Spore Blossom
        for (int i = 0; i <= 12; i++) {
            final int finalI = i;
            Scheduler.runLater(() -> {
                double y = center.getY() + 0.6 - (0.05 * finalI);
                world.spawnParticle(Particle.REDSTONE, center.getX(), y, center.getZ(), 0, 0, 0, 0, createDustOptions("#32bbca"));
            }, 50L + i * 2L);
        }

        // Step 6: Create particles that form a sphere
        for (int i = 0; i <= 10; i++) {
            final double r = i * 0.03;
            Scheduler.runLater(() -> {
                for (double theta = 0; theta <= 2 * Math.PI; theta += Math.PI / 10) {
                    for (double phi = 0; phi <= Math.PI; phi += Math.PI / 10) {
                        double x = r * Math.sin(phi) * Math.cos(theta);
                        double y = r * Math.sin(phi) * Math.sin(theta);
                        double z = r * Math.cos(phi);
                        world.spawnParticle(Particle.REDSTONE, center.getX() + x, center.getY() + y, center.getZ() + z, 0, 0, 0, 0, createDustOptions("#FFFFFF"));
                    }
                }
            }, 80L + i * 2L);
        }

        // Step 7: Play sound when the sphere is complete
        Scheduler.runLater(() -> {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.playSound(center, Sound.BLOCK_CONDUIT_ACTIVATE, SoundCategory.AMBIENT, 2.0f, 0.3f);
            }
        }, 100L);

        // Step 8: Show the reward item
        Scheduler.runLater(() -> AltarManager.showItemAnimation(plugin, center, world), 100L);

        // Step 9: Create a puff of smoke and play a sound when the item disappears
        Scheduler.runLater(() -> {
            world.spawnParticle(Particle.SMOKE_NORMAL, center, 10, 0.2, 0.2, 0.2, 0.05);
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.playSound(center, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.AMBIENT, 0.2f, 1.0f);
                deactivateHealingSpringsAltar();
            }
        }, 180L);
    }

  //  createDustOptions()...
}
