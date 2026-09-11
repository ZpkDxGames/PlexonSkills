package com.zpkdxgames.plexonskills.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkillStageTest {
    @Test void stageBoundariesScaleWithConfiguredMaximum() {
        assertEquals(SkillStage.NOVICE, SkillStage.forLevel(1, 1000));
        assertEquals(SkillStage.ADEPT, SkillStage.forLevel(101, 1000));
        assertEquals(SkillStage.EXPERT, SkillStage.forLevel(251, 1000));
        assertEquals(SkillStage.MASTER, SkillStage.forLevel(501, 1000));
        assertEquals(SkillStage.ASCENDANT, SkillStage.forLevel(751, 1000));
        assertEquals(SkillStage.MASTERY, SkillStage.forLevel(901, 1000));
        assertEquals(SkillStage.MASTERY, SkillStage.forLevel(1000, 1000));
    }

    @Test void unlockLevelsNeverExceedMaximum() {
        for (SkillStage stage : SkillStage.values()) {
            int unlock = stage.unlockLevel(12);
            assertEquals(true, unlock >= 1 && unlock <= 12);
        }
    }
}
