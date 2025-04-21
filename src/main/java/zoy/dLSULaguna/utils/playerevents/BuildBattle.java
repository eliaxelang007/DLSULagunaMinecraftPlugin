package zoy.dLSULaguna.utils.playerevents;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.world.PortalCreateEvent;
import zoy.dLSULaguna.DLSULaguna;
import zoy.dLSULaguna.utils.PlayerDataUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class BuildBattle implements CommandExecutor, Listener {

    private final DLSULaguna plugin;
    private World buildWorld;

    private final Map<UUID, Location> returnLocations = new HashMap<>();
    private final Map<String, UUID> claimedChunks     = new HashMap<>();
    private File claimedChunksFile;
    private FileConfiguration claimedChunksConfig;
    private final Map<UUID, Long> claimCooldown      = new HashMap<>();

    private static final long CLAIM_COOLDOWN_TIME = TimeUnit.SECONDS.toMillis(30);
    private static final int  CLAIM_RADIUS        = 2;

    public BuildBattle(DLSULaguna plugin) {
        this.plugin = plugin;
        createClaimedChunksFile();
    }

    /** Create or get the BuildBattle superflat world */
    public void createBuildWorld() {
        if (buildWorld != null) return;

        buildWorld = new WorldCreator("BuildBattleWorld")
                .environment(World.Environment.NORMAL)
                .type(WorldType.FLAT)
                .createWorld();

        if (buildWorld != null) {
            buildWorld.setDifficulty(Difficulty.PEACEFUL);
            buildWorld.setPVP(true);
            plugin.getLogger().info("Superflat BuildBattle world created.");
            loadClaimedChunks();
        } else {
            plugin.getLogger().severe("Could not create BuildBattleWorld!");
        }
    }

    /** Expose the BuildBattle world */
    public World getBuildWorld() {
        return buildWorld;
    }

    private void createClaimedChunksFile() {
        claimedChunksFile = new File(plugin.getDataFolder(), "claimed_chunks.yml");
        if (!claimedChunksFile.exists()) {
            claimedChunksFile.getParentFile().mkdirs();
            try { claimedChunksFile.createNewFile(); } catch (IOException ignored) {}
        }
        claimedChunksConfig = YamlConfiguration.loadConfiguration(claimedChunksFile);
    }

    private void loadClaimedChunks() {
        claimedChunks.clear();
        for (String key : claimedChunksConfig.getKeys(false)) {
            String uuidStr = claimedChunksConfig.getString(key);
            if (uuidStr == null) continue;
            UUID owner = UUID.fromString(uuidStr);
            claimedChunks.put(key, owner);

            String[] parts = key.split("_");
            if (parts.length == 3) {
                try {
                    int cx = Integer.parseInt(parts[1]);
                    int cz = Integer.parseInt(parts[2]);
                    markChunkBorders(buildWorld.getChunkAt(cx, cz));
                } catch (NumberFormatException ignored) {}
            }
        }
        plugin.getLogger().info("Loaded " + claimedChunks.size() + " claimed chunks.");
    }

    private void saveClaimedChunks() {
        claimedChunksConfig.getKeys(false).forEach(k -> claimedChunksConfig.set(k, null));
        claimedChunks.forEach((k, uuid) -> claimedChunksConfig.set(k, uuid.toString()));
        try {
            claimedChunksConfig.save(claimedChunksFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save claimed_chunks.yml!");
        }
    }

    private void markChunkBorders(Chunk chunk) {
        int minX = chunk.getX() << 4, minZ = chunk.getZ() << 4;
        int maxX = minX + 15, maxZ = minZ + 15;
        for (int x = minX; x <= maxX; x++) {
            protectBedrock(buildWorld.getBlockAt(x, 0, minZ));
            protectBedrock(buildWorld.getBlockAt(x, 0, maxZ));
        }
        for (int z = minZ + 1; z < maxZ; z++) {
            protectBedrock(buildWorld.getBlockAt(minX, 0, z));
            protectBedrock(buildWorld.getBlockAt(maxX, 0, z));
        }
    }

    private void protectBedrock(Block b) {
        b.setType(Material.BEDROCK);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player p = (Player) sender;
        if (!cmd.getName().equalsIgnoreCase("buildbattle")) return false;

        if (args.length == 0) {
            if (!p.getWorld().equals(buildWorld)) {
                returnLocations.put(p.getUniqueId(), p.getLocation());
                PlayerDataUtil.savePlayerState(p.getUniqueId());
                p.setGameMode(GameMode.CREATIVE);
                p.teleport(buildWorld.getSpawnLocation());
                p.sendMessage("Teleported to BuildBattle world.");
            }
            return true;
        }

        if (args.length == 1 && p.getWorld().equals(buildWorld)) {
            String sub = args[0].toLowerCase(Locale.ROOT);

            if ("back".equals(sub)) {
                UUID id = p.getUniqueId();
                Location back = returnLocations.remove(id);
                PlayerDataUtil.loadPlayerState(id);
                PlayerDataUtil.restorePlayerState(p);
                if (back != null) p.teleport(back);
                p.sendMessage("Returned to previous state.");
                return true;
            }

            if ("claim".equals(sub)) {
                long now = System.currentTimeMillis();
                long last = claimCooldown.getOrDefault(p.getUniqueId(), 0L);
                if (now - last < CLAIM_COOLDOWN_TIME) {
                    p.sendMessage(ChatColor.RED + "Wait " +
                            ((CLAIM_COOLDOWN_TIME - (now - last)) / 1000) + "s.");
                    return true;
                }

                Set<Chunk> toClaim = new HashSet<>();
                Chunk center = p.getLocation().getChunk();
                for (int dx = -CLAIM_RADIUS; dx <= CLAIM_RADIUS; dx++) {
                    for (int dz = -CLAIM_RADIUS; dz <= CLAIM_RADIUS; dz++) {
                        Chunk c = buildWorld.getChunkAt(center.getX() + dx, center.getZ() + dz);
                        String key = c.getWorld().getName() + "_" + c.getX() + "_" + c.getZ();
                        if (!claimedChunks.containsKey(key)) {
                            toClaim.add(c);
                        } else if (!claimedChunks.get(key).equals(p.getUniqueId())) {
                            p.sendMessage(ChatColor.RED +
                                    "Chunk (" + c.getX() + "," + c.getZ() + ") is already claimed.");
                            return true;
                        }
                    }
                }

                toClaim.forEach(c -> {
                    String key = c.getWorld().getName() +
                            "_" + c.getX() + "_" + c.getZ();
                    claimedChunks.put(key, p.getUniqueId());
                    markChunkBorders(c);
                });

                saveClaimedChunks();
                claimCooldown.put(p.getUniqueId(), now);
                p.sendMessage("Claimed a " +
                        (CLAIM_RADIUS*2+1) + "×" + (CLAIM_RADIUS*2+1) + " area.");
                return true;
            }
        }

        return false;
    }

    // only protect the bottom‐layer bedrock
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent e) {
        if (buildWorld != null
                && e.getBlock().getWorld().equals(buildWorld)
                && e.getBlock().getType() == Material.BEDROCK
                && e.getBlock().getY() == buildWorld.getMinHeight()) {

            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + "You cannot break the floor bedrock.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockDamage(BlockDamageEvent e) {
        if (buildWorld != null
                && e.getBlock().getWorld().equals(buildWorld)
                && e.getBlock().getType() == Material.BEDROCK
                && e.getBlock().getY() == buildWorld.getMinHeight()) {

            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + "You cannot damage the floor bedrock.");
        }
    }

    @EventHandler public void onEntityExplode(EntityExplodeEvent e) {
        if (buildWorld != null &&
                e.getLocation().getWorld().equals(buildWorld)) {
            e.setCancelled(true);
        }
    }

    @EventHandler public void onBlockExplode(BlockExplodeEvent e) {
        if (buildWorld != null &&
                e.getBlock().getWorld().equals(buildWorld)) {
            e.setCancelled(true);
        }
    }

    @EventHandler public void onPortalCreate(PortalCreateEvent e) {
        if (buildWorld != null &&
                e.getWorld().equals(buildWorld)) {
            e.setCancelled(true);
        }
    }

    @EventHandler public void onPlayerPortal(PlayerPortalEvent e) {
        if (buildWorld != null &&
                e.getFrom().getWorld().equals(buildWorld)) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + "Portals disabled.");
        }
    }

    @EventHandler public void onPlayerTeleport(PlayerTeleportEvent e) {
        if (buildWorld != null
                && e.getFrom().getWorld().equals(buildWorld)
                && (e.getCause() == TeleportCause.NETHER_PORTAL
                || e.getCause() == TeleportCause.END_PORTAL)) {

            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + "Portal travel disabled.");
        }
    }

    @EventHandler public void onCommandPreprocess(PlayerCommandPreprocessEvent e) {
        if (buildWorld != null
                && e.getPlayer().getWorld().equals(buildWorld)) {

            String msg = e.getMessage().toLowerCase(Locale.ROOT);
            if (msg.startsWith("/tp")
                    || msg.startsWith("/teleport")
                    || msg.startsWith("/tpa")) {

                e.setCancelled(true);
                e.getPlayer().sendMessage(ChatColor.RED + "Teleport disabled.");
            }
        }
    }
}
