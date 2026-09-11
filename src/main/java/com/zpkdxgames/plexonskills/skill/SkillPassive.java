package com.zpkdxgames.plexonskills.skill;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * First-party passive identity for each skill. Passives deliberately modify only valid PlexonSkills
 * XP so they do not compete with economy, drops, combat or item plugins.
 */
public record SkillPassive(
    SkillType skill,
    String id,
    String displayName,
    String description,
    double milestoneCoefficient
) {
    private static final Map<SkillType, SkillPassive> DEFAULTS = defaults();

    public SkillPassive {
        Objects.requireNonNull(skill, "skill");
        if (id == null || id.isBlank()) throw new IllegalArgumentException("passive id must not be blank");
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("passive displayName must not be blank");
        description = description == null ? "" : description.trim();
        if (!Double.isFinite(milestoneCoefficient) || milestoneCoefficient < 0.50 || milestoneCoefficient > 1.50) {
            throw new IllegalArgumentException("milestoneCoefficient must be 0.50..1.50");
        }
    }

    public static SkillPassive forSkill(SkillType skill) {
        return Objects.requireNonNull(DEFAULTS.get(skill), "Missing passive definition for " + skill);
    }

    /** Applies this skill's identity coefficient to a derived milestone multiplier, capped at +50%. */
    public double apply(double milestoneMultiplier) {
        double baseBonus = Math.max(0.0, milestoneMultiplier - 1.0);
        double adjusted = Math.min(0.50, baseBonus * milestoneCoefficient);
        return 1.0 + adjusted;
    }

    public double bonusPercent(double milestoneMultiplier) {
        return (apply(milestoneMultiplier) - 1.0) * 100.0;
    }

    private static Map<SkillType, SkillPassive> defaults() {
        EnumMap<SkillType, SkillPassive> map = new EnumMap<>(SkillType.class);
        put(map, SkillType.MINING, "ore_insight", "Ore Insight", "Refined ore knowledge improves valid Mining XP efficiency.", 1.10);
        put(map, SkillType.WOODCUTTING, "timber_rhythm", "Timber Rhythm", "Consistent harvesting technique improves valid Woodcutting XP efficiency.", 0.95);
        put(map, SkillType.EXCAVATION, "ground_sense", "Ground Sense", "Terrain familiarity improves valid Excavation XP efficiency.", 1.00);
        put(map, SkillType.FARMING, "harvest_wisdom", "Harvest Wisdom", "Mature harvest experience improves valid Farming XP efficiency.", 1.05);
        put(map, SkillType.FISHING, "anglers_patience", "Angler's Patience", "Successful catches build a steady Fishing XP efficiency bonus.", 0.90);
        put(map, SkillType.SWORDS, "blade_discipline", "Blade Discipline", "Disciplined valid combat improves Swords XP efficiency.", 0.85);
        put(map, SkillType.AXES, "cleaving_momentum", "Cleaving Momentum", "Controlled valid combat improves Axes XP efficiency.", 0.85);
        put(map, SkillType.ARCHERY, "marksman_instinct", "Marksman Instinct", "Precision combat experience improves Archery XP efficiency.", 0.90);
        put(map, SkillType.UNARMED, "inner_discipline", "Inner Discipline", "Unarmed mastery improves valid Unarmed XP efficiency.", 0.85);
        put(map, SkillType.TAMING, "bonded_instinct", "Bonded Instinct", "Companion combat experience improves valid Taming XP efficiency.", 0.90);
        put(map, SkillType.ACROBATICS, "surefooted", "Surefooted", "Surviving meaningful falls improves Acrobatics XP efficiency.", 0.80);
        put(map, SkillType.REPAIR, "craftsmans_insight", "Craftsman's Insight", "Repair experience improves valid Repair XP efficiency.", 1.00);
        put(map, SkillType.ALCHEMY, "brewers_intuition", "Brewer's Intuition", "Completed brews improve valid Alchemy XP efficiency.", 1.05);
        return Map.copyOf(map);
    }

    private static void put(Map<SkillType, SkillPassive> map, SkillType skill, String id, String name, String description, double coefficient) {
        map.put(skill, new SkillPassive(skill, id, name, description, coefficient));
    }
}
