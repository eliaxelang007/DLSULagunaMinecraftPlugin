package zoy.dLSULaguna.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import zoy.dLSULaguna.DLSULaguna;

import java.io.File;

public class ClearPoints implements CommandExecutor {
    private final DLSULaguna plugin;

    public ClearPoints(DLSULaguna plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        // Permission check
        if (!sender.hasPermission("dlsulaguna.clearpoints")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        File sectionStatsFile = new File(plugin.getDataFolder(), "section_stats.yml");
        if (sectionStatsFile.exists()) {
            if (sectionStatsFile.delete()) {
                sender.sendMessage(ChatColor.GREEN + "✅ Section stats have been cleared.");
            } else {
                sender.sendMessage(ChatColor.RED + "⚠ Failed to delete section stats file.");
                return true;
            }
        } else {
            sender.sendMessage(ChatColor.GRAY + "No section stats file exists to clear.");
        }

        return true;
    }
}
