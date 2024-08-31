package at.sleazlee.bmessentials;

import at.sleazlee.bmessentials.AltarSystem.AltarManager;
import at.sleazlee.bmessentials.AltarSystem.HealingSprings;
import at.sleazlee.bmessentials.Containers.*;
import at.sleazlee.bmessentials.SpawnSystems.*;
import at.sleazlee.bmessentials.art.Art;
import at.sleazlee.bmessentials.vot.VoteCommand;
import at.sleazlee.bmessentials.bmefunctions.BMECommandExecutor;
import at.sleazlee.bmessentials.bmefunctions.CommonCommands;
import at.sleazlee.bmessentials.bmefunctions.DonationCommand;
import at.sleazlee.bmessentials.bungeetell.BungeeTellCommand;
import at.sleazlee.bmessentials.huskhomes.HuskHomesAPIHook;
import at.sleazlee.bmessentials.maps.MapCommand;
import at.sleazlee.bmessentials.maps.MapTabCompleter;
import at.sleazlee.bmessentials.tpshop.TPShopCommand;
import at.sleazlee.bmessentials.tpshop.TPShopTabCompleter;
import at.sleazlee.bmessentials.trophyroom.commands.TrophyCommand;
import at.sleazlee.bmessentials.trophyroom.data.Data;
import at.sleazlee.bmessentials.trophyroom.db.Database;
import at.sleazlee.bmessentials.trophyroom.listeners.PlayerListener;
import at.sleazlee.bmessentials.trophyroom.menu.TrophyRoomMenu;
import at.sleazlee.bmessentials.trophyroom.smartinventory.SmartInventory;
import at.sleazlee.bmessentials.trophyroom.smartinventory.manager.BasicSmartInventory;
import at.sleazlee.bmessentials.trophyroom.util.PlaceHolderApiHook;
import at.sleazlee.bmessentials.votesystem.BMVote;
import at.sleazlee.bmessentials.votesystem.TestVoteTabCompleter;
import at.sleazlee.bmessentials.wild.BMWildCommand;
import at.sleazlee.bmessentials.wild.NoFallDamage;
import at.sleazlee.bmessentials.wild.WildTabCompleter;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import at.sleazlee.bmessentials.rankup.RankUpManager;
import net.milkbowl.vault.economy.Economy;

import java.sql.SQLException;

public class BMEssentials extends JavaPlugin {

    private FileConfiguration config = getConfig();
    private final SmartInventory inventory = new BasicSmartInventory(this);
    private HuskHomesAPIHook huskHomesAPIHook;
//    private DatabaseManager dbManager;
    private static BMEssentials main;
    private RankUpManager rankUpManager;
    private Economy economy = null;

    public static BMEssentials getInstance() {
        return getPlugin(BMEssentials.class);
    }

    @Override
    public void onEnable() {

        main = this;

        // Creates a new config.yml if it doesn't exist, copies from your resource.
        this.saveDefaultConfig();

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

        // Virtual Containers System
        if (config.getBoolean("systems.containers.enabled")) {

            //trash
            this.getCommand("trash").setExecutor(new TrashCommand());

            //Crafting Table
            this.getCommand("craft").setExecutor(new CraftCommand());

            //Ender Chest
            this.getCommand("enderchest").setExecutor(new eChestCommand());

            //Cartography Table
            this.getCommand("cartography").setExecutor(new cartographyTableCommand());

            //Loom
            this.getCommand("loom").setExecutor(new LoomCommand());

            //Stone Cutter
            this.getCommand("stonecutter").setExecutor(new StoneCutterCommand());

            //Smithing Table
            this.getCommand("smithing").setExecutor(new SmithingTableCommand());

            //Grindstone
            this.getCommand("grindstone").setExecutor(new GrindStoneCommand());

            //Anvil
            this.getCommand("anvil").setExecutor(new AnvilCommand());

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

//        // Ban/Mute System
//        if (config.getBoolean("systems.punishments.enabled")) {
//
//            this.getServer().getMessenger().registerIncomingPluginChannel(this, "bmessentials:mute", new BungeeMutePlayer(this));
//            this.getServer().getMessenger().registerOutgoingPluginChannel(this, "bmessentials:autoban");
//
//            this.getCommand("autoban").setExecutor(new AutoBanCommand(this));
//            this.getCommand("unmute").setExecutor(new UnMuteCommand());
//
//            //Add the system enabled message.
//            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled Ban/Mute Systems");
//        }

        // Donation System
        if (config.getBoolean("systems.donations.enabled")) {

            this.getCommand("donation").setExecutor(new DonationCommand(this));

            //Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled Donation Systems");
        }

        // Trophy System
        if (config.getBoolean("systems.trophies.enabled")) {
            Database database = new Database("plugins/BMessentials/");
            this.inventory.init();

            try {
                Data data = new Data();
                data.setMenu(new TrophyRoomMenu(this.inventory));
            } catch (JsonProcessingException | SQLException var2) {
                var2.printStackTrace();
            }


            Bukkit.getPluginManager().registerEvents(new PlayerListener(), this);
            this.getCommand("trophyroom").setExecutor(new TrophyCommand());

            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                (new PlaceHolderApiHook()).register();
                System.out.println("Registered PlaceholderAPI placeholders!");
            }

            this.getCommand("trophyroom").setExecutor(new TrophyCommand());

            //Add the system enabled message.
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled the Trophy Systems");
        }

        // Vot System
        if (getConfig().getBoolean("systems.Vot.enabled")) {
            getCommand("vot").setExecutor(new at.sleazlee.bmessentials.vot.VoteCommand());
            getServer().getConsoleSender().sendMessage(ChatColor.WHITE + " - Enabled Vot System");
        }

        // RankUp System
        if (getConfig().getBoolean("systems.Rankup.enabled")) {

            if (!setupEconomy()) {
                getLogger().severe("Disabled due to no Vault dependency found!");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            this.rankUpManager = new RankUpManager(this, getEconomyService());
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

//        // Close the database connection pool
//        if (dbManager != null) {
//            dbManager.close();
//            getLogger().info("Database connection closed successfully.");
//        }

        // Log a message to indicate the plugin has been successfully disabled
        getLogger().info("BMEssentials has been disabled!");
    }

    public static BMEssentials getMain() {
        return main;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().info("Vault (plugin) is not installed.");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().info("No economy provider found via Vault.");
            return false;
        }

        economy = rsp.getProvider();
        return economy != null;
    }


    private Economy getEconomyService() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return null;
        }
        return rsp.getProvider();
    }
}
