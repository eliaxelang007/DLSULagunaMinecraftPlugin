package zoy.dLSULaguna.utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import zoy.dLSULaguna.DLSULaguna;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

public class PointsCalculatorUtil {

    private static DLSULaguna plugin;

    /**
     * Initialize with plugin instance
     */
    public static void initialize(DLSULaguna pluginInstance) {
        plugin = pluginInstance;
    }

    /**
     * Calculate points only for online players to minimize lag
     */
    public static void calculateOnlinePlayerPoints() {
        if (plugin == null) {
            plugin.getLogger().severe("PointsCalculatorUtil not initialized!");
            return;
        }

        File playersFile = plugin.getPlayersStatsFile();
        if (playersFile == null || !playersFile.exists()) {
            plugin.getLogger().warning("players_stats.yml missing; cannot compute player points.");
            return;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(playersFile);
        plugin.getLogger().info("Recalculating points for online players...");
        boolean changed = false;

        for (Player player : Bukkit.getOnlinePlayers()) {
            String section = PlayerStatsFileUtil.findSectionByUUID(player.getUniqueId().toString());
            if (section == null) continue;

            String path = section + "." + player.getUniqueId();
            ConfigurationSection playerSec = cfg.getConfigurationSection(path);
            if (playerSec == null) continue;

            int total = 0;
            for (String stat : playerSec.getKeys(false)) {
                if (stat.equals("Points") || playerSec.isConfigurationSection(stat)) continue;
                total += computePointsFromStat(stat, playerSec);
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
     * Calculate points for a single player UUID in given section
     */
    public static void calculatePointsForPlayer(String sectionKey, String uuid) {
        if (plugin == null || sectionKey == null || uuid == null) return;

        File playersFile = plugin.getPlayersStatsFile();
        if (playersFile == null || !playersFile.exists()) {
            plugin.getLogger().warning("players_stats.yml missing; cannot compute section totals.");
            return;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(playersFile);
        String path = sectionKey + "." + uuid;
        ConfigurationSection playerSec = cfg.getConfigurationSection(path);
        if (playerSec == null) return;

        int total = 0;
        for (String stat : playerSec.getKeys(false)) {
            if (stat.equals("Points") || playerSec.isConfigurationSection(stat)) continue;
            total += computePointsFromStat(stat, playerSec);
        }
        playerSec.set("Points", total);

        try {
            cfg.save(playersFile);
            plugin.getLogger().info("Saved updated points for player " + uuid);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save players_stats.yml for player " + uuid + ": " + e.getMessage());
        }
    }

    /**
     * Helper to map stat keys to point values
     */
    private static int computePointsFromStat(String statKey, ConfigurationSection playerSec) {
        switch (statKey) {
            case "Duel-points":
            case "Bounty-points":
                return playerSec.getInt(statKey);
            case "Blocks broken":
            case "Blocks placed":
                return playerSec.getInt(statKey) / 300;
            case "Kills":
            case "Mobs Killed":
                return playerSec.getInt(statKey) / 100;
            case "Deaths":
                return - (playerSec.getInt(statKey) / 3);
            case "Distance":
                return (int)(playerSec.getDouble(statKey) / 1000);
            case "Time Logged In":
                return (int)(playerSec.getLong(statKey) / 60);
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
     * Recalculate and save total points for each section
     */
    public static void calculateAllSectionPoints() {
        if (plugin == null) {
            plugin.getLogger().severe("PointsCalculatorUtil not initialized!");
            return;
        }

        // Sum up from players_stats
        File playersFile = plugin.getPlayersStatsFile();
        FileConfiguration playersCfg = YamlConfiguration.loadConfiguration(playersFile);
        Map<String, Integer> totals = new HashMap<>();

        for (String section : playersCfg.getKeys(false)) {
            ConfigurationSection sec = playersCfg.getConfigurationSection(section);
            if (sec == null) continue;

            int sum = 0;
            for (String uuid : sec.getKeys(false)) {
                sum += playersCfg.getInt(section + "." + uuid + ".Points", 0);
            }
            totals.put(section, sum);
        }

        // Write to section_stats
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
    public static void calculateAllPlayerPoints() {
        if (plugin == null) {
            plugin.getLogger().severe("PointsCalculatorUtil not initialized!");
            return;
        }

        File playersFile = plugin.getPlayersStatsFile();
        if (playersFile == null || !playersFile.exists()) {
            plugin.getLogger().warning("players_stats.yml missing; cannot compute player points.");
            return;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(playersFile);
        plugin.getLogger().info("Recalculating points for ALL players...");

        for (String section : cfg.getKeys(false)) {
            ConfigurationSection sec = cfg.getConfigurationSection(section);
            if (sec == null) continue;

            for (String uuidStr : sec.getKeys(false)) {
                ConfigurationSection playerSec = sec.getConfigurationSection(uuidStr);
                if (playerSec == null) continue;

                int total = 0;
                for (String stat : playerSec.getKeys(false)) {
                    if ("Points".equals(stat) || playerSec.isConfigurationSection(stat)) continue;
                    total += computePointsFromStat(stat, playerSec);
                }

                // persist immediately to players_stats.yml
                UUID uuid = UUID.fromString(uuidStr);
                PlayerStatsFileUtil.setStatRaw(uuid, "Points", total);
                plugin.getLogger().finer("Set Points=" + total + " for player " + uuidStr + " in section " + section);
            }
        }

        plugin.getLogger().info("Finished recalculating points for all players.");
    }
}
