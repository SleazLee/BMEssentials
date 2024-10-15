package at.sleazlee.bmessentials.Help.Commands;

import at.sleazlee.bmessentials.Help.HelpBooks;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * CommandExecutor for handling the /book command.
 */
public class BookCommand implements CommandExecutor {

    private final HelpBooks books;

    /**
     * Constructs a new BookCommand instance.
     *
     * @param books The Books instance to use.
     */
    public BookCommand(HelpBooks books) {
        this.books = books;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can run this command.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Component.text("Please specify a book name."));
            return true;
        }

        String bookName = args[0];
        books.openBook(player, bookName);

        return true;
    }
}
