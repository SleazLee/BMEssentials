package at.sleazlee.bmessentials.SimplePortals;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.SpawnSystems.HealCommand;
import at.sleazlee.bmessentials.wild.WildCommand;
import at.sleazlee.bmessentials.wild.WildData;
import at.sleazlee.bmessentials.wild.WildLocationsDatabase;
import at.sleazlee.bmessentials.huskhomes.HuskHomesAPIHook;
import at.sleazlee.bmessentials.wild.WildLocationsDatabase;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.SessionManager;
import com.sk89q.worldguard.session.handler.FlagValueChangeHandler;
import com.sk89q.worldguard.session.handler.Handler;
import org.bukkit.entity.Player;

/**
 * Registers simple portal handlers using WorldGuard custom flags.
 */
public class SimplePortals {

    public SimplePortals(BMEssentials plugin, WildLocationsDatabase wildDB) {
        WildData wildData = new WildData(plugin);
        WildCommand wildCommand = new WildCommand(wildData, wildDB, plugin);
        HealCommand healCommand = new HealCommand(plugin);

        SessionManager manager = WorldGuard.getInstance().getPlatform().getSessionManager();
        manager.registerHandler(new SendToWildHandler.Factory(wildCommand), null);
        manager.registerHandler(new HealingSpringsHandler.Factory(healCommand), null);
        manager.registerHandler(new SendToWarpHandler.Factory(), null);
    }

    private static class SendToWildHandler extends FlagValueChangeHandler<StateFlag.State> {
        private final WildCommand wild;

        protected SendToWildHandler(Session session, WildCommand wild) {
            super(session, BMEssentials.SEND_TO_WILD_FLAG);
            this.wild = wild;
        }

        @Override
        protected void onInitialValue(LocalPlayer player, ApplicableRegionSet set, StateFlag.State value) {
        }

        @Override
        protected boolean onSetValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet set,
                                     StateFlag.State currentValue, StateFlag.State lastValue, MoveType moveType) {
            if (currentValue == StateFlag.State.ALLOW && currentValue != lastValue) {
                Player bukkitPlayer = BukkitAdapter.adapt(player);
                wild.randomLocation(bukkitPlayer, "all");
            }
            return true;
        }

        @Override
        protected boolean onAbsentValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet set,
                                        StateFlag.State lastValue, MoveType moveType) {
            return true;
        }

        public static class Factory extends Handler.Factory<SendToWildHandler> {
            private final WildCommand wild;
            public Factory(WildCommand wild) {
                this.wild = wild;
            }
            @Override
            public SendToWildHandler create(Session session) {
                return new SendToWildHandler(session, wild);
            }
        }
    }

    private static class HealingSpringsHandler extends FlagValueChangeHandler<StateFlag.State> {
        private final HealCommand heal;

        protected HealingSpringsHandler(Session session, HealCommand heal) {
            super(session, BMEssentials.ENTERED_HEALING_SPRINGS_FLAG);
            this.heal = heal;
        }

        @Override
        protected void onInitialValue(LocalPlayer player, ApplicableRegionSet set, StateFlag.State value) {
        }

        @Override
        protected boolean onSetValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet set,
                                     StateFlag.State currentValue, StateFlag.State lastValue, MoveType moveType) {
            if (currentValue == StateFlag.State.ALLOW && currentValue != lastValue) {
                Player p = BukkitAdapter.adapt(player);
                heal.checkAndExecute(p);
            }
            return true;
        }

        @Override
        protected boolean onAbsentValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet set,
                                        StateFlag.State lastValue, MoveType moveType) {
            return true;
        }

        public static class Factory extends Handler.Factory<HealingSpringsHandler> {
            private final HealCommand heal;
            public Factory(HealCommand heal) {
                this.heal = heal;
            }
            @Override
            public HealingSpringsHandler create(Session session) {
                return new HealingSpringsHandler(session, heal);
            }
        }
    }

    private static class SendToWarpHandler extends FlagValueChangeHandler<String> {

        protected SendToWarpHandler(Session session) {
            super(session, BMEssentials.SEND_TO_WARP_FLAG);
        }

        @Override
        protected void onInitialValue(LocalPlayer player, ApplicableRegionSet set, String value) {
        }

        @Override
        protected boolean onSetValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet set,
                                     String currentValue, String lastValue, MoveType moveType) {
            if (currentValue != null && !currentValue.equals(lastValue)) {
                Player p = BukkitAdapter.adapt(player);
                HuskHomesAPIHook.warpPlayer(p, currentValue);
            }
            return true;
        }

        @Override
        protected boolean onAbsentValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet set,
                                        String lastValue, MoveType moveType) {
            return true;
        }

        public static class Factory extends Handler.Factory<SendToWarpHandler> {
            @Override
            public SendToWarpHandler create(Session session) {
                return new SendToWarpHandler(session);
            }
        }
    }
}
