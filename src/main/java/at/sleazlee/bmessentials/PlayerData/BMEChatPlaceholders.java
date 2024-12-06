package at.sleazlee.bmessentials.PlayerData;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.TextUtils.TextCenter;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This expansion provides the %bm_player_stats% placeholder which returns a
 * nicely formatted, centered multiline string of player stats.
 */
public class BMEChatPlaceholders extends PlaceholderExpansion {

    private BMEssentials plugin;

    public BMEChatPlaceholders(BMEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getIdentifier() {
        return "bm"; // or "bme" if you prefer. Just ensure uniqueness.
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
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

        if (identifier.equalsIgnoreCase("player_stats")) {
            return getFormattedStats(player);
        }

        return null;
    }

    private String getFormattedStats(Player player) {
        // Fetch placeholders from PAPI:
        // If null or empty, default to "0"
        String mcmmoPower = fetchOrDefault(player, "%mcmmo_power_level%", "0");
        String balanceDollars = fetchOrDefault(player, "%gemseconomy_balance_dollars_formatted%", "0");
        String balanceVPs = fetchOrDefault(player, "%gemseconomy_balance_VPs_formatted%", "0");

        // Fetch join date from your player data DB (assuming %bme_joindate% works)
        long joinDateLong = plugin.getPlayerDataDBManager().getJoinDate(player.getUniqueId().toString());
        String joinDateStr = "";
        if (joinDateLong != -1) {
            Date date = new Date(joinDateLong);
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy ha z");
            joinDateStr = sdf.format(date);
        } else {
            joinDateStr = "Unknown";
        }

        // Compute playtime from Statistic.PLAY_TIME (measured in ticks)
        // 1 second = 20 ticks
        int playTimeTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        int totalSeconds = playTimeTicks / 20;
        int days = totalSeconds / 86400; // 86400 sec in a day
        int hours = (totalSeconds % 86400) / 3600;
        int minutes = ((totalSeconds % 86400) % 3600) / 60;

        // Compute mined blocks (sum of all MINE_BLOCK stats)
        int minedBlocks = sumAllBlockStats(player, Statistic.MINE_BLOCK);

        // Player kills
        int playerKills = player.getStatistic(Statistic.PLAYER_KILLS);

        // Mob kills: sum all KILL_ENTITY except player
        int mobKills = sumAllEntityKillStats(player);

        // Enchanted times: ENCHANT_ITEM
        int itemsEnchanted = player.getStatistic(Statistic.ITEM_ENCHANTED);

        // Blocks placed: sum all USE_ITEM for block materials
        int placedBlocks = sumAllBlockStats(player, Statistic.USE_ITEM);

        // Deaths
        int deaths = player.getStatistic(Statistic.DEATHS);

        // Now let's format the lines:
        // Original lines:
        // "&7Name:&f {player_name} &7 &aPower LVL: {mcmmo_power_level}"
        // "&6First joined: &6{playerdata_JoinDate}"
        // "&7Money:&b {gemseconomy_balance_dollars_formatted} &8/ &e{gemseconomy_balance_VPs_formatted}"
        // "&7Playtime: &c{statistic_time_played:days} Days &6{statistic_time_played:hours} Hours &e{statistic_time_played:minutes} Minutes"
        // ""
        // "&7Mined {statistic_mine_block} blocks &8/ &7Killed {statistic_player_kills} players"
        // "&7Killed {statistic_mob_kills} mobs &8/ &7Enchanted {statistic_item_enchanted} times"
        // "&7Placed {statistic_use_item} blocks &8/ &7Died {statistic_deaths} times"

        String line1 = ChatColor.translateAlternateColorCodes('&', "&7Name:&f " + player.getName() + " &7 &aPower LVL: " + mcmmoPower);
        String line2 = ChatColor.translateAlternateColorCodes('&', "&6First joined: &6" + joinDateStr);
        String line3 = ChatColor.translateAlternateColorCodes('&', "&7Money:&b " + balanceDollars + " &8/ &e" + balanceVPs);
        String line4 = ChatColor.translateAlternateColorCodes('&',
                "&7Playtime: &c" + days + " Days &6" + hours + " Hours &e" + minutes + " Minutes");
        String line5 = "";
        String line6 = ChatColor.translateAlternateColorCodes('&',
                "&7Mined " + minedBlocks + " blocks &8/ &7Killed " + playerKills + " players");
        String line7 = ChatColor.translateAlternateColorCodes('&',
                "&7Killed " + mobKills + " mobs &8/ &7Enchanted " + itemsEnchanted + " times");
        String line8 = ChatColor.translateAlternateColorCodes('&',
                "&7Placed " + placedBlocks + " blocks &8/ &7Died " + deaths + " times");

        // Center each line
        line1 = TextCenter.center(line1, 62);
        line2 = TextCenter.center(line2, 62);
        line3 = TextCenter.center(line3, 62);
        line4 = TextCenter.center(line4, 62);
        line6 = TextCenter.center(line6, 62);
        line7 = TextCenter.center(line7, 62);
        line8 = TextCenter.center(line8, 62);

        // Join all lines. The resulting string will be placed where %bm_player_stats% is used.
        // Depending on how ChatControlRed handles multiline placeholders, we may need to use \n.
        // Typically, a single placeholder returns a single line. If ChatControlRed supports multiline,
        // we can join them with "\n". Otherwise, consider returning a single line or separate placeholders.
        return line1 + "\n" +
                line2 + "\n" +
                line3 + "\n" +
                line4 + "\n" +
                line5 + "\n" +
                line6 + "\n" +
                line7 + "\n" +
                line8;
    }

    /**
     * Fetch a PlaceholderAPI placeholder and return a default value if null or empty.
     */
    private String fetchOrDefault(Player player, String placeholder, String def) {
        String result = PlaceholderAPI.setPlaceholders(player, placeholder);
        if (result == null || result.isEmpty() || result.equals(placeholder)) {
            return def;
        }
        return result;
    }

    /**
     * Sum all block stats for a given Statistic that takes a Material parameter.
     * For example, MINE_BLOCK or USE_ITEM.
     */
    private int sumAllBlockStats(Player player, Statistic stat) {
        int sum = 0;
        for (Material mat : Material.values()) {
            try {
                if (stat == Statistic.MINE_BLOCK && mat.isBlock()) {
                    sum += player.getStatistic(Statistic.MINE_BLOCK, mat);
                } else if (stat == Statistic.USE_ITEM && mat.isBlock()) {
                    sum += player.getStatistic(Statistic.USE_ITEM, mat);
                }
            } catch (Exception ignored) {
                // Some stats might throw exceptions if not applicable.
            }
        }
        return sum;
    }

    /**
     * Sum all mob kills (all KILL_ENTITY for non-player entities).
     */
    private int sumAllEntityKillStats(Player player) {
        int sum = 0;
        for (EntityType type : EntityType.values()) {
            if (type.isAlive() && type != EntityType.PLAYER && hasKillEntityStat(type)) {
                try {
                    sum += player.getStatistic(Statistic.KILL_ENTITY, type);
                } catch (Exception ignored) {
                }
            }
        }
        return sum;
    }

    private boolean hasKillEntityStat(EntityType type) {
        // Not all entity types have stats tracked, but generally all living entities should.
        // We'll assume all alive entities have a KILL_ENTITY stat.
        return true;
    }

}
