package at.sleazlee.bmessentials.DonationSystem;

import at.sleazlee.bmessentials.BMEssentials;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import java.nio.charset.StandardCharsets;

public class GetDonations implements PluginMessageListener {

    private final BMEssentials plugin;
    private final DonationCommand donationCommand;

    public GetDonations(BMEssentials plugin) {
        this.plugin = plugin;
        this.donationCommand = new DonationCommand(plugin);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("bmessentials:donation")) {
            return;
        }

        String payload = new String(message, StandardCharsets.UTF_8);

        String[] parts = payload.split(";", 2);
        if (parts.length != 2) {
            plugin.getLogger().warning("Invalid donation payload: " + payload);
            return;
        }

        String playerName = parts[0];
        String donationPackage = parts[1];

        // Use the instance to call the method
        donationCommand.processDonationCommand(playerName, donationPackage);
    }
}
