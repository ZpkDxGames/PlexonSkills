# Native PlexonSkills 1.x / prerelease -> 2.0 Migration

This document covers migration of PlexonSkills' own SQLite database. It is separate from the clean-room mcMMO migration path documented in `MIGRATION_MCMMO.md`.

## What is authoritative

Canonical state is the `total_xp` value for each `(player_uuid, skill)` row. The stored `level` column is derived/cacheable state and is recalculated against the active 2.0 XP curve during schema migration.

2.0 does not silently convert or normalize canonical XP.

## Startup flow

Before applying any Core schema migration, PlexonSkills performs a read-only inspection of `plugins/PlexonSkills/skills.db`.

Possible states:

- `EMPTY` — fresh database; create the current schema.
- `LEGACY_1_X` — recognized 1.x/prerelease schema; mandatory backup then migrate.
- `CURRENT_2_0` — current schema; migrations are idempotently skipped.
- `INCOMPATIBLE` — startup is refused without modifying the source schema.

A compatible legacy database must contain the required `players` and `skill_progress` columns. Every legacy progression row must contain:

- a valid UUID;
- a known PlexonSkills skill identifier;
- non-negative canonical XP.

Partial schemas, unknown skills and malformed rows fail closed.

## Mandatory backup

For `LEGACY_1_X`, startup creates:

```text
plugins/PlexonSkills/backups/skills-pre-2.0-<timestamp>.db
```

The backup is created through PlexonCore SQLite after a WAL checkpoint. If the backup fails, 2.0 migration is not attempted and plugin enable fails safely.

The successful backup path is recorded in `migration_meta` as `pre_2_0_backup`.

## Transactional schema migration

PlexonCore's migration coordinator runs schema version 2 in one transaction. It:

1. fingerprints canonical player/progression rows;
2. validates legacy rows again inside the migration transaction;
3. creates `milestone_claims` for future transaction-safe claimable rewards;
4. creates `skill_statistics` for bounded/persisted statistics extensions;
5. recalculates the derived `level` column using the configured `XpCurve`;
6. writes product schema/version/strategy metadata;
7. fingerprints canonical rows again;
8. rolls back if player/progression row counts or canonical XP hash changed.

Core writes its own transactional migration version in `plexon_schema`. PlexonSkills writes product metadata in `migration_meta`.

Expected product metadata includes:

```text
product_schema = 2
product_version = 2.0.0
xp_migration_strategy = PRESERVE_TOTAL_XP_RECALCULATE_LEVEL
schema_migrated_at = <epoch millis>
pre_2_0_backup = <path>    # when legacy migration occurred
```

## XP curve changes

The 2.0 migration strategy is intentionally:

```text
PRESERVE_TOTAL_XP_RECALCULATE_LEVEL
```

If an administrator changes POWER curve parameters before first 2.0 startup, the same canonical XP can correspond to a different derived level. This is expected and does not constitute XP loss. Test curve changes on staging first.

## Verification after startup

Run:

```text
/skillsadmin diagnostics
```

Confirm:

- product schema is `CURRENT_2_0`;
- expected progression row count is shown;
- a pre-2.0 backup path is present when migrating legacy data;
- persistence health is not `FAILED`;
- queue/in-flight/detached write counts settle normally.

Also inspect representative players with `/skills stats` and compare canonical XP to the backup before enabling production progression.

## Rollback

1. Stop the server cleanly.
2. Preserve the failed/current `skills.db` for investigation.
3. Restore the timestamped `skills-pre-2.0-*.db` backup to `skills.db`.
4. Restore the matching 1.x/prerelease plugin/config if rolling the entire deployment back.
5. Start on staging first and verify representative profiles.

Do not merge individual tables between database copies manually unless you have a tested recovery procedure.

## Automated coverage

The 2.0 test suite constructs a real legacy SQLite database through the Core 2.0.4 SQLite runtime and verifies:

- legacy detection;
- incompatible unknown-skill refusal;
- backup creation;
- transactional version advancement;
- 2.0 table/meta creation;
- derived-level recalculation;
- exact canonical XP fingerprint preservation.

Automated coverage does not replace a production-copy migration rehearsal.

## Stable release gate

Stable `v2.0.0` requires a real PlexonCraft/staging migration rehearsal using a representative 1.x/prerelease database copy. Until that has actually been executed and passed, migration runtime validation status is `NOT EXECUTED` and only a prerelease candidate may be published.
