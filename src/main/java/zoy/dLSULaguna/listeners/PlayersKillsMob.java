package zoy.dLSULaguna.listeners;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import zoy.dLSULaguna.DLSULaguna;

import java.io.File;

public class PlayersKillsMob implements Listener {
    private DLSULaguna plugin;
    public PlayersKillsMob(DLSULaguna plugin){
        this.plugin = plugin;
    }
    @EventHandler
    public void onPlayerKillMob(EntityDeathEvent event){
        File statsFile = plugin.getPlayersStatsFile();
        FileConfiguration stats = plugin.getPlayerStatsConfig();
        EntityType type = event.getEntityType();
        if(event.getEntity().getKiller() != null){
            if (type == EntityType.ZOMBIE ||
                    type == EntityType.SKELETON ||
                    type == EntityType.CREEPER ||
                    type == EntityType.SPIDER ||
                    type == EntityType.ENDERMAN ||
                    type == EntityType.WITCH ||
                    type == EntityType.BLAZE ||
                    type == EntityType.WITHER_SKELETON ||
                    type == EntityType.HUSK ||
                    type == EntityType.DROWNED) {
                String playerKey = event.getEntity().getKiller().getUniqueId().toString();
                Player player = event.getEntity().getKiller();
                player.sendMessage("You have killed " + event.getEntity().getType().toString());
                stats.set(playerKey + ".Mobs killed", stats.getInt(playerKey + ".Mobs killed") + 1);
                stats.set(playerKey + ".Mobs killed type." + event.getEntity().getType().toString(), stats.getInt(playerKey + ".Mobs killed type." + event.getEntity().getType().toString()) + 1);

                try{
                    stats.save(statsFile);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

        }

    }
}
