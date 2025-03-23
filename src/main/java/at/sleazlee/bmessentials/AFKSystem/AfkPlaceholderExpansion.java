package at.sleazlee.bmessentials.AFKSystem;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Placeholder expansion for providing a boolean AFK status.
 * <p>Usage: %afk_status% returns "true" if the player is AFK, "false" otherwise.</p>
 */
public class AfkPlaceholderExpansion extends PlaceholderExpansion {

    private final Plugin plugin;

    /**
     * Constructs the placeholder expansion.
     *
     * @param plugin the plugin instance.
     */
    public AfkPlaceholderExpansion(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getIdentifier() {
        return "afk";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /**
     * Processes placeholder requests.
     *
     * @param player     the player whose placeholder is requested.
     * @param identifier the placeholder identifier.
     * @return "true" or "false" for the identifier "status", otherwise null.
     */
    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }
        if ("status".equalsIgnoreCase(identifier)) {
            boolean afk = AfkManager.getInstance().isAfk(player);
            return String.valueOf(afk);
        }
        return null;
    }
}
