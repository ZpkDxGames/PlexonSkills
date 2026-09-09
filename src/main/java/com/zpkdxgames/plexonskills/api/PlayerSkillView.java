package com.zpkdxgames.plexonskills.api;

import java.util.List;
import java.util.UUID;

public record PlayerSkillView(UUID playerId, List<SkillProgressView> skills, int totalLevel, long revision) {
    public PlayerSkillView { skills = List.copyOf(skills); }
}
