package at.sleazlee.bmessentials.wild;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * WildData class loads and provides access to the Wild system's configuration,
 * including version bounds and WorldGuard regions.
 */
public class WildData {

    private final double centerX;
    private final double centerZ;
    private final Map<String, CoordinateBounds> boundsMap;
    private final List<Region> regions;

    /**
     * Constructs a WildData object by loading configuration from the provided plugin.
     *
     * @param plugin The main plugin instance to access the configuration.
     */
    public WildData(JavaPlugin plugin) {
        // Load the "Systems.Wild" section from config.yml
        ConfigurationSection wildSection = plugin.getConfig().getConfigurationSection("Systems.Wild");
        if (wildSection == null) {
            throw new IllegalArgumentException("Systems.Wild section not found in config.yml");
        }

        // Load Center Coordinates
        this.centerX = wildSection.getDouble("Center.X", 256.0);
        this.centerZ = wildSection.getDouble("Center.Z", 256.0);

        // Load Bounds
        ConfigurationSection boundsSection = wildSection.getConfigurationSection("Bounds");
        if (boundsSection == null) {
            throw new IllegalArgumentException("Systems.Wild.Bounds section not found in config.yml");
        }

        this.boundsMap = new HashMap<>();
        for (String key : boundsSection.getKeys(false)) {
            ConfigurationSection versionSection = boundsSection.getConfigurationSection(key);
            if (versionSection != null) {
                String version = versionSection.getString("Version");
                double lower = versionSection.getDouble("Lower");
                double upper = versionSection.getDouble("Upper");
                if (version != null) {
                    boundsMap.put(version, new CoordinateBounds(lower, upper));
                }
            }
        }

        // Load Regions
        ConfigurationSection regionsSection = wildSection.getConfigurationSection("Regions");
        if (regionsSection == null) {
            this.regions = Collections.emptyList();
        } else {
            this.regions = new ArrayList<>();
            for (String key : regionsSection.getKeys(false)) {
                ConfigurationSection regionSection = regionsSection.getConfigurationSection(key);
                if (regionSection != null) {
                    String name = regionSection.getString("Name");
                    String returnValue = regionSection.getString("Return");
                    if (name != null && returnValue != null) {
                        regions.add(new Region(name, returnValue));
                    }
                }
            }
        }
    }

    /**
     * Retrieves the set of available version names.
     *
     * @return A Set containing all version names.
     */
    public Set<String> getVersions() {
        return boundsMap.keySet();
    }

    /**
     * Retrieves the CoordinateBounds for a given version.
     *
     * @param version The version name.
     * @return The CoordinateBounds object, or null if not found.
     */
    public CoordinateBounds getBounds(String version) {
        return boundsMap.get(version);
    }

    /**
     * Retrieves the X-coordinate of the center.
     *
     * @return Center X.
     */
    public double getCenterX() {
        return centerX;
    }

    /**
     * Retrieves the Z-coordinate of the center.
     *
     * @return Center Z.
     */
    public double getCenterZ() {
        return centerZ;
    }

    /**
     * Retrieves the list of configured Regions.
     *
     * @return List of Region objects.
     */
    public List<Region> getRegions() {
        return regions;
    }

    /**
     * Inner class representing the bounds of a version ring.
     */
    public static class CoordinateBounds {
        private final double lower;
        private final double upper;

        /**
         * Constructs a CoordinateBounds object with specified lower and upper bounds.
         *
         * @param lower The lower bound of the distance.
         * @param upper The upper bound of the distance.
         */
        public CoordinateBounds(double lower, double upper) {
            this.lower = lower;
            this.upper = upper;
        }

        /**
         * Retrieves the lower bound.
         *
         * @return Lower bound.
         */
        public double getLower() {
            return lower;
        }

        /**
         * Retrieves the upper bound.
         *
         * @return Upper bound.
         */
        public double getUpper() {
            return upper;
        }
    }

    /**
     * Inner class representing a WorldGuard region.
     */
    public static class Region {
        private final String name;
        private final String returnValue;

        /**
         * Constructs a Region object with specified name and return value.
         *
         * @param name        The name of the WorldGuard region.
         * @param returnValue The value to return when a player is within this region.
         */
        public Region(String name, String returnValue) {
            this.name = name;
            this.returnValue = returnValue;
        }

        /**
         * Retrieves the name of the region.
         *
         * @return Region name.
         */
        public String getName() {
            return name;
        }

        /**
         * Retrieves the return value associated with the region.
         *
         * @return Return value.
         */
        public String getReturnValue() {
            return returnValue;
        }
    }
}