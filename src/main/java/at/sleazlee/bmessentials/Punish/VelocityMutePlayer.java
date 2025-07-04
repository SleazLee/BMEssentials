package at.sleazlee.bmessentials.Punish;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.crypto.AESEncryptor;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import at.sleazlee.bmessentials.Scheduler;

public class VelocityMutePlayer implements PluginMessageListener {

    private final BMEssentials plugin;
    private final AESEncryptor aes;

    public VelocityMutePlayer(BMEssentials plugin) {
            this.plugin = plugin;
            this.aes = plugin.getAes();
    }

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if (!channel.equals("bmessentials:mute")) {
			return;
		}

                byte[] plain;
                try {
                        plain = aes.decrypt(message);
                } catch (Exception e) {
                        plugin.getLogger().warning("Failed to decrypt mute payload from " + player.getName());
                        return;
                }

                ByteArrayDataInput in = ByteStreams.newDataInput(plain);
                String muteCommandBuilder = in.readUTF();

		plugin.getLogger().info("Received the message: " + muteCommandBuilder);

                ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();

                // Execute the mute command on the main thread to satisfy Folia
                Scheduler.run(() -> Bukkit.dispatchCommand(console, muteCommandBuilder));

	}
}
