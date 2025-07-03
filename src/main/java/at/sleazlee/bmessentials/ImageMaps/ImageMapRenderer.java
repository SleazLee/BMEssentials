package at.sleazlee.bmessentials.ImageMaps;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

/**
 * Renderer that draws raw pixel data onto a map.
 */
public class ImageMapRenderer extends MapRenderer {
    private final byte[] pixels;
    private boolean rendered = false;

    public ImageMapRenderer(byte[] pixels) {
        this.pixels = pixels;
    }

    @Override
    public void render(MapView view, MapCanvas canvas, Player player) {
        if (rendered) return;
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                canvas.setPixel(x, y, pixels[y * w + x]);
            }
        }
        rendered = true;
    }
}
