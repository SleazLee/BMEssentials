package at.sleazlee.bmessentials.GivingTree;

import at.sleazlee.bmessentials.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.YamlConfiguration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * GivingTree system.
 * Handles donation of items through /donate or /trash commands and manages a
 * virtual Giving Tree inventory that is accessed by clicking the designated
 * chest blocks in the world. Items placed in the donation GUI are queued and
 * periodically inserted into the virtual tree inventory.
 */
public class GivingTree implements CommandExecutor, Listener {

    private static final String DONATE_TITLE = ChatColor.DARK_GRAY + "Donations";
    private static final String TREE_TITLE = ChatColor.DARK_GRAY + "The Giving Tree";

    private final Plugin plugin;
    private final Deque<ItemStack> donationQueue = new ConcurrentLinkedDeque<>();
    private Inventory treeInventory;
    private boolean ready;
    private final NamespacedKey itemKey;
    private final Set<UUID> claimed = ConcurrentHashMap.newKeySet();
    private final Set<UUID> disposeOnly = ConcurrentHashMap.newKeySet();
    private final Location chestLocation1;
    private final Location chestLocation2;
    private final File dataFile;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public GivingTree(Plugin plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "givingtree-id");

        org.bukkit.World world = Bukkit.getWorld("world");
        if (world == null) {
            throw new IllegalStateException("World 'world' not found");
        }
        this.chestLocation1 = new Location(world, 205, 66, 244);
        this.chestLocation2 = new Location(world, 205, 66, 243);
        plugin.getDataFolder().mkdirs();
        this.dataFile = new File(plugin.getDataFolder(), "GivingTree.yml");

        this.treeInventory = Bukkit.createInventory(null, 54, TREE_TITLE);
        this.ready = false;

        loadData(() -> {
            ready = true;
            Scheduler.runTimer(this::processQueue, 20L, 20L);
        });
    }

    /**
     * Opens the donation GUI for a player.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by a player!");
            return true;
        }

        if (player.hasPermission("bm.staff") &&
                (label.equalsIgnoreCase("trash") || label.equalsIgnoreCase("disposal"))) {
            disposeOnly.add(player.getUniqueId());
        } else {
            disposeOnly.remove(player.getUniqueId());
        }

        Location location = player.getLocation();
        player.getWorld().playSound(location, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);

        Inventory donate = Bukkit.createInventory(null, 54, DONATE_TITLE);
        player.openInventory(donate);
        return true;
    }

    /**
     * Handles donation inventory closing - items are queued for the Giving Tree.
     */
    @EventHandler
    public void onDonateClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(DONATE_TITLE)) {
            return;
        }

        Inventory inv = event.getInventory();
        Player player = (Player) event.getPlayer();
        ItemStack[] contents = inv.getContents();
        inv.clear();

        if (disposeOnly.remove(player.getUniqueId())) {
            return; // staff disposing items, do not donate
        }

        List<ItemStack> toAdd = new ArrayList<>();
        for (ItemStack item : contents) {
            if (item != null && item.getType() != Material.AIR) {
                toAdd.add(item.clone());
            }
        }
        if (!toAdd.isEmpty()) {
            Scheduler.runAsync(() -> {
                donationQueue.addAll(toAdd);
                saveData();
            });
        }
    }

    /**
     * Prevent any manipulation of the Giving Tree chest. Clicking an item gives
     * it to the player like a button.
     */
    @EventHandler
    public void onTreeClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TREE_TITLE)) {
            return;
        }

        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return; // ignore bottom inventory
        }

        switch (event.getAction()) {
            case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME -> {
                // allowed
            }
            default -> {
                return; // block shift-clicks, hotbar swaps, etc.
            }
        }

        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        String idStr = getItemId(current);
        if (idStr == null) {
            player.closeInventory();
            player.sendActionBar(mini.deserialize("<#ff3300>Oops! Too late...</#ff3300>"));
            return;
        }

        UUID id;
        try {
            id = UUID.fromString(idStr);
        } catch (IllegalArgumentException e) {
            player.closeInventory();
            player.sendActionBar(mini.deserialize("<#ff3300>Oops! Too late...</#ff3300>"));
            return;
        }

        if (!claim(id)) {
            player.closeInventory();
            player.sendActionBar(mini.deserialize("<#ff3300>Oops! Too late...</#ff3300>"));
            return;
        }

        if (player.getInventory().firstEmpty() == -1) {
            claimed.remove(id);
            return;
        }

        if (!removeFromTree(id)) {
            claimed.remove(id);
            player.closeInventory();
            player.sendActionBar(mini.deserialize("<#ff3300>Oops! Too late...</#ff3300>"));
            return;
        }

        event.setCurrentItem(null);
        ItemStack toGive = current.clone();
        clearId(toGive);
        player.getInventory().addItem(toGive);
        player.closeInventory();
        player.sendActionBar(mini.deserialize("<green>Yeah! You got it!</green>"));
    }

    /**
     * Prevent dragging items into the Giving Tree chest.
     */
    @EventHandler
    public void onTreeDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equals(TREE_TITLE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTreeInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }
        Location loc = clicked.getLocation();
        if (!(loc.equals(chestLocation1) || loc.equals(chestLocation2))) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (!ready) {
            player.sendMessage(mini.deserialize("<#ff3300>The Giving Tree isn't ready yet!</#ff3300>"));
            return;
        }

        // Require at least one empty inventory slot before opening the GUI
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(mini.deserialize("<#ff3300>You need at least one free slot to access the Giving Tree!</#ff3300>"));
            return;
        }

        ItemStack[] master = treeInventory.getContents();
        for (int i = 0; i < master.length; i++) {
            ItemStack item = master[i];
            if (item != null && getItemId(item) == null) {
                assignId(item);
                master[i] = item;
            }
        }
        treeInventory.setContents(master);

        player.openInventory(treeInventory);
    }

    private void loadData(Runnable postLoad) {
        Scheduler.runAsync(() -> {
            if (!dataFile.exists()) {
                if (postLoad != null) {
                    Scheduler.run(postLoad);
                }
                return;
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
            ItemStack[] chestItems = null;
            if (treeInventory != null) {
                List<?> chestList = config.getList("chest");
                if (chestList != null) {
                    chestItems = new ItemStack[treeInventory.getSize()];
                    for (int i = 0; i < Math.min(chestList.size(), chestItems.length); i++) {
                        Object obj = chestList.get(i);
                        if (obj instanceof ItemStack stack) {
                            chestItems[i] = stack;
                        }
                    }
                }
            }
            List<ItemStack> queueItems = new ArrayList<>();
            List<?> queueList = config.getList("queue");
            if (queueList != null) {
                for (Object obj : queueList) {
                    if (obj instanceof ItemStack stack) {
                        queueItems.add(stack);
                    }
                }
            }

            ItemStack[] finalChestItems = chestItems;
            Scheduler.run(() -> {
                if (treeInventory != null && finalChestItems != null) {
                    treeInventory.setContents(finalChestItems);
                }
                if (!queueItems.isEmpty()) {
                    donationQueue.addAll(queueItems);
                }
                if (postLoad != null) {
                    postLoad.run();
                }
            });
        });
    }

    public void saveData() {
        if (treeInventory == null) {
            return;
        }
        ItemStack[] chest = treeInventory.getContents();
        ItemStack[] queue = donationQueue.toArray(new ItemStack[0]);

        Runnable saveTask = () -> {
            YamlConfiguration config = new YamlConfiguration();
            config.set("chest", chest);
            config.set("queue", queue);
            try {
                config.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save Giving Tree data: " + e.getMessage());
            }
        };

        if (plugin.isEnabled()) {
            Scheduler.runAsync(saveTask);
        } else {
            saveTask.run();
        }
    }

    /**
     * Processes the donation queue, inserting one item into the Giving Tree
     * chest.
     */
    private void processQueue() {
        if (treeInventory == null) {
            return;
        }

        ItemStack next = donationQueue.poll();
        if (next == null) {
            return;
        }

        assignId(next);
        ItemStack[] contents = treeInventory.getContents();
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : contents) {
            if (item != null && item.getType() != Material.AIR) {
                items.add(item);
            }
        }
        ItemStack[] newContents = new ItemStack[treeInventory.getSize()];
        newContents[0] = next;
        for (int i = 0; i < Math.min(items.size(), newContents.length - 1); i++) {
            newContents[i + 1] = items.get(i);
        }
        treeInventory.setContents(newContents);
        saveData();
    }

    private void assignId(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        item.setItemMeta(meta);
    }

    private String getItemId(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
    }

    private void clearId(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().remove(itemKey);
        item.setItemMeta(meta);
    }

    private boolean claim(UUID id) {
        boolean added = claimed.add(id);
        if (added) {
            Scheduler.runLater(() -> claimed.remove(id), 400L);
        }
        return added;
    }

    private boolean removeFromTree(UUID id) {
        if (treeInventory == null) {
            return false;
        }

        ItemStack[] contents = treeInventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null) {
                continue;
            }
            String other = getItemId(item);
            if (other == null) {
                continue;
            }
            try {
                if (UUID.fromString(other).equals(id)) {
                    treeInventory.setItem(i, null);
                    saveData();
                    return true;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return false;
    }
}