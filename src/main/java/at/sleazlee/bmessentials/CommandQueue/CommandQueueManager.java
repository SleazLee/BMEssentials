package at.sleazlee.bmessentials.CommandQueue;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Manages the CommandQueue by loading commands from the configuration file
 * and executing them as per the executor type (player or console).
 */
public class CommandQueueManager {

    private final BMEssentials plugin;
    private List<String> commands;

    /**
     * Constructor for CommandQueueManager.
     *
     * @param plugin The main plugin instance.
     */
    public CommandQueueManager(BMEssentials plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads commands from CommandQueue.yml. If the file does not exist,
     * it creates the file with default commands.
     */
    public void loadCommands() {
        File commandQueueFile = new File(plugin.getDataFolder(), "CommandQueue.yml");

        if (!commandQueueFile.exists()) {
            createDefaultCommandQueueFile(commandQueueFile);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(commandQueueFile);
        commands = config.getStringList("commands");
    }

    /**
     * Creates the CommandQueue.yml file with default commands.
     *
     * @param file The CommandQueue.yml file to create.
     */
    private void createDefaultCommandQueueFile(File file) {
        try {
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();

            FileWriter writer = new FileWriter(file);
            writer.write("commands:\n  - say Hello, world!\n");
            writer.close();

        } catch (IOException e) {
            plugin.getLogger().severe("Could not create CommandQueue.yml!");
            e.printStackTrace();
        }
    }

    /**
     * Returns the number of commands currently loaded.
     *
     * @return the command count.
     */
    public int getCommandCount() {
        return (commands == null) ? 0 : commands.size();
    }

    /**
     * Resets the CommandQueue.yml to its default state.
     */
    public void resetToDefault() {
        File commandQueueFile = new File(plugin.getDataFolder(), "CommandQueue.yml");
        if (commandQueueFile.exists()) {
            commandQueueFile.delete();
        }
        createDefaultCommandQueueFile(commandQueueFile);
    }

    /**
     * Executes the queued commands either as a player or the console.
     *
     * @param executorType   The type of executor ("player" or "console").
     * @param sender         The command sender initiating the execution.
     * @param delayInSeconds The delay in seconds between commands.
     */
    public void runCommands(String executorType, CommandSender sender, int delayInSeconds) {
        if (commands == null || commands.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No commands found in CommandQueue.yml");
            return;
        }

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a&lQueue &7The Command Queue has started running..."));

        Iterator<String> iterator = commands.iterator();
        int delayInTicks = delayInSeconds * 20; // Convert seconds to ticks

        /**
         * Runnable class to execute each command with the specified delay.
         */
        class CommandRunner implements Runnable {
            @Override
            public void run() {
                if (!iterator.hasNext()) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a&lQueue &7All commands have been run successfully!"));
                    return;
                }

                String cmd = iterator.next();
                if (executorType.equals("player")) {
                    if (sender instanceof Player) {
                        Scheduler.run(() -> Bukkit.dispatchCommand((Player) sender, cmd));
                    } else {
                        sender.sendMessage(ChatColor.RED + "Only players can run commands as player.");
                        return;
                    }
                } else {
                    Scheduler.run(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
                }

                // Schedule the next command after the specified delay
                Scheduler.runLater(this, delayInTicks);
            }
        }

        // Start the command execution
        Scheduler.run(new CommandRunner());
    }
}