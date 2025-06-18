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

public class TPShopCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return false;
        }

        if (args.length == 0) {
            player.sendMessage("§b§lBMS§7 You need the shops plot number! Try §b/tpshop §8<§b###§8>§7.");
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
            if (!nickname.isEmpty() && nickname.equalsIgnoreCase(query)) {
                matchId = key;
                break;
            }
        }

        if (matchId != null) {
            Bukkit.getServer().dispatchCommand(player, "warp " + matchId);
        } else {
            player.sendMessage("§b§lBMS§7 You need the shops plot number! Try §b/tpshop §8<§b###§8>§7.");
        }

        return true;
    }
}
