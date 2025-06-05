package at.sleazlee.bmessentials.PurpurFeatures;

import at.sleazlee.bmessentials.wild.NoFallDamage; // <-- Make sure this import is correct
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Listener class that allows a Totem of Undying to function from any slot in the player’s inventory.
 * Checks the NoFallDamage system first so a Totem isn't consumed if fall damage is already forgiven.
 */
public class totemWorksAnywhere implements Listener {

    /**
     * Called whenever an entity takes damage.
     * <p>
     * If the entity is a Player and the final damage is lethal, checks the player's inventory
     * for a Totem of Undying. If found, prevents death, consumes one Totem, applies Regeneration,
     * Absorption, Fire Resistance, and plays the Totem sound effect. Skips Totem usage if the
     * NoFallDamage system is already handling fall damage for this player.
     *
     * @param event The {@link EntityDamageEvent} triggered by the server.
     */
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        // Check if the damaged entity is a player
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // If this is fall damage and NoFallDamage says the player is protected, skip Totem usage
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL
                && NoFallDamage.getFallDisabled().contains(player.getUniqueId())) {
            // The player is in the no-fall list, so do nothing regarding Totems
            return;
        }

        // Would the player die from this damage?
        double finalDamage = event.getFinalDamage();
        double currentHealth = player.getHealth();
        if (finalDamage < currentHealth) {
            // Not lethal; do nothing
            return;
        }

        // Search for a Totem of Undying in the player's inventory
        PlayerInventory inv = player.getInventory();
        int totemSlot = -1;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack != null && stack.getType() == Material.TOTEM_OF_UNDYING) {
                totemSlot = i;
                break;
            }
        }

        // If no Totem is found, do nothing
        if (totemSlot == -1) {
            return;
        }

        // "Use" the Totem:
        event.setCancelled(true);   // Cancel the lethal damage
        player.setHealth(1.0);     // Bring player back to 1 HP

        // Remove one Totem from the stack
        ItemStack totem = inv.getItem(totemSlot);
        totem.setAmount(totem.getAmount() - 1);
        if (totem.getAmount() <= 0) {
            inv.setItem(totemSlot, null);
        }

        // Apply the normal totem effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 900, 1)); // 900 ticks = 45s
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1));  // 100 ticks = 5s
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 800, 0)); // 800 ticks = 40s

        // Totem use sound
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);

        // Small firework effect at the player's location for visual feedback
        Firework firework = player.getWorld().spawn(player.getLocation(), Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .withColor(Color.LIME)
                .with(FireworkEffect.Type.BURST)
                .flicker(true)
                .trail(true)
                .build());
        meta.setPower(0);
        firework.setFireworkMeta(meta);
        firework.detonate();
    }
}
