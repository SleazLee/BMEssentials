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
            switch (args[0].toLowerCase()) {
                case "abilities" -> commands.sendCommandInfo(player, "abilities");
                case "basics" -> commands.sendCommandInfo(player, "basics");
                case "bms" -> commands.sendCommandInfo(player, "bms");
                case "chat" -> commands.sendCommandInfo(player, "communication");
                case "chestshop" -> commands.sendCommandInfo(player, "chestshop");
                case "fun" -> commands.sendCommandInfo(player, "fun");
                case "lands" -> commands.sendCommandInfo(player, "lands1");
                case "mcmmo" -> commands.sendCommandInfo(player, "mcmmo");
                case "settings" -> commands.sendCommandInfo(player, "settings");
                case "teleportation" -> commands.sendCommandInfo(player, "teleportation");
                case "trophies" -> commands.sendCommandInfo(player, "trophies");
                case "unlocks" -> commands.sendCommandInfo(player, "unlocks");
                case "vip" -> commands.sendCommandInfo(player, "vip");
                default -> {
                    player.sendMessage("Unknown command. Try /help commands for a list.");
                    return true;
                }
            }

        }

        if (args.length < 1) {
            player.sendMessage(Component.text("Please specify a book name."));
            return true;
        }

        return true;
    }
}
