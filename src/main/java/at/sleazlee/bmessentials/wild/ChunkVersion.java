package at.sleazlee.bmessentials.wild;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The ChunkVersion command class allows players to check which version-generated chunk they are in.
 * If their current coordinates fit within the defined bounds of a version, it reports that version.
 * Otherwise, it defaults to "1.16".
 */
public class ChunkVersion implements CommandExecutor {

    private static WildData wildData;
    private final JavaPlugin plugin;

    /**
     * Constructs a ChunkVersion object.
     *
     * @param wildData The WildData instance providing version bounds.
     * @param plugin   The main plugin instance.
     */
    public ChunkVersion(WildData wildData, JavaPlugin plugin) {
        ChunkVersion.wildData = wildData;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the sender is a player.
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by a player.");
            return true;
        }

        Player player = (Player) sender;

        // Get player's current location.
        double x = player.getLocation().getX();
        double z = player.getLocation().getZ();
        String world = player.getWorld().getName();

        // Check if player is in the "world".
        if (!world.equals("world")) {
            String message = "<#ff3300><bold>Ver</bold> <red>You are not currently in the wild.";
            player.sendMessage(MiniMessage.miniMessage().deserialize(message));
            return true;
        }

        // Determine which version (if any) the player is currently in.
        String version = getVersionFromLocation(x, z);

        // Use MiniMessage for formatting:
        String message = "<yellow><bold>Ver</bold></yellow> <gray>You are currently in a <yellow>"
                + version + "</yellow> generated chunk.";
        player.sendMessage(MiniMessage.miniMessage().deserialize(message));

        return true;
    }


    /**
     * Determines the version of the chunk at the given coordinates by first checking if they are within a defined
     * rectangular region. If so, returns "1.14". If not, it checks against configured version bounds.
     *
     * @param x The player's X coordinate.
     * @param z The player's Z coordinate.
     * @return The version string if found; otherwise "1.16" as a default.
     */
    public static String getVersionFromLocation(double x, double z) {
        // Spawn Inner
        double spawnInnerMinX = 160;
        double spawnInnerMaxX = 337;
        double spawnInnerMinZ = 160;
        double spawnInnerMaxZ = 329;

        // shops
        double shopsMinX = 70;
        double shopsMaxX = 428;
        double shopsMinZ = 66;
        double shopsMaxZ = 424;

        // Spawn Inner
        double spawnOuterMinX = 21;
        double spawnOuterMaxX = 505;
        double spawnOuterMinZ = 3;
        double spawnOuterMaxZ = 514;

        if (x >= spawnInnerMinX && x <= spawnInnerMaxX && z >= spawnInnerMinZ && z <= spawnInnerMaxZ) {
            return "spawn";
        }
        if (x >= shopsMinX && x <= shopsMaxX && z >= shopsMinZ && z <= shopsMaxZ) {
            return "shops";
        }
        if (x >= spawnOuterMinX && x <= spawnOuterMaxX && z >= spawnOuterMinZ && z <= spawnOuterMaxZ) {
            return "spawn";
        }

        // If not in the special area, check the configured version bounds.
        double absX = Math.abs(x);
        double absZ = Math.abs(z);

        for (String version : wildData.getVersions()) {
            WildData.CoordinateBounds bounds = wildData.getBounds(version);
            if (bounds != null) {
                double lower = bounds.getLower();
                double upper = bounds.getUpper();

                boolean withinX = absX >= lower && absX <= upper;
                boolean withinZ = absZ >= lower && absZ <= upper;

                if (withinX && withinZ) {
                    return version;
                }
            }
        }

        // If no match was found, return default version "1.16"
        return "1.16";
    }
}
