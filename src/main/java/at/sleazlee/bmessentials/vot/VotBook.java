package at.sleazlee.bmessentials.vot;

import at.sleazlee.bmessentials.BMEssentials;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the book GUI for the voting system.
 */
public class VotBook {

    private final BMEssentials plugin;

    public VotBook(BMEssentials plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens a book for the specified player.
     *
     * @param player   The player who will receive the book.
     * @param bookName The name of the book to open.
     */
    public void openBook(Player player, String bookName) {
        ConfigurationSection booksSection = plugin.getConfig().getConfigurationSection("Systems.Vot.BookLayouts");
        if (booksSection == null) {
            player.sendMessage(Component.text("Voting books are not configured."));
            return;
        }

        List<String> bookContent = booksSection.getStringList(bookName);
        if (bookContent != null && !bookContent.isEmpty()) {
            displayBook(player, bookContent);
        } else {
            player.sendMessage(Component.text("This book is empty or does not exist."));
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

        meta.setTitle("Voting");
        meta.setAuthor("Server");

        MiniMessage miniMessage = MiniMessage.miniMessage();

        List<Component> pageComponents = new ArrayList<>();

        for (String pageText : pages) {
            String replacedText = applyPlaceholders(pageText, player);
            Component page = miniMessage.deserialize(replacedText);
            pageComponents.add(page);
        }

        // Set the pages with Components
        meta.pages(pageComponents);

        book.setItemMeta(meta);
        player.openBook(book);
    }

    /**
     * Applies placeholders to the book content.
     *
     * @param text   The text with placeholders.
     * @param player The player to apply context-specific placeholders.
     * @return The text with placeholders replaced.
     */
    private String applyPlaceholders(String text, Player player) {
        VoteManager voteManager = VoteManager.getInstance();
        if (voteManager.isVoteInProgress()) {
            String voteType = StringUtils.capitalize(voteManager.getVoteOption());
            String color = getColorForVoteType(voteManager.getVoteOption());
            String timeLeft = voteManager.getTimeLeft(player);
            text = text.replace("{vote_type}", voteType);
            text = text.replace("{color}", color);
            text = text.replace("{time_left}", timeLeft);
        }
        return text;
    }

    /**
     * Gets the color code for the given vote type.
     *
     * @param voteType The vote type.
     * @return The color code.
     */
    static String getColorForVoteType(String voteType) {
        return switch (voteType.toLowerCase()) {
            case "day" -> "<#ffd746>";
            case "night" -> "<#1e1e43>";
            case "clear" -> "<#63a1f2>";
            case "rain" -> "<#4e4dce>";
            case "thunder" -> "<#6d5091>";
            default -> "<gold>";
        };
    }
}