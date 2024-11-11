package at.sleazlee.bmessentials.Help;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.TextUtils.TextCenter;
import at.sleazlee.bmessentials.TextUtils.replaceLegacyColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.bukkit.Bukkit.getServer;

/**
 * Handles the Commands system, allowing players to get command information.
 * Loads the commands configuration from the plugin resources.
 */
public class HelpCommands {

    private final BMEssentials plugin;
    private FileConfiguration commandsConfig;

    /**
     * Constructs a new HelpCommands instance and loads the commands configuration.
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

        List<String> messages = commandsConfig.getStringList(path);
        if (messages != null && !messages.isEmpty()) {
            for (String message : messages) {
                // Process the message
                String processedMessage = processMessage(message);

                // Deserialize and send the message to the player
                Component component = MiniMessage.miniMessage().deserialize(processedMessage);
                player.sendMessage(component);
            }
        } else {
            player.sendMessage(Component.text("This command information is empty."));
        }
    }

    /**
     * Processes the message by handling custom placeholders and replacing legacy color codes.
     *
     * @param message The message to process.
     * @return The processed message.
     */
    private String processMessage(String message) {
        // Replace legacy color codes with MiniMessage tags
        message = replaceLegacyColors.replaceLegacyColors(message);

        // Process custom placeholders
        message = processCustomPlaceholders(message);

        return message;
    }

    /**
     * Processes custom placeholders in the message.
     *
     * @param message The message containing placeholders.
     * @return The message with placeholders replaced.
     */
    private String processCustomPlaceholders(String message) {
        // Handle {center [color="..."]}...{/center}
        message = processCenterPlaceholder(message);

        // Handle {fullLineStrike color="..."}
        message = processFullLineStrikePlaceholder(message);

        return message;
    }

    private String processCenterPlaceholder(String message) {
        // Pattern to match {center}...{/center} or {center color="..."}...{/center}
        Pattern centerPattern = Pattern.compile("\\{center(?:\\s+color='([^']+)')?}(.*?)\\{/center}");
        Matcher matcher = centerPattern.matcher(message);
        while (matcher.find()) {
            String strikeColorName = matcher.group(1); // May be null if color is not specified
            String textToCenter = matcher.group(2);

            // Center the text with or without strikethrough lines
            String centeredText = TextCenter.center(textToCenter, strikeColorName);

            // Replace the placeholder with the centered text
            message = matcher.replaceFirst(Matcher.quoteReplacement(centeredText));
            matcher = centerPattern.matcher(message); // Reset matcher after replacement
        }
        return message;
    }

    private String processFullLineStrikePlaceholder(String message) {
        Pattern fullLinePattern = Pattern.compile("\\{fullLineStrike\\s+color='([^']+)'\\}");
        Matcher matcher = fullLinePattern.matcher(message);
        while (matcher.find()) {
            String colorName = matcher.group(1);

            // Generate full-line strike
            String fullLineStrike = TextCenter.fullLineStrike(colorName);

            // Replace the placeholder with the full-line strike
            message = matcher.replaceFirst(Matcher.quoteReplacement(fullLineStrike));
            matcher = fullLinePattern.matcher(message); // Reset matcher after replacement
        }
        return message;
    }
}