package at.sleazlee.bmessentials.CombineSystem;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.Map;

/**
 * Allows players to apply unsafe enchantment levels using anvils
 * while preventing the creation of new unsafe books.
 */
public class CombineListener implements Listener {

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        ItemStack left = inventory.getItem(0);
        ItemStack right = inventory.getItem(1);

        if (left == null || right == null) {
            return;
        }

        boolean leftBook = left.getType() == Material.ENCHANTED_BOOK;
        boolean rightBook = right.getType() == Material.ENCHANTED_BOOK;

        // Prevent combining two books to create unsafe levels
        if (leftBook && rightBook) {
            event.setResult(null);
            return;
        }

        if (!(leftBook ^ rightBook)) {
            return; // only process when exactly one item is a book
        }

        ItemStack item = leftBook ? right : left;
        ItemStack book = leftBook ? left : right;
        if (!(book.getItemMeta() instanceof EnchantmentStorageMeta meta)) {
            return;
        }

        ItemStack result = item.clone();
        for (Map.Entry<Enchantment, Integer> entry : meta.getStoredEnchants().entrySet()) {
            result.addUnsafeEnchantment(entry.getKey(), entry.getValue());
        }

        event.setResult(result);
    }
}