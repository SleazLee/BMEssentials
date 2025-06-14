package at.sleazlee.bmessentials.SimplePortals;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.SpawnSystems.HealCommand;
import at.sleazlee.bmessentials.wild.WildCommand;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.FlagConflictException;
import com.sk89q.worldguard.protection.flags.FlagRegistry;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.session.SessionManager;

/**
 * Registers custom WorldGuard flags and session handlers for simple portal functionality.
 */
public class SimplePortals {

    /** Flag that teleports players to the wild when they enter a region. */
    public static StateFlag SEND_TO_WILD;

    /** Flag that heals players when they enter the Healing Springs region. */
    public static StateFlag ENTERED_HEALING_SPRINGS;

    /**
     * Initialise the SimplePortals system.
     *
     * @param plugin       reference to the main plugin
     * @param wildCommand  instance of WildCommand used for teleportation
     * @param healCommand  instance of HealCommand used for healing
     */
    public SimplePortals(BMEssentials plugin, WildCommand wildCommand, HealCommand healCommand) {
        registerFlags();
        registerHandlers(wildCommand, healCommand);
    }

    private void registerFlags() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

        SEND_TO_WILD = new StateFlag("send-to-wild", false);
        ENTERED_HEALING_SPRINGS = new StateFlag("entered-healing-springs", false);

        try {
            registry.register(SEND_TO_WILD);
        } catch (FlagConflictException e) {
            SEND_TO_WILD = (StateFlag) registry.get("send-to-wild");
        }

        try {
            registry.register(ENTERED_HEALING_SPRINGS);
        } catch (FlagConflictException e) {
            ENTERED_HEALING_SPRINGS = (StateFlag) registry.get("entered-healing-springs");
        }
    }

    private void registerHandlers(WildCommand wildCommand, HealCommand healCommand) {
        SessionManager sessionManager = WorldGuard.getInstance().getPlatform().getSessionManager();
        sessionManager.registerHandler(new SendToWildHandler.Factory(wildCommand), null);
        sessionManager.registerHandler(new HealingSpringsHandler.Factory(healCommand), null);
    }
}
