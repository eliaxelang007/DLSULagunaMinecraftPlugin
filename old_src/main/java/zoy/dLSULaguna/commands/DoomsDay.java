package zoy.dLSULaguna.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import zoy.dLSULaguna.utils.SectionSerializable;

import java.io.File;

public class DoomsDay implements CommandExecutor {
    private final Plugin plugin;

    public DoomsDay(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Strong permission check
        if (!sender.hasPermission("dlsulaguna.doomsday")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        // Require confirmation
        if (args.length != 1 || !args[0].equalsIgnoreCase("confirm")) {
            sender.sendMessage(ChatColor.RED + "⚠ WARNING: This will wipe ALL plugin data and player section data.");
            sender.sendMessage(ChatColor.RED + "To proceed, type: §c/doomsday confirm");
            return true;
        }

        // Delete plugin data folder
        File dataFolder = plugin.getDataFolder();
        if (dataFolder.exists()) {
            sender.sendMessage(ChatColor.YELLOW + "Deleting plugin data...");
            boolean deleted = deleteFolderRecursively(dataFolder);
            if (deleted) {
                sender.sendMessage(ChatColor.RED + "☠ All plugin data has been deleted.");
                plugin.getLogger().warning("☠ Doomsday was executed by " + sender.getName() + " ☠");
            } else {
                sender.sendMessage(ChatColor.RED + "⚠ Failed to delete some files. Manual cleanup may be required.");
            }
        } else {
            sender.sendMessage(ChatColor.GRAY + "Plugin data folder does not exist. Nothing to delete.");
        }

        // Wipe player persistent data
        NamespacedKey sectionKey = new NamespacedKey(plugin, "section_name"); // Does this also need to be wrapped in
                                                                              // the section type?
        for (Player player : Bukkit.getOnlinePlayers()) {
            PersistentDataContainer container = player.getPersistentDataContainer();
            if (container.has(sectionKey, SectionSerializable.persistent)) {
                container.remove(sectionKey);
            }
            player.sendMessage(ChatColor.DARK_RED + "☠ Your section data has been wiped by an admin ☠");
        }

        // Reload server
        sender.sendMessage(ChatColor.YELLOW + "Reloading server to apply changes...");
        Bukkit.getScheduler().runTaskLater(plugin, Bukkit::reload, 40L); // 2 seconds delay

        return true;
    }

    private boolean deleteFolderRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!deleteFolderRecursively(f)) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }
}
