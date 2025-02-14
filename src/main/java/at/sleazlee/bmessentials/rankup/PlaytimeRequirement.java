package at.sleazlee.bmessentials.rankup;

import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Requirement that checks if a player has met the required playtime.
 */
public class PlaytimeRequirement implements Requirement {
    private final JavaPlugin plugin;
    private final int requiredMinutes;
    private final String originalRequirement;

    /**
     * Constructs a PlaytimeRequirement.
     *
     * @param plugin       The plugin instance (used for logging).
     * @param playtimeStr  The playtime string from the config (e.g., "30 minutes").
     */
    public PlaytimeRequirement(JavaPlugin plugin, String playtimeStr) {
        this.plugin = plugin;
        this.originalRequirement = playtimeStr;
        this.requiredMinutes = parsePlaytime(playtimeStr);
    }

    @Override
    public boolean isMet(Player player) {
        if (requiredMinutes <= 0) return true;
        int playerPlaytime = getPlayerPlaytimeMinutes(player);
        return playerPlaytime >= requiredMinutes;
    }

    @Override
    public String getDenyMessage() {
        return "&c&lRanks &cYou need &e" + originalRequirement + "&c of playtime to rank up.";
    }

    /**
     * Parses a human-readable playtime string into minutes.
     *
     * Supported formats:
     * - "X seconds"
     * - "X minutes"
     * - "X hours"
     * - "X days"
     *
     * @param playtimeStr The playtime string.
     * @return The playtime in minutes, or -1 if invalid.
     */
    private int parsePlaytime(String playtimeStr) {
        Pattern pattern = Pattern.compile("(\\d+)\\s*(seconds|minutes|hours|days)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(playtimeStr.trim());
        if (matcher.matches()) {
            int amount = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();
            switch (unit) {
                case "seconds":
                    return Math.max(1, amount / 60); // At least 1 minute if seconds are provided
                case "minutes":
                    return amount;
                case "hours":
                    return amount * 60;
                case "days":
                    return amount * 60 * 24;
                default:
                    return -1;
            }
        }
        return -1; // Invalid format
    }

    /**
     * Retrieves the player's playtime in minutes using the server's statistics.
     *
     * @param player The player whose playtime is to be retrieved.
     * @return The playtime in minutes.
     */
    private int getPlayerPlaytimeMinutes(Player player) {
        int playTimeTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        // 20 ticks = 1 second, 60 seconds = 1 minute
        return (playTimeTicks / 20) / 60;
    }
}