package at.sleazlee.bmessentials.Combinations;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.AnvilView;

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
        if (!(event.getInventory() instanceof AnvilInventory inv) || !(event.getView() instanceof AnvilView view)) {
            return;
        }
        if (event.getRawSlot() != 2) {
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
        inv.setItem(0, null);
        inv.setItem(1, null);
        view.setRepairCost(0);
        ItemStack result = combo.getResult();
        if (event.isShiftClick()) {
            Player player = (Player) event.getWhoClicked();
            player.getInventory().addItem(result);
        } else {
            event.setCursor(result);
        }
    }
}