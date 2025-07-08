package at.sleazlee.bmessentials.Help.Abilities;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChestSortTabCompleter implements TabCompleter {

    private static final List<String> BASE_COMMANDS = Arrays.asList(
            "Toggle",
            "Hotkeys"
    );
    private static final List<String> TOGGLE_OPTIONS = Arrays.asList(
            "DoubleClick",
            "ShiftClick",
            "MiddleClick",
            "ShiftRightClick"
    );

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {

        if (args.length == 1) {
            return filterList(BASE_COMMANDS, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
            return filterList(TOGGLE_OPTIONS, args[1]);
        }

        return Collections.emptyList();
    }

    private List<String> filterList(List<String> input, String arg) {
        List<String> result = new ArrayList<>();
        for (String option : input) {
            if (option.toLowerCase().startsWith(arg.toLowerCase())) {
                result.add(option);
            }
        }
        return result;
    }
}
