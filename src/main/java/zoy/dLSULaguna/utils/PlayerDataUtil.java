package zoy.dLSULaguna.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import zoy.dLSULaguna.DLSULaguna;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class PlayerDataUtil {

    private static DLSULaguna plugin;
    private static NamespacedKey sectionKey;
    private static File playerStateFile;
    private static FileConfiguration playerStateConfig;

    // State caches
    private static final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
    private static final Map<UUID, ItemStack[]> savedArmor = new HashMap<>();
    private static final Map<UUID, ItemStack> savedOffhand = new HashMap<>();
    private static final Map<UUID, Location> savedLocations = new HashMap<>();
    private static final Map<UUID, GameMode> savedGameModes = new HashMap<>();
    private static final Map<UUID, List<PotionEffect>> savedEffects = new HashMap<>();
    private static final Map<UUID, Float> savedExp = new HashMap<>();
    private static final Map<UUID, Integer> savedLevel = new HashMap<>();
    private static final Map<UUID, Integer> savedFoodLevel = new HashMap<>();
    private static final Map<UUID, Double> savedHealth = new HashMap<>();

    /** Initialize the PlayerDataUtil with plugin instance. */
    public static void initialize(DLSULaguna pluginInstance) {
        plugin = pluginInstance;
        sectionKey = new NamespacedKey(plugin, "section_name");
        createPlayerStateFile();
    }

    /** Create or load the player_states.yml file. */
    private static void createPlayerStateFile() {
        File file = new File(plugin.getDataFolder(), "player_states.yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
                plugin.getLogger().info("Created player_states.yml");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create player_states.yml", e);
            }
        }
        playerStateFile = file;
        playerStateConfig = YamlConfiguration.loadConfiguration(file);
    }

    /** Get section for online or offline player by UUID. */
    public static String getPlayerSection(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            PersistentDataContainer container = online.getPersistentDataContainer();
            return container.has(sectionKey, PersistentDataType.STRING)
                    ? container.get(sectionKey, PersistentDataType.STRING)
                    : null;
        }
        return getStatRaw(uuid, "section_name");
    }

    /** Overloaded version for Player object */
    public static String getPlayerSection(Player player) {
        return getPlayerSection(player.getUniqueId());
    }

    /** Set section for online or offline player by UUID. */
    public static void setPlayerSection(UUID uuid, String section) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            PersistentDataContainer container = online.getPersistentDataContainer();
            if (section != null) container.set(sectionKey, PersistentDataType.STRING, section);
            else container.remove(sectionKey);
        } else {
            PlayerStatsFileUtil.setStatRaw(uuid, "section_name", section);
        }
    }

    /** Remove section for online or offline player by UUID. */
    public static void removePlayerSection(UUID uuid) {
        setPlayerSection(uuid, null);
    }

    /** Get a raw stat value for an online or offline player by key. */
    public static String getStatRaw(UUID uuid, String statKey) {
        Object raw = PlayerStatsFileUtil.getStat(uuid, getPlayerSection(uuid), statKey);
        return raw != null ? raw.toString() : null;
    }

    /** Get a raw stat value for an online player by key. */
    public static String getStatRaw(Player player, String statKey) {
        return getStatRaw(player.getUniqueId(), statKey);
    }


    /** Save a player's current state to disk and memory. */
    public static void savePlayerState(UUID uuid) {
        if (playerStateConfig == null) return;
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            plugin.getLogger().warning("Attempted to save state for offline player: " + uuid);
            return;
        }

        // Cache in memory
        savedInventories.put(uuid, player.getInventory().getContents());
        savedArmor.put(uuid, player.getInventory().getArmorContents());
        savedOffhand.put(uuid, player.getInventory().getItemInOffHand());
        savedLocations.put(uuid, player.getLocation());
        savedGameModes.put(uuid, player.getGameMode());
        savedEffects.put(uuid, new ArrayList<>(player.getActivePotionEffects()));
        savedExp.put(uuid, player.getExp());
        savedLevel.put(uuid, player.getLevel());
        savedFoodLevel.put(uuid, player.getFoodLevel());
        savedHealth.put(uuid, player.getHealth());

        // Persist to YAML
        try {
            playerStateConfig.set(uuid + ".inventory", Arrays.asList(player.getInventory().getContents()));
            playerStateConfig.set(uuid + ".armor", Arrays.asList(player.getInventory().getArmorContents()));
            playerStateConfig.set(uuid + ".offhand", player.getInventory().getItemInOffHand());

            Location loc = player.getLocation();
            playerStateConfig.set(uuid + ".location.world", loc.getWorld().getName());
            playerStateConfig.set(uuid + ".location.x", loc.getX());
            playerStateConfig.set(uuid + ".location.y", loc.getY());
            playerStateConfig.set(uuid + ".location.z", loc.getZ());

            playerStateConfig.set(uuid + ".gameMode", player.getGameMode().name());
            playerStateConfig.set(uuid + ".effects", serializePotionEffects(player.getActivePotionEffects()));
            playerStateConfig.set(uuid + ".exp", player.getExp());
            playerStateConfig.set(uuid + ".level", player.getLevel());
            playerStateConfig.set(uuid + ".foodLevel", player.getFoodLevel());
            playerStateConfig.set(uuid + ".health", player.getHealth());

            playerStateConfig.save(playerStateFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player state for UUID: " + uuid);
            e.printStackTrace();
        }
    }

    /** Load a player's saved state into memory from disk. */
    public static void loadPlayerState(UUID uuid) {
        if (playerStateConfig == null || !playerStateConfig.contains(uuid.toString())) return;

        // Inventory
        List<?> invList = playerStateConfig.getList(uuid + ".inventory");
        if (invList != null) {
            ItemStack[] inv = invList.stream()
                    .filter(o -> o instanceof ItemStack)
                    .map(o -> (ItemStack) o)
                    .toArray(ItemStack[]::new);
            savedInventories.put(uuid, inv);
        }

        // Armor
        List<?> armList = playerStateConfig.getList(uuid + ".armor");
        if (armList != null) {
            ItemStack[] armor = armList.stream()
                    .filter(o -> o instanceof ItemStack)
                    .map(o -> (ItemStack) o)
                    .toArray(ItemStack[]::new);
            savedArmor.put(uuid, armor);
        }

        // Offhand
        ItemStack off = playerStateConfig.getItemStack(uuid + ".offhand");
        if (off != null) savedOffhand.put(uuid, off);

        // Location
        String worldName = playerStateConfig.getString(uuid + ".location.world");
        World world = Bukkit.getWorld(worldName);
        double x = playerStateConfig.getDouble(uuid + ".location.x");
        double y = playerStateConfig.getDouble(uuid + ".location.y");
        double z = playerStateConfig.getDouble(uuid + ".location.z");
        savedLocations.put(uuid, new Location(world, x, y, z));

        // GameMode
        savedGameModes.put(uuid, GameMode.valueOf(
                playerStateConfig.getString(uuid + ".gameMode", "SURVIVAL")));

        // Effects
        List<?> effectList = playerStateConfig.getList(uuid + ".effects");
        if (effectList != null) {
            List<PotionEffect> effects = deserializePotionEffects(effectList);
            savedEffects.put(uuid, effects);
        }

        // Exp, Level, Food, Health
        savedExp.put(uuid, (float) playerStateConfig.getDouble(uuid + ".exp", 0.0));
        savedLevel.put(uuid, playerStateConfig.getInt(uuid + ".level", 0));
        savedFoodLevel.put(uuid, playerStateConfig.getInt(uuid + ".foodLevel", 20));
        savedHealth.put(uuid, playerStateConfig.getDouble(uuid + ".health", 20.0));
    }

    /** Restore a player's saved state from memory. */
    public static void restorePlayerState(Player player) {
        UUID uuid = player.getUniqueId();

        // Clear inventory slots to avoid duplication
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);

        // Restore inventory contents
        if (savedInventories.containsKey(uuid)) {
            player.getInventory().setContents(savedInventories.get(uuid));
        }

        // Restore armor slots
        if (savedArmor.containsKey(uuid)) {
            player.getInventory().setArmorContents(savedArmor.get(uuid));
        }

        // Restore offhand item
        if (savedOffhand.containsKey(uuid)) {
            player.getInventory().setItemInOffHand(savedOffhand.get(uuid));
        }

        // Restore location
        if (savedLocations.containsKey(uuid)) {
            player.teleport(savedLocations.get(uuid));
        }

        // Restore gamemode
        if (savedGameModes.containsKey(uuid)) {
            player.setGameMode(savedGameModes.get(uuid));
        }

        // Restore potion effects
        if (savedEffects.containsKey(uuid)) {
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
            player.addPotionEffects(savedEffects.get(uuid));
        }

        // Restore misc state
        if (savedExp.containsKey(uuid)) player.setExp(savedExp.get(uuid));
        if (savedLevel.containsKey(uuid)) player.setLevel(savedLevel.get(uuid));
        if (savedFoodLevel.containsKey(uuid)) player.setFoodLevel(savedFoodLevel.get(uuid));
        if (savedHealth.containsKey(uuid)) player.setHealth(savedHealth.get(uuid));

        player.updateInventory();
        plugin.getLogger().info("Restored state for player: " + player.getName());
    }

    /** Serialize potion effects into a list of maps for YAML. */
    private static List<Map<String,Object>> serializePotionEffects(Collection<PotionEffect> effects) {
        List<Map<String,Object>> list = new ArrayList<>();
        for (PotionEffect effect : effects) {
            Map<String,Object> map = new HashMap<>();
            map.put("type", effect.getType().getName());
            map.put("duration", effect.getDuration());
            map.put("amplifier", effect.getAmplifier());
            list.add(map);
        }
        return list;
    }

    /** Deserialize potion effects from YAML. */
    @SuppressWarnings("unchecked")
    private static List<PotionEffect> deserializePotionEffects(List<?> list) {
        List<PotionEffect> effects = new ArrayList<>();
        if (list == null) return effects;
        for (Object obj : list) {
            if (obj instanceof Map<?,?> map) {
                String type = (String) map.get("type");
                int duration = (int) map.get("duration");
                int amp = (int) map.get("amplifier");
                PotionEffectType pet = PotionEffectType.getByName(type);
                if (pet != null) effects.add(new PotionEffect(pet, duration, amp));
            }
        }
        return effects;
    }
}
