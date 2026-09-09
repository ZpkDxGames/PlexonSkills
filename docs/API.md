# Public API

PlexonSkills registers `com.zpkdxgames.plexonskills.api.PlexonSkillsAPI` through Bukkit `ServicesManager`.

```java
var registration = Bukkit.getServicesManager().getRegistration(PlexonSkillsAPI.class);
if (registration != null) {
    PlexonSkillsAPI skills = registration.getProvider();
    int mining = skills.getLevel(playerId, SkillType.MINING);
}
```

## Thread contract

Async-safe reads:

- `getLevel`
- `getTotalXp`
- `getTotalLevel`
- `profile`
- `isProfileReady`

Main-thread-only mutations:

- `addXp`
- `setXp`

Calling a mutation asynchronously throws `IllegalStateException` rather than racing profile state.

## Events

- `PlexonSkillXpGainEvent` — synchronous and cancellable before authoritative XP mutation.
- `PlexonSkillLevelUpEvent` — synchronous, reports old/new level for one grant.
- `PlexonSkillAbilityActivateEvent` — synchronous/cancellable framework event.
- `PlexonSkillAbilityEndEvent` — synchronous framework event.

Bulk migration is intentionally not implemented as noisy per-level gameplay events until the real mcMMO source schema is supplied and staged.
