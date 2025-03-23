package at.sleazlee.bmessentials.AFKSystem;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command executor for the /afk command.
 */
public class AfkCommand implements CommandExecutor {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    /**
     * Toggles the player's AFK status manually and broadcasts the corresponding message.
     *
     * @param sender  the command sender.
     * @param command the command.
     * @param label   the alias used.
     * @param args    command arguments.
     * @return true if the command was processed.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        boolean newState = AfkManager.getInstance().toggleAfk(player);
        boolean canSendMessage = AfkManager.getInstance().canSendAfkMessage(player);

        if (canSendMessage) {
            String message = newState
                    ? "<italic><gray>" + player.getName() + " is now AFK</gray></italic>"
                    : "<italic><gray>" + player.getName() + " is no longer AFK</gray></italic>";

            Bukkit.broadcast(MiniMessage.miniMessage().deserialize(message));
            AfkManager.getInstance().updateLastAfkMessageTime(player);
            AfkManager.getInstance().setBroadcastedAfk(player, true);
        } else {
            AfkManager.getInstance().setBroadcastedAfk(player, false);
        }

        return true;
    }


}
