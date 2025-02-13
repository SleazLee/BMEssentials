package at.sleazlee.bmessentials;

import at.sleazlee.bmessentials.AltarSystem.AltarManager;
import at.sleazlee.bmessentials.AltarSystem.Altars.HealingSprings;
import at.sleazlee.bmessentials.AltarSystem.Altars.Obelisk;
import at.sleazlee.bmessentials.AltarSystem.Altars.WishingWell;
import at.sleazlee.bmessentials.CommandQueue.CommandQueueCommandExecutor;
import at.sleazlee.bmessentials.CommandQueue.CommandQueueManager;
import at.sleazlee.bmessentials.CommandQueue.CommandQueueTabCompleter;
import at.sleazlee.bmessentials.Containers.*;
import at.sleazlee.bmessentials.EconomySystem.BMSEconomyProvider;
import at.sleazlee.bmessentials.EconomySystem.EconomyCommands;
import at.sleazlee.bmessentials.EconomySystem.LegacyEconomyProvider;
import at.sleazlee.bmessentials.Help.Commands.BookCommand;
import at.sleazlee.bmessentials.Help.Commands.CommandsCommand;
import at.sleazlee.bmessentials.Help.HelpBooks;
import at.sleazlee.bmessentials.Help.HelpCommands;
import at.sleazlee.bmessentials.PlayerData.BMEChatPlaceholders;
import at.sleazlee.bmessentials.PlayerData.BMEPlaceholders;
import at.sleazlee.bmessentials.PlayerData.PlayerDatabaseManager;
import at.sleazlee.bmessentials.PlayerData.PlayerJoinListener;
import at.sleazlee.bmessentials.SpawnSystems.FirstJoinCommand;
import at.sleazlee.bmessentials.SpawnSystems.HealCommand;
import at.sleazlee.bmessentials.art.Art;
import at.sleazlee.bmessentials.bmefunctions.*;
import at.sleazlee.bmessentials.VTell.VTellCommand;
import at.sleazlee.bmessentials.maps.MapCommand;
import at.sleazlee.bmessentials.maps.MapTabCompleter;
import at.sleazlee.bmessentials.rankup.RankUpManager;
import at.sleazlee.bmessentials.tpshop.TPShopCommand;
import at.sleazlee.bmessentials.tpshop.TPShopTabCompleter;
import at.sleazlee.bmessentials.trophyroom.*;
import at.sleazlee.bmessentials.vot.PlayerEventListener;
import at.sleazlee.bmessentials.vot.VotTabCompleter;
import at.sleazlee.bmessentials.vot.VoteCommand;
import at.sleazlee.bmessentials.votesystem.BMVote;
import at.sleazlee.bmessentials.votesystem.TestVoteTabCompleter;
import at.sleazlee.bmessentials.wild.*;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The main class for the BMEssentials plugin, which extends JavaPlugin.
 * This class initializes and manages various systems within the plugin,
 * such as TPShop, Vote System, Wild System, Spawn Systems, and more.
 */
public class BMEssentials extends JavaPlugin {

    /** The main instance of the plugin. */
    private static BMEssentials main;

    /** The RankUpManager for handling rank-up functionality. */
    private RankUpManager rankUpManager;

    /** The database for the trophy system. */
    private TrophyDatabase trophiesDB;

    /** The database for the PlayerData system.
     * -- GETTER --
     *  Gets the instance of the DatabaseManager.
     *
     * @return the database manager
     */
    @Getter
    private PlayerDatabaseManager PlayerDataDBManager;

    /** The menu GUI for the trophy system. */
    private TrophyMenu trophyGUI;

    private CommandQueueManager queueManager;
    private FileConfiguration config;

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

        // Reload the configuration to ensure it's loaded into memory
        this.reloadConfig();

        // Now, initialize the config variable with the loaded configuration
        this.config = getConfig();

        // Art at the beginning.
        String[] startArt = Art.startupArt().split("\n");
        for (String line : startArt) {
            getServer().getConsoleSender().sendMessage(ChatColor.AQUA + line);
        }

        // Enable PlayerData Systems
        if (getConfig().getBoolean("Systems.PlayerData.Enabled")) {
            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled PlayerData Systems");

            // Initialize the database manager
            PlayerDataDBManager = new PlayerDatabaseManager(this);
            PlayerDataDBManager.initializeDatabase();

            // Register the player join event listener
            getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

            // Register PlaceholderAPI expansion if PlaceholderAPI is present
            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new BMEPlaceholders(this).register();
                new BMEChatPlaceholders(this).register();
            } else {
                getLogger().warning("PlaceholderAPI not found! Placeholders will not work.");
            }

            // Enable Economy Systems
            if (getConfig().getBoolean("Systems.PlayerData.EconomySystems.Enabled")) {
                getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled Economy Systems");

                // 1. Register our BMSEconomyProvider as an Economy
                BMSEconomyProvider economyProvider = new BMSEconomyProvider(this, PlayerDataDBManager);
                getServer().getServicesManager().register(
                        net.milkbowl.vault2.economy.Economy.class,
                        economyProvider,
                        this,
                        ServicePriority.High
                );

                // Register legacy Vault provider
                LegacyEconomyProvider legacyEconomyProvider = new LegacyEconomyProvider(this, PlayerDataDBManager);
                getServer().getServicesManager().register(net.milkbowl.vault.economy.Economy.class, legacyEconomyProvider, this, ServicePriority.High);

                // 2. Register the commands
                getCommand("pay").setExecutor(new EconomyCommands(this));
                getCommand("bal").setExecutor(new EconomyCommands(this));
                getCommand("money").setExecutor(new EconomyCommands(this));
                getCommand("baltop").setExecutor(new EconomyCommands(this));
                getCommand("moneytop").setExecutor(new EconomyCommands(this));
                getCommand("eco").setExecutor(new EconomyCommands(this));

                // 3. Set tab completers
                getCommand("pay").setTabCompleter(new EconomyCommands(this));
                getCommand("bal").setTabCompleter(new EconomyCommands(this));
                getCommand("baltop").setTabCompleter(new EconomyCommands(this));
                getCommand("money").setTabCompleter(new EconomyCommands(this));
                getCommand("moneytop").setTabCompleter(new EconomyCommands(this));
                getCommand("eco").setTabCompleter(new EconomyCommands(this));

            }

        }

        // TPShop System
        if (config.getBoolean("Systems.TPShop.Enabled")) {
            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled TPShop");

            this.getCommand("tpshop").setExecutor(new TPShopCommand());
            this.getCommand("tpshop").setTabCompleter(new TPShopTabCompleter());
        }

        // Vote System
        if (config.getBoolean("Systems.VoteSystem.Enabled")) {
            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled Vote Systems");

            this.getCommand("adminvote").setExecutor(new BMVote(this));
            this.getCommand("adminvote").setTabCompleter(new TestVoteTabCompleter());
        }

        // Wild System
        if (config.getBoolean("Systems.Wild.Enabled")) {
            // Add the system enabled message to the console.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled Wild Systems");

            // Create an instance of WildData with the plugin instance
            WildData wildData = new WildData(this);

            // Register the NoFallDamage event listener.
            getServer().getPluginManager().registerEvents(new NoFallDamage(this), this);

            // Instantiate the command executor and tab completer, passing the WildData instance.
            WildCommand wildCommand = new WildCommand(wildData, this);
            WildTabCompleter wildTabCompleter = new WildTabCompleter(wildData);

            // Register the /wild command executor and tab completer.
            this.getCommand("wild").setExecutor(wildCommand);
            this.getCommand("wild").setTabCompleter(wildTabCompleter);

            // Register the /version command
            getCommand("version").setExecutor(new ChunkVersion(wildData, this));
        }

        // Spawn Systems
        if (config.getBoolean("Systems.SpawnSystems.Enabled")) {
            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled Spawn Systems");

            this.getCommand("firstjoinmessage").setExecutor(new FirstJoinCommand(this));
            this.getCommand("springsheal").setExecutor(new HealCommand(this));

            AltarManager altarManager = new AltarManager(this);
            getServer().getPluginManager().registerEvents(altarManager, this);
            HealingSprings.startHealingSpringsAmbient(this);
            WishingWell.startWishingWellAmbient(this);
            Obelisk.startObeliskAmbient(this);
        }

        // Common Commands
        if (config.getBoolean("Systems.CommonCommands.Enabled")) {
            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled All Common Commands");

            this.getCommand("playtime").setExecutor(new CommonCommands(this));
            this.getCommand("lag").setExecutor(new CommonCommands(this));
            this.getCommand("bmdiscord").setExecutor(new CommonCommands(this));

            // Register the /bmrestart command executor
            this.getCommand("bmrestart").setExecutor(new BMRestart(this));
        }

        // Virtual Containers System
        if (config.getBoolean("Systems.Containers.Enabled")) {
            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled Virtual Containers System");

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
        }

        // Velocity Tell System
        if (config.getBoolean("Systems.VTell.Enabled")) {
            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled VTell System");

            this.getCommand("vtell").setExecutor(new VTellCommand(this));
            this.getServer().getMessenger().registerOutgoingPluginChannel(this, "bmessentials:vtell");
        }

        // Map System
        if (config.getBoolean("Systems.Maps.Enabled")) {
            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled Maps");

            this.getCommand("map").setExecutor(new MapCommand(this));
            this.getCommand("map").setTabCompleter(new MapTabCompleter());
            this.getCommand("maps").setExecutor(new MapCommand(this));
            this.getCommand("maps").setTabCompleter(new MapTabCompleter());
        }

        // Donation System
        if (config.getBoolean("Systems.Donations.Enabled")) {
            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled Donation Systems");

            this.getCommand("donation").setExecutor(new DonationCommand(this));
        }

        // Trophy System
        if (config.getBoolean("Systems.Trophies.Enabled")) {
            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled the Trophy Systems");

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
            this.getCommand("trophy").setTabCompleter(new TrophyTabCompleter());

            // Register PlaceholderAPI expansion
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new TrophyRoomPlaceholderExpansion(this, trophiesDB).register();
            } else {
                getLogger().warning("PlaceholderAPI not found. Placeholders will not be available.");
            }
        }

        // CommandQueue System
        if (config.getBoolean("Systems.CommandQueue.Enabled")) {
            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled the CommandQueue System");

            this.queueManager = new CommandQueueManager(this);

            // Register command executor
            this.getCommand("commandqueue").setExecutor(new CommandQueueCommandExecutor(this, queueManager));
            getCommand("commandqueue").setTabCompleter(new CommandQueueTabCompleter());

            // Load commands from CommandQueue.yml
            queueManager.loadCommands();
        }


        // Vote System
        if (getConfig().getBoolean("Systems.Vot.Enabled")) {
            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled Vote System");

            getCommand("vot").setExecutor(new VoteCommand(this));
            this.getCommand("vot").setTabCompleter(new VotTabCompleter());
            getServer().getPluginManager().registerEvents(new PlayerEventListener(), this);
        }

        // RankUp System
        if (config.getBoolean("Systems.Rankup.Enabled")) {
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled the RankUp System");
            this.rankUpManager = new RankUpManager(this);
        }

        // Enable Book Systems
        if (getConfig().getBoolean("Systems.Help.Books.Enabled")) {
            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled the Book Systems");

            HelpBooks books = new HelpBooks(this);
            getCommand("book").setExecutor(new BookCommand(books));
        }

        // Enable Commands System
        if (getConfig().getBoolean("Systems.Help.Commands.Enabled")) {
            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled the Commands System");

            HelpCommands commandsSystem = new HelpCommands(this);
            getCommand("commands").setExecutor(new CommandsCommand(commandsSystem));
        }

        // AntiTrample System
        if (getConfig().getBoolean("Systems.AntiTrample.Enabled")) {
            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled AntiTrample Systems");

            // Register the CropTrampleListener to disable crop trampling by both players and mobs
            getServer().getPluginManager().registerEvents(new CropTrampleListener(), this);
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

        // Close the PlayerData Database Connection
        if (PlayerDataDBManager != null) {
            PlayerDataDBManager.closeConnection();
        }

        // Log a message to indicate the plugin has been successfully disabled
        getLogger().info("BMEssentials has been disabled!");
    }

}
