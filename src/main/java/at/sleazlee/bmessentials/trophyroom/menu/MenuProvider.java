package at.sleazlee.bmessentials.trophyroom.menu;

import at.sleazlee.bmessentials.trophyroom.data.Data;
import at.sleazlee.bmessentials.trophyroom.data.MessageProvider;
import at.sleazlee.bmessentials.trophyroom.data.Trophy;
import io.github.portlek.bukkititembuilder.ItemStackBuilder;
import at.sleazlee.bmessentials.trophyroom.smartinventory.Icon;
import at.sleazlee.bmessentials.trophyroom.smartinventory.InventoryContents;
import at.sleazlee.bmessentials.trophyroom.smartinventory.InventoryProvider;
import at.sleazlee.bmessentials.trophyroom.smartinventory.event.abs.SmartEvent;
import at.sleazlee.bmessentials.trophyroom.smartinventory.util.SlotPos;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;

public class MenuProvider implements InventoryProvider {
    private final ItemStack borderItem;
    private final ItemStack closeMenuItem;
    private List<SlotPos> borderSlots;
    private final SlotPos closeMenuSlot;
    private String ownerUUID;

    public MenuProvider(String ownerUUID) {
        this.borderItem = ((ItemStackBuilder)ItemStackBuilder.from(new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1)).name("&7", true)).itemStack();
        this.closeMenuItem = ((ItemStackBuilder)ItemStackBuilder.from(new ItemStack(Material.RED_STAINED_GLASS_PANE, 1)).name("&c&lClose", true)).itemStack();
        this.borderSlots = new ArrayList();
        this.closeMenuSlot = SlotPos.of(0, 8);
        this.borderSlots.add(SlotPos.of(0, 0));
        this.borderSlots.add(SlotPos.of(0, 1));
        this.borderSlots.add(SlotPos.of(0, 2));
        this.borderSlots.add(SlotPos.of(0, 3));
        this.borderSlots.add(SlotPos.of(0, 4));
        this.borderSlots.add(SlotPos.of(0, 5));
        this.borderSlots.add(SlotPos.of(0, 6));
        this.borderSlots.add(SlotPos.of(0, 7));
        this.borderSlots.add(SlotPos.of(4, 0));
        this.borderSlots.add(SlotPos.of(4, 1));
        this.borderSlots.add(SlotPos.of(4, 2));
        this.borderSlots.add(SlotPos.of(4, 3));
        this.borderSlots.add(SlotPos.of(4, 4));
        this.borderSlots.add(SlotPos.of(4, 5));
        this.borderSlots.add(SlotPos.of(4, 6));
        this.borderSlots.add(SlotPos.of(4, 7));
        this.borderSlots.add(SlotPos.of(4, 8));
        this.ownerUUID = ownerUUID;
    }

    public void init(@NotNull InventoryContents contents) {
        Iterator var2 = this.borderSlots.iterator();

        while(var2.hasNext()) {
            SlotPos pos = (SlotPos)var2.next();
            contents.set(pos, Icon.cancel(this.borderItem));
        }

        contents.set(this.closeMenuSlot, Icon.click(this.closeMenuItem, SmartEvent::close, new Predicate[0]));
        Data data = Data.getData();
        Map<String, Integer> trophies = data.getTrophies(this.ownerUUID);
        if (trophies != null) {
            Iterator var4 = trophies.keySet().iterator();

            while(var4.hasNext()) {
                String id = (String)var4.next();
                this.setTrophy(contents, data.getTrophy(id), (Integer)trophies.get(id));
            }
        }

    }

    public void update(@NotNull InventoryContents contents) {
        Data data = Data.getData();
        Map<String, Integer> trophies = data.getTrophies(this.ownerUUID);
        if (trophies != null) {
            Iterator var4 = trophies.keySet().iterator();

            while(var4.hasNext()) {
                String id = (String)var4.next();
                Trophy trophy = data.getTrophy(id);
                contents.removeFirst(trophy.getItem());
                this.setTrophy(contents, trophy, (Integer)trophies.get(id));
            }
        }

    }

    private void setTrophy(InventoryContents contents, Trophy trophy, int slot) {
        Player opener = contents.player();
        if (!opener.getUniqueId().toString().replaceAll("-", "").equals(this.ownerUUID)) {
            contents.set(slot, Icon.cancel(trophy.getItem()));
        } else {
            contents.set(slot, Icon.click(trophy.getItem(), (clickEvent) -> {
                if (opener.getInventory().firstEmpty() == -1) {
                    opener.sendMessage(((MessageProvider)Objects.requireNonNull(MessageProvider.build("NO_INVENTORY_ROOM"))).getMessage());
                    clickEvent.cancel();
                } else {
                    Data.getData().removePlayerTrophy(this.ownerUUID, trophy);
                    opener.getInventory().setItem(opener.getInventory().firstEmpty(), trophy.getItem());
                    contents.removeFirst(clickEvent.icon());
                    contents.notifyUpdate();
                }
            }, new Predicate[0]));
        }

    }
}