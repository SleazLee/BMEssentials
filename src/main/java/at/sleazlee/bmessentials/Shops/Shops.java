package at.sleazlee.bmessentials.Shops;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import at.sleazlee.bmessentials.bmefunctions.IsInWorldGuardRegion;
import at.sleazlee.bmessentials.EconomySystem.BMSEconomyProvider;
import net.milkbowl.vault2.economy.Economy;
import net.milkbowl.vault2.economy.EconomyResponse;
import org.bukkit.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldguard.WorldGuard;
import net.william278.huskhomes.api.HuskHomesAPI;
import net.william278.huskhomes.position.Position;
import at.sleazlee.bmessentials.huskhomes.HuskHomesAPIHook;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.sign.Side;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Base64;

/**
 * Handles the Block Miner Shops system.
 * <p>
 * This class manages rentable WorldGuard regions and provides the
 * command logic for players to buy, disband, invite others, transfer
 * ownership, extend rent time and rename their shops. Data is stored in a
 * {@code shops.yml} file located inside the plugin's data folder.
 */
public class Shops implements CommandExecutor, TabCompleter, Listener {

    /** Reference to the main plugin instance. */
    private final BMEssentials plugin;
    /** File storing persistent shop information. */
    private final File shopsFile;
    /** Configuration wrapper for {@link #shopsFile}. */
    private FileConfiguration shopsConfig;
    /** Cached shop data keyed by region name. */
    private final Map<String, Shop> shops = new HashMap<>();
    /** Vault economy provider, if available. */
    private final Economy economy;
    /** Reference to main configuration for message retrieval. */
    private final FileConfiguration config;
    /** MiniMessage instance for formatting output. */
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    /** Tab suggestions for the named colors. */
    private static final List<String> COLOR_NAMES = List.of(
            "reset",
            "black",
            "dark_blue",
            "dark_green",
            "dark_aqua",
            "dark_red",
            "dark_purple",
            "gold",
            "gray",
            "dark_gray",
            "blue",
            "green",
            "aqua",
            "red",
            "light_purple",
            "yellow",
            "white"
    );

    /**
     * Encodes a nickname to a Base64 string for safe YAML storage.
     */
    private static String encodeNickname(String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            return "";
        }
        return Base64.getEncoder()
                .encodeToString(nickname.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decodes a nickname previously stored with {@link #encodeNickname}.
     * If decoding fails, the original value is returned to preserve legacy data.
     */
    private static String decodeNickname(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return "";
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            // value wasn't actually Base64 encoded
            return encoded;
        }
    }

    private static String decodeNicknameFromID(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return "";
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            // value wasn't actually Base64 encoded
            return encoded;
        }
    }

    /**
     * Sends a formatted message to the given command sender using the
     * MiniMessage template stored in the configuration.
     *
     * @param sender target to send the message to
     * @param key    configuration key under Systems.Shops.Messages
     * @param replacements optional pairs of placeholder and value
     */
    private void send(CommandSender sender, String key, String... replacements) {
        String path = "Systems.Shops.Messages." + key;
        String message = config.getString(path);
        if (message == null) {
            return;
        }
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        sender.sendMessage(miniMessage.deserialize(message));
    }

    /**
     * Creates a new instance of the shop system and loads configuration
     * from {@code shops.yml}. The constructor also registers command
     * executors and schedules periodic expiration checks.
     *
     * @param plugin the owning plugin instance
     */
    public Shops(BMEssentials plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.shopsFile = new File(plugin.getDataFolder(), "shops.yml");
        if (!shopsFile.exists()) {
            plugin.saveResource("shops.yml", false);
        }
        this.shopsConfig = YamlConfiguration.loadConfiguration(shopsFile);
        loadShops();

        RegisteredServiceProvider<Economy> rsp =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        this.economy = rsp == null ? null : rsp.getProvider();

        plugin.getCommand("bms").setExecutor(this);
        plugin.getCommand("bms").setTabCompleter(this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        Scheduler.runTimer(this::checkExpirations, 20L * 30, 20L * 30);
    }

    /**
     * Loads shop information from {@link #shopsFile} into memory.
     * Each section key represents a WorldGuard region name which is
     * mapped to a {@link Shop} instance.
     */
    private void loadShops() {
        for (String key : shopsConfig.getKeys(false)) {
            ConfigurationSection sec = shopsConfig.getConfigurationSection(key);
            if (sec == null) continue;
            Shop shop = new Shop(key);
            shop.nickname = decodeNickname(sec.getString("Nickname", ""));
            shop.nameColor = sec.getString("NameColor", "<black>");
            shop.sign = sec.getString("Sign", "");
            shop.price = sec.getDouble("Price", 100.0);
            shop.extendTime = sec.getLong("extendTime", 604800000L);
            shop.maxExtendTime = sec.getLong("maxExtendTime", 31536000000L);
            shop.defaultTp = sec.getString("DefaultTP", "");
            shop.owner = sec.getString("Owner", "");
            shop.rented = sec.getBoolean("Rented", !shop.owner.isEmpty());
            String cos = sec.getString("CoOwner", "");
            if (!cos.isEmpty()) {
                for (String s : cos.split(",")) {
                    String trim = s.trim();
                    if (!trim.isEmpty()) {
                        shop.coowners.add(trim);
                    }
                }
            }
            shop.expires = sec.getLong("Expires", 0L);
            shops.put(key, shop);
        }
    }

    /**
     * Writes the current shop state back to {@link #shopsFile}.
     * This method is called whenever shop data changes or when
     * a periodic expiration check runs.
     */
    private void saveShops() {
        for (String key : shops.keySet()) {
            Shop shop = shops.get(key);
            ConfigurationSection sec = shopsConfig.getConfigurationSection(key);
            if (sec == null) sec = shopsConfig.createSection(key);
            sec.set("Nickname", encodeNickname(shop.nickname));
            sec.set("NameColor", shop.nameColor);
            sec.set("Sign", shop.sign);
            sec.set("Price", shop.price);
            sec.set("extendTime", shop.extendTime);
            sec.set("maxExtendTime", shop.maxExtendTime);
            sec.set("DefaultTP", shop.defaultTp);
            sec.set("Owner", shop.owner);
            sec.set("Rented", shop.rented);
            sec.set("CoOwner", String.join(", ", shop.coowners));
            sec.set("Expires", shop.expires);
        }
        try {
            shopsConfig.save(shopsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save shops.yml");
            e.printStackTrace();
        }
    }

    /**
     * Handles the "/bms" command and dispatches to the appropriate
     * sub-command handler.
     *
     * @return true if the command was processed
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "players-only");
            return true;
        }
        if (args.length == 0) {
            send(player, "usage");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "buy" -> {
                if (args.length >= 2) {
                    yield handleBuy(player, args[1]);
                }
                yield handleBuy(player, null);
            }
            case "disband" -> handleDisband(player, args);
            case "invite" -> {
                if (args.length < 2) {
                    send(player, "invite-usage");
                    yield true;
                }
                yield handleInvite(player, args[1]);
            }
            case "remove" -> {
                if (args.length < 2) {
                    send(player, "remove-usage");
                    yield true;
                }
                yield handleRemove(player, args[1]);
            }
            case "transfer" -> {
                if (args.length < 2) {
                    send(player, "transfer-usage");
                    yield true;
                }
                yield handleTransfer(player, args[1]);
            }
            case "extend" -> handleExtend(player);
            case "tp" -> {
                if (args.length >= 2 && args[1].equalsIgnoreCase("set")) {
                    yield handleTpSet(player);
                }
                yield handleTp(player);
            }
            case "rename","name" -> {
                if (args.length < 2) {
                    send(player, "name-usage");
                    yield true;
                }
                switch (args[1].toLowerCase(Locale.ROOT)) {
                    case "color" -> {
                        if (args.length < 3) {
                            send(player, "name-usage");
                            yield true;
                        }
                        yield handleNameColor(player, args[2]);
                    }
                    case "hex" -> {
                        if (args.length < 3) {
                            send(player, "name-usage");
                            yield true;
                        }
                        yield handleNameHex(player, args[2]);
                    }
                    case "set" -> {
                        if (args.length < 3) {
                            send(player, "name-set-usage");
                            yield true;
                        }
                        String newName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                        yield handleRename(player, newName);
                    }
                    default -> {
                        send(player, "name-usage");
                        yield true;
                    }
                }
            }
            case "admin" -> handleAdmin(player, Arrays.copyOfRange(args, 1, args.length));
            default -> {
                send(player, "unknown-command");
                yield true;
            }
        };
    }

    /**
     * Provides tab completion for the "/bms" command.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // level-1: sub-commands
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList(
                    "buy", "disband", "invite", "remove", "transfer", "extend", "name", "rename", "tp"
            ));
            if (sender.isOp()) {
                completions.add("admin");
            }
            return completions;
        }

        // level-2: “buy” shops
        if (args.length == 2 && args[0].equalsIgnoreCase("buy")) {
            return new ArrayList<>(shops.keySet());
        }

        // level-2: “name” / “rename” actions
        if (args.length == 2
                && (args[0].equalsIgnoreCase("name") || args[0].equalsIgnoreCase("rename")))
        {
            return Arrays.asList("set", "color", "hex");
        }

        // level-2: "tp" sub-command
        if (args.length == 2 && args[0].equalsIgnoreCase("tp")) {
            return Collections.singletonList("set");
        }

        // level-3: color names under name/rename color
        if (args.length == 3
                && (args[0].equalsIgnoreCase("name") || args[0].equalsIgnoreCase("rename"))
                && args[1].equalsIgnoreCase("color"))
        {
            return new ArrayList<>(COLOR_NAMES);
        }

        // level-2: admin actions (ops only)
        if (args.length == 2
                && args[0].equalsIgnoreCase("admin")
                && sender.isOp())
        {
            return Arrays.asList("renewall", "disband");
        }

        // level-3: shops under admin disband (ops only)
        if (args.length == 3
                && args[0].equalsIgnoreCase("admin")
                && args[1].equalsIgnoreCase("disband")
                && sender.isOp())
        {
            return new ArrayList<>(shops.keySet());
        }

        return Collections.emptyList();
    }


    /**
     * Gets the shop region the player is currently standing in.
     *
     * @param player the player to check
     * @return the {@link Shop} the player is inside or {@code null}
     */
    private Shop shopAt(Player player) {
        for (Shop shop : shops.values()) {
            if (IsInWorldGuardRegion.isPlayerInRegion(player, shop.id)) {
                return shop;
            }
        }
        return null;
    }

    /**
     * Attempts to rent the shop region the player is currently in.
     *
     * @param player the player attempting to rent
     * @return true always to indicate the command was handled
     */
    private boolean handleBuy(Player player, String targetShop) {
        Shop shop = targetShop == null ? shopAt(player) : shops.get(targetShop);
        if (shop == null) {
            if (targetShop == null) {
                send(player, "not-in-shop");
                send(player, "buy-usage");
            } else {
                send(player, "shop-not-found");
            }
            return true;
        }
        if (shop.rented) {
            send(player, "shop-already-rented");
            return true;
        }
        if (ownsShop(player.getUniqueId().toString())) {
            send(player, "already-manage");
            return true;
        }
        if (!withdraw(player, shop.price)) {
            send(player, "cannot-afford-shop");
            return true;
        }
        shop.owner = player.getUniqueId().toString();
        shop.nickname = "";
        shop.nameColor = "<black>";
        shop.expires = System.currentTimeMillis() + shop.extendTime;
        shop.rented = true;
        updateRegionOwners(shop);
        resetWarpLocation(shop);
        saveShops();
        updateSign(shop);
        send(player, "shop-rented");
        return true;
    }

    /**
     * Disbands the player's shop after confirmation.
     *
     * @param player the shop owner issuing the command
     * @param args command arguments to check for "confirm"
     * @return true once processed
     */
    private boolean handleDisband(Player player, String[] args) {
        Shop shop = ownedShop(player.getUniqueId().toString());
        if (shop == null) {
            send(player, "not-owner");
            return true;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            send(player, "disband-confirm");
            return true;
        }
        shop.owner = "";
        shop.coowners.clear();
        shop.nickname = "";
        shop.nameColor = "<black>";
        shop.expires = 0L;
        shop.rented = false;
        clearRegion(shop);
        saveShops();
        updateRegionOwners(shop);
        updateSign(shop);
        send(player, "shop-disbanded");
        return true;
    }

    /**
     * Adds another player as co-owner of the shop.
     *
     * @param player     the current owner
     * @param targetName the player to invite
     * @return true once processed
     */
    private boolean handleInvite(Player player, String targetName) {
        Shop shop = ownedShop(player.getUniqueId().toString());
        if (shop == null) {
            send(player, "not-owner");
            return true;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            send(player, "player-not-found");
            return true;
        }
        if (ownsShop(target.getUniqueId().toString())) {
            send(player, "player-already-manages");
            return true;
        }
        if (shop.coowners.contains(target.getUniqueId().toString())) {
            send(player, "player-already-manages");
            return true;
        }
        shop.coowners.add(target.getUniqueId().toString());
        saveShops();
        updateRegionOwners(shop);
        send(player, "coowner-added", "player", target.getName());
        send(target, "you-are-coowner", "id", shop.id);
        return true;
    }

    /**
     * Removes a co-owner from the shop.
     */
    private boolean handleRemove(Player player, String targetName) {
        Shop shop = ownedShop(player.getUniqueId().toString());
        if (shop == null) {
            send(player, "not-owner");
            return true;
        }
        if (shop.coowners.isEmpty()) {
            send(player, "no-coowner");
            return true;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !shop.coowners.contains(target.getUniqueId().toString())) {
            send(player, "not-your-coowner");
            return true;
        }
        shop.coowners.remove(target.getUniqueId().toString());
        saveShops();
        updateRegionOwners(shop);
        send(player, "coowner-removed", "player", target.getName());
        send(target, "you-are-not-coowner", "id", shop.id);
        return true;
    }

    /**
     * Transfers shop ownership to the current co-owner.
     *
     * @param player     the current owner
     * @param targetName the co-owner's name
     * @return true once processed
     */
    private boolean handleTransfer(Player player, String targetName) {
        Shop shop = ownedShop(player.getUniqueId().toString());
        if (shop == null) {
            send(player, "not-owner");
            return true;
        }
        if (shop.coowners.isEmpty()) {
            send(player, "no-coowner");
            return true;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !shop.coowners.contains(target.getUniqueId().toString())) {
            send(player, "not-your-coowner");
            return true;
        }
        shop.owner = target.getUniqueId().toString();
        shop.coowners.remove(target.getUniqueId().toString());
        shop.nickname = "";
        shop.nameColor = "<black>";
        saveShops();
        updateRegionOwners(shop);
        updateSign(shop);
        send(player, "ownership-transferred");
        send(target, "you-now-own", "id", shop.id);
        return true;
    }

    /**
     * Pays rent to extend the remaining time on a shop.
     *
     * @param player the owner or co-owner paying rent
     * @return true when the extension is processed
     */
    private boolean handleExtend(Player player) {
        Shop shop = managedShop(player.getUniqueId().toString());
        if (shop == null) {
            send(player, "not-manage");
            return true;
        }
        long now = System.currentTimeMillis();
        long remaining = Math.max(0, shop.expires - now);
        if (remaining + shop.extendTime > shop.maxExtendTime) {
            send(player, "beyond-max");
            return true;
        }
        if (!withdraw(player, shop.price)) {
            send(player, "cannot-afford-rent");
            return true;
        }
        shop.expires = now + remaining + shop.extendTime;
        saveShops();
        updateSign(shop);
        send(player, "rent-extended");
        return true;
    }

    /**
     * Renames the player's shop if the new name is within the
     * allowed character limit.
     *
     * @param player the shop owner
     * @param name   the new nickname
     * @return true after renaming
     */
    private boolean handleRename(Player player, String name) {
        if (name.length() > 15) {
            send(player, "name-too-long");
            return true;
        }
        Shop shop = ownedShop(player.getUniqueId().toString());
        if (shop == null) {
            send(player, "not-owner");
            return true;
        }
        shop.nickname = name;
        saveShops();
        updateSign(shop);
        send(player, "shop-renamed");
        return true;
    }

    /**
     * Changes the sign color using a named ChatColor.
     */
    private boolean handleNameColor(Player player, String colorName) {
        if (!player.hasPermission("bm.shops.color")) {
            send(player, "color-no-permission");
            return true;
        }
        Shop shop = ownedShop(player.getUniqueId().toString());
        if (shop == null) {
            send(player, "not-owner");
            return true;
        }
        if (colorName.equalsIgnoreCase("reset")) {
            shop.nameColor = "<reset>";
        } else {
            TextColor color = NamedTextColor.NAMES.value(colorName.toLowerCase(Locale.ROOT));
            if (color == null) {
                send(player, "invalid-color");
                return true;
            }
            shop.nameColor = "<" + colorName.toLowerCase(Locale.ROOT) + ">";
        }
        saveShops();
        updateSign(shop);
        send(player, "color-changed");
        return true;
    }

    /**
     * Teleports the player to their shop warp.
     */
    private boolean handleTp(Player player) {
        Shop shop = managedShop(player.getUniqueId().toString());
        if (shop == null) {
            send(player, "not-manage");
            return true;
        }
        HuskHomesAPIHook.warpPlayer(player, shop.id);
        return true;
    }

    /**
     * Sets the HuskHomes warp for the player's shop to their current location
     * if they are within 9 blocks of their shop region.
     */
    private boolean handleTpSet(Player player) {
        Shop shop = ownedShop(player.getUniqueId().toString());
        if (shop == null) {
            send(player, "not-owner");
            return true;
        }

        if (shop.sign == null || shop.sign.isEmpty()) {
            send(player, "shop-not-found");
            return true;
        }

        String[] parts = shop.sign.split(";");
        org.bukkit.World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            send(player, "shop-not-found");
            return true;
        }
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(world));
        if (manager == null) {
            send(player, "shop-not-found");
            return true;
        }
        ProtectedRegion region = manager.getRegion(shop.id);
        if (region == null) {
            send(player, "shop-not-found");
            return true;
        }

        Location loc = player.getLocation();
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        double cx = Math.max(min.x(), Math.min(max.x(), loc.getX()));
        double cy = Math.max(min.y(), Math.min(max.y(), loc.getY()));
        double cz = Math.max(min.z(), Math.min(max.z(), loc.getZ()));
        double distance = loc.distance(new Location(world, cx, cy, cz));

        if (distance > 9) {
            send(player, "tp-too-far");
            return true;
        }

        String serverName = plugin.getConfig().getString("serverName", "server");
        HuskHomesAPI api = HuskHomesAPI.getInstance();
        Position pos = api.adaptPosition(loc, serverName);
        api.getWarp(shop.id).thenAccept(opt -> {
            if (opt.isPresent()) {
                api.relocateWarp(shop.id, pos);
            } else {
                api.createWarp(shop.id, pos);
            }
        });

        send(player, "tp-set-success");
        return true;
    }

    /**
     * Changes the sign color using a hex code.
     */
    private boolean handleNameHex(Player player, String hex) {
        if (!player.hasPermission("bm.shops.hex")) {
            send(player, "hex-no-permission");
            return true;
        }
        Shop shop = ownedShop(player.getUniqueId().toString());
        if (shop == null) {
            send(player, "not-owner");
            return true;
        }
        String sanitized = hex.startsWith("#") ? hex : "#" + hex;
        if (!sanitized.matches("^#[0-9a-fA-F]{6}$")) {
            send(player, "invalid-hex");
            return true;
        }
        TextColor color;
        try {
            color = TextColor.fromHexString(sanitized);
        } catch (IllegalArgumentException e) {
            send(player, "invalid-hex");
            return true;
        }
        shop.nameColor = "<" + color.asHexString() + ">";
        saveShops();
        updateSign(shop);
        send(player, "color-changed");
        return true;
    }

    /**
     * Handles admin commands such as renewing all plots or forcibly disbanding one.
     */
    private boolean handleAdmin(Player player, String[] args) {
        if (!player.isOp()) {
            send(player, "admin-no-permission");
            return true;
        }
        if (args.length < 1) {
            send(player, "admin-usage");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "renewall" -> {
                long now = System.currentTimeMillis();
                for (Shop shop : shops.values()) {
                    if (!shop.rented) continue;
                    long remaining = Math.max(0, shop.expires - now);
                    if (remaining + shop.extendTime <= shop.maxExtendTime) {
                        shop.expires = now + remaining + shop.extendTime;
                    }
                    updateSign(shop);
                }
                saveShops();
                send(player, "admin-renewall");
                return true;
            }
            case "disband" -> {
                if (args.length < 2) {
                    send(player, "admin-disband-usage");
                    return true;
                }
                Shop target = shops.get(args[1]);
                if (target == null) {
                    send(player, "shop-not-found");
                    return true;
                }
                if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
                    send(player, "admin-disband-confirm", "region", args[1]);
                    return true;
                }
                target.owner = "";
                target.coowners.clear();
                target.nickname = "";
                target.nameColor = "<black>";
                target.expires = 0L;
                target.rented = false;
                clearRegion(target);
                updateRegionOwners(target);
                resetWarpLocation(target);
                updateSign(target);
                saveShops();
                send(player, "admin-disbanded", "region", args[1]);
                return true;
            }
//            case "readinwarps" -> {
//                readDefaultWarps();
//                send(player, "admin-readinwarps");
//                return true;
//            }
            default -> {
                send(player, "admin-usage");
                return true;
            }
        }
    }

    /**
     * Gets the shop owned by the specified player.
     *
     * @param uuid the player's UUID as string
     * @return the owned shop or {@code null}
     */
    private Shop ownedShop(String uuid) {
        for (Shop shop : shops.values()) {
            if (uuid.equals(shop.owner)) return shop;
        }
        return null;
    }

    /**
     * Gets a shop either owned or co-owned by the specified player.
     *
     * @param uuid the player's UUID
     * @return the managed shop or {@code null}
     */
    private Shop managedShop(String uuid) {
        for (Shop shop : shops.values()) {
            if (uuid.equals(shop.owner) || shop.coowners.contains(uuid)) return shop;
        }
        return null;
    }

    /**
     * Handles right-click interactions with shop signs. If the sign corresponds
     * to an unrented shop the player will attempt to purchase it. Otherwise, if
     * the player manages the shop the rental time will be extended.
     */
    @EventHandler
    public void onSignInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!(block.getState() instanceof Sign)) return;

        Location loc = block.getLocation();
        Shop target = null;
        for (Shop shop : shops.values()) {
            if (shop.sign == null || shop.sign.isEmpty()) continue;
            String[] parts = shop.sign.split(";");
            if (parts.length < 5) continue;
            World w = Bukkit.getWorld(parts[0]);
            if (w == null) continue;
            int x = (int) Double.parseDouble(parts[1]);
            int y = (int) Double.parseDouble(parts[2]);
            int z = (int) Double.parseDouble(parts[3]);
            if (loc.getWorld().equals(w) && loc.getBlockX() == x && loc.getBlockY() == y && loc.getBlockZ() == z) {
                target = shop;
                break;
            }
        }
        if (target == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!target.rented) {
            handleBuy(player, target.id);
        } else if (player.getUniqueId().toString().equals(target.owner) ||
                   target.coowners.contains(player.getUniqueId().toString())) {
            handleExtend(player);
        }
    }

    /**
     * Checks if the player already manages a shop.
     */
    private boolean ownsShop(String uuid) {
        return managedShop(uuid) != null;
    }

    /**
     * Withdraws the specified amount from the player's balance using Vault.
     * When Vault or an economy provider is not present this method returns true
     * to allow the command to proceed without charging.
     */
    private boolean withdraw(Player player, double amount) {
        if (economy == null) return true;
        EconomyResponse res = economy.withdraw(
                plugin.getName(),
                player.getUniqueId(),
                "no_world",
                BMSEconomyProvider.CURRENCY_DOLLARS,
                BigDecimal.valueOf(amount)
        );
        return res.transactionSuccess();
    }

    /**
     * Sync WorldGuard region owners with the shop state.
     */
    private void updateRegionOwners(Shop shop) {
        if (shop.sign == null || shop.sign.isEmpty()) return;
        String[] parts = shop.sign.split(";");
        if (parts.length < 1) return;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return;
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(world));
        if (manager == null) return;
        ProtectedRegion region = manager.getRegion(shop.id);
        if (region == null) return;
        region.getOwners().clear();
        if (!shop.owner.isEmpty()) {
            try {
                region.getOwners().addPlayer(UUID.fromString(shop.owner));
            } catch (IllegalArgumentException ignored) {}
        }
        for (String co : shop.coowners) {
            try {
                region.getOwners().addPlayer(UUID.fromString(co));
            } catch (IllegalArgumentException ignored) {}
        }
        try {
            manager.save();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save region owners for " + shop.id);
        }
    }

    /**
     * Updates the sign associated with a shop to reflect its current state.
     * If the sign is missing or invalid, the method safely returns.
     */
    private void updateSign(Shop shop) {
        if (shop.sign == null || shop.sign.isEmpty()) return;
        String[] parts = shop.sign.split(";");
        if (parts.length < 5) return;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return;
        int x = (int) Double.parseDouble(parts[1]);
        int y = (int) Double.parseDouble(parts[2]);
        int z = (int) Double.parseDouble(parts[3]);
        String faceStr = parts.length >= 5 ? parts[4].toUpperCase(Locale.ROOT) : "NORTH";
        Location loc = new Location(world, x, y, z);
        Scheduler.run(loc, () -> {
            Block b = loc.getBlock();
            if (!(b.getState() instanceof Sign sign)) return;
            org.bukkit.block.sign.Side side = org.bukkit.block.sign.Side.FRONT;
            try {
                BlockFace configFace = BlockFace.valueOf(faceStr);
                BlockFace actual = BlockFace.NORTH;
                if (b.getBlockData() instanceof Directional dir) {
                    actual = dir.getFacing();
                } else if (b.getBlockData() instanceof Rotatable rot) {
                    actual = rot.getRotation();
                }
                if (actual != configFace) side = org.bukkit.block.sign.Side.BACK;
            } catch (IllegalArgumentException ignored) {}

            String name = shop.nickname.isEmpty() ? shop.id : shop.nickname;
            String coloredName = LegacyComponentSerializer.legacySection()
                    .serialize(miniMessage.deserialize(shop.nameColor + name));

            if (!shop.rented) {
                String line0 = LegacyComponentSerializer.legacySection()
                        .serialize(Component.text("For Rent", NamedTextColor.AQUA));
                sign.getSide(side).setLine(0, line0);
                sign.getSide(side).setLine(1, coloredName);
                sign.getSide(side).setLine(2, shop.price + "/" + timeLabel(shop.extendTime));
                sign.getSide(side).setLine(3, "Max: " + durationLabel(shop.maxExtendTime));
            } else {
                long remaining = Math.max(0, shop.expires - System.currentTimeMillis());
                String line0 = LegacyComponentSerializer.legacySection()
                        .serialize(Component.text("Rented", NamedTextColor.DARK_AQUA));
                sign.getSide(side).setLine(0, line0);
                sign.getSide(side).setLine(1, coloredName);
                sign.getSide(side).setLine(2, shop.price + "/" + timeLabel(shop.extendTime));
                sign.getSide(side).setLine(3, durationLabel(remaining));
            }
            sign.update();
        });
    }

    /**
     * Clears the entire shop region, removing any QuickShop shops inside and
     * resetting the terrain using WorldEdit.
     */
    private void clearRegion(Shop shop) {
        if (shop.sign == null || shop.sign.isEmpty()) return;
        String[] parts = shop.sign.split(";");
        if (parts.length < 1) return;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return;
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(world));
        if (manager == null) return;
        ProtectedRegion region = manager.getRegion(shop.id);
        if (region == null) return;

        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        // Remove any QuickShop shops within the region bounds
        Location l1 = new Location(world, min.x(), -64, min.z());
        Location l2 = new Location(world, max.x(), 319, max.z());
        QuickShopUtils.removeShopsInCuboid(l1, l2);

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
        try (EditSession session = WorldEdit.getInstance()
                .newEditSessionBuilder().world(weWorld).build()) {
            session.setBlocks(new CuboidRegion(weWorld,
                            BlockVector3.at(min.x(), -64, min.z()),
                            BlockVector3.at(max.x(), -64, max.z())),
                    BlockTypes.BEDROCK.getDefaultState());

            session.setBlocks(new CuboidRegion(weWorld,
                            BlockVector3.at(min.x(), -63, min.z()),
                            BlockVector3.at(max.x(), 66, max.z())),
                    BlockTypes.STONE.getDefaultState());

            session.setBlocks(new CuboidRegion(weWorld,
                            BlockVector3.at(min.x(), 67, min.z()),
                            BlockVector3.at(max.x(), 69, max.z())),
                    BlockTypes.DIRT.getDefaultState());

            session.setBlocks(new CuboidRegion(weWorld,
                            BlockVector3.at(min.x(), 70, min.z()),
                            BlockVector3.at(max.x(), 70, max.z())),
                    BlockTypes.GRASS_BLOCK.getDefaultState());

            session.setBlocks(new CuboidRegion(weWorld,
                            BlockVector3.at(min.x(), 71, min.z()),
                            BlockVector3.at(max.x(), 319, max.z())),
                    BlockTypes.AIR.getDefaultState());
        } catch (WorldEditException e) {
            plugin.getLogger().warning("Failed to clear region " + shop.id + ": " + e.getMessage());
        }
    }

    /**
     * Formats a duration into a short label used on rental signs.
     *
     * @param millis duration in milliseconds
     * @return formatted label such as "7d" or "3h"
     */
    private String timeLabel(long millis) {
        long days = millis / 86400000L;
        long hours = (millis % 86400000L) / 3600000L;
        long mins = (millis % 3600000L) / 60000L;
        if (days > 0) return days + "d";
        if (hours > 0) return hours + "h";
        return mins + "m";
    }

    /**
     * Formats a duration into a long label like "2d 3h 4m".
     */
    private String durationLabel(long millis) {
        long days = millis / 86400000L;
        long hours = (millis % 86400000L) / 3600000L;
        long mins = (millis % 3600000L) / 60000L;
        long secs = (millis % 60000L) / 1000L;
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
            if (hours > 0) sb.append(hours).append("h ");
            if (mins > 0) sb.append(mins).append("m");
        } else {
            if (hours > 0) sb.append(hours).append("h ");
            if (mins > 0) sb.append(mins).append("m ");
            sb.append(secs).append("s");
        }
        return sb.toString().trim();
    }

    /**
     * Periodically checks for expired shops and resets them.
     */
    private void checkExpirations() {
        long now = System.currentTimeMillis();
        for (Shop shop : shops.values()) {
            if (shop.rented && shop.expires > 0 && shop.expires <= now) {
                shop.owner = "";
                shop.coowners.clear();
                shop.nickname = "";
                shop.nameColor = "<black>";
                shop.expires = 0L;
                shop.rented = false;
                clearRegion(shop);
                updateRegionOwners(shop);
            }
            updateSign(shop);
        }
        saveShops();
    }

    /**
     * Moves the HuskHomes warp for a shop back to its default location if defined.
     */
    private void resetWarpLocation(Shop shop) {
        if (shop.defaultTp == null || shop.defaultTp.isEmpty()) {
            return;
        }
        String[] parts = shop.defaultTp.split(";");
        if (parts.length < 6) {
            return;
        }
        org.bukkit.World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            return;
        }
        double x, y, z; float yaw, pitch;
        try {
            x = Double.parseDouble(parts[1]);
            y = Double.parseDouble(parts[2]);
            z = Double.parseDouble(parts[3]);
            yaw = Float.parseFloat(parts[4]);
            pitch = Float.parseFloat(parts[5]);
        } catch (NumberFormatException e) {
            return;
        }
        Location loc = new Location(world, x, y, z, yaw, pitch);
        String serverName = plugin.getConfig().getString("serverName", "server");
        HuskHomesAPI api = HuskHomesAPI.getInstance();
        Position pos = api.adaptPosition(loc, serverName);
        api.getWarp(shop.id).thenAccept(opt -> {
            if (opt.isPresent()) {
                api.relocateWarp(shop.id, pos);
            } else {
                api.createWarp(shop.id, pos);
            }
        });
    }

//    /**
//     * Reads existing HuskHomes warp positions and stores them as defaults.
//     */
//    private void readDefaultWarps() {
//        HuskHomesAPI api = HuskHomesAPI.getInstance();
//        for (Shop shop : shops.values()) {
//            api.getWarp(shop.id).thenAccept(opt -> opt.ifPresent(warp -> {
//                String worldName = warp.getWorld().getName();
//                shop.defaultTp = String.format(
//                        "%s;%f;%f;%f;%f;%f",
//                        worldName,
//                        warp.getX(),
//                        warp.getY(),
//                        warp.getZ(),
//                        warp.getYaw(),
//                        warp.getPitch()
//                );
//                saveShops();
//            }));
//        }
//    }

    /**
     * Container for shop configuration and runtime state.
     */
    private static class Shop {
        /** WorldGuard region id for this shop. */
        final String id;
        /** Player-specified nickname displayed on signs. */
        String nickname;
        /** Color code prefix for the name on signs. */
        String nameColor = "<black>";
        /** Sign location data in the format {@code world;x;y;z;facing}. */
        String sign;
        /** Default warp location string {@code world;x;y;z;yaw;pitch}. */
        String defaultTp = "";
        /** Cost to extend rental time. */
        double price;
        /** Amount of time purchased per extension in milliseconds. */
        long extendTime;
        /** Maximum time that can be purchased in advance. */
        long maxExtendTime;
        /** UUID of the owner or empty when unrented. */
        String owner = "";
        /** UUIDs of co-owners if any. */
        Set<String> coowners = new HashSet<>();
        /** Timestamp when the rental expires. */
        long expires = 0L;
        /** Whether the region is currently rented. */
        boolean rented = false;

        Shop(String id) { this.id = id; }
    }
}

