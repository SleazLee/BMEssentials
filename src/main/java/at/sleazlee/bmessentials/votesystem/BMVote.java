package at.sleazlee.bmessentials.votesystem;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import at.sleazlee.bmessentials.art.Art;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Slab;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
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
        Player target = Bukkit.getPlayer(UUID.fromString(uuidString));
        if (target == null) {
            // Player is offline or not found.
            return;
        }
        givePrize(target.getName());
    }

    /**
     * Randomly selects a reward key.
     *
     * @return a string representing the reward key.
     */
    public String randomKey() {
        double randomValue = Math.random();

        if (randomValue < 0.15) {
            // 15% chance, obelisk
            return "obelisk";
        } else if (randomValue < 0.45) {
            // 30% chance, wishingwell
            return "wishingwell";
        } else if (randomValue < 0.85) {
            // 40% chance, healingsprings
            return "healingsprings";
        } else {
            // 15% chance, Nothing
            return "nothing";
        }
    }

    public void givePrize(String playerName) {
        Player player = Bukkit.getPlayer(playerName);

        // Check that the player is not null (i.e., they're online)
        if (player != null) {
            // Get a console command sender
            ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();

            // Give the player their random reward
            String commandOne = "";
            String commandTwo = "";
            String hexCode = "#0dc6ff";

            switch (randomKey()) {
                case "obelisk":
                    player.sendMessage("§d§lVote §fYou just received §e1 VP§f & a§x§C§A§6§5§0§0 §lObelisk§7§l Token§f!");
                    commandOne = "si give obeliskkey 1 " + playerName + " true";
                    commandTwo = "eco give " + playerName + " 1 VotePoints";
                    hexCode = "#CA6500";
                    break;
                case "wishingwell":
                    player.sendMessage("§d§lVote §fYou just received §e1 VP§f & a§x§3§2§C§A§F§C §lWishing-Well§7§l Token§f!");
                    commandOne = "si give wishingwellkey 1 " + playerName + " true";
                    commandTwo = "eco give " + playerName + " 1 VotePoints";
                    hexCode = "#32CAFC";
                    break;
                case "healingsprings":
                    player.sendMessage("§d§lVote §fYou just received §e1 VP§f & a§x§3§2§C§A§6§5 §lHealing Springs§7§l Token§f!");
                    commandOne = "si give healingspringskey 1 " + playerName + " true";
                    commandTwo = "eco give " + playerName + " 1 VotePoints";
                    hexCode = "#32CA65";
                    break;
                default:
                    player.sendMessage("§d§lVote §fYou just received §e2 VPs§f!");
                    commandOne = "none";
                    commandTwo = "eco give " + playerName + " 2 VotePoints";
                    hexCode = "#AAAAAA";
                    break;
            }


            // Execute the commands
            if (!commandOne.equals("none")) {
                String finalCommandOne = commandOne;
                Scheduler.run(() -> Bukkit.dispatchCommand(console, finalCommandOne));
            }
            String finalCommandTwo = commandTwo;
            Scheduler.run(() -> Bukkit.dispatchCommand(console, finalCommandTwo));

            // Play a sound and spawn particles
            if (player != null) {
                Location location = player.getLocation();

                // play the vote sound
                player.getWorld().playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

                // spawn a particle
                spawnFallingParticleSphere(player, hexCode);

            }





        } else {
            // If the player is offline, you could log a message to the console or handle it in some other way
            System.out.println("Player " + playerName + " is currently offline. Prize could not be given.");
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