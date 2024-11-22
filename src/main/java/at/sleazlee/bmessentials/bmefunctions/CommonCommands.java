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

				String lagTitleStart = TextCenter.center("BM Performance", "dark_gray");
				String lagTitleEnd = TextCenter.fullLineStrike("dark_gray");

				String messageText = lagTitleStart + "<newline><gray>Memory: </gray><dark_gray>[<white><hover:show_text:'Memory refers to RAM (Random Access Memory), which is used by the server to store and quickly access data. The more RAM a server has, the more information it can handle simultaneously, leading to smoother gameplay.'>" + replaceLegacyColors.replaceLegacyColors(ramBar) + "</hover></white>]</dark_gray> <yellow><hover:show_text:'The amount of RAM currently being utilized by the server.'>" + ramUsed + " GB</hover></yellow> <gray>Used, </gray><green><hover:show_text:'The amount of RAM available and not currently in use by the server.'>" + ramUnused + " GB</hover></green><gray> Free</gray><newline><gray>TPS: </gray><dark_gray>[<blue><hover:show_text:'TPS averaged over the last 1 minute.'> 1m: " + tpsOneMin + "</hover></blue>] [<blue><hover:show_text:'TPS averaged over the last 5 minutes.'> 5m: " + tpsFiveMins + "</hover></blue>] [<blue><hover:show_text:'TPS averaged over the last 15 minutes.'> 15m: " + tpsFifteenMins + "</hover></blue>]<gray> Your ping: </gray><dark_gray>[<green><hover:show_text:'Ping measures the latency between your computer and the server, expressed in milliseconds (ms). Lower values mean a more responsive connection.'>" + ping + " ms</hover></green>]</dark_gray><newline>" + lagTitleEnd;
				Component message = MiniMessage.miniMessage().deserialize(messageText);

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
