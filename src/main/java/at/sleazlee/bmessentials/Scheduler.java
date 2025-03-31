package at.sleazlee.bmessentials;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

import static org.bukkit.Bukkit.getLogger;

/**
 * Utility class for scheduling tasks, with support for Folia.
 * <p>
 * This class provides methods for both global scheduling and region-specific scheduling
 * (i.e. tasks that require a proper world context). In a Folia environment the appropriate
 * scheduler is used to ensure that world-related operations (like retrieving block states)
 * have a valid context.
 * </p>
 */
public final class Scheduler {

    private static final boolean isFolia;
    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            folia = true;
            getLogger().warning("TREEFELLER You are running FOLIA");
        } catch (ClassNotFoundException e) {
            folia = false;
            getLogger().warning("TREEFELLER You are running BUKKIT");
        }
        isFolia = folia;
    }

    /**
     * Runs a task immediately using a global scheduler.
     *
     * @param runnable The task to run.
     */
    public static void run(Runnable runnable) {
        if (isFolia)
            Bukkit.getGlobalRegionScheduler().execute(BMEssentials.getInstance(), runnable);
        else
            Bukkit.getScheduler().runTask(BMEssentials.getInstance(), runnable);
    }

    /**
     * Schedules a task to run after a delay using a global scheduler.
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
     * Schedules a repeating task using a global scheduler.
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

    // -------------------------------------------------------------------
    // REGION-BASED SCHEDULING METHODS
    // Use these methods when a task requires a proper world/region context,
    // such as when accessing block data.
    // -------------------------------------------------------------------

    /**
     * Runs a task immediately using a region-based scheduler.
     * The task is scheduled with the given location to ensure proper world context.
     *
     * @param location The location used to determine the region context.
     * @param runnable The task to run.
     */
    public static void run(Location location, Runnable runnable) {
        if (isFolia)
            Bukkit.getRegionScheduler().execute(BMEssentials.getInstance(), location, runnable);
        else
            Bukkit.getScheduler().runTask(BMEssentials.getInstance(), runnable);
    }

    /**
     * Schedules a task to run after a delay using a region-based scheduler.
     * The task is scheduled with the given location to ensure proper world context.
     *
     * @param location   The location used to determine the region context.
     * @param runnable   The task to run.
     * @param delayTicks The delay in ticks before running the task.
     * @return The scheduled task.
     */
    public static Task runLater(Location location, Runnable runnable, long delayTicks) {
        if (isFolia)
            return new Task(Bukkit.getRegionScheduler().runDelayed(BMEssentials.getInstance(), location, t -> runnable.run(), delayTicks));
        else
            return new Task(Bukkit.getScheduler().runTaskLater(BMEssentials.getInstance(), runnable, delayTicks));
    }

    /**
     * Schedules a repeating task using a region-based scheduler.
     * The task is scheduled with the given location to ensure proper world context.
     *
     * @param location    The location used to determine the region context.
     * @param runnable    The task to run.
     * @param delayTicks  The initial delay in ticks before running the task.
     * @param periodTicks The period in ticks between successive runs.
     * @return The scheduled task.
     */
    public static Task runTimer(Location location, Runnable runnable, long delayTicks, long periodTicks) {
        if (isFolia)
            return new Task(Bukkit.getRegionScheduler().runAtFixedRate(BMEssentials.getInstance(), location, t -> runnable.run(), delayTicks < 1 ? 1 : delayTicks, periodTicks));
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
     * Cancels the current task.
     * (Implementation can be added if needed)
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