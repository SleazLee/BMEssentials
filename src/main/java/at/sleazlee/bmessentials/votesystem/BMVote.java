package at.sleazlee.bmessentials.votesystem;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import at.sleazlee.bmessentials.art.Art;
import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Receives Velocity vote rewards and executes them on the Spigot server.
 */
public class BMVote implements PluginMessageListener {

    private static final Gson GSON = new Gson();

    private final BMEssentials plugin;

    public BMVote(BMEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!"bmessentials:vote".equals(channel)) {
            return;
        }

        byte[] plain;
        try {
            plain = plugin.getAes().decrypt(message);
        } catch (Exception e) {
            plugin.getLogger().warning("Dropping malformed vote packet from " + player.getName());
            return;
        }

        String data = new String(plain, StandardCharsets.UTF_8);
        RewardEnvelope envelope;
        try {
            envelope = GSON.fromJson(data, RewardEnvelope.class);
        } catch (Exception ex) {
            plugin.getLogger().warning("Invalid vote payload received: " + data);
            return;
        }

        if (envelope == null || envelope.reward == null || envelope.uuid == null) {
            plugin.getLogger().warning("Vote payload missing required fields: " + data);
            return;
        }

        Scheduler.run(() -> deliverReward(envelope));
    }

    private void deliverReward(RewardEnvelope envelope) {
        Player target = Bukkit.getPlayer(envelope.uuid);
        if (target == null) {
            plugin.getLogger().warning("Unable to deliver vote reward because player is offline: " + envelope.uuid);
            return;
        }

        Reward reward = envelope.reward;
        executeReward(target, reward);
    }

    private void executeReward(Player player, Reward reward) {
        TokenDescriptor tokenDescriptor = describeToken(reward.token(), player.getName());

        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        if (tokenDescriptor != null && tokenDescriptor.command() != null) {
            String command = tokenDescriptor.command();
            Scheduler.run(() -> Bukkit.dispatchCommand(console, command));
        }

        String votePointCommand = "eco give " + player.getName() + " " + reward.vp() + " VotePoints";
        Scheduler.run(() -> Bukkit.dispatchCommand(console, votePointCommand));

        sendRewardMessages(player, reward, tokenDescriptor);
        playCelebration(player, tokenDescriptor != null ? tokenDescriptor.hexColor() : "#AAAAAA");
    }

    private void sendRewardMessages(Player player, Reward reward, TokenDescriptor descriptor) {
        StringBuilder base = new StringBuilder("§d§lVote §fYou just received §e")
                .append(reward.vp())
                .append(" VP");
        if (reward.vp() != 1) {
            base.append("s");
        }
        if (descriptor != null) {
            base.append("§f &").append(descriptor.display());
        } else {
            base.append("§f!");
        }
        player.sendMessage(base.toString());

        if (reward.streak() > 1) {
            if (reward.streakIncremented()) {
                player.sendMessage("§d§lVote §fYou are on a §e" + reward.streak() + "§f day streak!");
            } else {
                player.sendMessage("§d§lVote §fYour streak has been reset to §e" + reward.streak() + "§f day" + (reward.streak() == 1 ? "" : "s") + ".");
            }
        }
    }

    private void playCelebration(Player player, String hexCode) {
        Location location = player.getLocation();
        player.getWorld().playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        spawnFallingParticleSphere(player, hexCode);
    }

    private TokenDescriptor describeToken(String token, String playerName) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return switch (token.toUpperCase()) {
            case "OBELISK" -> new TokenDescriptor(
                    "si give obeliskkey 1 " + playerName + " true",
                    "§x§C§A§6§5§0§0 §lObelisk§7§l Token§f!",
                    "#CA6500"
            );
            case "WISHING" -> new TokenDescriptor(
                    "si give wishingwellkey 1 " + playerName + " true",
                    "§x§3§2§C§A§F§C §lWishing-Well§7§l Token§f!",
                    "#32CAFC"
            );
            case "HEALING" -> new TokenDescriptor(
                    "si give healingspringskey 1 " + playerName + " true",
                    "§x§3§2§C§A§6§5 §lHealing Springs§7§l Token§f!",
                    "#32CA65"
            );
            default -> null;
        };
    }

    /**
     * Spawns a falling particle sphere at the player's location.
     */
    public void spawnFallingParticleSphere(Player player, String hexCode) {
        Particle.DustOptions dustOptions = Art.createDustOptions(hexCode);
        double radius = plugin.getConfig().getDouble("Systems.VoteSystem.Particles.Radius");
        Location location = player.getLocation();

        Scheduler.Task particleTask = Scheduler.runTimer(new Runnable() {
            double y = radius;

            @Override
            public void run() {
                double sliceRadius = Math.sqrt(radius * radius - y * y);
                for (double theta = 0; theta < 2 * Math.PI; theta += Math.PI / 16) {
                    double x = sliceRadius * Math.cos(theta);
                    double z = sliceRadius * Math.sin(theta);
                    Location particleLocation = location.clone().add(x, 0, z);
                    particleLocation.getWorld().spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, dustOptions);
                }
                y -= radius / 8;
            }
        }, 0L, 1L);

        Scheduler.runLater(() -> {
            if (particleTask != null) {
                particleTask.cancel();
            }
        }, 60L);
    }

    private record RewardEnvelope(UUID uuid, Reward reward) {
    }

    private record Reward(String type, String token, int vp, int streak, int lifetime, boolean streakIncremented) {
    }

    private record TokenDescriptor(String command, String display, String hexColor) {
    }
}
