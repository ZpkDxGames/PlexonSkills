package com.zpkdxgames.plexonskills.milestone;

import com.zpkdxgames.plexonskills.skill.SkillType;

import java.util.Objects;

/** Derived progression milestone. No claim row is required because its reward is level-derived. */
public record MilestoneDefinition(
    SkillType skill,
    String id,
    String displayName,
    String description,
    int level,
    double passiveXpBonus
) {
    public MilestoneDefinition {
        Objects.requireNonNull(skill, "skill");
        id = requireText(id, "id");
        displayName = requireText(displayName, "displayName");
        description = description == null ? "" : description.trim();
        if (level < 1) throw new IllegalArgumentException("milestone level must be >= 1 for " + id);
        if (!Double.isFinite(passiveXpBonus) || passiveXpBonus < 0.0 || passiveXpBonus > 0.25) {
            throw new IllegalArgumentException("passiveXpBonus must be 0.0..0.25 for " + id);
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }
}
