package at.sleazlee.bmessentials.Shops;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import at.sleazlee.bmessentials.bmefunctions.IsInWorldGuardRegion;
import at.sleazlee.bmessentials.EconomySystem.BMSEconomyProvider;
import net.milkbowl.vault2.economy.Economy;
import net.milkbowl.vault2.economy.EconomyResponse;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * Handles the Block Miner Shops system.
 * <p>
 * This class manages rentable WorldGuard regions and provides the
 * command logic for players to buy, disband, invite others, transfer
 * ownership, extend rent time and rename their shops. Data is stored in a
 * {@code shops.yml} file located inside the plugin's data folder.
 */
public class Shops implements CommandExecutor, TabCompleter {

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

        Scheduler.runTimer(this::checkExpirations, 20L * 60, 20L * 60);
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
            shop.nickname = sec.getString("Nickname", "");
            shop.sign = sec.getString("Sign", "");
            shop.price = sec.getDouble("Price", 100.0);
            shop.extendTime = sec.getLong("extendTime", 604800000L);
            shop.maxExtendTime = sec.getLong("maxExtendTime", 31536000000L);
            shop.owner = sec.getString("Owner", "");
            shop.coowner = sec.getString("CoOwner", "");
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
            sec.set("Nickname", shop.nickname);
            sec.set("Sign", shop.sign);
            sec.set("Price", shop.price);
            sec.set("extendTime", shop.extendTime);
            sec.set("maxExtendTime", shop.maxExtendTime);
            sec.set("Owner", shop.owner);
            sec.set("CoOwner", shop.coowner);
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
            case "buy" -> handleBuy(player);
            case "disband" -> handleDisband(player, args);
            case "invite" -> {
                if (args.length < 2) {
                    send(player, "invite-usage");
                    yield true;
                }
                yield handleInvite(player, args[1]);
            }
            case "transfer" -> {
                if (args.length < 2) {
                    send(player, "transfer-usage");
                    yield true;
                }
                yield handleTransfer(player, args[1]);
            }
            case "extend" -> handleExtend(player);
            case "rename" -> {
                if (args.length < 2) {
                    send(player, "rename-usage");
                    yield true;
                }
                yield handleRename(player, args[1]);
            }
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
        if (args.length == 1) {
            return Arrays.asList("buy","disband","invite","transfer","extend","rename");
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
    private boolean handleBuy(Player player) {
        Shop shop = shopAt(player);
        if (shop == null) {
            send(player, "not-in-shop");
            return true;
        }
        if (!shop.owner.isEmpty()) {
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
        shop.nickname = player.getName() + "'s shop";
        shop.expires = System.currentTimeMillis() + shop.extendTime;
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
        shop.coowner = "";
        shop.nickname = "";
        shop.expires = 0L;
        saveShops();
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
        shop.coowner = target.getUniqueId().toString();
        saveShops();
        send(player, "coowner-added", "player", target.getName());
        send(target, "you-are-coowner", "id", shop.id);
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
        if (shop.coowner.isEmpty()) {
            send(player, "no-coowner");
            return true;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.getUniqueId().toString().equals(shop.coowner)) {
            send(player, "not-your-coowner");
            return true;
        }
        shop.owner = shop.coowner;
        shop.coowner = "";
        shop.nickname = target.getName() + "'s shop";
        saveShops();
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
            if (uuid.equals(shop.owner) || uuid.equals(shop.coowner)) return shop;
        }
        return null;
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
        Block block = world.getBlockAt(x, y, z);
        if (!(block.getState() instanceof Sign sign)) return;
        if (shop.owner.isEmpty()) {
            String name = shop.nickname.isEmpty() ? shop.id : shop.nickname;
            sign.setLine(0, "For Rent");
            sign.setLine(1, name);
            sign.setLine(2, shop.price + "/" + timeLabel(shop.extendTime));
            sign.setLine(3, timeLabel(shop.maxExtendTime));
        } else {
            long remaining = Math.max(0, shop.expires - System.currentTimeMillis());
            sign.setLine(0, "Rented");
            sign.setLine(1, shop.nickname.isEmpty() ? shop.id : shop.nickname);
            sign.setLine(2, shop.price + "/" + timeLabel(shop.extendTime));
            sign.setLine(3, timeLabel(remaining));
        }
        sign.update();
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
     * Periodically checks for expired shops and resets them.
     */
    private void checkExpirations() {
        long now = System.currentTimeMillis();
        for (Shop shop : shops.values()) {
            if (!shop.owner.isEmpty() && shop.expires > 0 && shop.expires <= now) {
                shop.owner = "";
                shop.coowner = "";
                shop.nickname = "";
                shop.expires = 0L;
                updateSign(shop);
            }
        }
        saveShops();
    }

    /**
     * Container for shop configuration and runtime state.
     */
    private static class Shop {
        /** WorldGuard region id for this shop. */
        final String id;
        /** Player-specified nickname displayed on signs. */
        String nickname;
        /** Sign location data in the format {@code world;x;y;z;facing}. */
        String sign;
        /** Cost to extend rental time. */
        double price;
        /** Amount of time purchased per extension in milliseconds. */
        long extendTime;
        /** Maximum time that can be purchased in advance. */
        long maxExtendTime;
        /** UUID of the owner or empty when unrented. */
        String owner = "";
        /** UUID of the co-owner if any. */
        String coowner = "";
        /** Timestamp when the rental expires. */
        long expires = 0L;

        Shop(String id) { this.id = id; }
    }
}

