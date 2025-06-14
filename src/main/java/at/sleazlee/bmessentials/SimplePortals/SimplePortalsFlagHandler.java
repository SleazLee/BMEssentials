package at.sleazlee.bmessentials.SimplePortals;

import at.sleazlee.bmessentials.SpawnSystems.HealCommand;
import at.sleazlee.bmessentials.wild.WildCommand;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.Handler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * WorldGuard session handler that checks for custom SimplePortals flags
 * when players cross region boundaries.
 */
public class SimplePortalsFlagHandler extends Handler {

    /** Factory used by WorldGuard to create handler instances. */
    public static class Factory extends Handler.Factory<SimplePortalsFlagHandler> {
        private final WildCommand wildCommand;
        private final HealCommand healCommand;

        public Factory(WildCommand wildCommand, HealCommand healCommand) {
            super(SimplePortalsFlagHandler.class);
            this.wildCommand = wildCommand;
            this.healCommand = healCommand;
        }

        @Override
        public SimplePortalsFlagHandler create(Session session) {
            return new SimplePortalsFlagHandler(session, wildCommand, healCommand);
        }
    }

    private final WildCommand wildCommand;
    private final HealCommand healCommand;

    private SimplePortalsFlagHandler(Session session, WildCommand wildCommand, HealCommand healCommand) {
        super(session);
        this.wildCommand = wildCommand;
        this.healCommand = healCommand;
    }

    @Override
    public void initialize(LocalPlayer player, ApplicableRegionSet set) {
        handle(player, set);
    }

    @Override
    public void onCrossBoundary(LocalPlayer player, ApplicableRegionSet to, ApplicableRegionSet from, MoveType moveType) {
        handle(player, to);
    }

    private void handle(LocalPlayer localPlayer, ApplicableRegionSet set) {
        Player player = Bukkit.getPlayer(localPlayer.getUniqueId());
        if (player == null) {
            return;
        }

        if (set.testState(localPlayer, SimplePortals.SEND_TO_WILD)) {
            wildCommand.randomLocation(player, "all");
        }

        if (set.testState(localPlayer, SimplePortals.ENTERED_HEALING_SPRINGS)) {
            healCommand.checkAndExecute(player);
        }
    }
}
