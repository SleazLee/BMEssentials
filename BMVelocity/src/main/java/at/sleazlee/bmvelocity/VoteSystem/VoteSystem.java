package at.sleazlee.bmvelocity.VoteSystem;

import at.sleazlee.bmvelocity.BMVelocity;
import com.google.gson.Gson;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Handles vote reward delivery to the Spigot servers and queued rewards for offline players.
 */
public class VoteSystem {

    private static final Duration STREAK_GRACE = Duration.ofHours(36);
    private static final Duration STREAK_WINDOW = Duration.ofHours(18);
    private static final int VOTES_PER_INCREMENT = 4;
    private static final int STREAK_CAP = 30;
    private static final MinecraftChannelIdentifier VOTE_CHANNEL = MinecraftChannelIdentifier.create("bmessentials", "vote");

    private final BMVelocity plugin;
    private final RewardCalculator rewardCalculator = new RewardCalculator();
    private final Gson gson = new Gson();
    private final Set<UUID> delivering = ConcurrentHashMap.newKeySet();

    public VoteSystem(BMVelocity plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        schedulePendingDelivery(event.getPlayer().getUniqueId(), 2, TimeUnit.SECONDS);
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        schedulePendingDelivery(event.getPlayer().getUniqueId(), 1, TimeUnit.SECONDS);
    }

    /**
     * Handles a vote received by the proxy.
     */
    public void handleIncomingVote(UUID uuid, String playerName) {
        plugin.getServer().getScheduler().buildTask(plugin, () -> processVote(uuid, playerName)).schedule();
    }

    private void processVote(UUID uuid, String playerName) {
        try {
            VoteData data = plugin.getDatabaseManager().loadOrCreateVoteData(uuid);
            long now = System.currentTimeMillis();

            int previousStreak = Math.max(data.getCurrentStreak(), 0);
            long lastVote = data.getLastVote();
            int votesTowardNext = Math.max(data.getVotesSinceIncrement(), 0);
            long windowStart = data.getStreakWindowStart();

            data.setLastVote(now);
            data.setLifetimeVotes(data.getLifetimeVotes() + 1);

            int newStreak = previousStreak <= 0 ? 1 : previousStreak;
            boolean streakIncremented = false;

            if (lastVote > 0 && now - lastVote > STREAK_GRACE.toMillis()) {
                newStreak = 1;
                votesTowardNext = 1;
                windowStart = now;
            } else {
                if (votesTowardNext <= 0 || windowStart <= 0) {
                    votesTowardNext = 1;
                    windowStart = now;
                } else if (now - windowStart > STREAK_WINDOW.toMillis()) {
                    votesTowardNext = 1;
                    windowStart = now;
                } else {
                    votesTowardNext += 1;
                }

                if (votesTowardNext >= VOTES_PER_INCREMENT) {
                    int incremented = Math.min(newStreak + 1, STREAK_CAP);
                    if (incremented > newStreak) {
                        newStreak = incremented;
                        streakIncremented = true;
                        data.setLastStreakIncrement(now);
                    }
                    votesTowardNext = 0;
                    windowStart = 0L;
                }
            }

            data.setCurrentStreak(newStreak);
            data.setVotesSinceIncrement(votesTowardNext);
            data.setStreakWindowStart(windowStart);
            if (!streakIncremented && newStreak == 1) {
                data.setLastStreakIncrement(0L);
            }

            QueuedReward reward = rewardCalculator.createReward(newStreak, data.getLifetimeVotes(), streakIncremented);

            boolean delivered = deliverImmediateReward(uuid, reward);
            if (!delivered) {
                data.getPendingRewards().add(reward);
            }

            plugin.getDatabaseManager().saveVoteData(data);

            if (!delivered) {
                schedulePendingDelivery(uuid, 2, TimeUnit.SECONDS);
            }

            if (reward.streakIncremented() && reward.streak() > 1) {
                announceStreak(playerName, reward.streak());
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Failed to process vote for {}", uuid, e);
        }
    }

    private boolean deliverImmediateReward(UUID uuid, QueuedReward reward) {
        return plugin.getServer().getPlayer(uuid)
                .map(player -> player.getCurrentServer()
                        .map(connection -> sendRewardPayload(connection, uuid, reward))
                        .orElse(false))
                .orElse(false);
    }

    private void schedulePendingDelivery(UUID uuid, long delay, TimeUnit unit) {
        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            if (delivering.add(uuid)) {
                tryDeliverPending(uuid);
            }
        }).delay(delay, unit).schedule();
    }

    private void tryDeliverPending(UUID uuid) {
        plugin.getServer().getPlayer(uuid).ifPresentOrElse(player ->
                player.getCurrentServer().ifPresentOrElse(connection -> deliverNextReward(uuid, connection), () -> {
                    delivering.remove(uuid);
                    schedulePendingDelivery(uuid, 1, TimeUnit.SECONDS);
                }), () -> delivering.remove(uuid));
    }

    private void deliverNextReward(UUID uuid, ServerConnection connection) {
        try {
            VoteData data = plugin.getDatabaseManager().loadOrCreateVoteData(uuid);
            List<QueuedReward> queue = data.getPendingRewards();
            if (queue.isEmpty()) {
                delivering.remove(uuid);
                return;
            }

            QueuedReward reward = queue.remove(0);
            boolean sent = sendRewardPayload(connection, uuid, reward);
            if (sent) {
                data.setPendingRewards(queue);
                plugin.getDatabaseManager().updatePendingRewards(data);
                if (queue.isEmpty()) {
                    delivering.remove(uuid);
                } else {
                    plugin.getServer().getScheduler().buildTask(plugin, () -> tryDeliverPending(uuid))
                            .delay(2, TimeUnit.SECONDS).schedule();
                }
            } else {
                queue.add(0, reward);
                data.setPendingRewards(queue);
                plugin.getDatabaseManager().updatePendingRewards(data);
                delivering.remove(uuid);
                schedulePendingDelivery(uuid, 2, TimeUnit.SECONDS);
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Failed to deliver queued vote reward for {}", uuid, e);
            delivering.remove(uuid);
        }
    }

    private boolean sendRewardPayload(ServerConnection connection, UUID uuid, QueuedReward reward) {
        RewardEnvelope envelope = new RewardEnvelope(uuid, reward);
        byte[] encrypted;
        try {
            String json = gson.toJson(envelope);
            encrypted = plugin.getAes().encrypt(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            plugin.getLogger().error("Failed to encrypt vote reward payload for {}", uuid, e);
            return false;
        }

        boolean success = connection.sendPluginMessage(VOTE_CHANNEL, encrypted);
        if (!success) {
            plugin.getLogger().warn("Failed to send vote reward to backend for {}", uuid);
        }
        return success;
    }

    private void announceStreak(String playerName, int streak) {
        if (streak <= 1 || playerName == null || playerName.isBlank()) {
            return;
        }
        String message = "<light_purple><bold>Vote</light_purple> <aqua>" + playerName + "</aqua> <gray>is on a <green>" + streak +
                "</green>-day streak!";
        plugin.getServer().getAllPlayers().forEach(p -> p.sendMessage(plugin.getMiniMessage().deserialize(message)));
    }

    private record RewardEnvelope(UUID uuid, QueuedReward reward) {
    }
}
