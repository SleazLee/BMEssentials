package at.sleazlee.bmessentials;

import at.sleazlee.bmessentials.AltarSystem.AltarManager;
import at.sleazlee.bmessentials.AltarSystem.HealingSprings;
import at.sleazlee.bmessentials.CommandQueue.CommandQueueCommandExecutor;
import at.sleazlee.bmessentials.CommandQueue.CommandQueueManager;
import at.sleazlee.bmessentials.Containers.*;
import at.sleazlee.bmessentials.Help.HelpBooks;
import at.sleazlee.bmessentials.Help.Commands.BookCommand;
import at.sleazlee.bmessentials.Help.Commands.CommandsCommand;
import at.sleazlee.bmessentials.Help.HelpCommands;
import at.sleazlee.bmessentials.Migrator.MigratorManager;
import at.sleazlee.bmessentials.SpawnSystems.*;
import at.sleazlee.bmessentials.art.Art;
import at.sleazlee.bmessentials.rankup.RankUpManager;
import at.sleazlee.bmessentials.trophyroom.*;
import at.sleazlee.bmessentials.vot.*;
import at.sleazlee.bmessentials.bmefunctions.*;
import at.sleazlee.bmessentials.bungeetell.BungeeTellCommand;
import at.sleazlee.bmessentials.maps.*;
import at.sleazlee.bmessentials.tpshop.*;
import at.sleazlee.bmessentials.votesystem.*;
import at.sleazlee.bmessentials.wild.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The main class for the BMEssentials plugin, which extends JavaPlugin.
 * This class initializes and manages various systems within the plugin,
 * such as TPShop, Vote System, Wild System, Spawn Systems, and more.
 */
public class BMEssentials extends JavaPlugin {

    /** The plugin configuration. */
    private FileConfiguration config = getConfig();

    /** The main instance of the plugin. */
    private static BMEssentials main;

    /** The RankUpManager for handling rank-up functionality. */
    private RankUpManager rankUpManager;

    /** The economy service provider. */
    private Economy economy = null;

    /** The database for the trophy system. */
    private TrophyDatabase trophiesDB;

    /** The menu GUI for the trophy system. */
    private TrophyMenu trophyGUI;

    /** The manager for the migrator system. */
    private MigratorManager migratorManager;

    private CommandQueueManager queueManager;

    /**
     * Gets the instance of the main plugin class.
     *
     * @return the plugin instance
     */
    public static BMEssentials getInstance() {
        return getPlugin(BMEssentials.class);
    }

    /**
     * Called when the plugin is enabled. Initializes various systems based on the configuration.
     */
    @Override
    public void onEnable() {

        main = this;

        // Creates a new config.yml if it doesn't exist, copies from your resource.
        this.saveDefaultConfig();

        // Art at the beginning.
        String[] startArt = Art.startupArt().split("\n");
        for (String line : startArt) {
            getServer().getConsoleSender().sendMessage(ChatColor.AQUA + line);
        }

        // TPShop System
        if (config.getBoolean("Systems.TPShop.Enabled")) {
            this.getCommand("tpshop").setExecutor(new TPShopCommand());
            this.getCommand("tpshop").setTabCompleter(new TPShopTabCompleter());

            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled TPShop");
        }

        // Vote System
        if (config.getBoolean("Systems.VoteSystem.Enabled")) {
            this.getCommand("adminvote").setExecutor(new BMVote(this));
            this.getCommand("adminvote").setTabCompleter(new TestVoteTabCompleter());

            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled Vote Systems");
        }

        // Wild System
        if (config.getBoolean("Systems.Wild.Enabled")) {
            // Create an instance of WildData with the plugin's configuration and plugin instance
            WildData wildData = new WildData(this.getConfig(), this);

            // Register the NoFallDamage event listener.
            getServer().getPluginManager().registerEvents(new NoFallDamage(this), this);

            // Instantiate the command executor and tab completer, passing the WildData instance.
            WildCommand wildCommand = new WildCommand(wildData, this);
            WildTabCompleter wildTabCompleter = new WildTabCompleter(wildData);

            // Register the /wild command executor and tab completer.
            this.getCommand("wild").setExecutor(wildCommand);
            this.getCommand("wild").setTabCompleter(wildTabCompleter);

            // Add the system enabled message to the console.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled Wild Systems");
        }

        // Spawn Systems
        if (config.getBoolean("Systems.SpawnSystems.Enabled")) {
            this.getCommand("firstjoinmessage").setExecutor(new FirstJoinCommand(this));
            this.getCommand("springsheal").setExecutor(new HealCommand(this));
            this.getCommand("mcmmoboost").setExecutor(new McMMOBoost(this));
            this.getCommand("diamondcatch").setExecutor(new DiamondCatch(this));

            AltarManager altarManager = new AltarManager(this);
            getServer().getPluginManager().registerEvents(altarManager, this);
            HealingSprings.startHealingSpringsAmbient(this);

            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled Spawn Systems");
        }

        // Common Commands
        if (config.getBoolean("Systems.CommonCommands.Enabled")) {
            this.getCommand("playtime").setExecutor(new CommonCommands(this));
            this.getCommand("lag").setExecutor(new CommonCommands(this));
            this.getCommand("bmdiscord").setExecutor(new CommonCommands(this));

            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled All Common Commands");
        }

        // Virtual Containers System
        if (config.getBoolean("Systems.Containers.Enabled")) {

            // Trash
            this.getCommand("trash").setExecutor(new TrashCommand());

            // Crafting Table
            this.getCommand("craft").setExecutor(new CraftCommand());

            // Ender Chest
            this.getCommand("enderchest").setExecutor(new eChestCommand());

            // Cartography Table
            this.getCommand("cartography").setExecutor(new cartographyTableCommand());

            // Loom
            this.getCommand("loom").setExecutor(new LoomCommand());

            // Stone Cutter
            this.getCommand("stonecutter").setExecutor(new StoneCutterCommand());

            // Smithing Table
            this.getCommand("smithing").setExecutor(new SmithingTableCommand());

            // Grindstone
            this.getCommand("grindstone").setExecutor(new GrindStoneCommand());

            // Anvil
            this.getCommand("anvil").setExecutor(new AnvilCommand());

            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled Virtual Containers System");
        }

        // BungeeTell System
        if (config.getBoolean("Systems.BungeeTell.Enabled")) {
            this.getCommand("bungeetell").setExecutor(new BungeeTellCommand(this));
            this.getServer().getMessenger().registerOutgoingPluginChannel(this, "bmessentials:bungeetell");

            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled BungeeTell System");
        }

        // Map System
        if (config.getBoolean("Systems.Maps.Enabled")) {

            this.getCommand("map").setExecutor(new MapCommand(this));
            this.getCommand("map").setTabCompleter(new MapTabCompleter());
            this.getCommand("maps").setExecutor(new MapCommand(this));
            this.getCommand("maps").setTabCompleter(new MapTabCompleter());

            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled Maps");
        }

        // Donation System
        if (config.getBoolean("Systems.Donations.Enabled")) {

            this.getCommand("donation").setExecutor(new DonationCommand(this));

            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled Donation Systems");
        }

        // Trophy System
        if (config.getBoolean("Systems.Trophies.Enabled")) {
            // Ensure the plugin's data folder exists
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }

            // Initialize the trophies database
            trophiesDB = new TrophyDatabase(this);

            // Initialize the menu system
            trophyGUI = new TrophyMenu(this, trophiesDB);

            // Register the command executor
            getCommand("trophy").setExecutor(new TrophyCommand(this, trophiesDB, trophyGUI));

            // Register PlaceholderAPI expansion
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new TrophyRoomPlaceholderExpansion(this, trophiesDB).register();
            } else {
                getLogger().warning("PlaceholderAPI not found. Placeholders will not be available.");
            }
            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled the Trophy Systems");
        }

        // Migrator System
        if (config.getBoolean("Systems.Migrator.Enabled")) {
            migratorManager = new MigratorManager(this);
            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled the Migrator System");
        }

        // CommandQueue System
        if (config.getBoolean("Systems.CommandQueue.Enabled")) {

            this.queueManager = new CommandQueueManager(this);

            // Register command executor
            this.getCommand("commandqueue").setExecutor(new CommandQueueCommandExecutor(this, queueManager));

            // Load commands from CommandQueue.yml
            queueManager.loadCommands();

            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled the CommandQueue System");
        }


        // Vot System
        if (getConfig().getBoolean("Systems.Vot.Enabled")) {
            getCommand("vot").setExecutor(new VoteCommand());
            getServer().getPluginManager().registerEvents(new PlayerEventListener(), this);
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled Vot System");
        }

        // RankUp System
        if (getConfig().getBoolean("Systems.Rankup.Enabled")) {

            if (!setupEconomy()) {
                getLogger().severe("Disabled due to no Vault dependency found!");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            this.rankUpManager = new RankUpManager(this, economy);
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled the RankUp System");
        }

        // Enable Book Systems
        if (getConfig().getBoolean("Systems.Help.Books.Enabled")) {
            HelpBooks books = new HelpBooks(this);
            getCommand("book").setExecutor(new BookCommand(books));
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled the Book Systems");
        }

        // Enable Commands System
        if (getConfig().getBoolean("Systems.Help.Commands.Enabled")) {
            HelpCommands commandsSystem = new HelpCommands(this);
            getCommand("commands").setExecutor(new CommandsCommand(commandsSystem));
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled the Commands System");
        }

        // Finally enables the reload system.
        this.getCommand("bme").setExecutor(new BMECommandExecutor(this));

        // Finish the message at the very end and show it
        getServer().getConsoleSender().sendMessage("\n §b" + ChatColor.AQUA + " BMEssentials was successfully" + ChatColor.GREEN + " Enabled" + ChatColor.AQUA + "!" + "\n §b");
    }

    /**
     * Called when the plugin is disabled. Performs cleanup tasks.
     */
    @Override
    public void onDisable() {
        // Log a message to indicate the plugin is being disabled
        getLogger().info("Disabling BMEssentials...");

        // Close the Trophies Database Connection
        if (trophiesDB != null) {
            trophiesDB.close();
        }

        // Close the Player Migration Database Connection
        if (migratorManager != null) {
            migratorManager.shutdown();
        }

        // Log a message to indicate the plugin has been successfully disabled
        getLogger().info("BMEssentials has been disabled!");
    }

    /**
     * Sets up the economy service using Vault.
     *
     * @return true if the economy service was successfully set up, false otherwise
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault plugin not found!");
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().severe("No economy provider found via Vault!");
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    /**
     * Retrieves the economy service provider.
     *
     * @return the Economy provider, or null if not found
     */
    private Economy getEconomyService() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return null;
        }
        return rsp.getProvider();
    }
}
