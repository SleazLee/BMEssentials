package at.sleazlee.bmessentials.CommandQueue;

import at.sleazlee.bmessentials.BMEssentials;
import org.bukkit.ChatColor;
import org.bukkit.command.*;

/**
 * Handles the execution of the /commandqueue command.
 */
public class CommandQueueCommandExecutor implements CommandExecutor {

    private final BMEssentials plugin;
    private final CommandQueueManager manager;

    /**
     * Constructor for CommandQueueCommandExecutor.
     *
     * @param plugin  The main plugin instance.
     * @param manager The CommandQueueManager instance.
     */
    public CommandQueueCommandExecutor(BMEssentials plugin, CommandQueueManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    /**
     * Executes the /commandqueue command.
     *
     * @param sender  The command sender.
     * @param command The command being executed.
     * @param label   The alias of the command used.
     * @param args    The arguments passed to the command.
     * @return true if the command was handled successfully.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Check if the sender is an operator
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
            return true;
        }

        // Validate command arguments
        // Now expecting: /commandqueue run <player/console> <delayInSeconds>
        if (args.length != 3 || !args[0].equalsIgnoreCase("run") ||
                (!args[1].equalsIgnoreCase("player") && !args[1].equalsIgnoreCase("console"))) {
            sender.sendMessage(ChatColor.RED + "Usage: /commandqueue run <player/console> <delayInSeconds>");
            return true;
        }

        String executorType = args[1].toLowerCase();

        int delayInSeconds;
        try {
            delayInSeconds = Integer.parseInt(args[2]);
            if (delayInSeconds < 0) {
                sender.sendMessage(ChatColor.RED + "Delay must be a non-negative integer.");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Please provide a valid integer for the delay in seconds.");
            return true;
        }

        // Execute the command queue with the specified delay
        manager.runCommands(executorType, sender, delayInSeconds);

        return true;
    }
}
