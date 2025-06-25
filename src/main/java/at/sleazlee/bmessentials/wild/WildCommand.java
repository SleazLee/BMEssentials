package at.sleazlee.bmessentials.wild;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.bmefunctions.IsInWorldGuardRegion;
import at.sleazlee.bmessentials.huskhomes.HuskHomesAPIHook;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
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
    private final WildLocationsDatabase database; // Storage for pregenerated locations
    private final Logger logger;     // For logging
    private final BMEssentials plugin; // Reference to main plugin for scheduling tasks

    /**
     * Constructs a new WildCommand.
     *
     * @param wildData the WildData that contains version bounds.
     * @param plugin   the main plugin instance (for logging, scheduling, etc.).
     */
    public WildCommand(WildData wildData, WildLocationsDatabase database, BMEssentials plugin) {
        this.wildData = wildData;
        this.database = database;
        this.logger = plugin.getLogger();
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Admin subcommands
        if (args.length >= 2 && args[0].equalsIgnoreCase("admin")) {
            if (!sender.hasPermission("bmessentials.wild.admin")) {
                sender.sendMessage("§c§lWild §cYou do not have permission to use this command.");
                return true;
            }

            if (!(sender instanceof org.bukkit.command.ConsoleCommandSender)) {
                sender.sendMessage("§c§lWild §cThis command can only be run from the console.");
                return true;
            }

            if (args.length >= 2) {
                String sub = args[1].toLowerCase();
                switch (sub) {
                    case "gen":
                        if (args.length >= 3) {
                            String bound = args[2];
                            generateLocations(sender, bound);
                        } else {
                            generateAllLocations(sender);
                        }
                        break;
                    case "clear":
                        if (args.length >= 3) {
                            String bound = args[2];
                            database.purgeLocations(bound);
                            sender.sendMessage("§aCleared locations for " + bound);
                        } else {
                            sender.sendMessage("§c§lWild §cUsage: /wild admin clear <bound>");
                        }
                        break;
                    case "count":
                        if (args.length >= 3) {
                            String bound = args[2];
                            int count = database.getLocationCount(bound);
                            sender.sendMessage("§a" + count + " stored locations for " + bound);
                        } else {
                            sender.sendMessage("§c§lWild §cUsage: /wild admin count <bound>");
                        }
                        break;
                    default:
                        sender.sendMessage("§c§lWild §cUsage: /wild admin <gen|clear|count> [bound]");
                        break;
                }
            } else {
                sender.sendMessage("§c§lWild §cUsage: /wild admin <gen|clear|count> [bound]");
            }
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§c§lWild §cThis command can only be used by a player.");
            return true;
        }

        Player player = (Player) sender;

        // /wild => random version from config
        // /wild 1.21 => specifically that version
        // /wild all => random version
        if (args.length == 0) {
            teleportFromDatabase(player, "all");
        } else if (args.length == 1) {
            String version = args[0];
            if (wildData.getVersions().contains(version)) {
                teleportFromDatabase(player, version);
            } else if ("all".equalsIgnoreCase(version)) {
                teleportFromDatabase(player, "all");
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
    /**
     * Teleport the player using a pregenerated location from the database.
     */
    public void teleportFromDatabase(Player player, String version) {
        Random random = new Random();

        String chosenVersion = version;
        if ("all".equalsIgnoreCase(version)) {
            Set<String> versionSet = wildData.getVersions();
            if (versionSet.isEmpty()) {
                player.sendMessage("§c§lWild §cNo versions defined in config.");
                return;
            }
            List<String> versionsList = new ArrayList<>(versionSet);
            chosenVersion = versionsList.get(random.nextInt(versionsList.size()));
        }

        database.getAndRotateLocationAsync(chosenVersion, coords -> {
            if (coords == null) {
                player.sendMessage("§c§lWild §cNo pregenerated locations for that bound.");
                return;
            }

            double finalX = coords[0];
            double finalZ = coords[1];
            double finalY = 345;

            String worldName = "world";
            String serverName = "blockminer";

            if (IsInWorldGuardRegion.isPlayerInRegion(player, "Spawn")) {
                HuskHomesAPIHook.instantTeleportPlayer(player, finalX, finalY, finalZ, 0, 90, worldName, serverName);
            } else {
                HuskHomesAPIHook.timedTeleportPlayer(player, finalX, finalY, finalZ, 0, 90, worldName, serverName);
            }
        });
    }

    /**
     * Legacy random teleport used for generating locations.
     */
    public void randomLocation(Player player, String version) {
        // 1) Choose the version’s bounds
        WildData.CoordinateBounds bounds;
        Random random = new Random();

        if (!"all".equalsIgnoreCase(version)) {
            bounds = wildData.getBounds(version);
            if (bounds == null) {
                player.sendMessage("§c§lWild §cInvalid version specified. Check config.");
                return;
            }
        } else {
            Set<String> versionSet = wildData.getVersions();
            if (versionSet.isEmpty()) {
                player.sendMessage("§c§lWild §cNo versions defined in config.");
                return;
            }
            List<String> versionsList = new ArrayList<>(versionSet);
            String chosen = versionsList.get(random.nextInt(versionsList.size()));
            bounds = wildData.getBounds(chosen);
            version = chosen; // For logging
        }
        if (bounds == null) {
            player.sendMessage("§c§lWild §cCould not find ring bounds for version: " + version);
            return;
        }

        // 2) Get the Lower/Upper values and center coordinates.
        double lower = bounds.getLower();
        double upper = bounds.getUpper();
        double centerX = wildData.getCenterX();
        double centerZ = wildData.getCenterZ();
        double finalY = 345; // fixed Y-coordinate

        // 3) Try generating a valid location.
        int maxRetries = 5;  // maximum water-check attempts
        int attempts = 0;    // count only candidates that are in bounds
        double finalX = 0;
        double finalZ = 0;

        // Use an infinite loop and break once we have a candidate.
        while (true) {
            // Generate random offsets in [-Upper, Upper]
            double xOffset = random.nextDouble() * (2 * upper) - upper;
            double zOffset = random.nextDouble() * (2 * upper) - upper;
            double chebDist = Math.max(Math.abs(xOffset), Math.abs(zOffset));

            // Only accept coordinates within the defined ring.
            if (chebDist < lower || chebDist > upper) {
                continue; // not in bounds; try again without counting this attempt
            }

            // Increase the count for a valid candidate
            attempts++;
            finalX = centerX + xOffset;
            finalZ = centerZ + zOffset;

            logger.info("[Wild] Attempting to check if above water.");
            // If we are still within our allowed water-check attempts, check for water.
            if (attempts <= maxRetries) {
                boolean isAboveWater = false;
                // Check from Y=0 up to (but not including) finalY.
                for (int y = 0; y < finalY; y++) {
                    Location checkLocation = new Location(player.getWorld(), finalX, y, finalZ);
                    Material blockType = checkLocation.getBlock().getType();
                    if (blockType == Material.WATER) {
                        isAboveWater = true;
                        logger.info("[Wild] Attempt " + attempts + ": Teleport location ("
                                + finalX + ", " + y + ", " + finalZ + ") is above water. Retrying...");
                        break;
                    }
                }
                // If water was not found, we’ve got a valid candidate.
                if (!isAboveWater) {
                    break;
                }
                // If this was our final allowed attempt, break out and teleport anyway.
                else if (attempts == maxRetries) {
                    logger.info("[Wild] Attempt " + attempts + ": Teleport location ("
                            + finalX + ", " + (finalY - 1) + ", " + finalZ
                            + ") is above water. Teleporting anyway.");
                    break;
                }
            }
            // If we somehow exceed maxRetries (should not happen since we break at maxRetries),
            // break and use the candidate regardless.
            else {
                logger.info("[Wild] Exceeded water-check attempts. Teleporting without further checks.");
                break;
            }
        }

        // 4) Teleport the player using HuskHomesAPIHook.
        logger.info("[Wild] Teleporting player " + player.getName() + " to version " + version
                + " at X=" + finalX + ", Z=" + finalZ);

        String worldName = "world";
        String serverName = "blockminer";

        if (IsInWorldGuardRegion.isPlayerInRegion(player, "Spawn")) {
            // Instant teleport if in spawn or shop region
            HuskHomesAPIHook.instantTeleportPlayer(player, finalX, finalY, finalZ, 0, 90, worldName, serverName);
        } else {
            // Timed teleport otherwise
            HuskHomesAPIHook.timedTeleportPlayer(player, finalX, finalY, finalZ, 0, 90, worldName, serverName);
        }
    }

    /**
     * Generates and stores random locations for the specified bound until 5000 entries exist.
     */
    private void generateLocations(CommandSender sender, String bound) {
        WildData.CoordinateBounds bounds = wildData.getBounds(bound);
        if (bounds == null) {
            sender.sendMessage("§c§lWild §cUnknown bound " + bound);
            return;
        }

        int current = database.getLocationCount(bound);
        int target = 5000;
        int toGenerate = target - current;
        if (toGenerate <= 0) {
            sender.sendMessage("§aAlready have " + current + " locations for " + bound);
            return;
        }

        double lower = bounds.getLower();
        double upper = bounds.getUpper();
        double centerX = wildData.getCenterX();
        double centerZ = wildData.getCenterZ();
        double finalY = 345;
        Random random = new Random();
        int generated = 0;

        while (generated < toGenerate) {
            double xOffset = random.nextDouble() * (2 * upper) - upper;
            double zOffset = random.nextDouble() * (2 * upper) - upper;
            double chebDist = Math.max(Math.abs(xOffset), Math.abs(zOffset));
            if (chebDist < lower || chebDist > upper) {
                continue;
            }

            int finalX = (int) Math.round(centerX + xOffset);
            int finalZ = (int) Math.round(centerZ + zOffset);

            boolean isWater = false;
            for (int y = 0; y < finalY; y++) {
                Material type = new Location(plugin.getServer().getWorld("world"), finalX, y, finalZ).getBlock().getType();
                if (type == Material.WATER) {
                    isWater = true;
                    break;
                }
            }

            if (!isWater) {
                database.insertLocation(bound, finalX, finalZ);
                sender.sendMessage("§aAdded location: X=" + finalX + " Z=" + finalZ);
                generated++;
            }
        }

        int total = database.getLocationCount(bound);
        sender.sendMessage("§aGenerated " + generated + " locations for " + bound + ". Total: " + total);
        sender.sendMessage("§aGeneration complete.");
    }

    /**
     * Generate locations for all configured bounds.
     */
    private void generateAllLocations(CommandSender sender) {
        for (String ver : wildData.getVersions()) {
            generateLocations(sender, ver);
        }
    }

}