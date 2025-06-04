package at.sleazlee.bmessentials.playerutils;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTFile;
import de.tr7zw.changeme.nbtapi.NBTItem;
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
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

/**
 * Command to view and edit another player's inventory.
 */
public class InvseeCommand implements CommandExecutor, Listener {

    private final JavaPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<UUID, UUID> viewers = new HashMap<>();

    public InvseeCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>This command can only be used by players.</red>"));
            return true;
        }
        if (!player.hasPermission("bm.invsee.use")) {
            player.sendMessage(mm.deserialize("<red>You do not have permission to use this command.</red>"));
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(mm.deserialize("<gray>Usage: /invsee <player></gray>"));
            return true;
        }

        String targetName = args[0];
        Player targetOnline = Bukkit.getPlayerExact(targetName);
        if (targetOnline != null) {
            Inventory inv = Bukkit.createInventory(null, 54, targetOnline.getName() + "'s Inventory");
            inv.setContents(targetOnline.getInventory().getContents());
            player.openInventory(inv);
            viewers.put(player.getUniqueId(), targetOnline.getUniqueId());
            return true;
        }

        OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(targetName);
        if (!targetOffline.hasPlayedBefore()) {
            player.sendMessage(mm.deserialize("<red>Player not found.</red>"));
            return true;
        }

        Inventory inv = loadOfflineInventory(targetOffline);
        player.openInventory(inv);
        viewers.put(player.getUniqueId(), targetOffline.getUniqueId());
        return true;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        UUID viewerId = event.getPlayer().getUniqueId();
        UUID targetId = viewers.remove(viewerId);
        if (targetId == null) return;

        Player targetOnline = Bukkit.getPlayer(targetId);
        Inventory inv = event.getInventory();

        if (targetOnline != null) {
            ItemStack[] contents = Arrays.copyOf(inv.getContents(), targetOnline.getInventory().getContents().length);
            targetOnline.getInventory().setContents(contents);
            targetOnline.updateInventory();
        } else {
            OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(targetId);
            saveOfflineInventory(targetOffline, inv);
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
            List<NBTCompound> list = nbt.getCompoundList("Inventory");
            for (NBTCompound tag : list) {
                int slot = tag.getByte("Slot");
                ItemStack item = NBTItem.convertNBTCompoundToItemStack(tag);
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
            List<NBTCompound> list = new ArrayList<>();
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && item.getType().isItem()) {
                    NBTCompound tag = NBTItem.convertItemtoNBT(item).getCompound();
                    tag.setByte("Slot", (byte) i);
                    list.add(tag);
                }
            }
            nbt.setCompoundList("Inventory", list);
            nbt.save();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save inventory for " + off.getName());
        }
    }
}
