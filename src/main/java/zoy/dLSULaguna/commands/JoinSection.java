package zoy.dLSULaguna.commands;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import zoy.dLSULaguna.DLSULaguna;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public class JoinSection implements CommandExecutor {

    private final DLSULaguna plugin;
    private final NamespacedKey sectionKey;
    // Constructor that accepts the main plugin instance.
    public JoinSection(DLSULaguna plugin) {
        this.plugin = plugin;
        this.sectionKey = new NamespacedKey(plugin, "section_name");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        PersistentDataContainer playerData = player.getPersistentDataContainer();
        String sectionName = args[0].toUpperCase(Locale.ROOT);
        if(sectionName.length() != 1){
            player.sendMessage("Please specify a section letter, /joinsection <section letter>");
        }else{
            player.sendMessage("You joined the section " + sectionName +" !");
            playerData.set(sectionKey, PersistentDataType.STRING, sectionName);
            return true;
        }
        return false;
    }
}
