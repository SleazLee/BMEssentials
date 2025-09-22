package at.sleazlee.bmessentials.GivingTree;

import at.sleazlee.bmessentials.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
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
import java.util.concurrent.ThreadLocalRandom;

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
    private static final String SUCCESS_MESSAGES_PATH = "Systems.Containers.GivingTree.SuccessMessages";
    private static final String FAILURE_MESSAGES_PATH = "Systems.Containers.GivingTree.FailureMessages";
    private static final String DEFAULT_SUCCESS_MESSAGE = "<green>Yeah! You got it!</green>";
    private static final String DEFAULT_FAILURE_MESSAGE = "<#ff3300>Oops! Too late...</#ff3300>";

    private final Plugin plugin;
    private final Deque<ItemStack> donationQueue = new ConcurrentLinkedDeque<>();
    private Inventory treeInventory;
    private boolean ready;
    private final Set<UUID> claimed = ConcurrentHashMap.newKeySet();
    private final Set<UUID> disposeOnly = ConcurrentHashMap.newKeySet();
    private final Location chestLocation1;
    private final Location chestLocation2;
    private final File dataFile;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private final List<String> successMessages;
    private final List<String> failureMessages;
    private final UUID[] slotIds;

    public GivingTree(Plugin plugin) {
        this.plugin = plugin;
        org.bukkit.World world = Bukkit.getWorld("world");
        if (world == null) {
            throw new IllegalStateException("World 'world' not found");
        }
        this.chestLocation1 = new Location(world, 198, 66, 241);
        this.chestLocation2 = new Location(world, 198, 66, 240);
        plugin.getDataFolder().mkdirs();
        this.dataFile = new File(plugin.getDataFolder(), "GivingTree.yml");

        this.successMessages = loadMessages(SUCCESS_MESSAGES_PATH, DEFAULT_SUCCESS_MESSAGE);
        this.failureMessages = loadMessages(FAILURE_MESSAGES_PATH, DEFAULT_FAILURE_MESSAGE);

        this.treeInventory = Bukkit.createInventory(null, 54, TREE_TITLE);
        this.slotIds = new UUID[treeInventory.getSize()];
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

        int slot = event.getSlot();
        if (slot < 0 || slot >= slotIds.length) {
            sendFailureMessage(player);
            return;
        }

        UUID id = slotIds[slot];
        if (id == null) {
            sendFailureMessage(player);
            return;
        }

        if (!claim(id)) {
            sendFailureMessage(player);
            return;
        }

        if (player.getInventory().firstEmpty() == -1) {
            claimed.remove(id);
            return;
        }

        ItemStack toGive = removeFromTree(slot, id);
        if (toGive == null) {
            claimed.remove(id);
            sendFailureMessage(player);
            return;
        }

        event.setCurrentItem(null);
        player.getInventory().addItem(toGive);
        sendSuccessMessage(player);
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

        syncSlotIdsWithContents();

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
                    syncSlotIdsWithContents();
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

        ItemStack[] contents = treeInventory.getContents();
        List<ItemStack> items = new ArrayList<>();
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() != Material.AIR) {
                items.add(item);
                ids.add(slotIds[i]);
            }
        }

        ItemStack[] newContents = new ItemStack[treeInventory.getSize()];
        UUID[] newIds = new UUID[slotIds.length];
        newContents[0] = next;
        newIds[0] = UUID.randomUUID();

        int limit = Math.min(items.size(), newContents.length - 1);
        for (int i = 0; i < limit; i++) {
            newContents[i + 1] = items.get(i);
            UUID existingId = ids.get(i);
            if (existingId == null) {
                existingId = UUID.randomUUID();
            }
            newIds[i + 1] = existingId;
        }

        treeInventory.setContents(newContents);
        System.arraycopy(newIds, 0, slotIds, 0, slotIds.length);
        saveData();
    }

    private boolean claim(UUID id) {
        boolean added = claimed.add(id);
        if (added) {
            Scheduler.runLater(() -> claimed.remove(id), 400L);
        }
        return added;
    }

    private ItemStack removeFromTree(int slot, UUID expectedId) {
        if (treeInventory == null) {
            return null;
        }
        if (slot < 0 || slot >= treeInventory.getSize()) {
            return null;
        }

        UUID currentId = slotIds[slot];
        if (currentId == null || !currentId.equals(expectedId)) {
            return null;
        }

        ItemStack item = treeInventory.getItem(slot);
        if (item == null || item.getType() == Material.AIR) {
            slotIds[slot] = null;
            return null;
        }

        ItemStack result = item.clone();
        treeInventory.setItem(slot, null);
        slotIds[slot] = null;
        saveData();
        return result;
    }

    private void syncSlotIdsWithContents() {
        if (treeInventory == null) {
            return;
        }
        ItemStack[] contents = treeInventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) {
                slotIds[i] = null;
            } else if (slotIds[i] == null) {
                slotIds[i] = UUID.randomUUID();
            }
        }
    }

    private List<String> loadMessages(String path, String defaultMessage) {
        List<String> configured = plugin.getConfig().getStringList(path);
        List<String> messages = new ArrayList<>();
        for (String message : configured) {
            if (message != null && !message.isBlank()) {
                messages.add(message);
            }
        }
        if (messages.isEmpty()) {
            messages.add(defaultMessage);
        }
        return messages;
    }

    private void sendFailureMessage(Player player) {
        sendRandomActionBar(player, failureMessages);
    }

    private void sendSuccessMessage(Player player) {
        sendRandomActionBar(player, successMessages);
    }

    private void sendRandomActionBar(Player player, List<String> messages) {
        if (messages.isEmpty()) {
            return;
        }
        String rawMessage = messages.get(ThreadLocalRandom.current().nextInt(messages.size()));
        player.sendActionBar(mini.deserialize(rawMessage));
    }
}
