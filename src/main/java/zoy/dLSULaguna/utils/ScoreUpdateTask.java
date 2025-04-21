package zoy.dLSULaguna.utils;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import zoy.dLSULaguna.DLSULaguna;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Scheduled task for updating both Discord and in-game leaderboards.
 */
public class ScoreUpdateTask implements Runnable {

    private final DLSULaguna plugin;
    private final String discordChannelId;
    private final File leaderboardMessageFile;
    private long lastMessageId;

    public ScoreUpdateTask(DLSULaguna plugin, String discordChannelId) {
        this.plugin = plugin;
        this.discordChannelId = discordChannelId;
        this.leaderboardMessageFile = new File(plugin.getDataFolder(), "leaderboard_message.yml");
        if (leaderboardMessageFile.exists()) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(leaderboardMessageFile);
            lastMessageId = cfg.getLong("lastMessageId", -1L);
        }
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        plugin.getLogger().info("[ScoreUpdateTask] Starting...");
        try {
            // Recalculate and recache stats
            SectionStatsFileUtil.recalculateAggregateStats();
            PointsCalculatorUtil.calculateAllPlayerPoints();
            PointsCalculatorUtil.calculateAllSectionPoints();

            // Update Discord
            updateDiscordLeaderboard();

            // Update in-game leaderboard on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                String title = ChatColor.YELLOW + "" + ChatColor.BOLD + "Points";
                ScoreboardUtil.displayOnce("Points", title);
            });

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[ScoreUpdateTask] Unexpected error:", e);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            plugin.getLogger().info("[ScoreUpdateTask] Finished in " + elapsed + "ms");
        }
    }

    /**
     * Builds and sends or edits the Discord leaderboard message.
     */
    private void updateDiscordLeaderboard() {
        // Load section stats file once
        FileConfiguration secCfg = YamlConfiguration.loadConfiguration(plugin.getSectionStatsFile());
        Set<String> sections = secCfg.getKeys(false);
        if (sections.isEmpty()) {
            plugin.getLogger().info("[ScoreUpdateTask] No sections to post.");
            return;
        }

        // Gather all sections with > 0 points, then sort descending
        List<Map.Entry<String, Integer>> sorted = sections.stream()
                .map(key -> Map.entry(key.toUpperCase(), secCfg.getInt(key + ".Points", 0)))
                .filter(e -> e.getValue() > 0)               // only those that currently have points
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());

        if (sorted.isEmpty()) {
            plugin.getLogger().info("[ScoreUpdateTask] Sections have zero points.");
            return;
        }

        // Build message with a line per section
        StringBuilder sb = new StringBuilder("📊 **Current Section Leaderboard:**\n");
        int rank = 1;
        for (var e : sorted) {
            sb.append("`#")
                    .append(rank++)
                    .append("` **")
                    .append(e.getKey())
                    .append("** – ")
                    .append(e.getValue())
                    .append(" pts\n");
        }
        String content = sb.toString();

        // Send or edit the Discord message
        if (lastMessageId > 0) {
            DiscordUtil.editMessage(discordChannelId, lastMessageId, content);
            plugin.getLogger().info("[ScoreUpdateTask] Edited Discord leaderboard.");
        } else {
            CompletableFuture<Message> future = DiscordUtil.sendMessage(discordChannelId, content);
            future.thenAccept(msg -> {
                if (msg != null) {
                    lastMessageId = msg.getIdLong();
                    saveMessageId();
                    plugin.getLogger().info("[ScoreUpdateTask] Sent new Discord leaderboard (ID " + lastMessageId + ").");
                } else {
                    plugin.getLogger().warning("[ScoreUpdateTask] Discord send returned null.");
                }
            });
        }
    }


    /**
     * Persists the last Discord message ID to file.
     */
    private void saveMessageId() {
        try {
            FileConfiguration cfg = new YamlConfiguration();
            cfg.set("lastMessageId", lastMessageId);
            cfg.save(leaderboardMessageFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[ScoreUpdateTask] Failed to save lastMessageId", e);
        }
    }
}
