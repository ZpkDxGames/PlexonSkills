package com.zpkdxgames.plexonskills.placeholder;

import com.zpkdxgames.plexonskills.skill.SkillType;

import java.util.Locale;

/** Pure PlaceholderAPI contract resolver backed by already-loaded runtime values. */
public final class SkillsPlaceholderResolver {
    private SkillsPlaceholderResolver() { }

    public interface Values {
        boolean ready();
        int totalLevel();
        long totalXp();
        String highestSkill();
        int highestSkillLevel();
        int level(SkillType skill);
        long xp(SkillType skill);
        long xpRequired(SkillType skill);
        double progressPercent(SkillType skill);
        String stage(SkillType skill);
        String nextMilestone(SkillType skill);
        double masteryPercent(SkillType skill);
        double passiveBonusPercent(SkillType skill);
        String abilityState(SkillType skill);
        long abilityCooldownSeconds(SkillType skill);
    }

    public static String resolve(Values values, String params) {
        if (values == null || params == null || params.isBlank()) return "";
        if (!values.ready()) return "0";

        String key = params.trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "total_level" -> Integer.toString(values.totalLevel());
            case "total_xp" -> Long.toString(values.totalXp());
            case "highest_skill" -> values.highestSkill();
            case "highest_skill_level" -> Integer.toString(values.highestSkillLevel());
            default -> resolveSkill(values, key);
        };
    }

    private static String resolveSkill(Values values, String key) {
        for (SkillType skill : SkillType.values()) {
            String prefix = skill.id() + "_";
            if (!key.startsWith(prefix)) continue;
            String suffix = key.substring(prefix.length());
            return switch (suffix) {
                case "level" -> Integer.toString(values.level(skill));
                case "xp" -> Long.toString(values.xp(skill));
                case "xp_required", "xp_next" -> Long.toString(values.xpRequired(skill));
                case "progress" -> oneDecimal(values.progressPercent(skill));
                case "stage" -> values.stage(skill);
                case "next_milestone" -> values.nextMilestone(skill);
                case "mastery" -> oneDecimal(values.masteryPercent(skill));
                case "mastered" -> Boolean.toString(values.masteryPercent(skill) >= 100.0);
                case "passive_bonus" -> oneDecimal(values.passiveBonusPercent(skill));
                case "ability_state" -> values.abilityState(skill);
                case "ability_cooldown" -> Long.toString(Math.max(0L, values.abilityCooldownSeconds(skill)));
                // Kept for compatibility. Accurate ranks remain async/cache-backed and are not queried here.
                case "rank" -> "0";
                default -> "";
            };
        }
        return "";
    }

    private static String oneDecimal(double value) {
        double safe = Double.isFinite(value) ? value : 0.0;
        return String.format(Locale.ROOT, "%.1f", safe);
    }
}
