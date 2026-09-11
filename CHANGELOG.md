# Changelog

## 2.0.0 — stable

- Promotes the accepted PlexonSkills 2.0 premium product line and Phase 3 performance boundary to stable `2.0.0`.
- Preserves all 13 skill domains, Core-routed block provenance, main-thread XP authority, milestone-derived passives, asynchronous leaderboards, premium GUI/admin flows, PlaceholderAPI, public API/events, and product schema 2.
- Preserves generation-safe profile loading, detached logout snapshots, coalesced Core SQLite persistence, the final shutdown write barrier, migration backup/fingerprint validation, and one shared ability-expiration coordinator.
- Fixes an ability cooldown exploit: logout now ends the active effect but preserves the original configured cooldown deadline across reconnect instead of resetting the cooldown.
- The shared ability sweep now ages cooldown-only offline owners and removes expired cooldown state without counting those players as active.
- Adds regression coverage for retained future cooldown deadlines and expired/null cleanup.
- Replaces RC-specific release paths with one canonical Build workflow and one exact-current-`main` stable Release workflow.
- Stable publication rebuilds/retests exact final `main`, publishes JAR/checksum/test/provenance evidence, then downloads and verifies the public assets before completion.
- Removes the obsolete one-off `v2.0.0-rc.2` publisher while preserving RC1/RC2 as immutable historical evidence.
- Live PlexonCraft GUI/migration/progression/Spark/soak certification remains a deployment follow-up and may be recorded as `NOT_EXECUTED` in stable release provenance.
- Rollback artifact: `v2.0.0-rc.2` at `fd1f15205353f91e42236865b8dd6141d46b3634`, JAR SHA-256 `af31444e428d2f547ec9c8f5db49aaffeac4594efbe2c3c24e90e74dcb38bf07`.

## 2.0.0-rc.1 — Phase 2 release candidate

Target branch: `agent/2.0.0-premium-tier-product-rebuild`.

### Product rebuild

- Premium session-aware `/skills` GUI hierarchy with profile, skill detail, progression, ability, leaderboard, statistics and help flows.
- PlexonTools-derived Phase 2 MiniMessage presentation language and terminal mastery treatment.
- Full 13-skill active ability runtime with one shared expiration coordinator, lifecycle events and diagnostics.
- Milestone progression with differentiated skill-derived passives and bounded scaling.
- Expanded PlaceholderAPI and premium administrative GUI/configuration flows.
- Provenance-aware anti-exploit policies across block, farming, combat and auxiliary skill sources.

### Closure gates

- Added native 1.x/prerelease schema detection, validation and mandatory pre-2.0 backup.
- Added transactional 2.0 schema migration preserving canonical XP and recalculating only derived levels.
- Added schema metadata, reserved milestone-claim/statistics tables and canonical row fingerprint validation.
- Stabilized public API 2.0 with additive immutable ability, milestone, mastery and statistics snapshots while retaining 1.x method signatures.
- Stabilized Bukkit event integration with existing compact accessors plus JavaBean getters.
- Fixed logout/shutdown persistence race by tracking all in-flight/detached writes through the shutdown barrier.
- Strengthened reload rollback so prior runtime subsystem scheduling/routes are restored after candidate-application failure.
- Added deterministic leaderboard tie ordering.
- Expanded automated migration, API compatibility and concurrent-profile regression coverage.
- Completed README, configuration, commands/permissions, PlaceholderAPI, native migration, performance, staging and recovery documentation.
- Updated RC workflow so prerelease can be produced from the Phase 2 branch without merging PR #2; stable publication remained exact-tag gated at that stage.

### Runtime certification

Manual PlexonCraft GUI, real migration rehearsal, Spark comparison and 30-minute soak were `NOT EXECUTED` at RC publication. This historical candidate was not stable-certified.

## 1.0.0 — candidate implementation

- Added all thirteen required skill domains.
- Added precomputed POWER progression tables with canonical total-XP state.
- Added PlexonCore block-break subscription and Core-owned block-origin anti-exploit routing.
- Added narrow combat, fishing, acrobatics, repair and alchemy listeners for event families not centralized by Core.
- Added in-memory player profiles with generation-safe asynchronous loading and coalesced SQLite persistence.
- Added public Bukkit Services API and XP/level/ability events.
- Added `/skills`, `/skillsadmin`, GUI, diagnostics, backup and PlaceholderAPI expansion.
- Added SHADOW/PRIMARY migration modes and schema-safe clean-room mcMMO migration coordination.
