package at.sleazlee.bmessentials.PurpurFeatures;

import io.papermc.paper.event.block.BeaconActivatedEvent;
import org.bukkit.block.Beacon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Modifies beacon ranges when activated to match custom Purpur settings.
 */
public class BeaconRangeModifier implements Listener {

    /**
     * Custom effect ranges per beacon tier.
     * Index corresponds to tier number.
     */
    private static final double[] EFFECT_RANGES = {0, 32, 48, 64, 80};

    /**
     * Apply custom range whenever a beacon becomes active.
     *
     * @param event the beacon activation event
     */
    @EventHandler
    public void onBeaconActivated(BeaconActivatedEvent event) {
        Beacon beacon = event.getBeacon();
        int tier = beacon.getTier();
        if (tier > 0 && tier < EFFECT_RANGES.length) {
            beacon.setEffectRange(EFFECT_RANGES[tier]);
            beacon.update();
        }
    }
}