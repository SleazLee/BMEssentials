package at.sleazlee.bmessentials.SimplePortals;

import at.sleazlee.bmessentials.wild.WildCommand;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.FlagValueChangeHandler;
import com.sk89q.worldguard.session.handler.Handler;
import org.bukkit.entity.Player;

/**
 * Handler that teleports a player to the wild when entering a region with the send-to-wild flag set to allow.
 */
public class SendToWildHandler extends FlagValueChangeHandler<Boolean> {

    private final WildCommand wildCommand;

    public static class Factory extends Handler.Factory<SendToWildHandler> {
        private final WildCommand wildCommand;
        public Factory(WildCommand wildCommand) {
            this.wildCommand = wildCommand;
        }
        @Override
        public SendToWildHandler create(Session session) {
            return new SendToWildHandler(session, wildCommand);
        }
    }

    private SendToWildHandler(Session session, WildCommand wildCommand) {
        super(session, SimplePortals.SEND_TO_WILD);
        this.wildCommand = wildCommand;
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
            wildCommand.randomLocation(bukkit, "all");
        }
    }
}
