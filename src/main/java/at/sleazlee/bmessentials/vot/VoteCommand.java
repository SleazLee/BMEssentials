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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static at.sleazlee.bmessentials.Scheduler.runLater;

public class VoteCommand implements CommandExecutor {
    private static Set<Player> votedPlayers = new HashSet<>();
    private static boolean voteInProgress = false;
    private static int yesVotes = 0;
    private static int noVotes = 0;
    private static String voteOption;
    private Scheduler.Task actionBarTask;
    private HashMap<UUID, Long> lastVoteTime = new HashMap<>();

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

        if (yesVotes > noVotes) {
            Bukkit.broadcastMessage(formatMessage("The vote to change " + voteOption + " has passed. Changing now...", true));
            executeChange(voteOption);
        } else {
            Bukkit.broadcastMessage(formatMessage("The vote to change " + voteOption + " has failed. No change will occur.", false));
        }

        startCooldown();
        votedPlayers.clear();
    }



    private void startVote(String option, Player initiator) {
        long currentTime = System.currentTimeMillis();
        if (lastVoteTime.containsKey(initiator.getUniqueId()) &&
                currentTime - lastVoteTime.get(initiator.getUniqueId()) < 15 * 60 * 1000) {
            initiator.sendMessage(formatMessage("You are still in a cooldown period.", false));
            return;
        }

        lastVoteTime.put(initiator.getUniqueId(), currentTime);
        voteInProgress = true;
        voteOption = option;
        yesVotes = 1;
        noVotes = 0;
        votedPlayers.clear();
        votedPlayers.add(initiator);

        Bukkit.broadcastMessage(formatMessage("A vote to change " + option + " has started! Vote yes or no with /vot yes or /vot no.", true));
        Bukkit.broadcastMessage(ChatColor.YELLOW + initiator.getName() + ChatColor.GREEN + " started the vote and voted YES.");

        checkAndExecuteVote();
    }


    private void checkAndExecuteVote() {
        if (Bukkit.getOnlinePlayers().size() == 1 && yesVotes == 1) {
            // Only one player online and they initiated the vote
            Bukkit.broadcastMessage(formatMessage("As the only player online, your vote to change " + voteOption + " passes automatically.", true));
            executeChange(voteOption);

            voteInProgress = false;
            startCooldown();
            votedPlayers.clear();
            clearActionBar(); // Clear the action bar
        } else {
            // Schedule the normal end of the voting period using Scheduler
            Scheduler.runLater(() -> {
                voteInProgress = false;
                clearActionBar(); // Clear the action bar

                if (yesVotes > noVotes) {
                    Bukkit.broadcastMessage(formatMessage("The vote to change " + voteOption + " has passed. Changing now...", true));
                    executeChange(voteOption);
                } else {
                    Bukkit.broadcastMessage(formatMessage("The vote to change " + voteOption + " has failed. No change will occur.", false));
                }

                startCooldown();
                votedPlayers.clear();
            }, 20 * 60); // 60 seconds
        }
    }



    private void updateVoteProgress() {
        int totalVotes = yesVotes + noVotes;
        int yesPercentage = totalVotes > 0 ? (yesVotes * 10) / totalVotes : 0;
        int noPercentage = 10 - yesPercentage;

        String progressBar = ChatColor.GREEN + StringUtils.repeat("█", yesPercentage) +
                ChatColor.RED + StringUtils.repeat("█", noPercentage);

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
        if (world.getEnvironment() == World.Environment.NETHER || world.getEnvironment() == World.Environment.THE_END) {
            return;
        }

        final long step = 20; // Slightly faster step to speed up time change
        final long periodTicks = 1L; // Update every tick

        long currentTime = world.getTime();
        long targetAdjustedTime = targetTime;

        if (currentTime > targetAdjustedTime) {
            targetAdjustedTime += 24000; // Adjust for full day cycle if needed
        }

        final Scheduler.Task[] taskHolder = new Scheduler.Task[1];

        long finalTargetAdjustedTime = targetAdjustedTime;
        taskHolder[0] = Scheduler.runTimer(new Runnable() {
            long newTime = currentTime;

            @Override
            public void run() {
                newTime = (newTime + step) % 24000;
                world.setTime(newTime);

                if (newTime >= finalTargetAdjustedTime % 24000) {
                    world.setTime(targetTime); // Set exactly the target time, adjusted for any day wrap-around
                    if (taskHolder[0] != null) {
                        taskHolder[0].cancel();
                    }
                }
            }
        }, 0L, periodTicks);
    }
















}