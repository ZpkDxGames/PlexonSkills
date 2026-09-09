package com.zpkdxgames.plexonskills.placeholder;

import com.zpkdxgames.plexonskills.ability.AbilityRuntime;
import com.zpkdxgames.plexonskills.api.PlexonSkillsAPI;
import com.zpkdxgames.plexonskills.config.RuntimeSettings;
import com.zpkdxgames.plexonskills.gui.SkillStage;
import com.zpkdxgames.plexonskills.skill.SkillType;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** PlaceholderAPI adapter. Resolution is strictly memory/runtime backed; it never queries SQLite. */
public final class SkillsPlaceholderExpansion extends PlaceholderExpansion {
    private final PlexonSkillsAPI api;
    private final Supplier<RuntimeSettings> settings;
    private final AbilityRuntime abilities;
    private final String version;

    public SkillsPlaceholderExpansion(PlexonSkillsAPI api, Supplier<RuntimeSettings> settings, AbilityRuntime abilities, String version) {
        this.api = api;
        this.settings = settings;
        this.abilities = abilities;
        this.version = version;
    }

    @Override public String getIdentifier() { return "plexonskills"; }
    @Override public String getAuthor() { return "Tonim (ZpkDxGames)"; }
    @Override public String getVersion() { return version; }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || params == null) return "";
        UUID playerId = player.getUniqueId();
        RuntimeSettings runtime = settings.get();
        return SkillsPlaceholderResolver.resolve(new LiveValues(playerId, runtime), params);
    }

    private final class LiveValues implements SkillsPlaceholderResolver.Values {
        private final UUID playerId;
        private final RuntimeSettings runtime;

        private LiveValues(UUID playerId, RuntimeSettings runtime) {
            this.playerId = playerId;
            this.runtime = runtime;
        }

        @Override public boolean ready() { return api.isProfileReady(playerId); }
        @Override public int totalLevel() { return api.getTotalLevel(playerId); }

        @Override
        public long totalXp() {
            long total = 0L;
            for (SkillType skill : runtime.skills().enabledSkills()) {
                long value = Math.max(0L, api.getTotalXp(playerId, skill));
                if (Long.MAX_VALUE - total < value) return Long.MAX_VALUE;
                total += value;
            }
            return total;
        }

        @Override public String highestSkill() { return highest().displayName(); }
        @Override public int highestSkillLevel() { return api.getLevel(playerId, highest()); }
        @Override public int level(SkillType skill) { return api.getLevel(playerId, skill); }
        @Override public long xp(SkillType skill) { return api.getTotalXp(playerId, skill); }
        @Override public long xpRequired(SkillType skill) { return runtime.curve().requiredForNext(level(skill)); }
        @Override public double progressPercent(SkillType skill) { return runtime.curve().progressWithinLevel(xp(skill)) * 100.0; }
        @Override public String stage(SkillType skill) { return SkillStage.forLevel(level(skill), runtime.curve().maximumLevel()).displayName(); }

        @Override
        public String nextMilestone(SkillType skill) {
            int level = level(skill);
            if (level >= runtime.curve().maximumLevel()) return "FULLY MASTERED";
            return runtime.milestones().next(skill, level)
                .map(milestone -> milestone.displayName() + " @ " + milestone.level())
                .orElse("NONE");
        }

        @Override
        public double masteryPercent(SkillType skill) {
            int maximum = Math.max(1, runtime.curve().maximumLevel());
            return Math.max(0.0, Math.min(100.0, (double) level(skill) * 100.0 / maximum));
        }

        @Override
        public double passiveBonusPercent(SkillType skill) {
            return Math.max(0.0, (runtime.milestones().passiveMultiplier(skill, level(skill)) - 1.0) * 100.0);
        }

        @Override public String abilityState(SkillType skill) { return abilities.view(playerId, skill, level(skill)).state().name(); }
        @Override public long abilityCooldownSeconds(SkillType skill) { return ceilSeconds(abilities.cooldownRemainingMillis(playerId, skill)); }

        private SkillType highest() {
            List<SkillType> enabled = runtime.skills().enabledSkills();
            SkillType best = enabled.isEmpty() ? SkillType.MINING : enabled.getFirst();
            long bestXp = Long.MIN_VALUE;
            for (SkillType skill : enabled) {
                long candidate = api.getTotalXp(playerId, skill);
                if (candidate > bestXp) {
                    bestXp = candidate;
                    best = skill;
                }
            }
            return best;
        }
    }

    private static long ceilSeconds(long millis) {
        if (millis <= 0L) return 0L;
        return (millis + 999L) / 1_000L;
    }
}
