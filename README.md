# PlexonSkills

PlexonSkills is PlexonCraft's first-party, Core-native RPG progression system. The 2.0 line is a premium-tier rebuild with 13 skills, session-safe inventory UI, active abilities, milestone-derived passives, asynchronous leaderboards, administration tooling, PlaceholderAPI integration and conservative anti-exploit provenance rules.

## Requirements

- Paper 26.2
- Java 25
- PlexonCore 2.0.4 / Core API 2.0
- PlaceholderAPI 2.12.1 optional
- PlexonBlacksmith optional

PlexonCore is a hard runtime dependency. Do not install a PlexonSkills 2.0 build against an older Core runtime.

## Skills

Mining, Woodcutting, Excavation, Farming, Fishing, Swords, Axes, Archery, Unarmed, Taming, Acrobatics, Repair and Alchemy.

The player-facing presentation follows the PlexonTools Phase 2 language: compact MiniMessage cards, domain-specific gradients, explicit non-italic lore, semantic icon/label/value rows, shared progress rendering and an explicit terminal mastery state.

## Primary commands

- `/skills` — open the premium skill dashboard.
- `/skills <skill>` — show compact skill progress.
- `/skills stats [player]` — loaded-profile statistics for an online player.
- `/skills top [skill]` — asynchronous top 10.
- `/skills rank [skill]` — asynchronous rank lookup.
- `/skills abilities` — open the ability flow.
- `/skills admin` — administrator GUI when permitted.
- `/skillsadmin ...` — scoped administrative, backup, diagnostics and migration commands.

See [Commands & Permissions](docs/COMMANDS_PERMISSIONS.md) for the complete contract.

## Persistence and 1.x migration

PlexonSkills owns `plugins/PlexonSkills/skills.db` through PlexonCore's SQLite service. On startup, 2.0 performs a read-only schema inspection before migration.

For a compatible 1.x/prerelease schema it:

1. validates required tables/columns and canonical XP rows;
2. creates `backups/skills-pre-2.0-<timestamp>.db`;
3. runs Core's transactional schema migration;
4. preserves every `(player_uuid, skill, total_xp)` value;
5. recalculates only the derived stored level against the configured 2.0 XP curve;
6. creates 2.0 milestone/statistics tables and schema metadata;
7. re-checks canonical row count/hash invariants before startup continues.

Unknown/partial schemas, unknown skill identifiers, invalid UUIDs and negative canonical XP fail closed. See [2.0 Migration](docs/MIGRATION_2_0.md). The separate clean-room mcMMO migration remains read-only and schema-refusing unless a validated user-owned mapping exists; see [mcMMO Migration](docs/MIGRATION_MCMMO.md).

## Runtime safety

- Loaded profiles are authoritative in memory and expose lock-free async-safe reads.
- XP/profile mutations are server-primary-thread authoritative.
- SQLite writes are coalesced and serialized by Core.
- Logout writes are tracked; shutdown enqueues final READY snapshots and waits for the complete persistence barrier before closing SQLite.
- Reload validates a candidate runtime before keeping it and restores the previous runtime/subsystem scheduling if application fails.
- Leaderboards never query synchronously from inventory events.
- Ability expiration uses one shared coordinator, not one repeating task per player.
- Natural-only block progression rejects player-placed provenance and can fail closed for unknown origin.

## Public API

`PlexonSkillsAPI` is registered through Bukkit's `ServicesManager`. API contract version is `2.0`. Existing 1.x service methods remain present; 2.0 adds immutable/defaulted ability, milestone, mastery and aggregate-statistics views. Bukkit events retain their existing constructors/compact accessors and also expose conventional JavaBean getters.

See [API](docs/API.md).

## PlaceholderAPI

Identifier: `plexonskills`. Placeholder resolution is strictly loaded-memory/runtime backed and does not synchronously query SQLite. See [PlaceholderAPI](docs/PLACEHOLDERAPI.md).

## Build and verification

CI pins PlexonCore 2.0.4 by SHA-256 and uses Java 25:

```bash
gradle --no-daemon clean test check javadoc shadowJar verifyDistribution writeSha256
```

Distribution outputs:

```text
build/libs/PlexonSkills-2.0.0.jar
build/distributions/SHA256SUMS.txt
```

## Release policy

`v2.0.0-rc.1` is a prerelease candidate only. Stable `v2.0.0` remains blocked until the production-like manual matrix has actually passed on PlexonCraft, including GUI/navigation, real legacy migration, Spark comparison, persistence/reload behavior and soak validation. Missing runtime evidence is reported as `NOT EXECUTED`; it is never inferred from CI.

Documentation:

- [Architecture](docs/ARCHITECTURE.md)
- [API](docs/API.md)
- [Configuration](docs/CONFIGURATION.md)
- [Commands & Permissions](docs/COMMANDS_PERMISSIONS.md)
- [PlaceholderAPI](docs/PLACEHOLDERAPI.md)
- [2.0 Migration](docs/MIGRATION_2_0.md)
- [mcMMO Migration](docs/MIGRATION_MCMMO.md)
- [Performance](docs/PERFORMANCE.md)
- [Staging / Acceptance](docs/STAGING.md)
- [Recovery](docs/RECOVERY.md)
