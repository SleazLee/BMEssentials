package at.sleazlee.bmessentials.Punish;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

/**
 * Sends a formatted warning message to a player using MiniMessage formatting.
 */
public class WarnBanCommand implements CommandExecutor {

    private static final String PERMISSION = "bm.staff.warnban";
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!label.equalsIgnoreCase("warnban")) {
            return false;
        }

        if (args.length < 2) {
            sender.sendMessage("§c§lBM §fUsage: /warnban <player> <message>");
            return true;
        }

        if (sender instanceof Player player && !player.hasPermission(PERMISSION)) {
            sender.sendMessage("§c§lBM §cAccess Denied.");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("§c§lBM §fPlayer §e" + args[0] + " §fis not online.");
            return true;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        Component component;
        try {
            component = miniMessage.deserialize(message);
        } catch (IllegalArgumentException ex) {
            sender.sendMessage("§c§lBM §fInvalid MiniMessage format. Please check your tags.");
            return true;
        }

        target.sendMessage(component);

        if (!sender.equals(target)) {
            Component confirmation = miniMessage.deserialize("<green><bold>WarnBan </bold></green><gray>Sent message to <yellow>"
                    + target.getName() + "</yellow>.</gray>");
            sender.sendMessage(confirmation);
        }

        return true;
    }
}
