# Staging and stable-release gate

A green CI build makes this repository **candidate ready**, not automatically production-stable.

Before exact stable tag `v1.0.0`, verify on a production-like Paper 26.2 server:

- Core 2 API integration and module registration;
- profile async load, logout flush and shutdown flush;
- Mining, Woodcutting, Excavation, Farming, Fishing, combat and Acrobatics scenarios;
- player-placed/unknown origin rejection and other anti-exploit policies;
- enabled abilities, if any;
- PlaceholderAPI and public API/events;
- real mcMMO migration dry-run and execution on backup/staging data;
- SHADOW comparison;
- PRIMARY staging with mcMMO progression disabled;
- Spark/performance comparison;
- 1/5/10 miner load, combat profile and 30-minute mixed soak;
- rollback/recovery exercise.

Do not fabricate missing evidence. Until these pass, publish only a prerelease candidate such as `v1.0.0-rc.1`.
