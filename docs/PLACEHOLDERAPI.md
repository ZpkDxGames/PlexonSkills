# PlaceholderAPI

PlexonSkills registers a persistent PlaceholderAPI expansion when PlaceholderAPI is enabled.

Identifier: `plexonskills`

All placeholders resolve from the loaded in-memory profile and immutable runtime configuration. Placeholder resolution never performs a synchronous SQLite query. If the profile is not ready, the resolver returns `0` for a valid request rather than blocking the calling thread.

## Global placeholders

```text
%plexonskills_total_level%
%plexonskills_total_xp%
%plexonskills_highest_skill%
%plexonskills_highest_skill_level%
```

## Per-skill placeholders

Replace `<skill>` with one of:

```text
mining
woodcutting
excavation
farming
fishing
swords
axes
archery
unarmed
taming
acrobatics
repair
alchemy
```

Available suffixes:

| Placeholder | Value |
|---|---|
| `%plexonskills_<skill>_level%` | Current derived level |
| `%plexonskills_<skill>_xp%` | Canonical lifetime XP |
| `%plexonskills_<skill>_xp_required%` | XP required for next level; 0 at mastery |
| `%plexonskills_<skill>_xp_next%` | Compatibility alias of `xp_required` |
| `%plexonskills_<skill>_progress%` | Current-level completion percentage, one decimal |
| `%plexonskills_<skill>_stage%` | Premium progression stage label |
| `%plexonskills_<skill>_next_milestone%` | Next milestone name and level, `NONE`, or `FULLY MASTERED` |
| `%plexonskills_<skill>_mastery%` | Overall mastery percentage, one decimal |
| `%plexonskills_<skill>_mastered%` | `true`/`false` |
| `%plexonskills_<skill>_passive_bonus%` | Current milestone-derived passive percentage, one decimal |
| `%plexonskills_<skill>_ability_state%` | `DISABLED`, `LOCKED`, `READY`, `ACTIVE`, or `COOLDOWN` |
| `%plexonskills_<skill>_ability_cooldown%` | Remaining cooldown in whole seconds, rounded up |
| `%plexonskills_<skill>_rank%` | Compatibility placeholder; currently `0` because live rank is async/cache-backed |

## Examples

```text
%plexonskills_mining_level%
%plexonskills_mining_progress%
%plexonskills_mining_next_milestone%
%plexonskills_mining_ability_state%
%plexonskills_total_level%
```

## Rank behavior

PlexonSkills deliberately does not run a database query from PlaceholderAPI for ranking. `%plexonskills_<skill>_rank%` remains available for compatibility but returns `0`. Use `/skills rank <skill>` or the leaderboard GUI for the asynchronous authoritative rank path.

## Formatting

Numeric percentages use a locale-independent decimal point and one decimal place. Cooldown values never return negative numbers. Invalid identifiers return an empty string.
