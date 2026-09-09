package com.zpkdxgames.plexonskills.skill;

import java.util.Arrays;

/** Immutable precomputed progression table. Level 1 starts at 0 XP. */
public final class XpCurve {
    private final int maximumLevel;
    private final long[] requiredByLevel;
    private final long[] cumulativeByLevel;

    public XpCurve(int maximumLevel, long base, double exponent) {
        if (maximumLevel < 1 || maximumLevel > 100_000) throw new IllegalArgumentException("maximumLevel out of range");
        if (base < 1) throw new IllegalArgumentException("base must be positive");
        if (!Double.isFinite(exponent) || exponent <= 0.0) throw new IllegalArgumentException("exponent must be positive");
        this.maximumLevel = maximumLevel;
        this.requiredByLevel = new long[maximumLevel + 1];
        this.cumulativeByLevel = new long[maximumLevel + 1];
        cumulativeByLevel[1] = 0L;
        for (int level = 1; level < maximumLevel; level++) {
            double raw = base * Math.pow(level, exponent);
            long required = Math.max(1L, Math.round(raw));
            requiredByLevel[level] = required;
            cumulativeByLevel[level + 1] = saturatedAdd(cumulativeByLevel[level], required);
        }
        requiredByLevel[maximumLevel] = 0L;
    }

    public int maximumLevel() { return maximumLevel; }
    public long requiredForNext(int level) { return level >= maximumLevel ? 0 : requiredByLevel[Math.max(1, level)]; }
    public long cumulativeForLevel(int level) { return cumulativeByLevel[Math.max(1, Math.min(maximumLevel, level))]; }
    public long maximumTrackedXp() { return cumulativeByLevel[maximumLevel]; }

    public int levelForXp(long totalXp) {
        long xp = Math.max(0L, Math.min(totalXp, maximumTrackedXp()));
        int index = Arrays.binarySearch(cumulativeByLevel, 1, cumulativeByLevel.length, xp);
        if (index >= 0) return Math.min(index, maximumLevel);
        int insertion = -index - 1;
        return Math.max(1, Math.min(maximumLevel, insertion - 1));
    }

    public long clampXp(long xp) { return Math.max(0L, Math.min(xp, maximumTrackedXp())); }

    public double progressWithinLevel(long totalXp) {
        int level = levelForXp(totalXp);
        if (level >= maximumLevel) return 1.0;
        long start = cumulativeByLevel[level];
        long required = requiredByLevel[level];
        return required == 0 ? 1.0 : Math.max(0.0, Math.min(1.0, (double) (totalXp - start) / required));
    }

    private static long saturatedAdd(long a, long b) {
        if (b > 0 && a > Long.MAX_VALUE - b) return Long.MAX_VALUE;
        return a + b;
    }
}
