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

        // Check for the "reload" subcommand.
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            // Optional: Check for a permission before allowing a reload.
            if (!sender.hasPermission("bmessentials.books.reload")) {
                sender.sendMessage(Component.text("You do not have permission to reload the books configuration."));
                return true;
            }

            books.reloadBooksConfig();
            sender.sendMessage(Component.text("Books configuration reloaded successfully."));
            return true;
        }


        // Shows books without prerequisites
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("help") ||
                    args[0].equalsIgnoreCase("votemenu") ||
                    args[0].equalsIgnoreCase("votehelp") ||
                    args[0].equalsIgnoreCase("rankhelp") ||
                    args[0].equalsIgnoreCase("economy") ||
                    args[0].equalsIgnoreCase("claiming") ||
                    args[0].equalsIgnoreCase("settings") ||
                    args[0].equalsIgnoreCase("abilities") ||
                    args[0].equalsIgnoreCase("commands") ||
                    args[0].equalsIgnoreCase("servercommands") ||
                    args[0].equalsIgnoreCase("abilitiesunlockscommands") ||
                    args[0].equalsIgnoreCase("shopcommands")) {

                String bookName = args[0];
                books.openBook(player, bookName);

                return true;

                // Checks if the player is an OP.
            } else if (sender.isOp()) {

                String bookName = args[0];
                books.openBook(player, bookName);

                return true;

            }
            
        }


        if (args.length < 1) {
            player.sendMessage(Component.text("Please specify a book name."));
            return true;
        }

        return true;
    }
}
