package at.sleazlee.bmessentials.BlueMapFunctions;

import at.sleazlee.bmessentials.BMEssentials;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.WebApp;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class MapCommand implements CommandExecutor {

    private final WebApp webApp;
    private final BMEssentials plugin;


    public MapCommand(BMEssentials plugin) {
        this.plugin = plugin;
        Optional<BlueMapAPI> apiOpt = BlueMapAPI.getInstance();
        this.webApp = apiOpt.flatMap(api -> Optional.ofNullable(api.getWebApp()))
                .orElse(null);
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        // only players can use it
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        // ensure BlueMap is up
        if (!BlueMapAPI.getInstance().isPresent()) {
            sender.sendMessage(ChatColor.RED + "<color:#ff3300><bold>Maps </color:#ff3300></bold><red>The Server Map is not available.</red> <white>Try again later!</white>");
            return true;
        }

        UUID uuid = player.getUniqueId();
        var api = BlueMapAPI.getInstance().get();

        if (args.length == 0) {
            findServer(player);
            return true;

        } else if (args[0].equalsIgnoreCase("toggle")) {
            boolean currentlyVisible = api.getWebApp().getPlayerVisibility(uuid);
            api.getWebApp().setPlayerVisibility(uuid, !currentlyVisible);

            String state = currentlyVisible
                    ? "<gold>invisible</gold>"
                    : "<aqua>visible</aqua>";

            player.sendMessage(MiniMessage.miniMessage().deserialize("<green><bold>Maps </bold></green><gray>You are now " + state + " on the Server Map!</gray>"));
            return true;
        }

        // explicit show/hide
        if (args[0].equalsIgnoreCase("show")) {
            api.getWebApp().setPlayerVisibility(uuid, true);
            player.sendMessage(MiniMessage.miniMessage().deserialize("<green><bold>Maps </bold></green><gray>You are now <aqua>visible</aqua> on the Server Map!</gray>"));
        } else if (args[0].equalsIgnoreCase("hide")) {
            api.getWebApp().setPlayerVisibility(uuid, false);
            player.sendMessage(MiniMessage.miniMessage().deserialize("<green><bold>Maps </bold></green><gray>You are now <gold>invisible</gold> on the Server Map!</gray>"));
        } else {
            player.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " [toggle|show|hide]");
        }

        return true;
    }



    // Old Map functions
    public void findServer(Player player) {

        String district = plugin.getConfig().getString("serverName");

        matchServer(district, player);


    }

    public void matchServer(String district, Player player) {

        String mapLink = "";

        switch (district.toLowerCase()) {
            case "blockminer":
                createMapLink(district.toLowerCase(), player);
                break;
            default:
                mapLink = plugin.getConfig().getString("Systems.Maps.Default");
                player.sendMessage(MiniMessage.miniMessage().deserialize("<green><bold>Maps </bold></green><gray>Here's the link: <hover:show_text:'<dark_green>Click to open the Map URL.</dark_green>'><click:open_url:'" + mapLink + "'><dark_green><bold>Click Me</bold></dark_green><gray>!</gray></click></hover>"));
                break;
        }
    }

    public void createMapLink(String districtName, Player player) {

        String mapLink = "";

        // Get the player's location
        Location location = player.getLocation();

        // Get the world name
        String worldName = location.getWorld().getName();

        // Get the X and Z coordinates
        int x = (int) location.getX();
        int y = (int) location.getY();
        int z = (int) location.getZ();


        if (worldName.contains("_the_end")) {
            mapLink = "https://blockminer.net/map/";

        } else {
            mapLink = "https://blockminer.net/map/#" + worldName + ":" + x + ":" + y + ":" + z + ":150:0:0:0:0:perspective";
            //
            // https://blockminer.net/map/#world:X:Y:Z:150:0:0:0:0:perspective
        }

        player.sendMessage(MiniMessage.miniMessage().deserialize("<green><bold>Maps </bold></green><gray>Here's the link: <hover:show_text:'<dark_green>Click to open the Map URL.</dark_green>'><click:open_url:'" + mapLink + "'><dark_green><bold>Click Me</bold></dark_green><gray>!</gray></click></hover>"));
    }
}
