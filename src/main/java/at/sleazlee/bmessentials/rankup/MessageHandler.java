package at.sleazlee.bmessentials.rankup;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Handles message formatting and placeholder replacements.
 */
public class MessageHandler {
    /**
     * Formats a message by replacing placeholders and translating color codes.
     *
     * @param message The message template.
     * @param player  The player to replace placeholders for.
     * @return The formatted message.
     */
    public String formatMessage(String message, Player player) {
        if (message == null) return "";
        // Replace all PlaceholderAPI placeholders
        message = PlaceholderAPI.setPlaceholders(player, message);
        // Translate color codes
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
