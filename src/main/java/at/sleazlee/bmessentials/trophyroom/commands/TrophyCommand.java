package at.sleazlee.bmessentials.trophyroom.commands;

import at.sleazlee.bmessentials.trophyroom.data.Data;
import at.sleazlee.bmessentials.trophyroom.data.MessageProvider;
import at.sleazlee.bmessentials.trophyroom.data.Trophy;
import at.sleazlee.bmessentials.trophyroom.db.Database;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.portlek.bukkititembuilder.ItemStackBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TrophyCommand implements CommandExecutor, TabExecutor {
    public TrophyCommand() {
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        Data data = Data.getData();
        if (args.length == 0) {
            if (sender instanceof Player) {
                data.getMenu().open((Player)sender, ((Player)sender).getUniqueId().toString().replaceAll("-", ""));
            } else {
                this.printHelp(sender);
            }

            return true;
        } else {
            String id;
            switch (args[0].toLowerCase()) {
                case "create":
                    if (!sender.hasPermission("trophyroom.create")) {
                        sender.sendMessage(((MessageProvider)Objects.requireNonNull(MessageProvider.build("NO_PERMISSIONS"))).getMessage());
                        return true;
                    }

                    if (args.length != 2) {
                        sender.sendMessage("§Usage: §l/tropy create <name>");
                        return true;
                    }

                    id = args[1];
                    if (data.getTrophy(id) != null) {
                        sender.sendMessage(((MessageProvider)Objects.requireNonNull(MessageProvider.build("TROPHY_ALREADY_EXISTS"))).replace("{trophy}", id).getMessage());
                        return true;
                    }

                    ArrayList<String> lore = new ArrayList<String>() {
                        {
                            this.add("§8§n----------------------");
                            this.add("   §cfancy description");
                            this.add("§8§n----------------------");
                            this.add("       §7April 2021");
                            this.add("§8§n----------------------");
                        }
                    };
                    ItemStackBuilder builder = (ItemStackBuilder)((ItemStackBuilder)((ItemStackBuilder)ItemStackBuilder.from(Material.NETHER_STAR).setName("&7new trophy", true)).setLore(lore)).addGlowEffect(Enchantment.LUCK_OF_THE_SEA);

                    try {
                        data.addTrophy(id, new Trophy(id, builder.getItemStack()));
                        sender.sendMessage("§7Successfully created a new trophy §a" + id + "§7, edit yourself inside trophies table in the database.");
                    } catch (JsonProcessingException | SQLException var14) {
                        sender.sendMessage("§cSomething went wrong with the database!");
                        var14.printStackTrace();
                    }
                    break;
                case "give":
                    if (!sender.hasPermission("trophyroom.give")) {
                        sender.sendMessage(((MessageProvider)Objects.requireNonNull(MessageProvider.build("NO_PERMISSIONS"))).getMessage());
                        return true;
                    }

                    if (args.length < 3) {
                        sender.sendMessage("§cUsage: §l/trophy give <id> <player>");
                        return true;
                    }

                    id = args[1];
                    Trophy trophy = data.getTrophy(id);
                    if (trophy == null) {
                        sender.sendMessage(((MessageProvider)Objects.requireNonNull(MessageProvider.build("TROPHY_NOT_VALID"))).replace("{trophy}", id).getMessage());
                        return true;
                    }

                    Player receiver = Bukkit.getPlayer(args[2]);
                    if (receiver == null) {
                        sender.sendMessage(((MessageProvider)Objects.requireNonNull(MessageProvider.build("PLAYER_NOT_ONLINE"))).replace("{player}", args[2]).getMessage());
                        return true;
                    }

                    if (receiver.getInventory().firstEmpty() == -1) {
                        sender.sendMessage(((MessageProvider)Objects.requireNonNull(MessageProvider.build("NO_INVENTORY_ROOM"))).getMessage());
                        return true;
                    }

                    receiver.getInventory().addItem(new ItemStack[]{trophy.getItem()});
                    sender.sendMessage(((MessageProvider)Objects.requireNonNull(MessageProvider.build("PLAYER_RECEIVED_TROPHY"))).replace("{player}", receiver.getName()).replace("{trophy}", id).getMessage());
                    receiver.sendMessage(((MessageProvider)Objects.requireNonNull(MessageProvider.build("YOU_RECEIVED_TROPHY"))).replace("{trophy}", id).getMessage());
                    break;
                case "help":
                    this.printHelp(sender);
                    break;
                default:
                    if (!(sender instanceof Player)) {
                        this.printHelp(sender);
                        return true;
                    }

                    if (!sender.hasPermission("trophyroom.showother")) {
                        sender.sendMessage(((MessageProvider)Objects.requireNonNull(MessageProvider.build("NO_PERMISSIONS"))).getMessage());
                        return true;
                    }

                    Player opener = (Player)sender;
                    String name = args[0];
                    Database db = Database.getDatabase();

                    try {
                        String uuid;
                        if ((uuid = db.getUUID(name)) != null) {
                            data.getMenu().open(opener, uuid);
                        } else {
                            sender.sendMessage(((MessageProvider)Objects.requireNonNull(MessageProvider.build("PLAYER_NOT_ONLINE"))).replace("{player}", name).getMessage());
                        }
                    } catch (SQLException var13) {
                        var13.printStackTrace();
                    }
            }

            return true;
        }
    }

    private void printHelp(CommandSender sender) {
        sender.sendMessage("Trophyroom - Available commands");
        sender.sendMessage("/trophy help - Shows this");
        if (sender.hasPermission("trophyroom.showother")) {
            sender.sendMessage("/trophy <player> - Show another players trophyroom");
        }

        if (sender.hasPermission("trophyroom.create")) {
            sender.sendMessage("/trophy create <id> - Creates a new entry in the database");
        }

        if (sender.hasPermission("trophyroom.give")) {
            sender.sendMessage("/trophy give <id> <player> - Gives a physical trophy to the player");
        }

    }

    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player) {
            if (args.length == 1) {
                List<String> possibleResults = new ArrayList();
                if (sender.hasPermission("trophyroom.create")) {
                    possibleResults.add("create");
                }

                if (sender.hasPermission("trophyroom.give")) {
                    possibleResults.add("give");
                }

                if (sender.hasPermission("trophyroom.showother")) {
                    try {
                        possibleResults.addAll(this.getMatchingStrings(args[0], Database.getDatabase().getNames()));
                    } catch (SQLException var7) {
                        var7.printStackTrace();
                    }
                }

                return this.getMatchingStrings(args[0], possibleResults);
            }

            if (args.length == 2) {
                if (!args[0].equalsIgnoreCase("give")) {
                    return null;
                }

                try {
                    return this.getMatchingStrings(args[1], Database.getDatabase().getTrophyIds());
                } catch (SQLException var8) {
                    var8.printStackTrace();
                }
            } else if (args.length == 3) {
                return this.getMatchingStrings(args[2], (List)Bukkit.getOnlinePlayers().stream().map(OfflinePlayer::getName).collect(Collectors.toList()));
            }
        }

        return null;
    }

    private List<String> getMatchingStrings(String input, List<String> strings) {
        return (List)strings.stream().filter((s) -> {
            return s.toLowerCase().startsWith(input.toLowerCase());
        }).sorted().collect(Collectors.toList());
    }
}
