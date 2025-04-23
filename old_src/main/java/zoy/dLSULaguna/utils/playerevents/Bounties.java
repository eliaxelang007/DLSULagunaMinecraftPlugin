package zoy.dLSULaguna.utils.playerevents;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import zoy.dLSULaguna.DLSULaguna;
import zoy.dLSULaguna.commands.BountyListCommand;
import zoy.dLSULaguna.utils.PlayerStatsFileUtil;
import zoy.dLSULaguna.utils.ScoreboardUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;


/**
 * Manages periodic bounty assignments and handles bounty claims on death.
 */
public class Bounties implements Listener, CommandExecutor {
    private final DLSULaguna plugin;
    private final NamespacedKey sectionKey;
    private final File bountyFile;
    private final long claimCooldown = 24 * 60 * 60 * 1000L;
    private Map<UUID, Bounty> currentBounties = new HashMap<>();

    public Bounties(DLSULaguna plugin) {
        this.plugin = plugin;
        this.sectionKey = new NamespacedKey(plugin, "section_name");
        this.bountyFile = new File(plugin.getDataFolder(), "bounty_stats.yml");
        loadBounties();
        startBountyScheduler();
    }

    private static class Bounty {
        public final UUID target;
        public final String username;
        public final int reward;
        public final long lastClaim;
        public Bounty(UUID target, String username, int reward, long lastClaim) {
            this.target = target;
            this.username = username;
            this.reward = reward;
            this.lastClaim = lastClaim;
        }
    }

    /**
     * Load existing bounties from disk.
     */
    private void loadBounties() {
        if (!bountyFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(bountyFile);
        for (String key : cfg.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String user = cfg.getString(key + ".Username", "");
                int reward = cfg.getInt(key + ".Reward", 0);
                long last = cfg.getLong(key + ".LastClaimTime", 0L);
                currentBounties.put(uuid, new Bounty(uuid, user, reward, last));
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid bounty entry: " + key);
            }
        }
    }

    /**
     * Persist current bounties to disk.
     */
    private void saveBountiesAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            FileConfiguration cfg = new YamlConfiguration();
            currentBounties.forEach((uuid, b) -> {
                String k = uuid.toString();
                cfg.set(k + ".Username", b.username);
                cfg.set(k + ".Reward", b.reward);
                cfg.set(k + ".LastClaimTime", b.lastClaim);
            });
            try { cfg.save(bountyFile); }
            catch (IOException e) { plugin.getLogger().log(Level.SEVERE, "Failed saving bounties", e); }
        });
    }

    /**
     * Refresh top-6 bounties based on player point standings.
     */
    public void refreshBounties() {
        // Load stats file
        FileConfiguration stats = YamlConfiguration.loadConfiguration(plugin.getPlayersStatsFile());
        Map<UUID, Integer> points = new HashMap<>();
        for (String section : stats.getKeys(false)) {
            ConfigurationSection sec = stats.getConfigurationSection(section);
            if (sec==null) continue;
            for (String uid : sec.getKeys(false)) {
                int pts = sec.getInt(uid + ".Points", 0);
                try { points.put(UUID.fromString(uid), pts);} catch(Exception ignored){}
            }
        }
        // Sort and pick top 6
        List<Map.Entry<UUID, Integer>> top = points.entrySet().stream()
                .sorted(Map.Entry.<UUID,Integer>comparingByValue().reversed())
                .limit(6)
                .collect(Collectors.toList());
        // Build new bounty map
        Map<UUID,Bounty> updated = new HashMap<>();
        for (var e : top) {
            UUID uuid = e.getKey();
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            int reward = (int)(e.getValue() * 0.3);
            updated.put(uuid, new Bounty(uuid, op.getName(), reward, 0L));
        }
        currentBounties = updated;
        saveBountiesAsync();
        plugin.getLogger().info("[Bounties] Refreshed top bounties.");
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent ev) {
        Player victim = ev.getEntity();
        Player killer = victim.getKiller();
        if (killer==null) return;
        Bounty b = currentBounties.get(victim.getUniqueId());
        if (b==null) return;
        long now=System.currentTimeMillis();
        if (now - b.lastClaim < claimCooldown) return;
        // Cannot claim own section
        String vs = victim.getPersistentDataContainer().get(sectionKey, PersistentDataType.STRING);
        String ks = killer.getPersistentDataContainer().get(sectionKey, PersistentDataType.STRING);
        if (Objects.equals(vs,ks)) return;
        // Apply points
        PlayerStatsFileUtil.increaseStat(victim, "Bounty-points", -b.reward);
        PlayerStatsFileUtil.increaseStat(killer, "Bounty-points", b.reward);
        // Broadcast
        Bukkit.broadcastMessage(ChatColor.RED + killer.getName() + " claimed bounty on " + victim.getName() + " for " + b.reward + " pts!");
        // Update record
        currentBounties.put(victim.getUniqueId(), new Bounty(b.target, b.username, b.reward, now));
        saveBountiesAsync();
        // Refresh in-game leaderboard
        ScoreboardUtil.displayOnce("Points", ChatColor.YELLOW+"Section Points");
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if ("bountylist".equalsIgnoreCase(c.getName())) {
            BountyListCommand cmd = (BountyListCommand)plugin.getCommand("bountylist").getExecutor();
            if (cmd!=null) cmd.sendBountyList(s);
            return true;
        }
        return false;
    }

    /**
     * Starts automatic bounty refresh every 20 minutes.
     */
    public void startBountyScheduler() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> refreshBounties(), 0L, 20L*60*20);
    }
}
