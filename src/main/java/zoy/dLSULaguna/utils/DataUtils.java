package zoy.dLSULaguna.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import zoy.dLSULaguna.DLSULaguna;

public class DataUtils {

    public static String getSection(Player player, DLSULaguna plugin) {
        // Create the same key used when setting the data.
        NamespacedKey sectionKey = new NamespacedKey(plugin, "section_name");
        PersistentDataContainer container = player.getPersistentDataContainer();
        return container.get(sectionKey, PersistentDataType.STRING);
    }
    public static Boolean isPlayerOnline(Player player, DLSULaguna plugin){
        return plugin.getServer().getPlayer(player.getUniqueId()) != null;
    }
}