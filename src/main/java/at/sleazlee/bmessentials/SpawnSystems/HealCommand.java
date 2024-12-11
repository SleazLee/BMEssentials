package at.sleazlee.bmessentials.SpawnSystems;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class HealCommand implements CommandExecutor {

	private final BMEssentials plugin;
	private final Map<UUID, Long> cooldowns = new HashMap<>();
	private final long cooldownDuration = 90 * 60 * 1000; // 90 minutes in milliseconds
	private final Random random = new Random();

	public HealCommand(BMEssentials plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length > 0) {
			String playerName = args[0];
			Player player = Bukkit.getPlayer(playerName);

			if (player != null) {
				checkAndExecute(player);
			} else {
				sender.sendMessage("Player not found or not online.");
			}
		}
		return true;
	}

	private void checkAndExecute(Player player) {
		UUID playerUUID = player.getUniqueId();
		long currentTime = System.currentTimeMillis();

		if (cooldowns.containsKey(playerUUID) && cooldowns.get(playerUUID) > currentTime) {
			// Player is on cooldown
			long remainingTime = (cooldowns.get(playerUUID) - currentTime) / 1000; // Time in seconds
			player.sendMessage("§5§lHealing Springs §fThe spring's power is not fully restored yet. Please try back later. §8(§e" + formatTime(remainingTime) + "§8)");
		} else {
			// Heal the player and apply effects
			healPlayer(player);

			// Set the cooldown expiration time
			long cooldownExpiry = currentTime + cooldownDuration;
			cooldowns.put(playerUUID, cooldownExpiry);

			// Optionally schedule a task to clean up expired cooldowns (not strictly necessary)
			Scheduler.runLater(() -> cooldowns.remove(playerUUID), (int) (cooldownDuration / 50)); // Convert milliseconds to ticks
		}
	}

	private void healPlayer(Player player) {
		// Heal the player
		double maxHealth = Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getValue();
		player.setHealth(maxHealth);

		// Fill their hunger
		player.setFoodLevel(20);

		// Give them the regeneration effect for 10 seconds
		player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 10 * 20, 1));

		// Play the sound "ENTITY_GLOW_SQUID_AMBIENT" to the player
		player.playSound(player.getLocation(), Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.0f, 1.0f);

		player.sendMessage(getRandomMessage());
	}

	private String formatTime(long seconds) {
		long minutes = seconds / 60;
		seconds = seconds % 60;
		return minutes + "m " + seconds + "s";
	}

	public String getRandomMessage() {
		List<String> messageList = List.of(
				"§d§lHealing Springs§f A gentle healing embrace. You've been healed!",
				"§d§lHealing Springs§f A healing touch from the spring. You've been healed!",
				"§d§lHealing Springs§f A smooth and blissful state of mind takes over, and you feel at peace. You've been healed!",
				"§d§lHealing Springs§f A wave of relief washes over you as the healing waters cleanse your wounds and mend your aches. You've been healed!",
				"§d§lHealing Springs§f As the spring's healing power restores your body and soul, a vivid vision of a mysterious maze flashes before your eyes, filled with formidable foes and perplexing puzzles, promising unparalleled rewards for those who dare to venture within. You've been healed, and a thrilling challenge awaits.",
				"§d§lHealing Springs§f As the spring's waters cascade over your wounds, the pain ebbs away, replaced by a comforting warmth. Your body feels reenergized, and your spirit renewed. You've been healed!",
				"§d§lHealing Springs§f As the spring's waters cascade over your wounds, the pain subsides, replaced by a disquieting chill. Your body feels reenergized, but your spirit is troubled. You've been healed... but something feels off.",
				"§d§lHealing Springs§f As you touch the water, an ethereal glow surrounds you, mending your wounds and revitalizing your spirit. You've been healed!",
				"§d§lHealing Springs§f Healing energy surges through you. You've been healed!",
				"§d§lHealing Springs§f Nourished and rejuvenated. You've been healed!",
				"§d§lHealing Springs§f Restored in body and hunger. You've been healed!",
				"§d§lHealing Springs§f The crystal-clear waters of the spring wash away your pain and sorrow. As your injuries fade, your strength returns, and your spirit is lifted by the comforting embrace of the healing waters. You've been healed!",
				"§d§lHealing Springs§f The healing energy of the spring envelops you, dissolving your pain and leaving you feeling invigorated. You've been healed!",
				"§d§lHealing Springs§f The healing spring's ancient wisdom permeates your body, mending your wounds and revitalizing your spirit. Yet, as your injuries fade away, you sense a malevolent presence lurking beneath the surface. You've been healed, but at what unseen cost?",
				"§d§lHealing Springs§f The healing spring's gentle touch washes away your pain, leaving you refreshed and whole once more. You've been healed!",
				"§d§lHealing Springs§f The moment you touch the healing spring, a gentle wave of energy radiates from the water, coursing through your body and mending every cut, bruise, and injury. You feel a deep sense of tranquility. You've been healed!",
				"§d§lHealing Springs§f The moment you touch the healing spring, a pulsating wave of energy radiates from the water, coursing through your body and mending every cut, bruise, and injury. However, a lingering sense of unease takes hold. You've been healed, but can you trust the spring?",
				"§d§lHealing Springs§f The moment you touch the healing spring, a pulsating wave of energy radiates from the water, coursing through your body and mending every cut, bruise, and injury. You've never felt so alive. You've been healed!",
				"§d§lHealing Springs§f The spring's enchanting aura seeps into your being, knitting together your injuries and reinvigorating your spirit. You've been healed!",
				"§d§lHealing Springs§f The spring's magic mends your wounds. You've been healed!",
				"§d§lHealing Springs§f Wounds that you have had for years suddenly fade away… Whatever purpose this power has doesn't matter to you at this moment. You are thankful for its blessing.",
				"§d§lHealing Springs§f You are fully healed!",
				"§d§lHealing Springs§f You can't help but marvel at the miraculous spring as it works its magic, restoring your health and vigor. You've been healed!",
				"§d§lHealing Springs§f You feel as if you are bestowed with a great honor, but all power comes with a price... in the meantime at least you are healed.",
				"§d§lHealing Springs§f You immerse yourself in the spring's waters, and a surge of rejuvenation courses through your veins. You've been healed!",
				"§d§lHealing Springs§f You watch as your cuts and wounds disappear. You are now fully healed!",
				"§d§lHealing Springs§f You've been healed but, for a moment, an unsettling feeling arises. What are the spring's true intentions?",
				"§d§lHealing Springs§f You've been healed!",
				"§d§lHealing Springs§f Your health is completely restored!"
		);

		int randomIndex = random.nextInt(messageList.size()); // Generate a random index
		return messageList.get(randomIndex); // Get the message at the random index
	}
}
