# PlexonSkills

PlexonSkills is the first-party, Core-native RPG skill/progression system for PlexonCraft.

The repository is currently developing **PlexonSkills 2.0.0**, the Phase 2 premium-tier product rebuild. The 2.0 line preserves the safe Core-native progression/persistence foundation from the 1.0 candidate while rebuilding the player and administrator experience around meaningful progression, abilities, milestones and premium GUI flows.

## Platform

- Paper 26.2
- Java 25
- PlexonCore 2.0.4 / Core API 2.0 required
- PlaceholderAPI optional

## Skill set

Mining, Woodcutting, Excavation, Farming, Fishing, Swords, Axes, Archery, Unarmed, Taming, Acrobatics, Repair and Alchemy.

Block progression uses PlexonCore's shared block-break gateway and authoritative origin classification. Event families that are not exposed through a shared Core gateway remain handled by narrow PlexonSkills listeners, with duplicate-processing prevention preserved.

## 2.0 product direction

The Phase 2 rebuild targets:

- premium `/skills` navigation;
- player profile and per-skill detail screens;
- milestone-oriented progression roadmaps;
- configurable active abilities and cooldown states;
- milestone rewards and mastery presentation;
- GUI leaderboards and statistics;
- PlexonTools-derived MiniMessage presentation grammar;
- practical administrator tooling;
- safe 1.x -> 2.0 data migration;
- measurable runtime quality without hot-path regressions.

See [`PlexonSkills_2.0.0_PremiumTier_Product_Rebuild_AI_Builder_Specification.md`](PlexonSkills_2.0.0_PremiumTier_Product_Rebuild_AI_Builder_Specification.md) for the complete implementation contract.

## Safety defaults

- Migration mode defaults to `SHADOW` while a legacy migration source is being validated.
- Natural-only block skills reject `PLAYER_PLACED` and can fail closed on `UNKNOWN` origin.
- Farming validates harvest maturity rather than blindly applying Mining's natural-only policy.
- Profiles are authoritative in memory; SQLite writes are coalesced.
- No database/file I/O belongs in the block-break hot path.
- Visual feedback is coalesced rather than emitted once per high-frequency event.

## Build

CI pins PlexonCore 2.0.4 by SHA-256 and builds on Java 25:

```bash
gradle --no-daemon clean test check javadoc shadowJar verifyDistribution writeSha256
```

The 2.0 distribution target is:

```text
build/libs/PlexonSkills-2.0.0.jar
build/distributions/SHA256SUMS.txt
```

## Release status

The `agent/2.0.0-premium-tier-product-rebuild` branch is a development branch. Stable `v2.0.0` requires the dedicated acceptance checklist, green automated verification, safe migration coverage and explicit reporting for runtime-only Spark/soak/manual GUI gates. Runtime evidence is never fabricated.

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [API](docs/API.md)
- [Configuration](docs/CONFIGURATION.md)
- [mcMMO migration](docs/MIGRATION_MCMMO.md)
- [Performance](docs/PERFORMANCE.md)
- [Staging/release gate](docs/STAGING.md)
- [Recovery](docs/RECOVERY.md)
