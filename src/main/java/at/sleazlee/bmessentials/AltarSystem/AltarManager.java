package at.sleazlee.bmessentials.AltarSystem;

import at.sleazlee.bmessentials.BMEssentials;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AltarManager implements Listener {

	private final BMEssentials plugin;
	private FileConfiguration Config;

	private final Map<String, Location> altarLocations = new HashMap<>();
	private final Map<String, String> altarTokens = new HashMap<>();
	private final Map<String, String> altarColors = new HashMap<>();
	private Map<String, Long> lastUsedTime = new HashMap<>();

	public AltarManager(BMEssentials plugin) {
		this.plugin = plugin;
		loadAltarsFromConfig();
	}

	private void loadAltarsFromConfig() {
		FileConfiguration config = plugin.getConfig();
		ConfigurationSection altarsSection = config.getConfigurationSection("systems.spawnsystems.altars");
		if (altarsSection == null) {
			plugin.getLogger().warning("Altars section not found in the configuration!");
			return;
		}

		// Load altar locations from the config
		for (String altarName : altarsSection.getKeys(false)) {
			double x = altarsSection.getDouble(altarName + ".x");
			double y = altarsSection.getDouble(altarName + ".y");
			double z = altarsSection.getDouble(altarName + ".z");
			altarLocations.put(altarName, new Location(Bukkit.getWorld("hub"), x, y, z));

			// Assuming you have tokens named like "HealingSpringsToken", "WishingWellToken", etc.
			altarTokens.put(altarName, altarName + "Token");

			// Add colors for each altar
			altarColors.put("healingsprings", "#32CA65");
			altarColors.put("wishingwell", "#32CAFC");
			altarColors.put("obelisk", "#CA6500");
		}
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		// Check if the event was triggered by the main hand and is a right-click action
		if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		Player player = event.getPlayer();
		Location clickedLocation = event.getClickedBlock().getLocation();

		for (String altarName : altarLocations.keySet()) {
			if (altarLocations.get(altarName).equals(clickedLocation)) {
				Long lastUsed = lastUsedTime.getOrDefault(altarName, 0L);
				if (System.currentTimeMillis() - lastUsed < 9300) {
					player.sendMessage("§x§F§F§3§3§0§0§lAltar §cPlease wait until this Monument's activation sequence is complete.");
					return;
				}

				if (hasToken(player, altarName)) {
					// Play the correct animation based on the altar
					switch (altarName) {
						case "healingsprings":
							HealingSprings.playHealingSpringsAnimation(plugin, clickedLocation);
							break;
						case "wishingwell":
							WishingWell.playWishingWellAnimation(plugin, clickedLocation);
							break;
						case "obelisk":
							//playObeliskAnimation(plugin, clickedLocation);
							break;
					}
					giveRandomReward(player);
					lastUsedTime.put(altarName, System.currentTimeMillis());
				} else {
					player.sendMessage("§x§F§F§3§3§0§0§lAltar §cYou don't have the proper token to activate this altar. §fUse §c/vote§f to earn tokens.");
				}
				break;
			}
		}
	}

	private boolean hasToken(Player player, String altarName) {
		ItemStack token = null;
		String tokenName = null;

		switch (altarName) {
			case "healingsprings":
				token = new ItemStack(Material.GHAST_TEAR);
				tokenName = "§x§3§3§C§C§6§6§lHealing Springs §7Token";
				break;
			case "wishingwell":
				token = new ItemStack(Material.IRON_NUGGET);
				tokenName = "§7 §x§3§3§C§C§F§F§lWishing-Well §7Token §7";
				break;
			case "obelisk":
				token = new ItemStack(Material.GOLD_NUGGET);
				tokenName = "§7 §x§C§C§6§6§0§0§lObelisk §7Token";
				break;
		}

		if (token != null) {
			for (ItemStack item : player.getInventory().getContents()) {
				if (item != null && item.getType() == token.getType()) {
					ItemMeta meta = item.getItemMeta();
					if (meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(tokenName)) {
//						 .
//						 Commented out the take item from player code.
//						 .
//						if (item.getAmount() > 1) {
//							item.setAmount(item.getAmount() - 1);
//						} else {
//							player.getInventory().remove(item);
//						}
						return true;
					}
				}
			}
		}

		return false;
	}


	// healing Springs Code


	static void showItemAnimation(BMEssentials plugin, Location center, World world) {
		ItemStack rewardItem = new ItemStack(Material.DIAMOND); // Example item
		// Adjust the location slightly to center the item
		Location adjustedCenter = center.clone().add(0.0, -0.3, 0.0);
		Item floatingItem = world.dropItem(adjustedCenter, rewardItem);
		floatingItem.setGravity(false);
		floatingItem.setInvulnerable(true);
		floatingItem.setVelocity(new Vector(0, 0, 0)); // Ensure the item doesn't move
		floatingItem.setPickupDelay(Integer.MAX_VALUE);

		// Remove the floating item after 1 second.
		Bukkit.getScheduler().runTaskLater(plugin, floatingItem::remove, 80L);
	}

	private void giveRandomReward(Player player) {
		// Assuming you have a method to get a random reward command
		String command = getRandomRewardCommand();
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("<player>", player.getName()));
	}


	private Particle.DustOptions createDustOptions(String hexCode) {
		int r = Integer.valueOf(hexCode.substring(1, 3), 16);
		int g = Integer.valueOf(hexCode.substring(3, 5), 16);
		int b = Integer.valueOf(hexCode.substring(5, 7), 16);
		return new Particle.DustOptions(Color.fromRGB(r, g, b), 1);
	}

	private String getRandomRewardCommand() {
		// Implement your logic to get a random reward command
		String[] commands = {"give <player> dirt 1", "give <player> stone 1"};
		return commands[new Random().nextInt(commands.length)];
	}
}
