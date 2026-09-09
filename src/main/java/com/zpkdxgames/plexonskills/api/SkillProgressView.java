package com.zpkdxgames.plexonskills.api;

import com.zpkdxgames.plexonskills.skill.SkillType;

public record SkillProgressView(SkillType skill, long totalXp, int level, long xpForNextLevel, double progress) {}
