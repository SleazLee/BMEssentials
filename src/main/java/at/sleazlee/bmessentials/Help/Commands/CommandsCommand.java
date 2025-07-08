package at.sleazlee.bmessentials.Help.Commands;

import at.sleazlee.bmessentials.Help.HelpBooks;
import at.sleazlee.bmessentials.Help.HelpText;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * CommandExecutor for handling the /book command.
 */
public class CommandsCommand implements CommandExecutor {

    private final HelpText commands;

    /**
     * Constructs a new HelpCommand instance.
     *
     * @param commands The Books instance to use.
     */
    public CommandsCommand(HelpText commands) {
        this.commands = commands;
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
                sender.sendMessage(Component.text("You do not have permission to reload the commands configuration."));
                return true;
            }

            commands.reloadCommandConfig();
            sender.sendMessage(Component.text("Commands configuration reloaded successfully."));
            return true;
        }


        // Shows books without prerequisites
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("abilities") ||
                    args[0].equalsIgnoreCase("basics") ||
                    args[0].equalsIgnoreCase("bms") ||
                    args[0].equalsIgnoreCase("chat") ||
                    args[0].equalsIgnoreCase("communication") ||
                    args[0].equalsIgnoreCase("chestshop") ||
                    args[0].equalsIgnoreCase("fun") ||
                    args[0].equalsIgnoreCase("lands") ||
                    args[0].equalsIgnoreCase("mcmmo") ||
                    args[0].equalsIgnoreCase("settings") ||
                    args[0].equalsIgnoreCase("teleportation") ||
                    args[0].equalsIgnoreCase("trophies") ||
                    args[0].equalsIgnoreCase("unlocks") ||
                    args[0].equalsIgnoreCase("vip")) {

                if (args[0].equalsIgnoreCase("chat")) {
                    commands.sendCommandInfo(player, "communication");
                    return true;
                }
                if (args[0].equalsIgnoreCase("lands")) {
                    commands.sendCommandInfo(player, "lands1");
                    return true;
                }

                String textName = args[0];
                commands.sendCommandInfo(player, textName);

                return true;

                // Checks if the player is an OP.
            } else if (sender.isOp()) {

                String textName = args[0];
                commands.sendCommandInfo(player, textName);

                return true;

            }

        }

        if (args.length < 1) {
            player.sendMessage(Component.text("Please specify a book name."));
            return true;
        }

        return true;
    }
}
