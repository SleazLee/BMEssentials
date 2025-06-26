package at.sleazlee.bmessentials.PurpurFeatures;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.SpawnerSpawnEvent;

/**
 * Disables mob spawner activation when the block is powered by redstone.
 * Cancels {@link SpawnerSpawnEvent} if the spawner has power, effectively
 * mimicking the Purpur feature that allows redstone to toggle spawners.
 */
public class RedstoneDisableSpawner implements Listener {

    @EventHandler
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        Block spawnerBlock = event.getSpawner().getBlock();
        if (spawnerBlock.isBlockPowered() || spawnerBlock.isBlockIndirectlyPowered()) {
            event.setCancelled(true);
        }
    }
}