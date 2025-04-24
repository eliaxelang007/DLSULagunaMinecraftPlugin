package zoy.dLSULaguna.listeners;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import zoy.dLSULaguna.DLSULaguna;
import zoy.dLSULaguna.commands.SectionChat;
import zoy.dLSULaguna.utils.PlayerDataUtil;

import java.io.File;
import java.util.UUID;

public class PlayerChatListener implements Listener {
    private final DLSULaguna plugin;
    private final SectionChat sectionChat;
    private File donatorsFile;
    private FileConfiguration donatorsConfig;

    public PlayerChatListener(DLSULaguna plugin, SectionChat sectionChat) {
        this.plugin = plugin;
        this.sectionChat = sectionChat;
        donatorsFile = new File(plugin.getDataFolder(), "donators.yml");
        donatorsConfig = YamlConfiguration.loadConfiguration(donatorsFile);
        if (!donatorsFile.exists()) {
            try {
                donatorsFile.getParentFile().mkdirs();
                donatorsFile.createNewFile();
            } catch (Exception e) {
                plugin.getLogger().warning("Could not create donators.yml");
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        String section = PlayerDataUtil.getPlayerSection(player);

        if (sectionChat.isSectionChatEnabled(uuid)) {
            // Send it only to players in the same section
            event.setCancelled(true);
            String message = "[" + section + " Private] " + ChatColor.RESET + player.getName() + ": " + ChatColor.WHITE + event.getMessage();
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (section.equals(PlayerDataUtil.getPlayerSection(p))) {
                    p.sendMessage(message);
                }
            }
        } else {
            // Section chat is off: let the message go globally, but format it
            event.setFormat("[" + section + "] " + ChatColor.RESET + "%s: " + ChatColor.WHITE + "%s");
            if(donatorsConfig.contains(player.getName())){
                event.setFormat("[" + section + "] " + ChatColor.RESET + "%s(Donator): " + ChatColor.WHITE + "%s");
            }
        }
    }

}
