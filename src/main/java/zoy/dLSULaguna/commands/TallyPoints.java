package zoy.dLSULaguna.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import zoy.dLSULaguna.DLSULaguna;
import zoy.dLSULaguna.utils.PointsCalculatorUtil;

import java.util.logging.Level;

/**
 * Command to force recalculation of all points and refresh the in-game leaderboard.
 */
public class TallyPoints implements CommandExecutor {
    private final DLSULaguna plugin;

    public TallyPoints(DLSULaguna plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String msg = "Recalculating all section scores...";
        if (sender instanceof Player) {
            ((Player) sender).sendMessage(ChatColor.GREEN + msg);
        } else {
            sender.sendMessage(msg);
        }

        // Perform recalculation asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PointsCalculatorUtil.calculateAllPlayerPoints();
                PointsCalculatorUtil.calculateAllSectionPoints();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error during point recalculation", e);
            }
            // Update the in-game scoreboard on the main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                String title = ChatColor.YELLOW + "" + ChatColor.BOLD + "Section Points";
            });
        });

        return true;
    }
}
