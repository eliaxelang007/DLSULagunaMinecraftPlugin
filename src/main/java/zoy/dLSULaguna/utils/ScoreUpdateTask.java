package zoy.dLSULaguna.utils;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;
import zoy.dLSULaguna.DLSULaguna;

/**
 * Scheduled task for updating Discord and in-game leaderboards.
 * Runs calculations for online players only to minimize lag.
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
            // 1) Recalculate cached stats (async)
            SectionStatsFileUtil.recalculateAggregateStats();

            // 2) Recalculate points only for online players (async)
            PointsCalculatorUtil.calculateOnlinePlayerPoints();

            // 3) Recalculate section totals (async)
            PointsCalculatorUtil.calculateAllSectionPoints();

            // 4) Update Discord message (async)
            updateDiscordLeaderboard();

            // 5) Update in-game scoreboard (sync)
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

    private void updateDiscordLeaderboard() {
        FileConfiguration secCfg = YamlConfiguration.loadConfiguration(plugin.getSectionStatsFile());
        Set<String> sections = secCfg.getKeys(false);

        StringBuilder sb = new StringBuilder("📊 **Current Section Leaderboard:**\n");
        if (!sections.isEmpty()) {
            List<Map.Entry<String, Integer>> sorted = sections.stream()
                    .map(key -> Map.entry(key.toUpperCase(), secCfg.getInt(key + ".Points", 0)))
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .collect(Collectors.toList());

            int rank = 1;
            for (var entry : sorted) {
                sb.append("`#")
                        .append(rank++)
                        .append("` ")
                        .append("**")
                        .append(entry.getKey())
                        .append("**")
                        .append(" – ")
                        .append(entry.getValue())
                        .append(" pts\n");
            }
        } else {
            sb.append("_No section points yet. Stay tuned!_\n");
        }

        String content = sb.toString();
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
