package zoy.dLSULaguna.utils;

import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;

public record SectionSerializable() implements PersistentDataType<String, Section> {
    public static final PersistentDataType<String, Section> persistent = new SectionSerializable();

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
