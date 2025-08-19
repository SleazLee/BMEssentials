package at.sleazlee.bmessentials.Combinations;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
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
}