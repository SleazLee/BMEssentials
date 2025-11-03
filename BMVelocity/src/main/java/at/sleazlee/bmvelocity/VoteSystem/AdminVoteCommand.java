package at.sleazlee.bmvelocity.VoteSystem;

import at.sleazlee.bmvelocity.BMVelocity;
import at.sleazlee.bmvelocity.util.UUIDTools;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Allows staff to emulate a vote for testing the proxy-driven vote system.
 */
public class AdminVoteCommand implements SimpleCommand {

    private final BMVelocity plugin;

    public AdminVoteCommand(BMVelocity plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        if (!source.hasPermission("bm.staff")) {
            source.sendMessage(miniMessage().deserialize("<red>You do not have permission to use this command.</red>"));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length != 1) {
            source.sendMessage(miniMessage().deserialize("<red>Usage: /adminvote <player></red>"));
            return;
        }

        String targetName = args[0];
        UUID uuid = UUIDTools.getUUID(plugin, targetName);
        if (uuid == null) {
            source.sendMessage(miniMessage().deserialize("<red>Unable to resolve a UUID for <gray>" + targetName + "</gray>.</red>"));
            return;
        }

        String resolvedName = plugin.getServer().getPlayer(uuid)
                .map(Player::getUsername)
                .orElse(targetName);

        plugin.getVoteSystem().handleIncomingVote(uuid, resolvedName);
        source.sendMessage(miniMessage().deserialize("<green>Simulated a vote for <aqua>" + resolvedName + "</aqua>.</green>"));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> suggestions = new ArrayList<>();
            plugin.getServer().getAllPlayers().forEach(player -> {
                String username = player.getUsername();
                if (username.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    suggestions.add(username);
                }
            });
            return suggestions;
        }
        return List.of();
    }

    private MiniMessage miniMessage() {
        return plugin.getMiniMessage();
    }
}
