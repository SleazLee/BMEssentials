package at.sleazlee.bmessentials.wild;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The /version command class determines which "version ring" (if any)
 * the player's current location falls into, based on the "Bounds" config.
 */
public class ChunkVersion implements CommandExecutor {

    private static WildData wildData = null;   // holds version bounds
    private final JavaPlugin plugin;   // main plugin reference (for logging, etc.)

    /**
     * Constructs a ChunkVersion object to check chunk versions.
     *
     * @param wildData The WildData containing version bounds
     * @param plugin   The main plugin instance
     */
    public ChunkVersion(WildData wildData, JavaPlugin plugin) {
        this.wildData = wildData;
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
            String msg = "<red>You are not in the main overworld. No version check here.";
            player.sendMessage(MiniMessage.miniMessage().deserialize(msg));
            return true;
        }

        // Determine which version ring (if any) the player is currently in
        String version = getVersionFromLocation(x, z);

        // Construct a MiniMessage to show the result
        String message = "<yellow><bold>Ver</bold></yellow> <gray>You are currently in a <yellow>"
                + version + "</yellow> region.";
        player.sendMessage(MiniMessage.miniMessage().deserialize(message));

        return true;
    }

    /**
     * Determines which version ring a given (x, z) coordinate belongs to,
     * based on Chebyshev distance from (256, 256).
     *
     * <p>Chebyshev distance = max(|x - centerX|, |z - centerZ|).</p>
     *
     * @param x The player's X coordinate
     * @param z The player's Z coordinate
     * @return The version string if found, otherwise "unmatched"
     */
    public static String getVersionFromLocation(double x, double z) {
        // 1) Compute the "square distance" from the plugin's center
        double centerX = wildData.getCenterX();
        double centerZ = wildData.getCenterZ();

        double distX = Math.abs(x - centerX);
        double distZ = Math.abs(z - centerZ);
        double distance = Math.max(distX, distZ);

        // 2) Check each version ring to see if distance falls in [lower, upper]
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