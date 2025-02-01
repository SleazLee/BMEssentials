package at.sleazlee.bmessentials.SpawnSystems;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

// WorldGuard imports
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FirstJoinCommand implements CommandExecutor {

    private final Map<UUID, Scheduler.Task> tasks = new HashMap<>();
    private final BMEssentials plugin;

    public FirstJoinCommand(BMEssentials plugin) {
        this.plugin = plugin;
    }

    // Method to check if player is in a specific WorldGuard region
    private boolean playerIsInRegion(Player player, String regionName) {
        // Adapt the player's Bukkit World to a WorldGuard World
        World wgWorld = BukkitAdapter.adapt(player.getWorld());
        if (wgWorld == null) {
            return false;
        }
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(wgWorld);
        if (regions == null) {
            return false;
        }
        // Adapt the player's location
        com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(player.getLocation());
        ApplicableRegionSet set = regions.getApplicableRegions(loc.toVector().toBlockPoint());
        for (ProtectedRegion region : set) {
            if (region.getId().equalsIgnoreCase(regionName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gives the new player their starter items.
     * Four leather armor pieces (helmet, chestplate, leggings, boots) with custom name, lore, color, and enchantments,
     * plus six hotbar items placed in slots 0-5.
     */
    private void giveStarterItems(Player player) {
        // Define the common lore for the armor pieces
        List<String> armorLore = new ArrayList<>();
        armorLore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
        armorLore.add(ChatColor.translateAlternateColorCodes('&', "&3Your first armor set!"));

        // --- Create Leather Armor Items ---

        // Leather Helmet
        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta helmetMeta = (LeatherArmorMeta) helmet.getItemMeta();
        helmetMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&lBlockminer Starter Cap"));
        helmetMeta.setLore(armorLore);
        helmetMeta.setColor(Color.fromRGB(107, 147, 177));
        helmetMeta.addEnchant(Enchantment.PROTECTION, 1, true);
        helmetMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
        helmet.setItemMeta(helmetMeta);

        // Leather Chestplate
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta chestMeta = (LeatherArmorMeta) chestplate.getItemMeta();
        chestMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&lBlockminer Starter Shirt"));
        chestMeta.setLore(armorLore);
        chestMeta.setColor(Color.fromRGB(107, 147, 177));
        chestMeta.addEnchant(Enchantment.PROTECTION, 1, true);
        chestMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
        chestplate.setItemMeta(chestMeta);

        // Leather Leggings
        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta leggingsMeta = (LeatherArmorMeta) leggings.getItemMeta();
        leggingsMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&lBlockminer Starter Pants"));
        leggingsMeta.setLore(armorLore);
        leggingsMeta.setColor(Color.fromRGB(107, 147, 177));
        leggingsMeta.addEnchant(Enchantment.PROTECTION, 1, true);
        leggingsMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
        leggings.setItemMeta(leggingsMeta);

        // Leather Boots
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta bootsMeta = (LeatherArmorMeta) boots.getItemMeta();
        bootsMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&lBlockminer Starter Shoes"));
        bootsMeta.setLore(armorLore);
        bootsMeta.setColor(Color.fromRGB(107, 147, 177));
        bootsMeta.addEnchant(Enchantment.PROTECTION, 1, true);
        bootsMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
        boots.setItemMeta(bootsMeta);

        // Equip the armor
        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);

        // --- Create HotBar Items ---

        // Stone Sword
        ItemStack stoneSword = new ItemStack(Material.STONE_SWORD);
        // Stone Pickaxe
        ItemStack stonePickaxe = new ItemStack(Material.STONE_PICKAXE);
        // Stone Axe
        ItemStack stoneAxe = new ItemStack(Material.STONE_AXE);
        // Stone Shovel
        ItemStack stoneShovel = new ItemStack(Material.STONE_SHOVEL);
        // Stone Hoe
        ItemStack stoneHoe = new ItemStack(Material.STONE_HOE);
        // Baked Potato with a custom name "Hot Potato"
        ItemStack bakedPotato = new ItemStack(Material.BAKED_POTATO);
        ItemMeta potatoMeta = bakedPotato.getItemMeta();
        potatoMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "Hot Potato"));
        bakedPotato.setItemMeta(potatoMeta);

        // Place the hotbar items in slots 0-5
        player.getInventory().setItem(0, stoneSword);
        player.getInventory().setItem(1, stonePickaxe);
        player.getInventory().setItem(2, stoneAxe);
        player.getInventory().setItem(3, stoneShovel);
        player.getInventory().setItem(4, stoneHoe);
        player.getInventory().setItem(5, bakedPotato);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();

        // Give the starter items to the new player
        giveStarterItems(player);

        if (tasks.containsKey(playerId)) {
            sender.sendMessage("The /firstjoinmessage command has already been activated.");
            return true;
        }

        // Scheduler to send welcome messages on a timer
        Scheduler.Task task = Scheduler.runTimer(new Runnable() {
            private int count = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    tasks.remove(playerId).cancel();
                } else if (!playerIsInRegion(player, "spawn")) {
                    tasks.remove(playerId).cancel();
                } else if (count <= 1) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent(ChatColor.AQUA + "" + ChatColor.BOLD + "Welcome to Blockminer!"));
                    count++;
                } else if (count <= 2) {
                    count++;
                } else if (count <= 3) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent(ChatColor.AQUA + "To Start Your Journey"));
                    count++;
                } else if (count <= 7) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent(ChatColor.AQUA + "" + ChatColor.BOLD + "JUMP IN THE PIT!"));
                    count++;
                } else {
                    count = 2;
                }
            }
        }, 0L, 40L);  // 40 ticks = 2 seconds

        tasks.put(playerId, task);

        return true;
    }
}