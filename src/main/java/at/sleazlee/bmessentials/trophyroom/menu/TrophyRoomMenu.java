package at.sleazlee.bmessentials.trophyroom.menu;

import at.sleazlee.bmessentials.trophyroom.data.Data;
import at.sleazlee.bmessentials.trophyroom.data.MessageProvider;
import at.sleazlee.bmessentials.trophyroom.data.Trophy;
import at.sleazlee.bmessentials.trophyroom.db.Database;
import at.sleazlee.bmessentials.trophyroom.smartinventory.Icon;
import at.sleazlee.bmessentials.trophyroom.smartinventory.Page;
import at.sleazlee.bmessentials.trophyroom.smartinventory.SmartInventory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;
import java.util.function.Predicate;

public class TrophyRoomMenu {
    private SmartInventory inventory;

    public TrophyRoomMenu(SmartInventory inventory) {
        this.inventory = inventory;
    }

    public void open(Player opener, String ownerUUID) {
        Data data = Data.getData();
        Page.build(this.inventory, new MenuProvider(ownerUUID)).row(5).title(data.getSetting("menuTitle").replaceAll("&", "§").replaceAll("\\{player}", Database.getDatabase().getName(ownerUUID))).whenClose((closeEvent) -> {
        }).whenEmptyClick((pageClickEvent) -> {
            if (!this.uuidEquals(opener, ownerUUID)) {
                pageClickEvent.cancel();
            } else {
                ItemStack stack = pageClickEvent.getEvent().getCurrentItem();
                if (pageClickEvent.getEvent().isShiftClick()) {
                    pageClickEvent.getEvent().setCancelled(true);
                    pageClickEvent.cancel();
                }

                Trophy trophy;
                if (stack != null && stack.hasItemMeta() && (trophy = data.getTrophy(stack)) != null) {
                    if (pageClickEvent.contents().findItem(trophy.getItem()).isPresent()) {
                        opener.sendMessage(((MessageProvider)Objects.requireNonNull(MessageProvider.build("ALREADY_OWN_TROPHY"))).replace("{trophy}", ((ItemMeta)Objects.requireNonNull(trophy.getItem().getItemMeta())).getDisplayName()).getMessage());
                        pageClickEvent.getEvent().setCancelled(true);
                    }
                } else {
                    stack = pageClickEvent.getEvent().getCursor();
                    if (stack != null && stack.hasItemMeta() && (trophy = data.getTrophy(stack)) != null) {
                        if (pageClickEvent.getEvent().getClickedInventory() != null && pageClickEvent.getEvent().getClickedInventory().equals(pageClickEvent.getEvent().getView().getTopInventory())) {
                            if (pageClickEvent.contents().findItem(stack).isPresent()) {
                                opener.sendMessage(((MessageProvider)Objects.requireNonNull(MessageProvider.build("ALREADY_OWN_TROPHY"))).replace("{trophy}", ((ItemMeta)Objects.requireNonNull(trophy.getItem().getItemMeta())).getDisplayName()).getMessage());
                                pageClickEvent.getEvent().setCancelled(true);
                                pageClickEvent.cancel();
                                return;
                            }

                            if (pageClickEvent.getEvent().getSlot() >= 9 && pageClickEvent.getEvent().getSlot() <= 35) {
                                Trophy finalTrophy = trophy;
                                pageClickEvent.contents().set(pageClickEvent.getEvent().getSlot(), Icon.click(trophy.getItem(), (clickEvent) -> {
                                    if (opener.getInventory().firstEmpty() == -1) {
                                        opener.sendMessage(((MessageProvider)Objects.requireNonNull(MessageProvider.build("NO_INVENTORY_ROOM"))).getMessage());
                                        clickEvent.cancel();
                                    } else {
                                        Data.getData().removePlayerTrophy(ownerUUID, finalTrophy);
                                        opener.getInventory().setItem(opener.getInventory().firstEmpty(), finalTrophy.getItem());
                                        pageClickEvent.contents().removeFirst(clickEvent.icon());
                                        pageClickEvent.contents().notifyUpdate();
                                    }
                                }, new Predicate[0]));
                                data.addPlayerTrophy(ownerUUID, trophy, pageClickEvent.getEvent().getSlot());
                                pageClickEvent.contents().notifyUpdate();
                                pageClickEvent.getEvent().setCancelled(true);
                                pageClickEvent.cancel();
                                opener.getItemOnCursor().setAmount(opener.getItemOnCursor().getAmount() - 1);
                            } else {
                                pageClickEvent.getEvent().setCancelled(true);
                            }
                        }
                    } else {
                        pageClickEvent.cancel();
                        pageClickEvent.getEvent().setCancelled(true);
                    }
                }

            }
        }).open(opener);
    }

    private boolean uuidEquals(Player p, String uuid) {
        return p.getUniqueId().toString().replaceAll("-", "").equals(uuid);
    }
}
