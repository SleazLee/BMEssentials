package at.sleazlee.bmessentials.vot;

import at.sleazlee.bmessentials.BMEssentials;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

/**
 * Handles the /vot command for initiating and participating in votes.
 */
public class VoteCommand implements CommandExecutor {

    private final VotBook votBook;

    public VoteCommand(BMEssentials plugin) {
        this.votBook = new VotBook(plugin);
    }

    /**
     * Executes the /vot command.
     *
     * @param sender  the command sender
     * @param command the command
     * @param label   the alias used
     * @param args    the command arguments
     * @return true if the command was successful, false otherwise
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Ensure the command sender is a player.
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be executed by players.");
            return true;
        }

        VoteManager voteManager = VoteManager.getInstance();

        // If no arguments, open the vote book GUI
        if (args.length == 0) {
            openVoteBook(player);
            return true;
        }

        String option = args[0].toLowerCase();

        // Handle voting options.
        switch (option) {
            case "yes" -> {
                voteManager.castVote(player, true);
                return true;
            }
            case "no" -> {
                voteManager.castVote(player, false);
                return true;
            }
            case "reset" -> {
                if (player.hasPermission("bmessentials.vot.reset")) {
                    voteManager.finalizeVote(true); // Indicate that the vote was cancelled
                    player.sendMessage(ChatColor.GREEN + "Vote has been reset.");
                } else {
                    player.sendMessage(ChatColor.RED + "You don't have permission to reset votes.");
                }
                return true;
            }
            case "day", "night", "sun", "rain", "thunder" -> {
                if (!voteManager.startVote(option, player)) {
                    // Vote didn't start, message already sent in startVote method
                    return true;
                }
                return true;
            }
            default -> {
                player.sendMessage(ChatColor.GRAY + "Usage: /" + label + " <day|night|sun|rain|thunder|yes|no>");
                return true;
            }
        }
    }

    /**
     * Opens the appropriate vote book GUI to the player.
     *
     * @param player the player to show the book to
     */
    private void openVoteBook(Player player) {
        VoteManager voteManager = VoteManager.getInstance();
        if (voteManager.isVoteInProgress()) {
            votBook.openBook(player, "CurrentVote");
        } else {
            votBook.openBook(player, "NewVote");
        }
    }
}