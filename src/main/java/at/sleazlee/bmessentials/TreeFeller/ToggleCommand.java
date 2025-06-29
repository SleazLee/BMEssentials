package at.sleazlee.bmessentials.TreeFeller;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command used to toggle automatic tree felling for an individual player.
 */
public class ToggleCommand implements CommandExecutor {
    private final TreeFellerManager manager;

    public ToggleCommand(TreeFellerManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can toggle TreeFeller.");
            return true;
        }

        manager.toggle(player);
        boolean enabled = manager.isEnabled(player);
        player.sendMessage("TreeFeller is now " + (enabled ? "enabled" : "disabled") + ".");
        return true;
    }
}
