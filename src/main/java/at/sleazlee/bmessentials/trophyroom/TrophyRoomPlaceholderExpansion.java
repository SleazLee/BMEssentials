package at.sleazlee.bmessentials.trophyroom;

import at.sleazlee.bmessentials.TextUtils.TextCenter;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * TrophyRoomPlaceholderExpansion integrates with PlaceholderAPI to provide custom placeholders.
 */
public class TrophyRoomPlaceholderExpansion extends PlaceholderExpansion {

    private final Plugin plugin;
    private final TrophyDatabase database;

    /**
     * Constructor for the TrophyRoomPlaceholderExpansion.
     *
     * @param plugin   The main plugin instance.
     * @param database The Database instance.
     */
    public TrophyRoomPlaceholderExpansion(Plugin plugin, TrophyDatabase database) {
        this.plugin = plugin;
        this.database = database;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getIdentifier() {
        return "trophyroom";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }

        // %trophyroom_message%
        if (identifier.equalsIgnoreCase("message")) {
            return getFormattedTrophyRoom(player);
            }

        // Return null if the placeholder is not recognized
        return null;
        }

    /**
     * Sum all mob kills (all KILL_ENTITY for non-player entities).
     */
    private String getTrophyCount(Player player) {
        int count = database.getTrophyCount(player.getUniqueId());
        if (count < 1) {
            return "no";
        } else {
            return String.valueOf(count);
        }
    }

    private String getFormattedTrophyRoom(Player player) {

        String line1 = ChatColor.translateAlternateColorCodes('&', "<white>" + player.getName());
        String line2 = ChatColor.translateAlternateColorCodes('&', "<white>has <yellow>" + getTrophyCount(player) + "<white> Trophies");

        // Center each line
        line1 = TextCenter.center(line1, 29);
        line2 = TextCenter.center(line2, 29);

        // Join all lines.
        return line1 + "\n" +
                line2;
    }

}
