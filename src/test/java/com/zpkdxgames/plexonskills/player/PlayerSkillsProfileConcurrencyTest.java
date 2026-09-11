package com.zpkdxgames.plexonskills.player;

import com.zpkdxgames.plexonskills.skill.SkillType;
import com.zpkdxgames.plexonskills.skill.XpCurve;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PlayerSkillsProfileConcurrencyTest {
    @Test
    void lockFreeReadersObserveOnlyValidXpAndLevelsDuringAuthoritativeMutation() throws Exception {
        XpCurve curve = new XpCurve(1000, 100L, 1.45);
        PlayerSkillsProfile profile = new PlayerSkillsProfile(UUID.randomUUID(), ProfileStatus.READY);
        int readers = 4;
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        try (var executor = Executors.newFixedThreadPool(readers)) {
            for (int r = 0; r < readers; r++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < 20_000; i++) {
                            long xp = profile.totalXp(SkillType.MINING);
                            int level = profile.level(SkillType.MINING);
                            assertTrue(xp >= 0L && xp <= curve.maximumTrackedXp());
                            assertTrue(level >= 1 && level <= curve.maximumLevel());
                            PlayerSkillsProfile.Snapshot snapshot = profile.snapshot();
                            assertTrue(snapshot.xp(SkillType.MINING) >= 0L);
                            assertTrue(snapshot.level(SkillType.MINING) >= 1);
                        }
                    } catch (Throwable t) { failure.compareAndSet(null, t); }
                });
            }
            start.countDown();
            for (int i = 0; i < 10_000; i++) profile.setXp(SkillType.MINING, (long) i * 317L, curve);
            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
        if (failure.get() != null) fail(failure.get());
        assertTrue(profile.revision() >= 10_000L);
    }
}
