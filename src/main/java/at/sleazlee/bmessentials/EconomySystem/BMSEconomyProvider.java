package at.sleazlee.bmessentials.EconomySystem;

import net.milkbowl.vault2.economy.AccountPermission;
import net.milkbowl.vault2.economy.Economy;
import net.milkbowl.vault2.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.PlayerData.PlayerDatabaseManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Minimal Economy implementation for VaultUnlocked,
 * storing Dollars and VotePoints in an SQLite database.
 */
public class BMSEconomyProvider implements Economy {

    private final BMEssentials plugin;
    private final PlayerDatabaseManager db;

    // For demonstration, let's define known currency types:
    public static final String CURRENCY_DOLLARS = "Dollars";
    public static final String CURRENCY_VP = "VotePoints";

    public BMSEconomyProvider(BMEssentials plugin, PlayerDatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    @Override
    public boolean isEnabled() {
        return true; // Our economy is "enabled" once the plugin is loaded
    }

    @NotNull
    @Override
    public String getName() {
        return "BMSEconomy";
    }

    @Override
    public boolean hasSharedAccountSupport() {
        // Return false if you don't want to implement "shared" / "guild" accounts
        return false;
    }

    @Override
    public boolean hasMultiCurrencySupport() {
        // We do support multiple currencies: Dollars and VotePoints
        return true;
    }

    @Override
    public int fractionalDigits(@NotNull String pluginName) {
        // Number of digits after decimal. Return -1 to indicate no limit, or 2 for typical currency.
        return 2;
    }

    /**
     * @deprecated in VaultUnlocked, replaced by a multi-currency-aware method.
     */
    @Deprecated
    @NotNull
    @Override
    public String format(@NotNull BigDecimal amount) {
        // This is the old method. We’ll just default to format as Dollars
        return format("BMEssentials", amount, CURRENCY_DOLLARS);
    }

    @NotNull
    @Override
    public String format(@NotNull String pluginName, @NotNull BigDecimal amount) {
        // Again, default to Dollars
        return format(pluginName, amount, CURRENCY_DOLLARS);
    }

    /**
     * @deprecated in VaultUnlocked
     */
    @Deprecated
    @Override
    @NotNull
    public String format(@NotNull BigDecimal amount, @NotNull String currency) {
        // Old method. We'll just delegate:
        return format("BMEssentials", amount, currency);
    }

    @NotNull
    @Override
    public String format(@NotNull String pluginName, @NotNull BigDecimal amount, @NotNull String currency) {
        if (currency.equalsIgnoreCase(CURRENCY_VP)) {
            // Round down to the nearest whole number for VP
            BigDecimal vpAmount = amount.setScale(0, RoundingMode.DOWN);
            long vp = vpAmount.longValueExact();
            String suffix = vp == 1 ? "VP" : "VPs";
            return String.format("%,d %s", vp, suffix);
        } else {
            // Format Dollars with commas and two decimal places
            return String.format("$%,.2f", amount.doubleValue());
        }
    }

    @Override
    public boolean hasCurrency(@NotNull String currency) {
        // We support exactly two: "Dollars" and "VotePoints"
        return CURRENCY_DOLLARS.equalsIgnoreCase(currency) || CURRENCY_VP.equalsIgnoreCase(currency);
    }

    @NotNull
    @Override
    public String getDefaultCurrency(@NotNull String pluginName) {
        // Default to "Dollars" for your plugin
        return CURRENCY_DOLLARS;
    }

    @NotNull
    @Override
    public String defaultCurrencyNamePlural(@NotNull String pluginName) {
        // If you wanted to localize, you can. We'll just say "Dollars"
        return "Dollars";
    }

    @NotNull
    @Override
    public String defaultCurrencyNameSingular(@NotNull String pluginName) {
        // "Dollar"
        return "Dollar";
    }

    @NotNull
    @Override
    public Collection<String> currencies() {
        // Return both currency names
        return Arrays.asList(CURRENCY_DOLLARS, CURRENCY_VP);
    }

    // We won’t implement shared accounts for simplicity, so many of these can remain minimal/no-op
    @Deprecated
    @Override
    public boolean createAccount(@NotNull UUID accountID, @NotNull String name) {
        return createAccount(accountID, name, true);
    }

    @Override
    public boolean createAccount(@NotNull UUID accountID, @NotNull String name, boolean player) {
        // If the player doesn't exist, insert them into the DB with zero balances
        String uuidStr = accountID.toString();
        if (!db.hasPlayerData(uuidStr)) {
            db.insertPlayerData(uuidStr, System.currentTimeMillis());
        }
        return true;
    }

    @Deprecated
    @Override
    public boolean createAccount(@NotNull UUID accountID, @NotNull String name, @NotNull String worldName) {
        return createAccount(accountID, name, true);
    }

    @Override
    public boolean createAccount(@NotNull UUID accountID, @NotNull String name, @NotNull String worldName, boolean player) {
        // We’re ignoring world-based economies for now
        return createAccount(accountID, name, player);
    }

    @Override
    public @NotNull Map<UUID, String> getUUIDNameMap() {
        // For simplicity, we’ll just return an empty map or implement a method if you have a name cache
        return Collections.emptyMap();
    }

    @Override
    public Optional<String> getAccountName(@NotNull UUID accountID) {
        // If you store names in the DB, you could retrieve them. We'll skip that logic for brevity.
        return Optional.empty();
    }

    @Override
    public boolean hasAccount(@NotNull UUID accountID) {
        return db.hasPlayerData(accountID.toString());
    }

    @Override
    public boolean hasAccount(@NotNull UUID accountID, @NotNull String worldName) {
        // Not implementing multi-world. Just delegate.
        return hasAccount(accountID);
    }

    @Override
    public boolean renameAccount(@NotNull UUID accountID, @NotNull String name) {
        // If you want to store a "last known username" you can update here
        return true;
    }

    @Override
    public boolean renameAccount(@NotNull String plugin, @NotNull UUID accountID, @NotNull String name) {
        return renameAccount(accountID, name);
    }

    @Override
    public boolean deleteAccount(@NotNull String plugin, @NotNull UUID accountID) {
        // If you want to remove them from the DB, do so here
        return false;
    }

    @Override
    public boolean accountSupportsCurrency(@NotNull String plugin, @NotNull UUID accountID, @NotNull String currency) {
        // We only have 2 currencies, so yes if the user is in the DB
        return hasAccount(accountID) && hasCurrency(currency);
    }

    @Override
    public boolean accountSupportsCurrency(@NotNull String plugin, @NotNull UUID accountID, @NotNull String currency, @NotNull String world) {
        return accountSupportsCurrency(plugin, accountID, currency);
    }

    // -------------------------
    // Balance & Has
    // -------------------------

    @Deprecated
    @NotNull
    @Override
    public BigDecimal getBalance(@NotNull String pluginName, @NotNull UUID accountID) {
        return balance(pluginName, accountID);
    }

    @Deprecated
    @NotNull
    @Override
    public BigDecimal getBalance(@NotNull String pluginName, @NotNull UUID accountID, @NotNull String world) {
        return balance(pluginName, accountID, world);
    }

    @Deprecated
    @NotNull
    @Override
    public BigDecimal getBalance(@NotNull String pluginName, @NotNull UUID accountID, @NotNull String world, @NotNull String currency) {
        return balance(pluginName, accountID, world, currency);
    }

    @Override
    public @NotNull BigDecimal balance(@NotNull String pluginName, @NotNull UUID accountID) {
        // Return the default currency (Dollars)
        return balance(pluginName, accountID, "no_world", CURRENCY_DOLLARS);
    }

    @Override
    public @NotNull BigDecimal balance(@NotNull String pluginName, @NotNull UUID accountID, @NotNull String world) {
        // Return the default currency for that “world”
        return balance(pluginName, accountID, world, CURRENCY_DOLLARS);
    }

    @Override
    public @NotNull BigDecimal balance(@NotNull String pluginName,
                                       @NotNull UUID accountID,
                                       @NotNull String world,
                                       @NotNull String currency) {
        // Actually fetch from DB
        if (!hasAccount(accountID) || !hasCurrency(currency)) return BigDecimal.ZERO;

        String uuidStr = accountID.toString();
        if (CURRENCY_VP.equalsIgnoreCase(currency)) {
            return BigDecimal.valueOf(db.getVotePoints(uuidStr));
        } else {
            return BigDecimal.valueOf(db.getDollars(uuidStr));
        }
    }

    @Override
    public boolean has(@NotNull String pluginName,
                       @NotNull UUID accountID,
                       @NotNull BigDecimal amount) {
        return has(pluginName, accountID, "no_world", amount);
    }

    @Override
    public boolean has(@NotNull String pluginName,
                       @NotNull UUID accountID,
                       @NotNull String worldName,
                       @NotNull BigDecimal amount) {
        // default currency
        return has(pluginName, accountID, worldName, CURRENCY_DOLLARS, amount);
    }

    @Override
    public boolean has(@NotNull String pluginName,
                       @NotNull UUID accountID,
                       @NotNull String worldName,
                       @NotNull String currency,
                       @NotNull BigDecimal amount) {
        BigDecimal bal = balance(pluginName, accountID, worldName, currency);
        return bal.compareTo(amount) >= 0;
    }

    // -------------------------
    // Deposit & Withdraw
    // -------------------------
    @NotNull
    @Override
    public EconomyResponse withdraw(@NotNull String pluginName,
                                    @NotNull UUID accountID,
                                    @NotNull BigDecimal amount) {
        return withdraw(pluginName, accountID, "no_world", amount);
    }

    @NotNull
    @Override
    public EconomyResponse withdraw(@NotNull String pluginName,
                                    @NotNull UUID accountID,
                                    @NotNull String worldName,
                                    @NotNull BigDecimal amount) {
        return withdraw(pluginName, accountID, worldName, CURRENCY_DOLLARS, amount);
    }

    @NotNull
    @Override
    public EconomyResponse withdraw(@NotNull String pluginName,
                                    @NotNull UUID accountID,
                                    @NotNull String worldName,
                                    @NotNull String currency,
                                    @NotNull BigDecimal amount) {
        if (!hasAccount(accountID) || !hasCurrency(currency)) {
            return new EconomyResponse(amount, BigDecimal.ZERO, EconomyResponse.ResponseType.FAILURE,
                    "Account or currency doesn't exist.");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return new EconomyResponse(amount, BigDecimal.ZERO, EconomyResponse.ResponseType.FAILURE,
                    "Cannot withdraw negative amounts.");
        }

        BigDecimal oldBalance = balance(pluginName, accountID, worldName, currency);
        if (oldBalance.compareTo(amount) < 0) {
            return new EconomyResponse(amount, oldBalance, EconomyResponse.ResponseType.FAILURE,
                    "Insufficient funds.");
        }

        double newVal = oldBalance.subtract(amount).doubleValue();
        updateBalance(accountID, currency, newVal);
        return new EconomyResponse(amount, BigDecimal.valueOf(newVal), EconomyResponse.ResponseType.SUCCESS, "");
    }

    @NotNull
    @Override
    public EconomyResponse deposit(@NotNull String pluginName,
                                   @NotNull UUID accountID,
                                   @NotNull BigDecimal amount) {
        return deposit(pluginName, accountID, "no_world", amount);
    }

    @NotNull
    @Override
    public EconomyResponse deposit(@NotNull String pluginName,
                                   @NotNull UUID accountID,
                                   @NotNull String worldName,
                                   @NotNull BigDecimal amount) {
        return deposit(pluginName, accountID, worldName, CURRENCY_DOLLARS, amount);
    }

    @NotNull
    @Override
    public EconomyResponse deposit(@NotNull String pluginName,
                                   @NotNull UUID accountID,
                                   @NotNull String worldName,
                                   @NotNull String currency,
                                   @NotNull BigDecimal amount) {
        if (!hasAccount(accountID) || !hasCurrency(currency)) {
            return new EconomyResponse(amount, BigDecimal.ZERO, EconomyResponse.ResponseType.FAILURE,
                    "Account or currency doesn't exist.");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return new EconomyResponse(amount, BigDecimal.ZERO, EconomyResponse.ResponseType.FAILURE,
                    "Cannot deposit negative amounts.");
        }

        BigDecimal oldBalance = balance(pluginName, accountID, worldName, currency);
        double newVal = oldBalance.add(amount).doubleValue();
        updateBalance(accountID, currency, newVal);
        return new EconomyResponse(amount, BigDecimal.valueOf(newVal), EconomyResponse.ResponseType.SUCCESS, "");
    }

    // -------------------------
    // Set (VaultUnlocked 2.10+)
    // -------------------------
    @NotNull
    @Override
    public EconomyResponse set(@NotNull String pluginName,
                               @NotNull UUID accountID,
                               @NotNull BigDecimal amount) {
        return set(pluginName, accountID, "no_world", amount);
    }

    @NotNull
    @Override
    public EconomyResponse set(@NotNull String pluginName,
                               @NotNull UUID accountID,
                               @NotNull String worldName,
                               @NotNull BigDecimal amount) {
        return set(pluginName, accountID, worldName, CURRENCY_DOLLARS, amount);
    }

    @NotNull
    @Override
    public EconomyResponse set(@NotNull String pluginName,
                               @NotNull UUID accountID,
                               @NotNull String worldName,
                               @NotNull String currency,
                               @NotNull BigDecimal amount) {
        if (!hasAccount(accountID) || !hasCurrency(currency)) {
            return new EconomyResponse(amount, BigDecimal.ZERO, EconomyResponse.ResponseType.FAILURE,
                    "Account or currency doesn't exist.");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return new EconomyResponse(amount, BigDecimal.ZERO, EconomyResponse.ResponseType.FAILURE,
                    "Cannot set negative amounts.");
        }

        updateBalance(accountID, currency, amount.doubleValue());
        return new EconomyResponse(amount, amount, EconomyResponse.ResponseType.SUCCESS, "");
    }

    private void updateBalance(UUID accountID, String currency, double newVal) {
        String uuidStr = accountID.toString();
        if (CURRENCY_VP.equalsIgnoreCase(currency)) {
            db.setVotePoints(uuidStr, newVal);
        } else {
            db.setDollars(uuidStr, newVal);
        }
    }

    // -------------------------
    // Shared Account / Guild / Bank placeholders
    // (We won't implement these in detail.)
    // -------------------------
    @Override
    public boolean createSharedAccount(@NotNull String pluginName,
                                       @NotNull UUID accountID,
                                       @NotNull String name,
                                       @NotNull UUID owner) {
        return false;
    }

    @Override
    public boolean isAccountOwner(@NotNull String pluginName,
                                  @NotNull UUID accountID,
                                  @NotNull UUID uuid) {
        return false;
    }

    @Override
    public boolean setOwner(@NotNull String pluginName,
                            @NotNull UUID accountID,
                            @NotNull UUID uuid) {
        return false;
    }

    @Override
    public boolean isAccountMember(@NotNull String pluginName,
                                   @NotNull UUID accountID,
                                   @NotNull UUID uuid) {
        return false;
    }

    @Override
    public boolean addAccountMember(@NotNull String pluginName,
                                    @NotNull UUID accountID,
                                    @NotNull UUID uuid) {
        return false;
    }

    @Override
    public boolean addAccountMember(@NotNull String pluginName,
                                    @NotNull UUID accountID,
                                    @NotNull UUID uuid,
                                    AccountPermission... initialPermissions) {
        return false;
    }

    @Override
    public boolean removeAccountMember(@NotNull String pluginName,
                                       @NotNull UUID accountID,
                                       @NotNull UUID uuid) {
        return false;
    }

    @Override
    public boolean hasAccountPermission(@NotNull String pluginName,
                                        @NotNull UUID accountID,
                                        @NotNull UUID uuid,
                                        @NotNull AccountPermission permission) {
        return false;
    }

    @Override
    public boolean updateAccountPermission(@NotNull String pluginName,
                                           @NotNull UUID accountID,
                                           @NotNull UUID uuid,
                                           @NotNull AccountPermission permission,
                                           boolean value) {
        return false;
    }
}