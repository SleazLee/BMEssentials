package at.sleazlee.bmessentials.DonationSystem;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.crypto.AESEncryptor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;

/**
 * Listener for handling encrypted donation plugin messages from the proxy.
 */
public class GetDonations implements PluginMessageListener {

    private final BMEssentials plugin;
    private final DonationCommand donationCommand;
    private final AESEncryptor aes;

    public GetDonations(BMEssentials plugin) {
        this.plugin = plugin;
        this.donationCommand = new DonationCommand(plugin);
        this.aes = plugin.getAes();
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        // Only handle our custom donation channel
        if (!channel.equals("bmessentials:donation")) {
            return;
        }

        // 1) Decrypt the incoming bytes
        byte[] plain;
        try {
            plain = aes.decrypt(message);
        } catch (Exception e) {
            plugin.getLogger().warning("Dropping malformed donation packet from " + player.getName());
            return;
        }

        // 2) Convert to UTF-8 string
        String payload = new String(plain, StandardCharsets.UTF_8);
        plugin.getLogger().info("BMDonation: Decrypted payload from " + player.getName() + ": " + payload);

        // 3) Split and validate
        String[] parts = payload.split(";", 2);
        if (parts.length != 2) {
            plugin.getLogger().warning("Invalid donation payload: " + payload);
            return;
        }

        String playerName = parts[0];
        String donationPackage = parts[1];

        // 4) Process the donation command
        donationCommand.processDonationCommand(playerName, donationPackage);
    }
}
