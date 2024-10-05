package at.sleazlee.bmessentials.Migrator.Commands;

import at.sleazlee.bmessentials.Migrator.MigratorManager;
import at.sleazlee.bmessentials.Migrator.VirtualChestGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the /retrieveinventory player command.
 */
public class RetrieveInventoryCommand implements CommandExecutor {

    private MigratorManager migratorManager;

    /**
     * Constructs the command executor.
     *
     * @param migratorManager the migrator manager instance
     */
    public RetrieveInventoryCommand(MigratorManager migratorManager) {
        this.migratorManager = migratorManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can execute this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("inventorymigrator.retrieve")) {
            player.sendMessage("You do not have permission to execute this command.");
            return true;
        }

        // Open the virtual chest GUI
        VirtualChestGUI gui = new VirtualChestGUI(migratorManager, player);
        gui.openInventory();

        return true;
    }
}
