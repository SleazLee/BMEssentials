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
            switch (args[0].toLowerCase()) {
                case "abilities" -> commandsSystem.sendCommandInfo(player, "abilities");
                case "basics" -> commandsSystem.sendCommandInfo(player, "basics");
                case "bms" -> commandsSystem.sendCommandInfo(player, "bms");
                case "chat" -> commandsSystem.sendCommandInfo(player, "communication");
                case "chestshop" -> commandsSystem.sendCommandInfo(player, "chestshop");
                case "fun" -> commandsSystem.sendCommandInfo(player, "fun");
                case "lands" -> commandsSystem.sendCommandInfo(player, "lands1");
                case "mcmmo" -> commandsSystem.sendCommandInfo(player, "mcmmo");
                case "settings" -> commandsSystem.sendCommandInfo(player, "settings");
                case "teleportation" -> commandsSystem.sendCommandInfo(player, "teleportation");
                case "trophies" -> commandsSystem.sendCommandInfo(player, "trophies");
                case "unlocks" -> commandsSystem.sendCommandInfo(player, "unlocks");
                case "vip" -> commandsSystem.sendCommandInfo(player, "vip");
                default -> {
                    player.sendMessage("Unknown command. Try /help commands for a list.");
                    return true;
                }
            }

        }

        if (args.length < 1) {
            player.sendMessage(Component.text("Please specify a command name."));
            return true;
        }

        return true;
    }
}
