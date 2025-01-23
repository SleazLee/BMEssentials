package at.sleazlee.bmessentials.EconomySystem;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.PlayerData.PlayerDatabaseManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault2.economy.Economy;
import net.milkbowl.vault2.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class EconomyCommands implements CommandExecutor, TabCompleter {

    private final BMEssentials plugin;
    private final PlayerDatabaseManager db;
    private final Economy economy; // VaultUnlocked Economy

    public EconomyCommands(BMEssentials plugin) {
        this.plugin = plugin;
        this.db = plugin.getPlayerDataDBManager();

        // Attempt to get a net.milkbowl.vault2.economy.Economy instance
        RegisteredServiceProvider<Economy> rsp = plugin.getServer()
                .getServicesManager()
                .getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
        } else {
            economy = null;
        }
    }

    // -------------------------------------------------------------------------
    // COMMAND HANDLER
    // -------------------------------------------------------------------------
    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        String cmdName = command.getName().toLowerCase(Locale.ROOT);

        switch (cmdName) {
            case "pay":
                return handlePay(sender, args);

            case "bal":
            case "money":
                return handleBal(sender);

            case "baltop":
            case "moneytop":
                return handleBalTop(sender, args);

            case "eco":
                return handleEco(sender, args);

            default:
                return false;
        }
    }

    // -------------------------------------------------------------------------
    // /pay <player> <amount> [currency]
    // -------------------------------------------------------------------------
    private boolean handlePay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Must be a player to use /pay!");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("Usage: /pay <player> <amount> [currency]");
            return true;
        }

        Player player = (Player) sender;
        String targetName = args[0];
        String amountStr = args[1];
        String currency = (args.length >= 3) ? args[2] : BMSEconomyProvider.CURRENCY_DOLLARS;

        OfflinePlayer target = getOfflinePlayer(targetName);
        if (target == null || !target.hasPlayedBefore()) {
            player.sendMessage(mini("<red>Player not found!"));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            player.sendMessage(mini("<red>Invalid amount."));
            return true;
        }

        if (economy == null) {
            player.sendMessage(mini("<red>Economy not available!"));
            return true;
        }

        // 1) Withdraw from sender
        EconomyResponse withdrawResp = economy.withdraw(
                plugin.getName(),
                player.getUniqueId(),
                "no_world",
                currency,
                BigDecimal.valueOf(amount)
        );
        if (!withdrawResp.transactionSuccess()) {
            player.sendMessage(mini("<red>Transaction failed: " + withdrawResp.errorMessage));
            return true;
        }

        // 2) Deposit to receiver
        EconomyResponse depositResp = economy.deposit(
                plugin.getName(),
                target.getUniqueId(),
                "no_world",
                currency,
                BigDecimal.valueOf(amount)
        );
        if (!depositResp.transactionSuccess()) {
            // If deposit fails for some reason, rollback the sender's withdrawal
            economy.deposit(plugin.getName(),
                    player.getUniqueId(),
                    "no_world",
                    currency,
                    BigDecimal.valueOf(amount));
            player.sendMessage(mini("<red>Payment failed: " + depositResp.errorMessage));
            return true;
        }

        // 3) Success messages
        double senderBalance = economy.balance(plugin.getName(), player.getUniqueId(), "no_world", currency).doubleValue();
        player.sendMessage(mini("<gray>You paid <white>" + targetName +
                " " + economy.format(plugin.getName(), BigDecimal.valueOf(amount), currency) +
                "</white>. Your new balance: " +
                economy.format(plugin.getName(), BigDecimal.valueOf(senderBalance), currency)));

        if (target.isOnline()) {
            Player onlineTarget = (Player) target;
            double targetBalance = economy.balance(plugin.getName(), onlineTarget.getUniqueId(), "no_world", currency).doubleValue();
            onlineTarget.sendMessage(mini("<gray>You received <white>" +
                    economy.format(plugin.getName(), BigDecimal.valueOf(amount), currency) +
                    "</white> from " + player.getName() +
                    ". New balance: " +
                    economy.format(plugin.getName(), BigDecimal.valueOf(targetBalance), currency)));
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // /bal OR /money
    // -------------------------------------------------------------------------
    private boolean handleBal(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Must be a player to use /bal or /money!");
            return true;
        }
        Player player = (Player) sender;
        if (economy == null) {
            player.sendMessage(mini("<red>Economy not available!"));
            return true;
        }

        double dollars = economy.balance(plugin.getName(), player.getUniqueId(), "no_world", BMSEconomyProvider.CURRENCY_DOLLARS).doubleValue();
        double votePoints = economy.balance(plugin.getName(), player.getUniqueId(), "no_world", BMSEconomyProvider.CURRENCY_VP).doubleValue();

        player.sendMessage(mini(
                "<aqua><bold>BM</bold> <gray>Your balance is <green>$" + String.format("%.2f", dollars) +
                        "</green> <gray>and <yellow>" + String.format("%.2f", votePoints) + "VP</yellow><gray>."
        ));
        return true;
    }

    // -------------------------------------------------------------------------
    // /baltop [currency] OR /moneytop [currency]
    // -------------------------------------------------------------------------
    private boolean handleBalTop(CommandSender sender, String[] args) {
        if (economy == null) {
            sender.sendMessage(mini("<red>Economy not available!"));
            return true;
        }

        String currency = BMSEconomyProvider.CURRENCY_DOLLARS;
        if (args.length >= 1 && args[0].equalsIgnoreCase("votepoints")) {
            currency = BMSEconomyProvider.CURRENCY_VP;
        }

        String column = currency.equalsIgnoreCase(BMSEconomyProvider.CURRENCY_VP) ? "votepoints" : "dollars";
        String topList = db.getTopBalances(column, 10);

        sender.sendMessage(mini("<gold><bold>--- Top 10 " + currency + " ---</bold>"));
        if (topList.isEmpty()) {
            sender.sendMessage(mini("<gray>No data available!"));
        } else {
            sender.sendMessage(mini("<gray>" + topList));
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // ECO HANDLER  (OP Only)
    // -------------------------------------------------------------------------
    private boolean handleEco(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(mini("<yellow>Usage: /eco <give|take|set> <player> <amount> [currency]"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give":
                return handleEcoGive(sender, Arrays.copyOfRange(args, 1, args.length));
            case "take":
                return handleEcoTake(sender, Arrays.copyOfRange(args, 1, args.length));
            case "set":
                return handleEcoSet(sender, Arrays.copyOfRange(args, 1, args.length));
            default:
                sender.sendMessage(mini("<yellow>Usage: /eco <give|take|set> <player> <amount> [currency]"));
                return true;
        }
    }

    private boolean handleEcoGive(CommandSender sender, String[] args) {
        // Check OP (or could check a permission "bmessentials.eco.give")
        if (!sender.isOp()) {
            sender.sendMessage(mini("<red>You must be OP (or have permission) to run this command."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(mini("<yellow>Usage: /eco give <player> <amount> [currency]"));
            return true;
        }

        String playerName = args[0];
        String amountStr = args[1];
        String currency = (args.length >= 3) ? args[2] : BMSEconomyProvider.CURRENCY_DOLLARS;

        OfflinePlayer target = getOfflinePlayer(playerName);
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(mini("<red>That player has never joined or doesn't exist."));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(mini("<red>Invalid amount."));
            return true;
        }

        if (economy == null) {
            sender.sendMessage(mini("<red>Economy not available!"));
            return true;
        }

        EconomyResponse resp = economy.deposit(
                plugin.getName(),
                target.getUniqueId(),
                "no_world",
                currency,
                BigDecimal.valueOf(amount)
        );
        if (!resp.transactionSuccess()) {
            sender.sendMessage(mini("<red>Failed to give money: " + resp.errorMessage));
            return true;
        }

        sender.sendMessage(mini("<green>You gave <white>" + playerName +
                " " + economy.format(plugin.getName(), BigDecimal.valueOf(amount), currency) +
                "</white>."));
        if (target.isOnline()) {
            Player onlineTarget = (Player) target;
            double newBal = economy.balance(plugin.getName(), onlineTarget.getUniqueId(), "no_world", currency).doubleValue();
            onlineTarget.sendMessage(mini("<gray>You were given <white>" +
                    economy.format(plugin.getName(), BigDecimal.valueOf(amount), currency) +
                    "</white> by " + sender.getName() +
                    ". New balance: " +
                    economy.format(plugin.getName(), BigDecimal.valueOf(newBal), currency)));
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // NEW: /eco take <player> <amount> [currency]
    // -------------------------------------------------------------------------
    /**
     * Handles the eco take command - removes currency from a player's balance
     * @param sender Command sender
     * @param args Command arguments [player, amount, currency?]
     * @return true if command was handled
     */
    private boolean handleEcoTake(CommandSender sender, String[] args) {
        if (!checkEcoPermission(sender)) return true;

        if (args.length < 2) {
            sender.sendMessage(mini("<yellow>Usage: /eco take <player> <amount> [currency]"));
            return true;
        }

        return handleEcoTransaction(sender, args, TransactionType.TAKE);
    }

    // -------------------------------------------------------------------------
    // NEW: /eco set <player> <amount> [currency]
    // -------------------------------------------------------------------------
    /**
     * Handles the eco set command - sets a player's balance to exact amount
     * @param sender Command sender
     * @param args Command arguments [player, amount, currency?]
     * @return true if command was handled
     */
    private boolean handleEcoSet(CommandSender sender, String[] args) {
        if (!checkEcoPermission(sender)) return true;

        if (args.length < 2) {
            sender.sendMessage(mini("<yellow>Usage: /eco set <player> <amount> [currency]"));
            return true;
        }

        String playerName = args[0];
        String amountStr = args[1];
        String currency = (args.length >= 3) ? args[2] : BMSEconomyProvider.CURRENCY_DOLLARS;

        OfflinePlayer target = getOfflinePlayer(playerName);
        if (invalidTarget(sender, target)) return true;

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(mini("<red>Invalid amount."));
            return true;
        }

        if (economy == null) {
            sender.sendMessage(mini("<red>Economy not available!"));
            return true;
        }

        // Calculate needed adjustment
        BigDecimal current = economy.balance(plugin.getName(), target.getUniqueId(), "no_world", currency);
        BigDecimal desired = BigDecimal.valueOf(amount);
        BigDecimal difference = desired.subtract(current);

        EconomyResponse resp;
        if (difference.compareTo(BigDecimal.ZERO) > 0) {
            resp = economy.deposit(plugin.getName(), target.getUniqueId(), "no_world", currency, difference);
        } else if (difference.compareTo(BigDecimal.ZERO) < 0) {
            resp = economy.withdraw(plugin.getName(), target.getUniqueId(), "no_world", currency, difference.abs());
        } else {
            sender.sendMessage(mini("<yellow>Player already has exactly " + desired));
            return true;
        }

        handleEcoResponse(sender, target, resp, desired, currency, "set");
        return true;
    }


    // -------------------------------------------------------------------------
    // SHARED ECO HELPER METHODS
    // -------------------------------------------------------------------------
    private enum TransactionType { GIVE, TAKE }

    /**
     * Checks if sender has permission to use eco commands
     * @param sender Command sender
     * @return true if authorized
     */
    private boolean checkEcoPermission(CommandSender sender) {
        if (!sender.isOp() && !sender.hasPermission("bmessentials.eco.admin")) {
            sender.sendMessage(mini("<red>You don't have permission for this command!"));
            return false;
        }
        return true;
    }

    /**
     * Validates if target player exists
     * @param sender Command sender for error messages
     * @param target Player being checked
     * @return true if target is invalid
     */
    private boolean invalidTarget(CommandSender sender, OfflinePlayer target) {
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(mini("<red>Player not found!"));
            return true;
        }
        return false;
    }

    /**
     * Handles generic eco transactions (give/take)
     * @param sender Command sender
     * @param args Command arguments
     * @param type Transaction type (GIVE/TAKE)
     */
    private boolean handleEcoTransaction(CommandSender sender, String[] args, TransactionType type) {
        String playerName = args[0];
        String amountStr = args[1];
        String currency = (args.length >= 3) ? args[2] : BMSEconomyProvider.CURRENCY_DOLLARS;

        OfflinePlayer target = getOfflinePlayer(playerName);
        if (invalidTarget(sender, target)) return true;

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(mini("<red>Invalid amount."));
            return true;
        }

        if (economy == null) {
            sender.sendMessage(mini("<red>Economy not available!"));
            return true;
        }

        EconomyResponse resp;
        BigDecimal bigAmount = BigDecimal.valueOf(amount);

        if (type == TransactionType.GIVE) {
            resp = economy.deposit(plugin.getName(), target.getUniqueId(), "no_world", currency, bigAmount);
        } else {
            resp = economy.withdraw(plugin.getName(), target.getUniqueId(), "no_world", currency, bigAmount);
        }

        handleEcoResponse(sender, target, resp, bigAmount, currency, type.name().toLowerCase());
        return true;
    }

    /**
     * Handles response from economy operations
     * @param sender Command sender
     * @param target Target player
     * @param resp Economy response
     * @param amount Amount involved
     * @param currency Currency type
     * @param action Action performed (give/take/set)
     */
    private void handleEcoResponse(CommandSender sender, OfflinePlayer target, EconomyResponse resp,
                                   BigDecimal amount, String currency, String action) {
        if (!resp.transactionSuccess()) {
            sender.sendMessage(mini("<red>Failed to " + action + " money: " + resp.errorMessage));
            return;
        }

        String formattedAmount = economy.format(plugin.getName(), amount, currency);
        sender.sendMessage(mini("<green>Successfully " + action + " <white>" + formattedAmount +
                "</white> to/from " + target.getName()));

        if (target.isOnline()) {
            Player onlineTarget = (Player) target;
            BigDecimal newBalance = economy.balance(plugin.getName(), onlineTarget.getUniqueId(), "no_world", currency);
            onlineTarget.sendMessage(mini("<gray>Your balance was adjusted by <white>" +
                    formattedAmount + "</white>. New balance: " +
                    economy.format(plugin.getName(), newBalance, currency)));
        }
    }

    // -------------------------------------------------------------------------
    // TAB COMPLETION
    // -------------------------------------------------------------------------
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      @NotNull String[] args) {

        String cmdName = command.getName().toLowerCase(Locale.ROOT);

        switch (cmdName) {
            // /pay <player> <amount> [currency]
            case "pay":
                return tabCompletePay(args);

            // /bal OR /money (no arguments)
            case "bal":
            case "money":
                return Collections.emptyList();

            // /baltop [currency] OR /moneytop [currency]
            case "baltop":
            case "moneytop":
                return tabCompleteBalTop(args);

            // /eco give <player> <amount> [currency]
            case "eco":
                return tabCompleteEco(args);

            default:
                return Collections.emptyList();
        }
    }

    private List<String> tabCompletePay(String[] args) {
        switch (args.length) {
            case 1:
                // Player names
                return getOnlinePlayerNames(args[0]);
            case 2:
                // Amount? We'll just return empty or you can suggest "10", "100", etc.
                return Collections.emptyList();
            case 3:
                // Currency suggestions
                return filterByPrefix(Arrays.asList(
                        BMSEconomyProvider.CURRENCY_DOLLARS,
                        BMSEconomyProvider.CURRENCY_VP), args[2]);
            default:
                return Collections.emptyList();
        }
    }

    private List<String> tabCompleteBalTop(String[] args) {
        // /baltop [currency?]
        if (args.length == 1) {
            return filterByPrefix(Arrays.asList(
                    BMSEconomyProvider.CURRENCY_DOLLARS,
                    BMSEconomyProvider.CURRENCY_VP), args[0]);
        }
        return Collections.emptyList();
    }

    private List<String> tabCompleteEco(String[] args) {
        if (args.length == 1) {
            // subcommands: "give"
            return filterByPrefix(Arrays.asList("give", "take", "set"), args[0]);
        } else if (args.length == 2) {
            // player name
            return getOnlinePlayerNames(args[1]);
        } else if (args.length == 3) {
            // amount? We can leave empty or provide suggestions
            return Collections.emptyList();
        } else if (args.length == 4) {
            // currency suggestions
            return filterByPrefix(Arrays.asList(
                    BMSEconomyProvider.CURRENCY_DOLLARS,
                    BMSEconomyProvider.CURRENCY_VP), args[3]);
        }
        return Collections.emptyList();
    }

    // -------------------------------------------------------------------------
    // HELPER METHODS
    // -------------------------------------------------------------------------
    private net.kyori.adventure.text.Component mini(String message) {
        return MiniMessage.miniMessage().deserialize(message);
    }

    private OfflinePlayer getOfflinePlayer(String name) {
        // Attempt a direct cache check
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
        if (cached != null) return cached;
        // else fallback
        return Bukkit.getOfflinePlayer(name);
    }

    /**
     * Returns a list of online players matching a prefix.
     */
    private List<String> getOnlinePlayerNames(String prefix) {
        prefix = prefix.toLowerCase(Locale.ROOT);
        String finalPrefix = prefix;
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(finalPrefix))
                .collect(Collectors.toList());
    }

    /**
     * Filters a list by prefix.
     */
    private List<String> filterByPrefix(List<String> options, String prefix) {
        prefix = prefix.toLowerCase(Locale.ROOT);
        List<String> results = new ArrayList<>();
        for (String opt : options) {
            if (opt.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                results.add(opt);
            }
        }
        return results;
    }
}