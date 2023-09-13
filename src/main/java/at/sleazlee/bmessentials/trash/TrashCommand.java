package at.sleazlee.bmessentials.trash;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class TrashCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("This command can only be used by a player!");
			return true;
		}

		Player player = (Player) sender;
		Location location = player.getLocation();

		// Play a sound
		player.getWorld().playSound(location, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);

		// Create a new inventory with 54 slots (size of a double chest)
		Inventory trash = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "Trash");

		// Open the inventory
		player.openInventory(trash);

		return true;
	}
}