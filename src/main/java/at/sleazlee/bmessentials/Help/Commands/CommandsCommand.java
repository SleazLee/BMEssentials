package at.sleazlee.bmessentials.Help.Commands;

import at.sleazlee.bmessentials.Help.HelpCommands;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**`
 * CommandExecutor for handling the /commands command.
 */
public class CommandsCommand implements CommandExecutor {

    private final HelpCommands commandsSystem;

    /**
     * Constructs a new CommandsCommand instance.
     *
     * @param commandsSystem The CommandsSystem instance to use.
     */
    public CommandsCommand(HelpCommands commandsSystem) {
        this.commandsSystem = commandsSystem;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can run this command.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Component.text("Please specify a command name."));
            return true;
        }

        String commandName = args[0];
        commandsSystem.sendCommandInfo(player, commandName);

        return true;
    }
}
