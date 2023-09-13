package at.sleazlee.bmessentials.art;

import org.bukkit.Color;
import org.bukkit.Particle;

public class Art {

    public static String startupArt() {
        String bmeTextArt = "\n §b" +
                " ____  __  __ ______\n" +
                " |  _ \\|  \\/  |  ____|\n" +
                " | |_) | \\  / | |__\n" +
                " |  _ <| |\\/| |  __|\n" +
                " | |_) | |  | | |____\n" +
                " |____/|_|  |_|______|\n" +
                "\n §b";

        return bmeTextArt;
    }

    // Creates a DustOptions Object from a hex code.
    public static Particle.DustOptions createDustOptions(String hexCode) {
        // Parse RGB values from the hex color code
        int r = Integer.valueOf(hexCode.substring(1, 3), 16);
        int g = Integer.valueOf(hexCode.substring(3, 5), 16);
        int b = Integer.valueOf(hexCode.substring(5, 7), 16);

        // Create and return the DustOptions object
        return new Particle.DustOptions(Color.fromRGB(r, g, b), 1);
    }


}
