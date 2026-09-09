package com.zpkdxgames.plexonskills.api;

import com.zpkdxgames.plexonskills.skill.SkillType;

import java.util.Optional;
import java.util.UUID;

public interface PlexonSkillsAPI {
    /** ASYNC_SAFE_READ for currently loaded profiles. */
    int getLevel(UUID playerId, SkillType skill);
    /** ASYNC_SAFE_READ for currently loaded profiles. */
    long getTotalXp(UUID playerId, SkillType skill);
    /** ASYNC_SAFE_READ for currently loaded profiles. */
    int getTotalLevel(UUID playerId);
    /** ASYNC_SAFE_READ immutable view. */
    Optional<PlayerSkillView> profile(UUID playerId);
    /** MAIN_THREAD_ONLY authoritative mutation. */
    boolean addXp(UUID playerId, SkillType skill, long amount, String source);
    /** MAIN_THREAD_ONLY authoritative mutation. */
    boolean setXp(UUID playerId, SkillType skill, long amount, String source);
    /** ANY_THREAD. */
    boolean isProfileReady(UUID playerId);
}
