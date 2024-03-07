package at.sleazlee.bmessentials.trophyroom.util;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.trophyroom.data.Data;
import java.util.Map;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceHolderApiHook extends PlaceholderExpansion {
    public PlaceHolderApiHook() {
    }

    public @NotNull String getIdentifier() {
        return "trophyroom";
    }

    public @NotNull String getAuthor() {
        return String.join(", ", BMEssentials.getMain().getDescription().getAuthors());
    }

    public @NotNull String getVersion() {
        return BMEssentials.getMain().getDescription().getVersion();
    }

    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (!identifier.equalsIgnoreCase(this.getIdentifier())) {
            return null;
        } else {
            Data data = Data.getData();
            Map trophies;
            return (trophies = data.getTrophies(player.getUniqueId().toString().replaceAll("-", ""))) != null ? "" + trophies.size() : "0";
        }
    }
}
