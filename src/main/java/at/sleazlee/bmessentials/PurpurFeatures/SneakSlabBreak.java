package at.sleazlee.bmessentials.PurpurFeatures;


import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class SneakSlabBreak implements Listener {

    /**
     * Handles block break events. If a sneaking player breaks a double slab,
     * this method computes the hit point of the player's view ray on the block.
     * Based on the vertical (Y) coordinate of that intersection relative to the block,
     * it determines whether to remove the top or bottom slab.
     *
     * @param event the block break event triggered when a player attempts to break a block.
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Only proceed if the player is sneaking.
        if (!player.isSneaking()) {
            return;
        }

        Block block = event.getBlock();

        // Ensure the block is a slab.
        if (!(block.getBlockData() instanceof Slab)) {
            return;
        }

        Slab slabData = (Slab) block.getBlockData();

        // Process only double slabs.
        if (slabData.getType() != Slab.Type.DOUBLE) {
            return;
        }

        // Cancel the normal break event to prevent the entire block from being removed.
        event.setCancelled(true);

        // Calculate the intersection of the player's view ray with the block's bounding box.
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();
        Vector hitPoint = getRayIntersection(eyeLocation, direction, block);

        /*
         * Determine which slab to remove:
         * If hitPoint is null, default to removing the top half (keeping the bottom).
         * Otherwise, compare the hit's Y coordinate relative to the block.
         */
        if (hitPoint == null) {
            slabData.setType(Slab.Type.BOTTOM);
        } else {
            double relativeY = hitPoint.getY() - block.getY();
            // If the hit is on or above 0.5 (top half), remove the top slab.
            if (relativeY >= 0.5) {
                slabData.setType(Slab.Type.BOTTOM);
            } else {
                slabData.setType(Slab.Type.TOP);
            }
        }

        // Update the block state.
        block.setBlockData(slabData);

        // Drop a slab item at the block's location.
        block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(block.getType(), 1));
    }

    /**
     * Performs a ray-box intersection to find where the player's view ray intersects
     * the block's axis-aligned bounding box.
     *
     * @param eyeLocation the starting location of the ray (player's eye location).
     * @param direction the normalized direction vector of the player's view.
     * @param block the block whose bounding box is used for the intersection test.
     * @return the intersection point as a Vector, or null if no intersection is found.
     */
    private Vector getRayIntersection(Location eyeLocation, Vector direction, Block block) {
        double ox = eyeLocation.getX();
        double oy = eyeLocation.getY();
        double oz = eyeLocation.getZ();

        double bx = block.getX();
        double by = block.getY();
        double bz = block.getZ();

        double bxMax = bx + 1;
        double byMax = by + 1;
        double bzMax = bz + 1;

        double tMin = Double.NEGATIVE_INFINITY;
        double tMax = Double.POSITIVE_INFINITY;

        // X axis
        if (direction.getX() != 0) {
            double tx1 = (bx - ox) / direction.getX();
            double tx2 = (bxMax - ox) / direction.getX();
            double tXMin = Math.min(tx1, tx2);
            double tXMax = Math.max(tx1, tx2);
            tMin = Math.max(tMin, tXMin);
            tMax = Math.min(tMax, tXMax);
        } else {
            // Ray is parallel to the x-axis; check if the origin is within the slab.
            if (ox < bx || ox > bxMax) {
                return null;
            }
        }

        // Y axis
        if (direction.getY() != 0) {
            double ty1 = (by - oy) / direction.getY();
            double ty2 = (byMax - oy) / direction.getY();
            double tYMin = Math.min(ty1, ty2);
            double tYMax = Math.max(ty1, ty2);
            tMin = Math.max(tMin, tYMin);
            tMax = Math.min(tMax, tYMax);
        } else {
            if (oy < by || oy > byMax) {
                return null;
            }
        }

        // Z axis
        if (direction.getZ() != 0) {
            double tz1 = (bz - oz) / direction.getZ();
            double tz2 = (bzMax - oz) / direction.getZ();
            double tZMin = Math.min(tz1, tz2);
            double tZMax = Math.max(tz1, tz2);
            tMin = Math.max(tMin, tZMin);
            tMax = Math.min(tMax, tZMax);
        } else {
            if (oz < bz || oz > bzMax) {
                return null;
            }
        }

        // If there is no valid intersection, return null.
        if (tMax < 0 || tMin > tMax) {
            return null;
        }

        double tHit = tMin < 0 ? tMax : tMin;
        return eyeLocation.toVector().add(direction.clone().multiply(tHit));
    }
}
