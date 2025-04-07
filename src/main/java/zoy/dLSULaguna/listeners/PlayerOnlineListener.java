package zoy.dLSULaguna.listeners;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import zoy.dLSULaguna.DLSULaguna;
import zoy.dLSULaguna.utils.PlayerStatsUtils;

import java.io.File;
import java.io.IOException;

public class PlayerOnlineListener implements Listener {
    private final DLSULaguna plugin;

    public PlayerOnlineListener(DLSULaguna plugin){
        this.plugin = plugin;
        // If you want to schedule periodic updates, you can do that here.
        FileConfiguration stats = plugin.getPlayerStatsConfig();
        File statsFile = plugin.getPlayersStatsFile();
        Bukkit.getScheduler().runTaskTimer(plugin, ()->{
            for(Player p : Bukkit.getOnlinePlayers()){
                stats.set(p.getUniqueId().toString() + ".Time Logged In", stats.getInt(p.getUniqueId().toString() + ".Time Logged In") + 1);
                p.sendMessage("You have been online for " + stats.getInt(p.getUniqueId().toString() + ".Time Logged In") + " minutes");
                try{
                    stats.save(statsFile);
                }catch (IOException e){
                    e.printStackTrace();
                }

            }
        },0L,1200L);
    }
    }

