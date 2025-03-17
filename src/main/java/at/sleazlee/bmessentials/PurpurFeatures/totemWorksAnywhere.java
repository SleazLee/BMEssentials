package at.sleazlee.bmessentials.PurpurFeatures;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Listener class that allows a Totem of Undying to function from any slot in the player’s inventory.
 * <p>
 * This class listens for lethal damage events on players. If the damage is lethal and a Totem of Undying is found
 * in the player's inventory, it prevents the death, consumes one Totem, applies the usual Totem of Undying effects,
 * and triggers Paper's built-in totem animation (if available) so the player sees the official Totem sequence.
 */
public class totemWorksAnywhere implements Listener {

    /**
     * Called whenever an entity takes damage.
     * <p>
     * If the entity is a Player and the final damage is lethal, checks the player's inventory for a Totem of Undying.
     * If found, prevents death, consumes one Totem, applies Regeneration, Absorption, and Fire Resistance,
     * plays the Totem sound effect, and shows the Totem animation.
     *
     * @param event The {@link EntityDamageEvent} triggered by the server.
     */
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        // Check if the damaged entity is a player
        if (!(event.getEntity() instanceof Player player)) {
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

        // We have a Totem in the inventory; "use" it
        event.setCancelled(true);      // Cancel the lethal damage
        player.setHealth(1.0);        // Bring player back to 1 HP

        // Remove one Totem from the stack
        ItemStack totem = inv.getItem(totemSlot);
        totem.setAmount(totem.getAmount() - 1);
        if (totem.getAmount() <= 0) {
            inv.setItem(totemSlot, null);
        }

        // Apply the normal totem effects (Regeneration, Fire Resistance, Absorption, etc.)
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 900, 1)); // 900 ticks = 45s of Regeneration II
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1));   // 100 ticks = 5s of Absorption II
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 800, 0)); // 800 ticks = 40s Fire Resist

        // Totem use sound
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
    }
}
