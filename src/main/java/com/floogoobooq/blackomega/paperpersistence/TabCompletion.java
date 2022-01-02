package com.floogoobooq.blackomega.paperpersistence;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class TabCompletion implements TabCompleter {

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

        List<String> tabCompleteValues = new ArrayList<>();
        if (args.length == 1) {
            if (args[0].isEmpty()) {
                for (Player player : commandSender.getServer().getOnlinePlayers()) {
                    tabCompleteValues.add(player.getName());
                }
            } else {
                for (Player player : commandSender.getServer().matchPlayer(args[0])) {
                    tabCompleteValues.add(player.getName());
                }
            }

        } else if (args.length == 2) {
            if (args[1].isEmpty()) {
                tabCompleteValues.add("main");
                tabCompleteValues.add("offhand");
            } else {
                Pattern pattern = Pattern.compile(args[1].toLowerCase());
                if (pattern.matcher("main").lookingAt()) {
                    tabCompleteValues.add("main");
                }
                if (pattern.matcher("offhand").lookingAt()) {
                    tabCompleteValues.add("offhand");
                }
            }

        } else if (args.length > 2) {
            tabCompleteValues.add("Too many arguments!");
        }

        return tabCompleteValues;
    }
}
