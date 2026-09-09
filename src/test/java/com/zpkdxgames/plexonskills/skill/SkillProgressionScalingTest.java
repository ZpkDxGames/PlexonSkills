package com.zpkdxgames.plexonskills.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkillProgressionScalingTest {
    @Test void abilityScalingIsBoundedAndNeverReducesXp() {
        assertEquals(100L, SkillProgressionService.scale(100L, 1.0));
        assertEquals(125L, SkillProgressionService.scale(100L, 1.25));
        assertEquals(0L, SkillProgressionService.scale(0L, 2.0));
        assertEquals(Long.MAX_VALUE, SkillProgressionService.scale(Long.MAX_VALUE, 1.25));
    }
}
