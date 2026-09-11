# Changelog

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
- Updated RC workflow so prerelease can be produced from the Phase 2 branch without merging PR #2; stable publication remains exact-tag gated.

### Runtime certification

Manual PlexonCraft GUI, real migration rehearsal, Spark comparison and 30-minute soak remain `NOT EXECUTED` at RC publication. This candidate is not stable-certified.

## 2.0.0 — stable target

Stable 2.0.0 remains unreleased. It is permitted only after every required manual/staging gate in `docs/STAGING.md` has actually passed.

## 1.0.0 — candidate implementation

- Added all thirteen required skill domains.
- Added precomputed POWER progression tables with canonical total-XP state.
- Added PlexonCore block-break subscription and Core-owned block-origin anti-exploit routing.
- Added narrow combat, fishing, acrobatics, repair and alchemy listeners for event families not centralized by Core.
- Added in-memory player profiles with generation-safe asynchronous loading and coalesced SQLite persistence.
- Added public Bukkit Services API and XP/level/ability events.
- Added `/skills`, `/skillsadmin`, GUI, diagnostics, backup and PlaceholderAPI expansion.
- Added SHADOW/PRIMARY migration modes and schema-safe clean-room mcMMO migration coordination.
