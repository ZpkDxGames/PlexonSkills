package com.zpkdxgames.plexonskills.milestone;

import com.zpkdxgames.plexonskills.skill.SkillPassive;
import com.zpkdxgames.plexonskills.skill.SkillType;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Immutable level-derived milestone registry. */
public final class MilestoneRegistry {
    private final boolean enabled;
    private final Map<SkillType, List<MilestoneDefinition>> milestones;

    private MilestoneRegistry(boolean enabled, Map<SkillType, List<MilestoneDefinition>> milestones) {
        this.enabled = enabled;
        EnumMap<SkillType, List<MilestoneDefinition>> copy = new EnumMap<>(SkillType.class);
        milestones.forEach((skill, values) -> copy.put(skill, List.copyOf(values)));
        this.milestones = Collections.unmodifiableMap(copy);
    }

    public static MilestoneRegistry load(File file, int maximumLevel) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        boolean enabled = yaml.getBoolean("milestones.enabled", true);
        Set<SkillType> disabled = EnumSet.noneOf(SkillType.class);
        for (String raw : yaml.getStringList("milestones.disabled-skills")) SkillType.parse(raw).ifPresent(disabled::add);

        List<Map<?, ?>> templates = new ArrayList<>(yaml.getMapList("milestones.defaults"));
        EnumMap<SkillType, List<MilestoneDefinition>> result = new EnumMap<>(SkillType.class);
        for (SkillType skill : SkillType.values()) {
            if (disabled.contains(skill)) { result.put(skill, List.of()); continue; }
            List<MilestoneDefinition> values = new ArrayList<>();
            double totalBonus = 0.0;
            int previousLevel = 0;
            for (Map<?, ?> template : templates) {
                String id = text(template, "id", "milestone_" + (values.size() + 1));
                double progress = number(template, "progress", -1.0);
                if (!(progress > 0.0 && progress <= 1.0)) throw new IllegalArgumentException("milestone progress must be > 0 and <= 1 for " + id);
                int level = scaledLevel(maximumLevel, progress);
                if (level <= previousLevel) throw new IllegalArgumentException("milestone levels must be strictly increasing after scaling; duplicate at " + id);
                previousLevel = level;
                String name = text(template, "name", "{skill} Milestone").replace("{skill}", skill.displayName());
                String description = text(template, "description", "A meaningful progression checkpoint.").replace("{skill}", skill.displayName());
                double bonus = number(template, "passive-xp-bonus", 0.0);
                MilestoneDefinition definition = new MilestoneDefinition(skill, id, name, description, level, bonus);
                values.add(definition);
                totalBonus += bonus;
            }
            if (totalBonus > 0.50 + 1.0e-9) throw new IllegalArgumentException("cumulative passive XP bonus exceeds 50% for " + skill);
            result.put(skill, values);
        }
        return new MilestoneRegistry(enabled, result);
    }

    public boolean enabled() { return enabled; }
    public List<MilestoneDefinition> milestones(SkillType skill) { return milestones.getOrDefault(skill, List.of()); }

    public Optional<MilestoneDefinition> next(SkillType skill, int level) {
        if (!enabled) return Optional.empty();
        return milestones(skill).stream().filter(m -> m.level() > level).findFirst();
    }

    public Optional<MilestoneDefinition> highestReached(SkillType skill, int level) {
        if (!enabled) return Optional.empty();
        MilestoneDefinition result = null;
        for (MilestoneDefinition milestone : milestones(skill)) if (milestone.level() <= level) result = milestone;
        return Optional.ofNullable(result);
    }

    public List<MilestoneDefinition> crossed(SkillType skill, int oldLevel, int newLevel) {
        if (!enabled || newLevel <= oldLevel) return List.of();
        return milestones(skill).stream().filter(m -> m.level() > oldLevel && m.level() <= newLevel).toList();
    }

    /** Derived, restart-safe skill-specific passive multiplier. No claim state is persisted. */
    public double passiveMultiplier(SkillType skill, int level) {
        if (!enabled) return 1.0;
        double baseBonus = 0.0;
        for (MilestoneDefinition milestone : milestones(skill)) if (level >= milestone.level()) baseBonus += milestone.passiveXpBonus();
        double baseMultiplier = 1.0 + Math.min(0.50, Math.max(0.0, baseBonus));
        return SkillPassive.forSkill(skill).apply(baseMultiplier);
    }

    static int scaledLevel(int maximumLevel, double progress) {
        int max = Math.max(1, maximumLevel);
        if (progress >= 1.0) return max;
        return Math.max(1, Math.min(max, 1 + (int) Math.floor(max * progress)));
    }

    private static String text(Map<?, ?> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null || value.toString().isBlank() ? fallback : value.toString().trim();
    }

    private static double number(Map<?, ?> map, String key, double fallback) {
        Object value = map.get(key);
        if (value == null) return fallback;
        if (value instanceof Number number) return number.doubleValue();
        try { return Double.parseDouble(value.toString().trim()); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Invalid number for milestone " + key + ": " + value); }
    }
}
