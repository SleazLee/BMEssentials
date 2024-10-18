package at.sleazlee.bmessentials.Help;

import at.sleazlee.bmessentials.BMEssentials;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.bukkit.Bukkit.getServer;

/**
 * Handles the Books system, allowing players to open formatted books via commands.
 * Loads the books configuration from the plugin resources.
 */
public class HelpBooks {

    private final BMEssentials plugin;
    private FileConfiguration booksConfig;

    /**
     * Constructs a new Books instance and loads the books configuration.
     *
     * @param plugin The main plugin instance.
     */
    public HelpBooks(BMEssentials plugin) {
        this.plugin = plugin;
        loadBooksConfig();
    }

    /**
     * Loads the books configuration from the plugin's resources.
     * After loading, logs the number of books found.
     */
    private void loadBooksConfig() {
        InputStream inputStream = plugin.getResource("books.yml");
        if (inputStream == null) {
            plugin.getLogger().severe("Could not find books.yml in plugin resources!");
            return;
        }
        booksConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(inputStream));

        // Get the number of books and log it
        if (booksConfig.contains("Books")) {
            Set<String> books = booksConfig.getConfigurationSection("Books").getKeys(false);
            int numBooks = books.size();
            getServer().getConsoleSender().sendMessage(ChatColor.GRAY + "    Discovered " + ChatColor.DARK_AQUA + numBooks + ChatColor.GRAY + " Books!");
        } else {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "    No books found in books.yml!");
        }
    }

    /**
     * Opens a book for the specified player.
     *
     * @param player   The player who will receive the book.
     * @param bookName The name of the book to open.
     */
    public void openBook(Player player, String bookName) {
        if (booksConfig == null) {
            player.sendMessage(Component.text("Book system is not loaded."));
            return;
        }

        String path = "Books." + bookName;
        if (!booksConfig.contains(path)) {
            player.sendMessage(Component.text("This book does not exist."));
            return;
        }

        List<String> bookContent = booksConfig.getStringList(path);
        if (bookContent != null && !bookContent.isEmpty()) {
            displayBook(player, bookContent);
        } else {
            player.sendMessage(Component.text("This book is empty."));
        }
    }

    /**
     * Creates a virtual book with the specified content and opens it for the player.
     *
     * @param player The player who will receive the book.
     * @param pages  The list of page contents, formatted using MiniMessage.
     */
    private void displayBook(Player player, List<String> pages) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        if (meta == null) {
            plugin.getLogger().severe("Could not get BookMeta for written book!");
            player.sendMessage(Component.text("An error occurred while opening the book."));
            return;
        }

        meta.setTitle("Help Book");
        meta.setAuthor("Server");

        MiniMessage miniMessage = MiniMessage.miniMessage();

        List<Component> pageComponents = new ArrayList<>();

        for (String pageText : pages) {
            String replacedText = TextUtils.replaceLegacyColors(pageText);
            Component page = miniMessage.deserialize(replacedText);
            pageComponents.add(page);
        }

        // Set the pages with Components
        meta.pages(pageComponents);

        book.setItemMeta(meta);
        player.openBook(book);
    }
}
