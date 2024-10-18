package at.sleazlee.bmessentials.Migrator.Commands;

import at.sleazlee.bmessentials.Migrator.DatabaseManager;
import at.sleazlee.bmessentials.Migrator.MigratorManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Handles the /resetmigration admin command.
 */
public class ResetMigrationCommand implements CommandExecutor {

    private MigratorManager migratorManager;

    /**
     * Constructs the command executor.
     *
     * @param migratorManager the migrator manager instance
     */
    public ResetMigrationCommand(MigratorManager migratorManager) {
        this.migratorManager = migratorManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("inventorymigrator.admin")) {
            sender.sendMessage("You do not have permission to execute this command.");
            return true;
        }

        DatabaseManager dbManager = migratorManager.getDatabaseManager();

        try (Connection conn = dbManager.getConnection()) {
            // Delete all data from the inventories, stats, and advancements tables
            String deleteInventoriesSQL = "DELETE FROM inventories;";
            String deleteStatsSQL = "DELETE FROM stats;";
            String deleteAdvancementsSQL = "DELETE FROM advancements;";

            try (PreparedStatement ps1 = conn.prepareStatement(deleteInventoriesSQL);
                 PreparedStatement ps2 = conn.prepareStatement(deleteStatsSQL);
                 PreparedStatement ps3 = conn.prepareStatement(deleteAdvancementsSQL)) {

                ps1.executeUpdate();
                ps2.executeUpdate();
                ps3.executeUpdate();
            }

            sender.sendMessage("Migration data has been reset successfully.");

        } catch (Exception e) {
            e.printStackTrace();
            sender.sendMessage("An error occurred while resetting migration data.");
        }

        return true;
    }
}