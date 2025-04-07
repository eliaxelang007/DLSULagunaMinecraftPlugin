package zoy.dLSULaguna.listeners;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.util.FileUtil;
import zoy.dLSULaguna.DLSULaguna;
import zoy.dLSULaguna.utils.DataUtils;
import zoy.dLSULaguna.utils.PlayerStatsUtils;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class PlayerJoinListener implements Listener {
    private DLSULaguna plugin;

    public PlayerJoinListener(DLSULaguna plugin){
        this.plugin = plugin;
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        String player_section = DataUtils.getSection(event.getPlayer(), plugin);
        if(player_section != null){
            event.getPlayer().sendMessage("You are in the section " + player_section);
        }else{
            event.getPlayer().sendMessage("You are not in a section");
        }
        try{
            File statsFile = plugin.getPlayersStatsFile();
            FileConfiguration stats = plugin.getPlayerStatsConfig();
            // Example: setting multiple values for a player
            String playerKey = event.getPlayer().getUniqueId().toString();
            stats.set(playerKey + ".Last Log in", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd yyyy HH:mm:ss a")));
            stats.set(playerKey + ".Time Logged In", 0);
            stats.save(statsFile);

        }catch (IOException e ){
            e.printStackTrace();
        }

    }
}
