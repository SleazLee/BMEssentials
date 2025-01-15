package at.sleazlee.bmessentials.bmefunctions;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Helper class to determine if a player is within a specific WorldGuard region.
 */
public class IsInWorldGuardRegion {

    /**
     * Checks if the player is within a specific WorldGuard region by name.
     *
     * @param player     The player to check.
     * @param regionName The name of the WorldGuard region (case-insensitive).
     * @return true if the player is inside the specified region, false otherwise.
     */
    public static boolean isPlayerInRegion(Player player, String regionName) {
        WorldGuardPlugin wg = getWorldGuard();
        if (wg == null) {
            player.sendMessage("§c§lWild §cWorldGuard plugin not found!");
            Bukkit.getLogger().warning("[IsInWorldGuardRegion] WorldGuard plugin not found!");
            return false;
        }

        // Convert Bukkit location to WorldEdit/WorldGuard location
        com.sk89q.worldedit.util.Location weLoc = BukkitAdapter.adapt(player.getLocation());

        // Get the RegionContainer from WorldGuard
        RegionManager mgr = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(player.getWorld()));
        if (mgr == null) {
            player.sendMessage("§c§lWild §cCould not get region manager for this world.");
            Bukkit.getLogger().warning("[IsInWorldGuardRegion] Could not get RegionManager for world: " + player.getWorld().getName());
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
     * @return WorldGuardPlugin instance if found, otherwise null.
     */
    private static WorldGuardPlugin getWorldGuard() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
        if (plugin instanceof WorldGuardPlugin) {
            return (WorldGuardPlugin) plugin;
        }
        return null;
    }
}