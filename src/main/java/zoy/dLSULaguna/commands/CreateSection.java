package zoy.dLSULaguna.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import zoy.dLSULaguna.utils.SectionFileUtil;
import zoy.dLSULaguna.utils.SectionStatsFileUtil;

import java.io.IOException;

public class CreateSection implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /createsection <section name>");
            return true;
        }

        SectionFileUtil.createSection(args[0]);
        sender.sendMessage(ChatColor.GREEN + "Created a section " + args[0]);
        return true;
    }
}
