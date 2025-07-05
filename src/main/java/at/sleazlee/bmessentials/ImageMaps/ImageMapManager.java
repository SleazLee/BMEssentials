package at.sleazlee.bmessentials.ImageMaps;

import at.sleazlee.bmessentials.BMEssentials;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.map.MapView;
import java.util.List;

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
     * Load all saved image maps and reapply their renderers.
     */
    public void loadMaps() {
        if (!config.isConfigurationSection("images")) {
            return;
        }
        Set<String> files = config.getConfigurationSection("images").getKeys(false);
        for (String filename : files) {
            String base = "images." + filename + ".";
            int width = config.getInt(base + "width", 128);
            int height = config.getInt(base + "height", 128);
            List<Integer> ids = config.getIntegerList(base + "maps");
            if (ids.isEmpty()) continue;

            try {
                BufferedImage img = ImageLoader.load(plugin, filename, width, height);
                int tilesX = width / 128;
                for (int i = 0; i < ids.size(); i++) {
                    int id = ids.get(i);
                    MapView view = Bukkit.getMap(id);
                    if (view == null) continue;

                    int sx = (i % tilesX) * 128;
                    int sy = (i / tilesX) * 128;
                    BufferedImage part = img.getSubimage(sx, sy, 128, 128);
                    int[] pixels = Ditherer.dither(part);
                    view.getRenderers().clear();
                    view.addRenderer(new ImageMapRenderer(pixels));
                    view.setTrackingPosition(false);
                    view.setLocked(true);
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to load image maps for " + filename + ": " + ex.getMessage());
            }
        }
    }

    /**
     * Persist information about a set of maps that make up an image.
     */
    public void registerImage(String filename, int width, int height, List<Integer> mapIds) {
        String base = "images." + filename + ".";
        config.set(base + "width", width);
        config.set(base + "height", height);
        config.set(base + "maps", mapIds);
        saveConfig();
    }

    /**
     * Returns true if the given file was already processed into maps.
     */
    public boolean hasImage(String filename) {
        return config.isConfigurationSection("images." + filename);
    }

    /**
     * Gets the list of map ids associated with the given file.
     */
    public List<Integer> getMapIds(String filename) {
        return config.getIntegerList("images." + filename + ".maps");
    }

    private void saveConfig() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save imagemaps.yml: " + e.getMessage());
        }
    }
}
