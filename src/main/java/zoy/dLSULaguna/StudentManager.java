package zoy.dLSULaguna;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

public record StudentManager(DLSULagunaPlugin plugin, NamespacedKey sectionKey) {
    StudentManager(DLSULagunaPlugin plugin) {
        this(plugin, new NamespacedKey(plugin, "section_name"));
    }

    Student fromPlayer(Player player) {
        return new Student(this, player);
    }
}
