# Architecture

## Runtime ownership

```text
Paper BlockBreakEvent
  -> PlexonCore 2 CoreEventGateway
  -> immutable CoreBlockBreakContext + Core BlockOrigin
  -> PlexonSkills CoreBlockSkillsRuntime
  -> SkillProgressionService
  -> in-memory profile
  -> coalesced persistence / public events / feedback
```

PlexonSkills does not register a second BlockBreakEvent progression listener. Material routes are compiled once and Core subscribes only to relevant materials.

Core 2.0.0 currently centralizes block-break events. Event families without a Core gateway use narrow PlexonSkills-owned listeners: combat damage, fishing result, fall damage, repair completion and brewing completion. They remain synchronous for authoritative gameplay mutation and avoid task-per-event fanout.

## Progression state

Total XP per skill is canonical. Level is derived from a precomputed cumulative XP table and cached in atomic ordinal-indexed arrays. Online profiles are authoritative in memory.

## Persistence

PlexonSkills uses the Core `SqliteService` and a module-owned `skills.db`. Schema migrations are transactional. Dirty players are coalesced and flushed in batches; event handlers never wait on SQLite.

## Threading

- XP mutation: main thread only.
- Bukkit world/entity/inventory mutation: main thread only.
- Profile/leaderboard database reads: Core IO lane.
- Serialized writes: Core SQLite writer.
- Public profile snapshots: immutable and async-readable.

## Origin policy

Mining, Woodcutting and Excavation require `NATURAL` origin by default. `PLAYER_PLACED` and `UNKNOWN` are rejected. Farming instead validates mature harvest semantics so player-planted crops can progress without place/break exploits.
