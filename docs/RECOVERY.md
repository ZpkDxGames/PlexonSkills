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

Active effects and cooldown timers remain runtime state rather than durable database progression. Runtime reload intentionally resets transient ability/cooldown state and recreates the single shared expiration task.

Normal player logout is different: it ends the player's active effect, but a configured cooldown deadline remains authoritative across reconnect until it expires or an administrator explicitly clears it. The shared sweep also expires cooldown-only offline state so reconnect cannot be used to bypass the configured cooldown.

## Release rollback

Historical `v2.0.0-rc.1` and `v2.0.0-rc.2` assets are immutable and must never be replaced.

The stable 2.0 rollback artifact is:

- tag: `v2.0.0-rc.2`
- source: `fd1f15205353f91e42236865b8dd6141d46b3634`
- JAR: `PlexonSkills-2.0.0.jar`
- SHA-256: `af31444e428d2f547ec9c8f5db49aaffeac4594efbe2c3c24e90e74dcb38bf07`

If a stable release defect is found, preserve the published stable tag/assets and open a new remediation version rather than rewriting history. Live PlexonCraft deployment certification is tracked separately in `STAGING.md`; `runtime_certification=NOT_EXECUTED` in stable release provenance means only that live certification has not yet been performed.
