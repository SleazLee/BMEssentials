package at.sleazlee.bmessentials.wild;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Logger;

/**
 * The WildData class handles loading and storing the "Bounds" data for each version.
 *
 * <p>Each version has:
 * <ul>
 *   <li>Version: e.g. "1.19"</li>
 *   <li>Lower: double, the inner radius threshold</li>
 *   <li>Upper: double, the outer radius threshold</li>
 * </ul>
 *
 * <p>By default, this code assumes a center of (256,256). Adjust if needed.</p>
 */
public class WildData {

    /**
     * CoordinateBounds represents a single ring in terms of:
     * <p>distance in [lower, upper]</p>
     * <p>We measure distance using Chebyshev distance: max(|x-256|, |z-256|).</p>
     */
    public static class CoordinateBounds {
        private final double lower;
        private final double upper;

        /**
         * Constructs a new coordinate-bound ring.
         *
         * @param lower the inner threshold
         * @param upper the outer threshold
         */
        public CoordinateBounds(double lower, double upper) {
            this.lower = lower;
            this.upper = upper;
        }

        /**
         * @return The lower radius threshold
         */
        public double getLower() {
            return lower;
        }

        /**
         * @return The upper radius threshold
         */
        public double getUpper() {
            return upper;
        }
    }

    // A map from version string (e.g. "1.19") to its [lower, upper] ring
    private final Map<String, CoordinateBounds> versionBounds = new HashMap<>();
    // Keep a list of versions for iteration
    private final List<String> versions = new ArrayList<>();

    // Hard-coded center (change to read from config if needed)
    private double centerX = 256.0;
    private double centerZ = 256.0;

    private final Logger logger;

    /**
     * Constructs WildData by reading the "Systems.Wild.Bounds" section of config.
     *
     * @param config the plugin's FileConfiguration
     * @param plugin the main JavaPlugin for logging, etc.
     */
    public WildData(FileConfiguration config, JavaPlugin plugin) {
        this.logger = plugin.getLogger();

        // If you need a dynamic center, uncomment or add these lines:
        centerX = config.getDouble("Systems.Wild.Center.X", 256.0);
        centerZ = config.getDouble("Systems.Wild.Center.Z", 256.0);

        // Read the "Bounds" section
        ConfigurationSection boundsSection = config.getConfigurationSection("Systems.Wild.Bounds");
        if (boundsSection == null) {
            logger.warning("[Wild] Bounds section is null or missing in config.yml");
            return;
        }

        // For each numeric key (1, 2, 3...) under "Bounds"
        for (String key : boundsSection.getKeys(false)) {
            ConfigurationSection versionSec = boundsSection.getConfigurationSection(key);
            if (versionSec != null) {
                String versionName = versionSec.getString("Version");
                double lower = versionSec.getDouble("Lower");
                double upper = versionSec.getDouble("Upper");

                if (versionName != null) {
                    // Store the ring
                    versionBounds.put(versionName, new CoordinateBounds(lower, upper));
                    versions.add(versionName);
                } else {
                    logger.warning("[Wild] Missing 'Version' key in Bounds entry: " + key);
                }
            }
        }
    }

    /**
     * @return X-coordinate of the square center (default 256)
     */
    public double getCenterX() {
        return centerX;
    }

    /**
     * @return Z-coordinate of the square center (default 256)
     */
    public double getCenterZ() {
        return centerZ;
    }

    /**
     * Retrieve the [lower, upper] ring for a specific version.
     *
     * @param version the version string (e.g. "1.19")
     * @return the corresponding CoordinateBounds, or null if not found
     */
    public CoordinateBounds getBounds(String version) {
        return versionBounds.get(version);
    }

    /**
     * @return A list of all version strings found in "Bounds"
     */
    public List<String> getVersions() {
        return versions;
    }
}