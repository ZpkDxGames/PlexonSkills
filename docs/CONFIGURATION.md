# Configuration

Files:

- `config.yml` — migration, progression, persistence, anti-exploit, world/game-mode and feedback policy.
- `skills.yml` — enabled skills and XP material/action tables.
- `abilities.yml` — bounded ability definitions; disabled by default for candidate 1.0.
- `rewards.yml` — milestone rewards.
- `messages.yml` — presentation text.

## Migration mode

```yaml
migration:
  mcmmo:
    mode: SHADOW # DISABLED | SHADOW | PRIMARY
```

`SHADOW` calculates contribution totals without changing visible player progression. `PRIMARY` makes PlexonSkills authoritative.

## XP curve

The POWER curve is compiled at load time. No `Math.pow` work occurs per XP grant.

## Reload

`/skillsadmin reload` parses and validates a complete candidate runtime snapshot before swapping it live. Core block subscriptions are rebuilt only after the replacement snapshot is accepted.
