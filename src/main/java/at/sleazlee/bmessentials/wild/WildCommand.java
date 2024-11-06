package at.sleazlee.bmessentials.wild;

import at.sleazlee.bmessentials.huskhomes.HuskHomesAPIHook;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;
import java.util.List;
import java.util.logging.Logger;

public class WildCommand implements CommandExecutor {

    private final WildData wildData; // Reference to WildData for version bounds.
    private final Logger logger; // Logger for logging information.

    /**
     * Constructs a WildCommand object.
     *
     * @param wildData The WildData instance containing version bounds.
     * @param plugin   The main plugin instance.
     */
    public WildCommand(WildData wildData, JavaPlugin plugin) {
        this.wildData = wildData;
        this.logger = plugin.getLogger();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Check if the sender is a player.
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args.length == 0) {
                // No arguments provided; teleport to a random version.
                randomLocation(player, "all");
            } else if (args.length == 1) {
                String version = args[0];
                if (wildData.getVersions().contains(version)) {
                    // Teleport to the specified version.
                    randomLocation(player, version);
                } else if (version.equalsIgnoreCase("all")) {
                    // Teleport to a random version.
                    randomLocation(player, "all");
                } else {
                    // Invalid version argument.
                    player.sendMessage("§c§lWild §cInvalid version argument. §fTry /wild [version]");
                }
            } else {
                // Too many arguments provided.
                player.sendMessage("§c§lWild §cToo many arguments. §fTry /wild [version]");
            }
        } else {
            // Command was not run by a player.
            sender.sendMessage("§c§lWild §cThis command can only be used by a player.");
        }
        return true;
    }

    public void randomLocation(Player player, String version) {
        Random random = new Random();
        WildData.CoordinateBounds bounds;

        if (!version.equals("all")) {
            // Get bounds for the specified version.
            bounds = wildData.getBounds(version);
            if (bounds == null) {
                player.sendMessage("§c§lWild §cInvalid version argument. §fTry /wild [version]");
                return;
            }
        } else {
            // Select a random version.
            List<String> versions = wildData.getVersions();
            if (versions.isEmpty()) {
                player.sendMessage("§c§lWild §cNo versions available. Please check the configuration.");
                return;
            }
            int index = random.nextInt(versions.size());
            String randomVersion = versions.get(index);
            bounds = wildData.getBounds(randomVersion);
            version = randomVersion; // Set version for logging.
        }

        if (bounds == null) {
            player.sendMessage("§c§lWild §cCould not retrieve bounds. Please check the configuration.");
            return;
        }

        double lower = bounds.getLower();
        double upper = bounds.getUpper();

        // Log the teleportation event.
        logger.info("[Wild] Teleporting player " + player.getName() + " to version " + version + " bounds: Lower=" + lower + ", Upper=" + upper);

        // Calculate mean and standard deviation for Gaussian distribution.
        double mean = (upper + lower) / 2;
        double stdDev = (upper - lower) / 6;

        // Generate random coordinates using Gaussian distribution.
        double x = random.nextGaussian() * stdDev + mean;
        double y = 345; // Fixed Y position.
        double z = random.nextGaussian() * stdDev + mean;
        float yaw = 0;
        float pitch = 90;

        // Ensure the generated coordinates are within bounds.
        while (x > upper || x < lower) {
            x = random.nextGaussian() * stdDev + mean;
        }
        while (z > upper || z < lower) {
            z = random.nextGaussian() * stdDev + mean;
        }

        // Randomly decide the sign (positive/negative) of the coordinates.
        boolean xPositive = random.nextBoolean();
        boolean zPositive = random.nextBoolean();

        double newx = xPositive ? x : -x;
        double newz = zPositive ? z : -z;

        // Log the final coordinates.
        logger.info("[Wild] Final coordinates for player " + player.getName() + ": x=" + newx + ", z=" + newz);

        // Define server and world names.
        String serverName = "blockminer";
        String worldName = "world";

        // Check if the player is in the 'spawn' region.
        if (isPlayerInRegion(player, "spawn")) {
            // Instant teleport
            HuskHomesAPIHook.instantTeleportPlayer(player, newx, y, newz, yaw, pitch, worldName, serverName);
        } else {
            // Timed teleport
            HuskHomesAPIHook.timedTeleportPlayer(player, newx, y, newz, yaw, pitch, worldName, serverName);
        }
    }

    private boolean isPlayerInRegion(Player player, String regionName) {
        WorldGuardPlugin worldGuard = getWorldGuard();
        if (worldGuard == null) {
            player.sendMessage("WorldGuard plugin not found!");
            return false;
        }

        // Get the player's location in WorldGuard format
        com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(player.getLocation());

        // Get the RegionContainer
        com.sk89q.worldguard.protection.regions.RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();

        // Get the RegionManager for the player's world
        com.sk89q.worldguard.protection.managers.RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));

        if (regions != null) {
            // Get the set of regions at the player's location
            ApplicableRegionSet regionSet = regions.getApplicableRegions(loc.toVector().toBlockPoint());

            // Check if 'spawn' region is in the set
            for (ProtectedRegion region : regionSet) {
                if (region.getId().equalsIgnoreCase(regionName)) {
                    return true;
                }
            }
        } else {
            player.sendMessage("Could not get region manager for your world.");
        }
        return false;
    }

    private WorldGuardPlugin getWorldGuard() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            return null;
        }
        return (WorldGuardPlugin) plugin;
    }
}