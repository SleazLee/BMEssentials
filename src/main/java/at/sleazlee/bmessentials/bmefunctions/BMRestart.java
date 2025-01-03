package at.sleazlee.bmessentials.bmefunctions;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class BMRestart implements CommandExecutor {

    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public BMRestart(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Ensure the command is executed by the console
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(miniMessage.deserialize("<red><bold>Only the console can execute this command.</>"));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(miniMessage.deserialize("<red>Incorrect usage. Please use: /bmrestart <30|15|5|1></>"));
            return true;
        }

        String timeArg = args[0];
        String message;

        switch (timeArg) {
            case "30":
                message = "<red><bold>Restart</bold> <gray>The server will restart in <red>30 minutes</gray>!";
                break;
            case "15":
                message = "<red><bold>Restart</bold> <gray>The server will restart in <red>15 minutes</gray>!";
                break;
            case "5":
                message = "<red><bold>Restart</bold> <gray>The server will restart in <red>5 minutes</gray>!";
                break;
            case "1":
                message = "<red><bold>Restart</bold> <gray>The server will restart in <red>1 minute</gray>!";
                break;
            default:
                sender.sendMessage(miniMessage.deserialize("<red>Invalid option. Choose from: 30, 15, 5, 1.</>"));
                return true;
        }

        // Broadcast the message to all players
        Bukkit.broadcast(miniMessage.deserialize(message));
        return true;
    }
}
