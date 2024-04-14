package at.sleazlee.bmessentials.vot;

import at.sleazlee.bmessentials.BMEssentials;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;

import static at.sleazlee.bmessentials.Scheduler.runLater;

public class VoteCommand implements CommandExecutor {
    private static Set<Player> votedPlayers = new HashSet<>();
    private static boolean voteInProgress = false;
    private static int yesVotes = 0;
    private static int noVotes = 0;
    private static String voteOption;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage(formatMessage("Usage: /vot <day|night|clear|rain|thunder|yes|no>", false));
            return true;
        }

        String option = args[0].toLowerCase();

        if (option.equals("yes") || option.equals("no")) {
            handleVote(option, player);
            return true;
        }

        if (voteInProgress) {
            player.sendMessage(formatMessage("A vote is already in progress.", false));
            return true;
        }

        if (Set.of("day", "night", "clear", "rain", "thunder").contains(option)) {
            startVote(option, player);
        } else {
            player.sendMessage(formatMessage("Invalid option. Usage: /vot <day|night|clear|rain|thunder>", false));
        }

        return true;
    }

    private void handleVote(String option, Player player) {
        if (!voteInProgress) {
            player.sendMessage(formatMessage("There is no vote in progress.", false));
            return;
        }
        if (votedPlayers.contains(player)) {
            player.sendMessage(formatMessage("You have already voted.", false));
            return;
        }

        if (option.equals("yes")) {
            yesVotes++;
            player.sendMessage(formatMessage("You voted YES.", true));
        } else {
            noVotes++;
            player.sendMessage(formatMessage("You voted NO.", true));
        }
        votedPlayers.add(player);
        updateVoteProgress();
    }

    private void startVote(String option, Player initiator) {
        voteInProgress = true;
        voteOption = option;
        yesVotes = 0;
        noVotes = 0;
        votedPlayers.clear();

        Bukkit.broadcastMessage(formatMessage("A vote to change " + option + " has started! Vote yes or no with /vot yes or /vot no.", true));
        Bukkit.broadcastMessage(ChatColor.YELLOW + initiator.getName() + ChatColor.GREEN + " started the vote.");

        runLater(() -> {
            voteInProgress = false;

            if (yesVotes > noVotes) {
                Bukkit.broadcastMessage(formatMessage("The vote to change " + voteOption + " has passed. Changing now...", true));
                executeChange(voteOption);
            } else {
                Bukkit.broadcastMessage(formatMessage("The vote to change " + voteOption + " has failed. No change will occur.", false));
            }

            // Start cooldown for all players
            startCooldown();

            // Clear voted players
            votedPlayers.clear();
        }, 20 * 60); // 60 seconds
    }

    private void updateVoteProgress() {
        int totalVotes = yesVotes + noVotes;
        int yesPercentage = totalVotes > 0 ? (yesVotes * 10) / totalVotes : 0;
        int noPercentage = 10 - yesPercentage;

        String progressBar = ChatColor.GREEN + StringUtils.repeat("█", yesPercentage) +
                ChatColor.RED + StringUtils.repeat("█", noPercentage);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendActionBar(progressBar);
        }
    }

    private void executeChange(String option) {
        for (World world : Bukkit.getWorlds()) {
            if (option.equals("day")) {
                smoothTimeChange(world, 1000); // Time for day start
            } else if (option.equals("night")) {
                smoothTimeChange(world, 13000); // Time for night start
            } else if (option.equals("clear")) {
                world.setStorm(false);
                world.setThundering(false);
            } else if (option.equals("rain")) {
                world.setStorm(true);
                world.setThundering(false);
            } else if (option.equals("thunder")) {
                world.setStorm(true);
                world.setThundering(true);
            }
        }
    }

    private void startCooldown() {
        long cooldownDuration = 15 * 60 * 20L; // 15 minutes in ticks
        for (Player player : Bukkit.getOnlinePlayers()) {
            votedPlayers.add(player);
            player.sendMessage(formatMessage("Vote cooldown started. You cannot initiate a new vote for 15 minutes.", false));
        }

        // Cooldown mechanism (simulated with a delay to accept new votes)
        runLater(() -> {
            votedPlayers.clear(); // Reset the cooldown after 15 minutes
        }, cooldownDuration);
    }

    private String formatMessage(String message, boolean positive) {
        String prefix = ChatColor.translateAlternateColorCodes('&', "&a&lVot &a");
        if (!positive) {
            message = message.replace("&a", "&c");
        }
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }

    private void smoothTimeChange(World world, long targetTime) {
        long currentTime = world.getTime();
        long difference = (targetTime - currentTime) % 24000;
        if (difference < 0) {
            difference += 24000; // ensure positive difference
        }
        int transitionTime = 200; // total time in ticks to complete the transition (10 seconds)
        long step = difference / (transitionTime / 5); // Adjust the step since we're updating less frequently

        new BukkitRunnable() {
            long ticksPassed = 0;

            @Override
            public void run() {
                if (ticksPassed >= transitionTime) {
                    this.cancel();
                } else {
                    world.setTime(world.getTime() + step);
                    ticksPassed += 5;
                }
            }
        }.runTaskTimer(BMEssentials.getInstance(), 0L, 5L); // Update every 5 ticks
    }

}
