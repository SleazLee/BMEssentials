
package at.sleazlee.bmessentials.Combinations;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Applies custom crafting table combinations when a matching matrix is present.
 */
public class CraftingCombinationListener implements Listener {
    private final Combinations combinations;

    public CraftingCombinationListener(Combinations combinations) {
        this.combinations = combinations;
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack[] matrix = inv.getMatrix();
        Combinations.CraftingCombination combo = combinations.matchCrafting(matrix);
        if (combo != null) {
            inv.setResult(combo.getResult());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof CraftingInventory)) {
            return;
        }
        if (event.getSlotType() != SlotType.RESULT) {
            return;
        }

        CraftingInventory inv = (CraftingInventory) event.getInventory();
        ItemStack[] matrix = inv.getMatrix();
        Combinations.CraftingCombination combo = combinations.matchCrafting(matrix);
        if (combo == null) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack result = combo.getResult();

        if (event.isShiftClick()) {
            int crafts = Integer.MAX_VALUE;
            for (ItemStack item : matrix) {
                if (item != null && item.getType() != Material.AIR) {
                    crafts = Math.min(crafts, item.getAmount());
                }
            }
            ItemStack toGive = result.clone();
            toGive.setAmount(result.getAmount() * crafts);
            PlayerInventory pinv = player.getInventory();
            java.util.Map<Integer, ItemStack> leftover = pinv.addItem(toGive);
            leftover.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
            for (int i = 0; i < matrix.length; i++) {
                ItemStack item = matrix[i];
                if (item != null && item.getType() != Material.AIR) {
                    int remaining = item.getAmount() - crafts;
                    if (remaining <= 0) {
                        inv.setItem(i, null);
                    } else {
                        item.setAmount(remaining);
                        inv.setItem(i, item);
                    }
                }
            }
        } else {
            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                if (!cursor.isSimilar(result) || cursor.getAmount() + result.getAmount() > cursor.getMaxStackSize()) {
                    return;
                }
                cursor.setAmount(cursor.getAmount() + result.getAmount());
                event.getView().setCursor(cursor);
            } else {
                event.getView().setCursor(result);
            }

            for (int i = 0; i < matrix.length; i++) {
                ItemStack item = matrix[i];
                if (item != null && item.getType() != Material.AIR) {
                    int remaining = item.getAmount() - 1;
                    if (remaining <= 0) {
                        inv.setItem(i, null);
                    } else {
                        item.setAmount(remaining);
                        inv.setItem(i, item);
                    }
                }
            }
        }

        inv.setResult(new ItemStack(Material.AIR));
    }
}