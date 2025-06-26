package at.sleazlee.bmessentials.bmefunctions;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.session.SessionManager;

public class WalkSpeedSystem {
    public WalkSpeedSystem() {
        SessionManager manager = WorldGuard.getInstance().getPlatform().getSessionManager();
        manager.registerHandler(WalkSpeedFlagHandler.FACTORY(), null);
    }
}
