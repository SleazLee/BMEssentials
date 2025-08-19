package at.sleazlee.bmessentials.Combinations;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

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
}