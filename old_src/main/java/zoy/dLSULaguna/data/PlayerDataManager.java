/* This class isn't actually used? */
/*
 * package zoy.dLSULaguna.data;
 * 
 * import org.bukkit.NamespacedKey;
 * import org.bukkit.entity.Player;
 * import org.bukkit.persistence.PersistentDataContainer;
 * import org.bukkit.persistence.PersistentDataType;
 * import org.bukkit.plugin.java.JavaPlugin;
 * 
 * public class PlayerDataManager {
 * private final NamespacedKey key;
 * 
 * public PlayerDataManager(JavaPlugin plugin, String keyName) {
 * this.key = new NamespacedKey(plugin, keyName);
 * }
 * 
 * public <P, C> void setData(Player player, PersistentDataType<P, C> type, C
 * value) {
 * PersistentDataContainer container = player.getPersistentDataContainer();
 * container.set(this.key, type, value);
 * }
 * 
 * public <T> T getData(Player player) {
 * PersistentDataContainer container = player.getPersistentDataContainer();
 * return container.get(key, PersistentDataType.STRING);
 * }
 * }
 */