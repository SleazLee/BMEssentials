package at.sleazlee.bmessentials.bmefunctions;

import at.sleazlee.bmessentials.BMEssentials;
import me.clip.placeholderapi.PlaceholderAPI;
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


				player.sendMessage("§8§m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §b BM Performance §8§m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m ");
				player.sendMessage("§7Memory∶ §8[§f" + ramBar + "§8] §e" + ramUsed + " GB §7Used, §a" + ramUnused + " GB§7 Free");
				player.sendMessage("§7TPS∶ §8[§71m∶ " + tpsOneMin + "§8] [§75m∶ " + tpsFiveMins + "§8] [§715m∶ " + tpsFifteenMins + "§8]§7 §7 §7 §7 Your ping∶ §8[§a" + ping + "§7ms§8]");
				player.sendMessage("§8§m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m §m ");

				return true;
			}
			if (label.equalsIgnoreCase("bmdiscord")) {
				ChatColor discordColor = ChatColor.of("#7187D7");
				ChatColor linkColor = ChatColor.of("#97A8B3");

				String discordLink = plugin.getConfig().getString("systems.discord.link");

				player.sendMessage(discordColor + "" + "§lDiscord " + linkColor + "Link to join: " + discordColor + discordLink);

				return true;
			}
		}
		return false;
	}
}
