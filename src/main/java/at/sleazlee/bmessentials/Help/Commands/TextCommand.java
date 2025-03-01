package at.sleazlee.bmessentials.Help.Commands;

import at.sleazlee.bmessentials.Help.HelpText;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**`
 * CommandExecutor for handling the /commands command.
 */
public class TextCommand implements CommandExecutor {

    private final HelpText commandsSystem;

    /**
     * Constructs a new CommandsCommand instance.
     *
     * @param commandsSystem The CommandsSystem instance to use.
     */
    public TextCommand(HelpText commandsSystem) {
        this.commandsSystem = commandsSystem;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can run this command.");
            return true;
        }

        // Check for the "reload" subcommand.
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            // Optional: Check for a permission before allowing a reload.
            if (!sender.hasPermission("bmessentials.commands.reload")) {
                sender.sendMessage(Component.text("You do not have permission to reload the command configuration."));
                return true;
            }

            commandsSystem.reloadCommandConfig();
            sender.sendMessage(Component.text("Command configuration reloaded successfully."));
            return true;
        }

        // Shows commands without prerequisites
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("basic") ||
                    args[0].equalsIgnoreCase("trophies") ||
                    args[0].equalsIgnoreCase("teleportation") ||
                    args[0].equalsIgnoreCase("communication") ||
                    args[0].equalsIgnoreCase("fun") ||
                    args[0].equalsIgnoreCase("mcmmo") ||
                    args[0].equalsIgnoreCase("settings") ||
                    args[0].equalsIgnoreCase("abilities") ||
                    args[0].equalsIgnoreCase("unlocks") ||
                    args[0].equalsIgnoreCase("vip") ||
                    args[0].equalsIgnoreCase("arm") ||
                    args[0].equalsIgnoreCase("quickshop") ||
                    args[0].equalsIgnoreCase("lands1") ||
                    args[0].equalsIgnoreCase("lands2")) {

                String commandName = args[0];
                commandsSystem.sendCommandInfo(player, commandName);

                return true;

                // Checks if the player is an OP.
            } else if (sender.isOp()) {

                String commandName = args[0];
                sender.sendMessage(Component.text("Opening Command as OP."));
                commandsSystem.sendCommandInfo(player, commandName);

                return true;

            }

        }

        if (args.length < 1) {
            player.sendMessage(Component.text("Please specify a command name."));
            return true;
        }

        return true;
    }
}
