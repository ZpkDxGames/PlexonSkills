# Recovery and Rollback

## Before changing production state

Create and verify a backup with:

```text
/skillsadmin backup
```

Native 1.x/prerelease -> 2.0 startup migration also creates a mandatory `backups/skills-pre-2.0-<timestamp>.db` copy before any 2.0 schema change.

## Failed 2.0 schema migration

If startup refuses or rolls back migration:

1. keep the server stopped;
2. copy the current/failed `skills.db` elsewhere for diagnosis;
3. retain the automatic pre-2.0 backup;
4. inspect the startup error and `/logs` before retrying;
5. correct only the identified schema/config problem on a staging copy;
6. rerun migration on staging;
7. restore the verified backup before another production attempt if the source database was altered outside PlexonSkills.

The built-in migration transaction is fail-closed: canonical XP fingerprint mismatch throws and rolls back. Unknown/partial schemas are refused before migration.

## Restoring a pre-2.0 database

For a complete rollback to 1.x/prerelease:

1. stop Paper cleanly;
2. preserve the current 2.0 database separately;
3. restore the chosen `skills-pre-2.0-*.db` as `skills.db`;
4. restore the matching legacy plugin/configuration;
5. start on staging and verify sample canonical XP values first.

Do not use a 2.0 database with a legacy binary unless that legacy build is known to tolerate the 2.0 Core schema metadata/tables.

## Persistence failure during normal operation

Run `/skillsadmin diagnostics` and inspect:

- Core SQLite health;
- Core write queue;
- PlexonSkills in-flight write count;
- detached-pending logout snapshot count;
- persistence failure counter.

If health is `FAILED` or queues grow continuously, stop generating administrative mutations, capture logs and perform a clean shutdown. The shutdown barrier waits for tracked writes up to `persistence.shutdown-timeout-seconds`; a timeout/failure is logged explicitly.

Do not delete `skills.db-wal`/`skills.db-shm` while the server is running. PlexonCore performs WAL checkpointing for backups.

## Rejected runtime reload

A validation/application failure returns:

```text
Reload rejected; previous runtime remains active.
```

PlexonSkills restores the previous immutable runtime and rebuilds/reschedules the previous routes/shared coordinators. If rollback itself encounters a secondary failure, the module is marked failed and the server log must be inspected before another reload.

For an admin-GUI boolean mutation, the file value is also restored when the candidate runtime rejects it. A failed file rollback emits a high-severity warning.

## Ability/transient state

Active abilities and cooldowns are intentionally transient. Reload clears transient ability state and recreates the single shared expiration task. Quit removes that player's transient ability state. Do not treat active/cooldown state as durable progression.

## Release rollback

Prerelease `v2.0.0-rc.1` is immutable. Do not overwrite RC assets. If a release-blocking problem is found, fix it on the Phase 2 branch and issue a new RC tag/release rather than replacing the old binary.

Stable `v2.0.0` must not be published until `STAGING.md` is fully satisfied with real runtime evidence.
