package com.zpkdxgames.plexonskills.api;

import com.zpkdxgames.plexonskills.api.events.PlexonSkillAbilityActivateEvent;
import com.zpkdxgames.plexonskills.api.events.PlexonSkillAbilityEndEvent;
import com.zpkdxgames.plexonskills.api.events.PlexonSkillLevelUpEvent;
import com.zpkdxgames.plexonskills.api.events.PlexonSkillXpGainEvent;
import com.zpkdxgames.plexonskills.skill.SkillType;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PublicApiContractTest {
    @Test
    void keepsOneXApiSignaturesAndAddsDefaultTwoZeroViews() throws Exception {
        assertMethod("getLevel", int.class, UUID.class, SkillType.class);
        assertMethod("getTotalXp", long.class, UUID.class, SkillType.class);
        assertMethod("getTotalLevel", int.class, UUID.class);
        assertMethod("profile", Optional.class, UUID.class);
        assertMethod("addXp", boolean.class, UUID.class, SkillType.class, long.class, String.class);
        assertMethod("setXp", boolean.class, UUID.class, SkillType.class, long.class, String.class);
        assertMethod("isProfileReady", boolean.class, UUID.class);

        assertTrue(PlexonSkillsAPI.class.getMethod("ability", UUID.class, SkillType.class).isDefault());
        assertTrue(PlexonSkillsAPI.class.getMethod("milestones", SkillType.class).isDefault());
        assertTrue(PlexonSkillsAPI.class.getMethod("isMastered", UUID.class, SkillType.class).isDefault());
        assertTrue(PlexonSkillsAPI.class.getMethod("statistics", UUID.class).isDefault());
        assertEquals("2.0", PlexonSkillsAPI.API_VERSION);
    }

    @Test
    void eventContractsRetainCompactAccessorsAndExposeBeanAccessors() {
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class}, (proxy, method, args) -> defaultValue(method.getReturnType()));
        PlexonSkillXpGainEvent xp = new PlexonSkillXpGainEvent(player, SkillType.MINING, 25L, "TEST", 125L, 2, 77L);
        assertSame(player, xp.player()); assertSame(player, xp.getPlayer());
        assertEquals(SkillType.MINING, xp.skill()); assertEquals(xp.skill(), xp.getSkill());
        assertEquals(25L, xp.amount()); assertEquals(xp.amount(), xp.getAmount());
        assertEquals("TEST", xp.source()); assertEquals(xp.source(), xp.getSource());
        assertEquals(125L, xp.projectedTotalXp()); assertEquals(xp.projectedTotalXp(), xp.getProjectedTotalXp());
        assertEquals(2, xp.currentLevel()); assertEquals(xp.currentLevel(), xp.getCurrentLevel());
        assertEquals(77L, xp.transactionId()); assertEquals(xp.transactionId(), xp.getTransactionId());

        PlexonSkillLevelUpEvent level = new PlexonSkillLevelUpEvent(player, SkillType.MINING, 2, 3, 125L, "TEST");
        assertSame(level.player(), level.getPlayer()); assertEquals(level.skill(), level.getSkill());
        assertEquals(level.oldLevel(), level.getOldLevel()); assertEquals(level.newLevel(), level.getNewLevel());
        assertEquals(level.totalXp(), level.getTotalXp()); assertEquals(level.source(), level.getSource());

        PlexonSkillAbilityActivateEvent activate = new PlexonSkillAbilityActivateEvent(player, SkillType.MINING, "vein_focus");
        PlexonSkillAbilityEndEvent end = new PlexonSkillAbilityEndEvent(player, SkillType.MINING, "vein_focus");
        assertEquals(activate.abilityId(), activate.getAbilityId());
        assertEquals(end.abilityId(), end.getAbilityId());
    }

    @Test
    void immutableApiRecordsRejectInvalidDurationsAndCounts() {
        assertThrows(IllegalArgumentException.class, () -> new AbilityView("a", "A", "", SkillType.MINING, AbilityState.READY, 1, -1, 1, 0, true));
        assertThrows(IllegalArgumentException.class, () -> new SkillStatisticsView(UUID.randomUUID(), 1, 1, SkillType.MINING, 1, 2, 1, 0));
        MilestoneView milestone = new MilestoneView("m", "M", "", SkillType.MINING, 10, 0.05);
        assertEquals(10, milestone.level());
        assertEquals(List.of(milestone), List.copyOf(List.of(milestone)));
    }

    private static void assertMethod(String name, Class<?> returnType, Class<?>... parameters) throws Exception {
        Method method = PlexonSkillsAPI.class.getMethod(name, parameters);
        assertEquals(returnType, method.getReturnType(), name);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }
}
