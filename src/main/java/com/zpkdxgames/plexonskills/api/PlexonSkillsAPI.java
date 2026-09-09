package com.zpkdxgames.plexonskills.api;

import com.zpkdxgames.plexonskills.skill.SkillType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Stable service contract published through Bukkit's ServicesManager.
 *
 * <p>All read methods operate only on the in-memory loaded-profile snapshot and are safe to call
 * from asynchronous integrations. Mutation methods are authoritative and main-thread-only. No API
 * method performs a synchronous database read.</p>
 */
public interface PlexonSkillsAPI {
    String API_VERSION = "2.0";

    /** ANY_THREAD. Contract version, independent from the plugin artifact version. */
    default String apiVersion() { return API_VERSION; }

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

    /** ASYNC_SAFE_READ. Added in API 2.0; default preserves compatibility with alternate providers. */
    default Optional<AbilityView> ability(UUID playerId, SkillType skill) { return Optional.empty(); }

    /** ASYNC_SAFE_READ. Configured immutable milestone definitions for one skill. */
    default List<MilestoneView> milestones(SkillType skill) { return List.of(); }

    /** ASYNC_SAFE_READ. */
    default boolean isMastered(UUID playerId, SkillType skill) { return false; }

    /** ASYNC_SAFE_READ immutable derived statistics for a currently loaded profile. */
    default Optional<SkillStatisticsView> statistics(UUID playerId) { return Optional.empty(); }
}
