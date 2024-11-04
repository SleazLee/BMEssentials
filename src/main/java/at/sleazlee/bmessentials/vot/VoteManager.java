package at.sleazlee.bmessentials.vot;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages the voting process for time and weather changes.
 */
public class VoteManager {

    private static VoteManager instance;

    /**
     * Gets the singleton instance of the VoteManager.
     *
     * @return the VoteManager instance
     */
    public static VoteManager getInstance() {
        if (instance == null) {
            instance = new VoteManager();
        }
        return instance;
    }

    private final Set<UUID> votedPlayers = new HashSet<>();
    private boolean voteInProgress = false;
    private int yesVotes = 0;
    private int noVotes = 0;
    private String voteOption;
    private long lastVoteTime = 0;
    private BossBar bossBar;
    private Scheduler.Task actionBarTask;
    private Scheduler.Task scheduledVoteEndTask;
    private int totalPlayersAtVoteStart;

    private final BMEssentials plugin;
    private int voteDurationSeconds;
    private long cooldownMilliseconds;

    /**
     * Private constructor for singleton pattern.
     */
    private VoteManager() {
        this.plugin = BMEssentials.getInstance();

        // Ensure the plugin instance is available
        if (plugin == null) {
            throw new IllegalStateException("BMEssentials instance is not available yet!");
        }

        // Load configurable values
        voteDurationSeconds = plugin.getConfig().getInt("Systems.Vot.VoteDurationSeconds", 60);
        cooldownMilliseconds = plugin.getConfig().getInt("Systems.Vot.CooldownMinutes", 15) * 60 * 1000;
    }

    /**
     * Starts a new vote for the specified option.
     *
     * @param option    the voting option (e.g., "day", "night")
     * @param initiator the player who initiated the vote
     * @return true if the vote started successfully, false otherwise
     */
    public boolean startVote(String option, Player initiator) {
        if (voteInProgress) {
            initiator.sendMessage(ChatColor.RED + "A vote is already in progress.");
            return false;
        }

        long timeSinceLastVote = System.currentTimeMillis() - lastVoteTime;

        if (timeSinceLastVote < cooldownMilliseconds && !initiator.hasPermission("bmessentials.vot.bypasscooldown")) {
            long timeLeft = (cooldownMilliseconds - timeSinceLastVote) / 1000; // Time left in seconds
            initiator.sendMessage(ChatColor.RED + "You must wait " + timeLeft + " seconds before starting a new vote.");
            return false;
        }

        voteInProgress = true;
        voteOption = option.toLowerCase();
        yesVotes = 0;
        noVotes = 0;
        votedPlayers.clear();
        totalPlayersAtVoteStart = Bukkit.getOnlinePlayers().size();

        String startMessage = ChatColor.AQUA + initiator.getName() + ChatColor.GRAY + " has started a vote for " +
                ChatColor.AQUA + option + ChatColor.GRAY + ".";
        Bukkit.broadcastMessage(startMessage);

        displayVotePrompt();

        // Initialize the BossBar for voting.
        initializeBossBar(option);

        // Start the vote ending countdown.
        startVoteCountdown(option);

        // Initiator votes "Yes" by default.
        castVote(initiator, true);

        // Check for single-player scenario
        if (totalPlayersAtVoteStart == 1) {
            finalizeVote();
            return true;
        }

        return true;
    }

    /**
     * Allows a player to cast a vote.
     *
     * @param player  the player who is voting
     * @param voteYes true if voting "Yes", false if "No"
     */
    public void castVote(Player player, boolean voteYes) {
        if (!voteInProgress) {
            player.sendMessage(ChatColor.RED + "There is no vote in progress.");
            return;
        }

        if (votedPlayers.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You have already voted.");
            return;
        }

        if (voteYes) {
            yesVotes++;
            player.sendMessage(ChatColor.GREEN + "You voted YES.");
        } else {
            noVotes++;
            player.sendMessage(ChatColor.RED + "You voted NO.");
        }

        votedPlayers.add(player.getUniqueId());
        updateVoteProgress();

        // Check if all players have voted.
        if (votedPlayers.size() >= totalPlayersAtVoteStart) {
            finalizeVote();
        }
    }

    /**
     * Finalizes the vote, tallies results, and applies changes if successful.
     */
    void finalizeVote() {
        if (!voteInProgress) return; // Prevent double finalization

        voteInProgress = false;
        clearActionBar();

        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }

        if (scheduledVoteEndTask != null) {
            scheduledVoteEndTask.cancel();
            scheduledVoteEndTask = null;
        }

        lastVoteTime = System.currentTimeMillis(); // Start cooldown

        if (yesVotes > noVotes) {
            Bukkit.broadcastMessage(ChatColor.GREEN + "The vote to change to " + voteOption + " has passed.");
            executeChange(voteOption);
        } else {
            Bukkit.broadcastMessage(ChatColor.RED + "The vote to change to " + voteOption + " has failed.");
        }

        String resultMessage = ChatColor.GRAY + "Vote Results: " + ChatColor.GREEN + yesVotes + " Yes " + ChatColor.DARK_GRAY + "| " +
                ChatColor.RED + noVotes + " No";
        Bukkit.broadcastMessage(resultMessage);

        votedPlayers.clear();
    }

    /**
     * Applies the vote outcome by changing the time or weather.
     *
     * @param option the option that was voted on
     */
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
                        smoothTimeChange(world, 1000); // Sunrise time
                        break;
                    case "night":
                        smoothTimeChange(world, 13000); // Sunset time
                        break;
                }
            });
        }
    }

    /**
     * Smoothly transitions the world's time to the target time, always moving forward.
     *
     * @param world      the world to change the time in
     * @param targetTime the target time (in ticks)
     */
    private void smoothTimeChange(World world, long targetTime) {
        if (world.getEnvironment() == World.Environment.NETHER || world.getEnvironment() == World.Environment.THE_END) {
            return; // No day/night cycle in these environments
        }

        AtomicLong currentTime = new AtomicLong(world.getTime());

        // Calculate total ticks to add to reach the target time, always moving forward
        long totalTicksToAdd = (targetTime - currentTime.get() + 24000) % 24000;

        // Desired time increment per tick
        final long desiredTimeIncrement = 60; // Adjust this value for speed of transition

        // Calculate the number of steps
        final long steps = totalTicksToAdd / desiredTimeIncrement;
        final long transitionDurationTicks = Math.max(1, steps);

        // Adjust time increment to divide totalTicksToAdd evenly
        final long timeIncrement = Math.max(1, totalTicksToAdd / transitionDurationTicks);

        final Scheduler.Task[] taskHolder = new Scheduler.Task[1];
        taskHolder[0] = Scheduler.runTimer(() -> {
            long newTime = (currentTime.get() + timeIncrement) % 24000;
            currentTime.set(newTime);
            world.setTime(newTime);

            long ticksRemaining = (targetTime - currentTime.get() + 24000) % 24000;

            if (ticksRemaining <= 0 || ticksRemaining < timeIncrement) {
                world.setTime(targetTime); // Ensure exact target time is set
                taskHolder[0].cancel();
            }
        }, 0L, 1L); // Runs every tick
    }

    /**
     * Updates the action bar with the current voting progress.
     */
    private void updateVoteProgress() {
        int totalPlayers = totalPlayersAtVoteStart;
        int progressBars = 20;

        double yesPercentage = yesVotes / (double) totalPlayers;
        double noPercentage = noVotes / (double) totalPlayers;

        int yesBars = (int) Math.round(yesPercentage * progressBars);
        int noBars = (int) Math.round(noPercentage * progressBars);
        int neutralBars = progressBars - yesBars - noBars;

        // Ensure bars do not exceed total
        if (yesBars + noBars + neutralBars > progressBars) {
            neutralBars = progressBars - yesBars - noBars;
        }

        StringBuilder progressBar = new StringBuilder();
        progressBar.append(ChatColor.GREEN).append(StringUtils.repeat("█", yesBars));
        progressBar.append(ChatColor.GRAY).append(StringUtils.repeat("█", neutralBars));
        progressBar.append(ChatColor.RED).append(StringUtils.repeat("█", noBars));

        String actionBarMessage = ChatColor.GRAY + "Voting Progress: " + progressBar.toString();

        if (actionBarTask != null) {
            actionBarTask.cancel();
        }

        actionBarTask = Scheduler.runTimer(() -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendActionBar(actionBarMessage);
            }
        }, 0L, 20L); // Update every second
    }

    /**
     * Clears the action bar messages from all players.
     */
    private void clearActionBar() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendActionBar(""); // Clear the action bar
        }
    }

    /**
     * Initializes the boss bar for the voting session.
     *
     * @param option the voting option
     */
    private void initializeBossBar(String option) {
        if (bossBar != null) {
            bossBar.removeAll();
        }
        bossBar = Bukkit.createBossBar(
                ChatColor.GRAY + "Vote for " + ChatColor.YELLOW + option + ChatColor.GRAY + "! Ends in " + voteDurationSeconds + "s",
                BarColor.BLUE, BarStyle.SEGMENTED_20);
        for (Player online : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(online);
        }
        bossBar.setProgress(1.0);
    }

    /**
     * Starts the countdown for the voting session using the boss bar.
     *
     * @param option the voting option
     */
    private void startVoteCountdown(String option) {
        AtomicLong timeLeft = new AtomicLong(voteDurationSeconds);

        scheduledVoteEndTask = Scheduler.runTimer(() -> {
            double progress = timeLeft.get() / (double) voteDurationSeconds;
            bossBar.setProgress(progress);

            long secsLeft = timeLeft.decrementAndGet();
            bossBar.setTitle(ChatColor.GRAY + "Vote for " + ChatColor.YELLOW + option + ChatColor.GRAY +
                    "! Ends in " + ChatColor.YELLOW + secsLeft + "s");

            if (secsLeft <= 0) {
                finalizeVote();
            }
        }, 20L, 20L); // Update every second
    }

    /**
     * Displays the vote prompt to all players with clickable text.
     */
    private void displayVotePrompt() {
        TextComponent message = new TextComponent(ChatColor.GRAY + "Click to vote: ");
        TextComponent yesComponent = new TextComponent(ChatColor.GREEN + "[YES]");
        yesComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/vot yes"));
        TextComponent separator = new TextComponent(ChatColor.DARK_GRAY + " / ");
        TextComponent noComponent = new TextComponent(ChatColor.RED + "[NO]");
        noComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/vot no"));

        message.addExtra(yesComponent);
        message.addExtra(separator);
        message.addExtra(noComponent);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.spigot().sendMessage(message);
        }
    }

    /**
     * Handles player joining during a vote.
     *
     * @param player the player who joined
     */
    public void handlePlayerJoin(Player player) {
        if (voteInProgress && bossBar != null) {
            bossBar.addPlayer(player);
            totalPlayersAtVoteStart = Bukkit.getOnlinePlayers().size();
            updateVoteProgress();
        }
    }

    /**
     * Handles player quitting during a vote.
     *
     * @param player the player who quit
     */
    public void handlePlayerQuit(Player player) {
        if (voteInProgress && bossBar != null) {
            bossBar.removePlayer(player);
            // Remove vote if the player had voted
            if (votedPlayers.remove(player.getUniqueId())) {
                if (votedPlayers.size() >= totalPlayersAtVoteStart) {
                    finalizeVote();
                    return;
                }
            }
            totalPlayersAtVoteStart = Bukkit.getOnlinePlayers().size();
            updateVoteProgress();

            // If no players are left, cancel the vote
            if (totalPlayersAtVoteStart == 0) {
                finalizeVote();
            }
        }
    }
}
