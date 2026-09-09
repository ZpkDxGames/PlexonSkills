package com.zpkdxgames.plexonskills.skill;

import org.bukkit.Material;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public record SkillDefinition(
    SkillType type,
    boolean enabled,
    boolean naturalOriginRequired,
    boolean requireMatureCrop,
    long defaultXp,
    double damageXp,
    Map<Material, Long> materialXp
) {
    public SkillDefinition {
        Objects.requireNonNull(type, "type");
        defaultXp = Math.max(0L, defaultXp);
        damageXp = Math.max(0.0, damageXp);
        EnumMap<Material, Long> copy = new EnumMap<>(Material.class);
        if (materialXp != null) copy.putAll(materialXp);
        materialXp = Collections.unmodifiableMap(copy);
    }

    public long xpFor(Material material) { return materialXp.getOrDefault(material, defaultXp); }
}
