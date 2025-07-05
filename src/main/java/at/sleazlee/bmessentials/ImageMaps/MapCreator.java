package at.sleazlee.bmessentials.ImageMaps;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

public final class MapCreator {

    public static MapView giveMap(Player player, int[] pixels) {
        World world = player.getWorld();
        MapView view = Bukkit.createMap(world);
        view.getRenderers().clear();
        view.addRenderer(new ImageMapRenderer(pixels));
        view.setTrackingPosition(false);
        view.setLocked(true);

        ItemStack map = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) map.getItemMeta();
        meta.setMapView(view);
        map.setItemMeta(meta);

        player.getInventory().addItem(map);
        return view;
    }

    /**
     * Gives the player an item of an already existing map.
     *
     * @param player the player to give the map item to
     * @param mapId  the id of the existing map
     */
    public static void giveExistingMap(Player player, int mapId) {
        MapView view = Bukkit.getMap(mapId);
        if (view == null) {
            return;
        }
        view.setTrackingPosition(false);
        view.setLocked(true);

        ItemStack map = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) map.getItemMeta();
        meta.setMapView(view);
        map.setItemMeta(meta);

        player.getInventory().addItem(map);
    }
}