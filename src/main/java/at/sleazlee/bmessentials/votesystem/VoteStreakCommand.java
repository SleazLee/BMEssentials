package at.sleazlee.bmessentials.votesystem;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.PlayerData.PlayerDatabaseManager;
import at.sleazlee.bmessentials.PlayerData.PlayerDatabaseManager.VoteData;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Provides the `/vote streak` informational command so players can check their current streak,
 * remaining progress toward the next day, and the token odds that currently apply to their votes.
 */
public class VoteStreakCommand implements CommandExecutor, TabCompleter {

    private final BMEssentials plugin;
    private final VoteRewardCalculator calculator = new VoteRewardCalculator();

    public VoteStreakCommand(BMEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("streak")) {
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Use /" + label + " streak to view your vote streak details.");
            return true;
        }

        PlayerDatabaseManager database = plugin.getPlayerDataDBManager();
        if (database == null) {
            player.sendMessage(ChatColor.RED + "Vote data is temporarily unavailable. Please try again shortly.");
            return true;
        }

        VoteData data = database.getVoteData(player.getUniqueId().toString());
        Instant now = Instant.now();
        VoteRewardCalculator.StreakSnapshot snapshot = calculator.preview(
                data.currentStreak(),
                data.bestStreak(),
                data.lastVoteAt(),
                data.lastStreakIncrementAt(),
                data.votesTowardsNextIncrement(),
                now);

        VoteRewardCalculator.TokenOdds odds = calculator.describeOddsForStreak(snapshot.effectiveStreak());
        double healingPct = odds.healing() * 100.0;
        double wishingPct = odds.wishing() * 100.0;
        double obeliskPct = odds.obelisk() * 100.0;

        Duration cooldown = snapshot.timeUntilNextEligible();
        String cooldownText = cooldown.isZero()
                ? ChatColor.GREEN + "ready now"
                : ChatColor.YELLOW + formatDuration(cooldown) + ChatColor.GRAY + " remaining";

        player.sendMessage(ChatColor.LIGHT_PURPLE + "Vote Streak" + ChatColor.GRAY + ": " + ChatColor.AQUA + snapshot.currentStreak()
                + ChatColor.GRAY + " (Best " + ChatColor.AQUA + data.bestStreak() + ChatColor.GRAY + ")");
        player.sendMessage(ChatColor.GRAY + "Progress: " + ChatColor.AQUA + snapshot.votesTowardsNextIncrement()
                + ChatColor.GRAY + "/" + ChatColor.AQUA + snapshot.votesRequired()
                + ChatColor.GRAY + " votes | Next increment " + cooldownText);
        player.sendMessage(ChatColor.GRAY + "Token odds → "
                + ChatColor.AQUA + formatPercentage(healingPct) + ChatColor.GRAY + " Healing, "
                + ChatColor.AQUA + formatPercentage(wishingPct) + ChatColor.GRAY + " Wishing, "
                + ChatColor.AQUA + formatPercentage(obeliskPct) + ChatColor.GRAY + " Obelisk");
        return true;
    }

    private String formatPercentage(double value) {
        return String.format(Locale.US, "%.1f%%", value);
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours > 0 && minutes > 0) {
            return hours + "h " + minutes + "m";
        }
        if (hours > 0) {
            return hours + "h";
        }
        if (minutes > 0) {
            return minutes + "m";
        }
        long remainingSeconds = seconds % 60;
        if (remainingSeconds > 0) {
            return remainingSeconds + "s";
        }
        return "0s";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Collections.singletonList("streak");
        }
        return Collections.emptyList();
    }
}
