package at.sleazlee.bmessentials.vot;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VotTabCompleter implements TabCompleter {

    private static final List<String> voteInProgressOptions = Arrays.asList(
            "Yes",
            "No"
    );
    private static final List<String> noVoteInProgressOptions = Arrays.asList(
            "Day",
            "Night",
            "Clear",
            "Rain",
            "Thunder"
    );

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {

        if (args.length == 1) {
            if (VoteManager.isVoteInProgress()) {
                return filterList(voteInProgressOptions, args[0]);
            } else {
                return filterList(noVoteInProgressOptions, args[0]);
            }
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