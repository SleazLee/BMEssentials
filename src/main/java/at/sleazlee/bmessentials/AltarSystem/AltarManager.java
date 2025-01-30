package at.sleazlee.bmessentials.AltarSystem;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;

/**
 * Manages Altar interactions (via PlayerInteractEvent), token validation,
 * reward selection from AltarPrizes.yml, and shared animation methods.
 */
public class AltarManager implements Listener {

	private final BMEssentials plugin;

	/** Tracks the in-world location of each Altar by name, loaded from config. */
	private final Map<String, Location> altarLocations = new HashMap<>();

	/**
	 * Records the last time (in milliseconds) each Altar was used.
	 * Used to enforce a short cooldown so the animation isn't spammed.
	 */
	private final Map<String, Long> lastUsedTime = new HashMap<>();

	/** For random selection of fallback commands or weighted prizes. */
	private final Random random = new Random();

	/**
	 * Constructor for AltarManager. Loads Altar locations from plugin config.
	 *
	 * @param plugin Reference to the main plugin instance.
	 */
	public AltarManager(BMEssentials plugin) {
		this.plugin = plugin;
		loadAltarsFromConfig();
	}

	/**
	 * Loads altar locations from the server's config file under:
	 * "Systems.SpawnSystems.Altars.<AltarName>.<x|y|z>"
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
	 * Event handler for when a player right-clicks a block.
	 * Checks if the block is an Altar, if the player has the correct token,
	 * and then triggers the relevant animation with the correct reward item.
	 *
	 * @param event The interact event.
	 */
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		// We only care about right-clicking a block.
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		// Avoid double-trigger from off-hand (1.9+).
		if (event.getHand() != EquipmentSlot.HAND) {
			return;
		}

		// Ensure the clicked block is valid.
		if (event.getClickedBlock() == null) {
			return;
		}

		Player player = event.getPlayer();
		Location clickedLocation = event.getClickedBlock().getLocation();

		// Check if the clicked block matches any known Altar location.
		for (String altarName : altarLocations.keySet()) {
			if (altarLocations.get(altarName).equals(clickedLocation)) {

				// Check for short cooldown (~9.3s in this example).
				long lastUsed = lastUsedTime.getOrDefault(altarName, 0L);
				if (System.currentTimeMillis() - lastUsed < 9300) {
					player.sendMessage("§x§F§F§3§3§0§0§lAltar §cPlease wait until this monument's activation sequence is complete.");
					return;
				}

				// Verify the player has the matching token for this Altar.
				if (hasToken(player, altarName)) {
					// Pick and give the prize item. We'll also get its Material for animation.
					Material displayMaterial = pickAndGivePrize(player, altarName);

					// Trigger the correct altar animation, passing the chosen Material.
					switch (altarName.toLowerCase()) {
						case "healingsprings":
							HealingSprings.playHealingSpringsAnimation(plugin, clickedLocation, displayMaterial);
							break;
						case "wishingwell":
							WishingWell.playWishingWellAnimation(plugin, clickedLocation, displayMaterial);
							break;
						case "obelisk":
							Obelisk.playObeliskAnimation(plugin, clickedLocation, displayMaterial);
							break;
						default:
							player.sendMessage("§cUnknown altar: " + altarName);
							return;
					}

					// Record the use time for cooldown.
					lastUsedTime.put(altarName, System.currentTimeMillis());

				} else {
					// Player does NOT have the proper token.
					player.sendMessage("§x§F§F§3§3§0§0§lAltar §cYou don't have the proper token to activate this altar. §fUse §c/vote§f to earn tokens.");
				}
				// Stop searching once we found the relevant altar.
				return;
			}
		}
	}

	/**
	 * Checks if a player has the required token for the given Altar,
	 * by matching both the Material and a unique text color in its display name.
	 * If found, one token is consumed.
	 *
	 * @param player    The player to check.
	 * @param altarName The Altar name (e.g. "WishingWell").
	 * @return true if the player has a matching token, false otherwise.
	 */
	private boolean hasToken(Player player, String altarName) {
		Token token = Token.getByAltarName(altarName);
		if (token == null) {
			return false; // No known token for this altar name.
		}

		for (ItemStack item : player.getInventory().getContents()) {
			if (item != null && item.getType() == token.getMaterial()) {
				ItemMeta meta = item.getItemMeta();
				if (meta != null && meta.hasDisplayName()) {
					Component displayNameComponent = meta.displayName();
					if (containsHexColor(displayNameComponent, token.getUniqueHexColor())) {
						// Found the valid token, consume 1 from the stack.
						item.setAmount(item.getAmount() - 1);
						if (item.getAmount() <= 0) {
							player.getInventory().remove(item);
						}
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Checks if a Component (and its children) contains a specific hex color.
	 *
	 * @param component   The component to inspect.
	 * @param targetColor The TextColor to look for.
	 * @return true if found, false otherwise.
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
	 * Selects and dispatches a reward from AltarPrizes.yml (if present) for the given Altar,
	 * returning the Material that should appear in the final floating animation.
	 *
	 * @param player    The player receiving the reward.
	 * @param altarName The name of the altar, e.g. "WishingWell".
	 * @return The Material to display in the item-floating animation.
	 */
	private Material pickAndGivePrize(Player player, String altarName) {
		// Attempt to load from AltarPrizes.yml
		File prizesFile = new File(plugin.getDataFolder(), "AltarPrizes.yml");
		if (!prizesFile.exists()) {
			// If file doesn't exist, fallback to random vanilla command
			giveRandomReward(player);
			return Material.DIAMOND; // fallback material for display
		}

		YamlConfiguration prizeConfig = YamlConfiguration.loadConfiguration(prizesFile);

		ConfigurationSection altSection = prizeConfig.getConfigurationSection(altarName + ".prizes");
		if (altSection == null) {
			// If no prizes listed for this altar, fallback
			giveRandomReward(player);
			return Material.DIAMOND;
		}

		// Parse the sub-keys (prize entries).
		List<Prize> allPrizes = new ArrayList<>();
		for (String key : altSection.getKeys(false)) {
			ConfigurationSection singlePrize = altSection.getConfigurationSection(key);
			if (singlePrize == null) continue;

			String name = singlePrize.getString("name", "UnknownItem");
			String type = singlePrize.getString("type", "STONE");
			int amount = singlePrize.getInt("amount", 1);
			int rarity = singlePrize.getInt("rarity", 100);

			allPrizes.add(new Prize(name, type, amount, rarity));
		}

		// If none found, fallback
		if (allPrizes.isEmpty()) {
			giveRandomReward(player);
			return Material.DIAMOND;
		}

		// Weighted random selection
		Prize chosen = getWeightedRandomPrize(allPrizes);
		if (chosen == null) {
			giveRandomReward(player);
			return Material.DIAMOND;
		}

		// Dispatch the actual command to give the item: "/si give <itemName> <amount> <player>"
		String cmd = String.format("si give %s %d %s", chosen.getName(), chosen.getAmount(), player.getName());
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);

		// Return the Material to display in the floating animation
		try {
			return Material.valueOf(chosen.getType().toUpperCase());
		} catch (IllegalArgumentException e) {
			// If the config type was invalid, fallback
			return Material.DIAMOND;
		}
	}

	/**
	 * Simple fallback method for older "random reward" commands
	 * if AltarPrizes.yml is missing or misconfigured.
	 *
	 * @param player The player receiving the reward.
	 */
	private void giveRandomReward(Player player) {
		String[] commands = {
				"give <player> minecraft:diamond 1",
				"give <player> minecraft:emerald 1",
				"give <player> minecraft:gold_ingot 5"
		};
		String command = commands[random.nextInt(commands.length)];
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("<player>", player.getName()));
	}

	/**
	 * Selects one prize from a list of possible prizes using each prize's 'rarity' as a weight.
	 *
	 * @param allPrizes A list of Prize objects to choose from.
	 * @return The selected Prize, or null if the list is empty.
	 */
	private Prize getWeightedRandomPrize(List<Prize> allPrizes) {
		int totalWeight = allPrizes.stream().mapToInt(Prize::getRarity).sum();
		if (totalWeight <= 0) {
			return null;
		}

		int roll = random.nextInt(totalWeight) + 1;
		int running = 0;
		for (Prize p : allPrizes) {
			running += p.getRarity();
			if (roll <= running) {
				return p;
			}
		}
		return null; // Should not happen unless totalWeight=0
	}

	/**
	 * Displays a glowing floating item above the altar for ~4 seconds.
	 * Typically called by Altar animation classes in their final reveal step.
	 *
	 * @param plugin       Reference to the main plugin instance.
	 * @param center       The location at which to show the item.
	 * @param world        The world in which to drop the item.
	 * @param displayType  Which Material to display. Must not be null.
	 */
	public static void showItemAnimation(BMEssentials plugin, Location center, World world, Material displayType) {
		if (displayType == null) {
			displayType = Material.DIAMOND;
		}

		ItemStack rewardItem = new ItemStack(displayType, 1);

		// Give it a "glowing" enchant effect
		ItemMeta meta = rewardItem.getItemMeta();
		if (meta != null) {
			meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			rewardItem.setItemMeta(meta);
		}

		// Slightly adjust spawn location
		Location adjustedCenter = center.clone().add(0.0, -0.3, 0.0);
		Item floatingItem = world.dropItem(adjustedCenter, rewardItem);

		floatingItem.setGravity(false);
		floatingItem.setInvulnerable(true);
		floatingItem.setVelocity(new Vector(0, 0, 0));
		floatingItem.setPickupDelay(Integer.MAX_VALUE);

		// Remove item after ~4 seconds (80 ticks).
		Scheduler.runLater(floatingItem::remove, 80L);
	}

	/**
	 * Enumeration of possible Altar tokens, each with:
	 * - The altar name
	 * - The required Bukkit Material
	 * - A unique hex color to match the item display name
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

		/**
		 * Finds a matching token by altar name, ignoring case.
		 *
		 * @param altarName The name of the altar (e.g. "WishingWell").
		 * @return The corresponding Token, or null if none.
		 */
		public static Token getByAltarName(String altarName) {
			for (Token token : values()) {
				if (token.getAltarName().equalsIgnoreCase(altarName)) {
					return token;
				}
			}
			return null;
		}
	}

	/**
	 * Represents a Prize entry loaded from AltarPrizes.yml,
	 * including the ItemEdit name, display Material type, amount, and rarity weight.
	 */
	private static class Prize {
		private final String name;   // The ItemEdit name used in /si give
		private final String type;   // Material type for display
		private final int amount;    // How many items to give
		private final int rarity;    // Weighted chance of awarding (e.g. 90 vs 10)

		public Prize(String name, String type, int amount, int rarity) {
			this.name = name;
			this.type = type;
			this.amount = amount;
			this.rarity = rarity;
		}

		public String getName() {
			return name;
		}

		public String getType() {
			return type;
		}

		public int getAmount() {
			return amount;
		}

		public int getRarity() {
			return rarity;
		}
	}
}