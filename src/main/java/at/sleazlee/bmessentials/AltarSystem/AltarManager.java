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
							playHealingSpringsAnimation(clickedLocation);
							break;
						case "wishingwell":
							playWishingWellAnimation(clickedLocation);
							break;
						case "obelisk":
							//playObeliskAnimation(clickedLocation);
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
						// .
						// Commented out the take item from player code.
						// .
						//if (item.getAmount() > 1) {
						//	item.setAmount(item.getAmount() - 1);
						//} else {
						//	player.getInventory().remove(item);
						//}
						return true;
					}
				}
			}
		}

		return false;
	}


	// healing Springs Code


	private int taskId;
	private boolean altarActivated = false;
	private String activeHexColor = "#32CA65"; // Color for the REDSTONE particle when altar is active

	public void startHealingSpringsAmbient() {
		Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			if (!altarActivated) {
				spawnParticleAtAltar(Particle.FIREWORKS_SPARK, false);
			} else {
				spawnParticleAtAltar(Particle.FIREWORKS_SPARK, true);
			}
		}, 0L, 3L); // Reduced delay for faster animation
	}

	private double theta = 0; // Class-level variable to keep track of the current angle

	private void spawnParticleAtAltar(Particle particle, boolean active) {
		World world = Bukkit.getWorld("hub");
		double x = -246.5; // Centered on the block
		double y = 61.8;
		double z = -107.5; // Centered on the block
		double radius = 0.8;

		double dx = radius * Math.sin(theta);
		double dz = radius * Math.cos(theta);

		if (active) {
			world.spawnParticle(particle, x + dx, (y - 0.7), z + dz, 0, 0, -0.02, 0, 1);
		} else {
			// For FIREWORKS_SPARK, add a downward velocity
			world.spawnParticle(particle, x + dx, y, z + dz, 0, 0, -0.02, 0, 1);
		}

		// Increment theta by 2*PI/12 to move to the next point in the circle
		theta += 2 * Math.PI / 18;
		if (theta >= 2 * Math.PI) {
			theta = 0; // Reset theta when a full circle is completed
		}
	}

	public void stopHealingSpringsAmbient() {
		Bukkit.getScheduler().cancelTask(taskId);
	}

	// Call this method when the altar is activated
	public void activateHealingSpringsAltar() {
		altarActivated = true;
	}

	// Call this method when the altar activation is done
	public void deactivateHealingSpringsAltar() {
		altarActivated = false;
	}

	private void playHealingSpringsAnimation(Location location) {
		World world = location.getWorld();
		Location center = new Location(world, -246.5, 63, -107.5);

		// Step 1: Activate the Healing Springs Altar
		activateHealingSpringsAltar();

		// Step 2: Create a beam of particles from the Lectern to the Spore Blossom
		for (int i = 0; i <= 10; i++) {
			final int finalI = i;
			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				double y = center.getY() - 1.0 + (0.15 * finalI);
				world.spawnParticle(Particle.REDSTONE, center.getX(), y, center.getZ(), 0, 0, 0, 0, createDustOptions("#32CA65"));
			}, i * 2L);
		}

		// Step 3: Play initial ambient sounds
		for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
			onlinePlayer.playSound(center, Sound.BLOCK_BEACON_AMBIENT, SoundCategory.AMBIENT, 0.8f, 0.7f);
			onlinePlayer.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.AMBIENT, 2.0f, 1.0f);
		}

		// Step 4: Play additional ambient sounds after a short delay
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				Location adjustedCenter = center.clone().add(0.0, 0.4, 0.0);
				onlinePlayer.playSound(adjustedCenter, Sound.BLOCK_AMETHYST_CLUSTER_FALL, SoundCategory.AMBIENT, 2.0f, 0.3f);
			}
		}, 20L);

		// Step 5: Create particles that come down from the Spore Blossom
		for (int i = 0; i <= 12; i++) {
			final int finalI = i;
			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				double y = center.getY() + 0.6 - (0.05 * finalI);
				world.spawnParticle(Particle.REDSTONE, center.getX(), y, center.getZ(), 0, 0, 0, 0, createDustOptions("#32bbca"));
			}, 50L + i * 2L);
		}

		// Step 6: Create particles that form a sphere
		for (int i = 0; i <= 10; i++) {
			final double r = i * 0.03;
			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				for (double theta = 0; theta <= 2 * Math.PI; theta += Math.PI / 10) {
					for (double phi = 0; phi <= Math.PI; phi += Math.PI / 10) {
						double x = r * Math.sin(phi) * Math.cos(theta);
						double y = r * Math.sin(phi) * Math.sin(theta);
						double z = r * Math.cos(phi);
						world.spawnParticle(Particle.REDSTONE, center.getX() + x, center.getY() + y, center.getZ() + z, 0, 0, 0, 0, createDustOptions("#FFFFFF"));
					}
				}
			}, 80L + i * 2L);
		}

		// Step 7: Play sound when the sphere is complete
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				onlinePlayer.playSound(center, Sound.BLOCK_CONDUIT_ACTIVATE, SoundCategory.AMBIENT, 2.0f, 0.3f);
			}
		}, 100L);

		// Step 8: Show the reward item
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			showItemAnimation(center, world);
		}, 100L);

		// Step 9: Create a puff of smoke and play a sound when the item disappears
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			world.spawnParticle(Particle.SMOKE_NORMAL, center, 10, 0.2, 0.2, 0.2, 0.05);
			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				onlinePlayer.playSound(center, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.AMBIENT, 0.2f, 1.0f);
				deactivateHealingSpringsAltar();
			}
		}, 180L);
	}


	private void playWishingWellAnimation(Location altarLocation) {
		World world = altarLocation.getWorld();
		Location coneCenter = new Location(world, -237, 72.3, 37);
		Location wellCenter = new Location(world, -241, 72.3, 40);
		Location wellBottom = new Location(world, -241, 70, 40);
		Location sphereAboveAltar = new Location(world, -238, 72, 37);
		Location coneTop = new Location(world, -237, 73, 37);
		Location altarCenter = new Location(world, -238, 72, 37);


		// Step 1: Initial Activation (Portal particle spiraling up)
		for (int i = 0; i <= 10; i++) {
			final int finalI = i;
			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				double angle = 2 * Math.PI * finalI / 10; // Angle in radians
				double radius = 0.2 * finalI; // Radius of the spiral at this step
				double x = coneCenter.getX() + radius * Math.cos(angle);
				double y = coneCenter.getY() + 0.1 * finalI; // Height of the spiral at this step
				double z = coneCenter.getZ() + radius * Math.sin(angle);
				world.spawnParticle(Particle.PORTAL, x, y, z, 1);
			}, i * 2L);
		}


		// Step 2: Particle pauses at the top and then shoots into the wishing well
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			// Pause at the top of the cone
			world.spawnParticle(Particle.PORTAL, coneTop.getX(), coneTop.getY(), coneTop.getZ(), 1);

			// Delay for the pause
			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				// Shoot into the center of the wishing well
				for (int i = 0; i <= 10; i++) {
					final int finalI = i;
					Bukkit.getScheduler().runTaskLater(plugin, () -> {
						double x = coneTop.getX() + (wellCenter.getX() - coneTop.getX()) * finalI / 10.0;
						double y = coneTop.getY() + (wellCenter.getY() - coneTop.getY()) * finalI / 10.0;
						double z = coneTop.getZ() + (wellCenter.getZ() - coneTop.getZ()) * finalI / 10.0;
						world.spawnParticle(Particle.PORTAL, x, y, z, 1);
					}, i * 2L);
				}

				// Delay for the particle to reach the well
				Bukkit.getScheduler().runTaskLater(plugin, () -> {
					// Shoot down the well
					for (int i = 0; i <= 10; i++) {
						final int finalI = i;
						Bukkit.getScheduler().runTaskLater(plugin, () -> {
							double y = wellCenter.getY() - (wellCenter.getY() - wellBottom.getY()) * finalI / 10.0;
							world.spawnParticle(Particle.PORTAL, wellCenter.getX(), y, wellCenter.getZ(), 1);
						}, i * 2L);
					}
				}, 20L);  // 1 second delay
			}, 20L);  // 1 second delay
		}, 20L);  // 1 second delay

		// Step 3: Sphere spirals up from the bottom to the middle of the wishing well
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			for (int i = 0; i <= 10; i++) {
				final int finalI = i;
				Bukkit.getScheduler().runTaskLater(plugin, () -> {
					double y = wellBottom.getY() + (wellCenter.getY() - wellBottom.getY()) * finalI / 10.0;
					world.spawnParticle(Particle.END_ROD, wellCenter.getX(), y, wellCenter.getZ(), 10);
				}, i * 2L);
			}

			// Delay for the sphere to reach the middle of the well
			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				// Sphere travels right above the Altar
				for (int i = 0; i <= 10; i++) {
					final int finalI = i;
					Bukkit.getScheduler().runTaskLater(plugin, () -> {
						double y = wellCenter.getY() + (altarCenter.getY() - wellCenter.getY()) * finalI / 10.0;
						world.spawnParticle(Particle.END_ROD, altarCenter.getX(), y, altarCenter.getZ(), 10);
					}, i * 2L);
				}

				// Delay for the sphere to reach above the Altar
				Bukkit.getScheduler().runTaskLater(plugin, () -> {
					// Show the item above the Altar
					showItemAnimation(altarCenter, world);
				}, 20L);  // 1 second delay
			}, 20L);  // 1 second delay
		}, 40L);  // 2 seconds delay after the particle shoots down the well


		// Step 4: Sphere Emergence (End Rod particles)
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			// Your code for the sphere made of end_rod particles
			// Use wellBottom and wellCenter for the movement
		}, 50L);

		// Step 5: Sphere Movement (To above the Altar)
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			// Your code for moving the sphere above the altar
			// Use sphereAboveAltar as the destination
		}, 70L);

		// Step 6: Item Appearance
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			showItemAnimation(sphereAboveAltar, world);
		}, 90L);
	}


	private void showItemAnimation(Location center, World world) {
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
