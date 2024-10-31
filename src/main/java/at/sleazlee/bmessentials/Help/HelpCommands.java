package at.sleazlee.bmessentials.Help;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.TextUtils.replaceLegacyColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;

import static org.bukkit.Bukkit.getServer;

/**
 * Handles the Commands system, allowing players to get command information.
 * Loads the commands configuration from the plugin resources.
 */
public class HelpCommands {

    private final BMEssentials plugin;
    private FileConfiguration commandsConfig;

    /**
     * Constructs a new CommandsSystem instance and loads the commands configuration.
     *
     * @param plugin The main plugin instance.
     */
    public HelpCommands(BMEssentials plugin) {
        this.plugin = plugin;
        loadCommandsConfig();
    }

    /**
     * Loads the commands configuration from the plugin's resources.
     * After loading, logs the number of commands found.
     */
    private void loadCommandsConfig() {
        InputStream inputStream = plugin.getResource("commands.yml");
        if (inputStream == null) {
            plugin.getLogger().severe("Could not find commands.yml in plugin resources!");
            return;
        }
        commandsConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(inputStream));
        // Get the number of commands and log it
        if (commandsConfig.contains("Commands")) {
            Set<String> commands = commandsConfig.getConfigurationSection("Commands").getKeys(false);
            int numCommands = commands.size();
            getServer().getConsoleSender().sendMessage(ChatColor.GRAY + "    Discovered " + ChatColor.GOLD + numCommands + ChatColor.GRAY + " Commands!");
        } else {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "    No commands found in commands.yml!");
        }
    }

    /**
     * Sends the command information to the player.
     *
     * @param player      The player to send the information to.
     * @param commandName The name of the command to get information about.
     */
    public void sendCommandInfo(Player player, String commandName) {
        if (commandsConfig == null) {
            player.sendMessage(Component.text("Commands system is not loaded."));
            return;
        }

        String path = "Commands." + commandName;
        if (!commandsConfig.contains(path)) {
            player.sendMessage(Component.text("This command does not exist."));
            return;
        }

        String message = commandsConfig.getString(path);
        if (message != null && !message.isEmpty()) {
            String replacedText = replaceLegacyColors.replaceLegacyColors(message);
            Component component = MiniMessage.miniMessage().deserialize(replacedText);
            player.sendMessage(component);
        } else {
            player.sendMessage(Component.text("This command information is empty."));
        }
    }
}
