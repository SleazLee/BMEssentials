package at.sleazlee.bmessentials.ImageMaps;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.map.MapView;

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
        int defW = plugin.getConfig().getInt("Systems.ImageMaps.DefaultWidth", 128);
        int defH = plugin.getConfig().getInt("Systems.ImageMaps.DefaultHeight", 128);
        int width = defW;
        int height = defH;
        if (args.length > 1) {
            try { width = Integer.parseInt(args[1]); } catch (NumberFormatException ignore) {}
        }
        if (args.length > 2) {
            try { height = Integer.parseInt(args[2]); } catch (NumberFormatException ignore) {}
        }
        player.sendMessage(ChatColor.GRAY + "Processing image...");
        int w = width;
        int h = height;
        Scheduler.runAsync(() -> {
            try {
                BufferedImage img = ImageLoader.load(plugin, filename, w, h);
                int[] pixels = Ditherer.dither(img);          // NEW: int[], not byte[]
                Scheduler.run(() -> {
                    MapView view = MapCreator.giveMap(player, pixels);
                    plugin.getImageMapManager().registerMap(view.getId(), filename, w, h);
                });
            } catch (Exception e) {
                Scheduler.run(() -> player.sendMessage(ChatColor.RED + "Failed: " + e.getMessage()));
            }
        });
        return true;
    }
}
