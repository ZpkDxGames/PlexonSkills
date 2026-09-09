package com.zpkdxgames.plexonskills.persistence;

import com.zpkdxgames.plexoncore.persistence.SqliteService;
import com.zpkdxgames.plexoncore.scheduler.CoreScheduler;
import com.zpkdxgames.plexonskills.skill.XpCurve;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class SkillsRepositoryMigrationIntegrationTest {
    @TempDir Path temp;

    @Test
    void migratesLegacySchemaWithoutChangingCanonicalXp() throws Exception {
        Path databasePath = temp.resolve("skills.db");
        Class.forName("org.sqlite.JDBC");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath); var st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys=ON");
            st.execute("CREATE TABLE players(player_uuid TEXT PRIMARY KEY,last_name TEXT,updated_at INTEGER NOT NULL)");
            st.execute("CREATE TABLE skill_progress(player_uuid TEXT NOT NULL,skill TEXT NOT NULL,total_xp INTEGER NOT NULL,level INTEGER NOT NULL,updated_at INTEGER NOT NULL,PRIMARY KEY(player_uuid,skill),FOREIGN KEY(player_uuid) REFERENCES players(player_uuid) ON DELETE CASCADE)");
            st.execute("CREATE TABLE migration_meta(key TEXT PRIMARY KEY,value TEXT NOT NULL)");
            st.execute("INSERT INTO players VALUES('00000000-0000-0000-0000-000000000001','LegacyOne',1)");
            st.execute("INSERT INTO players VALUES('00000000-0000-0000-0000-000000000002','LegacyTwo',1)");
            st.execute("INSERT INTO skill_progress VALUES('00000000-0000-0000-0000-000000000001','MINING',12345,999,1)");
            st.execute("INSERT INTO skill_progress VALUES('00000000-0000-0000-0000-000000000001','FARMING',67890,999,1)");
            st.execute("INSERT INTO skill_progress VALUES('00000000-0000-0000-0000-000000000002','FISHING',13579,999,1)");
        }

        Plugin plugin = pluginStub();
        try (CoreScheduler scheduler = new CoreScheduler(plugin, 1, 64, 1, 64);
             SqliteService sqlite = new SqliteService(scheduler)) {
            SkillsRepository repository = new SkillsRepository(sqlite.open(databasePath));
            var before = repository.inspectSchema().get();
            assertEquals(SkillsRepository.SchemaState.LEGACY_1_X, before.state());
            assertEquals(2, before.fingerprint().playerRows());
            assertEquals(3, before.fingerprint().progressRows());

            Path backup = repository.backup(temp.resolve("backups/pre-2.0.db"));
            assertTrue(Files.isRegularFile(backup));
            assertTrue(Files.size(backup) > 0L);

            XpCurve curve = new XpCurve(1000, 100L, 1.45);
            assertEquals(2, repository.initialize(curve).get());
            var after = repository.inspectSchema().get();
            assertEquals(SkillsRepository.SchemaState.CURRENT_2_0, after.state());
            assertEquals(before.fingerprint(), after.fingerprint(), "canonical player/skill XP rows must remain identical");

            try (var connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath)) {
                try (var ps = connection.prepareStatement("SELECT total_xp,level FROM skill_progress WHERE player_uuid=? AND skill='MINING'")) {
                    ps.setString(1, "00000000-0000-0000-0000-000000000001");
                    try (var rs = ps.executeQuery()) {
                        assertTrue(rs.next());
                        assertEquals(12345L, rs.getLong("total_xp"));
                        assertEquals(curve.levelForXp(12345L), rs.getInt("level"));
                    }
                }
                try (var ps = connection.prepareStatement("SELECT value FROM migration_meta WHERE key='product_schema'"); var rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals("2", rs.getString(1));
                }
                try (var rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM milestone_claims")) {
                    assertTrue(rs.next()); assertEquals(0, rs.getInt(1));
                }
                try (var rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM skill_statistics")) {
                    assertTrue(rs.next()); assertEquals(0, rs.getInt(1));
                }
            }
            repository.close();
        }
    }

    @Test
    void rejectsUnknownLegacySkillBeforeMigration() throws Exception {
        Path databasePath = temp.resolve("invalid.db");
        Class.forName("org.sqlite.JDBC");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath); var st = connection.createStatement()) {
            st.execute("CREATE TABLE players(player_uuid TEXT PRIMARY KEY,last_name TEXT,updated_at INTEGER NOT NULL)");
            st.execute("CREATE TABLE skill_progress(player_uuid TEXT NOT NULL,skill TEXT NOT NULL,total_xp INTEGER NOT NULL,level INTEGER NOT NULL,updated_at INTEGER NOT NULL,PRIMARY KEY(player_uuid,skill))");
            st.execute("CREATE TABLE migration_meta(key TEXT PRIMARY KEY,value TEXT NOT NULL)");
            st.execute("INSERT INTO players VALUES('00000000-0000-0000-0000-000000000001','Legacy',1)");
            st.execute("INSERT INTO skill_progress VALUES('00000000-0000-0000-0000-000000000001','UNKNOWN_SKILL',50,1,1)");
        }
        Plugin plugin = pluginStub();
        try (CoreScheduler scheduler = new CoreScheduler(plugin, 1, 64, 1, 64);
             SqliteService sqlite = new SqliteService(scheduler)) {
            SkillsRepository repository = new SkillsRepository(sqlite.open(databasePath));
            var inspection = repository.inspectSchema().get();
            assertEquals(SkillsRepository.SchemaState.INCOMPATIBLE, inspection.state());
            assertTrue(inspection.detail().contains("Unknown legacy skill"));
            repository.close();
        }
    }

    private static Plugin pluginStub() {
        return (Plugin) Proxy.newProxyInstance(
            Plugin.class.getClassLoader(),
            new Class<?>[]{Plugin.class},
            (proxy, method, args) -> {
                if (method.getName().equals("getLogger")) return Logger.getLogger("PlexonSkillsMigrationTest");
                if (method.getName().equals("getName")) return "PlexonSkillsMigrationTest";
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
