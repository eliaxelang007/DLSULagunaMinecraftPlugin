package zoy.dLSULaguna.utils;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import zoy.dLSULaguna.DLSULaguna;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlayerStatsFileUtil {

    private static DLSULaguna plugin;
    private static File statsFile;
    private static FileConfiguration config;
    private static final Set<UUID> pendingSave = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Map<String, Object>> statCache = new ConcurrentHashMap<>();

    /**
     * Initialize stats file and config.
     */
    public static void initialize(DLSULaguna pluginInstance) {
        plugin = pluginInstance;
        statsFile = plugin.getPlayersStatsFile();

        if (!statsFile.exists()) {
            try {
                if (statsFile.getParentFile() != null) statsFile.getParentFile().mkdirs();
                statsFile.createNewFile();
                plugin.getLogger().info("Created players_stats.yml");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create players_stats.yml on initialize!", e);
            }
        }
        config = YamlConfiguration.loadConfiguration(statsFile);
        // Schedule periodic flush of pending stat updates every 5 seconds
        Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin,
                PlayerStatsFileUtil::flushPending,
                100L,
                100L
        );
    }

    /**
     * Batch-save cached stats for offline players asynchronously.
     */
    public static void batchSave(Set<UUID> playersToSave) {
        for (UUID uuid : playersToSave) {
            Map<String, Object> stats = statCache.remove(uuid);
            if (stats == null) continue;

            String section = PlayerDataUtil.getPlayerSection(uuid);
            if (section == null) continue;

            String base = section + "." + uuid;
            for (Map.Entry<String, Object> entry : stats.entrySet()) {
                config.set(base + "." + entry.getKey(), entry.getValue());
            }
        }
        try {
            config.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not batch save players_stats.yml!", e);
        }
    }

    /**
     * Queue a stat update for an online player.
     */
    public static void setStat(Player player, String statKey, Object value) {
        queueStat(player.getUniqueId(), statKey, value);
    }

    /**
     * Queue a stat update when you have UUID and section.
     */
    public static void setStat(UUID uuid, String section, String statKey, Object value) {
        if (section == null) return;
        statCache.putIfAbsent(uuid, new ConcurrentHashMap<>());
        statCache.get(uuid).put(statKey, value);
        pendingSave.add(uuid);
    }

    /**
     * Immediately sets a stat for offline player, saving directly to file.
     */
    public static void setStatRaw(UUID uuid, String statKey, Object value) {
        String section = PlayerDataUtil.getPlayerSection(uuid);
        if (section == null) return;
        String path = section + "." + uuid + "." + statKey;
        config.set(path, value);
        try {
            config.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save players_stats.yml during raw set!", e);
        }
    }

    /**
     * Increase a numeric stat for an online player.
     */
    public static void increaseStat(Player player, String statKey, Object change) {
        increaseStat(player.getUniqueId(), PlayerDataUtil.getPlayerSection(player), statKey, change);
    }

    /**
     * Increase a stat by delta when you have uuid and section.
     */
    public static void increaseStat(UUID uuid, String section, String statKey, Object change) {
        if (section == null) return;
        String path = section + "." + uuid + "." + statKey;
        Object currentVal = config.get(path);
        Object result;
        if (change instanceof Integer) {
            int curr = currentVal instanceof Number ? ((Number) currentVal).intValue() : 0;
            result = curr + (Integer) change;
        } else if (change instanceof Double) {
            double curr = currentVal instanceof Number ? ((Number) currentVal).doubleValue() : 0.0;
            result = curr + (Double) change;
        } else {
            plugin.getLogger().warning("Invalid stat change type for key: " + statKey);
            return;
        }
        queueStat(uuid, statKey, result);
    }

    /**
     * Helper to queue stat updates in cache.
     */
    private static void queueStat(UUID uuid, String statKey, Object value) {
        statCache.putIfAbsent(uuid, new ConcurrentHashMap<>());
        statCache.get(uuid).put(statKey, value);
        pendingSave.add(uuid);
    }

    /**
     * Get a stat for online player.
     */
    public static Object getStat(Player player, String statKey) {
        return getStat(player.getUniqueId(), PlayerDataUtil.getPlayerSection(player), statKey);
    }

    /**
     * Get a stat when you have uuid and section.
     */
    public static Object getStat(UUID uuid, String section, String statKey) {
        if (section == null) return null;
        return config.get(section + "." + uuid + "." + statKey);
    }

    public static int getStatInt(Player player, String statKey, int defaultValue) {
        return getStatInt(player.getUniqueId(), PlayerDataUtil.getPlayerSection(player), statKey, defaultValue);
    }

    public static int getStatInt(UUID uuid, String section, String statKey, int defaultValue) {
        Object val = getStat(uuid, section, statKey);
        return val instanceof Number ? ((Number) val).intValue() : defaultValue;
    }

    public static double getStatDouble(Player player, String statKey, double defaultValue) {
        return getStatDouble(player.getUniqueId(), PlayerDataUtil.getPlayerSection(player), statKey, defaultValue);
    }

    public static double getStatDouble(UUID uuid, String section, String statKey, double defaultValue) {
        Object val = getStat(uuid, section, statKey);
        return val instanceof Number ? ((Number) val).doubleValue() : defaultValue;
    }

    /**
     * Remove a player's entire entry.
     */
    public static boolean removePlayerEntry(String section, String uuid) {
        if (section == null || uuid == null) return false;
        String path = section + "." + uuid;
        if (config.contains(path)) {
            config.set(path, null);
            try {
                config.save(statsFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save players_stats.yml during removal!", e);
            }
            return true;
        }
        return false;
    }

    public static String findSectionByUUID(String uuid) {
        for (String section : config.getKeys(false)) {
            ConfigurationSection sec = config.getConfigurationSection(section);
            if (sec != null && sec.isConfigurationSection(uuid)) {
                return section;
            }
        }
        return null;
    }

    public static String findUUIDByUsername(String username) {
        for (String section : config.getKeys(false)) {
            ConfigurationSection sec = config.getConfigurationSection(section);
            if (sec != null) {
                for (String uuid : sec.getKeys(false)) {
                    if (sec.isString(uuid + ".Username") && sec.getString(uuid + ".Username").equalsIgnoreCase(username)) {
                        return uuid;
                    }
                }
            }
        }
        return null;
    }

    public static void showPlayerPoints(Player player) {
        String section = PlayerDataUtil.getPlayerSection(player);
        if (section == null) {
            player.sendMessage(ChatColor.YELLOW + "You are not currently assigned to a section.");
            return;
        }
        String path = section + "." + player.getUniqueId() + ".Points";
        int points = config.getInt(path);
        player.sendMessage(ChatColor.GOLD + "★ " + ChatColor.YELLOW + "Your current contribution: " + ChatColor.AQUA + points + ChatColor.GREEN + " pts");
    }

    public static void clearPlayerStatsFully(Player player) {
        if (plugin == null) return;
        String section = PlayerDataUtil.getPlayerSection(player);
        String uuid = player.getUniqueId().toString();
        boolean entryRemoved = false;
        boolean countDecremented = false;
        if (section != null) {
            entryRemoved = removePlayerEntry(section, uuid);
            countDecremented = SectionStatsFileUtil.decrementMemberCount(section);
        }
        PlayerDataUtil.removePlayerSectionData(player);
        if (entryRemoved && countDecremented) {
            player.sendMessage(ChatColor.GREEN + "Your stats have been cleared! Please join a new section if you wish to participate again.");
            plugin.getLogger().info("Fully cleared stats for player " + player.getName() + " from section " + section);
        } else if (section != null) {
            player.sendMessage(ChatColor.RED + "An error occurred while clearing your stats. Please contact an admin.");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Your section assignment was cleared, but no corresponding stats were found.");
        }
    }
    /**
     * Flush queued stat updates to disk.
     */
    public static void flushPending() {
        Set<UUID> toSave = new HashSet<>(pendingSave);
        pendingSave.removeAll(toSave);
        if (!toSave.isEmpty()) {
            batchSave(toSave);
        }
    }
}
