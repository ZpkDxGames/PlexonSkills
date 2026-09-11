package com.zpkdxgames.plexonskills.api;

import com.zpkdxgames.plexonskills.skill.SkillType;

import java.util.Objects;

/** Immutable public ability snapshot. Times are durations in milliseconds, never absolute timestamps. */
public record AbilityView(
    String id,
    String displayName,
    String description,
    SkillType skill,
    AbilityState state,
    int unlockLevel,
    long durationMillis,
    long cooldownMillis,
    long remainingMillis,
    boolean canActivate
) {
    public AbilityView {
        id = Objects.requireNonNull(id, "id");
        displayName = Objects.requireNonNull(displayName, "displayName");
        description = description == null ? "" : description;
        skill = Objects.requireNonNull(skill, "skill");
        state = Objects.requireNonNull(state, "state");
        if (unlockLevel < 1) throw new IllegalArgumentException("unlockLevel must be >= 1");
        if (durationMillis < 0L || cooldownMillis < 0L || remainingMillis < 0L) throw new IllegalArgumentException("ability durations must be non-negative");
    }
}
