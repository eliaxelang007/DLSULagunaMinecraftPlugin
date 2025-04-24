package zoy.dLSULaguna.utils.playerevents;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import zoy.dLSULaguna.DLSULaguna;
import zoy.dLSULaguna.utils.PlayerDataUtil;
import zoy.dLSULaguna.utils.PlayerStatsFileUtil;

import java.util.*;

public class Duels implements Listener, CommandExecutor {

    private final DLSULaguna plugin;
    private final Map<UUID, UUID> pendingDuels = new HashMap<>();
    private final Map<UUID, Location> oldLocations = new HashMap<>();
    private final Map<UUID, Integer> xpPoints = new HashMap<>();
    private final Map<UUID, ItemStack[]> storedInventories = new HashMap<>();
    private final Set<UUID> inDuel = new HashSet<>();
    private final Map<UUID, Location> pendingTeleport = new HashMap<>();
    private int pointsWagered;

    public Duels(DLSULaguna plugin) {
        this.plugin = plugin;
        createDuelWorld();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (!(sender instanceof Player)) return false;
        Player challenger = (Player) sender;

        if (!inDuel.isEmpty()) {
            challenger.sendMessage(ChatColor.RED + "A duel is already in progress. Please wait.");
            return true;
        }
        if (args.length < 2) {
            challenger.sendMessage(ChatColor.RED + "Usage: /duel <player> <points>");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || target == challenger) {
            challenger.sendMessage(ChatColor.RED + "Invalid player.");
            return true;
        }
        try {
            pointsWagered = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            challenger.sendMessage(ChatColor.RED + "Invalid number of points.");
            return true;
        }
        int challengerPoints = PlayerStatsFileUtil.getStatInt(
                challenger.getUniqueId(),
                PlayerDataUtil.getPlayerSection(challenger),
                "Points", 0
        );
        int targetPoints = PlayerStatsFileUtil.getStatInt(
                target.getUniqueId(),
                PlayerDataUtil.getPlayerSection(target),
                "Points", 0
        );
        if (challengerPoints < pointsWagered) {
            challenger.sendMessage(ChatColor.RED + "You don't have enough points to wager that amount.");
            return true;
        }
        if (targetPoints < pointsWagered) {
            challenger.sendMessage(ChatColor.RED + target.getName() + " doesn't have enough points.");
            return true;
        }
        pendingDuels.put(target.getUniqueId(), challenger.getUniqueId());
        target.sendMessage(
                ChatColor.YELLOW + challenger.getName() + " has challenged you to a duel for "
                        + pointsWagered + " points. Type /duelaccept or /dueldeny."
        );
        challenger.sendMessage(
                ChatColor.YELLOW + "Duel request sent to " + target.getName()
        );
        return true;
    }

    public boolean duelAcceptCommand(CommandSender sender, Command command,
                                     String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        Player acceptor = (Player) sender;
        UUID challengerId = pendingDuels.remove(acceptor.getUniqueId());
        if (challengerId == null) {
            acceptor.sendMessage(ChatColor.RED + "No pending duel.");
            return true;
        }
        Player challenger = Bukkit.getPlayer(challengerId);
        if (challenger == null) {
            acceptor.sendMessage(ChatColor.RED + "Challenger is no longer online.");
            return true;
        }
        startDuel(challenger, acceptor);
        return true;
    }

    public boolean duelDenyCommand(CommandSender sender, Command command,
                                   String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        Player denier = (Player) sender;
        UUID challengerId = pendingDuels.remove(denier.getUniqueId());
        if (challengerId != null) {
            Player challenger = Bukkit.getPlayer(challengerId);
            if (challenger != null) {
                challenger.sendMessage(
                        ChatColor.RED + denier.getName() + " denied your duel request."
                );
            }
        }
        denier.sendMessage(ChatColor.RED + "You denied the duel request.");
        return true;
    }

    private void startDuel(Player p1, Player p2) {
        inDuel.add(p1.getUniqueId());
        inDuel.add(p2.getUniqueId());

        // store state
        oldLocations.put(p1.getUniqueId(), p1.getLocation());
        oldLocations.put(p2.getUniqueId(), p2.getLocation());
        storedInventories.put(p1.getUniqueId(), p1.getInventory().getContents());
        storedInventories.put(p2.getUniqueId(), p2.getInventory().getContents());
        xpPoints.put(p1.getUniqueId(), p1.getLevel());
        xpPoints.put(p2.getUniqueId(), p2.getLevel());

        // clear and equip
        p1.getInventory().clear();
        p2.getInventory().clear();
        setDuelInventory(p1);
        setDuelInventory(p2);

        // countdown
        new BukkitRunnable() {
            int time = 5;
            @Override
            public void run() {
                if (time <= 0) {
                    World duelWorld = Bukkit.getWorld("duel");
                    Location loc1 = new Location(duelWorld, -5, 2, -5);
                    Location loc2 = new Location(duelWorld, 5, 2, 5);
                    p1.teleport(loc1);
                    p2.teleport(loc2);
                    Bukkit.broadcastMessage(
                            ChatColor.GREEN + "Duel has started between "
                                    + p1.getName() + " and " + p2.getName()
                                    + " for " + pointsWagered + " points."
                    );
                    cancel();
                } else {
                    p1.sendMessage(ChatColor.YELLOW + "Duel starts in " + time + "...");
                    p2.sendMessage(ChatColor.YELLOW + "Duel starts in " + time + "...");
                    time--;
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public static void createDuelWorld() {
        if (Bukkit.getWorld("duel") != null) return;
        WorldCreator wc = new WorldCreator("duel")
                .environment(World.Environment.NORMAL)
                .generator(new ChunkGenerator() {
                               @Override
                               public ChunkData generateChunkData(
                                       World world, Random random, int chunkX, int chunkZ, BiomeGrid biome
                               ) {
                                   ChunkData chunk = createChunkData(world);
                                   int baseX = chunkX * 16, baseZ = chunkZ * 16;
                                   for (int x = 0; x < 16; x++) {
                                       for (int z = 0; z < 16; z++) {
                                           int absX = baseX + x, absZ = baseZ + z;
                                           if (absX >= -10 && absX < 10 && absZ >= -10 && absZ < 10) {
                                               chunk.setBlock(x, 0, z, Material.BEDROCK);
                                           }
                                       }
                                   }
                                   return chunk;
                               }
                           }
                );
        World duel = Bukkit.createWorld(wc);
        duel.setSpawnLocation(new Location(duel, 0, 2, 0));
        WorldBorder border = duel.getWorldBorder();
        border.setCenter(0, 0);
        border.setSize(20);
        border.setWarningDistance(1);
        border.setWarningTime(5);
        border.setDamageBuffer(0);
        border.setDamageAmount(1);
    }

    @EventHandler
    public void onDuelDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer != null && inDuel.contains(killer.getUniqueId())
                && inDuel.contains(victim.getUniqueId())) {
            PlayerStatsFileUtil.increaseStat(killer, "Duel-points", pointsWagered);
            PlayerStatsFileUtil.increaseStat(victim, "Duel-points", -pointsWagered);
            Bukkit.broadcastMessage(
                    ChatColor.GOLD + killer.getName() + " won the duel against "
                            + victim.getName()
            );
            pendingTeleport.put(victim.getUniqueId(), oldLocations.get(victim.getUniqueId()));
            new BukkitRunnable() {
                @Override
                public void run() {
                    restorePlayer(killer);
                }
            }.runTaskLater(plugin, 20L);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        if (pendingTeleport.containsKey(uuid)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    restorePlayer(player);
                    pendingTeleport.remove(uuid);
                }
            }.runTaskLater(plugin, 20L);
        }
    }

    private void restorePlayer(Player player) {
        UUID id = player.getUniqueId();
        player.teleport(oldLocations.remove(id));
        player.getInventory().setContents(storedInventories.remove(id));
        player.setLevel(xpPoints.remove(id));
        player.setGameMode(GameMode.SURVIVAL);
        inDuel.remove(id);
    }

    private void setDuelInventory(Player player) {
        // armor, weapons, potions, etc. unchanged

        // Armor
        player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
        // Weapons
        player.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
        player.getInventory().addItem(new ItemStack(Material.IRON_AXE));
        // Shield offhand
        player.getInventory().setItemInOffHand(new ItemStack(Material.SHIELD));
        // Bow + arrows
        player.getInventory().addItem(new ItemStack(Material.BOW));
        player.getInventory().addItem(new ItemStack(Material.ARROW, 32));
        // Food
        player.getInventory().addItem(new ItemStack(Material.BREAD, 16));
        // Potions
        for (int i = 0; i < 3; i++) {
            ItemStack potion = new ItemStack(Material.SPLASH_POTION);
            PotionMeta meta = (PotionMeta) potion.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "Healing Potion");
            meta.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 1), true);
            potion.setItemMeta(meta);
            player.getInventory().addItem(potion);
        }
        // Regeneration buff
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
    }
}
