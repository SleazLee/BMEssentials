package at.sleazlee.bmessentials.bmefunctions;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.TextUtils.TextCenter;
import at.sleazlee.bmessentials.TextUtils.replaceLegacyColors;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommonCommands implements CommandExecutor {

	private final BMEssentials plugin;

	public CommonCommands(BMEssentials plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender instanceof Player) {

			Player player = (Player) sender;

			if (label.equalsIgnoreCase("playtime")) {
				String parsedDays = PlaceholderAPI.setPlaceholders(player, "%statistic_time_played:days%");
				String parsedHours = PlaceholderAPI.setPlaceholders(player, "%statistic_time_played:hours%");
				String parsedMinutes = PlaceholderAPI.setPlaceholders(player, "%statistic_time_played:minutes%");
				String parsedSeconds = PlaceholderAPI.setPlaceholders(player, "%statistic_time_played:seconds%");

				player.sendMessage("§a§lPlaytime §7You've played for §c" + parsedDays + " Days§7, §6" + parsedHours + " Hours§7, §e" + parsedMinutes + " Minutes§7, and §f" + parsedSeconds + " Seconds§7!");

				return true;
			}
			if (label.equalsIgnoreCase("lag")) {

				String ramBar = PlaceholderAPI.setPlaceholders(player, "%progress_bar_{server_ram_free}_m:{server_ram_total}_l:20_c:§a■_p:§e■_r:§c■%");
				String ramUsed = PlaceholderAPI.setPlaceholders(player, "%math_0:1_((131072/{server_ram_total})*({server_ram_used}))/(1024)%");
				String ramUnused = PlaceholderAPI.setPlaceholders(player, "%math_0:1_((131072/{server_ram_total})*({server_ram_free}))/(1024)%");

				String tpsOneMin = PlaceholderAPI.setPlaceholders(player, "%server_tps_1_colored%");
				String tpsFiveMins = PlaceholderAPI.setPlaceholders(player, "%server_tps_5_colored%");
				String tpsFifteenMins = PlaceholderAPI.setPlaceholders(player, "%server_tps_15_colored%");
				String ping = PlaceholderAPI.setPlaceholders(player, "%player_colored_ping%");

				String lagTitleStart = TextCenter.center("&bBM Performance", "dark_gray");
				String lagTitleEnd = TextCenter.fullLineStrike("dark_gray");

				String messageText = lagTitleStart + "<newline><hover:show_text:'<gold><bold>Memory</bold><gray> refers to <yellow>RAM (Random Access Memory)<gray>, which is used by the server to store and quickly access data. The more RAM a server has, the more information it can handle simultaneously, leading to <green>smoother<gray> gameplay.'><gray>Memory: </hover><dark_gray>[<white>" + ramBar + "</white>] <hover:show_text:'<red><bold>Used Ram:</bold><newline><gray>The amount of RAM currently being utilized by the server.'><yellow>" + ramUsed + " GB <gray>Used</hover>, <hover:show_text:'<green><bold>Free Ram:</bold><newline><gray>The amount of RAM available and not currently in use by the server.'><green>" + ramUnused + " GB<gray> Free</hover><newline><hover:show_text:'<aqua><bold>TPS</bold> (Ticks Per Second)<gray> is a measure of the servers performance. In Minecraft, the game operates on a system of <green>\"ticks,\"<gray> with the ideal rate being 20 TPS, meaning the server is processing <yellow>20 game cycles<gray> every second.<newline><newline><green>20 TPS<gray>: Indicates optimal server performance with no noticeable lag.<newline><yellow>18 TPS and above<gray>: Minor lag may occur but is generally unnoticeable.<newline><red>Below 18 TPS<gray>: Players may start experiencing server-side lag, affecting gameplay smoothness.'><gray>TPS: </hover><hover:show_text:'<gray>TPS averaged over the last <aqua>1<gray> minute.'><dark_gray>[<gray>1m: " + tpsOneMin + "<dark_gray>]</hover> <hover:show_text:'<gray>TPS averaged over the last <aqua>5<gray> minutes.'><dark_gray>[<gray>5m: " + tpsFiveMins + "<dark_gray>]</hover> <hover:show_text:'<gray>TPS averaged over the last <aqua>15<gray> minutes.'><dark_gray>[<gray>15m: " + tpsFifteenMins + "<dark_gray>]</hover><hover:show_text:'<yellow><bold>Ping</bold><gray> measures the latency between your computer and the server, expressed in <aqua>milliseconds (ms)<gray>. It indicates how quickly your actions are communicated to the server and vice versa.<newline><newline><green>Lower Ping<gray>: Results in faster response times and smoother gameplay.<newline><red>Higher Ping<gray>: Can lead to delays, making actions feel laggy or unresponsive.'><gray> Your ping: <dark_gray>[<green>" + ping + " ms</hover><dark_gray>]<newline>" + lagTitleEnd;
				Component message = MiniMessage.miniMessage().deserialize(replaceLegacyColors.replaceLegacyColors(messageText));

				sender.sendMessage(message);
				return true;
			}
			if (label.equalsIgnoreCase("bmdiscord")) {
				ChatColor discordColor = ChatColor.of("#7187D7");
				ChatColor linkColor = ChatColor.of("#97A8B3");

				String discordLink = plugin.getConfig().getString("Systems.Discord.Link");

				player.sendMessage(discordColor + "" + "§lDiscord " + linkColor + "Link to join: " + discordColor + discordLink);

				return true;
			}
		}
		return false;
	}
}
