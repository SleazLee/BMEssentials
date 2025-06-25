package at.sleazlee.bmessentials.PurpurFeatures;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Allows players to rapidly compost an entire stack of items by
 * sneaking and right-clicking a composter.
 */
public class ComposterBulkProcess implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.COMPOSTER) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }

        EquipmentSlot hand = event.getHand();
        if (hand == null) {
            return;
        }

        ItemStack item = player.getInventory().getItem(hand);
        if (item == null || !item.getType().isCompostable()) {
            return;
        }

        event.setCancelled(true);

        Levelled data = (Levelled) block.getBlockData();
        int level = data.getLevel();
        int max = data.getMaximumLevel();

        int amount = item.getAmount();
        int used = 0;
        while (level < max && amount - used > 0) {
            used++;
            if (ThreadLocalRandom.current().nextFloat() <= item.getType().getCompostChance()) {
                level++;
            }
        }

        data.setLevel(level);
        block.setBlockData(data, true);

        item.setAmount(amount - used);
        player.getInventory().setItem(hand, item.getAmount() > 0 ? item : null);
    }
}