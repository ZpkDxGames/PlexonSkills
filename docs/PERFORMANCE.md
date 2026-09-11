# Performance and Concurrency

PlexonSkills 2.0 keeps gameplay mutation authoritative on the Paper primary thread while moving persistence/leaderboard work away from hot paths.

## Hot-path rules

- No database or file I/O in block, combat, farming, fishing, repair, alchemy or acrobatics event handlers.
- Skill definitions/material routes are precomputed at runtime load.
- Block progression consumes PlexonCore's shared block-origin gateway rather than duplicating provenance storage.
- XP actionbar feedback is coalesced.
- Ability expiration uses one shared sweep task for active and cooldown-only players; logout cannot reset a configured cooldown.
- PlaceholderAPI reads only loaded in-memory state.
- Leaderboard queries are asynchronous and bounded.

## Profile concurrency model

`PlayerSkillsProfile` stores canonical XP and derived levels in atomic arrays. Mutations are primary-thread-only; reads are lock-free and async-safe. Each mutation increments a revision used by immutable snapshots/session logic.

Automated tests exercise concurrent readers while authoritative mutations occur and verify all observed XP/level values remain within configured bounds.

## Persistence

PlexonCore SQLite provides:

- WAL mode;
- foreign keys;
- busy timeout;
- one bounded serialized write queue per database;
- transactional writes/migrations.

PlexonSkills adds lifecycle tracking around those futures. Periodic dirty-profile flushes and logout writes are registered as in-flight. Logout snapshots are retained in a detached-pending map until a successful write. Shutdown submits a final authoritative batch containing all READY loaded profiles plus detached snapshots, then waits for the tracked persistence barrier before SQLite is closed.

Diagnostics expose Core queue depth plus PlexonSkills in-flight and detached-pending counts.

## Reload behavior

Configuration parsing/validation creates a candidate immutable runtime. On successful application, material routes, shared ability state and feedback/flush scheduling are rebuilt once. If application fails, the previous immutable runtime is restored and the previous subsystem schedule/routes are rebuilt. No per-player repeating reload tasks are created.

## Leaderboards

Database ranking stays outside inventory callbacks. `top` is limited to at most 100 rows and has deterministic ordering (`total_xp DESC`, UUID tiebreak). Premium GUI callbacks must verify player/session generation before applying async results.

## Performance acceptance

Automated GitHub CI proves compilation, tests, distribution contents, dependency isolation, accepted source lineage and deterministic migration contracts. Stable publication additionally rebuilds exact final `main` and verifies the downloaded public release assets.

Those automated gates do not prove production MSPT. PlexonCraft performance certification therefore remains a separate deployment follow-up using `STAGING.md`, including Spark baseline/comparison and a minimum 30-minute soak. Record at least MSPT, TPS, heap trend, thread count, relevant samples, persistence queue depth and active UI sessions.

Until that live work is executed, release provenance must retain `runtime_certification=NOT_EXECUTED`; it is not inferred from CI and does not block reproducible GitHub stable source/release closure.
