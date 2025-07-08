package at.sleazlee.bmessentials.AltarSystem.Altars;

import at.sleazlee.bmessentials.AltarSystem.AltarManager;
import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

import static at.sleazlee.bmessentials.art.Art.createDustOptions;

/**
 * Controls the animation sequence for the "Healing Springs" Altar.
 */
public class HealingSprings {

    /** Whether this altar is currently in an "active" animation state. */
    private static boolean altarActivated = false;

    /**
     * Class-level angle for the circular ambient particle effect.
     */
    private static double theta = 0;

    /**
     * Starts the ambient animation for the Healing Springs altar.
     * Runs on a scheduler indefinitely. The effect changes slightly if activated.
     *
     * @param plugin Main plugin instance.
     */
    public static void startHealingSpringsAmbient(BMEssentials plugin) {
        World world = plugin.getServer().getWorld("world");
        if (world == null) return;
        Location center = new Location(world, 201.5, 61.8, 164.5);
        Scheduler.runTimer(center, () -> {
            spawnParticleAtAltar(Particle.FIREWORK, altarActivated);
        }, 0L, 3L); // Every 3 ticks
    }

    /**
     * Spawns a spiral of firework particles around the altar block.
     * If active, the altitude is slightly changed.
     *
     * @param particle Particle type to spawn.
     * @param active   Whether the altar is currently active.
     */
    private static void spawnParticleAtAltar(Particle particle, boolean active) {
        World world = Bukkit.getWorld("world");
        if (world == null) return;

        // Center coordinates for the altar
        double x = 201.5;
        double y = 61.8;
        double z = 164.5;
        double radius = 0.8;

        // Parametric circle
        double dx = radius * Math.sin(theta);
        double dz = radius * Math.cos(theta);

        if (active) {
            // Slightly lower if active
            world.spawnParticle(particle, x + dx, (y - 0.7), z + dz, 0, 0, -0.02, 0, 1);
        } else {
            world.spawnParticle(particle, x + dx, y, z + dz, 0, 0, -0.02, 0, 1);
        }

        // Increment the angle
        theta += (2 * Math.PI / 18);
        if (theta >= 2 * Math.PI) {
            theta = 0;
        }
    }

    /**
     * Activates the Healing Springs Altar, causing changes in ambient effect.
     */
    public static void activateHealingSpringsAltar() {
        altarActivated = true;
    }

    /**
     * Deactivates the Healing Springs Altar, returning it to idle state.
     */
    public static void deactivateHealingSpringsAltar() {
        altarActivated = false;
    }

    /**
     * Plays the primary animation sequence for Healing Springs.
     * Called by AltarManager upon a valid token activation.
     *
     * @param plugin      Main plugin instance.
     * @param location    The location of the block that was clicked.
     * @param displayType The Material to display in the final floating animation.
     */
    public static void playHealingSpringsAnimation(BMEssentials plugin, Player player, Location location, Material displayType) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        // Center point of the main effect
        Location center = new Location(world, 201.5, 63, 164.5);

        // Step 1: Activate
        activateHealingSpringsAltar();

        // Step 2: Beam of particles from lectern up to blossom
        for (int i = 0; i <= 10; i++) {
            final int finalI = i;
            Scheduler.runLater(center, () -> {
                double y = center.getY() - 1.0 + (0.15 * finalI);
                world.spawnParticle(Particle.DUST, center.getX(), y, center.getZ(),
                        0, 0, 0, 0, createDustOptions("#32CA65"));
            }, i * 2L);
        }

        for (Player players : Bukkit.getOnlinePlayers()) {
            players.playSound(center, Sound.BLOCK_BEACON_AMBIENT, SoundCategory.AMBIENT, 0.8f, 0.7f);
            players.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.AMBIENT, 2.0f, 1.0f);
        }

        // Step 4: Additional ambient sound after a short delay
        Scheduler.runLater(center, () -> {
            for (Player players : Bukkit.getOnlinePlayers()) {
                Location adjustedCenter = center.clone().add(0.0, 0.4, 0.0);
                players.playSound(adjustedCenter, Sound.BLOCK_AMETHYST_CLUSTER_FALL, SoundCategory.AMBIENT, 2.0f, 0.3f);
            }
        }, 20L);

        // Step 5: Particles descending from the blossom
        for (int i = 0; i <= 12; i++) {
            final int finalI = i;
            Scheduler.runLater(center, () -> {
                double y = center.getY() + 0.6 - (0.05 * finalI);
                world.spawnParticle(Particle.DUST, center.getX(), y, center.getZ(),
                        0, 0, 0, 0, createDustOptions("#32bbca"));
            }, 50L + i * 2L);
        }

        // Step 6: Small white sphere formation
        for (int i = 0; i <= 10; i++) {
            final int step = i;
            Scheduler.runLater(center, () -> {
                double r = step * 0.03;
                // Sample points on a sphere
                for (double tTheta = 0; tTheta <= 2 * Math.PI; tTheta += Math.PI / 10) {
                    for (double phi = 0; phi <= Math.PI; phi += Math.PI / 10) {
                        double offsetX = r * Math.sin(phi) * Math.cos(tTheta);
                        double offsetY = r * Math.sin(phi) * Math.sin(tTheta);
                        double offsetZ = r * Math.cos(phi);
                        world.spawnParticle(Particle.DUST,
                                center.getX() + offsetX,
                                center.getY() + offsetY,
                                center.getZ() + offsetZ,
                                0, 0, 0, 0, createDustOptions("#FFFFFF"));
                    }
                }
            }, 80L + step * 2L);
        }

        // Step 7: Conduit activation sound
        Scheduler.runLater(center, () -> {
            for (Player players : Bukkit.getOnlinePlayers()) {
                players.playSound(center, Sound.BLOCK_CONDUIT_ACTIVATE, SoundCategory.AMBIENT, 2.0f, 0.3f);
            }
        }, 100L);

        // Step 8: Show the reward item (using the chosen Material).
        Scheduler.runLater(center, () ->
                        AltarManager.showItemAnimation(plugin, player, center, world, displayType),
        100L
        );

        // Step 9: Puff of smoke and final sound
        Scheduler.runLater(center, () -> {
            world.spawnParticle(Particle.SMOKE, center, 10, 0.2, 0.2, 0.2, 0.05);
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(center, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.AMBIENT, 0.2f, 1.0f);
            }
            deactivateHealingSpringsAltar();
        }, 180L);
    }}