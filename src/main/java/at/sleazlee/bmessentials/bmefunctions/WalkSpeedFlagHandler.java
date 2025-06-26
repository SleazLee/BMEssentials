package at.sleazlee.bmessentials.bmefunctions;

import at.sleazlee.bmessentials.BMEssentials;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.Handler;
import org.bukkit.entity.Player;

public class WalkSpeedFlagHandler extends AbstractSpeedFlagHandler {
    public static Handler.Factory<WalkSpeedFlagHandler> FACTORY() {
        return new Factory();
    }

    public static class Factory extends Handler.Factory<WalkSpeedFlagHandler> {
        @Override
        public WalkSpeedFlagHandler create(Session session) {
            return new WalkSpeedFlagHandler(session);
        }
    }

    protected WalkSpeedFlagHandler(Session session) {
        super(session, BMEssentials.SET_WALK_SPEED_FLAG);
    }

    @Override
    protected float getSpeed(Player player) {
        return player.getWalkSpeed();
    }

    @Override
    protected void setSpeed(Player player, float speed) {
        player.setWalkSpeed(speed);
    }
}
