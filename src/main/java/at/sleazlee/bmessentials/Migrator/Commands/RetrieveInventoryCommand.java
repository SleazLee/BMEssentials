package at.sleazlee.bmessentials.Migrator.Commands;

import at.sleazlee.bmessentials.Migrator.MigratorManager;
import at.sleazlee.bmessentials.Migrator.VirtualChestGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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

        try {
            String playerUUID = player.getUniqueId().toString();

            // Retrieve stats and advancements from database
            try (Connection conn = migratorManager.getDatabaseManager().getConnection()) {
                String queryStats = "SELECT stats_data FROM stats WHERE player_uuid = ?";
                String queryAdvancements = "SELECT advancements_data FROM advancements WHERE player_uuid = ?";

                try (PreparedStatement psStats = conn.prepareStatement(queryStats);
                     PreparedStatement psAdvancements = conn.prepareStatement(queryAdvancements)) {

                    psStats.setString(1, playerUUID);
                    psAdvancements.setString(1, playerUUID);

                    ResultSet rsStats = psStats.executeQuery();
                    if (rsStats.next()) {
                        String statsData = rsStats.getString("stats_data");
                        migratorManager.getStatsManager().applyStats(player, statsData);
                    }

                    ResultSet rsAdvancements = psAdvancements.executeQuery();
                    if (rsAdvancements.next()) {
                        String advancementsData = rsAdvancements.getString("advancements_data");
                        migratorManager.getAdvancementsManager().applyAdvancements(player, advancementsData);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage("An error occurred while applying your stats and advancements.");
        }

        // Open the virtual chest GUI
        VirtualChestGUI gui = new VirtualChestGUI(migratorManager, player);
        gui.openInventory();

        return true;
    }
}