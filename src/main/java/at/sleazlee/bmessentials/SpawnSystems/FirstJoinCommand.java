package at.sleazlee.bmessentials.SpawnSystems;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

import java.util.HashMap;
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();

        if (tasks.containsKey(playerId)) {
            sender.sendMessage("The /firstjoinmessage command has already been activated.");
            return true;
        }

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
