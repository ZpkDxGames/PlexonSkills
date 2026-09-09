package com.zpkdxgames.plexonskills.player;

import com.zpkdxgames.plexonskills.skill.SkillType;
import com.zpkdxgames.plexonskills.skill.XpCurve;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/** Main-thread authoritative mutation with lock-free, async-safe reads. */
public final class PlayerSkillsProfile {
    private final UUID playerId;
    private final AtomicLongArray totalXp = new AtomicLongArray(SkillType.values().length);
    private final AtomicIntegerArray levels = new AtomicIntegerArray(SkillType.values().length);
    private final AtomicLong revision = new AtomicLong();
    private volatile ProfileStatus status;

    public PlayerSkillsProfile(UUID playerId, ProfileStatus status) {
        this.playerId = playerId;
        this.status = status;
        for (int i = 0; i < levels.length(); i++) levels.set(i, 1);
    }

    public UUID playerId() { return playerId; }
    public ProfileStatus status() { return status; }
    public void status(ProfileStatus status) { this.status = status; }
    public long revision() { return revision.get(); }
    public long totalXp(SkillType type) { return totalXp.get(type.ordinal()); }
    public int level(SkillType type) { return levels.get(type.ordinal()); }

    public Mutation setXp(SkillType type, long requestedXp, XpCurve curve) {
        int idx = type.ordinal();
        long oldXp = totalXp.get(idx);
        int oldLevel = levels.get(idx);
        long xp = curve.clampXp(requestedXp);
        int newLevel = curve.levelForXp(xp);
        totalXp.set(idx, xp);
        levels.set(idx, newLevel);
        revision.incrementAndGet();
        return new Mutation(oldXp, xp, oldLevel, newLevel);
    }

    public Mutation addXp(SkillType type, long amount, XpCurve curve) {
        if (amount <= 0) return new Mutation(totalXp(type), totalXp(type), level(type), level(type));
        long current = totalXp(type);
        long target = current > Long.MAX_VALUE - amount ? Long.MAX_VALUE : current + amount;
        return setXp(type, target, curve);
    }

    public void load(SkillType type, long xp, int ignoredStoredLevel, XpCurve curve) {
        int idx = type.ordinal();
        long clamped = curve.clampXp(xp);
        totalXp.set(idx, clamped);
        levels.set(idx, curve.levelForXp(clamped));
    }

    public Snapshot snapshot() {
        long[] xp = new long[SkillType.values().length];
        int[] level = new int[SkillType.values().length];
        for (int i = 0; i < xp.length; i++) { xp[i] = totalXp.get(i); level[i] = levels.get(i); }
        return new Snapshot(playerId, xp, level, revision.get());
    }

    public record Mutation(long oldXp, long newXp, int oldLevel, int newLevel) {
        public boolean changed() { return oldXp != newXp || oldLevel != newLevel; }
        public boolean leveledUp() { return newLevel > oldLevel; }
    }

    public record Snapshot(UUID playerId, long[] totalXp, int[] levels, long revision) {
        public Snapshot { totalXp = totalXp.clone(); levels = levels.clone(); }
        @Override public long[] totalXp() { return totalXp.clone(); }
        @Override public int[] levels() { return levels.clone(); }
        public long xp(SkillType type) { return totalXp[type.ordinal()]; }
        public int level(SkillType type) { return levels[type.ordinal()]; }
    }
}
