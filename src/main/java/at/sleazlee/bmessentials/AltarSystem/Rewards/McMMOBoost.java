package at.sleazlee.bmessentials.AltarSystem.Rewards;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class McMMOBoost {

	private final BMEssentials plugin;

	public McMMOBoost(BMEssentials plugin) {
		this.plugin = plugin;
	}

	/**
	 * Attempts to give the player a McMMO boost based on a random chance (defined by TotalChance in the config)
	 * and further random selection between different boost types.
	 *
	 * @param player The player to receive the boost.
	 */
	public void mcmmoboost(Player player) {
		String playerName = player.getName();
		double chanceValue = Math.random();
		double totalChance = plugin.getConfig().getDouble("Systems.SpawnSystems.McmmoBoost.TotalChance");
		if (chanceValue > totalChance) {
			// Chance failed; no boost is given.
			return;
		}

		double randomValue = Math.random();
		boolean has10Percent = player.hasPermission("mcmmo.perks.xp.10percentboost.*");
		boolean has50Percent = player.hasPermission("mcmmo.perks.xp.50percentboost.*");
		boolean has150Percent = player.hasPermission("mcmmo.perks.xp.150percentboost.*");
		boolean hasDouble = player.hasPermission("mcmmo.perks.xp.double.*");
		String newReward;

		double tenPercentChance = plugin.getConfig().getDouble("Systems.SpawnSystems.McmmoBoost.TenPercentChance");
		double fiftyPercentChance = tenPercentChance + plugin.getConfig().getDouble("Systems.SpawnSystems.McmmoBoost.FiftyPercentChance");
		double oneHundredFiftyPercentChance = fiftyPercentChance + plugin.getConfig().getDouble("Systems.SpawnSystems.McmmoBoost.OneHundredFiftyPercentChance");

		if (randomValue <= tenPercentChance) {
			newReward = "10Percent";
		} else if (randomValue <= fiftyPercentChance) {
			newReward = "50Percent";
		} else if (randomValue <= oneHundredFiftyPercentChance) {
			newReward = "150Percent";
		} else {
			newReward = "Double";
		}

		switch (newReward) {
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
				break;
		}
	}

	/**
	 * Removes the temporary permission and reassigns it with an updated expiration time.
	 *
	 * @param permission The permission node to update.
	 * @param minutes    The additional minutes to add.
	 * @param player     The player whose permission is being updated.
	 * @param playerName The player's name.
	 * @param boostName  The display name for the boost.
	 */
	public void switchAndUpdate(String permission, int minutes, Player player, String playerName, String boostName) {
		String parsedTime = PlaceholderAPI.setPlaceholders(player, "%luckperms_expiry_time_" + permission + "%");
		int hour = hoursExtract(parsedTime);
		int min = ((minuteExtract(parsedTime) + minutes) + 1);
		String formattedTimeResult = formatTime(hour, min);

		player.sendMessage("§6§lMcMMO §7Nice! §7Your §a" + boostName + "§7 was extended and has §b" + formattedTimeResult + "§7 remaining!");

		// Remove the temporary permission first
		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		String unSetCommandBuilder = "lp user " + playerName + " permission unsettemp " + permission;
		Bukkit.dispatchCommand(console, unSetCommandBuilder);

		// Reassign the permission with the updated time
		String setCommandBuilder = "lp user " + playerName + " permission settemp " + permission + " true " + hour + "h" + min + "m";
		Scheduler.runLater(new Runnable() {
			@Override
			public void run() {
				Bukkit.dispatchCommand(console, setCommandBuilder);
			}
		}, 20L); // Delay in ticks (20 ticks = 1 second)
	}

	/**
	 * Assigns the temporary permission reward to the player.
	 *
	 * @param permission The permission node.
	 * @param minutes    The duration in minutes.
	 * @param playerName The player's name.
	 */
	public void giveReward(String permission, int minutes, String playerName) {
		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		String setCommandBuilder = "lp user " + playerName + " permission settemp " + permission + " true " + minutes + "m";
		Bukkit.dispatchCommand(console, setCommandBuilder);
	}

	/**
	 * Extracts the hours value from a string (expects a format like "2h" somewhere in the string).
	 *
	 * @param timeString The string to search.
	 * @return The extracted number of hours, or 0 if not found.
	 */
	public int hoursExtract(String timeString) {
		int hours = 0;
		Pattern hourPattern = Pattern.compile("(\\d+)h");
		Matcher hourMatcher = hourPattern.matcher(timeString);
		if (hourMatcher.find()) {
			hours = Integer.parseInt(hourMatcher.group(1));
		}
		return hours;
	}

	/**
	 * Extracts the minutes value from a string (expects a format like "30m" somewhere in the string).
	 *
	 * @param timeString The string to search.
	 * @return The extracted number of minutes, or 0 if not found.
	 */
	public int minuteExtract(String timeString) {
		int minutes = 0;
		Pattern minutePattern = Pattern.compile("(\\d+)m");
		Matcher minuteMatcher = minutePattern.matcher(timeString);
		if (minuteMatcher.find()) {
			minutes = Integer.parseInt(minuteMatcher.group(1));
		}
		return minutes;
	}

	/**
	 * Formats a given time (in hours and minutes) into a readable string.
	 *
	 * @param hours   The number of hours.
	 * @param minutes The number of minutes.
	 * @return A formatted string (e.g., "1 hour and 30 minutes").
	 */
	public String formatTime(int hours, int minutes) {
		int totalMinutes = hours * 60 + minutes;
		int formattedHours = totalMinutes / 60;
		int formattedMinutes = totalMinutes % 60;

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
