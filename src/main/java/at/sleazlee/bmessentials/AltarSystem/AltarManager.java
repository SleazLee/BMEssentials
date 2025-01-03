package at.sleazlee.bmessentials.AltarSystem;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Manages altar interactions, token validation, reward processing, and animations for the server.
 */
public class AltarManager implements Listener {

	private final BMEssentials plugin;
	private final Map<String, Location> altarLocations = new HashMap<>();
	private final Map<String, Long> lastUsedTime = new HashMap<>();

	/**
	 * Constructor for AltarManager. Loads altar locations from the configuration.
	 *
	 * @param plugin Reference to the main plugin instance.
	 */
	public AltarManager(BMEssentials plugin) {
		this.plugin = plugin;
		loadAltarsFromConfig();
	}

	/**
	 * Loads altar locations and configurations from the server's config file.
	 */
	private void loadAltarsFromConfig() {
		FileConfiguration config = plugin.getConfig();
		ConfigurationSection altarsSection = config.getConfigurationSection("Systems.SpawnSystems.Altars");

		if (altarsSection == null) {
			plugin.getLogger().warning("Altars section not found in the configuration!");
			return;
		}

		for (String altarName : altarsSection.getKeys(false)) {
			double x = altarsSection.getDouble(altarName + ".x");
			double y = altarsSection.getDouble(altarName + ".y");
			double z = altarsSection.getDouble(altarName + ".z");
			altarLocations.put(altarName, new Location(Bukkit.getWorld("world"), x, y, z));
		}
	}

	/**
	 * Handles player right-click interactions with altars.
	 *
	 * @param event The PlayerInteractEvent triggered by the interaction.
	 */
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		Player player = event.getPlayer();
		Location clickedLocation = event.getClickedBlock().getLocation();

		// Iterate through altar locations to find which altar was clicked
		for (String altarName : altarLocations.keySet()) {
			if (altarLocations.get(altarName).equals(clickedLocation)) {

				// Prevent activation if the altar is on cooldown
				Long lastUsed = lastUsedTime.getOrDefault(altarName, 0L);
				if (System.currentTimeMillis() - lastUsed < 9300) {
					player.sendMessage("§x§F§F§3§3§0§0§lAltar §cPlease wait until this monument's activation sequence is complete.");
					return;
				}

				// Validate the token and perform altar-specific actions
				if (hasToken(player, altarName)) {
					switch (altarName.toLowerCase()) {
						case "healingsprings":
							HealingSprings.playHealingSpringsAnimation(plugin, clickedLocation);
							break;
						case "wishingwell":
							WishingWell.playWishingWellAnimation(plugin, clickedLocation);
							break;
						case "obelisk":
							// Obelisk.playObeliskAnimation(plugin, clickedLocation);
							break;
						default:
							player.sendMessage("§cUnknown altar: " + altarName);
							return;
					}

					// Execute the reward logic and update the cooldown timer
					giveRandomReward(player);
					lastUsedTime.put(altarName, System.currentTimeMillis());
				} else {
					player.sendMessage("§x§F§F§3§3§0§0§lAltar §cYou don't have the proper token to activate this altar. §fUse §c/vote§f to earn tokens.");
				}
				break; // Stop checking the loop once the clicked altar is found
			}
		}
	}

	/**
	 * Validates whether the player has the correct token for the given altar.
	 *
	 * @param player    The player interacting with the altar.
	 * @param altarName The name of the altar.
	 * @return True if the player has a valid token, false otherwise.
	 */
	private boolean hasToken(Player player, String altarName) {
		Token token = Token.getByAltarName(altarName);
		if (token != null) {
			for (ItemStack item : player.getInventory().getContents()) {
				if (item != null && item.getType() == token.getMaterial()) {
					ItemMeta meta = item.getItemMeta();
					if (meta != null && meta.hasDisplayName()) {
						Component displayNameComponent = meta.displayName();

						// Check the display name component for the token's unique hex color
						if (containsHexColor(displayNameComponent, token.getUniqueHexColor())) {
							item.setAmount(item.getAmount() - 1);
							if (item.getAmount() <= 0) {
								player.getInventory().remove(item);
							}
							return true; // Token successfully validated
						}
					}
				}
			}
		}
		return false; // Token not found or invalid
	}

	/**
	 * Recursively searches a Component hierarchy for a specific hex color.
	 *
	 * @param component   The Adventure Component to search.
	 * @param targetColor The TextColor (hex code) to look for.
	 * @return True if the target color is found, false otherwise.
	 */
	private boolean containsHexColor(Component component, TextColor targetColor) {
		TextColor componentColor = component.style().color();
		if (componentColor != null && componentColor.equals(targetColor)) {
			return true;
		}

		for (Component child : component.children()) {
			if (containsHexColor(child, targetColor)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Displays a floating animation of the reward item above the altar.
	 *
	 * @param plugin The main plugin instance.
	 * @param center The location of the altar.
	 * @param world  The world where the animation takes place.
	 */
	public static void showItemAnimation(BMEssentials plugin, Location center, World world) {
		ItemStack rewardItem = new ItemStack(Material.DIAMOND); // Example reward item
		Location adjustedCenter = center.clone().add(0.0, -0.3, 0.0); // Slightly adjust the spawn location
		Item floatingItem = world.dropItem(adjustedCenter, rewardItem);

		// Set item properties to prevent movement/interference
		floatingItem.setGravity(false);
		floatingItem.setInvulnerable(true);
		floatingItem.setVelocity(new Vector(0, 0, 0));
		floatingItem.setPickupDelay(Integer.MAX_VALUE);

		// Remove the floating item after 4 seconds (80 ticks)
		Scheduler.runLater(floatingItem::remove, 80L);
	}

	/**
	 * Provides the player with a random reward using server commands.
	 *
	 * @param player The player to receive the reward.
	 */
	private void giveRandomReward(Player player) {
		String command = getRandomRewardCommand();
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("<player>", player.getName()));
	}

	/**
	 * Returns a random reward command to execute.
	 *
	 * @return The reward command string.
	 */
	private String getRandomRewardCommand() {
		String[] commands = {
				"give <player> minecraft:diamond 1",
				"give <player> minecraft:emerald 1",
				"give <player> minecraft:gold_ingot 5"
		};
		return commands[new Random().nextInt(commands.length)];
	}

	/**
	 * Enum to handle altar tokens, including defining their material and unique hex color.
	 */
	public enum Token {
		HEALINGSprings("healingsprings", Material.GHAST_TEAR, TextColor.color(0x33cc66)),
		WISHING_WELL("wishingwell", Material.IRON_NUGGET, TextColor.color(0x33ccff)),
		OBELISK("obelisk", Material.GOLD_NUGGET, TextColor.color(0xcc6600));

		private final String altarName;
		private final Material material;
		private final TextColor uniqueHexColor;

		Token(String altarName, Material material, TextColor uniqueHexColor) {
			this.altarName = altarName;
			this.material = material;
			this.uniqueHexColor = uniqueHexColor;
		}

		public String getAltarName() {
			return altarName;
		}

		public Material getMaterial() {
			return material;
		}

		public TextColor getUniqueHexColor() {
			return uniqueHexColor;
		}

		public static Token getByAltarName(String altarName) {
			for (Token token : Token.values()) {
				if (token.getAltarName().equalsIgnoreCase(altarName)) return token;
			}
			return null;
		}
	}
}