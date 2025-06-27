package at.sleazlee.bmessentials.wild;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import at.sleazlee.bmessentials.bmefunctions.IsInWorldGuardRegion;
import at.sleazlee.bmessentials.huskhomes.HuskHomesAPIHook;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.HeightMap;
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
 *   <li>Check if the block directly beneath the teleport location is water.</li>
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
                            database.purgeLocationsAsync(bound, () ->
                                    sender.sendMessage("§aCleared locations for " + bound));
                        } else {
                            sender.sendMessage("§c§lWild §cUsage: /wild admin clear <bound>");
                        }
                        break;
                    case "count":
                        if (args.length >= 3) {
                            String bound = args[2];
                            database.getLocationCountAsync(bound, count ->
                                    sender.sendMessage("§a" + count + " stored locations for " + bound));
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
     * or picks a random version if "all" is specified.
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

        database.getNextLocationAsync(chosenVersion, coords -> {
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
     * Generates and stores random locations for the specified bound until
     * {@link WildLocationsDatabase#MAX_LOCATIONS} entries exist.
     */
    private void generateLocations(CommandSender sender, String bound) {
        WildData.CoordinateBounds bounds = wildData.getBounds(bound);
        if (bounds == null) {
            sender.sendMessage("§c§lWild §cUnknown bound " + bound);
            return;
        }

        database.getLocationCountAsync(bound, current -> {
            int target = WildLocationsDatabase.MAX_LOCATIONS;
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
            int[] generated = {0};

            World world = plugin.getServer().getWorld("world");
            if (world == null) {
                sender.sendMessage("§c§lWild §cWorld not found.");
                return;
            }

            Scheduler.Task[] taskHolder = new Scheduler.Task[1];
            taskHolder[0] = Scheduler.runTimer(() -> {
                if (generated[0] >= toGenerate) {
                    taskHolder[0].cancel();
                    database.getLocationCountAsync(bound, total -> {
                        sender.sendMessage("§aGenerated " + generated[0] + " locations for " + bound + ". Total: " + total);
                        sender.sendMessage("§aGeneration complete.");
                    });
                    return;
                }

            double xOffset = random.nextDouble() * (2 * upper) - upper;
            double zOffset = random.nextDouble() * (2 * upper) - upper;
            double chebDist = Math.max(Math.abs(xOffset), Math.abs(zOffset));
            if (chebDist < lower || chebDist > upper) {
                return;
            }

            int finalX = (int) Math.round(centerX + xOffset);
            int finalZ = (int) Math.round(centerZ + zOffset);

            Location loc = new Location(world, finalX, 0, finalZ);
            Scheduler.run(loc, () -> {
                boolean isWater = false;


                //Find out if the Surface block is Water or Lava.

                // 1. find the Y of the highest non-air block at x,z
                Block SurfaceY = world.getHighestBlockAt(finalX, finalZ, HeightMap.WORLD_SURFACE);
                // 2. grab that block
                Block surfaceBlock = world.getBlockAt(finalX, SurfaceY.getY(), finalZ);
                // 3. check its type
                Material m = surfaceBlock.getType();
                if (m == Material.WATER || m == Material.LAVA) {
                    // it’s water!
                    isWater = true;
                }

                if (!isWater) {
                    database.insertLocationAsync(bound, finalX, finalZ, success -> {
                        if (success) {
                            sender.sendMessage("§aAdded location: X=" + finalX + " Z=" + finalZ);
                            generated[0]++;
                        }
                    });
                }
            });
        }, 0L, 5L);
        });
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