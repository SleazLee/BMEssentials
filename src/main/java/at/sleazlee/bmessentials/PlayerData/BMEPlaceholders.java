package at.sleazlee.bmessentials.PlayerData;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.wild.ChunkVersion;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Provides custom placeholders for PlaceholderAPI.
 * Placeholders:
 * - %bme_joindate%: The formatted join date of the player.
 * - %bme_centeredname%: The centered name of the player.
 */
public class BMEPlaceholders extends PlaceholderExpansion {

    private BMEssentials plugin;

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

        // Handle the %bme_centeredname% placeholder
        if (identifier.equals("centeredname")) {
            String centeredName = plugin.getPlayerDataDBManager().getCenteredName(uuid);
            if (centeredName == null) {
                return "";
            }
            return ChatColor.translateAlternateColorCodes('&', centeredName);
        }

        // Handle the %bme_chunkinfo% placeholder
        if (identifier.equals("chunkinfo")) {

            // Get player's current location.
            double x = player.getLocation().getX();
            double z = player.getLocation().getZ();

            String ver = ChunkVersion.getVersionFromLocation(x,z);
            if (ver == null) {
                return "";
            }
            return ver;
        }

        // If the placeholder is not recognized, return null
        return null;
    }
}

