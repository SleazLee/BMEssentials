package at.sleazlee.bmessentials.Shops;

import at.sleazlee.bmessentials.BMEssentials;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public class TPShopTabCompleter implements TabCompleter {

    /**
     * Decodes a nickname stored in Base64 for backwards compatibility.
     * If the value isn't valid Base64, the original string is returned.
     */
    private static String decodeNickname(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return "";
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return encoded;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length >= 1) {
            String prefix = String.join(" ", args).toLowerCase();
            File file = new File(BMEssentials.getInstance().getDataFolder(), "shops.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            List<String> nicknames = new ArrayList<>();
            for (String key : config.getKeys(false)) {
                String nickname = config.getString(key + ".Nickname", "");
                nickname = decodeNickname(nickname);
                if (nickname != null && !nickname.isEmpty()) {
                    nicknames.add(nickname);
                }
            }

            List<String> result = new ArrayList<>();
            for (String name : nicknames) {
                if (name.toLowerCase().startsWith(prefix)) {
                    result.add(name);
                }
            }
            return result;
        }
        return Collections.emptyList();
    }
}
