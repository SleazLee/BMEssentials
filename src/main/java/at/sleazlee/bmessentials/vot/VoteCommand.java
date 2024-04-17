package at.sleazlee.bmessentials.vot;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static at.sleazlee.bmessentials.Scheduler.runLater;

public class VoteCommand implements CommandExecutor {
    private static Set<Player> votedPlayers = new HashSet<>();
    private static boolean voteInProgress = false;
    private static int yesVotes = 0;
    private static int noVotes = 0;
    private static String voteOption;
    private Scheduler.Task actionBarTask;
    private static long lastVoteTime = 0;  // Global last vote time
    private BossBar bossBar;
    private Scheduler.Task scheduledVoteEndTask;

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

        // Check if all online players have voted
        if (votedPlayers.size() >= Bukkit.getOnlinePlayers().size()) {
            finalizeVote();
        }
    }

    private void finalizeVote() {
        voteInProgress = false;
        clearActionBar(); // Clear the action bar
        if (bossBar != null) {
            bossBar.removeAll();
        }

        if (yesVotes > noVotes) {
            Bukkit.broadcastMessage(formatMessage("The vote to change " + voteOption + " has passed. Changing now...", true));
            executeChange(voteOption);
        } else {
            Bukkit.broadcastMessage(formatMessage("The vote to change " + voteOption + " has failed. No change will occur.", false));
        }

        startCooldown();
        votedPlayers.clear();

        // Also cancel the scheduled task here for good measure
        if (scheduledVoteEndTask != null) {
            scheduledVoteEndTask.cancel();
        }
    }



    private void startVote(String option, Player initiator) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastVoteTime < 15 * 60 * 1000) {
            initiator.sendMessage(formatMessage("A global cooldown is currently in effect.", false));
            return;
        }

        lastVoteTime = currentTime;  // Set the last vote time to now for the global cooldown
        voteInProgress = true;
        voteOption = option;
        yesVotes = 1; // Initiator votes yes by default
        noVotes = 0;
        votedPlayers.clear();
        votedPlayers.add(initiator);

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "A vote to change " + option.toUpperCase() + " has started! Vote yes or no with /vot yes or no."));
        Bukkit.broadcastMessage(ChatColor.YELLOW + initiator.getName() + ChatColor.GREEN + " started the vote and voted YES.");

        // Initialize the BossBar for voting
        if (bossBar != null) {
            bossBar.removeAll();
        }
        bossBar = Bukkit.createBossBar(ChatColor.translateAlternateColorCodes('&', "&7&lPlace vote for &e&l" + option.toUpperCase() + "&7&l! &8| &7&lEnds in &e&l60s"), BarColor.BLUE, BarStyle.SEGMENTED_20);
        for (Player online : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(online);
        }
        bossBar.setProgress(1.0);

        // Start the vote ending countdown
        final int voteDurationSeconds = 60;
        AtomicLong timeLeft = new AtomicLong(voteDurationSeconds); // Keep track of the time left
        scheduledVoteEndTask = Scheduler.runTimer(() -> {
            double progressDecrement = 1.0 / voteDurationSeconds;
            double currentProgress = bossBar.getProgress() - progressDecrement;
            bossBar.setProgress(Math.max(0, currentProgress));  // Ensure progress doesn't go below 0

            // Update the BossBar title with the countdown
            long secsLeft = timeLeft.decrementAndGet();
            bossBar.setTitle(ChatColor.translateAlternateColorCodes('&', "&7&lPlace vote for &e&l" + option.toUpperCase() + "&7&l! &8| &7&lEnds in &e&l" + secsLeft + "s"));

            if (currentProgress <= 0) {
                finalizeVote();  // Automatically finalize vote when boss bar is empty
            }
        }, 20L, 20L); // Update every second (20 ticks per second)

        // Start updating the action bar immediately
        updateVoteProgress();
    }



    private void checkAndExecuteVote() {
        if (Bukkit.getOnlinePlayers().size() == 1 && yesVotes == 1) {
            // Only one player online and they initiated the vote
            Bukkit.broadcastMessage(formatMessage("As the only player online, your vote to change " + voteOption + " passes automatically.", true));
            executeChange(voteOption);

            // Cancel scheduled task if it exists
            if (scheduledVoteEndTask != null) {
                scheduledVoteEndTask.cancel();
            }

            finalizeVote();
        } else {
            // Cancel any previous scheduled task if it exists
            if (scheduledVoteEndTask != null) {
                scheduledVoteEndTask.cancel();
            }

            // Schedule the normal end of the voting period using custom Scheduler
            scheduledVoteEndTask = Scheduler.runLater(() -> {
                // Check if the vote is still in progress before finalizing
                if (voteInProgress) {
                    finalizeVote();
                }
            }, 20 * 60); // 60 seconds
        }
    }



    private void updateVoteProgress() {
        int totalVotes = yesVotes + noVotes;
        int yesPercentage = totalVotes > 0 ? (yesVotes * 100) / totalVotes : 0;
        int noPercentage = 100 - yesPercentage;

        String progressBar = ChatColor.GREEN + StringUtils.repeat("●", yesPercentage / 10) +
                ChatColor.RED + StringUtils.repeat("●", noPercentage / 10);

        if (actionBarTask != null) {
            actionBarTask.cancel(); // Cancel previous task if it exists
        }

        actionBarTask = Scheduler.runTimer(() -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendActionBar(progressBar);
            }
        }, 0L, 20L); // Update action bar every second
    }


    private void clearActionBar() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendActionBar(""); // Clear the action bar
        }
    }

    private void executeChange(String option) {
        for (World world : Bukkit.getWorlds()) {
            Scheduler.run(() -> {
                switch (option) {
                    case "clear":
                        world.setStorm(false);
                        world.setThundering(false);
                        break;
                    case "rain":
                        world.setStorm(true);
                        world.setThundering(false);
                        break;
                    case "thunder":
                        world.setStorm(true);
                        world.setThundering(true);
                        break;
                    case "day":
                        smoothTimeChange(world, 1000); // Roughly sunrise time
                        break;
                    case "night":
                        smoothTimeChange(world, 15000); // Roughly sunset time
                        break;
                }
            });
        }
    }



    private void startCooldown() {
        lastVoteTime = System.currentTimeMillis();  // Set the cooldown starting now
        long cooldownDuration = 15 * 60 * 1000; // 15 minutes in milliseconds
        runLater(() -> {
            // Optional: Add a notification that the cooldown has ended, if needed
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
        if (world.getEnvironment() == World.Environment.NETHER || world.getEnvironment() == World.Environment.THE_END) {
            return; // No day/night cycle in these environments
        }

        AtomicLong currentTime = new AtomicLong(world.getTime());
        long totalTicksToAdd;

        if (targetTime < currentTime.get()) {
            // Calculate the additional ticks needed to pass through midnight
            totalTicksToAdd = (24000 - currentTime.get()) + targetTime;
        } else {
            totalTicksToAdd = targetTime - currentTime.get();
        }

        // Calculate the number of actual ticks this change should be spread across
        final long maxRealTimeTicks = 200; // 10 seconds at 20 ticks per second
        final long baseline = 24000 / maxRealTimeTicks; // Calculate step size per real tick

        long steps = (totalTicksToAdd + baseline - 1) / baseline; // This rounds up the division

        final long step = totalTicksToAdd / steps; // Number of game ticks to add each real tick

        final Scheduler.Task[] taskHolder = new Scheduler.Task[1];

        taskHolder[0] = Scheduler.runTimer(() -> {
            long newCurrentTime = (currentTime.get() + step) % 24000;
            currentTime.set(newCurrentTime);
            world.setTime(newCurrentTime);

            if (Math.abs(newCurrentTime - targetTime) < step || newCurrentTime == targetTime) {
                world.setTime(targetTime); // Correct any overshoot
                if (taskHolder[0] != null) {
                    taskHolder[0].cancel();
                }
            }
        }, 0L, 1L); // Update every real tick
    }

}
