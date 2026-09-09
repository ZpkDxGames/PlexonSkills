package com.zpkdxgames.plexonskills.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XpCurveTest {
    @Test void levelBoundariesAreDeterministic() {
        XpCurve curve = new XpCurve(1000, 100, 1.45);
        assertEquals(1, curve.levelForXp(0));
        assertEquals(1, curve.levelForXp(99));
        assertEquals(2, curve.levelForXp(100));
        assertEquals(100, curve.cumulativeForLevel(2));
        assertTrue(curve.cumulativeForLevel(100) > curve.cumulativeForLevel(99));
    }

    @Test void clampsAtMaximumLevelWithoutOverflow() {
        XpCurve curve = new XpCurve(1000, 100, 1.45);
        assertEquals(1000, curve.levelForXp(Long.MAX_VALUE));
        assertEquals(curve.maximumTrackedXp(), curve.clampXp(Long.MAX_VALUE));
        assertEquals(1.0, curve.progressWithinLevel(Long.MAX_VALUE));
    }

    @Test void bulkXpCanCrossManyLevels() {
        XpCurve curve = new XpCurve(100, 100, 1.2);
        long xp = curve.cumulativeForLevel(50) + 1;
        assertEquals(50, curve.levelForXp(xp));
    }
}
