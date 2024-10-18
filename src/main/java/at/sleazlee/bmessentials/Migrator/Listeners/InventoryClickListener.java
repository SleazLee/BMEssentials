package at.sleazlee.bmessentials.Migrator.Listeners;

import at.sleazlee.bmessentials.Migrator.MigratorManager;
import at.sleazlee.bmessentials.Migrator.VirtualChestGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Listens for inventory click events in the virtual chest GUI.
 */
public class InventoryClickListener implements Listener {

    private MigratorManager migratorManager;

    /**
     * Constructs the event listener.
     *
     * @param migratorManager the migrator manager instance
     */
    public InventoryClickListener(MigratorManager migratorManager) {
        this.migratorManager = migratorManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player)) return;

        if (event.getInventory().getHolder() instanceof VirtualChestGUI) {
            // Prevent item insertion
            if (event.isShiftClick() || event.getClick().isKeyboardClick()) {
                event.setCancelled(true);
                return;
            }
            if (event.getRawSlot() < event.getInventory().getSize()) {
                // Allow taking items out
                // Do nothing here
            } else {
                // Prevent placing items into the GUI
                event.setCancelled(true);
            }

            // After the event, update the database
            VirtualChestGUI gui = (VirtualChestGUI) event.getInventory().getHolder();
            gui.saveInventory();
        }
    }
}