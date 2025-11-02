package at.sleazlee.bmvelocity.DonationSystem;

import at.sleazlee.bmvelocity.BMVelocity;
import com.google.gson.*;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class DonationSystem {
    private final BMVelocity plugin;
    private final Gson gson = new Gson();

    // Tebex API settings – adjust these as necessary
    private final String tebexQueueUrl = "https://plugin.tebex.io/queue";
    private final String tebexOnlineCommandsUrl = "https://plugin.tebex.io/queue/online-commands/"; // Append player id
    private final String tebexSecret = "00888f91fe17cae7999000c2d9436e988fd63b66";

    public DonationSystem(BMVelocity plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts periodic polling for pending donation commands.
     */
    public void startDonationPolling() {
        // Schedule this task to run every 60 seconds (adjust as needed)
        plugin.getServer().getScheduler().buildTask(plugin, this::pollDonations)
                .repeat(60, TimeUnit.SECONDS)
                .schedule();
    }

    /**
     * Polls the Tebex API for pending commands.
     * First calls GET /queue to get the list of players with pending commands.
     * Then, for each player, calls GET /queue/online-commands/{playerId} to retrieve command details.
     * Finally, stores the commands in the database and deletes them from Tebex.
     */
    public void pollDonations() {
        try {
            // GET /queue to retrieve players with pending commands.
            URL url = new URL(tebexQueueUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("X-Tebex-Secret", tebexSecret);
            conn.setRequestProperty("Accept", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder responseStr = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    responseStr.append(inputLine);
                }
                in.close();

                if (responseStr.toString().length() > 80) {
                    plugin.getLogger().info("Tebex GET /queue response: " + responseStr.toString());
                }

                JsonObject jsonResponse = JsonParser.parseString(responseStr.toString()).getAsJsonObject();
                JsonArray playersArray = jsonResponse.getAsJsonArray("players");
                List<Integer> commandIdsToDelete = new ArrayList<>();

                if (playersArray != null) {
                    for (JsonElement playerElement : playersArray) {
                        JsonObject playerObj = playerElement.getAsJsonObject();
                        int playerInternalId = playerObj.get("id").getAsInt();
                        String rawPlayerUuid = playerObj.get("uuid").getAsString();

                        // ** Fix: Convert Tebex UUID to Hyphenated Format**
                        String formattedUuid = formatUUID(rawPlayerUuid); // Convert Tebex UUID format

                        // Get the online commands for this player
                        List<CommandDetail> commands = getOnlineCommandsForPlayer(playerInternalId);
                        for (CommandDetail cmd : commands) {
                            // Check if the player is online
                            plugin.getServer().getPlayer(UUID.fromString(formattedUuid)).ifPresentOrElse(
                                    player -> {
                                        // Player is online, send the plugin message directly
                                        sendDonationMessageToSpigot(player.getUniqueId(), cmd.command);
                                    },
                                    () -> {
                                        // Player is offline, store in the database
                                        plugin.getDatabaseManager().asyncAddPendingDonation(formattedUuid, cmd.command);
                                    }
                            );
                            // Collect command id for deletion
                            commandIdsToDelete.add(cmd.id);
                        }
                    }
                }

                // Only delete if we have some command IDs to remove.
                if (!commandIdsToDelete.isEmpty()) {
                    deleteQueue(commandIdsToDelete);
                }
            } else {
                plugin.getLogger().error("Failed to poll donations from Tebex API. Response code: " + responseCode);
            }
        } catch (Exception e) {
            plugin.getLogger().error("Exception during donation polling: " + e.getMessage(), e);
        }
    }

    /**
     * Converts a UUID string from a no-hyphen Tebex format to the standard hyphenated format.
     */
    private String formatUUID(String uuid) {
        // If the UUID already contains hyphens and is 36 characters, assume it's valid.
        if (uuid.contains("-") && uuid.length() == 36) {
            return uuid.toLowerCase();
        }
        // Otherwise, add the hyphens.
        String normalized = uuid.replace("-", "").toLowerCase();
        if (normalized.length() != 32) {
            throw new IllegalArgumentException("UUID must have 32 hex digits when normalized");
        }
        return normalized.substring(0, 8) + "-" +
                normalized.substring(8, 12) + "-" +
                normalized.substring(12, 16) + "-" +
                normalized.substring(16, 20) + "-" +
                normalized.substring(20);
    }



    /**
     * Helper method to call GET /queue/online-commands/{playerId} and return command details.
     */
    private List<CommandDetail> getOnlineCommandsForPlayer(int playerId) {
        List<CommandDetail> commands = new ArrayList<>();
        try {
            URL url = new URL(tebexOnlineCommandsUrl + playerId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("X-Tebex-Secret", tebexSecret);
            conn.setRequestProperty("Accept", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder responseStr = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    responseStr.append(inputLine);
                }
                in.close();

                plugin.getLogger().info("GET /queue/online-commands/" + playerId + " response: " + responseStr.toString());
                JsonObject jsonResponse = JsonParser.parseString(responseStr.toString()).getAsJsonObject();
                JsonArray commandsArray = jsonResponse.getAsJsonArray("commands");

                if (commandsArray != null) {
                    for (JsonElement cmdElem : commandsArray) {
                        JsonObject cmdObj = cmdElem.getAsJsonObject();
                        int cmdId = cmdObj.get("id").getAsInt();
                        String commandText = cmdObj.get("command").getAsString();
                        commands.add(new CommandDetail(cmdId, commandText));
                    }
                }
            } else {
                plugin.getLogger().error("Failed to get online commands for player " + playerId + ". Response code: " + responseCode);
            }
        } catch (Exception e) {
            plugin.getLogger().error("Exception while retrieving online commands for player " + playerId + ": " + e.getMessage(), e);
        }
        return commands;
    }

    /**
     * Sends an HTTP DELETE request to the Tebex /queue endpoint with a JSON body containing the command IDs to delete.
     */
    private void deleteQueue(List<Integer> commandIds) {
        try {
            // Build JSON body: { "ids": [123, 124, ...] }
            JsonObject payload = new JsonObject();
            JsonArray idsArray = new JsonArray();
            for (int id : commandIds) {
                idsArray.add(id);
            }
            payload.add("ids", idsArray);
            String jsonInputString = payload.toString();

            URL url = new URL(tebexQueueUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-Tebex-Secret", tebexSecret);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_NO_CONTENT || responseCode == HttpURLConnection.HTTP_OK) {
                plugin.getLogger().info("Deleted " + commandIds.size() + " commands from Tebex queue successfully.");
            } else {
                plugin.getLogger().error("Failed to delete queue from Tebex API. Response code: " + responseCode);
            }
        } catch (Exception e) {
            plugin.getLogger().error("Exception during queue deletion: " + e.getMessage(), e);
        }
    }

    // --- Event Listeners for Processing Pending Donations ---

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        String serverName = event.getPlayer().getCurrentServer()
                .map(conn -> conn.getServerInfo().getName())
                .orElse("");
        checkPendingDonationsAndProcess(uuid, serverName);
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        String serverName = event.getServer().getServerInfo().getName();
        checkPendingDonationsAndProcess(uuid, serverName);
    }

    /**
     * Checks if the given player has pending donation commands.
     * If the player is on the "spawn" server, send the donation command via plugin message and remove it from the database.
     */
    public void checkPendingDonationsAndProcess(UUID uuid, String serverName) {
        if (!serverName.equalsIgnoreCase("spawn")) {
            return;
        }
        // Delay processing by 3 seconds to ensure the player's connection is fully ready
        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            plugin.getDatabaseManager().asyncGetPendingDonations(uuid.toString(), donationList -> {
                if (!donationList.isEmpty()) {
                    donationList.forEach(donation -> {
                        sendDonationMessageToSpigot(uuid, donation.command);
                        plugin.getDatabaseManager().asyncRemovePendingDonation(uuid.toString(), donation.command);
                    });
                } else {
                    plugin.getLogger().info("No pending donations found for player " + uuid.toString());
                }
            });
        }).delay(3, TimeUnit.SECONDS).schedule();
    }


    /**
     * Sends a plugin message to the Spigot backend with the donation command.
     */
    public void sendDonationMessageToSpigot(UUID uuid, String command) {
        plugin.getServer().getPlayer(uuid).ifPresent(player -> {
            player.getCurrentServer().ifPresent(connection -> {
                // Build the raw payload
                String updatedCommand = command.replace("{name}", player.getUsername());
                String[] commandParts = updatedCommand.split(" ", 3);
                String playerName = commandParts[1];
                String donationPackage = commandParts[2];
                byte[] raw = (playerName + ";" + donationPackage).getBytes(StandardCharsets.UTF_8);

                // Encrypt
                byte[] cipher;
                try {
                    cipher = plugin.getAes().encrypt(raw);
                } catch (Exception e) {
                    plugin.getLogger().error("[DonationSystem] Encryption failed for " + player.getUsername(), e);
                    return;
                }

                // Send over the plugin channel
                boolean success = connection.sendPluginMessage(
                        MinecraftChannelIdentifier.create("bmessentials", "donation"),
                        cipher
                );
                plugin.getLogger().info("[DonationSystem] Sent encrypted donation message for "
                        + player.getUsername() + ": " + success);
            });
        });
    }


    /**
     * Simple helper class to represent a command detail.
     */
    private static class CommandDetail {
        final int id;
        final String command;

        CommandDetail(int id, String command) {
            this.id = id;
            this.command = command;
        }
    }
}
