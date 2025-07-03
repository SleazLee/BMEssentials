package at.sleazlee.bmessentials.ImageMaps;

import org.bukkit.map.MapPalette;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Provides Floyd-Steinberg dithering for map images.
 */
public class Ditherer {
    public static byte[] dither(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        byte[] out = new byte[w * h];
        float[][] r = new float[h][w];
        float[][] g = new float[h][w];
        float[][] b = new float[h][w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                Color c = new Color(argb, true);
                r[y][x] = c.getRed();
                g[y][x] = c.getGreen();
                b[y][x] = c.getBlue();
            }
        }

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rr = clamp(Math.round(r[y][x]));
                int gg = clamp(Math.round(g[y][x]));
                int bb = clamp(Math.round(b[y][x]));

                Color sample = new Color(rr, gg, bb);
                byte mapColor;
                if ((img.getRGB(x, y) >> 24) == 0x00) {
                    mapColor = MapPalette.TRANSPARENT;
                } else {
                    mapColor = MapPalette.matchColor(sample);
                }
                out[y * w + x] = mapColor;
                Color newC = MapPalette.getColor(mapColor);

                float er = rr - newC.getRed();
                float eg = gg - newC.getGreen();
                float eb = bb - newC.getBlue();

                diffuse(r, g, b, x, y, er, eg, eb, w, h);
            }
        }
        return out;
    }

    private static void diffuse(float[][] r, float[][] g, float[][] b, int x, int y,
                                float er, float eg, float eb, int w, int h) {
        apply(r, g, b, x + 1, y,     er * 7 / 16f, eg * 7 / 16f, eb * 7 / 16f, w, h);
        apply(r, g, b, x - 1, y + 1, er * 3 / 16f, eg * 3 / 16f, eb * 3 / 16f, w, h);
        apply(r, g, b, x,     y + 1, er * 5 / 16f, eg * 5 / 16f, eb * 5 / 16f, w, h);
        apply(r, g, b, x + 1, y + 1, er * 1 / 16f, eg * 1 / 16f, eb * 1 / 16f, w, h);
    }

    private static void apply(float[][] r, float[][] g, float[][] b, int x, int y,
                              float dr, float dg, float db, int w, int h) {
        if (x < 0 || y < 0 || x >= w || y >= h) return;
        r[y][x] += dr;
        g[y][x] += dg;
        b[y][x] += db;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
