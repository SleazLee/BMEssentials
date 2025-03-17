package at.sleazlee.bmessentials.PurpurFeatures;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.entity.EntityType;

import java.util.Random;

public class SuperRareSkeletonHorses implements Listener {

    private final Random random = new Random();

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() == EntityType.SKELETON_HORSE) {
            // 1 out of 10 chance to allow spawn, making them 10x rarer
            if (random.nextInt(10) != 0) {
                event.setCancelled(true);
            }
        }
    }
}

