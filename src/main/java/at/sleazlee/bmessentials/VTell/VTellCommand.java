package at.sleazlee.bmessentials.VTell;

import at.sleazlee.bmessentials.BMEssentials;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;

public class VTellCommand implements CommandExecutor {
	private static BMEssentials plugin;
	private final MiniMessage miniMessage = MiniMessage.miniMessage();

	public VTellCommand(BMEssentials plugin) {
		VTellCommand.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equalsIgnoreCase("vtell")) {
			if (sender instanceof Player) {
				sender.sendMessage(miniMessage.deserialize("<red>This command can only be run from the console.</red>"));
				return true;
			}
			if (args.length == 0) {
				return false;
			}
			StringBuilder builder = new StringBuilder();
			for (String s : args) {
				builder.append(s).append(" ");
			}
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(b);
			try {
				out.writeUTF(builder.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
			Collection<? extends Player> players = Bukkit.getOnlinePlayers();
			if (!players.isEmpty()) {
				players.iterator().next().sendPluginMessage(plugin, "bmessentials:vtell", b.toByteArray());
			}
			return true;
		}
		return false;
	}

	public static void broadcastMessage(String message) {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(b);
		try {
			out.writeUTF(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Collection<? extends Player> players = Bukkit.getOnlinePlayers();
		if (!players.isEmpty()) {
			players.iterator().next().sendPluginMessage(plugin, "vtell:broadcast", b.toByteArray());
		}
	}
}
