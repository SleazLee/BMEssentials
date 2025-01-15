package at.sleazlee.bmessentials.wild;

import at.sleazlee.bmessentials.bmefunctions.IsInWorldGuardRegion;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The /version command class determines which "version ring" (if any)
 * the player's current location falls into, based on the "Bounds" and "Regions" config.
 */
public class ChunkVersion implements CommandExecutor {

    private static WildData wildData = null;   // holds version bounds and regions
    private final JavaPlugin plugin;           // main plugin reference (for logging, etc.)

    /**
     * Constructs a ChunkVersion object to check chunk versions.
     *
     * @param wildData The WildData containing version bounds and regions.
     * @param plugin   The main plugin instance.
     */
    public ChunkVersion(WildData wildData, JavaPlugin plugin) {
        ChunkVersion.wildData = wildData;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Must be a player to use this command
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by a player.");
            return true;
        }

        Player player = (Player) sender;

        // Grab player's location and world
        double x = player.getLocation().getX();
        double z = player.getLocation().getZ();
        String world = player.getWorld().getName();

        // If not in "world", we might not care
        if (!world.equalsIgnoreCase("world")) {
            String msg = "<red>You are not in the main overworld. No version check here.</red>";
            player.sendMessage(MiniMessage.miniMessage().deserialize(msg));
            return true;
        }

        // Determine which version ring (if any) the player is currently in
        String version = getVersionFromLocation(player);

        // Construct a MiniMessage to show the result
        String message;
        if (!version.equals("unmatched")) {
            message = "<yellow><bold>Ver</bold></yellow> <gray>You are currently in a <yellow>"
                    + version + "</yellow> region.";
        } else {
            message = "<yellow><bold>Ver</bold></yellow> <gray>You are currently in an <yellow>unmatched</yellow> region.";
        }
        player.sendMessage(MiniMessage.miniMessage().deserialize(message));

        return true;
    }

    /**
     * Determines which version ring a given Player is in,
     * based on Chebyshev distance from (centerX, centerZ) and WorldGuard regions.
     *
     * @param player The player whose location is to be checked.
     * @return The version string if found, or the region's return value, otherwise "unmatched".
     */
    public static String getVersionFromLocation(Player player) {
        // Grab player's coordinates
        double x = player.getLocation().getX();
        double z = player.getLocation().getZ();

        // Compute Chebyshev distance from the center
        double centerX = wildData.getCenterX();
        double centerZ = wildData.getCenterZ();

        double distX = Math.abs(x - centerX);
        double distZ = Math.abs(z - centerZ);
        double distance = Math.max(distX, distZ);

        // Define Spawn Area bounds
        double spawnLower = 0.0;
        double spawnUpper = 250.0;

        // Check if within Spawn Area
        if (distance >= spawnLower && distance <= spawnUpper) {
            // Player is within Spawn Area, check WorldGuard regions
            for (WildData.Region region : wildData.getRegions()) {
                if (IsInWorldGuardRegion.isPlayerInRegion(player, region.getName())) {
                    return region.getReturnValue();
                }
            }
            // Player is within Spawn Area but not in any defined region
            return "unmatched";
        }

        // Player is outside Spawn Area, check version rings
        for (String version : wildData.getVersions()) {
            WildData.CoordinateBounds cb = wildData.getBounds(version);
            if (cb == null) continue;

            if (distance >= cb.getLower() && distance <= cb.getUpper()) {
                return version; // Found a match
            }
        }

        // If no ring matched, return "unmatched"
        return "unmatched";
    }
}