package zoy.dLSULaguna.utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import zoy.dLSULaguna.DLSULaguna;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PointsCalculatorUtil {
    private static DLSULaguna plugin;

    /**
     * Initialize with plugin instance
     */
    public static void initialize(DLSULaguna pluginInstance) {
        plugin = pluginInstance;
    }

    /**
     * Safely accumulate session distance into YAML-backed total
     */
    /**
     * Calculate and save points for all online players in one batch
     */
    public static void calculateOnlinePlayerPoints() {
        if (plugin == null) throw new IllegalStateException("PointsCalculatorUtil not initialized");

        File playersFile = plugin.getPlayersStatsFile();
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(playersFile);
        plugin.getLogger().info("Recalculating points for online players...");

        boolean changed = false;
        for (Player player : Bukkit.getOnlinePlayers()) {
            String section = PlayerStatsFileUtil.findSectionByUUID(player.getUniqueId().toString());
            if (section == null) continue;

            ConfigurationSection playerSec = cfg.getConfigurationSection(section + "." + player.getUniqueId());
            if (playerSec == null) continue;

            int total = 0;
            for (String stat : playerSec.getKeys(false)) {
                if ("Points".equals(stat) || playerSec.isConfigurationSection(stat)) continue;
                total += computePointsFromStat(stat, playerSec, player);
            }

            playerSec.set("Points", total);
            changed = true;
        }

        if (changed) {
            try {
                cfg.save(playersFile);
                plugin.getLogger().info("Saved updated points for online players.");
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save players_stats.yml: " + e.getMessage());
            }
        }
    }

    /**
     * Compute points from a stat key
     */
    private static int computePointsFromStat(String statKey,
                                             ConfigurationSection playerSec,
                                             Player player) {
        switch (statKey) {
            case "Duel-points":
            case "Bounty-points":
                return playerSec.getInt(statKey);
            case "Blocks broken":
                return playerSec.getInt(statKey) / 500;
            case "Blocks placed":
                return playerSec.getInt(statKey) / 300;
            case "Kills":
            case "Mobs Killed":
                return playerSec.getInt(statKey) / 100;
            case "Deaths":
                return -(playerSec.getInt(statKey) / 3);
            case "Distance": {
                double raw = playerSec.getDouble(statKey, 0.0);
                return (int) Math.floor(raw / 1000.0);
            }
            case "Time Logged In":
                return (int) (playerSec.getLong(statKey, 0L) / 60L);
            case "Fish Caught":
                return playerSec.getInt(statKey) / 10;
            case "Items Crafted":
                return playerSec.getInt(statKey) / 100;
            case "Trades Made":
                return playerSec.getInt(statKey) / 10;
            default:
                return 0;
        }
    }

    /**
     * Calculate and save points for all players (batch mode)
     */
    public static void calculateAllPlayerPoints() {
        if (plugin == null) throw new IllegalStateException("Not initialized");

        File playersFile = plugin.getPlayersStatsFile();
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(playersFile);
        plugin.getLogger().info("Recalculating points for all players...");

        for (String sectionKey : cfg.getKeys(false)) {
            ConfigurationSection sec = cfg.getConfigurationSection(sectionKey);
            if (sec == null) continue;

            for (String uuidStr : sec.getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection playerSec = sec.getConfigurationSection(uuidStr);
                if (playerSec == null) continue;

                int total = 0;
                Player p = Bukkit.getPlayer(uuid);
                for (String stat : playerSec.getKeys(false)) {
                    if ("Points".equals(stat) || playerSec.isConfigurationSection(stat)) continue;
                    total += computePointsFromStat(stat, playerSec, p != null ? p : null);
                }

                playerSec.set("Points", total);
            }
        }

        try {
            cfg.save(playersFile);
            plugin.getLogger().info("Saved updated points for all players.");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save players_stats.yml: " + e.getMessage());
        }
    }

    /**
     * Calculate and save total points for each section
     */
    public static void calculateAllSectionPoints() {
        if (plugin == null) throw new IllegalStateException("Not initialized");

        File playersFile = plugin.getPlayersStatsFile();
        FileConfiguration playersCfg = YamlConfiguration.loadConfiguration(playersFile);
        Map<String, Integer> totals = new HashMap<>();

        for (String sectionKey : playersCfg.getKeys(false)) {
            ConfigurationSection sec = playersCfg.getConfigurationSection(sectionKey);
            if (sec == null) continue;

            int sum = 0;
            for (String uuid : sec.getKeys(false)) {
                sum += playersCfg.getInt(sectionKey + "." + uuid + ".Points", 0);
            }
            totals.put(sectionKey, sum);
        }

        File statsFile = plugin.getSectionStatsFile();
        FileConfiguration statsCfg = YamlConfiguration.loadConfiguration(statsFile);
        for (Map.Entry<String, Integer> e : totals.entrySet()) {
            statsCfg.set(e.getKey() + ".Points", e.getValue());
        }

        try {
            statsCfg.save(statsFile);
            plugin.getLogger().info("Saved updated section_stats.yml");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save section_stats.yml: " + e.getMessage());
        }
    }
}
