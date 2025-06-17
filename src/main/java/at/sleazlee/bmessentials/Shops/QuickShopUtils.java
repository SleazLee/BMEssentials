package at.sleazlee.bmessentials.Shops;

import com.ghostchu.quickshop.api.QuickShopAPI;
import com.ghostchu.quickshop.api.shop.Shop;
import com.ghostchu.quickshop.api.shop.ShopManager;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Map;

/** Utility methods for interacting with QuickShop-Hikari. */
public final class QuickShopUtils {
    private QuickShopUtils() {}

    /**
     * Deletes every QuickShop within the axis-aligned box defined by loc1 and loc2.
     * Both locations must be in the same world. If QuickShop is not present this
     * method simply returns.
     *
     * @param loc1 one corner of the cuboid
     * @param loc2 opposite corner of the cuboid
     * @throws IllegalArgumentException if the two locations are in different worlds
     */
    public static void removeShopsInCuboid(Location loc1, Location loc2) {
        if (!loc1.getWorld().equals(loc2.getWorld())) {
            throw new IllegalArgumentException("Both locations must be in the same world");
        }
        World world = loc1.getWorld();

        QuickShopAPI api;
        try {
            api = QuickShopAPI.getInstance();
        } catch (IllegalStateException ex) {
            return; // QuickShop not available
        }

        ShopManager shopManager = api.getShopManager();

        double minX = Math.min(loc1.getX(), loc2.getX());
        double maxX = Math.max(loc1.getX(), loc2.getX());
        double minY = Math.min(loc1.getY(), loc2.getY());
        double maxY = Math.max(loc1.getY(), loc2.getY());
        double minZ = Math.min(loc1.getZ(), loc2.getZ());
        double maxZ = Math.max(loc1.getZ(), loc2.getZ());

        int minChunkX = (int) Math.floor(minX / 16);
        int maxChunkX = (int) Math.floor(maxX / 16);
        int minChunkZ = (int) Math.floor(minZ / 16);
        int maxChunkZ = (int) Math.floor(maxZ / 16);

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                Chunk chunk = world.getChunkAt(cx, cz);
                Map<Location, Shop> shops = shopManager.getShops(chunk);
                if (shops == null) continue;
                for (Shop shop : shops.values()) {
                    Location sLoc = shop.getLocation();
                    if (sLoc.getX() >= minX && sLoc.getX() <= maxX
                        && sLoc.getY() >= minY && sLoc.getY() <= maxY
                        && sLoc.getZ() >= minZ && sLoc.getZ() <= maxZ) {
                        shopManager.deleteShop(shop);
                    }
                }
            }
        }
    }
}
