# Recovery

## Core unavailable

PlexonSkills fails enable cleanly. It does not fall back to a duplicate BlockBreakEvent/origin engine.

## Profile load failure

The profile becomes `DEGRADED`; zero state is not persisted over the failed source row and gameplay XP is suppressed for that player.

## Database pressure/failure

Authoritative online state remains in memory and the player stays dirty for a later coalesced flush. Diagnostics expose queue/health state. Do not leave the server in an unbounded memory-only condition without intervention.

## Interrupted migration

Do not mutate mcMMO source data. Restore the PlexonSkills DB backup and repeat only after inspecting the failure. Additive re-import is not the default.

## Rollback to mcMMO

Stop the server, preserve PlexonSkills data/config, restore the previously backed-up mcMMO files/database, restore command/placeholders as needed, start the server and verify progression before accepting traffic.
