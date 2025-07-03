package at.sleazlee.bmessentials.ImageMaps;

import at.sleazlee.bmessentials.BMEssentials;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Utility for loading and scaling images from the plugin's Images directory.
 */
public class ImageLoader {
    /**
     * Load an image from the plugin's Images folder and scale it to the given size.
     *
     * @param plugin   plugin instance
     * @param filename file name within the Images directory
     * @param width    target width
     * @param height   target height
     * @return scaled BufferedImage
     * @throws IOException if reading fails
     */
    public static BufferedImage load(BMEssentials plugin, String filename, int width, int height) throws IOException {
        File dir = new File(plugin.getDataFolder(), "Images");
        File file = new File(dir, filename);
        BufferedImage raw = ImageIO.read(file);
        if (raw == null) {
            throw new IOException("Unsupported image: " + filename);
        }
        if (raw.getWidth() == width && raw.getHeight() == height) {
            return raw;
        }
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(raw, 0, 0, width, height, null);
        g.dispose();
        return scaled;
    }
}
