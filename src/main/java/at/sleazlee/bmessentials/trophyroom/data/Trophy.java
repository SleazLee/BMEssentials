package at.sleazlee.bmessentials.trophyroom.data;

import org.bukkit.inventory.ItemStack;

public class Trophy {
    private String id;
    private ItemStack item;

    public Trophy(String id, ItemStack item) {
        this.id = id;
        this.item = item;
    }

    public String getId() {
        return this.id;
    }

    public ItemStack getItem() {
        return this.item;
    }
}

