package at.sleazlee.bmessentials;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import at.sleazlee.bmessentials.BMEssentials;

public final class Scheduler {

    private static final boolean isFolia = Bukkit.getVersion().contains("Folia");

    public static void run(Runnable runnable) {
        if (isFolia)
            Bukkit.getGlobalRegionScheduler()
                    .execute(BMEssentials.getInstance(), runnable);

        else
            Bukkit.getScheduler().runTask(BMEssentials.getInstance(), runnable);
    }

    public static Task runLater(Runnable runnable, long delayTicks) {
        if (isFolia)
            return new Task(Bukkit.getGlobalRegionScheduler()
                    .runDelayed(BMEssentials.getInstance(), t -> runnable.run(), delayTicks));

        else
            return new Task(Bukkit.getScheduler().runTaskLater(BMEssentials.getInstance(), runnable, delayTicks));
    }

    public static Task runTimer(Runnable runnable, long delayTicks, long periodTicks) {
        if (isFolia)
            return new Task(Bukkit.getGlobalRegionScheduler()
                    .runAtFixedRate(BMEssentials.getInstance(), t -> runnable.run(), delayTicks < 1 ? 1 : delayTicks, periodTicks));

        else
            return new Task(Bukkit.getScheduler().runTaskTimer(BMEssentials.getInstance(), runnable, delayTicks, periodTicks));
    }

    public static boolean isFolia() {
        return isFolia;
    }

    public static void cancelCurrentTask() {
    }

    public static class Task {

        private Object foliaTask;
        private BukkitTask bukkitTask;

        Task(Object foliaTask) {
            this.foliaTask = foliaTask;
        }

        Task(BukkitTask bukkitTask) {
            this.bukkitTask = bukkitTask;
        }

        public void cancel() {
            if (foliaTask != null) {
                ((ScheduledTask) foliaTask).cancel();
            } else if (bukkitTask != null) {
                bukkitTask.cancel();
            }
        }
    }
}

// if BukkitRunnable:
//Looks like this:

//        new BukkitRunnable() {
//      @Override
//       public void run() {
//        voteInProgress = false;
//
//        if (yesVotes > noVotes) {
//        Bukkit.broadcastMessage(ChatColor.GREEN + "The vote to change " + voteOption + " has passed. Changing now...");
//        executeChange(voteOption);
//        } else {
//        Bukkit.broadcastMessage(ChatColor.RED + "The vote to change " + voteOption + " has failed. No change will occur.");
//        }
//
//        // Start cooldown for all players
//        startCooldown();
//
//        // Clear voted players
//        votedPlayers.clear();
//        }
//        }.runTaskLater(this, 60 * 20); // 60 seconds
//        }

//Translate to this:

//      runLater(() -> {
//        voteInProgress = false;
//
//        if (yesVotes > noVotes) {
//        Bukkit.broadcastMessage(ChatColor.GREEN + "The vote to change " + voteOption + " has passed. Changing now...");
//        executeChange(voteOption);
//        } else {
//        Bukkit.broadcastMessage(ChatColor.RED + "The vote to change " + voteOption + " has failed. No change will occur.");
//        }
//
//        // Start cooldown for all players
//        startCooldown();
//
//        // Clear voted players
//        votedPlayers.clear();
//        }, 20 * 60); // 60 seconds
//        }