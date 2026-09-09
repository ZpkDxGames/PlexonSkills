package com.zpkdxgames.plexonskills.config;

import com.zpkdxgames.plexonskills.migration.MigrationMode;
import com.zpkdxgames.plexonskills.skill.SkillRegistry;
import com.zpkdxgames.plexonskills.skill.XpCurve;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public record RuntimeSettings(
    long epoch,
    SkillRegistry skills,
    XpCurve curve,
    MigrationMode migrationMode,
    boolean rejectUnknownOrigin,
    boolean allowPvp,
    Set<GameMode> allowedGameModes,
    WorldPolicy worldPolicy,
    boolean actionbar,
    int actionbarCoalesceTicks,
    boolean levelUpTitle,
    long flushIntervalTicks,
    int shutdownTimeoutSeconds
) {
    public RuntimeSettings {
        allowedGameModes = Set.copyOf(allowedGameModes);
        if (flushIntervalTicks < 20) throw new IllegalArgumentException("flush interval must be >= 20 ticks");
        if (shutdownTimeoutSeconds < 1 || shutdownTimeoutSeconds > 30) throw new IllegalArgumentException("shutdown timeout must be 1..30 seconds");
    }

    public static RuntimeSettings load(JavaPlugin plugin, long epoch) {
        plugin.reloadConfig();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
        SkillRegistry registry = SkillRegistry.load(new File(plugin.getDataFolder(), "skills.yml"));
        int maximum = config.getInt("progression.maximum-level", 1000);
        String curveType = config.getString("progression.curve.type", "POWER");
        if (!"POWER".equalsIgnoreCase(curveType)) throw new IllegalArgumentException("1.0.0 currently supports POWER curve; got " + curveType);
        long base = config.getLong("progression.curve.base", 100L);
        double exponent = config.getDouble("progression.curve.exponent", 1.45);
        XpCurve curve = new XpCurve(maximum, base, exponent);
        MigrationMode migrationMode = MigrationMode.parse(config.getString("migration.mcmmo.mode", "SHADOW"));
        boolean rejectUnknown = "REJECT".equalsIgnoreCase(config.getString("anti-exploit.block-origin.unknown-policy", "REJECT"));
        boolean allowPvp = config.getBoolean("anti-exploit.combat.allow-pvp", false);
        EnumSet<GameMode> gameModes = EnumSet.noneOf(GameMode.class);
        for (String raw : config.getStringList("anti-exploit.game-modes")) {
            try { gameModes.add(GameMode.valueOf(raw.toUpperCase(Locale.ROOT))); }
            catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Unknown game mode: " + raw); }
        }
        if (gameModes.isEmpty()) gameModes.add(GameMode.SURVIVAL);
        String worldMode = config.getString("worlds.mode", "BLACKLIST");
        Set<String> worlds = new HashSet<>();
        for (String world : config.getStringList("worlds.values")) worlds.add(world.toLowerCase(Locale.ROOT));
        WorldPolicy worldPolicy = new WorldPolicy(WorldMode.valueOf(worldMode.toUpperCase(Locale.ROOT)), Set.copyOf(worlds));
        return new RuntimeSettings(
            epoch, registry, curve, migrationMode, rejectUnknown, allowPvp, gameModes, worldPolicy,
            config.getBoolean("feedback.actionbar", true),
            Math.max(1, config.getInt("feedback.actionbar-coalesce-ticks", 8)),
            config.getBoolean("feedback.level-up-title", true),
            Math.max(20L, config.getLong("persistence.flush-interval-ticks", 200L)),
            config.getInt("persistence.shutdown-timeout-seconds", 5)
        );
    }

    public boolean allows(String worldName, GameMode gameMode) {
        return allowedGameModes.contains(gameMode) && worldPolicy.allows(worldName);
    }

    public enum WorldMode { BLACKLIST, WHITELIST }
    public record WorldPolicy(WorldMode mode, Set<String> values) {
        public boolean allows(String world) {
            String normalized = world == null ? "" : world.toLowerCase(Locale.ROOT);
            boolean contains = values.contains(normalized);
            return mode == WorldMode.BLACKLIST ? !contains : contains;
        }
    }
}
