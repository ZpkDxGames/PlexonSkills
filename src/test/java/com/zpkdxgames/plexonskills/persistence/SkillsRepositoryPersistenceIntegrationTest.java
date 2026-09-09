package com.zpkdxgames.plexonskills.persistence;

import com.zpkdxgames.plexoncore.persistence.SqliteService;
import com.zpkdxgames.plexoncore.scheduler.CoreScheduler;
import com.zpkdxgames.plexonskills.player.PlayerSkillsProfile;
import com.zpkdxgames.plexonskills.player.ProfileStatus;
import com.zpkdxgames.plexonskills.skill.SkillType;
import com.zpkdxgames.plexonskills.skill.XpCurve;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class SkillsRepositoryPersistenceIntegrationTest {
    @TempDir Path temp;

    @Test
    void roundTripsProfilesAndUsesDeterministicLeaderboardOrdering() throws Exception {
        Plugin plugin = pluginStub();
        XpCurve curve = new XpCurve(1000, 100L, 1.45);
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");

        try (CoreScheduler scheduler = new CoreScheduler(plugin, 1, 64, 1, 64);
             SqliteService sqlite = new SqliteService(scheduler)) {
            SkillsRepository repository = new SkillsRepository(sqlite.open(temp.resolve("skills.db")));
            assertEquals(2, repository.initialize(curve).get());

            PlayerSkillsProfile a = new PlayerSkillsProfile(first, ProfileStatus.READY);
            PlayerSkillsProfile b = new PlayerSkillsProfile(second, ProfileStatus.READY);
            a.setXp(SkillType.MINING, 12_345L, curve);
            a.setXp(SkillType.FARMING, 2_500L, curve);
            b.setXp(SkillType.MINING, 12_345L, curve);

            repository.save(List.of(
                new SkillsRepository.NamedSnapshot("First", a.snapshot()),
                new SkillsRepository.NamedSnapshot("Second", b.snapshot())
            )).get();

            var loaded = repository.load(first, "First", curve).get();
            assertEquals(12_345L, loaded.totalXp().get(SkillType.MINING));
            assertEquals(2_500L, loaded.totalXp().get(SkillType.FARMING));

            var top = repository.top(SkillType.MINING, 10).get();
            assertEquals(2, top.size());
            assertEquals(first, top.get(0).playerId(), "UUID is the deterministic tiebreak for equal XP");
            assertEquals(second, top.get(1).playerId());
            assertEquals(1, repository.rank(SkillType.MINING, first).get());
            assertEquals(1, repository.rank(SkillType.MINING, second).get());
            repository.close();
        }
    }

    private static Plugin pluginStub() {
        return (Plugin) Proxy.newProxyInstance(
            Plugin.class.getClassLoader(), new Class<?>[]{Plugin.class},
            (proxy, method, args) -> {
                if (method.getName().equals("getLogger")) return Logger.getLogger("PlexonSkillsPersistenceTest");
                if (method.getName().equals("getName")) return "PlexonSkillsPersistenceTest";
                Class<?> type = method.getReturnType();
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
            });
    }
}
