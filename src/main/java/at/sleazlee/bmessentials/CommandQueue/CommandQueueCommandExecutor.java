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

        // If no args provided
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /commandqueue [run|reload|clean]");
            return true;
        }

        // Handle /commandqueue reload
        if (args[0].equalsIgnoreCase("reload")) {
            manager.loadCommands();
            int commandCount = manager.getCommandCount();
            sender.sendMessage(ChatColor.GREEN + "CommandQueue reloaded! Found " + commandCount + " commands.");
            return true;
        }

        // Handle /commandqueue clean
        if (args[0].equalsIgnoreCase("clean")) {
            manager.resetToDefault();
            manager.loadCommands();
            int commandCount = manager.getCommandCount();
            sender.sendMessage(ChatColor.GREEN + "CommandQueue.yml has been reset to default. Found " + commandCount + " commands.");
            return true;
        }

        // Handle /commandqueue run <player/console> <delayInSeconds>
        if (args[0].equalsIgnoreCase("run")) {
            if (args.length != 3 ||
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

            manager.runCommands(executorType, sender, delayInSeconds);
            return true;
        }

        // If command not recognized
        sender.sendMessage(ChatColor.RED + "Unknown subcommand. Usage: /commandqueue [run|reload|clean]");
        return true;
    }
}