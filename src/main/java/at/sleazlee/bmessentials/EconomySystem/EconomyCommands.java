package at.sleazlee.bmessentials.EconomySystem;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.PlayerData.PlayerDatabaseManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import at.sleazlee.bmessentials.TextUtils.TextCenter;
import net.milkbowl.vault2.economy.Economy;
import net.milkbowl.vault2.economy.EconomyResponse;
import net.milkbowl.vault.chat.Chat;
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
    private final Map<UUID, Long> payCooldowns = new HashMap<>();

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
            case "balance":
            case "money":
                return handleBal(sender, args);

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
    // /pay <player> <amount> [currency] (with cooldown and self-payment check)
    // -------------------------------------------------------------------------
    /**
     * Handles the /pay command, allowing players to send currency to others.
     * Validates cooldowns, target existence, and sufficient funds before processing transactions.
     * Sends formatted success/error messages to both sender and receiver.
     *
     * @param sender The command sender (must be a player)
     * @param args The command arguments [player, amount, currency?]
     * @return true if the command was processed successfully
     */
    private boolean handlePay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Must be a player to use /pay!");
            return true;
        }

        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();

        // Check cooldown
        long currentTime = System.currentTimeMillis();
        if (payCooldowns.containsKey(playerUUID)) {
            long lastUsed = payCooldowns.get(playerUUID);
            if (currentTime - lastUsed < 3000) {
                player.sendMessage(mini("<red>You must wait 3 seconds between payments!"));
                return true;
            }
        }

        if (args.length < 2) {
            player.sendMessage(mini("Usage: /pay <player> <amount> [currency]"));
            return true;
        }

        String targetName = args[0];
        String amountStr = args[1];
        String currency = (args.length >= 3) ? args[2] : BMSEconomyProvider.CURRENCY_DOLLARS;

        OfflinePlayer target = getOfflinePlayer(targetName);
        if (target == null || !target.hasPlayedBefore()) {
            player.sendMessage(mini("<red>Player not found!"));
            return true;
        }

        // Prevent self-payment
        if (target.getUniqueId().equals(playerUUID)) {
            player.sendMessage(mini("<red><bold>ECO</bold> <red>You cannot pay yourself!"));
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

        // Withdraw from sender
        EconomyResponse withdrawResp = economy.withdraw(
                plugin.getName(),
                playerUUID,
                "no_world",
                currency,
                BigDecimal.valueOf(amount)
        );

        if (!withdrawResp.transactionSuccess()) {
            player.sendMessage(mini("<red>Transaction failed: " + withdrawResp.errorMessage));
            return true;
        }

        // Deposit to receiver
        EconomyResponse depositResp = economy.deposit(
                plugin.getName(),
                target.getUniqueId(),
                "no_world",
                currency,
                BigDecimal.valueOf(amount)
        );

        if (!depositResp.transactionSuccess()) {
            // Rollback withdrawal
            economy.deposit(
                    plugin.getName(),
                    playerUUID,
                    "no_world",
                    currency,
                    BigDecimal.valueOf(amount)
            );
            player.sendMessage(mini("<red>Payment failed: " + depositResp.errorMessage));
            return true;
        }

        // Update cooldown
        payCooldowns.put(playerUUID, currentTime);

        // Get formatted amounts and colors
        String color = currency.equalsIgnoreCase(BMSEconomyProvider.CURRENCY_VP) ? "yellow" : "green";
        String formattedAmount = economy.format(plugin.getName(), BigDecimal.valueOf(amount), currency);
        double senderBalance = economy.balance(
                plugin.getName(),
                playerUUID,
                "no_world",
                currency
        ).doubleValue();
        String formattedSenderBalance = economy.format(plugin.getName(), BigDecimal.valueOf(senderBalance), currency);

        // Build sender message
        String senderMessage = String.format(
                "<gray>You paid <white>%s</white> <%s>%s</%s><gray>. Your New balance is <%s>%s</%s><gray>.",
                targetName,
                color,
                formattedAmount,
                color,
                color,
                formattedSenderBalance,
                color
        );
        player.sendMessage(mini(senderMessage));

        // Notify receiver if online
        if (target.isOnline()) {
            Player onlineTarget = (Player) target;
            double targetBalance = economy.balance(
                    plugin.getName(),
                    onlineTarget.getUniqueId(),
                    "no_world",
                    currency
            ).doubleValue();
            String formattedTargetBalance = economy.format(plugin.getName(), BigDecimal.valueOf(targetBalance), currency);

            // Build receiver message
            String receiverMessage = String.format(
                    "<gray>You received <%s>%s</%s><gray> from <white>%s</white><gray>. Your new balance is <%s>%s</%s><gray>.",
                    color,
                    formattedAmount,
                    color,
                    player.getName(),
                    color,
                    formattedTargetBalance,
                    color
            );
            onlineTarget.sendMessage(mini(receiverMessage));
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // /bal OR /money
    // -------------------------------------------------------------------------
    private boolean handleBal(CommandSender sender, String[] args) {
        // First, check if the economy is loaded at all
        if (economy == null) {
            sender.sendMessage(mini("<red>Economy not available!"));
            return true;
        }

        // If no arguments, show sender's own balance
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(mini("<red>Usage: /balance <playerName>"));
                return true;
            }
            Player player = (Player) sender;

            double dollars = economy.balance(
                    plugin.getName(),
                    player.getUniqueId(),
                    "no_world",
                    BMSEconomyProvider.CURRENCY_DOLLARS
            ).doubleValue();
            double votePoints = economy.balance(
                    plugin.getName(),
                    player.getUniqueId(),
                    "no_world",
                    BMSEconomyProvider.CURRENCY_VP
            ).doubleValue();

            player.sendMessage(mini(
                    "<aqua><bold>BM</bold> <gray>Your balance is <green>$" + String.format("%.2f", dollars) +
                            "</green> <gray>and <yellow>" + String.format("%,d", (long) votePoints) + "VPs</yellow><gray>."
            ));
            return true;

            // If exactly one argument, interpret it as a target player's name
        } else if (args.length == 1) {
            String targetName = args[0];
            OfflinePlayer targetOffline = getOfflinePlayer(targetName);

            // If the server doesn't even recognize the name, or the target never joined
            if (targetOffline == null || !targetOffline.hasPlayedBefore()) {
                sender.sendMessage(mini("<red>Player not found."));
                return true;
            }

            // If the plugin's economy DB doesn't have them
            UUID targetUUID = targetOffline.getUniqueId();
            if (!economy.hasAccount(targetUUID, "no_world")) {
                sender.sendMessage(mini("<red>Player not found."));
                return true;
            }

            // Now fetch their balances
            double dollars = economy.balance(
                    plugin.getName(),
                    targetUUID,
                    "no_world",
                    BMSEconomyProvider.CURRENCY_DOLLARS
            ).doubleValue();
            double votePoints = economy.balance(
                    plugin.getName(),
                    targetUUID,
                    "no_world",
                    BMSEconomyProvider.CURRENCY_VP
            ).doubleValue();

            sender.sendMessage(mini("<aqua><bold>BM</bold> <gray>" +
                    "Balance for <white>" + targetName + "</white><gray> is " +
                    "<green>$" + String.format("%,.2f", dollars) +
                    "</green><gray> and <yellow>" + String.format("%,.2f", votePoints) + "VPs</yellow><gray>."
            ));
            return true;

            // If multiple arguments, just show usage
        } else {
            sender.sendMessage(mini("<gray>Usage: /balance [playerName]"));
            return true;
        }
    }


    // -------------------------------------------------------------------------
// /baltop [currency] (now shows player names instead of UUIDs)
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

        String column = currency.equals(BMSEconomyProvider.CURRENCY_VP) ? "votepoints" : "dollars";
        List<Map.Entry<String, Double>> topEntries = db.getTopBalances(column, 10);

        // Header with dynamic currency color
        String currencyColor = currency.equals(BMSEconomyProvider.CURRENCY_VP) ? "yellow" : "green";
        String header = String.format("<bold><color:#ffd52b>Top Balances <dark_gray>(<%s>%s<dark_gray>)", currencyColor, currency);
        String centeredHeader = TextCenter.center(header, 58, "dark_gray");
        sender.sendMessage(mini(centeredHeader));

        if (topEntries.isEmpty()) {
            sender.sendMessage(mini("<gray>No data available!"));
        } else {
            int rank = 1;
            for (Map.Entry<String, Double> entry : topEntries) {
                String uuid = entry.getKey();
                double balance = entry.getValue();

                OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
                String name = (player.getName() != null) ? player.getName() : "Unknown (" + uuid.substring(0, 8) + ")";

                String formattedBalance = economy.format(plugin.getName(), BigDecimal.valueOf(balance), currency);
                String balanceColor = currency.equals(BMSEconomyProvider.CURRENCY_VP) ? "yellow" : "green";

                // Format each entry line
                String line = String.format(
                        "<white>%d. <color:#33d3d3>%s <gray>| <%s>%s</%s>",
                        rank, name, balanceColor, formattedBalance, balanceColor
                );
                sender.sendMessage(mini(line));
                rank++;
            }
        }

        // Footer line
        String footer = "<dark_gray><st>                                                             </st>";
        sender.sendMessage(mini(footer));
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
        // Check OP
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

            // /bal OR /money
            case "bal":
            case "balance":
            case "money":
                // If args.length == 1, suggest player names
                return getOnlinePlayerNames(args[0]);


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
        // First, check if the player is currently online
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }

        // Next, attempt to grab a cached offline player. If it's not cached,
        // we assume the player has never joined this server and return null
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
        if (cached != null) {
            return cached;
        }

        // Avoid calling Bukkit#getOfflinePlayer(String) as it attempts a Mojang
        // lookup which can throw a MinecraftClientHttpException for completely
        // unknown names. By returning null here, callers can gracefully handle
        // "player not found" scenarios without an uncaught exception bubbling up.
        return null;
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