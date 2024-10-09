package at.sleazlee.bmessentials.HelpMenus;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles the Help Menus system, allowing players to open formatted books via commands.
 * Enhancements include:
 * - Enhanced MiniMessage usage for rich formatting and interactivity.
 * - Asynchronous operations for improved performance.
 * - Rate limiting to prevent command abuse.
 */
public class HelpMenus implements Listener {

    private final BMEssentials plugin;
    private FileConfiguration menusConfig;
    private final File menusFile;

    // Rate limiting: Map of player UUIDs to their last command timestamp
    private final Map<String, Long> playerCooldowns = new ConcurrentHashMap<>();

    // Cooldown period in milliseconds (e.g., 5000 ms = 5 seconds)
    private final long cooldownMillis;

    /**
     * Constructs a new HelpMenus instance and registers event listeners.
     *
     * @param plugin The main plugin instance.
     */
    public HelpMenus(BMEssentials plugin) {
        this.plugin = plugin;
        this.menusFile = new File(plugin.getDataFolder(), "menus.yml");
        this.cooldownMillis = plugin.getConfig().getLong("systems.Menus.cooldown", 5000); // Default 5 seconds
        loadMenusConfigAsync();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Loads the menus configuration file asynchronously. If it doesn't exist, creates it with default content.
     */
    private void loadMenusConfigAsync() {
        Scheduler.run(() -> {
            if (!menusFile.exists()) {
                try {
                    if (menusFile.getParentFile() != null) {
                        menusFile.getParentFile().mkdirs(); // Ensure the data folder exists
                    }
                    menusFile.createNewFile(); // Create the file

                    // Load default content
                    FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(menusFile);
                    defaultConfig.set("Books.book1", List.of(
                            "&2&lTown Claiming&f.Use <and>&a/Towns;&aShows your town menu.;/l<and>&0 to claim &0 &0 &0 and safeguard your &0 &f.&0 builds from player grief! You can: &2- &8Manage Members &2- &8Edit Town Flags &2- &8Assign Chunk Types <and>$RUN_COMMAND$Go Back; /help337<and>"
                    ));
                    defaultConfig.set("Books.getting_started", List.of(
                            "&2&lGetting Started&f.Use <and>&a/t create &8(&aname&8);&aCreate a new town!;/t create <and>&0 &f.&0to create a town. You can then use <and>&a/t claim;&aClaims the 16x16 chunk that you're on.;/t claim <and>&b/rankup;&bProgress through the ranks.;/ranks<and>&f.&0increase max claims &0 &0 &0 &0 and members!"
                    ));
                    defaultConfig.set("Books.inviting_friends", List.of(
                            "&2&lInviting Friends&f.Invite friends with: <and>&3/invite &8(&3player&8);&3Invite a player to your town!;/invite <and>&c&ofull&0 &c&oaccess&0 to your town. You can <and>&3/promote;&3Promote a player to an Admin within your town.<newline>&7You can also /demote (player).;/promote<and> &0.This allows them to invite, claim, and edit all."
                    ));
                    defaultConfig.set("Books.chunk_types_flags", List.of(
                            "&2&lChunk Types/Flags&f.There are 3 different Chunk Types∶ <italic>Private</italic>, <italic>Common</italic>, and <italic>Plots</italic>.<newline><click:run_command:\"/t settings\">TIP: Public means everyone, even people not in your town!!</click>"
                    ));
                    defaultConfig.set("Books.plot_usage", List.of(
                            "&3&lPlot Usage&f.Setting up the plot: <and>&a/Claim;&aClaims the 16x16 chunk that you're on.;/t claim <and>&3/t plot;&3Change the chunk type to a plot.;/t plot <and>&3/t plot add &8(&3name&8);&3Assigns the Plot that you are on to a specific In-Town player.;/plot assign <and>&7&oHover Text</and>Click me!;/help towns<and>"
                    ));
                    defaultConfig.set("systems.Menus.cooldown", 5000); // 5 seconds cooldown
                    defaultConfig.save(menusFile);
                    plugin.getLogger().info("Created default menus.yml");
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not create default menus.yml!");
                    e.printStackTrace();
                }
            }

            menusConfig = YamlConfiguration.loadConfiguration(menusFile);
            plugin.getLogger().info("Loaded menus.yml");
        });
    }

    /**
     * Handles player command preprocessing to intercept book commands and open the corresponding book.
     * Implements rate limiting to prevent abuse.
     *
     * @param event The PlayerCommandPreprocessEvent triggered when a player enters a command.
     */
    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();

        if (!message.startsWith("/")) {
            return; // Not a command
        }

        message = message.substring(1); // Remove the leading '/'
        String[] args = message.split(" ");
        String commandName = args[0].toLowerCase(); // Normalize command name

        // Check if the command is a book command
        if (commandName.startsWith("book")) { // Ensure the command starts with 'book'
            String bookName = commandName.substring(4); // Extract the book name after 'book'

            Player player = event.getPlayer();
            String playerId = player.getUniqueId().toString();
            long currentTime = System.currentTimeMillis();
            Long lastUsed = playerCooldowns.get(playerId);

            if (lastUsed != null && (currentTime - lastUsed) < cooldownMillis) {
                long waitTime = (cooldownMillis - (currentTime - lastUsed)) / 1000;
                player.sendMessage(Component.text("Please wait " + waitTime + " more seconds before opening another book."));
                event.setCancelled(true);
                return;
            }

            // Update the last used time
            playerCooldowns.put(playerId, currentTime);

            // Load the book content asynchronously
            Scheduler.run(() -> {
                if (menusConfig == null) {
                    player.sendMessage(Component.text("Menu system is still loading. Please try again shortly."));
                    return;
                }

                String path = "Books." + bookName;
                if (!menusConfig.contains(path)) {
                    player.sendMessage(Component.text("This book does not exist."));
                    return;
                }

                List<String> bookContent = menusConfig.getStringList(path);
                if (bookContent != null && !bookContent.isEmpty()) {
                    openBook(player, bookContent);
                } else {
                    player.sendMessage(Component.text("This book is empty."));
                }
            });

            event.setCancelled(true); // Cancel the command to prevent "Unknown command" message
        }
    }

    /**
     * Opens a written book with the specified content for the given player.
     *
     * @param player The player who will receive the book.
     * @param pages  The list of page contents, formatted using MiniMessage.
     */
    private void openBook(Player player, List<String> pages) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        if (meta == null) {
            plugin.getLogger().severe("Could not get BookMeta for written book!");
            player.sendMessage(Component.text("An error occurred while opening the book."));
            return;
        }

        MiniMessage miniMessage = MiniMessage.miniMessage();
        LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.builder()
                .useUnusualXRepeatedCharacterHexFormat()
                .build();

        for (String pageText : pages) {
            Component page = miniMessage.deserialize(pageText);
            String legacy = legacySerializer.serialize(page);
            meta.addPage(legacy);
        }

        book.setItemMeta(meta);

        // Use Scheduler to ensure thread safety when interacting with the player
        Scheduler.run(() -> player.openBook(book));
    }
}