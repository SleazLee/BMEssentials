package at.sleazlee.bmessentials.PurpurFeatures;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.block.Block;

public class DragonEggTPFix implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only look at left/right clicks on blocks
        if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block != null && block.getType() == Material.DRAGON_EGG) {
                // Canceling prevents the egg from teleporting
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        // Dragon egg sometimes moves/teleports due to block updates
        if (event.getBlock().getType() == Material.DRAGON_EGG) {
            // Cancel these events so it can't randomly move
            event.setCancelled(true);
        }
    }
}
