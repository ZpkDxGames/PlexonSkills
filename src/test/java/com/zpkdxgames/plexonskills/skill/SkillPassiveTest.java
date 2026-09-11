package com.zpkdxgames.plexonskills.skill;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillPassiveTest {
    @Test
    void allThirteenSkillsHaveDistinctPassiveIdentities() {
        Set<String> ids = Arrays.stream(SkillType.values()).map(SkillPassive::forSkill).map(SkillPassive::id).collect(Collectors.toSet());
        Set<String> names = Arrays.stream(SkillType.values()).map(SkillPassive::forSkill).map(SkillPassive::displayName).collect(Collectors.toSet());
        assertEquals(SkillType.values().length, ids.size());
        assertEquals(SkillType.values().length, names.size());
    }

    @Test
    void coefficientsActuallyDifferentiateSkillPassives() {
        Set<Double> coefficients = Arrays.stream(SkillType.values()).map(SkillPassive::forSkill).map(SkillPassive::milestoneCoefficient).collect(Collectors.toSet());
        assertTrue(coefficients.size() >= 5);
    }

    @Test
    void passiveApplicationIsDeterministicAndBounded() {
        SkillPassive mining = SkillPassive.forSkill(SkillType.MINING);
        double first = mining.apply(1.15);
        double second = mining.apply(1.15);
        assertEquals(first, second);
        assertTrue(first >= 1.0 && first <= 1.5);
        assertEquals(1.0, mining.apply(0.5));
        assertEquals(1.5, mining.apply(10.0));
    }
}
