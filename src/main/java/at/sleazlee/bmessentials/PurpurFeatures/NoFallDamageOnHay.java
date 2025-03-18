package at.sleazlee.bmessentials.PurpurFeatures;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class NoFallDamageOnHay  implements Listener {

    /**
     * Handles fall damage events and cancels damage if the player lands on a hay block.
     *
     * @param event The EntityDamageEvent triggered when an entity takes damage.
     */
    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        // Check if the damage is caused by falling and if the entity is a player
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            // Get the block directly below the player's feet
            Block blockBelow = player.getLocation().subtract(0, 1, 0).getBlock();
            // If the block is a hay block, cancel the fall damage
            if (blockBelow.getType() == Material.HAY_BLOCK) {
                event.setCancelled(true);
            }
        }
    }
}
