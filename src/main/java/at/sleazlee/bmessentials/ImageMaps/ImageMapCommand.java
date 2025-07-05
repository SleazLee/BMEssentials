package at.sleazlee.bmessentials.ImageMaps;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.map.MapView;

import javax.imageio.ImageIO;
import java.io.File;

import java.awt.image.BufferedImage;

/**
 * Command to convert an image file into a Minecraft map.
 */
public class ImageMapCommand implements CommandExecutor {
    private final BMEssentials plugin;

    public ImageMapCommand(BMEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can run this command.");
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /imagemap <filename> [width] [height]");
            return true;
        }
        String filename = args[0];

        File imgDir = new File(plugin.getDataFolder(), "Images");
        File file = new File(imgDir, filename);
        if (!file.exists()) {
            player.sendMessage(ChatColor.RED + "Image not found: " + filename);
            return true;
        }

        if (plugin.getImageMapManager().hasImage(filename)) {
            for (int id : plugin.getImageMapManager().getMapIds(filename)) {
                MapCreator.giveExistingMap(player, id);
            }
            player.sendMessage(ChatColor.GREEN + "Loaded existing maps for " + filename + ".");
            return true;
        }

        BufferedImage original;
        try {
            original = ImageIO.read(file);
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Failed to read image: " + e.getMessage());
            return true;
        }
        if (original == null) {
            player.sendMessage(ChatColor.RED + "Unsupported image format.");
            return true;
        }

        int width = args.length > 1 ? parseIntOr(args[1], original.getWidth()) : original.getWidth();
        int height = args.length > 2 ? parseIntOr(args[2], original.getHeight()) : original.getHeight();

        if (width % 128 != 0 || height % 128 != 0) {
            player.sendMessage(ChatColor.RED + "Width and height must be divisible by 128.");
            return true;
        }

        player.sendMessage(ChatColor.GRAY + "Processing image...");
        int w = width;
        int h = height;

        Scheduler.runAsync(() -> {
            try {
                BufferedImage img = ImageLoader.load(plugin, filename, w, h);
                int tilesX = w / 128;
                int tilesY = h / 128;
                java.util.List<int[]> slices = new java.util.ArrayList<>();
                for (int ty = 0; ty < tilesY; ty++) {
                    for (int tx = 0; tx < tilesX; tx++) {
                        BufferedImage part = img.getSubimage(tx * 128, ty * 128, 128, 128);
                        slices.add(Ditherer.dither(part));
                    }
                }
                Scheduler.run(() -> {
                    java.util.List<Integer> ids = new java.util.ArrayList<>();
                    for (int[] pixels : slices) {
                        MapView view = MapCreator.giveMap(player, pixels);
                        ids.add(view.getId());
                    }
                    plugin.getImageMapManager().registerImage(filename, w, h, ids);
                });
            } catch (Exception e) {
                Scheduler.run(() -> player.sendMessage(ChatColor.RED + "Failed: " + e.getMessage()));
            }
        });
        return true;
    }

    private int parseIntOr(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ignore) {
            return fallback;
        }
    }
}
