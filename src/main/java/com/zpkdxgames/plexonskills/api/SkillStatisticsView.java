package com.zpkdxgames.plexonskills.api;

import com.zpkdxgames.plexonskills.skill.SkillType;

import java.util.UUID;

/** Immutable, loaded-profile-only aggregate statistics. */
public record SkillStatisticsView(
    UUID playerId,
    int totalLevel,
    long totalXp,
    SkillType highestSkill,
    int highestLevel,
    int masteredSkills,
    int enabledSkills,
    long revision
) {
    public SkillStatisticsView {
        if (playerId == null) throw new IllegalArgumentException("playerId must be present");
        if (totalLevel < 0 || totalXp < 0L || highestLevel < 0 || masteredSkills < 0 || enabledSkills < 0 || revision < 0L) {
            throw new IllegalArgumentException("statistics values must be non-negative");
        }
        if (masteredSkills > enabledSkills) throw new IllegalArgumentException("masteredSkills cannot exceed enabledSkills");
    }
}
