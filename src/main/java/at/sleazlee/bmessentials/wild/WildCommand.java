package at.sleazlee.bmessentials.wild;

import at.sleazlee.bmessentials.huskhomes.HuskHomesAPIHook;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * The WildCommand class implements the /wild command, allowing players to
 * randomly teleport into a *square ring* defined by [Lower, Upper] around
 * a center (default: 256,256). This version ensures players do not spawn
 * above water by checking the blocks directly beneath the teleport location
 * from Y=0 up to Y=344.
 *
 * <p>Steps:
 * <ol>
 *   <li>Check if a version is specified, or pick random if "all".</li>
 *   <li>Retrieve the [Lower, Upper] bounds for that version.</li>
 *   <li>Randomly pick X, Z offsets in [-Upper, +Upper] and accept only if
 *       Chebyshev distance is in [Lower, Upper].</li>
 *   <li>Check if any block from Y=0 up to Y=344 beneath the teleport location is water. If so, retry up to 5 times.</li>
 *   <li>If all 5 attempts result in water, generate a new location and teleport without checking.</li>
 *   <li>Offset by the configured center and teleport the player there.</li>
 * </ol>
 */
public class WildCommand implements CommandExecutor {

    private final WildData wildData; // Holds info about each version's Lower/Upper ring
    private final Logger logger;     // For logging
    private final JavaPlugin plugin; // Reference to main plugin for scheduling tasks

    /**
     * Constructs a new WildCommand.
     *
     * @param wildData the WildData that contains version bounds
     * @param plugin   the main plugin instance (for logging, scheduling, etc.)
     */
    public WildCommand(FileConfiguration config, WildData wildData, JavaPlugin plugin) {
        this.wildData = wildData;
        this.logger = plugin.getLogger();
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Ensure sender is a Player
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c§lWild §cThis command can only be used by a player.");
            return true;
        }

        Player player = (Player) sender;

        // /wild => random version from config
        // /wild 1.21 => specifically that version
        // /wild all => random version
        if (args.length == 0) {
            randomLocation(player, "all");
        } else if (args.length == 1) {
            String version = args[0];
            if (wildData.getVersions().contains(version)) {
                randomLocation(player, version);
            } else if ("all".equalsIgnoreCase(version)) {
                randomLocation(player, "all");
            } else {
                player.sendMessage("§c§lWild §cInvalid version. Try /wild [version or all]");
            }
        } else {
            player.sendMessage("§c§lWild §cToo many arguments. Try /wild [version or all]");
        }

        return true;
    }

    /**
     * Teleports the player to a random location in the specified version's square ring,
     * or picks a random version if "all" is specified. Ensures that the teleport location
     * is not above water by checking the blocks directly beneath the target location
     * from Y=0 up to Y=344. Retries up to 5 times if water is detected.
     *
     * <p>If all 5 attempts result in water, generates a new location and teleports the player
     * there without checking its suitability.</p>
     *
     * @param player  the player to teleport
     * @param version a specific version or "all" to choose randomly from config
     */
    public void randomLocation(Player player, String version) {
        // 1) Determine which version's bounds we'll use
        WildData.CoordinateBounds bounds;
        Random random = new Random();

        if (!"all".equalsIgnoreCase(version)) {
            bounds = wildData.getBounds(version);
            if (bounds == null) {
                player.sendMessage("§c§lWild §cInvalid version specified. Check config.");
                return;
            }
        } else {
            // Pick a random version from the list
            List<String> versions = wildData.getVersions();
            if (versions.isEmpty()) {
                player.sendMessage("§c§lWild §cNo versions defined in config.");
                return;
            }
            String chosen = versions.get(random.nextInt(versions.size()));
            bounds = wildData.getBounds(chosen);
            version = chosen; // For logging purposes
        }

        if (bounds == null) {
            player.sendMessage("§c§lWild §cCould not find ring bounds for version: " + version);
            return;
        }

        // 2) Retrieve Lower, Upper, and center coordinates
        double lower = bounds.getLower();
        double upper = bounds.getUpper();

        double centerX = wildData.getCenterX();
        double centerZ = wildData.getCenterZ();

        // 3) Initialize variables for teleport location
        double finalX = 0;
        double finalZ = 0;
        double finalY = 345; // Fixed Y-coordinate for safety
        int maxRetries = 5;   // Maximum number of attempts to find a suitable location
        int attempt = 0;
        boolean validLocation = false;

        while (attempt < maxRetries && !validLocation) {
            attempt++;

            // 4) Randomly select X and Z offsets within [-Upper, Upper]
            double xOffset = random.nextDouble() * (2 * upper) - upper;
            double zOffset = random.nextDouble() * (2 * upper) - upper;

            // 5) Calculate Chebyshev distance from the center
            double chebDist = Math.max(Math.abs(xOffset), Math.abs(zOffset));

            // 6) Check if the distance is within [Lower, Upper]
            if (chebDist >= lower && chebDist <= upper) {
                // 7) Calculate the absolute teleport coordinates
                finalX = centerX + xOffset;
                finalZ = centerZ + zOffset;

                // 8) Check if any block from Y=0 up to Y=344 is water at (finalX, y, finalZ)
                boolean isAboveWater = false;
                for (int y = 0; y < finalY; y++) {
                    Location checkLocation = new Location(player.getWorld(), finalX, y, finalZ);
                    Material blockType = checkLocation.getBlock().getType();
                    if (blockType == Material.WATER) {
                        isAboveWater = true;
                        logger.info("[Wild] Attempt " + attempt + ": Teleport location (" + finalX + ", " + y + ", " + finalZ + ") is above water. Retrying...");
                        break;
                    }
                }

                if (!isAboveWater) {
                    // Suitable location found
                    validLocation = true;

                    // Log the successful teleport attempt
                    logger.info("[Wild] Teleporting player " + player.getName()
                            + " to version " + version
                            + " at X=" + finalX + ", Z=" + finalZ);

                    // Teleport the player using HuskHomes
                    String worldName = "world";
                    String serverName = "blockminer";

                    if (isPlayerInRegion(player, "spawn")) {
                        // Instant teleport if in spawn region
                        HuskHomesAPIHook.instantTeleportPlayer(player, finalX, finalY, finalZ, 0, 90, worldName, serverName);
                    } else {
                        // Timed teleport otherwise
                        HuskHomesAPIHook.timedTeleportPlayer(player, finalX, finalY, finalZ, 0, 90, worldName, serverName);
                    }
                } else if (attempt == maxRetries) {
                    // After max retries, proceed to generate a new location without checking
                    logger.info("[Wild] Attempt " + attempt + ": Teleport location (" + finalX + ", " + (finalY - 1) + ", " + finalZ + ") is above water. Generating a new location without checking.");

                    // Generate a new random location without checking
                    double newXOffset = random.nextDouble() * (2 * upper) - upper;
                    double newZOffset = random.nextDouble() * (2 * upper) - upper;

                    double newFinalX = centerX + newXOffset;
                    double newFinalZ = centerZ + newZOffset;

                    // Log the unconditional teleport
                    logger.info("[Wild] Teleporting player " + player.getName()
                            + " to version " + version
                            + " at X=" + newFinalX + ", Z=" + newFinalZ + " without water check.");

                    // Teleport the player using HuskHomes
                    String worldName = "world";
                    String serverName = "blockminer";

                    // Teleport the player using HuskHomes without water check
                    if (isPlayerInRegion(player, "spawn")) {
                        // Instant teleport if in spawn region
                        HuskHomesAPIHook.instantTeleportPlayer(player, newFinalX, finalY, newFinalZ, 0, 90, worldName, serverName);
                    } else {
                        // Timed teleport otherwise
                        HuskHomesAPIHook.timedTeleportPlayer(player, newFinalX, finalY, newFinalZ, 0, 90, worldName, serverName);
                    }
                }
            }
        }
    }

    /**
     * Checks if the player is within a specific WorldGuard region by name.
     *
     * @param player     the player
     * @param regionName the WorldGuard region ID to check (e.g., "spawn")
     * @return true if the player is inside the specified region, false otherwise
     */
    private boolean isPlayerInRegion(Player player, String regionName) {
        WorldGuardPlugin wg = getWorldGuard();
        if (wg == null) {
            player.sendMessage("WorldGuard plugin not found!");
            return false;
        }

        // Convert Bukkit location to WorldEdit/WorldGuard location
        com.sk89q.worldedit.util.Location weLoc = BukkitAdapter.adapt(player.getLocation());

        // Get the RegionContainer from WorldGuard
        RegionManager mgr = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(player.getWorld()));
        if (mgr == null) {
            player.sendMessage("Could not get region manager for this world.");
            return false;
        }

        // Get the set of regions applicable at the player's location
        ApplicableRegionSet set = mgr.getApplicableRegions(weLoc.toVector().toBlockPoint());

        // Check if any region in the set matches the specified region name
        for (ProtectedRegion region : set) {
            if (region.getId().equalsIgnoreCase(regionName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves the WorldGuard plugin instance from Bukkit's plugin manager.
     *
     * @return WorldGuardPlugin instance if found, otherwise null
     */
    private WorldGuardPlugin getWorldGuard() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
        if (plugin instanceof WorldGuardPlugin) {
            return (WorldGuardPlugin) plugin;
        }
        return null;
    }
}
