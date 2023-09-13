package at.sleazlee.bmessentials.Punish;

import at.sleazlee.bmessentials.BMEssentials;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class BungeeMutePlayer implements PluginMessageListener {

	private final BMEssentials plugin;

	public BungeeMutePlayer(BMEssentials plugin) {
		this.plugin = plugin;
	}

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if (!channel.equals("bmessentials:mute")) {
			return;
		}

		ByteArrayDataInput in = ByteStreams.newDataInput(message);
		String muteCommandBuilder = in.readUTF();

		plugin.getLogger().info("Received the message: " + muteCommandBuilder);

		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		Bukkit.dispatchCommand(console, muteCommandBuilder);

	}
}