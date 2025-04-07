package zoy.dLSULaguna.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import zoy.dLSULaguna.DLSULaguna;

public class PlayerDistanceListener implements Listener {
    private final DLSULaguna plugin;
    public PlayerDistanceListener(DLSULaguna plugin){
        this.plugin = plugin;
    }
    @EventHandler
    public void onPlayerDistance(PlayerMoveEvent event){
        Player player = event.getPlayer();
        Location from = event.getTo();
        Location to = event.getFrom();
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        double distance = from.distance(to);
        String playerKey = player.getUniqueId().toString();
        plugin.getPlayerStatsConfig().set(playerKey + ".Distance", plugin.getPlayerStatsConfig().getDouble(playerKey + ".Distance") + distance);
        try{
            plugin.getPlayerStatsConfig().save(plugin.getPlayersStatsFile());
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
