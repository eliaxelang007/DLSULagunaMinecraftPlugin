package zoy.dLSULaguna.listeners;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import zoy.dLSULaguna.DLSULaguna;
import zoy.dLSULaguna.utils.PlayerStatsFileUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class PlayerJoinListener implements Listener {
    private final DLSULaguna plugin;
    private final NamespacedKey sectionKey;
    private final NamespacedKey afkKey;
    private final Map<String, String> sectionNameMap = new HashMap<>();

    public PlayerJoinListener(DLSULaguna plugin) {
        this.plugin = plugin;
        this.sectionKey = new NamespacedKey(plugin, "section_name");
        this.afkKey = new NamespacedKey(plugin, "AFK");
        // Initialize the section name mapping for migration
        sectionNameMap.put("A", "STEM11-A");
        sectionNameMap.put("B", "STEM11-B");
        sectionNameMap.put("C", "STEM11-C");
        sectionNameMap.put("D", "STEM11-D");
        sectionNameMap.put("E", "STEM11-E");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.getPersistentDataContainer().set(afkKey, PersistentDataType.BOOLEAN, false);
        PersistentDataContainer playerData = player.getPersistentDataContainer();
        String oldSection = playerData.get(sectionKey, PersistentDataType.STRING);
        String newSection = null;

        if (oldSection != null && sectionNameMap.containsKey(oldSection.toUpperCase())) {
            newSection = sectionNameMap.get(oldSection.toUpperCase());
            playerData.set(sectionKey, PersistentDataType.STRING, newSection); // Update the persistent data
            player.sendMessage("Your section has been automatically updated to " + newSection + "!");
        } else if (oldSection != null) {
            newSection = oldSection; // Keep the existing (potentially already updated) section
            player.sendMessage("You are in the section " + newSection);
        } else {
            player.sendMessage("You are not in a section");
        }

        PlayerStatsFileUtil.setStat(player, "Last Log-in", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, HH:mm:ss a")));
        PlayerStatsFileUtil.setStat(player, "Username", player.getName());

        if (!player.hasPlayedBefore() && newSection == null) {
            player.sendMessage("§a§lWelcome to the server, §b§lSHS Students§a§l of §2§lDe La Salle University Laguna§a§l!");
            player.sendMessage("§7Please §e/joinsection <SECTION> §7to get started and be part of a section.");
        }

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Team team = board.getTeam(newSection);
        if (team == null && newSection != null) {
            team = board.registerNewTeam(newSection);
        }
        if (team != null) {
            team.addEntry(player.getName());
            player.setScoreboard(board);
        } else {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard()); // Set default if no section
        }
    }
}