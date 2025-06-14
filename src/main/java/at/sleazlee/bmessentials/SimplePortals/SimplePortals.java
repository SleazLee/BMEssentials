package at.sleazlee.bmessentials.SimplePortals;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.SpawnSystems.HealCommand;
import at.sleazlee.bmessentials.wild.WildCommand;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.FlagConflictException;
import com.sk89q.worldguard.protection.flags.FlagRegistry;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.SessionManager;
import com.sk89q.worldguard.session.handler.Handler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * A simple portal system leveraging WorldGuard regions and custom flags.
 * When a player enters a region with one of the custom flags set to ALLOW,
 * a specific method is executed.
 */
public class SimplePortals {

    /** Flag that triggers a random wild teleport. */
    public static StateFlag SEND_TO_WILD;
    /** Flag that triggers the healing springs check. */
    public static StateFlag ENTERED_HEALING_SPRINGS;

    private final WildCommand wildCommand;
    private final HealCommand healCommand;

    public SimplePortals(WildCommand wildCommand, HealCommand healCommand) {
        this.wildCommand = wildCommand;
        this.healCommand = healCommand;
    }

    /**
     * Register the custom flags with WorldGuard. Should be called during plugin load.
     */
    public static void registerFlags() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

        try {
            StateFlag flag = new StateFlag("send-to-wild", false);
            registry.register(flag);
            SEND_TO_WILD = flag;
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get("send-to-wild");
            if (existing instanceof StateFlag) {
                SEND_TO_WILD = (StateFlag) existing;
            }
        }

        try {
            StateFlag flag = new StateFlag("entered-healing-springs", false);
            registry.register(flag);
            ENTERED_HEALING_SPRINGS = flag;
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get("entered-healing-springs");
            if (existing instanceof StateFlag) {
                ENTERED_HEALING_SPRINGS = (StateFlag) existing;
            }
        }
    }

    /**
     * Register the session handlers that listen for region entry.
     */
    public void registerHandlers() {
        SessionManager manager = WorldGuard.getInstance().getPlatform().getSessionManager();
        manager.registerHandler(new SendToWildHandler.Factory(wildCommand), null);
        manager.registerHandler(new HealingSpringsHandler.Factory(healCommand), null);
    }

    /** Handler for the send-to-wild flag. */
    private static class SendToWildHandler extends Handler {
        static class Factory extends Handler.Factory<SendToWildHandler> {
            private final WildCommand wild;
            Factory(WildCommand wild) { this.wild = wild; }
            @Override
            public SendToWildHandler create(Session session) {
                return new SendToWildHandler(session, wild);
            }
        }
        private final WildCommand wild;
        SendToWildHandler(Session session, WildCommand wild) {
            super(session);
            this.wild = wild;
        }
        @Override
        public boolean onCrossBoundary(LocalPlayer player, Location from, Location to,
                                       ApplicableRegionSet toSet, Set<ProtectedRegion> entered,
                                       Set<ProtectedRegion> exited, MoveType moveType) {
            if (toSet.testState(player, SEND_TO_WILD)) {
                Player bukkit = Bukkit.getPlayer(player.getUniqueId());
                if (bukkit != null) {
                    wild.randomLocation(bukkit, "all");
                }
            }
            return true;
        }
    }

    /** Handler for the entered-healing-springs flag. */
    private static class HealingSpringsHandler extends Handler {
        static class Factory extends Handler.Factory<HealingSpringsHandler> {
            private final HealCommand heal;
            Factory(HealCommand heal) { this.heal = heal; }
            @Override
            public HealingSpringsHandler create(Session session) {
                return new HealingSpringsHandler(session, heal);
            }
        }
        private final HealCommand heal;
        HealingSpringsHandler(Session session, HealCommand heal) {
            super(session);
            this.heal = heal;
        }
        @Override
        public boolean onCrossBoundary(LocalPlayer player, Location from, Location to,
                                       ApplicableRegionSet toSet, Set<ProtectedRegion> entered,
                                       Set<ProtectedRegion> exited, MoveType moveType) {
            if (toSet.testState(player, ENTERED_HEALING_SPRINGS)) {
                Player bukkit = Bukkit.getPlayer(player.getUniqueId());
                if (bukkit != null) {
                    heal.checkAndExecute(bukkit);
                }
            }
            return true;
        }
    }
}
