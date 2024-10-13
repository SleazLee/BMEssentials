package at.sleazlee.bmessentials.SpawnSystems;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class McMMOBoost implements CommandExecutor {

	private final BMEssentials plugin;

	public McMMOBoost(BMEssentials plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender instanceof ConsoleCommandSender) {
			if (args.length > 0) {
				String playerName = args[0];
				Player player = Bukkit.getPlayer(playerName);
				double randomValue = Math.random();
				double totalChance = plugin.getConfig().getDouble("Systems.SpawnSystems.McmmoBoost.TotalChance");

				if (player != null) {
					if (randomValue <= totalChance) {
						mcmmoboost(player, playerName);
					}
				} else {
					sender.sendMessage("§c§lBM §cPlayer not found or not online.");
				}
			} else {
				sender.sendMessage("§c§lBM §cInvalid Syntax! §7Try §b/mcmmobonus <player>§7.");
			}
			return true;
		} else {
			// Inform the player that this command can only be executed by the console
			sender.sendMessage("§c§lBM §cThis command can only be executed by the console.");

			return true;
		}
	}

	public void mcmmoboost(Player player, String playerName) {
		double randomValue = Math.random();
		boolean has10Percent = false;
		boolean has50Percent = false;
		boolean has150Percent = false;
		boolean hasDouble = false;
		String newReward;
		double tenPercentChance = plugin.getConfig().getDouble("Systems.SpawnSystems.McmmoBoost.TenPercentChance");
		double fiftyPercentChance = (tenPercentChance + plugin.getConfig().getDouble("Systems.SpawnSystems.McmmoBoost.FiftyPercentChance"));
		double oneHundredFiftyPercentChance = (fiftyPercentChance + plugin.getConfig().getDouble("Systems.SpawnSystems.McmmoBoost.OneHundredFiftyPercentChance"));

		if (player.hasPermission("mcmmo.perks.xp.10percentboost.*")) {
			has10Percent = true;
		}
		if (player.hasPermission("mcmmo.perks.xp.50percentboost.*")) {
			has50Percent = true;
		}
		if (player.hasPermission("mcmmo.perks.xp.150percentboost.*")) {
			has150Percent = true;
		}
		if (player.hasPermission("mcmmo.perks.xp.double.*")) {
			hasDouble = true;
		}

		if (randomValue <= tenPercentChance) {
			newReward = "10Percent";
		} else if (randomValue <= fiftyPercentChance) {
			newReward = "50Percent";
		} else if (randomValue <= oneHundredFiftyPercentChance) {
			newReward = "150Percent";
		} else {
			newReward = "Double";
		}

		switch(newReward) {
			case "10Percent":
				if (has10Percent) {
					switchAndUpdate("mcmmo.perks.xp.10percentboost.*", 120, player, playerName, "10% boost");
				} else {
					giveReward("mcmmo.perks.xp.10percentboost.*", 120, playerName);
					player.sendMessage("§6§lMcMMO §7Nice! You received a §a10% boost§7 to your stats! It ends in §b2 hours§7!");
				}
				break;
			case "50Percent":
				if (has50Percent) {
					switchAndUpdate("mcmmo.perks.xp.50percentboost.*", 60, player, playerName, "50% boost");
				} else {
					giveReward("mcmmo.perks.xp.50percentboost.*", 60, playerName);
					player.sendMessage("§6§lMcMMO §7Great! You received a §a50% boost§7 to your stats! It ends in §b1 hour§7!");
				}
				break;
			case "150Percent":
				if (has150Percent) {
					switchAndUpdate("mcmmo.perks.xp.150percentboost.*", 45, player, playerName, "150% boost");
				} else {
					giveReward("mcmmo.perks.xp.150percentboost.*", 45, playerName);
					player.sendMessage("§6§lMcMMO §7Wow! You received a §a150% boost§7 to your stats! It ends in §b45 minutes§7!");
				}
				break;
			case "Double":
				if (hasDouble) {
					switchAndUpdate("mcmmo.perks.xp.double.*", 30, player, playerName, "200% boost");
				} else {
					giveReward("mcmmo.perks.xp.double.*", 30, playerName);
					player.sendMessage("§6§lMcMMO §7WOAH! You received a §a200% boost§7 to your stats! It ends in §b30 minutes§7!");
				}
				break;
			default:

		}
	}

	public void switchAndUpdate(String permission, int minutes, Player player, String playerName, String boostName) {
		String parsedTime = PlaceholderAPI.setPlaceholders(player, "%luckperms_expiry_time_" + permission + "%");
		int hour = hoursExtract(parsedTime);
		int min = ((minuteExtract(parsedTime) + minutes) + 1);
		String formattedTimeResult = formatTime(hour, min);


		player.sendMessage("§6§lMcMMO §7Nice! §7Your §a" + boostName + "§7 was extended and has §b" + formattedTimeResult + "§7 remaining!");
		// player.sendMessage("String: (" + parsedTime + "), Hours: (" + hour + "), Minutes: " + min );

		// unsets the permission
		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		String unSetCommandBuilder = "lp user " + playerName + " permission unsettemp " + permission;
		Bukkit.dispatchCommand(console, unSetCommandBuilder);

		// sets the permission
		String setCommandBuilder = "lp user " + playerName + " permission settemp "+ permission + " true " + hour + "h" + min +"m";
		Scheduler.runLater(new Runnable() {
			@Override
			public void run() {
				Bukkit.dispatchCommand(console, setCommandBuilder);
			}
		}, 20L); // Delay in ticks, 20 ticks = 1 second
	}

	public void giveReward(String permission, int minutes, String playerName) {
		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		String setCommandBuilder = "lp user " + playerName + " permission settemp " + permission + " true " + minutes +"m";
		Bukkit.dispatchCommand(console, setCommandBuilder);
	}

	public int hoursExtract(String timeString) {
		int hours = 0;
		Pattern hourPattern = Pattern.compile("(\\d+)h");
		Matcher hourMatcher = hourPattern.matcher(timeString);
		if (hourMatcher.find()) {
			hours = Integer.parseInt(hourMatcher.group(1));
		}
		return hours;
	}

	public int minuteExtract(String timeString) {
		int minutes = 0;
		Pattern minutePattern = Pattern.compile("(\\d+)m");
		Matcher minuteMatcher = minutePattern.matcher(timeString);
		if (minuteMatcher.find()) {
			minutes = Integer.parseInt(minuteMatcher.group(1));
		}
		return minutes;
	}

	public String formatTime(int hours, int minutes) {
		// Add the additional minutes to the total time
		int totalMinutes = hours * 60 + minutes;

		// Convert the total minutes back to hours and minutes format
		int formattedHours = totalMinutes / 60;
		int formattedMinutes = totalMinutes % 60;

		// Construct the formatted time string
		String formattedTime = "";
		if (formattedHours > 0) {
			formattedTime += formattedHours + (formattedHours == 1 ? " hour" : " hours");
		}
		if (formattedMinutes > 0) {
			if (!formattedTime.isEmpty()) {
				formattedTime += " and ";
			}
			formattedTime += formattedMinutes + (formattedMinutes == 1 ? " minute" : " minutes");
		}

		return formattedTime;
	}


}
