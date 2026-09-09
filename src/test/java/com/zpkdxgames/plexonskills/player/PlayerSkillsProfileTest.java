package com.zpkdxgames.plexonskills.player;

import com.zpkdxgames.plexonskills.skill.SkillType;
import com.zpkdxgames.plexonskills.skill.XpCurve;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerSkillsProfileTest {
    @Test void cumulativeXpIsCanonicalAndLevelIsDerived() {
        XpCurve curve = new XpCurve(100, 100, 1.0);
        PlayerSkillsProfile profile = new PlayerSkillsProfile(UUID.randomUUID(), ProfileStatus.READY);
        var mutation = profile.addXp(SkillType.MINING, 100, curve);
        assertTrue(mutation.leveledUp());
        assertEquals(2, profile.level(SkillType.MINING));
        assertEquals(100, profile.totalXp(SkillType.MINING));
    }

    @Test void maximumLevelStopsXpGrowth() {
        XpCurve curve = new XpCurve(3, 10, 1.0);
        PlayerSkillsProfile profile = new PlayerSkillsProfile(UUID.randomUUID(), ProfileStatus.READY);
        profile.setXp(SkillType.MINING, Long.MAX_VALUE, curve);
        long capped = profile.totalXp(SkillType.MINING);
        assertEquals(curve.maximumTrackedXp(), capped);
        assertEquals(3, profile.level(SkillType.MINING));
        assertFalse(profile.addXp(SkillType.MINING, 1000, curve).changed());
    }

    @Test void snapshotCannotMutateAuthoritativeArrays() {
        XpCurve curve = new XpCurve(10, 10, 1.0);
        PlayerSkillsProfile profile = new PlayerSkillsProfile(UUID.randomUUID(), ProfileStatus.READY);
        profile.setXp(SkillType.FISHING, 20, curve);
        var snapshot = profile.snapshot();
        long[] copy = snapshot.totalXp();
        copy[SkillType.FISHING.ordinal()] = 999999;
        assertEquals(20, profile.totalXp(SkillType.FISHING));
    }
}
