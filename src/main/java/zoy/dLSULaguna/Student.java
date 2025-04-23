package zoy.dLSULaguna;

import java.util.Optional;

import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;

public record Student(StudentManager manager, Player player) {
    public Optional<Section> getSection() {
        return Optional.ofNullable(
                player
                        .getPersistentDataContainer()
                        .get(
                                manager.sectionKey(),
                                PersistentSectionType.persistent));
    }

    public void setSection(Section section) {
        player
                .getPersistentDataContainer()
                .set(
                        manager.sectionKey(),
                        PersistentSectionType.persistent,
                        section);
    }

    public String username() {
        return player.getName();
    }
}

record PersistentSectionType() implements PersistentDataType<String, Section> {
    public static final PersistentDataType<String, Section> persistent = new PersistentSectionType();

    @Override
    public Class<Section> getComplexType() {
        return Section.class;
    }

    @Override
    public Class<String> getPrimitiveType() {
        return String.class;
    }

    @Override
    public String toPrimitive(Section complex, PersistentDataAdapterContext context) {
        return this.toString();
    }

    @Override
    public Section fromPrimitive(String primitive, PersistentDataAdapterContext context) {
        return Section.fromString(primitive).orElseThrow();
    }
}