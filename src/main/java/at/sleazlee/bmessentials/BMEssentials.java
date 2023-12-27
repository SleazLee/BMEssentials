package at.sleazlee.bmessentials;

import at.sleazlee.bmessentials.AltarSystem.AltarManager;
import at.sleazlee.bmessentials.AltarSystem.HealingSprings;
import at.sleazlee.bmessentials.LandsBonus.BonusCommand;
import at.sleazlee.bmessentials.LandsBonus.BonusMethods;
import at.sleazlee.bmessentials.Punish.AutoBanCommand;
import at.sleazlee.bmessentials.Punish.BungeeMutePlayer;
import at.sleazlee.bmessentials.Punish.UnMuteCommand;
import at.sleazlee.bmessentials.SpawnSystems.*;
import at.sleazlee.bmessentials.art.Art;
import at.sleazlee.bmessentials.bmefunctions.BMECommandExecutor;
import at.sleazlee.bmessentials.bmefunctions.CommonCommands;
import at.sleazlee.bmessentials.bmefunctions.DatabaseManager;
import at.sleazlee.bmessentials.bmefunctions.DonationCommand;
import at.sleazlee.bmessentials.bungeetell.BungeeTellCommand;
import at.sleazlee.bmessentials.huskhomes.HuskHomesAPIHook;
import at.sleazlee.bmessentials.maps.MapCommand;
import at.sleazlee.bmessentials.maps.MapTabCompleter;
import at.sleazlee.bmessentials.tpshop.TPShopCommand;
import at.sleazlee.bmessentials.tpshop.TPShopTabCompleter;
import at.sleazlee.bmessentials.trash.TrashCommand;
import at.sleazlee.bmessentials.votesystem.BMVote;
import at.sleazlee.bmessentials.votesystem.TestVoteTabCompleter;
import at.sleazlee.bmessentials.wild.BMWildCommand;
import at.sleazlee.bmessentials.wild.NoFallDamage;
import at.sleazlee.bmessentials.wild.WildTabCompleter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class BMEssentials extends JavaPlugin {

    private FileConfiguration config = getConfig();
    private HuskHomesAPIHook huskHomesAPIHook;
    private DatabaseManager dbManager;

    @Override
    public void onEnable() {

        // Creates a new config.yml if it doesn't exist, copies from your resource.
        this.saveDefaultConfig();

        // Establish the database connection
        try {
            dbManager = new DatabaseManager(this);
            getLogger().info("[BMEssentials] MySQL Connection established!");

            // Create Databases if they are not already created.
            dbManager.createDatabaseTables();


        } catch (Exception e) {
            getLogger().severe("Failed to establish MySQL connection: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }

        //Art at the beginning.
        String[] startArt = Art.startupArt().split("\n");
        for (String line : startArt) {
            getServer().getConsoleSender().sendMessage(ChatColor.AQUA + line);
        }

        // TpShop
        if (config.getBoolean("systems.tpshop.enabled")) {
            this.getCommand("tpshop").setExecutor(new TPShopCommand());
            this.getCommand("tpshop").setTabCompleter(new TPShopTabCompleter());

            //Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled TPShop");
        }

        // Vote System
        if (config.getBoolean("systems.votesystem.enabled")) {
            this.getCommand("adminvote").setExecutor(new BMVote(this));
            this.getCommand("adminvote").setTabCompleter(new TestVoteTabCompleter());

            //Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled Vote Systems");
        }

        // Wild System
        if (config.getBoolean("systems.bmwild.enabled")) {
            getServer().getPluginManager().registerEvents(new NoFallDamage(this), this);

            this.getCommand("wild").setExecutor(new BMWildCommand());
            this.getCommand("wild").setTabCompleter(new WildTabCompleter());
            this.getCommand("rtp").setExecutor(new BMWildCommand());
            this.getCommand("rtp").setTabCompleter(new WildTabCompleter());
            this.getCommand("randomtp").setExecutor(new BMWildCommand());
            this.getCommand("randomtp").setTabCompleter(new WildTabCompleter());
            this.getCommand("randomteleport").setExecutor(new BMWildCommand());
            this.getCommand("randomteleport").setTabCompleter(new WildTabCompleter());

            if (Bukkit.getPluginManager().getPlugin("HuskHomes") != null) {
                this.huskHomesAPIHook = new HuskHomesAPIHook();
            }

            //Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled BMWild");
        }

        // Spawn Systems
        if (config.getBoolean("systems.spawnsystems.enabled")) {
            this.getCommand("firstjoinmessage").setExecutor(new FirstJoinCommand(this));
            this.getCommand("springsheal").setExecutor(new HealCommand(this));
            this.getCommand("vot").setExecutor(new SpawnOnlyCommands());
            this.getCommand("voting").setExecutor(new SpawnOnlyCommands());
            this.getCommand("mcmmoboost").setExecutor(new McMMOBoost(this));
            this.getCommand("diamondcatch").setExecutor(new DiamondCatch(this));

            AltarManager altarManager = new AltarManager(this);
            getServer().getPluginManager().registerEvents(altarManager, this);
            HealingSprings.startHealingSpringsAmbient(this);

            //Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled Spawn Systems");
        }

        // Common Commands
        if (config.getBoolean("systems.commoncommands.enabled")) {
            this.getCommand("playtime").setExecutor(new CommonCommands(this));
            this.getCommand("lag").setExecutor(new CommonCommands(this));
            this.getCommand("bmdiscord").setExecutor(new CommonCommands(this));

            //Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled All Common Commands");
        }

        // Trash System
        if (config.getBoolean("systems.bmtrash.enabled")) {
            this.getCommand("trash").setExecutor(new TrashCommand());
            this.getCommand("disposal").setExecutor(new TrashCommand());

            //Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled Trash System");
        }

        // BungeeTell System
        if (config.getBoolean("systems.bungeetell.enabled")) {
            this.getCommand("bungeetell").setExecutor(new BungeeTellCommand(this));
            this.getServer().getMessenger().registerOutgoingPluginChannel(this, "bmessentials:bungeetell");

            //Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled BungeeTell System");
        }

        // Map System
        if (config.getBoolean("systems.maps.enabled")) {

            this.getCommand("map").setExecutor(new MapCommand(this));
            this.getCommand("map").setTabCompleter(new MapTabCompleter());
            this.getCommand("maps").setExecutor(new MapCommand(this));
            this.getCommand("maps").setTabCompleter(new MapTabCompleter());

            //Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled Maps");
        }

        // Ban/Mute System
        if (config.getBoolean("systems.punishments.enabled")) {

            this.getServer().getMessenger().registerIncomingPluginChannel(this, "bmessentials:mute", new BungeeMutePlayer(this));
            this.getServer().getMessenger().registerOutgoingPluginChannel(this, "bmessentials:autoban");

            this.getCommand("autoban").setExecutor(new AutoBanCommand(this));
            this.getCommand("unmute").setExecutor(new UnMuteCommand());

            //Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled Ban/Mute Systems");
        }

        // Donation System
        if (config.getBoolean("systems.donations.enabled")) {

            this.getCommand("donation").setExecutor(new DonationCommand(this));

            //Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled Donation Systems");
        }

        // Town Bonus System
        if (config.getBoolean("systems.townbonus.enabled")) {
            getServer().getPluginManager().registerEvents(new BonusMethods(this), this);
            BonusMethods.setupPermissions();
            this.getCommand("bmlands").setExecutor(new BonusCommand(this));

            //Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled Town Bonus Systems");
        }






        //Finally enables the reload system.
        this.getCommand("bme").setExecutor(new BMECommandExecutor(this));
        //Finish the message at the very end and show it
        getServer().getConsoleSender().sendMessage("\n §b" + ChatColor.AQUA + " BMEssentials was successfully" + ChatColor.GREEN + " Enabled" + ChatColor.AQUA + "!" + "\n §b");
    }

    @Override
    public void onDisable() {
        // Log a message to indicate the plugin is being disabled
        getLogger().info("Disabling BMEssentials...");

        // Close the database connection pool
        if (dbManager != null) {
            dbManager.close();
            getLogger().info("Database connection closed successfully.");
        }

        // Log a message to indicate the plugin has been successfully disabled
        getLogger().info("BMEssentials has been disabled!");
    }

}
