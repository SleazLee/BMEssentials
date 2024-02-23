package at.sleazlee.bmessentials.AltarSystem;

import at.sleazlee.bmessentials.BMEssentials;
import org.bukkit.Location;
import org.bukkit.World;

public class WishingWell {

    private int taskId;
    private boolean altarActivated = false;
    private String activeHexColor = "#32CA65";
    private static double theta = 0; // Class-level variable to keep track of the current angle

    // Methods for Wishing Well altar...
    static void playWishingWellAnimation(BMEssentials plugin, Location altarLocation) {
        World world = altarLocation.getWorld();
        Location sphereAboveAltar = new Location(world, -237.5, 72, 37.5);

        Location coneCenter = new Location(world, -237, 72.3, 37);
        Location wellCenter = new Location(world, -241, 72.3, 40);
        Location wellBottom = new Location(world, -241, 70, 40);
        Location coneTop = new Location(world, -237, 73, 37);
        Location altarCenter = new Location(world, -238, 72, 37);


        // Step 1: Initial Activation (Portal particle spiraling up)


        // Step 2: Particle pauses at the top and then shoots into the wishing well
                // Shoot into the center of the wishing well
                // Delay for the particle to reach the well

        // Step 3: Sphere spirals up from the bottom to the middle of the wishing well
                // Delay for the sphere to reach above the Altar
                    // Show the item above the Altar


        // Step 4: Sphere Emergence (End Rod particles)

        // Step 5: Sphere Movement (To above the Altar)

        // Step 6: Item Appearance

    }
}
