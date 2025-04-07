package zoy.dLSULaguna.listeners;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerEvent;
import zoy.dLSULaguna.DLSULaguna;

public class PlayerBreakBlockListener implements Listener {
    private DLSULaguna plugin;

    public PlayerBreakBlockListener(DLSULaguna plugin){
        this.plugin = plugin;
    }
    @EventHandler
    public void onPlayerBreakBlock(BlockBreakEvent event){
        Player player = event.getPlayer();
        String blockType = event.getBlock().getType().toString();
        FileConfiguration stats = plugin.getPlayerStatsConfig();
        String playerKey = player.getUniqueId().toString();
        stats.set(playerKey + ".Blocks broken", stats.getInt(playerKey + ".Blocks broken") + 1);
        stats.set(playerKey + ".Blocks broken type." + blockType, stats.getInt(playerKey + ".Blocks broken type." + blockType) + 1);
        player.sendMessage("You have broken " + blockType);
        try{
            stats.save(plugin.getPlayersStatsFile());
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
