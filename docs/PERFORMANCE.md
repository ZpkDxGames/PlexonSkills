# Performance design and validation

## Design constraints

- Core owns the BlockBreakEvent listener and block-origin lookup.
- Block material routes are precompiled.
- XP curves are precomputed.
- Profiles use ordinal-indexed state instead of YAML/database reads.
- Persistence is dirty/coalesced and asynchronous.
- Actionbar XP feedback is coalesced.
- No one-async-task-per-event fanout.

## Stable measurement gate

Collect comparable Spark captures for:

- current mcMMO production baseline;
- PlexonSkills SHADOW + mcMMO (interpret as dual-engine overhead);
- PlexonSkills PRIMARY with mcMMO progression disabled.

Record MSPT P50/P95/P99, main-thread CPU, block/combat callback self-time, Core origin time, persistence pressure, allocation behavior, 1/5/10 miner loads and a 30-minute mixed soak.

CI cannot manufacture server-side Spark evidence; release notes must state it as pending until measured on staging/production-like hardware.
