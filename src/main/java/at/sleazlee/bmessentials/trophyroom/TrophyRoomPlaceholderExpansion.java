package at.sleazlee.bmessentials.trophyroom;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

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

        // %trophyroom_trophycount%
        if (identifier.equalsIgnoreCase("trophycount")) {
            int count = database.getTrophyCount(player.getUniqueId());
            if (count < 1) {
                return "no";
            } else {
                return String.valueOf(count);
            }
        }

        // Return null if the placeholder is not recognized
        return null;
    }

}
