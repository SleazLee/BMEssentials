package at.sleazlee.bmessentials.votesystem;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import at.sleazlee.bmessentials.art.Art;
import at.sleazlee.bmessentials.PlayerData.PlayerDatabaseManager;
import at.sleazlee.bmessentials.PlayerData.PlayerDatabaseManager.VoteData;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Handles awarding vote rewards for the BMEssentials plugin.
 * <p>
 * This class no longer deals with Votifier events or vote storage.
 * It only provides methods to give vote rewards and a command to manually trigger them for testing.
 * </p>
 */
public class BMVote implements CommandExecutor, PluginMessageListener {

    private final BMEssentials plugin;
    private final VoteRewardCalculator rewardCalculator = new VoteRewardCalculator();

    /**
     * Constructs a new BMVote instance.
     *
     * @param plugin the BMEssentials plugin instance.
     */
    public BMVote(BMEssentials plugin) {
        this.plugin = plugin;
        // No event registrations here since vote handling is triggered via plugin messages from Velocity.
    }

    /**
     * Executes the vote command for manual testing.
     * <p>
     * Usage: /adminvote [playerName]
     * If no player is specified, the sender is used.
     * </p>
     *
     * @param sender  the command sender.
     * @param command the command.
     * @param label   the command label.
     * @param args    command arguments.
     * @return true if the command processed successfully.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            if (args.length > 0) {
                String otherPlayer = args[0];
                givePrize(otherPlayer);
            } else {
                sender.sendMessage("Too few args, try adding a player name!");
            }
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("bme.testvote")) {
            player.sendMessage(ChatColor.RED + "BM You do not have permission to use this command.");
            return true;
        }
        if (args.length > 0) {
            String otherPlayer = args[0];
            givePrize(otherPlayer);
        } else {
            givePrize(player.getName());
        }
        return true;
    }

    /**
     * Handles incoming plugin messages.
     * <p>
     * Expects a payload in the format "uuid;count". For each vote in the count,
     * the reward is awarded to the player corresponding to the given UUID.
     * </p>
     *
     * @param channel the channel name.
     * @param player  the player associated with the message.
     * @param message the message payload.
     */
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        // 0) Only our vote channel
        if (!"bmessentials:vote".equals(channel)) {
            return;
        }

        // 1) Decrypt the incoming bytes
        byte[] plain;
        try {
            plain = plugin.getAes().decrypt(message);
        } catch (Exception e) {
            plugin.getLogger().warning("Dropping malformed vote packet from " + player.getName());
            return;
        }

        // 2) Convert to UTF‑8 string
        String data = new String(plain, StandardCharsets.UTF_8);
        plugin.getLogger().info("BMVote: Decrypted payload from " + player.getName() + ": " + data);

        // 3) Split and validate format "uuid;count"
        String[] parts = data.split(";");
        if (parts.length != 2) {
            plugin.getLogger().warning("Invalid vote packet format: " + data);
            return;
        }

        String uuidString = parts[0];
        int voteCount;
        try {
            voteCount = Integer.parseInt(parts[1]);
        } catch (NumberFormatException ex) {
            plugin.getLogger().warning("Invalid vote count in packet: " + parts[1]);
            return;
        }

        // 4) Award the votes on the main thread
        Scheduler.run(() -> {
            for (int i = 0; i < voteCount; i++) {
                givePrizeByUUID(uuidString);
            }
        });
    }



    /**
     * Gives a prize to the player identified by the provided UUID string.
     *
     * @param uuidString the player's UUID as a string.
     */
    public void givePrizeByUUID(String uuidString) {
        UUID uuid = UUID.fromString(uuidString);
        Player target = Bukkit.getPlayer(uuid);
        if (target == null) {
            // Player is offline or not found.
            return;
        }
        awardVote(target);
    }

    public void givePrize(String playerName) {
        Player player = Bukkit.getPlayer(playerName);

        if (player != null) {
            awardVote(player);
        } else {
            System.out.println("Player " + playerName + " is currently offline. Prize could not be given.");
        }
    }

    /**
     * Pulls the player's vote metadata asynchronously, calculates their reward, then schedules the
     * visual/audio feedback back on the main thread. The heavy lifting is delegated to
     * {@link VoteRewardCalculator} so the odds are self-contained and easy to tweak.
     */
    private void awardVote(Player player) {
        UUID uuid = player.getUniqueId();
        Scheduler.runAsync(() -> {
            PlayerDatabaseManager database = plugin.getPlayerDataDBManager();
            if (database == null) {
                plugin.getLogger().warning("Vote reward skipped because the player data database was unavailable.");
                return;
            }

            VoteData data = database.getVoteData(uuid.toString());
            Instant now = Instant.now();

            // The calculator encapsulates how streak length modifies token odds and vote point bonuses.
            VoteRewardCalculator.Reward reward = rewardCalculator.calculate(
                    data.currentStreak(),
                    data.bestStreak(),
                    data.lastVoteAt(),
                    data.lastStreakIncrementAt(),
                    data.votesTowardsNextIncrement(),
                    data.totalVotes(),
                    now);

            database.updateVoteData(
                    uuid.toString(),
                    reward.currentStreak(),
                    reward.bestStreak(),
                    reward.lastVoteAt().toEpochMilli(),
                    reward.totalVotes(),
                    reward.votesTowardsNextIncrement(),
                    reward.lastStreakIncrementAt() != null ? reward.lastStreakIncrementAt().toEpochMilli() : 0);

            Scheduler.run(() -> deliverReward(uuid, reward));
        });
    }

    private void deliverReward(UUID uuid, VoteRewardCalculator.Reward reward) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }
        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        String playerName = player.getName();

        TokenPresentation presentation = TokenPresentation.from(reward.token());

        String votePointMessage = reward.votePoints() == 1 ? "§e1 VP" : "§e" + reward.votePoints() + " VPs";
        String streakDetails;
        if (reward.previousStreak() != reward.currentStreak()) {
            streakDetails = String.format("§7(§b%d§7 → §b%d§7)", reward.previousStreak(), reward.currentStreak());
        } else {
            streakDetails = String.format("§7[§b%d§7/§b%d§7]",
                    reward.votesTowardsNextIncrement(),
                    VoteRewardCalculator.VOTES_PER_INCREMENT);
        }

        player.sendMessage(String.format("§d§lVote §7Streak §b%d %s §f| %s §f& a%s§7§l Token§f!",
                reward.currentStreak(),
                streakDetails,
                votePointMessage,
                presentation.displayName));

        if (reward.streakIncremented()) {
            player.sendMessage(String.format("§aYou kept your streak alive! You're now on §b%d§a days.",
                    reward.currentStreak()));
        }

        Scheduler.run(() -> Bukkit.dispatchCommand(console,
                String.format("si give %skey 1 %s true", presentation.commandId, playerName)));
        Scheduler.run(() -> Bukkit.dispatchCommand(console,
                String.format("eco give %s %d VotePoints", playerName, reward.votePoints())));

        Location location = player.getLocation();
        player.getWorld().playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        spawnFallingParticleSphere(player, presentation.particleHex);
    }

    private enum TokenPresentation {
        HEALING(VoteRewardCalculator.Token.HEALING, "§x§3§2§C§A§6§5 §lHealing Springs", "healingsprings", "#32CA65"),
        WISHING(VoteRewardCalculator.Token.WISHING, "§x§3§2§C§A§F§C §lWishing-Well", "wishingwell", "#32CAFC"),
        OBELISK(VoteRewardCalculator.Token.OBELISK, "§x§C§A§6§5§0§0 §lObelisk", "obelisk", "#CA6500");

        private final VoteRewardCalculator.Token token;
        private final String displayName;
        private final String commandId;
        private final String particleHex;

        TokenPresentation(VoteRewardCalculator.Token token, String displayName, String commandId, String particleHex) {
            this.token = token;
            this.displayName = displayName;
            this.commandId = commandId;
            this.particleHex = particleHex;
        }

        static TokenPresentation from(VoteRewardCalculator.Token token) {
            for (TokenPresentation presentation : values()) {
                if (presentation.token == token) {
                    return presentation;
                }
            }
            return HEALING;
        }
    }

    /**
     * Spawns a falling particle sphere at the player's location.
     *
     * @param player  the player.
     * @param hexCode the hex color code for the particles.
     */
    public void spawnFallingParticleSphere(Player player, String hexCode) {
        Particle.DustOptions dustOptions = Art.createDustOptions(hexCode);
        double radius = plugin.getConfig().getDouble("Systems.VoteSystem.Particles.Radius");
        Location location = player.getLocation();

        // Create a new task for the repeating particle effect
        Scheduler.Task particleTask = Scheduler.runTimer(new Runnable() {
            double y = radius; // Start at the top of the sphere

            @Override
            public void run() {
                double sliceRadius = Math.sqrt(radius * radius - y * y); // Radius of the current slice
                for (double theta = 0; theta < 2 * Math.PI; theta += Math.PI / 16) {
                    double x = sliceRadius * Math.cos(theta);
                    double z = sliceRadius * Math.sin(theta);

                    // Add the offset to the player's location
                    Location particleLocation = location.clone().add(x, 0, z);

                    // Spawn the particle
                    particleLocation.getWorld().spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, dustOptions);
                }
                y -= radius / 8; // Lower the y-coordinate each tick
            }
        }, 0L, 1L); // Start immediately and repeat every tick (20 ticks = 1 second)

        // Schedule the cancellation after 3 seconds (60 ticks)
        Scheduler.runLater(() -> {
            // Check if the task is still running and cancel if so
            if (particleTask != null) {
                particleTask.cancel();
            }
        }, 60L); // Adjust the delay here to match your needs, 60 ticks for 3 seconds as per original requirement
    }
}