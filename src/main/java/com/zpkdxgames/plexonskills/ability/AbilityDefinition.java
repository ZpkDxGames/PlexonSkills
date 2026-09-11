package com.zpkdxgames.plexonskills.ability;

import com.zpkdxgames.plexonskills.skill.SkillType;

import java.util.Objects;

/** Immutable, validated definition for one skill's active progression ability. */
public record AbilityDefinition(
    SkillType skill,
    String id,
    String displayName,
    String description,
    boolean enabled,
    int unlockLevel,
    long durationMillis,
    long cooldownMillis,
    double xpMultiplier
) {
    public AbilityDefinition {
        Objects.requireNonNull(skill, "skill");
        id = requireText(id, "id");
        displayName = requireText(displayName, "displayName");
        description = description == null ? "" : description.trim();
        if (unlockLevel < 1) throw new IllegalArgumentException("unlockLevel must be >= 1 for " + id);
        if (durationMillis < 1_000L || durationMillis > 600_000L) throw new IllegalArgumentException("duration must be 1..600 seconds for " + id);
        if (cooldownMillis < durationMillis || cooldownMillis > 86_400_000L) throw new IllegalArgumentException("cooldown must be >= duration and <= 24h for " + id);
        if (!Double.isFinite(xpMultiplier) || xpMultiplier < 1.0 || xpMultiplier > 3.0) throw new IllegalArgumentException("xpMultiplier must be 1.0..3.0 for " + id);
    }

    public long durationSeconds() { return durationMillis / 1_000L; }
    public long cooldownSeconds() { return cooldownMillis / 1_000L; }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }
}
