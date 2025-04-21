package zoy.dLSULaguna.commands;

import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SectionChat implements CommandExecutor {
    private static final Map<UUID,Boolean> sectionChatEnabled = new HashMap<>();
    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String s, String[] args) {

        if(!(sender instanceof Player player)){
            return false;
        }
        UUID uuid = player.getUniqueId();
        boolean enabled = sectionChatEnabled.getOrDefault(uuid, false);
        sectionChatEnabled.put(uuid, !enabled);
        player.sendMessage("§6Section Chat is now " + (enabled ? "§c§lDISABLED" : "§a§lENABLED"));
        return true;
    }
    public static boolean isSectionChatEnabled(UUID uuid) {
        return sectionChatEnabled.getOrDefault(uuid, false);
    }
}
