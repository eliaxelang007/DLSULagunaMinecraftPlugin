package zoy.dLSULaguna.data;

import org.bukkit.entity.Player;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataContainer;

public class PlayerDataManager {
    private final NamespacedKey key;

    public PlayerDataManager(JavaPlugin plugin, String keyName){
        this.key = new NamespacedKey(plugin, keyName);
    }
    public void setData(Player player, String value){
        PersistentDataContainer container = player.getPersistentDataContainer();
        container.set(this.key, PersistentDataType.STRING, value);
    }
    public String getData(Player player){
        PersistentDataContainer container = player.getPersistentDataContainer();
        return container.get(key, PersistentDataType.STRING);
    }
}
