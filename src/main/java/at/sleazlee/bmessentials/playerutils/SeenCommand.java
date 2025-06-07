package at.sleazlee.bmessentials.playerutils;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Command to check when a player was last online.
 */
public class SeenCommand implements CommandExecutor {

    private static final MiniMessage mm = MiniMessage.miniMessage();
    private static final Set<String> HIDDEN = new HashSet<>(Arrays.asList("SleazLee", "Seyten"));

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(mm.deserialize("<gray>Usage: /seen <player></gray>"));
            return true;
        }

        String name = args[0];
        if (HIDDEN.contains(name)) {
            sender.sendMessage(mm.deserialize("<color:#ff3300><bold>Seen</bold> <red>This player is untraceable.</red>"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        if (target.isOnline()) {
            sender.sendMessage(mm.deserialize("<gold><bold>Seen</bold> <gray>" + name + " is currently online.</gray>"));
            return true;
        }

        if (!target.hasPlayedBefore()) {
            sender.sendMessage(mm.deserialize("<red>Player not found.</red>"));
            return true;
        }

        long last = target.getLastPlayed();
        String ago = formatAgo(System.currentTimeMillis() - last);
        sender.sendMessage(mm.deserialize("<gold><bold>Seen</bold> <gray>" + name + " was last online " + ago + " ago.</gray>"));
        return true;
    }

    private String formatAgo(long diff) {
        long days = diff / 86400000L;
        long hours = (diff / 3600000L) % 24;
        long mins = (diff / 60000L) % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (mins > 0) sb.append(mins).append("m");
        if (sb.length() == 0) sb.append("just now");
        return sb.toString().trim();
    }
}
