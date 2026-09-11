package com.zpkdxgames.plexonskills.ability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbilityLogoutCooldownTest {
    @Test
    void retainsAnyCooldownDeadlineThatIsStillInTheFuture() {
        long now = 1_000L;
        assertTrue(AbilityRuntime.hasFutureDeadline(new long[]{0L, now + 1L, now - 1L}, now));
    }

    @Test
    void doesNotRetainMissingOrExpiredCooldownState() {
        long now = 1_000L;
        assertFalse(AbilityRuntime.hasFutureDeadline(null, now));
        assertFalse(AbilityRuntime.hasFutureDeadline(new long[]{0L, now, now - 1L}, now));
    }
}
