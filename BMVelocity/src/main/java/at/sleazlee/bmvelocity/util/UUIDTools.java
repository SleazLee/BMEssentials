package at.sleazlee.bmvelocity.util;

import at.sleazlee.bmvelocity.BMVelocity;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.velocitypowered.api.proxy.Player;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class UUIDTools {

    private static final Map<String, UUID> FLOODGATE_UUID_CACHE = new ConcurrentHashMap<>();

    private UUIDTools() {
    }

    /**
     * Retrieves the UUID for a given player. For Java players we fall back to Mojang's API.
     * Bedrock players connecting through Floodgate are resolved via a cache populated on login.
     *
     * @param plugin     the plugin instance used for proxy access and logging
     * @param playerName the player's username as provided by the caller
     * @return the UUID of the player, or {@code null} if it cannot be found
     */
    public static UUID getUUID(BMVelocity plugin, String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return null;
        }

        // 1) Check if the player is currently connected to the proxy.
        Optional<Player> onlinePlayer = plugin.getServer().getPlayer(playerName);
        if (onlinePlayer.isPresent()) {
            UUID uuid = onlinePlayer.get().getUniqueId();
            cacheFloodgatePlayer(playerName, uuid);
            return uuid;
        }

        // 2) Try a cached Floodgate UUID (populated by FloodgatePlayerListener).
        UUID cachedFloodgate = FLOODGATE_UUID_CACHE.get(playerName.toLowerCase(Locale.ROOT));
        if (cachedFloodgate != null) {
            return cachedFloodgate;
        }

        // 3) Bedrock usernames are not resolvable via Mojang's API, so skip the HTTP lookup.
        if (playerName.startsWith(".")) {
            plugin.getLogger().warn("Unable to resolve UUID for Floodgate player '{}' - ensure they have joined while the proxy was online.", playerName);
            return null;
        }

        // 4) Fallback to Mojang's API for Java players.
        return requestMojangUuid(plugin, playerName);
    }

    private static UUID requestMojangUuid(BMVelocity plugin, String playerName) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setUseCaches(false);

            try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                JsonElement element = JsonParser.parseReader(reader);
                if (element == null || !element.isJsonObject()) {
                    return null;
                }
                String uuidString = element.getAsJsonObject().get("id").getAsString();

                return UUID.fromString(
                        uuidString.substring(0, 8) + "-" +
                                uuidString.substring(8, 12) + "-" +
                                uuidString.substring(12, 16) + "-" +
                                uuidString.substring(16, 20) + "-" +
                                uuidString.substring(20, 32)
                );
            }
        } catch (Exception e) {
            plugin.getLogger().error("Failed to look up Mojang UUID for {}", playerName, e);
            return null;
        }
    }

    /**
     * Cache Floodgate player data for later lookups.
     *
     * @param playerName the username (case-insensitive)
     * @param uuid       the player's UUID on the proxy
     */
    public static void cacheFloodgatePlayer(String playerName, UUID uuid) {
        if (playerName == null || uuid == null) {
            return;
        }
        String lowerCase = playerName.toLowerCase(Locale.ROOT);
        FLOODGATE_UUID_CACHE.put(lowerCase, uuid);
        if (playerName.startsWith(".") && playerName.length() > 1) {
            FLOODGATE_UUID_CACHE.put(playerName.substring(1).toLowerCase(Locale.ROOT), uuid);
        }
    }
}
