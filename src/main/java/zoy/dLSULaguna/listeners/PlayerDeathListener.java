package zoy.dLSULaguna.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import zoy.dLSULaguna.DLSULaguna;

public class PlayerDeathListener implements Listener {
    private DLSULaguna plugin;
    public PlayerDeathListener(DLSULaguna plugin){
        this.plugin = plugin;
    }
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event){
        Player player = event.getEntity();
        String playerKey = player.getUniqueId().toString();
        plugin.getPlayerStatsConfig().set(playerKey + ".Deaths", plugin.getPlayerStatsConfig().getInt(playerKey + ".Deaths") + 1);
        try{
            plugin.getPlayerStatsConfig().save(plugin.getPlayersStatsFile());
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
