package at.sleazlee.bmessentials.trophyroom.listeners;

import at.sleazlee.bmessentials.trophyroom.data.Data;
import at.sleazlee.bmessentials.trophyroom.db.Database;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.SQLException;

public class PlayerListener implements Listener {
    public PlayerListener() {
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) throws SQLException {
        Database.getDatabase().insertPlayer(e.getPlayer().getUniqueId().toString().replaceAll("-", ""), e.getPlayer().getName());
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (!e.isCancelled()) {
            Data data = Data.getData();
            if (e.getItemInHand().hasItemMeta() && data.getTrophy(e.getItemInHand()) != null) {
                e.setCancelled(true);
            }

        }
    }
}