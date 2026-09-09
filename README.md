# PlexonSkills

PlexonSkills is the first-party, Core-native RPG skill/progression system for PlexonCraft. Version 1.0.0 is designed as an incremental replacement for mcMMO gameplay progression without copying or porting mcMMO implementation code.

## Platform

- Paper 26.2
- Java 25
- PlexonCore 2.0.0 / Core API 2.0 required
- PlaceholderAPI optional

## 1.0 skill set

Mining, Woodcutting, Excavation, Farming, Fishing, Swords, Axes, Archery, Unarmed, Taming, Acrobatics, Repair and Alchemy.

Block progression uses PlexonCore's shared block-break gateway and authoritative origin classification. Combat, fishing, fall, repair and brewing use narrow local listeners because Core 2.0.0 does not yet expose shared gateways for those event families.

## Safety defaults

- Migration mode defaults to `SHADOW`.
- Natural-only block skills reject `PLAYER_PLACED` and fail closed on `UNKNOWN` origin.
- Farming rewards mature harvests rather than applying Mining's natural-only policy.
- Profiles are authoritative in memory; SQLite writes are coalesced.
- No database/file I/O occurs in the block-break hot path.
- Abilities ship disabled until production balancing/staging evidence exists.

## Build

CI pins PlexonCore 2.0.0 by SHA-256 and builds on Java 25:

```bash
gradle --no-daemon clean test check javadoc shadowJar verifyDistribution writeSha256
```

The installable output is `build/libs/PlexonSkills-1.0.0.jar`.

## Release status

The source can be released as a **candidate** after CI is green. Stable `v1.0.0` additionally requires real server migration, SHADOW/PRIMARY staging, Spark/performance and soak evidence described in [docs/STAGING.md](docs/STAGING.md). The repository does not fabricate those results.

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [API](docs/API.md)
- [Configuration](docs/CONFIGURATION.md)
- [mcMMO migration](docs/MIGRATION_MCMMO.md)
- [Performance](docs/PERFORMANCE.md)
- [Staging/release gate](docs/STAGING.md)
- [Recovery](docs/RECOVERY.md)
