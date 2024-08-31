package at.sleazlee.bmessentials.wild;

import at.sleazlee.bmessentials.huskhomes.HuskHomesAPIHook;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Random;

import static java.lang.Math.abs;

public class BMWildCommand implements CommandExecutor {


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("wild") || label.equalsIgnoreCase("rtp") || label.equalsIgnoreCase("randomtp") || label.equalsIgnoreCase("randomteleport")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;

                if (args.length == 0) {
                    // No additional arguments, perform default function
                    randomLocation(player, "all");
                } else if (args.length == 1) {
                    String version = args[0];
                    if (version.equals("1.19")) {
                        randomLocation(player, "1.19");
                    } else if (version.equals("1.21")) {
                        randomLocation(player, "1.21");
                    } else {
                        // Invalid version argument
                        player.sendMessage("§c§lWild §cInvalid version argument. §fTry /wild [version]");
                    }
                } else {
                    // Too many arguments
                    player.sendMessage("§c§lWild §cToo many arguments. §fTry /wild [version]");

                }
            } else {
                sender.sendMessage("§c§lWild §cThis command can only be used by a player.");
            }
            return true;
        }
        return false;
    }

    public void randomLocation(Player player, String version) {
        double upper = 750, lower = 750;
        Random random = new Random();

        if (!version.equals("all")) {
            switch (version) {
                // 1.19
                case "1.19":
                    lower = 612;
                    upper = 19867;
                    break;
                // 1.21
                case "1.21":
                    lower = 20580;
                    upper = 29595;
                    break;
                // newest version
                default:
                    System.out.println("§4§lBMWild §cError 1!"); // Debugging statement
                    break;
            }
        } else {
            int upperbound = 2;
            int randomVersion = random.nextInt(upperbound) + 1;
            switch (randomVersion) {
                // 1.19
                case 1:
                    lower = 612;
                    upper = 19867;
                    break;
                // 1.21
                case 2:
                    lower = 20580;
                    upper = 29595;
                    break;
                // newest version
                default:
                    System.out.println("§4§lBMWild §cError 2!"); // Debugging statement
                    break;
            }
        }

        double x = abs(lower + (random.nextGaussian() * (upper - lower)));
        double y = 345; // Y position
        double z = abs(lower + (random.nextGaussian() * (upper - lower)));
        float yaw = 0;
        float pitch = 90;

        // If the value is outside the range, generate a new value
        while (x > upper || x < lower) {
            x = lower + (random.nextGaussian() * (upper - lower));
        }
        while (z > upper || z < lower) {
            z = lower + (random.nextGaussian() * (upper - lower));
        }

        String serverName = "blockminer";
        String worldName = "world";

        double newx, newz;

        int i = random.nextInt(0,2);





        if (i > 60) {
            newx = x * 1;
            newz = z * -1;
        } else if (i >= 39) {
            newx = x * 1;
            newz = z * 1;
        } else if (i > 20) {
            newx = x * -1;
            newz = z * 1;
        }
        else {
            newx = x * -1;
            newz = z * -1;
        }

        HuskHomesAPIHook.teleportPlayer(player, newx, y, newz, yaw, pitch, worldName, serverName);
    }


}
