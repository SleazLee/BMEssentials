package at.sleazlee.bmessentials.vot;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import at.sleazlee.bmessentials.AFKSystem.AfkManager; // Ensure you import your AFK manager.
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
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
    private static boolean voteInProgress = false;
    private int yesVotes = 0;
    private int noVotes = 0;
    private String voteOption;
    private long lastVoteTime = 0;
    private BossBar bossBar;
    private Scheduler.Task actionBarTask;
    private Scheduler.Task scheduledVoteEndTask;
    private int totalPlayersAtVoteStart;
    private Player voteInitiator; // Tracks vote initiator

    private final BMEssentials plugin;
    private int votDurationSeconds;
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
        votDurationSeconds = plugin.getConfig().getInt("Systems.Vot.VotDurationSeconds", 60);
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
            String messageText = "<color:#ff3300><bold>Vot</bold> <red>Cannot start a new vote while another is running!";
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
        // Only count online players that are not AFK.
        totalPlayersAtVoteStart = (int) Bukkit.getOnlinePlayers().stream()
                .filter(p -> !AfkManager.getInstance().isAfk(p))
                .count();
        voteInitiator = initiator;

        String capitalizedOption = StringUtils.capitalize(voteOption);
        String customColor = VotBook.getColorForVoteType(voteOption);
        String startMessage = "<aqua>" + initiator.getName() + "<gray> has started a vote for <aqua>" + customColor +
                capitalizedOption + "<gray>.";

        Component chatMessage = MiniMessage.miniMessage().deserialize(startMessage);
        Bukkit.broadcast(chatMessage);

        displayVotePrompt();
        initializeBossBar(option);
        startVoteCountdown(option);

        // Initiator votes "Yes" by default.
        castVote(initiator, true);

        // If only one non-AFK player is online, finalize immediately.
        if (totalPlayersAtVoteStart <= 1) {
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
            String messageText = "<color:#ff3300><bold>Vot</bold> <red>No vote is currently running!";
            Component message = MiniMessage.miniMessage().deserialize(messageText);
            player.sendMessage(message);
            return;
        }

        // Prevent AFK players from voting.
        if (AfkManager.getInstance().isAfk(player)) {
            player.sendMessage(ChatColor.RED + "You cannot vote while AFK. Please become active to vote.");
            return;
        }

        if (votedPlayers.contains(player.getUniqueId())) {
            String messageText = "<color:#ff3300><bold>Vot</bold> <red>You have already voted this round!";
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

        String messageText = "<yellow><bold>Vot</bold> <gray>Your vote has been recorded!";
        Component message = MiniMessage.miniMessage().deserialize(messageText);
        player.sendMessage(message);

        updateVoteProgress();

        // Finalize if all eligible (non-AFK) players have voted.
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

        String bossBarMessage;
        if (yesVotes > noVotes) {
            bossBarMessage = "<gray>Voting was <green><bold>Successful</bold><gray>!";
            executeChange(voteOption);
        } else {
            bossBarMessage = "<gray>Voting was <color:#ff3300><bold>Unsuccessful</bold><gray>!";
        }

        String resultMessage;
        if (yesVotes > noVotes) {
            resultMessage = "<yellow><bold>Vot</bold> <gray>Vote for <gold>" + customColor + "<bold>"
                    + capitalizedOption + "</bold></gold> <gray>was <green><bold>Successful</bold></green><gray>!";
        } else {
            resultMessage = "<yellow><bold>Vot</bold> <gray>Vote for <gold>" + customColor + "<bold>"
                    + capitalizedOption + "</bold></gold> <gray>was <color:#ff3300><bold>Unsuccessful</bold></color:#ff3300><gray>!";
        }

        Component chatMessage = MiniMessage.miniMessage().deserialize(resultMessage);
        Bukkit.broadcast(chatMessage);

        if (bossBar != null) {
            final BossBar barToHide = bossBar; // capture current bar for delayed task
            Component bossBarComponent = MiniMessage.miniMessage().deserialize(bossBarMessage);
            barToHide.name(bossBarComponent);
            barToHide.progress(1.0f);

            Scheduler.runLater(() -> {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.hideBossBar(barToHide);
                }
                // Only null out the reference if no new vote replaced it
                if (bossBar == barToHide) {
                    bossBar = null;
                }
            }, 120L);
        }

        votedPlayers.clear();
    }

    /**
     * Overloaded method for finalizing a vote without specifying cancellation.
     */
    void finalizeVote() {
        finalizeVote(false);
    }

    /**
     * Checks if a vote cooldown is active for the initiator.
     *
     * @param initiator the player who initiated the vote
     * @return true if cooldown is active, false otherwise
     */
    boolean isCooldownActive(Player initiator) {
        long timeSinceLastVote = System.currentTimeMillis() - lastVoteTime;
        return (timeSinceLastVote < cooldownMilliseconds && !initiator.hasPermission("bmessentials.vot.bypasscooldown"));
    }

    String getTimeLeft() {
        long timeSinceLastVote = System.currentTimeMillis() - lastVoteTime;
        String timeMessage = "null";
        if (timeSinceLastVote < cooldownMilliseconds) {
            long timeLeft = (cooldownMilliseconds - timeSinceLastVote) / 1000;
            if (timeLeft < 60) {
                timeMessage = "0m " + timeLeft + "s";
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
                    case "day" -> smoothTimeChange(world, 1000);
                    case "night" -> smoothTimeChange(world, 13000);
                }
            });
        }
    }

    /**
     * Smoothly transitions the world's time to the target time.
     *
     * @param world      the world to change time in
     * @param targetTime the target time (in ticks)
     */
    private void smoothTimeChange(World world, long targetTime) {
        if (world.getEnvironment() == World.Environment.NETHER || world.getEnvironment() == World.Environment.THE_END) {
            return;
        }
        AtomicLong currentTime = new AtomicLong(world.getTime());
        long totalTicksToAdd = (targetTime - currentTime.get() + 24000) % 24000;
        final long desiredTimeIncrement = 60;
        final long steps = totalTicksToAdd / desiredTimeIncrement;
        final long transitionDurationTicks = Math.max(1, steps);
        final long timeIncrement = Math.max(1, totalTicksToAdd / transitionDurationTicks);

        final Scheduler.Task[] taskHolder = new Scheduler.Task[1];
        taskHolder[0] = Scheduler.runTimer(() -> {
            long newTime = (currentTime.get() + timeIncrement) % 24000;
            currentTime.set(newTime);
            world.setTime(newTime);
            long ticksRemaining = (targetTime - currentTime.get() + 24000) % 24000;
            if (ticksRemaining <= 0 || ticksRemaining < timeIncrement) {
                world.setTime(targetTime);
                taskHolder[0].cancel();
            }
        }, 0L, 1L);
    }

    /**
     * Updates the action bar with current voting progress.
     */
    private void updateVoteProgress() {
        int progressBars = 14;
        double yesPercentage = yesVotes / (double) totalPlayersAtVoteStart;
        double noPercentage = noVotes / (double) totalPlayersAtVoteStart;

        int yesBars = (int) Math.round(yesPercentage * progressBars);
        int noBars = (int) Math.round(noPercentage * progressBars);
        int neutralBars = progressBars - yesBars - noBars;

        if (yesBars + noBars > progressBars) {
            int totalBars = yesBars + noBars;
            double scalingFactor = (double) progressBars / totalBars;
            yesBars = (int) Math.round(yesBars * scalingFactor);
            noBars = (int) Math.round(noBars * scalingFactor);
            neutralBars = progressBars - yesBars - noBars;
        }

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
        }, 0L, 20L);
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
            online.sendActionBar(Component.empty());
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
                Component.text()
                        .append(Component.text("Voting for ", NamedTextColor.GRAY))
                        .append(Component.text(capitalizedOption, NamedTextColor.GREEN, TextDecoration.BOLD))
                        .append(Component.text("! | ", NamedTextColor.GRAY))
                        .append(Component.text(votDurationSeconds + "s", NamedTextColor.AQUA))
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
     * Starts the countdown for the voting session.
     *
     * @param option the voting option
     */
    private void startVoteCountdown(String option) {
        String capitalizedOption = StringUtils.capitalize(option);
        AtomicLong timeLeft = new AtomicLong(votDurationSeconds);

        scheduledVoteEndTask = Scheduler.runTimer(() -> {
            float progress = timeLeft.get() / (float) votDurationSeconds;
            bossBar.progress(progress);
            long secsLeft = timeLeft.decrementAndGet();
            bossBar.name(Component.text()
                    .append(Component.text("Voting for ", NamedTextColor.GRAY))
                    .append(Component.text(capitalizedOption, NamedTextColor.GREEN, TextDecoration.BOLD))
                    .append(Component.text("! | ", NamedTextColor.GRAY))
                    .append(Component.text(secsLeft + "s", NamedTextColor.AQUA))
                    .build());
            if (secsLeft <= 0) {
                finalizeVote(false);
            }
        }, 20L, 20L);
    }

    /**
     * Displays the vote prompt to all non-AFK players with clickable text.
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
            // Only send the prompt to players who are not AFK and not the initiator.
            if (!online.equals(voteInitiator) && !AfkManager.getInstance().isAfk(online)) {
                online.sendMessage(message);
            }
        }
    }

    /**
     * Handles player joining (or coming out of AFK) during a vote.
     *
     * @param player the player who joined or became active
     */
    public void handlePlayerJoin(Player player) {
        if (voteInProgress && bossBar != null) {
            // Only add players who are active.
            if (!AfkManager.getInstance().isAfk(player)) {
                player.showBossBar(bossBar);
                totalPlayersAtVoteStart = (int) Bukkit.getOnlinePlayers().stream()
                        .filter(p -> !AfkManager.getInstance().isAfk(p))
                        .count();
                updateVoteProgress();
            }
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
            votedPlayers.remove(player.getUniqueId());
            totalPlayersAtVoteStart = (int) Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !AfkManager.getInstance().isAfk(p))
                    .count();
            updateVoteProgress();
            if (totalPlayersAtVoteStart == 0) {
                finalizeVote(true);
            }
        }
    }

    /**
     * Handles a player going AFK during an ongoing vote.
     * If the player hasn't voted yet, they are removed from the eligible vote pool.
     *
     * @param player the player who just became AFK.
     */
    public void handlePlayerAfk(Player player) {
        if (voteInProgress && bossBar != null) {
            if (!votedPlayers.contains(player.getUniqueId())) {
                totalPlayersAtVoteStart = (int) Bukkit.getOnlinePlayers().stream()
                        .filter(p -> !AfkManager.getInstance().isAfk(p))
                        .count();
                updateVoteProgress();
            }
        }
    }


    /**
     * Checks if a vote is currently in progress.
     *
     * @return true if a vote is in progress, false otherwise
     */
    public static boolean isVoteInProgress() {
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
