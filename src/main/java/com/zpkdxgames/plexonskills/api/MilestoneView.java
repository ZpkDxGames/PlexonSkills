package com.zpkdxgames.plexonskills.api;

import com.zpkdxgames.plexonskills.skill.SkillType;

import java.util.Objects;

/** Immutable configured milestone definition exposed by API 2.0. */
public record MilestoneView(
    String id,
    String displayName,
    String description,
    SkillType skill,
    int level,
    double passiveXpBonus
) {
    public MilestoneView {
        id = Objects.requireNonNull(id, "id");
        displayName = Objects.requireNonNull(displayName, "displayName");
        description = description == null ? "" : description;
        skill = Objects.requireNonNull(skill, "skill");
        if (level < 1) throw new IllegalArgumentException("level must be >= 1");
        if (!Double.isFinite(passiveXpBonus) || passiveXpBonus < 0.0) throw new IllegalArgumentException("passiveXpBonus must be finite and non-negative");
    }
}
