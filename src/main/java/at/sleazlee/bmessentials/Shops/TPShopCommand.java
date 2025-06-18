package at.sleazlee.bmessentials.Shops;

import at.sleazlee.bmessentials.BMEssentials;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TPShopCommand implements CommandExecutor {

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
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return false;
        }

        if (args.length == 0) {
            player.sendMessage("§b§lBMS§7 You need the shops name! Try §b/tpshop §8<§bName§8>§7.");
            return true;
        }

        String query = String.join(" ", args).trim();

        File file = new File(BMEssentials.getInstance().getDataFolder(), "shops.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        String matchId = null;
        for (String key : config.getKeys(false)) {
            if (key.equalsIgnoreCase(query)) {
                matchId = key;
                break;
            }
            String nickname = config.getString(key + ".Nickname", "");
            nickname = decodeNickname(nickname);
            if (!nickname.isEmpty() && nickname.equalsIgnoreCase(query)) {
                matchId = key;
                break;
            }
        }

        if (matchId != null) {
            Bukkit.getServer().dispatchCommand(player, "warp " + matchId);
        } else {
            player.sendMessage("§b§lBMS§7 You need the shops name! Try §b/tpshop §8<§bName§8>§7.");
        }

        return true;
    }
}
