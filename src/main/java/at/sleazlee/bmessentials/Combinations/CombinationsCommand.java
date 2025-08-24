package at.sleazlee.bmessentials.Combinations;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Handles the /combinations command for creating and deleting combinations.
 */
public class CombinationsCommand implements CommandExecutor, TabCompleter, Listener {
    private final Combinations combinations;
    private final Map<UUID, CreationInfo> creating = new HashMap<>();

    public CombinationsCommand(Combinations combinations) {
        this.combinations = combinations;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /combinations <anvil|crafting> <create|delete> <name>");
            return true;
        }
        String type = args[0].toLowerCase();
        String action = args[1].toLowerCase();
        if (!(type.equals("anvil") || type.equals("crafting"))) {
            player.sendMessage(ChatColor.RED + "Unknown combination type.");
            return true;
        }
        if (action.equals("create")) {
            if (args.length < 3) {
                player.sendMessage(ChatColor.RED + "Usage: /combinations " + type + " create <name>");
                return true;
            }
            String name = args[2];
            openCreationGUI(player, type, name);
            return true;
        }
        if (action.equals("delete")) {
            if (args.length < 3) {
                player.sendMessage(ChatColor.RED + "Usage: /combinations " + type + " delete <name>");
                return true;
            }
            String name = args[2];
            boolean removed;
            switch (type) {
                case "anvil" -> removed = combinations.deleteAnvil(name);
                case "crafting" -> removed = combinations.deleteCrafting(name);
                default -> removed = false;
            }
            if (removed) {
                player.sendMessage(ChatColor.GREEN + "Deleted combination '" + name + "'.");
            } else {
                player.sendMessage(ChatColor.RED + "Combination not found.");
            }
            return true;
        }
        player.sendMessage(ChatColor.RED + "Unknown action.");
        return true;
    }

    private ItemStack createFiller() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.text(" ").color(net.kyori.adventure.text.format.NamedTextColor.BLACK));
        filler.setItemMeta(meta);
        return filler;
    }

    private void openCreationGUI(Player player, String type, String name) {
        ItemStack filler = createFiller();
        Inventory inv;
        if (type.equals("anvil")) {
            inv = Bukkit.createInventory(player, 9, Component.text("Anvil Combo: " + name));
            int[] slots = {0,2,3,5,6,8};
            for (int slot : slots) {
                inv.setItem(slot, filler);
            }
        } else if (type.equals("crafting")) {
            inv = Bukkit.createInventory(player, 27, Component.text("Crafting Combo: " + name));
            Set<Integer> allowed = new HashSet<>(Arrays.asList(0,1,2,9,10,11,18,19,20,24));
            for (int i = 0; i < inv.getSize(); i++) {
                if (!allowed.contains(i)) {
                    inv.setItem(i, filler);
                }
            }
        } else {
            // should not reach here as type validated earlier
            return;
        }
        creating.put(player.getUniqueId(), new CreationInfo(type, name));
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        CreationInfo info = creating.get(player.getUniqueId());
        if (info == null) {
            return;
        }
        Component expected = switch (info.type) {
            case "anvil" -> Component.text("Anvil Combo: " + info.name);
            case "crafting" -> Component.text("Crafting Combo: " + info.name);
            default -> null;
        };
        if (expected == null || !event.getView().title().equals(expected)) {
            return;
        }
        int slot = event.getRawSlot();
        if (info.type.equals("anvil")) {
            if (slot == 0 || slot == 2 || slot == 3 || slot == 5 || slot == 6 || slot == 8) {
                event.setCancelled(true);
            }
        } else if (info.type.equals("crafting")) {
            Set<Integer> allowed = new HashSet<>(Arrays.asList(0,1,2,9,10,11,18,19,20,24));
            if (slot < event.getInventory().getSize() && !allowed.contains(slot)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        CreationInfo info = creating.remove(player.getUniqueId());
        if (info == null) {
            return;
        }
        Component expected = switch (info.type) {
            case "anvil" -> Component.text("Anvil Combo: " + info.name);
            case "crafting" -> Component.text("Crafting Combo: " + info.name);
            default -> null;
        };
        if (expected == null || !event.getView().title().equals(expected)) {
            return;
        }
        Inventory inv = event.getInventory();
        if (info.type.equals("anvil")) {
            ItemStack first = inv.getItem(1);
            ItemStack second = inv.getItem(4);
            ItemStack result = inv.getItem(7);
            for (int slot : new int[]{1,4,7}) {
                ItemStack item = inv.getItem(slot);
                if (item != null) {
                    inv.setItem(slot, null);
                    player.getInventory().addItem(item);
                }
            }
            if (first != null && second != null && result != null) {
                combinations.createAnvil(info.name, first.clone(), second.clone(), result.clone());
                player.sendMessage(ChatColor.GREEN + "Created combination '" + info.name + "'.");
            } else {
                player.sendMessage(ChatColor.RED + "Combination not saved. You must fill all three slots.");
            }
        } else if (info.type.equals("crafting")) {
            int[] grid = {0,1,2,9,10,11,18,19,20};
            ItemStack[] matrix = new ItemStack[9];
            for (int i = 0; i < grid.length; i++) {
                matrix[i] = inv.getItem(grid[i]);
            }
            ItemStack result = inv.getItem(24);
            for (int slot : new int[]{0,1,2,9,10,11,18,19,20,24}) {
                ItemStack item = inv.getItem(slot);
                if (item != null) {
                    inv.setItem(slot, null);
                    player.getInventory().addItem(item);
                }
            }
            boolean any = false;
            for (ItemStack m : matrix) {
                if (m != null) {
                    any = true;
                    break;
                }
            }
            if (result != null && any) {
                combinations.createCrafting(info.name, Arrays.stream(matrix).map(m -> m == null ? null : m.clone()).toArray(ItemStack[]::new), result.clone());
                player.sendMessage(ChatColor.GREEN + "Created combination '" + info.name + "'.");
            } else {
                player.sendMessage(ChatColor.RED + "Combination not saved. Fill at least one ingredient and the result slot.");
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("anvil", "crafting");
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("anvil") || args[0].equalsIgnoreCase("crafting")) {
                return Arrays.asList("create", "delete");
            }
            return Collections.emptyList();
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("delete")) {
            if (args[0].equalsIgnoreCase("anvil")) {
                return new ArrayList<>(combinations.getAnvilNames());
            }
            if (args[0].equalsIgnoreCase("crafting")) {
                return new ArrayList<>(combinations.getCraftingNames());
            }
        }
        return Collections.emptyList();
    }

    private record CreationInfo(String type, String name) {}
}
