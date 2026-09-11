# PlexonSkills Public API 2.0

## Service discovery

PlexonSkills registers `com.zpkdxgames.plexonskills.api.PlexonSkillsAPI` in Bukkit's `ServicesManager` at normal priority. Consumers should discover the service at runtime rather than constructing implementation classes.

```java
RegisteredServiceProvider<PlexonSkillsAPI> registration =
    Bukkit.getServicesManager().getRegistration(PlexonSkillsAPI.class);
if (registration == null) return;
PlexonSkillsAPI skills = registration.getProvider();
```

`PlexonSkillsAPI.API_VERSION` and `apiVersion()` report the API contract line (`2.0`), not the plugin artifact version.

## Threading contract

Loaded-profile reads are async-safe and do not perform synchronous persistence I/O:

- `getLevel(UUID, SkillType)`
- `getTotalXp(UUID, SkillType)`
- `getTotalLevel(UUID)`
- `profile(UUID)`
- `isProfileReady(UUID)`
- `ability(UUID, SkillType)`
- `milestones(SkillType)`
- `isMastered(UUID, SkillType)`
- `statistics(UUID)`

Authoritative mutations are **main-thread-only**:

- `addXp(UUID, SkillType, long, String)`
- `setXp(UUID, SkillType, long, String)`

Calling a mutation asynchronously is a contract violation and throws `IllegalStateException`.

## Compatibility

The original 1.x API methods and signatures remain unchanged. 2.0 additions are interface default methods so an alternate provider compiled against the previous service contract is not forced to implement new methods immediately.

2.0 read models are immutable snapshots:

- `PlayerSkillView`
- `SkillProgressView`
- `AbilityView`
- `MilestoneView`
- `SkillStatisticsView`
- `AbilityState`

No mutable `PlayerSkillsProfile` implementation is exposed.

## Events

All PlexonSkills Bukkit events are synchronous because the authoritative gameplay mutation path is primary-thread-only.

### `PlexonSkillXpGainEvent`

Cancellable pre-mutation event. Cancellation prevents the XP mutation. It exposes player, skill, accepted amount, source, projected total XP, current level and transaction ID.

The event keeps its existing compact accessors (`player()`, `skill()`, `amount()`, etc.) and adds conventional JavaBean getters (`getPlayer()`, `getSkill()`, `getAmount()`, etc.).

### `PlexonSkillLevelUpEvent`

Post-mutation event when a single authoritative mutation increases a skill level. It exposes old/new levels, final canonical XP and source.

### `PlexonSkillAbilityActivateEvent`

Cancellable event fired immediately before an ability enters ACTIVE state. Cancelling leaves the ability unactivated.

### `PlexonSkillAbilityEndEvent`

Post-state event fired when an active ability expires or is ended administratively while its cooldown is preserved.

### `PlexonSkillMilestoneEvent`

Post-level-mutation event fired for each configured milestone crossed by that mutation. The milestone definition is immutable.

## Source identifiers

The `source` string on XP/level operations is an integration attribution field. Consumers should use a stable, namespaced value when possible, for example `MYPLUGIN_QUEST_REWARD`, and must not treat it as player-controlled text.

## Failure semantics

A boolean mutation result of `false` means the requested authoritative mutation did not apply. Common causes include an unloaded/not-ready profile, disabled skill/runtime mode, invalid amount, world/gamemode gate, cancellation by another plugin or a no-op clamped value.

Read methods return zero/empty values for profiles that are not loaded. Consumers that require authoritative data should first check `isProfileReady(UUID)`.
