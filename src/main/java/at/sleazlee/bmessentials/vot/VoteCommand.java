package at.sleazlee.bmessentials.vot;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

/**
 * Handles the /vot command for initiating and participating in votes.
 */
public class VoteCommand implements CommandExecutor {

    private final VoteManager voteManager = VoteManager.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Ensure the command sender is a player.
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed by players.");
            return true;
        }

        Player player = (Player) sender;

        // Check for correct usage.
        if (args.length != 1) {
            player.sendMessage(ChatColor.GRAY + "Usage: /" + label + " <day|night|clear|rain|thunder|yes|no>");
            return true;
        }

        String option = args[0].toLowerCase();

        // Handle voting options.
        if (option.equals("yes")) {
            voteManager.castVote(player, true);
            return true;
        } else if (option.equals("no")) {
            voteManager.castVote(player, false);
            return true;
        } else if (option.equals("reset") && player.hasPermission("bmessentials.vot.reset")) {
            voteManager.finalizeVote();
            player.sendMessage(ChatColor.GREEN + "Vote has been reset.");
            return true;
        }

        // Start a new vote if the option is valid.
        if (option.equals("day") || option.equals("night") || option.equals("clear") ||
                option.equals("rain") || option.equals("thunder")) {
            voteManager.startVote(option, player);
        } else {
            player.sendMessage(ChatColor.GRAY + "Usage: /" + label + " <day|night|clear|rain|thunder|yes|no>");
        }

        return true;
    }
}
