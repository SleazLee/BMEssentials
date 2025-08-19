package at.sleazlee.bmessentials.Combinations;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import at.sleazlee.bmessentials.Scheduler;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Handles loading, saving and matching of custom item combinations.
 *
 * Currently only supports Anvil combinations but is written to be easily
 * extended to other combination types in the future.
 */
public class Combinations {
    private final JavaPlugin plugin;
    private final Map<String, AnvilCombination> anvilCombinations = new HashMap<>();
    private final Map<String, CraftingCombination> craftingCombinations = new HashMap<>();
    private final Map<String, SmeltingCombination> smeltingCombinations = new HashMap<>();

    private final File file;
    private final FileConfiguration config;

    public Combinations(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "combinations.yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    /**
     * Loads combinations from the configuration file.
     */
    private void load() {
        anvilCombinations.clear();
        craftingCombinations.clear();
        smeltingCombinations.clear();

        ConfigurationSection anvilSection = config.getConfigurationSection("anvil");
        if (anvilSection != null) {
            for (String key : anvilSection.getKeys(false)) {
                ItemStack first = anvilSection.getItemStack(key + ".first");
                ItemStack second = anvilSection.getItemStack(key + ".second");
                ItemStack result = anvilSection.getItemStack(key + ".result");
                if (first != null && second != null && result != null) {
                    anvilCombinations.put(key.toLowerCase(), new AnvilCombination(first, second, result));
                }
            }
        }

        ConfigurationSection craftSection = config.getConfigurationSection("crafting");
        if (craftSection != null) {
            for (String key : craftSection.getKeys(false)) {
                List<?> list = craftSection.getList(key + ".matrix");
                ItemStack result = craftSection.getItemStack(key + ".result");
                if (list != null && list.size() == 9 && result != null) {
                    ItemStack[] matrix = new ItemStack[9];
                    for (int i = 0; i < 9; i++) {
                        matrix[i] = (ItemStack) list.get(i);
                    }
                    craftingCombinations.put(key.toLowerCase(), new CraftingCombination(matrix, result));
                }
            }
        }

        ConfigurationSection smeltSection = config.getConfigurationSection("smelting");
        if (smeltSection != null) {
            for (String key : smeltSection.getKeys(false)) {
                ItemStack input = smeltSection.getItemStack(key + ".input");
                ItemStack result = smeltSection.getItemStack(key + ".result");
                if (input != null && result != null) {
                    smeltingCombinations.put(key.toLowerCase(), new SmeltingCombination(input, result));
                }
            }
        }
    }

    /**
     * Writes all registered combinations to the YAML file.
     */
    private void writeFile() {
        synchronized (config) {
            config.set("anvil", null);
            ConfigurationSection anvilSection = config.createSection("anvil");
            for (Map.Entry<String, AnvilCombination> entry : anvilCombinations.entrySet()) {
                ConfigurationSection sec = anvilSection.createSection(entry.getKey());
                sec.set("first", entry.getValue().first);
                sec.set("second", entry.getValue().second);
                sec.set("result", entry.getValue().result);
            }

            config.set("crafting", null);
            ConfigurationSection craftSection = config.createSection("crafting");
            for (Map.Entry<String, CraftingCombination> entry : craftingCombinations.entrySet()) {
                ConfigurationSection sec = craftSection.createSection(entry.getKey());
                sec.set("matrix", Arrays.asList(entry.getValue().matrix));
                sec.set("result", entry.getValue().result);
            }

            config.set("smelting", null);
            ConfigurationSection smeltSection = config.createSection("smelting");
            for (Map.Entry<String, SmeltingCombination> entry : smeltingCombinations.entrySet()) {
                ConfigurationSection sec = smeltSection.createSection(entry.getKey());
                sec.set("input", entry.getValue().input);
                sec.set("result", entry.getValue().result);
            }
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save combinations.yml: " + e.getMessage());
            }
        }
    }

    /**
     * Saves combinations to disk asynchronously.
     */
    public void save() {
        // Perform disk writes off the main thread to avoid blocking game ticks
        Scheduler.runAsync(this::writeFile);
    }

    /**
     * Saves combinations to disk synchronously. Used during plugin shutdown
     * to guarantee that pending changes are written before the JVM exits.
     */
    public void saveSync() {
        writeFile();
    }

    /**
     * Creates or replaces an anvil combination.
     */
    public void createAnvil(String name, ItemStack first, ItemStack second, ItemStack result) {
        anvilCombinations.put(name.toLowerCase(), new AnvilCombination(first, second, result));
        save();
    }

    /**
     * Deletes an anvil combination.
     *
     * @param name Name of the combination to delete.
     * @return true if removed
     */
    public boolean deleteAnvil(String name) {
        boolean removed = anvilCombinations.remove(name.toLowerCase()) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    /**
     * Attempts to find a matching anvil combination for the provided items.
     *
     * @param left  first slot item
     * @param right second slot item
     * @return matching combination or null
     */
    public AnvilCombination matchAnvil(ItemStack left, ItemStack right) {
        for (AnvilCombination c : anvilCombinations.values()) {
            if (c.matches(left, right)) {
                return c;
            }
        }
        return null;
    }

    /**
     * @return immutable set of registered anvil combination names
     */
    public Set<String> getAnvilNames() {
        return Collections.unmodifiableSet(anvilCombinations.keySet());
    }

    /**
     * Represents a single combination.
     */
    public static class AnvilCombination {
        private final ItemStack first;
        private final ItemStack second;
        private final ItemStack result;

        public AnvilCombination(ItemStack first, ItemStack second, ItemStack result) {
            this.first = first;
            this.second = second;
            this.result = result;
        }

        public ItemStack getResult() {
            return result.clone();
        }

        public boolean matches(ItemStack a, ItemStack b) {
            return (a.isSimilar(first) && b.isSimilar(second)) ||
                    (a.isSimilar(second) && b.isSimilar(first));
        }
    }

    public static class CraftingCombination {
        private final ItemStack[] matrix;
        private final ItemStack result;

        public CraftingCombination(ItemStack[] matrix, ItemStack result) {
            this.matrix = matrix;
            this.result = result;
        }

        public ItemStack getResult() {
            return result.clone();
        }

        public boolean matches(ItemStack[] other) {
            if (other.length != 9) {
                return false;
            }
            for (int i = 0; i < 9; i++) {
                ItemStack expected = matrix[i];
                ItemStack given = other[i];
                boolean expEmpty = expected == null || expected.getType().isAir();
                boolean givenEmpty = given == null || given.getType().isAir();
                if (expEmpty && givenEmpty) {
                    continue;
                }
                if (expEmpty != givenEmpty) {
                    return false;
                }
                if (!given.isSimilar(expected)) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class SmeltingCombination {
        private final ItemStack input;
        private final ItemStack result;

        public SmeltingCombination(ItemStack input, ItemStack result) {
            this.input = input;
            this.result = result;
        }

        public ItemStack getResult() {
            return result.clone();
        }

        public boolean matches(ItemStack source) {
            return source.isSimilar(input);
        }
    }

    public void createCrafting(String name, ItemStack[] matrix, ItemStack result) {
        craftingCombinations.put(name.toLowerCase(), new CraftingCombination(matrix, result));
        save();
    }

    public boolean deleteCrafting(String name) {
        boolean removed = craftingCombinations.remove(name.toLowerCase()) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    public CraftingCombination matchCrafting(ItemStack[] matrix) {
        for (CraftingCombination c : craftingCombinations.values()) {
            if (c.matches(matrix)) {
                return c;
            }
        }
        return null;
    }

    public Set<String> getCraftingNames() {
        return Collections.unmodifiableSet(craftingCombinations.keySet());
    }

    public void createSmelting(String name, ItemStack input, ItemStack result) {
        smeltingCombinations.put(name.toLowerCase(), new SmeltingCombination(input, result));
        save();
    }

    public boolean deleteSmelting(String name) {
        boolean removed = smeltingCombinations.remove(name.toLowerCase()) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    public SmeltingCombination matchSmelting(ItemStack input) {
        for (SmeltingCombination c : smeltingCombinations.values()) {
            if (c.matches(input)) {
                return c;
            }
        }
        return null;
    }

    public Set<String> getSmeltingNames() {
        return Collections.unmodifiableSet(smeltingCombinations.keySet());
    }
}