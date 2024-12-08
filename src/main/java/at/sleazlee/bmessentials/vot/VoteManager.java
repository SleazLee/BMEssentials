package at.sleazlee.bmessentials.vot;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.*;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.*;
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
    private Player voteInitiator; // New field to track vote initiator

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

        // Log the loaded configuration values
        plugin.getLogger().info("VoteManager initialized with voteDurationSeconds: " + voteDurationSeconds
                + " seconds, cooldownMilliseconds: " + cooldownMilliseconds + " milliseconds");
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
            String messageText = "<color:#ff3300><bold>Vot</bold> <red>There is not able to start next round, while another is running!";
            Component message = MiniMessage.miniMessage().deserialize(messageText);
            initiator.sendMessage(message);
            return false;
        }

        long timeSinceLastVote = System.currentTimeMillis() - lastVoteTime;

        if (timeSinceLastVote < cooldownMilliseconds && !initiator.hasPermission("bmessentials.vot.bypasscooldown")) {
            long timeLeft = (cooldownMilliseconds - timeSinceLastVote) / 1000; // Time left in seconds
            String timeMessage;
            if (timeLeft < 60) {
                timeMessage = timeLeft + "s";
            } else {
                long secondsLeft = timeLeft % 60;
                long minutesLeft = (timeLeft - secondsLeft) / 60;
                timeMessage = minutesLeft + "m " + secondsLeft + "s";
            }
            
            String messageText = "<color:#ff3300><bold>Vot</bold> <red>You must wait " + timeMessage + " before starting a new vote.";
            Component message = MiniMessage.miniMessage().deserialize(messageText);

            initiator.sendMessage(message);
            return false;
        }

        voteInProgress = true;
        voteOption = option.toLowerCase();
        yesVotes = 0;
        noVotes = 0;
        votedPlayers.clear();
        totalPlayersAtVoteStart = Bukkit.getOnlinePlayers().size();
        voteInitiator = initiator;

        String startMessage = ChatColor.AQUA + initiator.getName() + ChatColor.GRAY + " has started a vote for " +
                ChatColor.AQUA + StringUtils.capitalize(option) + ChatColor.GRAY + ".";
        Bukkit.broadcast(Component.text(startMessage));

        displayVotePrompt();

        // Initialize the BossBar for voting.
        initializeBossBar(option);

        // Start the vote ending countdown.
        startVoteCountdown(option);

        // Initiator votes "Yes" by default.
        castVote(initiator, true);

        // Check for single-player scenario
        if (totalPlayersAtVoteStart == 1) {
            finalizeVote(false);
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
            String messageText = "<color:#ff3300><bold>Vot</bold> <red>Currently, no voting round is running!";
            Component message = MiniMessage.miniMessage().deserialize(messageText);
            player.sendMessage(message);
            return;
        }

        if (votedPlayers.contains(player.getUniqueId())) {
            String messageText = "<color:#ff3300><bold>Vot</bold> <red>You have already placed a vote at this round!";
            Component message = MiniMessage.miniMessage().deserialize(messageText);
            player.sendMessage(message);
            return;
        }

        if (voteYes) {
            yesVotes++;
        } else {
            noVotes++;
        }

        votedPlayers.add(player.getUniqueId());

        String messageText = "<yellow><bold>Vot</bold> <gray>Your vote was recorded!";
        Component message = MiniMessage.miniMessage().deserialize(messageText);
        player.sendMessage(message);

        updateVoteProgress();

        // Check if all players have voted.
        if (votedPlayers.size() >= totalPlayersAtVoteStart) {
            finalizeVote(false);
        }
    }

    /**
     * Finalizes the vote, tallies results, and applies changes if successful.
     *
     * @param isCancelled true if the vote was cancelled, false if it ended naturally
     */
    void finalizeVote(boolean isCancelled) {
        if (!voteInProgress) return; // Prevent double finalization

        voteInProgress = false;
        clearActionBar();

        if (scheduledVoteEndTask != null) {
            scheduledVoteEndTask.cancel();
            scheduledVoteEndTask = null;
        }

        if (!isCancelled) {
            lastVoteTime = System.currentTimeMillis(); // Start cooldown
        }

        String capitalizedOption = StringUtils.capitalize(voteOption);
        String customColor = VotBook.getColorForVoteType(voteOption);

        // Boss bar message as per your request
        String bossBarMessage;
        if (yesVotes > noVotes) {
            bossBarMessage = "<gray>Voting was <green><bold>Successful</bold><gray>!";
            executeChange(voteOption);
        } else {
            bossBarMessage = "<gray>Voting was <color:#ff3300><bold>Unsuccessful</bold><gray>!";
        }

        String resultMessage;
        if (yesVotes > noVotes) {
            resultMessage = "<yellow><bold>Vot</bold> <gray>Voting for <gold>" + customColor + "<bold>"
                    + capitalizedOption
                    + "</bold></gold> <gray>was <green><bold>Successful</bold></green><gray>!";
        } else {
            resultMessage = "<yellow><bold>Vot</bold> <gray>Voting for <gold>" + customColor + "<bold>"
                    + capitalizedOption
                    + "</bold></gold> <gray>was <color:#ff3300><bold>Unsuccessful</bold></color:#ff3300><gray>!";
        }

        Component chatMessage = MiniMessage.miniMessage().deserialize(resultMessage);
        Bukkit.broadcast(chatMessage);

        // Update boss bar message
        if (bossBar != null) {
            Component bossBarComponent = MiniMessage.miniMessage().deserialize(bossBarMessage);
            bossBar.name(bossBarComponent);
            bossBar.progress(1.0f); // Reset progress bar if needed

            // Keep the boss bar for 6 more seconds
            Scheduler.runLater(() -> {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.hideBossBar(bossBar);
                }
                bossBar = null;
            }, 120L); // 6 seconds (20 ticks per second)
        }

        votedPlayers.clear();
    }

    /**
     * Overloaded method for finalizing a vote without specifying cancellation.
     * Assumes the vote was not cancelled.
     */
    void finalizeVote() {
        finalizeVote(false);
    }









    boolean isCooldownActive(Player initiator) {

        long timeSinceLastVote = System.currentTimeMillis() - lastVoteTime;

        if (timeSinceLastVote < cooldownMilliseconds && !initiator.hasPermission("bmessentials.vot.bypasscooldown")) {
            return true;
        }
        return false;
    }

    String getTimeLeft(Player initiator) {

        long timeSinceLastVote = System.currentTimeMillis() - lastVoteTime;

        String timeMessage = "null";
        if (timeSinceLastVote < cooldownMilliseconds) {
            long timeLeft = (cooldownMilliseconds - timeSinceLastVote) / 1000; // Time left in seconds
            if (timeLeft < 60) {
                timeMessage = timeLeft + "s";
            } else {
                long secondsLeft = timeLeft % 60;
                long minutesLeft = (timeLeft - secondsLeft) / 60;
                timeMessage = minutesLeft + "m " + secondsLeft + "s";
            }

        }
        return timeMessage;
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
                    case "clear" -> {
                        world.setStorm(false);
                        world.setThundering(false);
                    }
                    case "rain" -> {
                        world.setStorm(true);
                        world.setThundering(false);
                    }
                    case "thunder" -> {
                        world.setStorm(true);
                        world.setThundering(true);
                    }
                    case "day" -> smoothTimeChange(world, 1000); // Sunrise time
                    case "night" -> smoothTimeChange(world, 13000); // Sunset time
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
        int progressBars = 14;

        double yesPercentage = yesVotes / (double) totalPlayers;
        double noPercentage = noVotes / (double) totalPlayers;

        int yesBars = (int) Math.round(yesPercentage * progressBars);
        int noBars = (int) Math.round(noPercentage * progressBars);
        int neutralBars = progressBars - yesBars - noBars;

        // Ensure bars do not exceed total
        if (yesBars + noBars > progressBars) {
            int totalBars = yesBars + noBars;
            double scalingFactor = (double) progressBars / totalBars;
            yesBars = (int) Math.round(yesBars * scalingFactor);
            noBars = (int) Math.round(noBars * scalingFactor);
            neutralBars = progressBars - yesBars - noBars;
        }

        // Build the progress bar
        String barChar = "■";
        StringBuilder progressBar = new StringBuilder();
        progressBar.append(ChatColor.GREEN).append(StringUtils.repeat(barChar, yesBars));
        progressBar.append(ChatColor.RED).append(StringUtils.repeat(barChar, noBars));
        progressBar.append(ChatColor.GRAY).append(StringUtils.repeat(barChar, neutralBars));

        String actionBarMessage = progressBar.toString();

        if (actionBarTask != null) {
            actionBarTask.cancel();
        }

        actionBarTask = Scheduler.runTimer(() -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendActionBar(Component.text(actionBarMessage));
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
            online.sendActionBar(Component.empty()); // Clear the action bar
        }
    }

    /**
     * Initializes the boss bar for the voting session.
     *
     * @param option the voting option
     */
    private void initializeBossBar(String option) {
        String capitalizedOption = StringUtils.capitalize(option);

        if (bossBar != null) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.hideBossBar(bossBar);
            }
        }
        bossBar = BossBar.bossBar(
                Component.text().append(Component.text("Voting for ", NamedTextColor.GRAY))
                        .append(Component.text(capitalizedOption, NamedTextColor.GREEN, TextDecoration.BOLD))
                        .append(Component.text("! | ", NamedTextColor.GRAY))
                        .append(Component.text(voteDurationSeconds + "s", NamedTextColor.AQUA))
                        .build(),
                1.0f,
                BossBar.Color.BLUE,
                BossBar.Overlay.PROGRESS
        );
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showBossBar(bossBar);
        }
    }

    /**
     * Starts the countdown for the voting session using the boss bar.
     *
     * @param option the voting option
     */
    private void startVoteCountdown(String option) {
        String capitalizedOption = StringUtils.capitalize(option);
        AtomicLong timeLeft = new AtomicLong(voteDurationSeconds);

        scheduledVoteEndTask = Scheduler.runTimer(() -> {
            float progress = timeLeft.get() / (float) voteDurationSeconds;
            bossBar.progress(progress);

            long secsLeft = timeLeft.decrementAndGet();
            bossBar.name(Component.text().append(Component.text("Voting for ", NamedTextColor.GRAY))
                    .append(Component.text(capitalizedOption, NamedTextColor.GREEN, TextDecoration.BOLD))
                    .append(Component.text("! | ", NamedTextColor.GRAY))
                    .append(Component.text(secsLeft + "s", NamedTextColor.AQUA))
                    .build());

            if (secsLeft <= 0) {
                finalizeVote(false);
            }
        }, 20L, 20L); // Update every second
    }

    /**
     * Displays the vote prompt to all players with clickable text.
     */
    private void displayVotePrompt() {
        String messageText = "<gray>Click to vote: "
                + "<click:run_command:/vot yes><hover:show_text:'<green>Click to vote Yes!'>"
                + "<green><bold>Yes</bold></hover></click> "
                + "<dark_gray>/ "
                + "<click:run_command:/vot no><hover:show_text:'<color:#ff3300>Click to vote No!'>"
                + "<color:#ff3300><bold>No</bold></hover></click>";

        Component message = MiniMessage.miniMessage().deserialize(messageText);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(voteInitiator)) { // Exclude the initiator
                online.sendMessage(message);
            }
        }
    }

    /**
     * Handles player joining during a vote.
     *
     * @param player the player who joined
     */
    public void handlePlayerJoin(Player player) {
        if (voteInProgress && bossBar != null) {
            player.showBossBar(bossBar);
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
            player.hideBossBar(bossBar);
            // Remove vote if the player had voted
            if (votedPlayers.remove(player.getUniqueId())) {
                if (votedPlayers.size() >= totalPlayersAtVoteStart) {
                    finalizeVote(false);
                    return;
                }
            }
            totalPlayersAtVoteStart = Bukkit.getOnlinePlayers().size();
            updateVoteProgress();

            // If no players are left, cancel the vote
            if (totalPlayersAtVoteStart == 0) {
                finalizeVote(true);
            }
        }
    }

    /**
     * Checks if a vote is currently in progress.
     *
     * @return true if a vote is in progress, false otherwise
     */
    public boolean isVoteInProgress() {
        return voteInProgress;
    }

    /**
     * Gets the current voting option.
     *
     * @return the voting option
     */
    public String getVoteOption() {
        return voteOption;
    }
}