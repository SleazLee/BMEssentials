package at.sleazlee.bmessentials.vot;

import at.sleazlee.bmessentials.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

import static at.sleazlee.bmessentials.Scheduler.runLater;

public class vot extends JavaPlugin {
    private static Set<Player> votedPlayers = new HashSet<>();
    private static boolean voteInProgress = false;
    private static int yesVotes = 0;
    private static int noVotes = 0;
    private static String voteOption;

    public static class VoteCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be executed by players.");
                return true;
            }

            Player player = (Player) sender;

            if (args.length != 1) {
                player.sendMessage(ChatColor.RED + "Usage: /vot <day|night|clear|rain|thunder>");
                return true;
            }

            if (voteInProgress) {
                player.sendMessage(ChatColor.RED + "A vote is already in progress.");
                return true;
            }

            String option = args[0].toLowerCase();

            if (option.equals("day") || option.equals("night") || option.equals("clear") || option.equals("rain") || option.equals("thunder")) {
                startVote(option);
            } else {
                player.sendMessage(ChatColor.RED + "Invalid option. Usage: /vot <day|night|clear|rain|thunder>");
            }

            return true;
        }
    }

    private static void startVote(String option) {
        voteInProgress = true;
        voteOption = option;
        yesVotes = 0;
        noVotes = 0;
        votedPlayers.clear();

        Bukkit.broadcastMessage(ChatColor.GREEN + "A vote to change " + option + " has started! Vote yes or no with /vot yes or /vot no.");

        Scheduler.runLater(() -> {
            voteInProgress = false;

            if (yesVotes > noVotes) {
                Bukkit.broadcastMessage(ChatColor.GREEN + "The vote to change " + voteOption + " has passed. Changing now...");
                executeChange(voteOption);
            } else {
                Bukkit.broadcastMessage(ChatColor.RED + "The vote to change " + voteOption + " has failed. No change will occur.");
            }

            // Start cooldown for all players
            startCooldown();

            // Clear voted players
            votedPlayers.clear();
        }, 20 * 60); // 60 seconds
    }

    private static void executeChange(String option) {
        for (World world : Bukkit.getWorlds()) {
            if (option.equals("day")) {
                // Add ticks to move closer to day
                long time = world.getTime();
                long ticksToAdd = (24000 - (time % 24000)) % 24000;
                world.setTime(time + ticksToAdd);
            } else if (option.equals("night")) {
                // Add ticks to move closer to night
                long time = world.getTime();
                long ticksToAdd = (14000 - (time % 24000)) % 24000;
                world.setTime(time + ticksToAdd);
            } else if (option.equals("clear")) {
                // Set weather to clear
                world.setStorm(false);
                world.setThundering(false);
            } else if (option.equals("rain")) {
                // Set weather to rain
                world.setStorm(true);
                world.setThundering(false);
            } else if (option.equals("thunder")) {
                // Set weather to thunderstorm
                world.setStorm(true);
                world.setThundering(true);
            }
        }
    }

    private static void startCooldown() {
        // Start cooldown for all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(ChatColor.YELLOW + "Vote cooldown started.");
            votedPlayers.add(player);
        }
    }
}

