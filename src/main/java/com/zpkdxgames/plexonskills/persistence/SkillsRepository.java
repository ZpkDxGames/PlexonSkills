package com.zpkdxgames.plexonskills.persistence;

import com.zpkdxgames.plexoncore.persistence.SqliteService;
import com.zpkdxgames.plexonskills.player.PlayerSkillsProfile;
import com.zpkdxgames.plexonskills.skill.SkillType;
import com.zpkdxgames.plexonskills.skill.XpCurve;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SkillsRepository implements AutoCloseable {
    public static final int PRODUCT_SCHEMA_VERSION = 2;
    private static final Set<String> PLAYER_COLUMNS = Set.of("player_uuid", "last_name", "updated_at");
    private static final Set<String> PROGRESS_COLUMNS = Set.of("player_uuid", "skill", "total_xp", "level", "updated_at");

    private final SqliteService.SqliteDatabase database;

    public SkillsRepository(SqliteService.SqliteDatabase database) { this.database = database; }

    /**
     * Applies the Core-owned transactional schema chain. Migration 2 preserves every canonical
     * total-XP value, recalculates only the derived stored level, and writes a product schema marker.
     */
    public CompletableFuture<Integer> initialize(XpCurve curve) {
        return database.migrate(List.of(
            new SqliteService.Migration(1, "initial skills schema", connection -> {
                try (Statement st = connection.createStatement()) {
                    st.execute("CREATE TABLE IF NOT EXISTS players(player_uuid TEXT PRIMARY KEY,last_name TEXT,updated_at INTEGER NOT NULL)");
                    st.execute("CREATE TABLE IF NOT EXISTS skill_progress(player_uuid TEXT NOT NULL,skill TEXT NOT NULL,total_xp INTEGER NOT NULL,level INTEGER NOT NULL,updated_at INTEGER NOT NULL,PRIMARY KEY(player_uuid,skill),FOREIGN KEY(player_uuid) REFERENCES players(player_uuid) ON DELETE CASCADE)");
                    st.execute("CREATE TABLE IF NOT EXISTS migration_meta(key TEXT PRIMARY KEY,value TEXT NOT NULL)");
                    st.execute("CREATE INDEX IF NOT EXISTS idx_skill_progress_rank ON skill_progress(skill,total_xp DESC)");
                }
            }),
            new SqliteService.Migration(2, "PlexonSkills 2.0 product schema", connection -> {
                SchemaFingerprint before = fingerprint(connection);
                validateLegacyRows(connection);
                try (Statement st = connection.createStatement()) {
                    st.execute("CREATE TABLE IF NOT EXISTS milestone_claims(player_uuid TEXT NOT NULL,skill TEXT NOT NULL,milestone_id TEXT NOT NULL,claimed_at INTEGER NOT NULL,PRIMARY KEY(player_uuid,skill,milestone_id),FOREIGN KEY(player_uuid) REFERENCES players(player_uuid) ON DELETE CASCADE)");
                    st.execute("CREATE TABLE IF NOT EXISTS skill_statistics(player_uuid TEXT NOT NULL,stat_key TEXT NOT NULL,value INTEGER NOT NULL,updated_at INTEGER NOT NULL,PRIMARY KEY(player_uuid,stat_key),FOREIGN KEY(player_uuid) REFERENCES players(player_uuid) ON DELETE CASCADE)");
                    st.execute("CREATE INDEX IF NOT EXISTS idx_milestone_claims_player ON milestone_claims(player_uuid)");
                }
                recalculateDerivedLevels(connection, curve);
                writeMeta(connection, "product_schema", Integer.toString(PRODUCT_SCHEMA_VERSION));
                writeMeta(connection, "product_version", "2.0.0");
                writeMeta(connection, "xp_migration_strategy", "PRESERVE_TOTAL_XP_RECALCULATE_LEVEL");
                writeMeta(connection, "schema_migrated_at", Long.toString(System.currentTimeMillis()));
                SchemaFingerprint after = fingerprint(connection);
                if (!before.equals(after)) {
                    throw new IllegalStateException("2.0 schema migration changed canonical XP rows: before=" + before + " after=" + after);
                }
            })
        ));
    }

    /** Read-only startup inspection used before any 2.0 migration is attempted. */
    public CompletableFuture<SchemaInspection> inspectSchema() {
        return database.query(connection -> {
            try {
                boolean players = tableExists(connection, "players");
                boolean progress = tableExists(connection, "skill_progress");
                if (!players && !progress) {
                    return new SchemaInspection(SchemaState.EMPTY, new SchemaFingerprint(0, 0, 0L), "Fresh database; no legacy schema detected.");
                }
                if (!players || !progress) {
                    return new SchemaInspection(SchemaState.INCOMPATIBLE, new SchemaFingerprint(0, 0, 0L), "Partial legacy schema: players=" + players + ", skill_progress=" + progress);
                }
                if (!columns(connection, "players").containsAll(PLAYER_COLUMNS)) {
                    return new SchemaInspection(SchemaState.INCOMPATIBLE, fingerprint(connection), "players table is missing required 1.x columns");
                }
                if (!columns(connection, "skill_progress").containsAll(PROGRESS_COLUMNS)) {
                    return new SchemaInspection(SchemaState.INCOMPATIBLE, fingerprint(connection), "skill_progress table is missing required 1.x columns");
                }
                Optional<String> invalid = validateLegacyRowsMessage(connection);
                if (invalid.isPresent()) {
                    return new SchemaInspection(SchemaState.INCOMPATIBLE, fingerprint(connection), invalid.get());
                }
                String marker = tableExists(connection, "migration_meta") ? readMeta(connection, "product_schema") : null;
                if (Integer.toString(PRODUCT_SCHEMA_VERSION).equals(marker)) {
                    boolean claims = tableExists(connection, "milestone_claims");
                    boolean statistics = tableExists(connection, "skill_statistics");
                    if (!claims || !statistics) {
                        return new SchemaInspection(SchemaState.INCOMPATIBLE, fingerprint(connection), "2.0 schema marker exists but required 2.0 tables are missing");
                    }
                    return new SchemaInspection(SchemaState.CURRENT_2_0, fingerprint(connection), "PlexonSkills product schema 2 is current.");
                }
                return new SchemaInspection(SchemaState.LEGACY_1_X, fingerprint(connection), "Compatible PlexonSkills 1.x/prerelease schema detected.");
            } catch (SQLException ex) {
                throw new IllegalStateException("Failed to inspect PlexonSkills schema", ex);
            }
        });
    }

    public CompletableFuture<LoadedProfile> load(UUID playerId, String playerName, XpCurve curve) {
        return database.query(connection -> {
            try {
                EnumMap<SkillType, Long> xp = new EnumMap<>(SkillType.class);
                try (PreparedStatement ps = connection.prepareStatement("SELECT skill,total_xp FROM skill_progress WHERE player_uuid=?")) {
                    ps.setString(1, playerId.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String skillId = rs.getString(1);
                            long totalXp = curve.clampXp(rs.getLong(2));
                            SkillType.parse(skillId).ifPresent(type -> xp.put(type, totalXp));
                        }
                    }
                }
                return new LoadedProfile(playerId, playerName, xp);
            } catch (SQLException ex) { throw new IllegalStateException("Failed to load skill profile " + playerId, ex); }
        });
    }

    public CompletableFuture<Void> save(Collection<NamedSnapshot> snapshots) {
        List<NamedSnapshot> batch = List.copyOf(snapshots);
        if (batch.isEmpty()) return CompletableFuture.completedFuture(null);
        return database.executeWrite(connection -> {
            long now = System.currentTimeMillis();
            try (PreparedStatement player = connection.prepareStatement("INSERT INTO players(player_uuid,last_name,updated_at) VALUES(?,?,?) ON CONFLICT(player_uuid) DO UPDATE SET last_name=excluded.last_name,updated_at=excluded.updated_at");
                 PreparedStatement skill = connection.prepareStatement("INSERT INTO skill_progress(player_uuid,skill,total_xp,level,updated_at) VALUES(?,?,?,?,?) ON CONFLICT(player_uuid,skill) DO UPDATE SET total_xp=excluded.total_xp,level=excluded.level,updated_at=excluded.updated_at")) {
                for (NamedSnapshot named : batch) {
                    PlayerSkillsProfile.Snapshot snapshot = named.snapshot();
                    String uuid = snapshot.playerId().toString();
                    player.setString(1, uuid); player.setString(2, named.playerName()); player.setLong(3, now); player.addBatch();
                    for (SkillType type : SkillType.values()) {
                        skill.setString(1, uuid); skill.setString(2, type.name()); skill.setLong(3, snapshot.xp(type)); skill.setInt(4, snapshot.level(type)); skill.setLong(5, now); skill.addBatch();
                    }
                }
                player.executeBatch();
                skill.executeBatch();
            }
        });
    }

    public CompletableFuture<List<LeaderboardEntry>> top(SkillType type, int limit) {
        int safeLimit = Math.max(1, Math.min(100, limit));
        return database.query(connection -> {
            try {
                List<LeaderboardEntry> result = new ArrayList<>();
                String sql = "SELECT p.player_uuid,p.last_name,s.total_xp,s.level FROM skill_progress s JOIN players p ON p.player_uuid=s.player_uuid WHERE s.skill=? ORDER BY s.total_xp DESC,p.player_uuid ASC LIMIT ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, type.name()); ps.setInt(2, safeLimit);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) result.add(new LeaderboardEntry(UUID.fromString(rs.getString(1)), rs.getString(2), rs.getLong(3), rs.getInt(4)));
                    }
                }
                return List.copyOf(result);
            } catch (SQLException ex) { throw new IllegalStateException("Leaderboard query failed", ex); }
        });
    }

    public CompletableFuture<Integer> rank(SkillType type, UUID playerId) {
        return database.query(connection -> {
            try {
                long xp;
                try (PreparedStatement own = connection.prepareStatement("SELECT total_xp FROM skill_progress WHERE player_uuid=? AND skill=?")) {
                    own.setString(1, playerId.toString()); own.setString(2, type.name());
                    try (ResultSet rs = own.executeQuery()) { if (!rs.next()) return 0; xp = rs.getLong(1); }
                }
                try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*)+1 FROM skill_progress WHERE skill=? AND total_xp>?")) {
                    ps.setString(1, type.name()); ps.setLong(2, xp);
                    try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
                }
            } catch (SQLException ex) { throw new IllegalStateException("Rank query failed", ex); }
        });
    }

    public CompletableFuture<Void> writeMigrationMeta(String key, String value) {
        return database.executeWrite(connection -> writeMeta(connection, key, value));
    }

    public Path backup(Path destination) throws Exception { return database.backup(destination); }
    public int queuedWrites() { return database.queuedWrites(); }
    public SqliteService.DatabaseHealth health() { return database.health(); }
    @Override public void close() { database.close(); }

    private static void recalculateDerivedLevels(Connection connection, XpCurve curve) throws SQLException {
        record Row(String playerId, String skill, long xp) { }
        List<Row> rows = new ArrayList<>();
        try (PreparedStatement read = connection.prepareStatement("SELECT player_uuid,skill,total_xp FROM skill_progress"); ResultSet rs = read.executeQuery()) {
            while (rs.next()) rows.add(new Row(rs.getString(1), rs.getString(2), rs.getLong(3)));
        }
        try (PreparedStatement update = connection.prepareStatement("UPDATE skill_progress SET level=? WHERE player_uuid=? AND skill=?")) {
            for (Row row : rows) {
                update.setInt(1, curve.levelForXp(curve.clampXp(row.xp())));
                update.setString(2, row.playerId());
                update.setString(3, row.skill());
                update.addBatch();
            }
            update.executeBatch();
        }
    }

    private static void validateLegacyRows(Connection connection) throws SQLException {
        Optional<String> invalid = validateLegacyRowsMessage(connection);
        if (invalid.isPresent()) throw new IllegalStateException(invalid.get());
    }

    private static Optional<String> validateLegacyRowsMessage(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT player_uuid,skill,total_xp FROM skill_progress ORDER BY player_uuid,skill"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String playerId = rs.getString(1);
                String skill = rs.getString(2);
                long xp = rs.getLong(3);
                try { UUID.fromString(playerId); }
                catch (IllegalArgumentException ex) { return Optional.of("Invalid player UUID in legacy skill_progress: " + playerId); }
                if (SkillType.parse(skill).isEmpty()) return Optional.of("Unknown legacy skill identifier: " + skill);
                if (xp < 0L) return Optional.of("Negative canonical XP found for " + playerId + "/" + skill);
            }
        }
        return Optional.empty();
    }

    private static SchemaFingerprint fingerprint(Connection connection) throws SQLException {
        int players = count(connection, "players");
        int progress = count(connection, "skill_progress");
        long hash = 1125899906842597L;
        if (tableExists(connection, "skill_progress")) {
            try (PreparedStatement ps = connection.prepareStatement("SELECT player_uuid,skill,total_xp FROM skill_progress ORDER BY player_uuid,skill"); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    hash = 31L * hash + rs.getString(1).hashCode();
                    hash = 31L * hash + rs.getString(2).hashCode();
                    hash = 31L * hash + Long.hashCode(rs.getLong(3));
                }
            }
        } else hash = 0L;
        return new SchemaFingerprint(players, progress, hash);
    }

    private static int count(Connection connection, String table) throws SQLException {
        if (!tableExists(connection, table)) return 0;
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static boolean tableExists(Connection connection, String table) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?")) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private static Set<String> columns(Connection connection, String table) throws SQLException {
        java.util.HashSet<String> result = new java.util.HashSet<>();
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) result.add(rs.getString("name"));
        }
        return Set.copyOf(result);
    }

    private static String readMeta(Connection connection, String key) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT value FROM migration_meta WHERE key=?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        }
    }

    private static void writeMeta(Connection connection, String key, String value) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO migration_meta(key,value) VALUES(?,?) ON CONFLICT(key) DO UPDATE SET value=excluded.value")) {
            ps.setString(1, key); ps.setString(2, value); ps.executeUpdate();
        }
    }

    public enum SchemaState { EMPTY, LEGACY_1_X, CURRENT_2_0, INCOMPATIBLE }
    public record SchemaFingerprint(int playerRows, int progressRows, long canonicalXpHash) { }
    public record SchemaInspection(SchemaState state, SchemaFingerprint fingerprint, String detail) { }
    public record LoadedProfile(UUID playerId, String playerName, Map<SkillType, Long> totalXp) {
        public LoadedProfile { totalXp = Map.copyOf(totalXp); }
    }
    public record NamedSnapshot(String playerName, PlayerSkillsProfile.Snapshot snapshot) {}
    public record LeaderboardEntry(UUID playerId, String playerName, long totalXp, int level) {}
}
