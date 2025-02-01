package at.sleazlee.bmessentials.bmefunctions;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class CropTrampleListener implements Listener {

    // Handles players stepping on farmland
    @EventHandler
    public void onPlayerTrample(PlayerInteractEvent event) {
        // Only proceed if the action is physical (i.e. stepping on the block)
        if (event.getAction() == Action.PHYSICAL) {
            if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.FARMLAND) {
                event.setCancelled(true);
            }
        }
    }

    // Handles mobs (and any other entity) interacting with farmland
    @EventHandler
    public void onEntityTrample(EntityInteractEvent event) {
        if (event.getBlock() != null && event.getBlock().getType() == Material.FARMLAND) {
            event.setCancelled(true);
        }
    }
}