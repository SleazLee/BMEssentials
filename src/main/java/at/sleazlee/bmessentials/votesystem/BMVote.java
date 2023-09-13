package at.sleazlee.bmessentials.votesystem;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.art.Art;
import com.vexsoftware.votifier.model.VotifierEvent;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;

public class BMVote implements Listener, CommandExecutor {
    private final BMEssentials plugin;
    private FileConfiguration votesConfig;
    private File votesFile;

    public BMVote(BMEssentials plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
        createVotesFile();
        cleanOldVotes();
    }

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

    private void createVotesFile() {
        // Create the plugin data folder if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // Create a File object for the votes file
        votesFile = new File(plugin.getDataFolder(), "vote.yml");

        // Copy the default vote.yml to the data folder if it doesn't exist
        if (!votesFile.exists()) {
            try (InputStream resource = plugin.getResource("vote.yml")) {
                if (resource != null) {
                    Files.copy(resource, votesFile.toPath());
                } else {
                    plugin.getLogger().warning("Default vote.yml not found in resources!");
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to copy default vote.yml", e);
            }
        }

        // Load the file into a FileConfiguration object so it can be used
        votesConfig = YamlConfiguration.loadConfiguration(votesFile);
    }

    private void cleanOldVotes() {
        // Iterate over all stored votes
        for (String key : votesConfig.getKeys(false)) {
            // Retrieve the timestamp of the vote
            Date voteDate = (Date) votesConfig.get(key + ".timestamp");

            // If the voteDate is not null and the vote is older than 30 days, remove it
            if (voteDate != null && Date.from(Instant.now().minusSeconds(60 * 60 * 24 * 30)).after(voteDate)) {
                votesConfig.set(key, null);
            }
        }

        // Save any changes to the file
        try {
            votesConfig.save(votesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @EventHandler
    public void onVotifierEvent(VotifierEvent event) {
        // Get the name of the player who voted
        String playerName = event.getVote().getUsername();

        // Get the UUID of the player
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        UUID playerUUID = offlinePlayer.getUniqueId();

        // Convert UUID to String for storage
        String uuidString = playerUUID.toString();

        // Check if the player is currently online
        Player player = offlinePlayer.getPlayer();

        if (player != null) { // Player is online

            // gives the player their prize
            givePrize(playerName);

        } else { // Player is offline
            // Get the current number of votes for the player, or 0 if they have no votes
            int voteCount = votesConfig.getInt(uuidString + ".count", 0);

            // Increment the vote count
            votesConfig.set(uuidString + ".count", voteCount + 1);

            // Store the current date and time as the timestamp for the vote
            votesConfig.set(uuidString + ".timestamp", Date.from(Instant.now()));

            // Save any changes to the file
            try {
                votesConfig.save(votesFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Get the player who joined
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Convert UUID to String for checking
        String uuidString = playerUUID.toString();

        // Check if the player has a stored vote
        if (votesConfig.contains(uuidString)) {
            // Get the timestamp of the vote
            Date voteDate = (Date) votesConfig.get(uuidString + ".timestamp");

            // Get the number of votes
            int voteCount = votesConfig.getInt(uuidString + ".count", 0);

            // Check if the vote is less than 30 days old
            if (Date.from(Instant.now().minusSeconds(60 * 60 * 24 * 30)).before(voteDate)) {

                // Execute the reward commands
                for (int i = 0; i < voteCount; i++) {

                    Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                        @Override
                        public void run() {
                            givePrize(player.getName());
                        }
                    }, 40L); // Delay in ticks, 20 ticks = 1 second

                }
            }

            // Remove the vote from the config
            votesConfig.set(uuidString, null);
            try {
                votesConfig.save(votesFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String randomKey() {
        double randomValue = Math.random();

        if (randomValue < 0.10) {
            // 10% chance, obelisk
            return "obelisk";
        } else if (randomValue < 0.32) {
            // 22% chance, wishingwell
            return "wishingwell";
        } else if (randomValue < 0.74) {
            // 32% chance, healingsprings
            return "healingsprings";
        } else {
            // 26% chance, healingsprings
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
                    commandOne = "si give obeliskkey 1 " + playerName;
                    commandTwo = "eco give " + playerName + " 1 VP";
                    hexCode = "#CA6500";
                    break;
                case "wishingwell":
                    player.sendMessage("§d§lVote §fYou just received §e1 VP§f & a§x§3§2§C§A§F§C §lWishing-Well§7§l Token§f!");
                    commandOne = "si give wishingwellkey 1 " + playerName;
                    commandTwo = "eco give " + playerName + " 1 VP";
                    hexCode = "#32CAFC";
                    break;
                case "healingsprings":
                    player.sendMessage("§d§lVote §fYou just received §e1 VP§f & a§x§3§2§C§A§6§5 §lHealing Springs§7§l Token§f!");
                    commandOne = "si give healingspringskey 1 " + playerName;
                    commandTwo = "eco give " + playerName + " 1 VP";
                    hexCode = "#32CA65";
                    break;
                default:
                    player.sendMessage("§d§lVote §fYou just received §e2 VPs§f!");
                    commandOne = "none";
                    commandTwo = "eco give " + playerName + " 2 VPs";
                    hexCode = "#AAAAAA";
                    break;
            }


            // Execute the commands
            if (!commandOne.equals("none")) {
                Bukkit.dispatchCommand(console, commandOne);
            }
            Bukkit.dispatchCommand(console, commandTwo);

            // Play a sound and spawn particles
            if (player != null) {
                Location location = player.getLocation();

                // play a sound
                String soundName = plugin.getConfig().getString("systems.votesystem.sounds");

                try {
                    Sound sound = Sound.valueOf(soundName); // Convert the string to a Sound
                    player.getWorld().playSound(location, sound, 1f, 1f); // Play the sound
                } catch (IllegalArgumentException e) {
                    // If the soundName is not a valid Sound, this exception will be thrown.
                    // You can add your error handling code here. For example:
                    System.err.println("Invalid sound name in config: " + soundName);
                }

                // spawn a particle
                spawnFallingParticleSphere(player, hexCode);

            }





        } else {
            // If the player is offline, you could log a message to the console or handle it in some other way
            System.out.println("Player " + playerName + " is currently offline. Prize could not be given.");
        }
    }

    public void spawnFallingParticleSphere(Player player, String hexCode) {
        Particle.DustOptions dustOptions = Art.createDustOptions(hexCode);
        double radius = plugin.getConfig().getDouble("systems.votesystem.particles.radius");
        Location location = player.getLocation();

        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        int taskId = scheduler.scheduleSyncRepeatingTask(plugin, new Runnable() {
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
                    particleLocation.getWorld().spawnParticle(Particle.REDSTONE, particleLocation, 0, dustOptions);
                }
                y -= radius / (8); // Lower the y-coordinate each tick
            }
        }, 0L, 1L); // Start immediately and repeat every tick (20 ticks = 1 second)

        // Cancel the task after 3 seconds (60 ticks)
        scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                scheduler.cancelTask(taskId);
            }
        }, 20L);
    }



}
