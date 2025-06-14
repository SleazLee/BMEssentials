package at.sleazlee.bmessentials.SimplePortals;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.SpawnSystems.HealCommand;
import at.sleazlee.bmessentials.wild.WildCommand;
import at.sleazlee.bmessentials.wild.WildData;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.FlagConflictException;
import com.sk89q.worldguard.protection.flags.FlagRegistry;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.session.SessionManager;

/**
 * Initializes custom WorldGuard flags and session handlers for simple portal logic.
 */
public class SimplePortals {

    /** Flag that triggers a random wild teleport when entering a region. */
    public static final StateFlag SEND_TO_WILD = new StateFlag("send-to-wild", false);

    /** Flag that triggers healing when entering a region. */
    public static final StateFlag ENTERED_HEALING_SPRINGS = new StateFlag("entered-healing-springs", false);

    private final BMEssentials plugin;

    /**
     * Constructs the SimplePortals system and registers flags and handlers.
     *
     * @param plugin main plugin instance
     */
    public SimplePortals(BMEssentials plugin) {
        this.plugin = plugin;
        registerFlags();
        registerHandler();
    }

    private void registerFlags() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            registry.register(SEND_TO_WILD);
        } catch (FlagConflictException ignored) {
            plugin.getLogger().warning("send-to-wild flag already registered");
        }
        try {
            registry.register(ENTERED_HEALING_SPRINGS);
        } catch (FlagConflictException ignored) {
            plugin.getLogger().warning("entered-healing-springs flag already registered");
        }
    }

    private void registerHandler() {
        WildData wildData = new WildData(plugin);
        WildCommand wildCommand = new WildCommand(wildData, plugin);
        HealCommand healCommand = new HealCommand();

        SessionManager manager = WorldGuard.getInstance().getPlatform().getSessionManager();
        manager.registerHandler(new SimplePortalsFlagHandler.Factory(wildCommand, healCommand), null);
    }
}
