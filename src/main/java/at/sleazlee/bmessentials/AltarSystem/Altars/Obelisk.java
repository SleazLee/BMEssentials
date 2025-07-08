package at.sleazlee.bmessentials.AltarSystem.Altars;

import at.sleazlee.bmessentials.AltarSystem.AltarManager;
import at.sleazlee.bmessentials.AltarSystem.Rewards.McMMOBoost;
import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import static at.sleazlee.bmessentials.art.Art.createDustOptions;

/**
 * Controls the animation sequence for the "Obelisk" Altar.
 */
public class Obelisk {

    /** Whether this altar is currently in an "active" animation state. */
    private static boolean altarActivated = false;

    // Coordinates for Obelisk top/base, as well as altar location.
    // The world is set dynamically in playObeliskAnimation(...)
    private static final Location OBELISK_TOP = new Location(null, 328, 108, 288);
    private static final Location OBELISK_BASE = new Location(null, 328.5, 71, 288.5);
    private static final Location ALTAR_LOCATION = new Location(null, 323, 72, 288);

    /**
     * Plays the full "Obelisk" animation sequence in multiple phases (charge up, energy descent, wave, etc.).
     *
     * @param plugin            Main plugin instance.
     * @param altarBlockLocation The location of the block that was clicked to activate the altar.
     * @param displayType       The Material to show in the final floating animation.
     */
    public static void playObeliskAnimation(BMEssentials plugin, Player player, Location altarBlockLocation, Material displayType) {
        World world = altarBlockLocation.getWorld();
        if (world == null) return;

        // Ensure these points are in the same world as the clicked block.
        OBELISK_TOP.setWorld(world);
        OBELISK_BASE.setWorld(world);
        ALTAR_LOCATION.setWorld(world);

        // Slightly above the block the player clicked.
        Location altarCenter = altarBlockLocation.clone().add(0.5, 1.5, 0.5);

        altarActivated = true;

        // Phase 1: Energy charge from altar center to obelisk top
        Scheduler.runLater(altarCenter, () -> {
            for (Player players : Bukkit.getOnlinePlayers()) {
                players.playSound(altarCenter, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.2f, 0.9f);
                players.playSound(altarCenter, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 1.5f);
            }

            Vector direction = OBELISK_TOP.toVector().subtract(altarCenter.toVector());
            int chargeDuration = 30; // e.g. 1.5 seconds

            for (int i = 0; i <= chargeDuration; i++) {
                final int step = i;
                Scheduler.runLater(altarCenter, () -> {
                    double progress = easeOutQuad((double) step / chargeDuration);
                    Location currentPos = altarCenter.clone().add(direction.clone().multiply(progress));

                    // Main energy beam
                    world.spawnParticle(Particle.FIREWORK, currentPos, 3, 0.1, 0.1, 0.1, 0.1);

                    // Golden dust highlight
                    world.spawnParticle(Particle.DUST, currentPos, 2, 0, 0, 0, createDustOptions("#E3AC27"));

                    // Bright flash every 5 steps
                    if (step % 5 == 0) {
                        world.spawnParticle(Particle.FLASH, currentPos, 1);
                    }
                }, step);
            }
        }, 0);

        // Phase 2: Enhanced energy descent from obelisk top
        Scheduler.runLater(OBELISK_TOP, () -> {
            int totalSteps = 60;
            for (int i = 0; i <= totalSteps; i++) {
                final int step = i;
                Scheduler.runLater(OBELISK_TOP, () -> {
                    if (!altarActivated) return;
                    double progress = (double) step / totalSteps;
                    double y = OBELISK_TOP.getY() - (36 * progress);
                    double radius = 2.5 * (1 - progress * 0.5);
                    double angle = 6 * Math.PI * progress; // multiple rotations

                    // Spiral with end rods
                    world.spawnParticle(Particle.END_ROD,
                            OBELISK_TOP.getX() + radius * Math.cos(angle),
                            y,
                            OBELISK_TOP.getZ() + radius * Math.sin(angle),
                            2, 0, 0, 0, 0.1);

                    // Occasional golden dust
                    if (step % 2 == 0) {
                        world.spawnParticle(Particle.DUST,
                                OBELISK_TOP.getX(), y, OBELISK_TOP.getZ(),
                                3, 0.3, 0.3, 0.3, createDustOptions("#E3AC27"));
                    }
                }, step);
            }
        }, 30);

        // Phase 3: Purple energy wave from the obelisk base
        Scheduler.runLater(OBELISK_BASE, () -> {
            for (Player players : Bukkit.getOnlinePlayers()) {
                players.playSound(OBELISK_BASE, Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.5f, 0.8f);
            }

            int waveSteps = 20;
            for (int i = 0; i <= waveSteps; i++) {
                final int step = i;
                Scheduler.runLater(OBELISK_BASE, () -> {
                    double radius = 5 * (double) step / waveSteps;
                    for (int j = 0; j < 30; j++) {
                        double angle = 2 * Math.PI * j / 30;
                        double x = OBELISK_BASE.getX() + radius * Math.cos(angle);
                        double z = OBELISK_BASE.getZ() + radius * Math.sin(angle);
                        world.spawnParticle(Particle.DUST, x, OBELISK_BASE.getY() + 0.5, z,
                                1, 0, 0.1, 0, createDustOptions("#6A0DAD"));
                    }
                }, step);
            }
        }, 80);

        // Phase 4: Westward enchantment wave from altar center
        Scheduler.runLater(altarCenter, () -> {
            for (Player players : Bukkit.getOnlinePlayers()) {
                players.playSound(altarCenter, Sound.BLOCK_BEACON_DEACTIVATE, 1.2f, 2.0f);
            }

            for (int i = 0; i < 40; i++) {
                final int step = i;
                Scheduler.runLater(altarCenter, () -> {
                    double x = altarCenter.getX() - (step * 0.15);
                    double y = altarCenter.getY() + 1.0 + Math.sin(step * 0.3) * 0.8;
                    world.spawnParticle(Particle.ENCHANT, x, y, altarCenter.getZ(),
                            8, 0.15, 0.25, 0.15, 0.6);
                }, step);
            }
        }, 110);

        // Phase 5: Explosion + reward + enchant vortex
        Scheduler.runLater(altarCenter, () -> {
            world.spawnParticle(Particle.EXPLOSION, altarCenter, 1);
            world.spawnParticle(Particle.FLASH, altarCenter, 10, 0.5, 0.5, 0.5, 0);

            // Show the final floating item with the chosen Material.
            AltarManager.showItemAnimation(plugin, player, altarCenter, world, displayType);

            // give random Diamond reward
            McMMOBoost mcMMOBoost = new McMMOBoost(plugin);
            mcMMOBoost.mcmmoboost(player);

            createEnchantVortex(world, altarCenter);

            for (Player players : Bukkit.getOnlinePlayers()) {
                players.playSound(altarCenter, Sound.ITEM_TRIDENT_THUNDER, 1.5f, 1.5f);
            }
        }, 150);

        // Reset altar to idle
        Scheduler.runLater(altarCenter, () -> altarActivated = false, 230);
    }

    /**
     * Creates a vertical enchant vortex effect above the center location.
     *
     * @param world  The world in which to spawn particles.
     * @param center The origin of the vortex.
     */
    private static void createEnchantVortex(World world, Location center) {
        int vortexSteps = 60;
        for (int i = 0; i < vortexSteps; i++) {
            final int step = i;
            Scheduler.runLater(center, () -> {
                double angle = 2 * Math.PI * step / 10;
                double radius = 1.8 * (1 - (double) step / vortexSteps);
                double y = center.getY() + (step * 0.15);

                world.spawnParticle(Particle.ENCHANT,
                        center.getX() + radius * Math.cos(angle),
                        y,
                        center.getZ() + radius * Math.sin(angle),
                        2, 0.1, 0.1, 0.1, 0.3);
            }, step);
        }
    }

    /**
     * Smooth "ease out" function for animations.
     *
     * @param t A number from 0 to 1 representing animation progress.
     * @return Smoothed output.
     */
    private static double easeOutQuad(double t) {
        return 1 - (1 - t) * (1 - t);
    }

    /**
     * Starts the ambient "chained flame" effect around the Obelisk Altar
     * when it is not active.
     *
     * @param plugin Main plugin instance.
     */
    public static void startObeliskAmbient(BMEssentials plugin) {
        Scheduler.runTimer(ALTAR_LOCATION, () -> {
            // Skip if the altar is currently running its animation
            if (altarActivated) return;

            World world = plugin.getServer().getWorld("world");
            if (world == null) return;

            // Slight offset from ALTAR_LOCATION
            Location center = ALTAR_LOCATION.clone().add(0.5, -1.0, 0.5);

            // 4 diagonal "chains" of flame around the block
            // 1) +X +Z
            world.spawnParticle(Particle.FLAME, center.clone().add(1, 0, 1), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, center.clone().add(.9, .1, .9), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, center.clone().add(.8, .2, .8), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, center.clone().add(.7, .3, .7), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, center.clone().add(.6, .5, .6), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, center.clone().add(.5, .7, .5), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, center.clone().add(.4, .9, .4), 1, 0, 0, 0, 0);

            // 2) -X +Z
            world.spawnParticle(Particle.FLAME, center.clone().add(-1, 0, 1), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, center.clone().add(-.9, .1, .9), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, center.clone().add(-.8, .2, .8), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, center.clone().add(-.7, .3, .7), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, center.clone().add(-.6, .5, .6), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, center.clone().add(-.5, .7, .5), 1, 0, 0, 0, 0);

            // 3) -X -Z
            world.spawnParticle(Particle.FLAME, center.clone().add(-1, 0, -1), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, center.clone().add(-.9, .1, -.9), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, center.clone().add(-.8, .2, -.8), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, center.clone().add(-.7, .3, -.7), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, center.clone().add(-.6, .5, -.6), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, center.clone().add(-.5, .7, -.5), 1, 0, 0, 0, 0);

            // 4) +X -Z
            world.spawnParticle(Particle.FLAME, center.clone().add(1, 0, -1), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, center.clone().add(.9, .1, -.9), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, center.clone().add(.8, .2, -.8), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, center.clone().add(.7, .3, -.7), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, center.clone().add(.6, .5, -.6), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, center.clone().add(.5, .7, -.5), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, center.clone().add(.4, .9, -.4), 1, 0, 0, 0, 0);

        }, 0L, 6L);
    }
}