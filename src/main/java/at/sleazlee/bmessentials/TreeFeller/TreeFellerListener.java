package at.sleazlee.bmessentials.TreeFeller;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Set;

/**
 * Listener that intercepts log breaking and delegates tree felling to mcMMO
 * when appropriate.
 */
public class TreeFellerListener implements Listener {
    private final TreeFellerManager manager;
    private final Method getPlayerMethod;
    private final Method getWoodcuttingManagerMethod;
    private final Method processTreeFellerMethod;
    private final Method getUserBlockTrackerMethod;
    private final Method isIneligibleMethod;

    private static final Set<Material> AXES = EnumSet.of(
        Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
        Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE
    );

    public TreeFellerListener(TreeFellerManager manager) {
        this.manager = manager;
        Method getPlayer = null;
        Method getManager = null;
        Method process = null;
        Method getTracker = null;
        Method isIneligible = null;
        try {
            Class<?> userManager = Class.forName("com.gmail.nossr50.util.player.UserManager");
            getPlayer = userManager.getMethod("getPlayer", Player.class);
            Class<?> mcMMOPlayer = Class.forName("com.gmail.nossr50.datatypes.player.McMMOPlayer");
            getManager = mcMMOPlayer.getMethod("getWoodcuttingManager");
            Class<?> woodcuttingManager = Class.forName("com.gmail.nossr50.skills.woodcutting.WoodcuttingManager");
            process = woodcuttingManager.getMethod("processTreeFeller", Block.class);
            Class<?> mcMMOClass = Class.forName("com.gmail.nossr50.mcMMO");
            getTracker = mcMMOClass.getMethod("getUserBlockTracker");
            Class<?> trackerClass = Class.forName("com.gmail.nossr50.util.blockmeta.UserBlockTracker");
            isIneligible = trackerClass.getMethod("isIneligible", Block.class);
        } catch (ReflectiveOperationException e) {
            manager.getPlugin().getLogger().warning("Failed to load mcMMO TreeFeller hooks: " + e.getMessage());
        }
        this.getPlayerMethod = getPlayer;
        this.getWoodcuttingManagerMethod = getManager;
        this.processTreeFellerMethod = process;
        this.getUserBlockTrackerMethod = getTracker;
        this.isIneligibleMethod = isIneligible;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!manager.isEnabled(player)) {
            return;
        }
        if (player.isSneaking()) {
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || !AXES.contains(hand.getType())) {
            return;
        }

        Block block = event.getBlock();
        if (!isLog(block.getType())) {
            return;
        }

        Plugin mcmmo = player.getServer().getPluginManager().getPlugin("mcMMO");
        if (mcmmo == null || !mcmmo.isEnabled()) {
            return;
        }

        if (invokeTreeFeller(player, block, event)) {
            // tree feller handled the break
        }
    }

    private static boolean isLog(Material mat) {
        return mat.name().endsWith("_LOG") || mat.name().endsWith("_STEM");
    }

    /**
     * Uses reflection to invoke mcMMO's tree feller logic on the given block.
     */
    private boolean invokeTreeFeller(Player player, Block block, BlockBreakEvent event) {
        if (getPlayerMethod == null || getWoodcuttingManagerMethod == null || processTreeFellerMethod == null
                || getUserBlockTrackerMethod == null || isIneligibleMethod == null) {
            return false;
        }
        try {
            Object tracker = getUserBlockTrackerMethod.invoke(null);
            boolean placed = (Boolean) isIneligibleMethod.invoke(tracker, block);
            if (placed) {
                return false;
            }

            Object mcMMOPlayer = getPlayerMethod.invoke(null, player);
            Object woodcuttingManager = getWoodcuttingManagerMethod.invoke(mcMMOPlayer);
            processTreeFellerMethod.invoke(woodcuttingManager, block);
            event.setCancelled(true);
            return true;
        } catch (ReflectiveOperationException e) {
            // mcMMO might not be present or API changed; fail silently
            return false;
        }
    }
}
