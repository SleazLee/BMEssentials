package at.sleazlee.bmessentials.ImageMaps;

import at.sleazlee.bmessentials.BMEssentials;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.map.MapView;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Handles persisting and restoring custom image maps across server restarts.
 */
public class ImageMapManager {

    private final BMEssentials plugin;
    private final File file;
    private FileConfiguration config;

    public ImageMapManager(BMEssentials plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "imagemaps.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create imagemaps.yml: " + e.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Load all saved maps and reapply their renderers.
     */
    public void loadMaps() {
        if (!config.isConfigurationSection("maps")) {
            return;
        }
        Set<String> keys = config.getConfigurationSection("maps").getKeys(false);
        for (String key : keys) {
            try {
                int id = Integer.parseInt(key);
                String path = "maps." + key + ".";
                String filename = config.getString(path + "file");
                int width = config.getInt(path + "width", 128);
                int height = config.getInt(path + "height", 128);
                MapView view = Bukkit.getMap(id);
                if (view == null || filename == null) continue;

                BufferedImage img = ImageLoader.load(plugin, filename, width, height);
                int[] pixels = Ditherer.dither(img);
                view.getRenderers().clear();
                view.addRenderer(new ImageMapRenderer(pixels));
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to load image map " + key + ": " + ex.getMessage());
            }
        }
    }

    /**
     * Persist information about a new map and apply its renderer immediately.
     */
    public void registerMap(int mapId, String filename, int width, int height) {
        String path = "maps." + mapId + ".";
        config.set(path + "file", filename);
        config.set(path + "width", width);
        config.set(path + "height", height);
        saveConfig();

        // Apply renderer now
        try {
            MapView view = Bukkit.getMap(mapId);
            if (view != null) {
                BufferedImage img = ImageLoader.load(plugin, filename, width, height);
                int[] pixels = Ditherer.dither(img);
                view.getRenderers().clear();
                view.addRenderer(new ImageMapRenderer(pixels));
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to register image map " + mapId + ": " + ex.getMessage());
        }
    }

    private void saveConfig() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save imagemaps.yml: " + e.getMessage());
        }
    }
}
