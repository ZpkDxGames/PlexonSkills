package com.zpkdxgames.plexonskills.milestone;

import com.zpkdxgames.plexonskills.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MilestoneRegistryTest {
    @Test void scaledMilestonesRespectMaximumLevel() {
        assertEquals(101, MilestoneRegistry.scaledLevel(1000, 0.10));
        assertEquals(251, MilestoneRegistry.scaledLevel(1000, 0.25));
        assertEquals(1000, MilestoneRegistry.scaledLevel(1000, 1.0));
        assertEquals(2, MilestoneRegistry.scaledLevel(10, 0.10));
    }

    @Test void passiveBonusIsBoundedPerMilestone() {
        assertDoesNotThrow(() -> new MilestoneDefinition(SkillType.MINING, "expert", "Mining Expert", "", 500, 0.04));
        assertThrows(IllegalArgumentException.class, () -> new MilestoneDefinition(SkillType.MINING, "bad", "Bad", "", 10, 0.30));
    }
}
