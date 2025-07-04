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

        ItemStack map = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) map.getItemMeta();
        meta.setMapView(view);
        map.setItemMeta(meta);

        player.getInventory().addItem(map);
        return view;
    }
}