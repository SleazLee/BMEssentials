package at.sleazlee.bmessentials.SimplePortals;

import at.sleazlee.bmessentials.SpawnSystems.HealCommand;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.FlagValueChangeHandler;
import com.sk89q.worldguard.session.handler.Handler;
import org.bukkit.entity.Player;

/**
 * Handler that triggers healing when a player enters a region with the entered-healing-springs flag set.
 */
public class HealingSpringsHandler extends FlagValueChangeHandler<Boolean> {

    private final HealCommand healCommand;

    public static class Factory extends Handler.Factory<HealingSpringsHandler> {
        private final HealCommand healCommand;
        public Factory(HealCommand healCommand) {
            this.healCommand = healCommand;
        }
        @Override
        public HealingSpringsHandler create(Session session) {
            return new HealingSpringsHandler(session, healCommand);
        }
    }

    private HealingSpringsHandler(Session session, HealCommand healCommand) {
        super(session, SimplePortals.ENTERED_HEALING_SPRINGS);
        this.healCommand = healCommand;
    }

    @Override
    protected void onInitialValue(LocalPlayer player, ApplicableRegionSet set, Boolean value) {
        if (Boolean.TRUE.equals(value)) {
            run(player);
        }
    }

    @Override
    protected boolean onSetValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet set,
                                 Boolean currentValue, Boolean lastValue, MoveType moveType) {
        if (Boolean.TRUE.equals(currentValue) && !Boolean.TRUE.equals(lastValue)) {
            run(player);
        }
        return true;
    }

    @Override
    protected boolean onAbsentValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet set,
                                    Boolean lastValue, MoveType moveType) {
        return true;
    }

    private void run(LocalPlayer localPlayer) {
        Player bukkit = localPlayer.getPlayer();
        if (bukkit != null) {
            healCommand.checkAndExecute(bukkit);
        }
    }
}
