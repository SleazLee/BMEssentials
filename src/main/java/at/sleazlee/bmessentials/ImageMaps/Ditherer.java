package at.sleazlee.bmessentials.ImageMaps;

import java.awt.image.BufferedImage;

/**
 * Returns the raw ARGB pixels of an already-scaled {@link BufferedImage}.
 * With the modern map API we let Bukkit do the final palette quantisation,
 * so we no longer need our own MapPalette calls.
 */
public final class Ditherer {

    private Ditherer() {}          // util class

    public static int[] dither(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int[] out = new int[w * h];
        // Fast native bulk copy
        img.getRGB(0, 0, w, h, out, 0, w);
        return out;
    }
}