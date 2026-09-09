# Changelog

## 2.0.0 — Phase 2 premium-tier rebuild

Development branch: `agent/2.0.0-premium-tier-product-rebuild`.

- Align build and CI with PlexonCore 2.0.4.
- Rebuild the player product layer around premium GUI navigation, meaningful milestones and active abilities.
- Preserve the Core-native progression, persistence and anti-exploit foundation from 1.0.
- Add safe 1.x -> 2.0 migration, premium diagnostics, updated API/PAPI surface and immutable release provenance.

This section remains in development until the 2.0.0 acceptance checklist is complete.

## 1.0.0 — candidate implementation

- Added all thirteen required skill domains.
- Added precomputed POWER progression tables with canonical total-XP state.
- Added PlexonCore 2.0 block-break subscription and Core-owned block-origin anti-exploit routing.
- Added narrow combat, fishing, acrobatics, repair and alchemy listeners for event families not centralized by Core 2.0.0.
- Added in-memory player profiles with generation-safe asynchronous loading and coalesced SQLite persistence.
- Added public Bukkit Services API and XP/level/ability events.
- Added `/skills`, `/skillsadmin`, GUI, diagnostics, backup and PlaceholderAPI expansion.
- Added SHADOW/PRIMARY migration modes and a deliberately schema-safe mcMMO migration coordinator that refuses to guess unknown source schemas.
- Added candidate release automation, deterministic JAR verification and SHA-256 generation.

Stable 1.0.0 remains gated on production-like migration, SHADOW/PRIMARY, Spark and soak evidence.
