package com.zpkdxgames.plexonskills.persistence;

import com.zpkdxgames.plexoncore.persistence.SqliteService;
import com.zpkdxgames.plexonskills.player.PlayerSkillsProfile;
import com.zpkdxgames.plexonskills.skill.SkillType;
import com.zpkdxgames.plexonskills.skill.XpCurve;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SkillsRepository implements AutoCloseable {
    private final SqliteService.SqliteDatabase database;

    public SkillsRepository(SqliteService.SqliteDatabase database) { this.database = database; }

    public CompletableFuture<Integer> initialize() {
        return database.migrate(List.of(
            new SqliteService.Migration(1, "initial skills schema", connection -> {
                try (Statement st = connection.createStatement()) {
                    st.execute("CREATE TABLE IF NOT EXISTS players(player_uuid TEXT PRIMARY KEY,last_name TEXT,updated_at INTEGER NOT NULL)");
                    st.execute("CREATE TABLE IF NOT EXISTS skill_progress(player_uuid TEXT NOT NULL,skill TEXT NOT NULL,total_xp INTEGER NOT NULL,level INTEGER NOT NULL,updated_at INTEGER NOT NULL,PRIMARY KEY(player_uuid,skill),FOREIGN KEY(player_uuid) REFERENCES players(player_uuid) ON DELETE CASCADE)");
                    st.execute("CREATE TABLE IF NOT EXISTS migration_meta(key TEXT PRIMARY KEY,value TEXT NOT NULL)");
                    st.execute("CREATE INDEX IF NOT EXISTS idx_skill_progress_rank ON skill_progress(skill,total_xp DESC)");
                }
            })
        ));
    }

    public CompletableFuture<LoadedProfile> load(UUID playerId, String playerName, XpCurve curve) {
        return database.query(connection -> {
            try {
                EnumMap<SkillType, Long> xp = new EnumMap<>(SkillType.class);
                try (PreparedStatement ps = connection.prepareStatement("SELECT skill,total_xp FROM skill_progress WHERE player_uuid=?")) {
                    ps.setString(1, playerId.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) SkillType.parse(rs.getString(1)).ifPresent(type -> xp.put(type, curve.clampXp(rs.getLong(2))));
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
                String sql = "SELECT p.player_uuid,p.last_name,s.total_xp,s.level FROM skill_progress s JOIN players p ON p.player_uuid=s.player_uuid WHERE s.skill=? ORDER BY s.total_xp DESC LIMIT ?";
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
        return database.executeWrite(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO migration_meta(key,value) VALUES(?,?) ON CONFLICT(key) DO UPDATE SET value=excluded.value")) {
                ps.setString(1, key); ps.setString(2, value); ps.executeUpdate();
            }
        });
    }

    public Path backup(Path destination) throws Exception { return database.backup(destination); }
    public int queuedWrites() { return database.queuedWrites(); }
    public SqliteService.DatabaseHealth health() { return database.health(); }
    @Override public void close() { database.close(); }

    public record LoadedProfile(UUID playerId, String playerName, Map<SkillType, Long> totalXp) {
        public LoadedProfile { totalXp = Map.copyOf(totalXp); }
    }
    public record NamedSnapshot(String playerName, PlayerSkillsProfile.Snapshot snapshot) {}
    public record LeaderboardEntry(UUID playerId, String playerName, long totalXp, int level) {}
}
