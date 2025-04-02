package at.sleazlee.bmessentials;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

/**
 * Utility class for scheduling tasks, with support for Folia.
 */
public final class Scheduler {

    private static final boolean isFolia;
    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        isFolia = folia;
    }

    /**
     * Runs a task immediately.
     *
     * @param runnable The task to run.
     */
    public static void run(Runnable runnable) {
        if (isFolia)
            Bukkit.getGlobalRegionScheduler()
                    .execute(BMEssentials.getInstance(), runnable);

        else
            Bukkit.getScheduler().runTask(BMEssentials.getInstance(), runnable);
    }

    /**
     * Schedules a task to run after a delay.
     *
     * @param runnable   The task to run.
     * @param delayTicks The delay in ticks before running the task.
     * @return The scheduled task.
     */
    public static Task runLater(Runnable runnable, long delayTicks) {
        if (isFolia)
            return new Task(Bukkit.getGlobalRegionScheduler()
                    .runDelayed(BMEssentials.getInstance(), t -> runnable.run(), delayTicks));

        else
            return new Task(Bukkit.getScheduler().runTaskLater(BMEssentials.getInstance(), runnable, delayTicks));
    }

    /**
     * Schedules a repeating task.
     *
     * @param runnable    The task to run.
     * @param delayTicks  The initial delay in ticks before running the task.
     * @param periodTicks The period in ticks between successive runs.
     * @return The scheduled task.
     */
    public static Task runTimer(Runnable runnable, long delayTicks, long periodTicks) {
        if (isFolia)
            return new Task(Bukkit.getGlobalRegionScheduler()
                    .runAtFixedRate(BMEssentials.getInstance(), t -> runnable.run(), delayTicks < 1 ? 1 : delayTicks, periodTicks));
        else
            return new Task(Bukkit.getScheduler().runTaskTimer(BMEssentials.getInstance(), runnable, delayTicks, periodTicks));
    }


    /**
     * Checks if the server is running Folia.
     *
     * @return true if Folia is detected, false otherwise.
     */
    public static boolean isFolia() {
        return isFolia;
    }

    /**
     * Cancels the current task. (Implementation can be added if needed)
     */
    public static void cancelCurrentTask() {
    }

    /**
     * Wrapper class for scheduled tasks.
     */
    public static class Task {

        private Object foliaTask;
        private BukkitTask bukkitTask;

        /**
         * Constructor for Folia tasks.
         *
         * @param foliaTask The Folia scheduled task.
         */
        Task(Object foliaTask) {
            this.foliaTask = foliaTask;
        }

        /**
         * Constructor for Bukkit tasks.
         *
         * @param bukkitTask The Bukkit scheduled task.
         */
        Task(BukkitTask bukkitTask) {
            this.bukkitTask = bukkitTask;
        }

        /**
         * Cancels the scheduled task.
         */
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