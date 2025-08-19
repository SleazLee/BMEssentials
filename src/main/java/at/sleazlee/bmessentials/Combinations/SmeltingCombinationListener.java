package at.sleazlee.bmessentials.Combinations;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Applies custom smelting combinations in furnaces.
 */
public class SmeltingCombinationListener implements Listener {
    private final Combinations combinations;

    public SmeltingCombinationListener(Combinations combinations) {
        this.combinations = combinations;
    }

    @EventHandler
    public void onSmelt(FurnaceSmeltEvent event) {
        ItemStack source = event.getSource();
        Combinations.SmeltingCombination combo = combinations.matchSmelting(source);
        if (combo != null) {
            event.setResult(combo.getResult());
        }
    }
}