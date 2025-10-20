package at.sleazlee.bmessentials.PlayerData;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import at.sleazlee.bmessentials.wild.ChunkVersion;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Provides custom placeholders for PlaceholderAPI.
 * Placeholders:
 * - %bme_joindate%: The formatted join date of the player.
 * - %bme_centeredname%: The centered name of the player.
 * - %bme_chunkinfo%: The version or region name where the player is located.
 * - %bme_tps%: Provides tps info depending on if folia is enabled.
 * - %bme_totalvotes%: Lifetime number of votes recorded for the player.
 */
public class BMEPlaceholders extends PlaceholderExpansion {

    private final BMEssentials plugin;

    /**
     * Constructs the BmePlaceholders expansion with a reference to the main plugin.
     *
     * @param plugin the main plugin instance
     */
    public BMEPlaceholders(BMEssentials plugin) {
        this.plugin = plugin;
    }

    /**
     * Checks if the expansion can register.
     *
     * @return true if it can register
     */
    @Override
    public boolean canRegister() {
        return true;
    }

    /**
     * Gets the identifier of the expansion.
     *
     * @return the identifier string
     */
    @Override
    public String getIdentifier() {
        return "bme";
    }

    /**
     * Gets the author of the expansion.
     *
     * @return the author string
     */
    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    /**
     * Gets the version of the expansion.
     *
     * @return the version string
     */
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /**
     * Handles placeholder requests.
     *
     * @param player     the player (can be null)
     * @param identifier the identifier of the placeholder
     * @return the value of the placeholder
     */
    @Override
    public String onPlaceholderRequest(Player player, String identifier) {

        // If the player is null, return an empty string
        if (player == null) {
            return "";
        }

        String uuid = player.getUniqueId().toString();

        // Handle the %bme_joindate% placeholder
        if (identifier.equals("joindate")) {
            long joinDate = plugin.getPlayerDataDBManager().getJoinDate(uuid);
            if (joinDate == -1) {
                return "";
            }
            // Format the timestamp into a readable date string
            Date date = new Date(joinDate);
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy ha z");
            return sdf.format(date);
        }

        // Handle the %bme_totalvotes% placeholder
        if (identifier.equals("totalvotes")) {
            int totalVotes = plugin.getPlayerDataDBManager().getTotalVotes(uuid);
            return String.valueOf(totalVotes);
        }

        if (identifier.equals("currentvotestreak")) {
            int streak = plugin.getPlayerDataDBManager().getCurrentStreak(uuid);
            return String.valueOf(streak);
        }

        if (identifier.equals("bestvotestreak")) {
            int best = plugin.getPlayerDataDBManager().getBestStreak(uuid);
            return String.valueOf(best);
        }

        // Handle the %bme_chunkinfo% placeholder
        if (identifier.equals("chunkinfo")) {
            // Get the version or region name where the player is located
            String ver = ChunkVersion.getVersionFromLocation(player);
            if (ver == null || ver.isEmpty()) {
                return "";
            }
            return ver;
        }

        // Handle the %bme_tps% placeholder
        if (identifier.equals("tps")) {
            return getTPSMessage(player);
        }

        // If the placeholder is not recognized, return null
        return null;
    }

    /**
     * Builds the TPS message depending on whether the server runs Folia or Bukkit.
     *
     * @param player The player requesting the placeholder, used for region TPS on Folia.
     * @return Formatted TPS string with color codes.
     */
    private String getTPSMessage(Player player) {
        double tps1m;
        double tps5m;
        double tps15m;
        String debug;

        if (Scheduler.isFolia()) {
            double[] regionTps = getRegionTps(player.getLocation());
            if (regionTps == null) {
                return "";
            }
            tps1m = regionTps[2];
            tps5m = regionTps[3];
            tps15m = regionTps[4];

        } else {
            double[] tps = Bukkit.getTPS();
            tps1m = tps[0];
            tps5m = tps[1];
            tps15m = tps[2];
        }

        return "<gray>TPS: " + colorizeTps(tps1m) + ", " + colorizeTps(tps5m) + ", " + colorizeTps(tps15m) + "</gray> ";
    }

    /**
     * Applies MiniMessage color tags to a TPS value.
     */
    private String colorizeTps(double tps) {
        String color;
        String noLag = "";
        if (tps > 19.2) {
            color = "green";
        } else if (tps > 17.4) {
            color = "yellow";
        } else {
            color = "red";
        }
        
        if (tps == 20 | tps == 20.0) {
            noLag = "*";
        }
        
        return "<" + color + ">" + noLag + String.format("%.1f", tps) + "</" + color + ">";
    }

    /**
     * Invokes the Folia-only Bukkit.getRegionTPS method via reflection.
     *
     * @param location location to query
     * @return region TPS array or null on failure
     */
    private double[] getRegionTps(Location location) {
        try {
            Method method = Bukkit.class.getMethod("getRegionTPS", Location.class);
            return (double[]) method.invoke(null, location);
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            return null;
        }
    }
}
