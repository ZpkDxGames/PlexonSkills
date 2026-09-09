# Configuration

PlexonSkills 2.0 separates global runtime policy from skill, ability, milestone/reward and message definitions.

## Files and reload boundaries

- `config.yml` — progression curve, migration mode, feedback, persistence, anti-exploit and world/gamemode policy.
- `skills.yml` — enabled skills, XP sources and domain definitions.
- `abilities.yml` — global/per-skill active ability definitions and bounds.
- `rewards.yml` — progression milestones and milestone-derived passive bonuses.
- `messages.yml` — presentation/localization text.

`/skillsadmin reload` loads and validates a complete candidate runtime on the primary thread. Invalid critical configuration rejects the reload. If applying the candidate to a runtime subsystem fails, PlexonSkills restores the previous runtime and rebuilds/reschedules the previous subsystem state.

The admin GUI intentionally permits only narrow boolean mutations in `skills.yml`, `abilities.yml` and `rewards.yml`. Complex balancing remains YAML-controlled.

## `config.yml`

### Migration

```yaml
migration:
  mcmmo:
    mode: SHADOW
    source: ''
    strategy: PRESERVE_LEVEL
```

`migration.mcmmo.*` controls the separate clean-room mcMMO compatibility path. It does **not** disable the automatic native PlexonSkills 1.x/prerelease database schema migration.

Modes:

- `SHADOW` — evaluate PlexonSkills progression without making its XP ledger authoritative.
- `PRIMARY` — PlexonSkills applies authoritative progression.
- `DISABLED` — progression grants are rejected.

An unknown mode rejects configuration loading.

### Progression

```yaml
progression:
  maximum-level: 1000
  curve:
    type: POWER
    base: 100
    exponent: 1.45
```

2.0 currently validates `POWER` as the supported curve. Existing canonical total XP is preserved during native schema migration; derived levels are recalculated against this configured curve. Changing curve parameters therefore changes level interpretation without rewriting canonical XP.

### Feedback

```yaml
feedback:
  actionbar: true
  actionbar-coalesce-ticks: 8
  level-up-title: true
```

XP actionbar feedback is coalesced. The coalesce value is clamped to at least one tick.

### Persistence

```yaml
persistence:
  flush-interval-ticks: 200
  shutdown-timeout-seconds: 5
```

- Flush interval is validated/clamped to at least 20 ticks.
- Shutdown timeout must be 1–30 seconds.
- Writes are serialized through PlexonCore SQLite.
- Logout writes remain tracked until completion; shutdown waits on all tracked writes plus a final snapshot batch before database close.

### Block provenance

```yaml
anti-exploit:
  block-origin:
    enabled: true
    unknown-policy: REJECT
```

Natural-only block sources reject `PLAYER_PLACED`. `REJECT` makes unknown provenance fail closed.

### Farming

```yaml
anti-exploit:
  farming:
    player-planted-policy: MATURE_ONLY
    allow-unknown-origin: false
```

Farming does not blindly inherit Mining's natural-block rule. Mature crop validation remains mandatory and the planted crop policy is explicit.

### Combat

```yaml
anti-exploit:
  combat:
    allow-pvp: false
    exclude-npcs: true
    spawner-xp-multiplier: 0.25
    breeding-xp-multiplier: 0.50
    custom-xp-multiplier: 0.0
    same-target-minimum-interval-ms: 250
```

The multipliers are provenance policy, not database queries. PvP is disabled by default.

### Other attribution/rate gates

```yaml
anti-exploit:
  fishing:
    minimum-catch-interval-ms: 1000
  acrobatics:
    minimum-event-interval-ms: 1500
  repair:
    minimum-event-interval-ms: 250
  alchemy:
    attribution-window-ms: 120000
```

### Allowed game modes

```yaml
anti-exploit:
  game-modes:
    - SURVIVAL
```

Unknown Bukkit game modes reject configuration loading. If the list is empty, SURVIVAL is used.

### Worlds

```yaml
worlds:
  mode: BLACKLIST
  values:
    - lobby
```

`mode` is `BLACKLIST` or `WHITELIST`; values are matched case-insensitively.

### Leaderboards

```yaml
leaderboards:
  cache-seconds: 30
```

Leaderboard reads remain asynchronous and UI callbacks are session/generation checked before applying results.

## Abilities

Every active ability definition is validated for identity, skill, enabled state, unlock level, duration, cooldown and XP multiplier. Duration is bounded to 1–600 seconds; cooldown must be at least the duration and at most 24 hours; XP multiplier is bounded to 1.0–3.0.

## Milestones/rewards

Milestone levels must be valid for the configured maximum level. Passive XP bonus values are bounded and then differentiated by the skill's passive coefficient. The current 2.0 milestone system is derived from level state and therefore does not require mutable player claim rows for passive-only milestones; the 2.0 schema reserves claim storage for future transactional rewards.

## Operational rule

Back up the plugin data directory before hand-editing production configuration. Use `/skillsadmin diagnostics` immediately after reload to verify runtime epoch, migration mode, persistence queue/health and product schema state.
