package at.sleazlee.bmessentials.TreeFeller;

import at.sleazlee.bmessentials.BMEssentials;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.PermissionNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Handles tracking of players who have automatic tree felling enabled
 * and registers the toggle command and listener.
 */
public class TreeFellerManager {
    static final String PERMISSION = "bmessentials.treefeller.use";

    private final BMEssentials plugin;
    private LuckPerms luckPerms;

    public TreeFellerManager(BMEssentials plugin) {
        this.plugin = plugin;
        setupLuckPerms();
        plugin.getCommand("treefeller").setExecutor(new ToggleCommand(this));
        Bukkit.getPluginManager().registerEvents(new TreeFellerListener(this), plugin);
    }

    private void setupLuckPerms() {
        try {
            this.luckPerms = LuckPermsProvider.get();
        } catch (IllegalStateException e) {
            plugin.getLogger().warning("LuckPerms not available; TreeFeller toggle will not persist.");
        }
    }

    BMEssentials getPlugin() {
        return plugin;
    }

    boolean isEnabled(Player player) {
        if (luckPerms == null) {
            return true;
        }
        return luckPerms.getPlayerAdapter(Player.class).getPermissionData(player)
                .checkPermission(PERMISSION).asBoolean();
    }

    void toggle(Player player) {
        if (luckPerms == null) {
            player.sendMessage("LuckPerms not loaded, cannot toggle TreeFeller.");
            return;
        }

        User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
        boolean currently = isEnabled(player);

        // Remove existing nodes for this permission
        user.data().clear(node -> node.getKey().equals(PERMISSION));

        if (!currently) {
            Node node = PermissionNode.builder(PERMISSION).value(true).build();
            user.data().add(node);
        } else {
            Node node = PermissionNode.builder(PERMISSION).value(false).build();
            user.data().add(node);
        }

        luckPerms.getUserManager().saveUser(user);
    }
}
