package at.sleazlee.bmessentials.SpawnSystems;

import at.sleazlee.bmessentials.BMEssentials;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FirstJoinCommand implements CommandExecutor {

    private final Map<UUID, BukkitTask> tasks = new HashMap<>();
    private final BMEssentials plugin;

    public FirstJoinCommand(BMEssentials plugin) {
        this.plugin = plugin;
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

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(this.plugin, new Runnable() {
            private int count = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    tasks.remove(playerId).cancel();
                } else if (count <= 1) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.AQUA + "" + ChatColor.BOLD + "Welcome to Blockminer!"));
                    count++;
                } else if (count <= 2) {
                    count++;
                } else if (count <= 3) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.AQUA + "To Start Your Journey"));
                    count++;
                } else if (count <= 7) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.AQUA + "" + ChatColor.BOLD + "JUMP IN THE PIT!"));
                    count++;
                } else {
                    count = 2;
                }

            }
        }, 0L, 40L);  // 80 ticks = 4 seconds, as 20 ticks = 1 second in Minecraft time.

        tasks.put(playerId, task);

        return true;
    }
}
