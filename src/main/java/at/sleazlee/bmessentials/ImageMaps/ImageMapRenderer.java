package at.sleazlee.bmessentials.ImageMaps;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.Color;

/**
 * Renders a 128 × 128 ARGB pixel buffer onto a map.
 */
public class ImageMapRenderer extends MapRenderer {

    private final int[] pixels;       // ARGB
    private boolean rendered = false;

    public ImageMapRenderer(int[] pixels) {
        this.pixels = pixels;
    }

    @Override
    public void render(MapView view, MapCanvas canvas, Player player) {
        if (rendered) return;

        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                int argb = pixels[y * 128 + x];
                canvas.setPixelColor(x, y, new Color(argb, true));
            }
        }
        rendered = true;
    }
}