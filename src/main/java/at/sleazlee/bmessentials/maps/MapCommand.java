package at.sleazlee.bmessentials.maps;

import at.sleazlee.bmessentials.BMEssentials;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MapCommand implements CommandExecutor {
	private final BMEssentials plugin;

	public MapCommand(BMEssentials plugin) {
		this.plugin = plugin;
	}


	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (label.equalsIgnoreCase("map") || label.equalsIgnoreCase("maps")) {
			if (sender instanceof Player) {
				Player player = (Player) sender;

				if (args.length == 0) {
					// No additional arguments, perform default function
					findServer(player);

				} else if (args.length == 1) {
					String district = args[0];

					matchServer(district, player);

				} else {
					// Too many arguments
					player.sendMessage("§c§lBM §cToo many arguments. §fTry /map [District]");

				}
			} else {
				sender.sendMessage("§c§lBM §cThis command can only be used by a player.");
			}
			return true;
		}
		return false;
	}

	public void findServer(Player player) {

		String district = plugin.getConfig().getString("serverName");

		matchServer(district, player);


	}

	public void matchServer(String district, Player player) {

		String mapLink = "";

		switch (district.toLowerCase()) {
			case "blockminer":
				createMapLink(district.toLowerCase(), player);
				break;
			default:
				mapLink = plugin.getConfig().getString("Systems.Maps.Default");

				TextComponent baseText = new TextComponent("§a§lMaps §7Here's the link: §2");
				// Create a text component with the desired message
				TextComponent clickableText = new TextComponent("§2§lClick Me!");
				// Add a click event to the message to open the URL
				clickableText.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, mapLink));
				// Append the clickable text to the base text
				baseText.addExtra(clickableText);
				// Send the message to the player
				player.spigot().sendMessage(baseText);
				break;
		}
	}


	public void createMapLink(String districtName, Player player) {

		String mapLink = "";

		// Get the player's location
		Location location = player.getLocation();

		// Get the world name
		String worldName = location.getWorld().getName();

		// Get the X and Z coordinates
		int x = (int) location.getX();
		int y = (int) location.getY();
		int z = (int) location.getZ();


		if (worldName.contains("_the_end")) {
			mapLink = "https://blockminer.net/map/" + districtName;

		} else {
			mapLink = "https://blockminer.net/map/#" + worldName + ":" + x + ":" + y + ":" + z + ":150:0:0:0:0:perspective";
			//
			// https://blockminer.net/map/#world:X:Y:Z:150:0:0:0:0:perspective
		}

		TextComponent baseText = new TextComponent("§a§lMaps §7Here's the link: §2");
		// Create a text component with the desired message
		TextComponent clickableText = new TextComponent("§2§lClick Me!");
		// Add a click event to the message to open the URL
		clickableText.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, mapLink));

		// Append the clickable text to the base text
		baseText.addExtra(clickableText);

		// Send the message to the player
		player.spigot().sendMessage(baseText);
	}

}