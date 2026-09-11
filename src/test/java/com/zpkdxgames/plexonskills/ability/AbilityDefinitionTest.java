package com.zpkdxgames.plexonskills.ability;

import com.zpkdxgames.plexonskills.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AbilityDefinitionTest {
    @Test void validatesSafeBounds() {
        AbilityDefinition definition = new AbilityDefinition(
            SkillType.MINING, "ore_focus", "Ore Focus", "Bonus Mining XP", true,
            25, 15_000L, 90_000L, 1.25);
        assertEquals(25, definition.unlockLevel());
        assertEquals(1.25, definition.xpMultiplier());
        assertThrows(IllegalArgumentException.class, () -> new AbilityDefinition(
            SkillType.MINING, "bad", "Bad", "", true, 1, 15_000L, 10_000L, 1.2));
        assertThrows(IllegalArgumentException.class, () -> new AbilityDefinition(
            SkillType.MINING, "bad", "Bad", "", true, 1, 15_000L, 90_000L, 4.0));
    }
}
