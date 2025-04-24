package zoy.dLSULaguna.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.*;

import java.util.Map;

public class ScoreboardUtil {
    private static Plugin plugin;

    public static void initialize(Plugin pluginInstance) {
        plugin = pluginInstance;
    }

    /**
     * Updates one persistent scoreboard object every intervalTicks,
     * reading scores from a pre‑filled cache.
     */
    public static void startAutoDisplayFromCache(
            Plugin plugin,
            String unusedTrackedStat,
            String title,
            long intervalTicks,
            Map<String,Integer> cache
    ) {
        ScoreboardManager mgr = Bukkit.getScoreboardManager();
        if (mgr == null) return;

        // create once and reuse
        Scoreboard board = mgr.getNewScoreboard();
        Objective obj   = board.registerNewObjective("sectionStats", Criteria.DUMMY, title);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        // schedule on main thread only the scoreboard updates
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // clear old scores
            for (String entry : board.getEntries()) {
                board.resetScores(entry);
            }

            if (cache.isEmpty()) {
                obj.getScore(ChatColor.GRAY + "No data").setScore(0);
            } else {
                cache.forEach((sect, pts) ->
                        obj.getScore(formatName(sect)).setScore(pts)
                );
            }

            // push to all players
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.setScoreboard(board);
            }
        }, 0L, intervalTicks);
    }

    private static String formatName(String key) {
        return key.length() > 40 ? key.substring(0, 40) : key;
    }
}
