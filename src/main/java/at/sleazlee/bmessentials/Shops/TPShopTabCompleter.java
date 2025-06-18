package at.sleazlee.bmessentials.Shops;

import at.sleazlee.bmessentials.BMEssentials;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TPShopTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length >= 1) {
            String prefix = String.join(" ", args).toLowerCase();
            File file = new File(BMEssentials.getInstance().getDataFolder(), "shops.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            List<String> nicknames = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            for (String key : config.getKeys(false)) {
                String nickname = config.getString(key + ".Nickname", "");
                if (nickname != null && !nickname.isEmpty()) {
                    nicknames.add(nickname);
                }
                ids.add(key);
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
