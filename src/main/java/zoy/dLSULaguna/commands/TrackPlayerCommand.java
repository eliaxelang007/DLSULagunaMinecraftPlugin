package zoy.dLSULaguna.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class TrackPlayerCommand implements CommandExecutor, Listener {

    private final JavaPlugin plugin;
    private final Map<String, UUID> sectionTrackers = new HashMap<>(); // Section -> Tracker
    private final Map<UUID, UUID> trackerToTarget = new HashMap<>();   // Tracker -> Target
    private final Map<String, BukkitRunnable> activeTrackers = new HashMap<>();

    public TrackPlayerCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin); // Register the listener
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /trackplayer <target_player_name>");
            return true;
        }

        String targetName = args[0];
        UUID playerUUID = player.getUniqueId();

        // Check if this player is already tracking someone
        if (trackerToTarget.containsKey(playerUUID)) {
            player.sendMessage(ChatColor.RED + "You are already tracking another player. You need to wait or if they die/log off.");
            return true;
        }

        // Find the target player (case-insensitive)
        Player target = null;
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getName().equalsIgnoreCase(targetName)) {
                target = onlinePlayer;
                break;
            }
        }

        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player " + targetName + " is not online.");
            return true;
        }

        String sectionIdentifier = target.getName().toLowerCase(); // Use target's lowercase name as section ID

        if (sectionTrackers.containsKey(sectionIdentifier)) {
            player.sendMessage(ChatColor.RED + "This player is already being tracked by someone else.");
            return true;
        }

        // Save tracker and target mappings
        sectionTrackers.put(sectionIdentifier, playerUUID);
        trackerToTarget.put(playerUUID, target.getUniqueId());

        // Notify target player
        target.sendMessage(ChatColor.RED + "⚠ You are being targeted by " + player.getName() + "! "
                + ChatColor.GRAY + "Your location will begin to be broadcasted to their section in 2 minutes.");

        sender.sendMessage(ChatColor.GREEN + "Now tracking player " + target.getName() + ". The tracking will start in 2 minutes.");

        // Start tracking
        BukkitRunnable trackerTask = new BukkitRunnable() {
            @Override
            public void run() {
                Player currentTarget = Bukkit.getPlayer(trackerToTarget.get(playerUUID));
                if (currentTarget == null || !currentTarget.isOnline()) {
                    player.sendMessage(ChatColor.RED + "Target is no longer online. Tracking ended.");
                    cancelTracking(sectionIdentifier, playerUUID);
                    return;
                }

                Location loc = currentTarget.getLocation();
                player.sendMessage(ChatColor.GREEN + "Tracking " + currentTarget.getName() + " at: "
                        + "X: " + loc.getBlockX()
                        + " Y: " + loc.getBlockY()
                        + " Z: " + loc.getBlockZ()
                        + " World: " + loc.getWorld().getName());
            }
        };

        trackerTask.runTaskTimer(plugin, 2400L, 2400L); // Start after 2 mins, repeat every 2 mins
        activeTrackers.put(sectionIdentifier, trackerTask);

        player.sendMessage(ChatColor.GREEN + "Now tracking player " + target.getName() + ".");
        return true;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        handleTrackerOrTargetDeath(event.getEntity());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        handleTrackerOrTargetDeath(event.getPlayer());
    }

    private void handleTrackerOrTargetDeath(Player player) {
        UUID playerUUID = player.getUniqueId();

        // Check if the dying/quitting player is a tracker
        if (trackerToTarget.containsKey(playerUUID)) {
            // Cancel their tracking session
            String sectionToRemove = null;
            for (Map.Entry<String, UUID> entry : sectionTrackers.entrySet()) {
                if (entry.getValue().equals(playerUUID)) {
                    sectionToRemove = entry.getKey();
                    break;
                }
            }

            if (sectionToRemove != null) {
                cancelTracking(sectionToRemove, playerUUID);
                player.sendMessage(ChatColor.YELLOW + "Your tracking session has ended.");
                Player target = Bukkit.getPlayer(trackerToTarget.get(playerUUID));
                if (target != null) {
                    target.sendMessage(ChatColor.YELLOW + player.getName() + " is no longer tracking you.");
                }
            }
            return; // If the player was a tracker, we're done here for this event
        }

        // Check if the dying/quitting player is a target
        UUID deadOrQuittingPlayerUUID = player.getUniqueId();
        List<UUID> affectedTrackers = new ArrayList<>();

        for (Map.Entry<UUID, UUID> entry : trackerToTarget.entrySet()) {
            if (entry.getValue().equals(deadOrQuittingPlayerUUID)) {
                affectedTrackers.add(entry.getKey());
            }
        }

        for (UUID trackerUUID : affectedTrackers) {
            Player tracker = Bukkit.getPlayer(trackerUUID);
            if (tracker != null) {
                tracker.sendMessage(ChatColor.YELLOW + "Your target " + player.getName() + " has logged off or died. Tracking session ended.");
            }

            String sectionToRemove = null;
            for (Map.Entry<String, UUID> entry : sectionTrackers.entrySet()) {
                if (entry.getValue().equals(trackerUUID)) {
                    sectionToRemove = entry.getKey();
                    break;
                }
            }

            if (sectionToRemove != null) {
                cancelTracking(sectionToRemove, trackerUUID);
            }
        }
    }

    private void cancelTracking(String section, UUID trackerUUID) {
        if (activeTrackers.containsKey(section)) {
            activeTrackers.get(section).cancel();
            activeTrackers.remove(section);
        }

        sectionTrackers.remove(section);
        trackerToTarget.remove(trackerUUID);
    }
}