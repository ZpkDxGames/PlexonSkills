package com.zpkdxgames.plexonskills.ability;

import com.zpkdxgames.plexonskills.skill.SkillType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Immutable ability registry loaded and validated as part of the atomic runtime settings snapshot. */
public final class AbilityRegistry {
    private final boolean enabled;
    private final Map<SkillType, AbilityDefinition> definitions;

    private AbilityRegistry(boolean enabled, Map<SkillType, AbilityDefinition> definitions) {
        this.enabled = enabled;
        this.definitions = Collections.unmodifiableMap(new EnumMap<>(definitions));
    }

    public static AbilityRegistry load(File file, int maximumLevel) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        boolean globallyEnabled = yaml.getBoolean("abilities.enabled", true);
        ConfigurationSection root = yaml.getConfigurationSection("abilities.skills");
        EnumMap<SkillType, AbilityDefinition> result = new EnumMap<>(SkillType.class);
        if (root == null) return new AbilityRegistry(globallyEnabled, result);

        for (SkillType skill : SkillType.values()) {
            ConfigurationSection section = root.getConfigurationSection(skill.name());
            if (section == null) continue;
            String fallbackId = skill.id() + "_focus";
            String id = section.getString("id", fallbackId);
            String name = section.getString("name", skill.displayName() + " Focus");
            String description = section.getString("description", "Temporarily increases " + skill.displayName() + " XP gains.");
            boolean enabled = section.getBoolean("enabled", true);
            int unlockLevel = section.getInt("unlock-level", Math.min(25, Math.max(1, maximumLevel)));
            if (unlockLevel > maximumLevel) throw new IllegalArgumentException("Ability unlock level exceeds maximum level for " + skill);
            long durationMillis = Math.multiplyExact(section.getLong("duration-seconds", 12L), 1_000L);
            long cooldownMillis = Math.multiplyExact(section.getLong("cooldown-seconds", 90L), 1_000L);
            double multiplier = section.getDouble("xp-multiplier", 1.20);
            AbilityDefinition definition = new AbilityDefinition(skill, id, name, description, enabled, unlockLevel, durationMillis, cooldownMillis, multiplier);
            result.put(skill, definition);
        }
        return new AbilityRegistry(globallyEnabled, result);
    }

    public boolean enabled() { return enabled; }
    public AbilityDefinition definition(SkillType skill) { return definitions.get(skill); }
    public Map<SkillType, AbilityDefinition> definitions() { return definitions; }
    public boolean enabled(SkillType skill) {
        AbilityDefinition definition = definitions.get(skill);
        return enabled && definition != null && definition.enabled();
    }
}
