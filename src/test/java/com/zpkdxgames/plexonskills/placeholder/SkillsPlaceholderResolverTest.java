package com.zpkdxgames.plexonskills.placeholder;

import com.zpkdxgames.plexonskills.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkillsPlaceholderResolverTest {
    private static final SkillsPlaceholderResolver.Values READY = new SkillsPlaceholderResolver.Values() {
        @Override public boolean ready() { return true; }
        @Override public int totalLevel() { return 42; }
        @Override public long totalXp() { return 12_345L; }
        @Override public String highestSkill() { return "Mining"; }
        @Override public int highestSkillLevel() { return 9; }
        @Override public int level(SkillType skill) { return skill == SkillType.MINING ? 9 : 1; }
        @Override public long xp(SkillType skill) { return skill == SkillType.MINING ? 900L : 0L; }
        @Override public long xpRequired(SkillType skill) { return skill == SkillType.MINING ? 200L : 100L; }
        @Override public double progressPercent(SkillType skill) { return skill == SkillType.MINING ? 75.0 : 0.0; }
        @Override public String stage(SkillType skill) { return skill == SkillType.MINING ? "Veteran" : "Initiate"; }
        @Override public String nextMilestone(SkillType skill) { return skill == SkillType.MINING ? "Mining Expert @ 10" : "NONE"; }
        @Override public double masteryPercent(SkillType skill) { return skill == SkillType.MINING ? 90.0 : 10.0; }
        @Override public double passiveBonusPercent(SkillType skill) { return skill == SkillType.MINING ? 10.0 : 0.0; }
        @Override public String abilityState(SkillType skill) { return skill == SkillType.MINING ? "COOLDOWN" : "LOCKED"; }
        @Override public long abilityCooldownSeconds(SkillType skill) { return skill == SkillType.MINING ? 12L : 0L; }
    };

    @Test
    void resolvesTopLevelContract() {
        assertEquals("42", SkillsPlaceholderResolver.resolve(READY, "total_level"));
        assertEquals("12345", SkillsPlaceholderResolver.resolve(READY, "total_xp"));
        assertEquals("Mining", SkillsPlaceholderResolver.resolve(READY, "highest_skill"));
        assertEquals("9", SkillsPlaceholderResolver.resolve(READY, "highest_skill_level"));
    }

    @Test
    void resolvesPerSkillContractAndCompatibilityAliases() {
        assertEquals("9", SkillsPlaceholderResolver.resolve(READY, "mining_level"));
        assertEquals("900", SkillsPlaceholderResolver.resolve(READY, "mining_xp"));
        assertEquals("200", SkillsPlaceholderResolver.resolve(READY, "mining_xp_required"));
        assertEquals("200", SkillsPlaceholderResolver.resolve(READY, "mining_xp_next"));
        assertEquals("75.0", SkillsPlaceholderResolver.resolve(READY, "mining_progress"));
        assertEquals("Veteran", SkillsPlaceholderResolver.resolve(READY, "mining_stage"));
        assertEquals("Mining Expert @ 10", SkillsPlaceholderResolver.resolve(READY, "mining_next_milestone"));
        assertEquals("90.0", SkillsPlaceholderResolver.resolve(READY, "mining_mastery"));
        assertEquals("false", SkillsPlaceholderResolver.resolve(READY, "mining_mastered"));
        assertEquals("10.0", SkillsPlaceholderResolver.resolve(READY, "mining_passive_bonus"));
        assertEquals("COOLDOWN", SkillsPlaceholderResolver.resolve(READY, "mining_ability_state"));
        assertEquals("12", SkillsPlaceholderResolver.resolve(READY, "mining_ability_cooldown"));
        assertEquals("0", SkillsPlaceholderResolver.resolve(READY, "mining_rank"));
    }

    @Test
    void invalidAndUnloadedInputsFailDeterministically() {
        assertEquals("", SkillsPlaceholderResolver.resolve(READY, "not_a_skill_level"));
        assertEquals("", SkillsPlaceholderResolver.resolve(READY, "mining_unknown"));
        assertEquals("", SkillsPlaceholderResolver.resolve(READY, null));

        SkillsPlaceholderResolver.Values loading = new DelegatingValues(READY) {
            @Override public boolean ready() { return false; }
        };
        assertEquals("0", SkillsPlaceholderResolver.resolve(loading, "mining_level"));
        assertEquals("0", SkillsPlaceholderResolver.resolve(loading, "highest_skill"));
    }

    private static class DelegatingValues implements SkillsPlaceholderResolver.Values {
        private final SkillsPlaceholderResolver.Values delegate;
        private DelegatingValues(SkillsPlaceholderResolver.Values delegate) { this.delegate = delegate; }
        @Override public boolean ready() { return delegate.ready(); }
        @Override public int totalLevel() { return delegate.totalLevel(); }
        @Override public long totalXp() { return delegate.totalXp(); }
        @Override public String highestSkill() { return delegate.highestSkill(); }
        @Override public int highestSkillLevel() { return delegate.highestSkillLevel(); }
        @Override public int level(SkillType skill) { return delegate.level(skill); }
        @Override public long xp(SkillType skill) { return delegate.xp(skill); }
        @Override public long xpRequired(SkillType skill) { return delegate.xpRequired(skill); }
        @Override public double progressPercent(SkillType skill) { return delegate.progressPercent(skill); }
        @Override public String stage(SkillType skill) { return delegate.stage(skill); }
        @Override public String nextMilestone(SkillType skill) { return delegate.nextMilestone(skill); }
        @Override public double masteryPercent(SkillType skill) { return delegate.masteryPercent(skill); }
        @Override public double passiveBonusPercent(SkillType skill) { return delegate.passiveBonusPercent(skill); }
        @Override public String abilityState(SkillType skill) { return delegate.abilityState(skill); }
        @Override public long abilityCooldownSeconds(SkillType skill) { return delegate.abilityCooldownSeconds(skill); }
    }
}
