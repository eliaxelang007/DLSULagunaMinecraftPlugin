package zoy.dLSULaguna;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import zoy.dLSULaguna.commands.JoinSection;
import zoy.dLSULaguna.data.PlayerDataManager;
import zoy.dLSULaguna.listeners.*;
import zoy.dLSULaguna.listeners.PlayerOnlineListener.*;

import java.io.File;

public final class DLSULaguna extends JavaPlugin {
    private FileConfiguration playerStatsConfig;
    private File playersStatsFile;

    @Override
    public void onEnable() {
        // Plugin startup logic
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }

        // Initialize players_stats.yml file and load its configuration
        playersStatsFile = new File(dataFolder, "players_stats.yml");
        if (!playersStatsFile.exists()) {
            try {
                playersStatsFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        playerStatsConfig = YamlConfiguration.loadConfiguration(playersStatsFile);

        getLogger().info("DLSU Laguna Plugin Enabled");
        // Register commands and listeners
        this.getCommand("joinsection").setExecutor(new JoinSection(this));
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        new PlayerOnlineListener(this);
        getServer().getPluginManager().registerEvents(new PlayerBreakBlockListener(this),this);
        getServer().getPluginManager().registerEvents(new PlayersKillsMob(this),this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this),this);
        getServer().getPluginManager().registerEvents(new PlayerDistanceListener(this),this);
    }

    // Getter for the player stats configuration
    public FileConfiguration getPlayerStatsConfig() {
        return playerStatsConfig;
    }

    // Getter for the players stats file (in case you need it to save changes)
    public File getPlayersStatsFile() {
        return playersStatsFile;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic (optionally save playerStatsConfig if needed)
    }
}
