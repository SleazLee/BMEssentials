package at.sleazlee.bmvelocity;

import at.sleazlee.bmvelocity.AnnouncementSystem.AnnouncementSystem;
import at.sleazlee.bmvelocity.DonationSystem.DonationSystem;
import at.sleazlee.bmvelocity.PunishSystem.AutoBanListener;
import at.sleazlee.bmvelocity.VTell.VTellListener;
import at.sleazlee.bmvelocity.VoteSystem.AdminVoteCommand;
import at.sleazlee.bmvelocity.VoteSystem.VoteEvent;
import at.sleazlee.bmvelocity.art.Art;
import at.sleazlee.bmvelocity.crypto.AESEncryptor;
import at.sleazlee.bmvelocity.util.FloodgatePlayerListener;
import at.sleazlee.bmvelocity.VoteSystem.VoteSystem;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "bmvelocity",
        name = "BMVelocity",
        version = "2.0",
        description = "Port of BMBungeeEssentials to Velocity using SQLite and Adventure",
        authors = {"Sleazlee"}
)
public class BMVelocity {

    private final ProxyServer server;
    private final Logger logger;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private DatabaseManager databaseManager;
    private VoteSystem voteSystem;
    private AnnouncementSystem announcementSystem;
    private DonationSystem donationSystem;
    private AESEncryptor aes;

    @Inject
    public BMVelocity(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {

        // ASCII Art at the beginning.
        String[] startArt = Art.startupArt().split("\n");
        for (String line : startArt) {
            String cleaned = line.replaceAll("§.", "");
            server.getConsoleCommandSource()
                    .sendMessage(miniMessage.deserialize("<aqua>" + cleaned));
        }

        // 0) load crypto key
        try {
            Path keyFile = getDataPath().resolve("crypto.key");
            aes = AESEncryptor.fromKeyFile(keyFile);
            server.getConsoleCommandSource().sendMessage(miniMessage.deserialize("<white>  - \uD83D\uDD10  Loaded AES Encryption Key"));
//            logger.info("🔐 Loaded encryption key.");
        } catch (Exception e) {
            logger.error("Could not load crypto key, shutting down.", e);
            server.shutdown();  // or throw
            return;
        }

        // 1) Initialize the SQLite database manager
        try {
            databaseManager = new DatabaseManager(this);
            server.getConsoleCommandSource().sendMessage(miniMessage.deserialize("<white>  - SQLite connection established"));
//            logger.info("SQLite connection established!");
            databaseManager.createPunishmentsTable();
            databaseManager.createVoteDataTable();
            databaseManager.createPendingDonationsTable();
        } catch (Exception e) {
            logger.error("Failed to initialize database: " + e.getMessage(), e);
        }

        // 2) Register commands
        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("punish").build(),
                new at.sleazlee.bmvelocity.PunishSystem.PunishCommand(this)
        );

        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("adminvote").build(),
                new AdminVoteCommand(this)
        );

        // 3) Register incoming plugin channels
        MinecraftChannelIdentifier vtellChannel = MinecraftChannelIdentifier.create("bmessentials", "vtell");
        MinecraftChannelIdentifier muteChannel = MinecraftChannelIdentifier.create("bmessentials", "mute");
        MinecraftChannelIdentifier autobanChannel = MinecraftChannelIdentifier.create("bmessentials", "autoban");
        MinecraftChannelIdentifier voteChannel = MinecraftChannelIdentifier.create("bmessentials", "vote");
        MinecraftChannelIdentifier donationChannel = MinecraftChannelIdentifier.create("bmessentials", "donation");
        server.getChannelRegistrar().register(vtellChannel);
        server.getChannelRegistrar().register(muteChannel);
        server.getChannelRegistrar().register(autobanChannel);
        server.getChannelRegistrar().register(voteChannel);
        server.getChannelRegistrar().register(donationChannel);

        server.getConsoleCommandSource().sendMessage(miniMessage.deserialize("<white>  - Registered Plugin Channels"));

        // 4) Register event listeners
        server.getEventManager().register(this, new VTellListener(this));
        server.getEventManager().register(this, new AutoBanListener(this));
        server.getEventManager().register(this, new FloodgatePlayerListener(this));
        server.getEventManager().register(this, new VoteEvent(this));

        server.getConsoleCommandSource().sendMessage(miniMessage.deserialize("<white>  - Registered Event Listeners"));

        // 5) Initialize and register our VoteSystem
        voteSystem = new VoteSystem(this);
        server.getEventManager().register(this, voteSystem);

        server.getConsoleCommandSource().sendMessage(miniMessage.deserialize("<white>  - Enabled Vote Systems"));

        // 6) Create and start our AnnouncementSystem
        announcementSystem = new AnnouncementSystem(this, server, logger, server.getScheduler(), getDataPath());
        announcementSystem.initialize();

        server.getConsoleCommandSource().sendMessage(miniMessage.deserialize("<white>  - Enabled the Announcement System"));

        // 7) Create, register, and start our DonationSystem
        donationSystem = new DonationSystem(this);
        server.getEventManager().register(this, donationSystem);
        donationSystem.startDonationPolling();

        server.getConsoleCommandSource().sendMessage(miniMessage.deserialize("<white>  - Enabled the Donation Systems"));

        server.getConsoleCommandSource().sendMessage(miniMessage.deserialize("<aqua> BMVelocity was successfully <green>Enabled</green>!"));
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("BMVelocity is shutting down!");
        if (databaseManager != null) {
            databaseManager.close();
            logger.info("Database connection closed.");
        }
    }

    // -----------------------------------------------------
    //  Simple getters
    // -----------------------------------------------------
    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public VoteSystem getVoteSystem() {
        return voteSystem;
    }

    public DonationSystem getDonationSystem() {
        return donationSystem;
    }

    // Expose data folder path (adjust as necessary)
    public Path getDataPath() {
        return Path.of("plugins/BMVelocity");
    }

    /** Expose for other classes */
    public AESEncryptor getAes() {
        return aes;
    }

}

