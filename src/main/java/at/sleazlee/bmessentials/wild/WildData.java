package at.sleazlee.bmessentials.wild;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Logger;

/**
 * The WildData class handles loading and storing coordinate bounds for different Minecraft versions
 * from the plugin's configuration file.
 */
public class WildData {

    /**
     * Inner class representing the coordinate bounds for a version.
     */
    public static class CoordinateBounds {
        private final double lower;
        private final double upper;

        /**
         * Constructs a CoordinateBounds object with specified lower and upper bounds.
         *
         * @param lower The lower bound coordinate.
         * @param upper The upper bound coordinate.
         */
        public CoordinateBounds(double lower, double upper) {
            this.lower = lower;
            this.upper = upper;
        }

        /**
         * Gets the lower bound coordinate.
         *
         * @return The lower bound.
         */
        public double getLower() {
            return lower;
        }

        /**
         * Gets the upper bound coordinate.
         *
         * @return The upper bound.
         */
        public double getUpper() {
            return upper;
        }
    }

    private final Map<String, CoordinateBounds> versionBounds = new HashMap<>(); // Stores bounds for each version.
    private final List<String> versions = new ArrayList<>(); // List of version strings.
    private final Logger logger; // Logger for logging information.

    /**
     * Constructs a WildData object and loads bounds from the configuration file.
     *
     * @param config The plugin's configuration file.
     * @param plugin The main plugin instance.
     */
    public WildData(FileConfiguration config, JavaPlugin plugin) {
        this.logger = plugin.getLogger();

        // Log the entire configuration to check if it's loaded
        logger.info("Config contents: " + config.saveToString());

        ConfigurationSection boundsSection = config.getConfigurationSection("Systems.Wild.Bounds");
        if (boundsSection != null) {
            logger.info("Bounds section found.");

            // Iterate over each key under 'bounds' (e.g., '1', '2', etc.).
            for (String key : boundsSection.getKeys(false)) {
                logger.info("Processing key: " + key);
                // Access the configuration section for each version entry.
                ConfigurationSection versionSection = boundsSection.getConfigurationSection(key);
                if (versionSection != null) {
                    // Get the version string (e.g., "1.19") from the 'Version' field.
                    String version = versionSection.getString("Version");
                    double lower = versionSection.getDouble("Lower");
                    double upper = versionSection.getDouble("Upper");

                    if (version != null) {

                        // Create a CoordinateBounds object and store it in the map.
                        CoordinateBounds bounds = new CoordinateBounds(lower, upper);
                        versionBounds.put(version, bounds);

                        // Add the version string to the list of versions.
                        versions.add(version);
                    } else {
                        logger.warning("[Wild] Version key is missing in bounds entry: " + key);
                    }
                } else {
                    logger.warning("[Wild] Version section for key " + key + " is null");
                }
            }
        } else {
            logger.warning("[Wild] Bounds section is null");
        }
    }

    /**
     * Retrieves the coordinate bounds for a given version.
     *
     * @param version The version string.
     * @return The CoordinateBounds object for the version, or null if not found.
     */
    public CoordinateBounds getBounds(String version) {
        return versionBounds.get(version);
    }

    /**
     * Gets the list of available version strings.
     *
     * @return List of version strings.
     */
    public List<String> getVersions() {
        return versions;
    }
}
