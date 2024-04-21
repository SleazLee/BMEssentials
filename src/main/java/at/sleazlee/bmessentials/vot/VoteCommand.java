package at.sleazlee.bmessentials.vot;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

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
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "Usage: /" + label + " <day|night|clear|rain|thunder|yes|no>"));
            return true;
        }

        String option = args[0].toLowerCase();
        boolean isAdminVote = label.equalsIgnoreCase("adminvot");

        if (isAdminVote && !player.hasPermission("bmessentials.adminvot")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (option.equals("yes") || option.equals("no")) {
            handleVote(option, player);
            return true;
        }

        if (!isAdminVote && voteInProgress) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "A vote is already in progress."));
            return true;
        }

        if (!isAdminVote && (System.currentTimeMillis() - lastVoteTime < 15 * 60 * 1000) && !isAdminVote) {
            String prefix = translateHexColorCodes("Vot", "#ff3300", true);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&c A global cooldown is currently in effect."));
            return true;
        }

        if (Set.of("day", "night", "clear", "rain", "thunder").contains(option)) {
            String capitalizedOption = option.substring(0, 1).toUpperCase() + option.substring(1);
            startVote(capitalizedOption, player, isAdminVote);
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cInvalid option. Usage: /" + label + " <day|night|clear|rain|thunder>"));
        }

        return true;
    }


    private void handleVote(String option, Player player) {
        if (!voteInProgress) {
            String prefix = translateHexColorCodes("Vot", "#ff3300", true);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&c There is no vote in progress."));
            return;
        }
        if (votedPlayers.contains(player)) {
            String prefix = translateHexColorCodes("Vot", "#ff3300", true);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " &cYou have already voted."));
            return;
        }

        if (option.equals("yes")) {
            yesVotes++;
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2&lVot &7You voted &a&lYES&7."));
        } else {
            noVotes++;
            String no = translateHexColorCodes("NO", "#ff3300", true);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2&lVot &7You voted " + no + "&7!"));
        }
        votedPlayers.add(player);
        updateVoteProgress();

        // Check if all online players have voted or if the only player online has voted
        if (votedPlayers.size() >= Bukkit.getOnlinePlayers().size()) {
            finalizeVote();
        } else if (Bukkit.getOnlinePlayers().size() == 1 && votedPlayers.size() == 1) {
            // If the only player on the server has voted
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
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&2&lVot &aSuccessful!&7 The Round for &b" + voteOption + "&7 is complete, changing now!"));
            String noVoteCount = translateHexColorCodes(String.valueOf(noVotes), "#ff3300", true);
            String resultMessage = ChatColor.translateAlternateColorCodes('&', "&2&lVot &7Result Ratio: &a&l" + yesVotes + " &8| " + noVoteCount);
            Bukkit.broadcastMessage(resultMessage);
            executeChange(voteOption);
        } else {
            String prefix = translateHexColorCodes("Vot", "#ff3300", true);
            String noVoteCount = translateHexColorCodes(String.valueOf(noVotes), "#ff3300", true);
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&c Unsuccessful!&7 The Round for &b" + voteOption + "&7 has failed!"));
            String resultMessage = ChatColor.translateAlternateColorCodes('&', prefix + "&7 Result Ratio: &a&l" + yesVotes + " &8| " + noVoteCount);
            Bukkit.broadcastMessage(resultMessage);
        }

        startCooldown();
        votedPlayers.clear();

        // Also cancel the scheduled task here for good measure
        if (scheduledVoteEndTask != null) {
            scheduledVoteEndTask.cancel();
        }
    }

    private void startVote(String option, Player initiator, boolean bypassCooldown) {
        if (!bypassCooldown && (System.currentTimeMillis() - lastVoteTime < 15 * 60 * 1000)) {
            String prefix = translateHexColorCodes("Vot", "#ff3300", true);
            initiator.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "A global cooldown is currently in effect."));
            return;
        }

        lastVoteTime = System.currentTimeMillis();  // Reset the cooldown timer if not bypassed
        voteInProgress = true;
        voteOption = option.toLowerCase();
        yesVotes = 0; // Initiator votes yes by default
        noVotes = 0;
        votedPlayers.clear();

        String startMessage = ChatColor.translateAlternateColorCodes('&', "&b" + initiator.getName() + " &7would like to change it to &b" + option + " &7in the world.");
        Bukkit.broadcastMessage(startMessage);

        // Send clickable options for voting
        TextComponent message = new TextComponent(ChatColor.translateAlternateColorCodes('&', "&7Click an option: "));
        TextComponent accept = new TextComponent(ChatColor.translateAlternateColorCodes('&', "&a&lAccept"));
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/vot yes"));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.translateAlternateColorCodes('&', "&aClick to vote Yes for " + option + "."))));
        TextComponent slash = new TextComponent(ChatColor.translateAlternateColorCodes('&', " &8/ "));

        // Now applying custom color and bold formatting
        String declineHoverMessage = "Click to vote No for " + option + ".";
        TextComponent decline = new TextComponent("Decline");
        decline.setColor(net.md_5.bungee.api.ChatColor.of("#ff3300"));
        decline.setBold(true);
        decline.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/vot no"));
        decline.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(declineHoverMessage)));

        message.addExtra(accept);
        message.addExtra(slash);
        message.addExtra(decline);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.spigot().sendMessage(message);
        }


        // Initialize the BossBar for voting
        if (bossBar != null) {
            bossBar.removeAll();
        }
        bossBar = Bukkit.createBossBar(ChatColor.translateAlternateColorCodes('&', "&7&lPlace vote for &e&l" + option + "&7&l! &8| &7&lEnds in &e&l60s"), BarColor.BLUE, BarStyle.SEGMENTED_20);
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
            bossBar.setTitle(ChatColor.translateAlternateColorCodes('&', "&7&lPlace vote for &e&l" + option + "&7&l! &8| &7&lEnds in &e&l" + secsLeft + "s"));

            if (currentProgress <= 0) {
                finalizeVote();  // Automatically finalize vote when boss bar is empty
            }
        }, 20L, 20L); // Update every second (20 ticks per second)

        // Start updating the action bar immediately
        updateVoteProgress();
        handleVote("yes", initiator.getPlayer());
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

    public String translateHexColorCodes(String message, String colorCode, boolean isBold) {
        StringBuilder translated = new StringBuilder("§x");
        for (char ch : colorCode.substring(1).toCharArray()) {  // Skip '#'
            translated.append("§").append(ch);
        }
        // Add bold formatting if required
        if (isBold) {
            translated.append("§l");
        }
        translated.append(message);
        return translated.toString();
    }


}
