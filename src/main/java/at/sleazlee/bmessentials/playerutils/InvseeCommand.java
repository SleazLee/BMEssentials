package at.sleazlee.bmessentials.playerutils;

import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTFile;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTCompoundList;
import de.tr7zw.nbtapi.NBTListCompound;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

/**
 * Command to view and edit another player's inventory.
 */
public class InvseeCommand implements CommandExecutor, Listener {

    private final JavaPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private enum ViewType { INVENTORY, ECHEST, ARMOR }

    private static class ViewSession {
        final UUID target;
        final ViewType type;
        ViewSession(UUID target, ViewType type) {
            this.target = target;
            this.type = type;
        }
    }

    private final Map<UUID, ViewSession> viewers = new HashMap<>();

    private final ItemStack filler;

    public InvseeCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.filler = createFiller();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>This command can only be used by players.</red>"));
            return true;
        }
        if (!player.hasPermission("bmessentials.invsee.use")) {
            player.sendMessage(mm.deserialize("<red>You do not have permission to use this command.</red>"));
            return true;
        }
        if (args.length < 1 || args.length > 2) {
            player.sendMessage(mm.deserialize("<gray>Usage: /invsee <player> [inventory|echest|armor]</gray>"));
            return true;
        }

        String targetName = args[0];
        ViewType type = ViewType.INVENTORY;
        if (args.length == 2) {
            switch (args[1].toLowerCase()) {
                case "echest" -> type = ViewType.ECHEST;
                case "armor" -> type = ViewType.ARMOR;
                default -> type = ViewType.INVENTORY;
            }
        }
        Player targetOnline = Bukkit.getPlayerExact(targetName);
        if (targetOnline != null) {
            Inventory inv;
            if (type == ViewType.INVENTORY) {
                inv = Bukkit.createInventory(null, 54, targetOnline.getName() + "'s Inventory");
                inv.setContents(targetOnline.getInventory().getContents());
            } else if (type == ViewType.ECHEST) {
                inv = Bukkit.createInventory(null, 27, targetOnline.getName() + "'s Ender Chest");
                inv.setContents(targetOnline.getEnderChest().getContents());
            } else { // ARMOR
                inv = Bukkit.createInventory(null, 9, targetOnline.getName() + "'s Armor");
                ItemStack[] armor = targetOnline.getInventory().getArmorContents();
                inv.setItem(0, armor[3]); // helmet
                inv.setItem(1, armor[2]); // chest
                inv.setItem(2, armor[1]); // legs
                inv.setItem(3, armor[0]); // boots
                for (int i = 4; i <= 7; i++) {
                    inv.setItem(i, filler);
                }
                inv.setItem(8, targetOnline.getInventory().getItemInOffHand());
            }
            player.openInventory(inv);
            viewers.put(player.getUniqueId(), new ViewSession(targetOnline.getUniqueId(), type));
            return true;
        }

        OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(targetName);
        if (!targetOffline.hasPlayedBefore()) {
            player.sendMessage(mm.deserialize("<red>Player not found.</red>"));
            return true;
        }

        Inventory inv;
        if (type == ViewType.INVENTORY) {
            inv = loadOfflineInventory(targetOffline);
        } else if (type == ViewType.ECHEST) {
            inv = loadOfflineEnderChest(targetOffline);
        } else {
            inv = loadOfflineArmor(targetOffline);
        }
        player.openInventory(inv);
        viewers.put(player.getUniqueId(), new ViewSession(targetOffline.getUniqueId(), type));
        return true;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        UUID viewerId = event.getPlayer().getUniqueId();
        ViewSession session = viewers.remove(viewerId);
        if (session == null) return;

        UUID targetId = session.target;
        ViewType type = session.type;
        Player targetOnline = Bukkit.getPlayer(targetId);
        Inventory inv = event.getInventory();

        if (targetOnline != null) {
            if (type == ViewType.INVENTORY) {
                ItemStack[] contents = Arrays.copyOf(inv.getContents(), targetOnline.getInventory().getContents().length);
                targetOnline.getInventory().setContents(contents);
            } else if (type == ViewType.ECHEST) {
                ItemStack[] contents = Arrays.copyOf(inv.getContents(), targetOnline.getEnderChest().getContents().length);
                targetOnline.getEnderChest().setContents(contents);
            } else {
                ItemStack helmet = inv.getItem(0);
                ItemStack chest = inv.getItem(1);
                ItemStack legs = inv.getItem(2);
                ItemStack boots = inv.getItem(3);
                ItemStack[] armor = new ItemStack[]{boots, legs, chest, helmet};
                targetOnline.getInventory().setArmorContents(armor);
                targetOnline.getInventory().setItemInOffHand(inv.getItem(8));
            }
            targetOnline.updateInventory();
        } else {
            OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(targetId);
            if (type == ViewType.INVENTORY) {
                saveOfflineInventory(targetOffline, inv);
            } else if (type == ViewType.ECHEST) {
                saveOfflineEnderChest(targetOffline, inv);
            } else {
                saveOfflineArmor(targetOffline, inv);
            }
        }
    }

    private Inventory loadOfflineInventory(OfflinePlayer off) {
        Inventory inv = Bukkit.createInventory(null, 54, off.getName() + "'s Inventory");
        try {
            File dataFile = new File(Bukkit.getWorlds().get(0).getWorldFolder(), "playerdata/" + off.getUniqueId() + ".dat");
            if (!dataFile.exists()) {
                return inv;
            }
            NBTFile nbt = new NBTFile(dataFile);
            NBTCompoundList list = nbt.getCompoundList("Inventory");
            for (int i = 0; i < list.size(); i++) {
                NBTCompound tag = list.get(i);
                int slot = tag.getByte("Slot");
                tag.removeKey("Slot");
                ItemStack item = NBTItem.convertNBTtoItem(tag);
                if (slot >= 0 && slot < inv.getSize()) {
                    inv.setItem(slot, item);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load inventory for " + off.getName());
        }
        return inv;
    }

    private void saveOfflineInventory(OfflinePlayer off, Inventory inv) {
        try {
            File dataFile = new File(Bukkit.getWorlds().get(0).getWorldFolder(), "playerdata/" + off.getUniqueId() + ".dat");
            if (!dataFile.exists()) return;
            NBTFile nbt = new NBTFile(dataFile);
            NBTCompoundList list = nbt.getCompoundList("Inventory");
            list.clear();
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && item.getType().isItem()) {
                    NBTListCompound tag = list.addCompound();
                    tag.mergeCompound(NBTItem.convertItemtoNBT(item));
                    tag.setByte("Slot", (byte) i);
                }
            }
            nbt.save();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save inventory for " + off.getName());
        }
    }

    private Inventory loadOfflineEnderChest(OfflinePlayer off) {
        Inventory inv = Bukkit.createInventory(null, 27, off.getName() + "'s Ender Chest");
        try {
            File dataFile = new File(Bukkit.getWorlds().get(0).getWorldFolder(), "playerdata/" + off.getUniqueId() + ".dat");
            if (!dataFile.exists()) {
                return inv;
            }
            NBTFile nbt = new NBTFile(dataFile);
            NBTCompoundList list = nbt.getCompoundList("EnderItems");
            for (int i = 0; i < list.size(); i++) {
                NBTCompound tag = list.get(i);
                int slot = tag.getByte("Slot");
                tag.removeKey("Slot");
                ItemStack item = NBTItem.convertNBTtoItem(tag);
                if (slot >= 0 && slot < inv.getSize()) {
                    inv.setItem(slot, item);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load ender chest for " + off.getName());
        }
        return inv;
    }

    private void saveOfflineEnderChest(OfflinePlayer off, Inventory inv) {
        try {
            File dataFile = new File(Bukkit.getWorlds().get(0).getWorldFolder(), "playerdata/" + off.getUniqueId() + ".dat");
            if (!dataFile.exists()) return;
            NBTFile nbt = new NBTFile(dataFile);
            NBTCompoundList list = nbt.getCompoundList("EnderItems");
            list.clear();
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && item.getType().isItem()) {
                    NBTListCompound tag = list.addCompound();
                    tag.mergeCompound(NBTItem.convertItemtoNBT(item));
                    tag.setByte("Slot", (byte) i);
                }
            }
            nbt.save();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save ender chest for " + off.getName());
        }
    }

    private Inventory loadOfflineArmor(OfflinePlayer off) {
        Inventory inv = Bukkit.createInventory(null, 9, off.getName() + "'s Armor");
        for (int i = 4; i <= 7; i++) {
            inv.setItem(i, filler);
        }
        try {
            File dataFile = new File(Bukkit.getWorlds().get(0).getWorldFolder(), "playerdata/" + off.getUniqueId() + ".dat");
            if (!dataFile.exists()) {
                return inv;
            }
            NBTFile nbt = new NBTFile(dataFile);
            NBTCompoundList list = nbt.getCompoundList("Inventory");
            ItemStack offhand = null;
            ItemStack[] armor = new ItemStack[4];
            for (int i = 0; i < list.size(); i++) {
                NBTCompound tag = list.get(i);
                int slot = tag.getByte("Slot");
                tag.removeKey("Slot");
                ItemStack item = NBTItem.convertNBTtoItem(tag);
                if (slot == 40) {
                    offhand = item;
                } else if (slot >= 100 && slot <= 103) {
                    switch (slot) {
                        case 103 -> armor[0] = item; // helmet
                        case 102 -> armor[1] = item; // chest
                        case 101 -> armor[2] = item; // legs
                        case 100 -> armor[3] = item; // boots
                    }
                }
            }
            for (int i = 0; i < 4; i++) {
                inv.setItem(i, armor[i]);
            }
            inv.setItem(8, offhand);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load armor for " + off.getName());
        }
        return inv;
    }

    private void saveOfflineArmor(OfflinePlayer off, Inventory inv) {
        try {
            File dataFile = new File(Bukkit.getWorlds().get(0).getWorldFolder(), "playerdata/" + off.getUniqueId() + ".dat");
            if (!dataFile.exists()) return;
            NBTFile nbt = new NBTFile(dataFile);
            NBTCompoundList list = nbt.getCompoundList("Inventory");
            // remove existing armor/offhand entries
            for (int i = list.size() - 1; i >= 0; i--) {
                NBTCompound tag = list.get(i);
                int slot = tag.getByte("Slot");
                if (slot == 40 || (slot >= 100 && slot <= 103)) {
                    list.remove(i);
                }
            }
            ItemStack[] armor = new ItemStack[4];
            armor[0] = inv.getItem(0); // helmet
            armor[1] = inv.getItem(1); // chest
            armor[2] = inv.getItem(2); // legs
            armor[3] = inv.getItem(3); // boots
            ItemStack offhand = inv.getItem(8);
            int[] slots = {103, 102, 101, 100};
            for (int i = 0; i < armor.length; i++) {
                ItemStack item = armor[i];
                if (item != null && item.getType().isItem()) {
                    NBTListCompound tag = list.addCompound();
                    tag.mergeCompound(NBTItem.convertItemtoNBT(item));
                    tag.setByte("Slot", (byte) slots[i]);
                }
            }
            if (offhand != null && offhand.getType().isItem()) {
                NBTListCompound tag = list.addCompound();
                tag.mergeCompound(NBTItem.convertItemtoNBT(offhand));
                tag.setByte("Slot", (byte) 40);
            }
            nbt.save();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save armor for " + off.getName());
        }
    }

    private ItemStack createFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("\u00A70 ");
            item.setItemMeta(meta);
        }
        return item;
    }
}
