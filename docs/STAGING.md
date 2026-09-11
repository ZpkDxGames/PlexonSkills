# Staging and Runtime Acceptance

This file is the authoritative **live PlexonCraft deployment-certification** checklist for PlexonSkills 2.0. It is intentionally separate from reproducible GitHub source/release closure.

Stable `v2.0.0` may therefore carry `runtime_certification=NOT_EXECUTED` in release provenance while still requiring exact-source CI, accepted lineage, distribution verification, immutable stable publication, and downloaded-asset checksum/provenance verification.

## Current runtime status

Automated source/release verification: run through GitHub Actions for each accepted commit and final stable publication.

PlexonCraft/manual runtime validation currently remains:

```text
GUI/navigation validation:        NOT EXECUTED
real 1.x/prerelease migration:    NOT EXECUTED
Spark baseline/comparison:        NOT EXECUTED
single-block progression load:    NOT EXECUTED
combat progression load:          NOT EXECUTED
GUI/leaderboard stress:           NOT EXECUTED
reload/restart persistence:       NOT EXECUTED
30-minute soak:                   NOT EXECUTED
```

These entries must not be changed to PASS without actual runtime evidence.

## Required environment

- production-like Paper 26.2 server;
- Java 25;
- PlexonCore 2.0.4;
- exact stable PlexonSkills JAR and matching checksum;
- representative config files;
- representative copy of any 1.x/prerelease `skills.db` used for migration testing;
- PlaceholderAPI if used in production;
- normal PlexonCraft integrations used alongside skills.

Never perform the first legacy migration rehearsal against the only production database copy.

## Functional GUI matrix

Execute and record PASS/FAIL for:

```text
/skills open
root cards for all 13 skills
player profile
all skill detail screens
progression roadmap
ability screens
leaderboard screen
statistics screen
help screen
back navigation
close navigation
rapid click
shift click
drag
inventory close
quit/rejoin while menu is open
async leaderboard completion after menu navigation/close
max-level mastery presentation
admin root and child screens
admin confirmation flow for destructive actions
```

Validate the PlexonTools-derived Phase 2 visual grammar on the target resource/client environment: compact card spacing, non-italic lore, domain gradients, progress bars, semantic rows, hover/name legibility and no stale inventory writes.

## Ability/milestone matrix

For each skill where feasible:

```text
LOCKED state below unlock level
READY state at/above unlock level
activation
ACTIVE state and effect
shared expiration
end event
COOLDOWN state
cooldown expiry back to READY
admin cooldown clear/end action
reload while transient ability state exists
logout while ACTIVE ends the active effect
logout/reconnect during COOLDOWN preserves the original cooldown deadline
expired offline cooldown is cleaned without counting the player as ACTIVE
```

Cross at least one configured milestone naturally and through an administrator XP operation. Verify milestone event, passive change, presentation and no duplicated effects after restart.

## Migration matrix

Using a representative legacy database copy:

1. record sample player/skill canonical XP values and row counts;
2. start the stable artifact;
3. verify `skills-pre-2.0-*.db` backup exists;
4. verify startup reaches `CURRENT_2_0`;
5. compare canonical XP samples and aggregate row counts with the source;
6. verify derived levels match the configured curve;
7. restart again and verify migration is idempotent;
8. intentionally test an invalid copy on staging and confirm startup refuses it without source mutation.

## Persistence/reload matrix

Exercise:

```text
XP gain then immediate logout
rapid logout/rejoin
XP gain then clean shutdown
multiple dirty profiles then shutdown
restart and verify exact XP
valid /skillsadmin reload
invalid config reload rejection
runtime-application failure/rollback where safely reproducible
backup command while normal writes exist
```

At shutdown, no persistence write should continue after SQLite is closed. Diagnostics should not show growing detached/in-flight writes during normal operation.

## Placeholder/API matrix

Validate representative global and per-skill PlaceholderAPI identifiers from `PLACEHOLDERAPI.md`. Confirm unloaded/not-ready profile behavior does not stall the server.

With a small consumer test plugin, discover `PlexonSkillsAPI` through ServicesManager, read an immutable snapshot, perform a main-thread XP mutation, and observe the XP/level/ability event contracts.

## Performance matrix

Capture a baseline from the previous production/accepted build and compare stable 2.0 under equivalent load:

```text
idle
single-block Mining/Excavation progression
moderate block progression
combat progression
farming harvest progression
GUI open/close stress
leaderboard repeated open
ability expiration with multiple players
persistence flush window
```

Collect:

```text
MSPT
TPS
heap trend
thread count
PlexonSkills/PlexonCore hot-path samples
persistence queue depth
in-flight/detached persistence counts
active GUI sessions
```

Run a minimum 30-minute soak with representative progression and menu activity. Verify no monotonic heap/session/task growth and no write-queue accumulation.

## Deployment decision

Production cutover is considered runtime-certified only when:

- the exact stable artifact is used;
- all required manual functional rows pass;
- real migration rehearsal passes where applicable;
- Spark/performance comparison is acceptable;
- persistence/reload/restart checks pass;
- 30-minute soak passes;
- no unresolved HIGH/CRITICAL runtime defects remain.

A failure discovered here starts a new defect/remediation cycle. It does not alter or rewrite the provenance of the already-published exact stable GitHub artifact.
