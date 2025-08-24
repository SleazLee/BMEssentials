package at.sleazlee.bmessentials.Combinations;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

/**
 * Listens for anvil prepares and applies custom combinations when matched.
 */
public class AnvilCombinationListener implements Listener {
    private final Combinations combinations;

    public AnvilCombinationListener(Combinations combinations) {
        this.combinations = combinations;
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack left = event.getInventory().getItem(0);
        ItemStack right = event.getInventory().getItem(1);
        if (left == null || right == null) {
            return;
        }
        Combinations.AnvilCombination combo = combinations.matchAnvil(left, right);
        if (combo != null) {
            event.setResult(combo.getResult());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof AnvilInventory inv)) {
            return;
        }
        if (event.getSlotType() != SlotType.RESULT) {
            return;
        }

        ItemStack left = inv.getItem(0);
        ItemStack right = inv.getItem(1);
        if (left == null || right == null) {
            return;
        }

        Combinations.AnvilCombination combo = combinations.matchAnvil(left, right);
        if (combo == null) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack result = combo.getResult();

        if (event.isShiftClick()) {
            java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(result);
            leftover.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
        } else {
            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                if (!cursor.isSimilar(result) || cursor.getAmount() + result.getAmount() > cursor.getMaxStackSize()) {
                    return;
                }
                cursor.setAmount(cursor.getAmount() + result.getAmount());
                event.setCursor(cursor);
            } else {
                event.setCursor(result);
            }
        }

        consume(inv, 0);
        consume(inv, 1);
        inv.setItem(2, new ItemStack(Material.AIR));
        inv.setRepairCost(0);
    }

    private void consume(AnvilInventory inv, int slot) {
        ItemStack item = inv.getItem(slot);
        if (item == null) {
            return;
        }
        int remaining = item.getAmount() - 1;
        if (remaining <= 0) {
            inv.setItem(slot, null);
        } else {
            item.setAmount(remaining);
            inv.setItem(slot, item);
        }
    }
}
