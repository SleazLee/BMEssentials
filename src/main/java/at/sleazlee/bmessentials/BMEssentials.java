package at.sleazlee.bmessentials;

import at.sleazlee.bmessentials.AFKSystem.AfkCommand;
import at.sleazlee.bmessentials.AFKSystem.AfkListener;
import at.sleazlee.bmessentials.AFKSystem.AfkManager;
import at.sleazlee.bmessentials.AFKSystem.AfkPlaceholderExpansion;
import at.sleazlee.bmessentials.AltarSystem.AltarManager;
import at.sleazlee.bmessentials.AltarSystem.Altars.HealingSprings;
import at.sleazlee.bmessentials.AltarSystem.Altars.Obelisk;
import at.sleazlee.bmessentials.AltarSystem.Altars.WishingWell;
import at.sleazlee.bmessentials.BlueMapFunctions.MapCommand;
import at.sleazlee.bmessentials.BlueMapFunctions.MapTabCompleter;
import at.sleazlee.bmessentials.CommandQueue.CommandQueueCommandExecutor;
import at.sleazlee.bmessentials.CommandQueue.CommandQueueManager;
import at.sleazlee.bmessentials.CommandQueue.CommandQueueTabCompleter;
import at.sleazlee.bmessentials.Containers.*;
import at.sleazlee.bmessentials.DonationSystem.GetDonations;
import at.sleazlee.bmessentials.EconomySystem.BMSEconomyProvider;
import at.sleazlee.bmessentials.EconomySystem.EconomyCommands;
import at.sleazlee.bmessentials.EconomySystem.LegacyEconomyProvider;
import at.sleazlee.bmessentials.Help.Abilities.ChestSortCommand;
import at.sleazlee.bmessentials.Help.Abilities.ChestSortTabCompleter;
import at.sleazlee.bmessentials.Help.Commands.*;
import at.sleazlee.bmessentials.Help.HelpBooks;
import at.sleazlee.bmessentials.Help.HelpText;
import at.sleazlee.bmessentials.Help.TabCompleter.CommandTabCompleter;
import at.sleazlee.bmessentials.Help.TabCompleter.DonorranksTabCompleter;
import at.sleazlee.bmessentials.Help.TabCompleter.HelpTabCompleter;
import at.sleazlee.bmessentials.Help.TabCompleter.RanksTabCompleter;
import at.sleazlee.bmessentials.PlayerData.BMEChatPlaceholders;
import at.sleazlee.bmessentials.PlayerData.BMEPlaceholders;
import at.sleazlee.bmessentials.PlayerData.PlayerDatabaseManager;
import at.sleazlee.bmessentials.PlayerData.PlayerJoinListener;
import at.sleazlee.bmessentials.PurpurFeatures.*;
import at.sleazlee.bmessentials.SpawnSystems.FirstJoinCommand;
import at.sleazlee.bmessentials.SpawnSystems.HealCommand;
import at.sleazlee.bmessentials.VTell.VTellCommand;
import at.sleazlee.bmessentials.art.Art;
import at.sleazlee.bmessentials.bmefunctions.BMECommandExecutor;
import at.sleazlee.bmessentials.bmefunctions.BMRestart;
import at.sleazlee.bmessentials.bmefunctions.CommonCommands;
import at.sleazlee.bmessentials.crypto.AESEncryptor;
import at.sleazlee.bmessentials.huskhomes.LandsTeleportFixListener;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

/**
 * The main class for the BMEssentials plugin, which extends JavaPlugin.
 * This class initializes and manages various systems within the plugin,
 * such as TPShop, Vote System, Wild System, Spawn Systems, and more.
 */
public class BMEssentials extends JavaPlugin {

    /** Timer for the AFK System */
    private static final long TIMEOUT_MILLIS = 5 * 60 * 1000; // 5 minutes

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

    /** The instance of the help book system. */
    private HelpBooks books;

    /** The instance of the Plugin Message Encryption Code. */
    private AESEncryptor aes;

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

        // Load the Plugin Message Encrypton
        try {
            Path keyFile = getDataFolder().toPath().resolve("crypto.key");
            this.aes = AESEncryptor.fromKeyFile(keyFile);
            getLogger().info("🔐 AES key loaded");
        } catch (Exception e) {
            getLogger().severe("Failed to load crypto.key, disabling plugin.");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

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
                getCommand("balance").setExecutor(new EconomyCommands(this));
                getCommand("money").setExecutor(new EconomyCommands(this));
                getCommand("baltop").setExecutor(new EconomyCommands(this));
                getCommand("moneytop").setExecutor(new EconomyCommands(this));
                getCommand("eco").setExecutor(new EconomyCommands(this));

                // 3. Set tab completers
                getCommand("pay").setTabCompleter(new EconomyCommands(this));
                getCommand("bal").setTabCompleter(new EconomyCommands(this));
                getCommand("balance").setTabCompleter(new EconomyCommands(this));
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

            // Register BMVote as the listener for incoming plugin messages on the "bmessentials:vote" channel.
            this.getServer().getMessenger().registerIncomingPluginChannel(this, "bmessentials:vote", new BMVote(this));
            this.getServer().getMessenger().registerOutgoingPluginChannel(this, "bmessentials:vote");

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
            getServer().getPluginManager().registerEvents(new NoFallDamage(), this);

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

            // Register the donation channel for both incoming and outgoing messages.
            getServer().getMessenger().registerIncomingPluginChannel(this, "bmessentials:donation", new GetDonations(this));
            getServer().getMessenger().registerOutgoingPluginChannel(this, "bmessentials:donation");
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


        // Vot System
        if (getConfig().getBoolean("Systems.Vot.Enabled")) {
            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled Vot System");

            getCommand("vot").setExecutor(new VoteCommand(this));
            this.getCommand("vot").setTabCompleter(new VotTabCompleter());
            getCommand("v").setExecutor(new VoteCommand(this));
            this.getCommand("v").setTabCompleter(new VotTabCompleter());
            getServer().getPluginManager().registerEvents(new PlayerEventListener(), this);
        }

        // RankUp System
        if (config.getBoolean("Systems.Rankup.Enabled")) {
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled the RankUp System");
            this.rankUpManager = new RankUpManager(this);
        }

        // Enable Help Systems
        if (getConfig().getBoolean("Systems.Help.Enabled")) {
            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled the Book Systems");

            books = new HelpBooks(this);
            getCommand("book").setExecutor(new BookCommand(books));

            getCommand("help").setExecutor(new HelpCommand());
            getCommand("help").setTabCompleter(new HelpTabCompleter());

            // Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled the Commands System");

            HelpText textSystem = new HelpText(this);
            getCommand("text").setExecutor(new TextCommand(textSystem));

            getCommand("commands").setExecutor(new CommandsCommand(textSystem));
            getCommand("commands").setTabCompleter(new CommandTabCompleter());


            // ranks command
            getCommand("ranks").setExecutor(new RanksCommand());
            getCommand("ranks").setTabCompleter(new RanksTabCompleter());

            // donorranks command
            getCommand("donorranks").setExecutor(new DonorrankCommand());
            getCommand("donorranks").setTabCompleter(new DonorranksTabCompleter());

            // Chest Sort System
            getCommand("chestsort").setExecutor(new ChestSortCommand());
            getCommand("chestsort").setTabCompleter(new ChestSortTabCompleter());
        }

        // Purpur feature Systems
        if (config.getBoolean("Systems.PurpurFeatures.Enabled")) {
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled the all Purpur feature Systems");

            // totem-of-undying-works-in-inventory
            Bukkit.getServer().getPluginManager().registerEvents(new totemWorksAnywhere(), this);

            // disable-trampling
            getServer().getPluginManager().registerEvents(new CropTrampleListener(), this);

            // disables dragon egg teleportation
            getServer().getPluginManager().registerEvents(new DragonEggTPFix(), this);

            // Makes Skeleton Horses 10x more rare
            Bukkit.getPluginManager().registerEvents(new SuperRareSkeletonHorses(), this);

            // Allows SilkTouch mines Mob Spawners
            getServer().getPluginManager().registerEvents(new MobSpawnerSystem(this), this);

            // Prevents fall damage when a player lands on a hay block
            getServer().getPluginManager().registerEvents(new NoFallDamageOnHay(), this);

            // You can break individual slabs in a double slab block while sneaking
            getServer().getPluginManager().registerEvents(new SneakSlabBreak(), this);

        }

        // AFK System
        if (config.getBoolean("Systems.AFKSystem.Enabled")) {
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled the AFK System");

            // Register /afk command executor.
            getCommand("afk").setExecutor(new AfkCommand());
            // Register event listeners.
            Bukkit.getPluginManager().registerEvents(new AfkListener(), this);
            // Schedule a periodic task to check for inactive players.
            // This task runs every minute.
            Scheduler.runTimer(() -> {
                long currentTime = System.currentTimeMillis();
                AfkManager.getInstance().checkForInactivity(currentTime, TIMEOUT_MILLIS);
            }, 20L * 15, 20L * 15);

            // Register the PlaceholderAPI expansion if PlaceholderAPI is installed.
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new AfkPlaceholderExpansion(this).register();
            }
        }

        // Lands TP Fix System
        if (config.getBoolean("Systems.LandsTPFix.Enabled")) {
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Fixed the Lands TP System");

            Plugin landsPlugin = getServer().getPluginManager().getPlugin("Lands");
            Plugin huskHomesPlugin = getServer().getPluginManager().getPlugin("HuskHomes");

            if (landsPlugin != null || landsPlugin.isEnabled()) {
                if (huskHomesPlugin != null || huskHomesPlugin.isEnabled()) {
                    // Passed plugin load checks, run logic here.
                    getServer().getPluginManager().registerEvents(new LandsTeleportFixListener(), this);

                } else {
                    // If HuskHomes is not found
                    getLogger().severe("HuskHomes plugin is not loaded or disabled. TeleportFix will be disabled.");
                }
            } else {
                // If lands is not found
                getLogger().severe("Lands plugin is not loaded or disabled. TeleportFix will be disabled.");
            }

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

    public HelpBooks getBooks() {
        return books;
    }

    public AESEncryptor getAes() {
        return aes;
    }

}
