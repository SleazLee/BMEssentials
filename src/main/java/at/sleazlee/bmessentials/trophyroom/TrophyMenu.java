package at.sleazlee.bmessentials.trophyroom;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * The Menu class handles the creation and management of the Trophy Room inventory GUI.
 * It builds the inventory, handles item placements, and ensures only trophy items can be placed.
 */
public class TrophyMenu implements Listener {

    private final JavaPlugin plugin;
    private final TrophyDatabase trophiesDB;

    // Map to keep track of which player's trophy room is being viewed by which viewer
    private final Map<UUID, UUID> viewerToOwnerMap = new HashMap<>();

    // NamespacedKey for identifying trophy items
    private final NamespacedKey trophyKey;

    /**
     * Constructor for the Menu class.
     *
     * @param plugin   The JavaPlugin instance.
     * @param database The Database instance for storing and retrieving trophy items.
     */
    public TrophyMenu(JavaPlugin plugin, TrophyDatabase database) {
        this.plugin = plugin;
        this.trophiesDB = database;
        this.trophyKey = new NamespacedKey(plugin, "trophy_item");
        // Register this class as an event listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Opens the trophy room menu for a player.
     *
     * @param viewer The player who is viewing the trophy room.
     * @param owner  The owner of the trophy room (can be online or offline).
     */
    public void openMenu(Player viewer, OfflinePlayer owner) {
        // Build the inventory
        Inventory trophyRoom = buildInventory(owner);

        // Keep track of who is viewing whose inventory
        viewerToOwnerMap.put(viewer.getUniqueId(), owner.getUniqueId());

        // Open the inventory for the viewer
        viewer.openInventory(trophyRoom);
    }

    /**
     * Builds the trophy room inventory for a given player.
     *
     * @param owner The owner of the trophy room (can be online or offline).
     * @return The Inventory representing the trophy room.
     */
    private Inventory buildInventory(OfflinePlayer owner) {
        // Set the inventory name with color codes
        String ownerName = owner.getName();
        if (ownerName == null) {
            ownerName = "Unknown";
        }
        String inventoryName = ChatColor.BLACK + ownerName + "'s Trophy Room";

        // Create a new inventory with 5 rows (45 slots)
        Inventory inventory = Bukkit.createInventory(null, 45, inventoryName);

        // **Create border items**
        ItemStack grayPane = createItem(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + " ");
        ItemStack closeButton = createItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "" + ChatColor.BOLD + "Close");

        // **Set border items in the first row (slots 0-7)**
        for (int slot = 0; slot <= 7; slot++) {
            inventory.setItem(slot, grayPane);
        }

        // **Set the close button in slot 8**
        inventory.setItem(8, closeButton);

        // **Set border items in the last row (slots 36-44)**
        for (int slot = 36; slot <= 44; slot++) {
            inventory.setItem(slot, grayPane);
        }

        // Load the trophy items from the database
        String contentsString = trophiesDB.getContents(owner.getUniqueId());
        if (contentsString != null && !contentsString.isEmpty()) {
            try {
                // Deserialize the items and place them in the inventory
                Map<Integer, ItemStack> contents = deserializeInventory(contentsString);
                for (Map.Entry<Integer, ItemStack> entry : contents.entrySet()) {
                    int slot = entry.getKey();
                    ItemStack item = entry.getValue();
                    // Ensure we don't overwrite border items
                    if (!isBorderSlot(slot)) {
                        inventory.setItem(slot, item);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                plugin.getLogger().log(Level.SEVERE, "Error deserializing inventory contents for player " + ownerName, e);
            }
        }

        return inventory;
    }


    /**
     * Creates an ItemStack with the specified material and display name.
     *
     * @param material The material of the item.
     * @param name     The display name of the item.
     * @return The created ItemStack.
     */
    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Set the display name of the item
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Checks if a slot is a border slot.
     *
     * @param slot The slot index.
     * @return True if the slot is a border slot, false otherwise.
     */
    private boolean isBorderSlot(int slot) {
        // First row slots (0-8)
        if (slot >= 0 && slot <= 8) {
            return true;
        }
        // Last row slots (36-44)
        if (slot >= 36 && slot <= 44) {
            return true;
        }
        return false;
    }

    /**
     * Serializes the inventory contents (excluding border items) to a Base64-encoded string.
     *
     * @param inventory The inventory to serialize.
     * @return The Base64-encoded string representing the inventory contents.
     * @throws IOException If an I/O error occurs during serialization.
     */
    private String serializeInventory(Inventory inventory) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

        // Temporarily store items to be serialized
        Map<Integer, ItemStack> itemsToSerialize = new HashMap<>();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (isBorderSlot(slot)) {
                continue; // Skip border slots
            }
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                itemsToSerialize.put(slot, item);
            }
        }

        // Write the number of items to be serialized
        dataOutput.writeInt(itemsToSerialize.size());

        // Write each slot and its corresponding item
        for (Map.Entry<Integer, ItemStack> entry : itemsToSerialize.entrySet()) {
            dataOutput.writeInt(entry.getKey()); // Slot index
            dataOutput.writeObject(entry.getValue()); // ItemStack
        }

        dataOutput.close();
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    /**
     * Deserializes the inventory contents from a Base64-encoded string.
     *
     * @param data The Base64-encoded string representing the inventory contents.
     * @return A map of slot indexes to ItemStacks.
     * @throws IOException            If an I/O error occurs during deserialization.
     * @throws ClassNotFoundException If the class of a serialized object cannot be found.
     */
    private Map<Integer, ItemStack> deserializeInventory(String data) throws IOException, ClassNotFoundException {
        Map<Integer, ItemStack> items = new HashMap<>();

        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);

        // Read the number of items stored
        int itemCount = dataInput.readInt();

        // Read each slot and its corresponding item
        for (int i = 0; i < itemCount; i++) {
            int slot = dataInput.readInt();
            ItemStack item = (ItemStack) dataInput.readObject();
            items.put(slot, item);
        }

        dataInput.close();
        return items;
    }

    /**
     * Checks if an ItemStack is a trophy item by looking for a custom NBT tag.
     *
     * @param item The ItemStack to check.
     * @return True if the item is a trophy item, false otherwise.
     */
    private boolean isTrophyItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Check if the PersistentDataContainer has the key
            Byte result = meta.getPersistentDataContainer().get(trophyKey, PersistentDataType.BYTE);
            return result != null && result == (byte) 1;
        }
        return false;
    }


    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        // Check if the inventory is a trophy room by its title
        if (event.getView().getTitle().contains("'s Trophy Room")) {

            // Get the owner of the trophy room
            UUID ownerUUID = viewerToOwnerMap.get(player.getUniqueId());
            if (ownerUUID == null) {
                return;
            }

            // Determine if the player is the owner of the trophy room
            boolean isOwner = player.getUniqueId().equals(ownerUUID);

            // If the player is not the owner, prevent any interactions
            if (!isOwner) {
                event.setCancelled(true);
                return;
            }

            // Check if any of the slots involved are in the trophy room (top inventory)
            for (int slot : event.getRawSlots()) {
                if (slot < event.getView().getTopInventory().getSize()) {
                    // Slot is in the trophy room
                    ItemStack draggedItem = event.getOldCursor();
                    if (draggedItem != null && !isTrophyItem(draggedItem)) {
                        event.setCancelled(true);
                        String message = ChatColor.of("#ff3300") + "" + ChatColor.BOLD + "BM " + ChatColor.RED + "You can only place Trophies into your Trophy Room!";
                        player.sendMessage(message);
                        return;
                    }
                }
            }

            // If all items are trophy items, allow the drag
            event.setCancelled(false);
        }
    }



    /**
     * Handles clicks in the trophy room inventory.
     *
     * @param event The InventoryClickEvent.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        // Check if the inventory is a trophy room by its title
        if (event.getView().getTitle().contains("'s Trophy Room")) {

            // Cancel the event by default
            event.setCancelled(true);

            int slot = event.getRawSlot();

            // If clicked outside inventory, do nothing
            if (slot < 0) {
                return;
            }

            // **Handle clicks on border items first**
            if (isBorderSlot(slot)) {
                // If clicked on the close button
                if (slot == 8 && event.getCurrentItem() != null
                        && event.getCurrentItem().getType() == Material.RED_STAINED_GLASS_PANE) {
                    player.closeInventory();
                }
                return; // Return after handling border items
            }

            // **Get the owner of the trophy room**
            UUID ownerUUID = viewerToOwnerMap.get(player.getUniqueId());
            if (ownerUUID == null) {
                return;
            }

            // **Determine if the player is the owner**
            boolean isOwner = player.getUniqueId().equals(ownerUUID);

            // **If the player is not the owner, prevent any interactions beyond closing the inventory**
            if (!isOwner) {
                return; // Viewers cannot interact further
            }

            // **From here on, handle interactions for the owner**

            Inventory clickedInventory = event.getClickedInventory();
            Inventory topInventory = event.getView().getTopInventory();
            Inventory bottomInventory = event.getView().getBottomInventory();

            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                if (clickedInventory != null && clickedInventory.equals(bottomInventory)) {
                    // Shift-clicking from player's inventory into trophy room
                    ItemStack currentItem = event.getCurrentItem();
                    if (currentItem != null && isTrophyItem(currentItem)) {
                        event.setCancelled(false); // Allow moving trophy items
                    } else {
                        String message = ChatColor.of("#ff3300") + "" + ChatColor.BOLD + "BM " + ChatColor.RED + "You can only place Trophies into your Trophy Room!";
                        player.sendMessage(message);
                    }
                } else if (clickedInventory != null && clickedInventory.equals(topInventory)) {
                    // Shift-clicking from trophy room into player's inventory
                    event.setCancelled(false); // Allow moving items out
                }
                return;
            }

            if (clickedInventory != null && clickedInventory.equals(topInventory)) {
                // Handle interactions with the trophy room inventory
                switch (event.getAction()) {
                    case PICKUP_ALL:
                    case PICKUP_HALF:
                    case PICKUP_SOME:
                    case PICKUP_ONE:
                    case DROP_ALL_SLOT:
                    case DROP_ONE_SLOT:
                        // Allow picking up items from the trophy room
                        event.setCancelled(false);
                        break;
                    case PLACE_ALL:
                    case PLACE_ONE:
                    case PLACE_SOME:
                    case SWAP_WITH_CURSOR:
                        // Allow placing items into the trophy room if they are trophy items
                        ItemStack cursorItem = event.getCursor();
                        if (cursorItem != null && isTrophyItem(cursorItem)) {
                            event.setCancelled(false); // Allow the action
                        } else {
                            String message = ChatColor.of("#ff3300") + "" + ChatColor.BOLD + "BM " + ChatColor.RED + "You can only place Trophies into your Trophy Room!";
                            player.sendMessage(message);
                        }
                        break;
                    case HOTBAR_SWAP:
                        // Handle swapping with hotbar
                        ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
                        if (hotbarItem != null && isTrophyItem(hotbarItem)) {
                            event.setCancelled(false);
                        } else {
                            String message = ChatColor.of("#ff3300") + "" + ChatColor.BOLD + "BM " + ChatColor.RED + "You can only place Trophies into your Trophy Room!";
                            player.sendMessage(message);
                        }
                        break;
                    default:
                        // Other actions are cancelled
                        break;
                }
            } else if (clickedInventory != null && clickedInventory.equals(bottomInventory)) {
                // Allow the player to interact with their own inventory
                event.setCancelled(false);
            } else {
                // For any other cases, cancel the event
                event.setCancelled(true);
            }
        }
    }





    /**
     * Handles when a player closes the trophy room inventory.
     *
     * @param event The InventoryCloseEvent.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();

        // Check if the inventory is a trophy room
        if (event.getView().getTitle().contains("'s Trophy Room")) {
            handleInventoryClose(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Check if the player has a trophy room open
        if (viewerToOwnerMap.containsKey(player.getUniqueId())) {
            // Manually call the inventory close handler
            handleInventoryClose(player);
        }
    }

    // Helper method to handle inventory closure
    private void handleInventoryClose(Player player) {
        // Get the owner of the trophy room
        UUID ownerUUID = viewerToOwnerMap.get(player.getUniqueId());
        if (ownerUUID == null) {
            return;
        }

        // Determine if the player is the owner
        boolean isOwner = player.getUniqueId().equals(ownerUUID);

        // Remove the player from the viewerToOwnerMap
        viewerToOwnerMap.remove(player.getUniqueId());

        if (isOwner) {
            // Save the inventory contents to the database
            Inventory inventory = player.getOpenInventory().getTopInventory();
            try {
                String contentsString = serializeInventory(inventory);
                trophiesDB.setContents(ownerUUID, contentsString);

                // Calculate and update the trophy count
                int trophyCount = calculateTrophyCount(inventory);
                trophiesDB.setTrophyCount(ownerUUID, trophyCount);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Error serializing inventory contents for player " + player.getName(), e);
            }
        }
    }

    /**
     * Calculates the number of trophy items in the inventory.
     *
     * @param inventory The inventory to calculate the count from.
     * @return The number of trophy items.
     */
    private int calculateTrophyCount(Inventory inventory) {
        int count = 0;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (isBorderSlot(slot)) {
                continue; // Skip border slots
            }
            ItemStack item = inventory.getItem(slot);
            if (item != null && isTrophyItem(item)) {
                count += item.getAmount(); // If items can stack
                // If items cannot stack (e.g., each trophy is unique), use count++
            }
        }
        return count;
    }

}
