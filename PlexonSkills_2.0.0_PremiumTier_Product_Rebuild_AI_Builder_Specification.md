# PlexonSkills 2.0.0 — Premium-Tier Product Rebuild
## Dedicated AI / Agentic Builder Specification

> **Repository:** `ZpkDxGames/PlexonSkills`
> **Target branch:** `agent/2.0.0-premium-tier-product-rebuild`
> **Target release:** `2.0.0`
> **Platform:** Paper `26.2`
> **Java:** `25`
> **Required Core baseline:** PlexonCore `2.0.4` / Core API `2.0`
> **Core 2.0.4 JAR SHA-256:** `61d625a717da9f46ee9231e1970d84b4c317ae12cf4090cdf7c9d39b6a1a9baf`
> **Phase:** Plexon Ecosystem Phase 2 — Premium-Tier Product Overhaul
> **Role:** first implementation target and reference premium gameplay product

---

# 0. Agent directive

Rebuild PlexonSkills from the current 1.0.0 Core-native candidate into a complete premium-tier skill/progression product.

This is **not** a cosmetic reskin.

The 2.0 release must combine:

```text
correct progression
meaningful gameplay depth
premium GUI UX
clear lore hierarchy
abilities and milestone rewards
safe anti-exploit rules
robust persistence
safe migration
useful administration
stable public API/PAPI
performance discipline
professional defaults
tested immutable release artifacts
```

Do not regress the already-correct Core-native architecture.

Do not mark 2.0.0 stable until automated verification is green and all runtime-only gates are explicitly either executed or reported as `NOT EXECUTED`.

---

# 1. Repository audit — current state

The current `main` branch is a freshly merged PlexonSkills `1.0.0` Core-native **candidate**, not a mature premium release.

Current strengths:

```text
Paper 26.2 / Java 25 build
PlexonCore module registration
Core-owned block-break routing
Core-owned placed/natural block origin integration
13 skill domains
POWER XP curve
in-memory authoritative player profiles
coalesced SQLite persistence
Bukkit Services API
XP/level events
PlaceholderAPI integration
SHADOW / PRIMARY migration framework
backup and diagnostics commands
CI build / test / checksum pipeline
```

Current critical Phase 2 gaps:

```text
GUI is one flat read-only 36-slot inventory
GUI title is plain `PlexonSkills`
GUI item names/lore use plain Component.text
no root/profile/detail/progression/ability navigation model
no premium MiniMessage presentation layer
no dynamic locked/available/completed/maxed GUI states
abilities.yml is globally disabled and contains no production ability definitions
rewards.yml is empty
no meaningful milestone reward system
no skill-stage visual model
no player-facing ability browser
no milestone browser
no GUI leaderboard experience
no statistics/history GUI
no premium completion/mastery presentation
commands remain primarily chat-text utilities
no admin GUI for skill balancing / enable-disable / preview
current build and CI still pin PlexonCore 2.0.0 instead of Phase 2 baseline 2.0.4
current release workflow is hard-coded for 1.0.0
```

This means the 1.0 implementation is a valid architectural foundation but not the target player product.

---

# 2. Phase 2 gap analysis

## CURRENT STATE

- Thirteen fixed skill enum values exist.
- Skill definitions are loaded from `skills.yml`.
- Block materials can map to Mining, Woodcutting, Excavation, and Farming.
- Fishing, combat, Acrobatics, Repair, and Alchemy have local event listeners where Core lacks shared gateways.
- XP is canonical total XP with level derived from `XpCurve`.
- Profiles are memory-authoritative and asynchronously persisted through Core SQLite.
- Migration defaults to `SHADOW`.
- `/skills` opens the current flat menu.
- `/skills stats`, `/skills top`, `/skills rank`, and direct skill lookup return chat text.
- `/skills abilities` currently states that abilities are disabled.

## BROKEN / INCOMPLETE BEHAVIOR

Treat the following as product defects for 2.0 even where the 1.0 candidate is technically functioning:

```text
abilities unavailable as real gameplay
empty rewards configuration
no milestone reward execution
no skill detail navigation
no progression roadmap
no player-facing cooldown presentation
no recent-unlock tracking
no integrated statistics view
no premium leaderboard screen
no meaningful mastery terminal state
```

Any additional runtime defect discovered during implementation must be added to the PR description and fixed before stable release where in scope.

## UX DEFECTS

```text
flat inventory
no back-navigation model
no close control
no stable screen hierarchy
no interaction hints
no per-skill detail cards
no unavailable-state explanation before click
no profile summary card
no pagination framework
no empty-state presentation
no pending/loading inventory state
chat-only leaderboard/rank flows
```

## VISUAL DEFECTS

```text
plain names
plain lore
no explicit <!italic>
no semantic icon hierarchy
no controlled gradients
no stage-aware identity
no shared progress renderer
no completion/mastery visual state
no canonical PlexonTools-derived structure
```

## MISSING FEATURES

```text
milestones
passive bonuses
active abilities
ability upgrades
ability cooldown model
mastery rewards
recent unlocks
skill statistics
profile overview
GUI leaderboards
admin balancing GUI
preview/test utilities
reward execution framework
migration report for 1.x -> 2.0 schema
```

## ADMIN DEFECTS

The existing admin command/diagnostics foundation is useful, but routine gameplay tuning is YAML-only.

2.0 must provide practical administration for:

```text
skill enable/disable
XP multiplier/tuning preview
milestone inspection
ability enable/disable
reward preview
configuration validation summary
player inspect
player XP/level mutation with confirmation where destructive
reload
backup
migration status
diagnostics
```

Do not force every advanced option into GUI. Advanced bulk balancing remains YAML-first.

## PERFORMANCE RISKS

2.0 must not introduce:

```text
one repeating task per player
one task per XP event
one DB write per XP event
full GUI rebuild every tick
per-event YAML reads
unbounded recent-event history
unbounded leaderboard cache
heavy PDC/NBT scans on hot gameplay paths
```

## DATA RISKS

New 2.0 state may include:

```text
milestone claim state
ability cooldown state if persistence is required
statistics counters
recent unlock timestamps/history
schema version metadata
```

All additions require explicit authoritative ownership, schema migration, backup, rollback and shutdown handling.

## TARGET EXPERIENCE

A player running `/skills` should immediately understand:

```text
who they are
what their total progression looks like
which skills are progressing
what each skill currently grants
what unlock is next
what abilities exist
what is ready/cooling down/locked
where they rank
what mastery means
```

An administrator should be able to inspect and operate the system without reading source code.

## OUT OF SCOPE

Unless required by implementation dependencies:

```text
no broad PlexonCore redesign
no unrelated PlexonTools changes
no MMORPG class system
no talent-tree system requiring a separate major product
no arbitrary NBT editor
no economy provider replacement
no database provider rewrite
no copying mcMMO implementation or proprietary assets
```

---

# 3. Core baseline migration — mandatory first technical change

The current project still pins `PlexonCore-2.0.0.jar`.

2.0 must move to:

```text
PlexonCore 2.0.4
Core API 2.0
SHA-256 61d625a717da9f46ee9231e1970d84b4c317ae12cf4090cdf7c9d39b6a1a9baf
```

Update:

```text
build.gradle.kts
.github/workflows/build.yml
.github/workflows/release.yml
README / docs compatibility statements
release notes
```

Use Core owner-aware module state updates when the 2.0.4 public API exposes the overload needed for asynchronous/lifecycle-safe state transitions.

Do not bundle PlexonCore classes.

---

# 4. Versioning / branch strategy

Development version:

```text
2.0.0-SNAPSHOT or 2.0.0-rc.x during branch development
```

Final distribution:

```text
PlexonSkills-2.0.0.jar
SHA256SUMS.txt
```

Recommended branch:

```text
agent/2.0.0-premium-tier-product-rebuild
```

Stable tag:

```text
v2.0.0
```

Never overwrite an existing stable release or artifact.

---

# 5. Canonical skill registry

Retain the thirteen current core domains unless repository/runtime discovery proves a domain is invalid:

```text
Mining
Woodcutting
Excavation
Farming
Fishing
Swords
Axes
Archery
Unarmed
Taming
Acrobatics
Repair
Alchemy
```

Each skill requires a complete `SkillPresentation` / `SkillDefinition` concept containing or deriving:

```text
id
display name
icon
enabled state
domain accent palette
short description
XP sources
maximum level
stage model
passive progression
milestones
active ability definition where applicable
statistics keys
anti-exploit policy
```

Do not hardcode player-facing display strings throughout event logic.

---

# 6. Progression model

Keep canonical total XP as authoritative state.

The existing POWER curve may remain the default if tests and balancing confirm it, but 2.0 configuration must validate progression clearly.

Required fields:

```text
maximum level
XP required for current level
XP required for next level
progress within level
total level
terminal/maxed state
```

A maxed skill must never display misleading `next level` information.

Provide a dedicated terminal state such as:

```text
✦ FULLY MASTERED
```

---

# 7. Progression stages

Add domain-appropriate progression chapters.

Default generic chapter model:

```text
Stage I    Novice
Stage II   Adept
Stage III  Expert
Stage IV   Master
Stage V    Ascendant
Stage VI   Mastery
```

Exact level boundaries are configurable and may differ from PlexonTools.

Suggested default boundaries for a 100-level premium presentation layer:

```text
1–9
10–24
25–49
50–74
75–89
90–99
100 mastery
```

If gameplay maximum remains above 100, presentation chapters must be adapted intentionally rather than pretending level 100 is terminal.

The implementation must choose one coherent model and document it.

Every stage transition should correspond to meaningful gameplay, such as:

```text
passive increase
ability unlock/upgrade
reward milestone
new mechanic
mastery bonus
```

---

# 8. Passive progression

Avoid meaningless `+1% every level` design.

Preferred structure:

```text
small baseline scaling
+ milestone-sized upgrades
+ unlockable mechanics
```

Examples by domain:

```text
Mining       ore-related efficiency / fortune-style bonus within safe server balance
Woodcutting  wood yield / tree utility
Excavation   excavation yield / archaeology-style utility
Farming      harvest yield / replant or crop utility if intentionally enabled
Fishing      catch quality / treasure weighting within configurable bounds
Swords       combat proficiency bonus with PvE-safe defaults
Axes         combat/wood synergy without duplicating Woodcutting progression
Archery      ranged proficiency
Unarmed      melee utility
Taming       companion proficiency
Acrobatics   fall mitigation / movement utility
Repair       repair efficiency / material conservation
Alchemy      brewing efficiency / duration-quality utility where safe
```

Do not alter vanilla mechanics in ways that create duplication or exploits without explicit gates.

---

# 9. Active ability system

The 1.0 placeholder ability framework must become a real, bounded system.

Every ability definition should support:

```text
id
skill
enabled
unlock level
cooldown
duration if relevant
max effect bounds
activation rule
feedback
upgrade milestones
```

Activation must be server-authoritative and main-thread safe.

Required states:

```text
LOCKED
READY
ACTIVE
COOLDOWN
DISABLED BY ADMIN
```

Cooldown UI must not require a per-player repeating task. Use timestamps and coalesced visible refresh only while a relevant menu is open or feedback is active.

Ability activation and end events must remain compatible with the public API where possible.

---

# 10. Milestones and rewards

Replace the empty `rewards.yml` with production-quality defaults.

Milestones may grant:

```text
passive upgrades
ability unlocks
ability upgrades
commands
items
permission-group hooks only through explicit integrations
cosmetic feedback
mastery markers
```

Reward execution requirements:

```text
idempotent claim state
no duplicate claim after restart
atomic state transition
main-thread Bukkit item/command mutation
clear error handling
no silent loss
```

If automatic rewards are used, persist claim state before/with execution using a transaction-safe strategy that prevents double execution after crash recovery.

---

# 11. Root GUI — `/skills`

Target inventory hierarchy:

```text
Skills
├─ Player Profile
├─ skill cards
│  ├─ Mining
│  ├─ Woodcutting
│  ├─ Excavation
│  ├─ Farming
│  ├─ Fishing
│  ├─ Swords
│  ├─ Axes
│  ├─ Archery
│  ├─ Unarmed
│  ├─ Taming
│  ├─ Acrobatics
│  ├─ Repair
│  └─ Alchemy
├─ Leaderboards
├─ Statistics
└─ Help
```

Use a stable layout. Do not fill every empty slot with noisy panes.

Root skill cards should show only scan-friendly information:

```text
identity line
level
stage
XP progress
next milestone
status / mastery
interaction hint
```

---

# 12. Player profile screen

Show:

```text
player head / identity
total skill level
total XP
highest skill
number mastered
recent unlocks
server/global summary if cheap to obtain
```

Avoid synchronous database work during menu open.

Use already-loaded profile data and cached async leaderboard/global data.

---

# 13. Skill detail screen

Each skill detail must expose:

```text
level
current XP
XP to next level
progress bar
current stage
current passive bonuses
active ability
ability state / cooldown
next milestone
recent unlocks
statistics
leaderboard position
```

Use tabs/subpages where needed rather than lore walls.

Primary navigation:

```text
Left Click  -> Progression
Right Click -> Abilities / details
```

Exact click mapping may be adjusted, but it must remain consistent across all skill cards.

---

# 14. Progression roadmap GUI

Render milestone-oriented progression, not one icon per every level.

Suggested visible milestone model:

```text
previous meaningful milestone
current stage
next 3–5 meaningful milestones
mastery target
```

States:

```text
COMPLETED
CURRENT
AVAILABLE
LOCKED
MASTERED
```

Use pagination only when genuinely needed.

---

# 15. Ability GUI

Show per ability:

```text
name
short description
unlock level
current tier
next upgrade
cooldown
state
activation hint
```

Do not expose internal IDs in normal player view.

---

# 16. Leaderboard GUI

Move `/skills top` from chat-only UX into a premium screen while retaining command compatibility.

Required:

```text
skill selector
top entries
player's own rank
level + exact XP
loading state
error state
cache age
```

Leaderboard reads remain async/cached.

Opening a leaderboard must never block the main thread on SQLite.

---

# 17. Statistics GUI

Track only meaningful bounded statistics.

Potential examples:

```text
blocks mined by eligible category
logs harvested
mature crops harvested
fish caught
PvE damage / valid kills
bows hits
fall XP events
repairs completed
brews completed
abilities activated
milestones reached
```

Do not create unbounded per-material maps per player unless a clear storage bound exists.

---

# 18. Canonical presentation language

PlexonTools is the frozen presentation reference.

Rules:

```text
MiniMessage-first
explicit <!italic>
semantic icon -> gray label -> white value
controlled domain gradients
dark-gray structure
compact lore cards
shared progress renderer
terminal mastery state
clear interaction hints
```

Do not copy Tools mining colors mechanically to every skill.

Use domain palettes.

Suggested defaults:

```text
Mining       cyan / diamond
Woodcutting  green / amber
Excavation   sand / orange
Farming      lime / gold
Fishing      aqua / blue
Swords       red / silver
Axes         orange / steel
Archery      green / teal
Unarmed      amber / red
Taming       gold / green
Acrobatics   light blue / white
Repair       steel / cyan
Alchemy      purple / magenta
```

All colors remain configurable.

---

# 19. Root card grammar

Example structure:

```text
<!italic><dark_gray>Skill progression</dark_gray>

<!italic><#FFD740>✥</#FFD740> <gray>Level</gray> <white>{level}</white>
<!italic><#90CAF9>◆</#90CAF9> <gray>Stage</gray> <white>{stage}</white>
<!italic><#FFD54F>⚡</#FFD54F> <gray>Progress</gray> <dynamic>{current}</dynamic><dark_gray>/</dark_gray><#B0BEC5>{required}</#B0BEC5>
<!italic><#66BB6A>▰</#66BB6A> <gray>Completion</gray> <dynamic>{percentage}%</dynamic>

<!italic><gray>Next</gray> <white>{next_milestone}</white>
<!italic><accent>Left Click</accent> <gray>to view progression.</gray>
<!italic><accent2>Right Click</accent2> <gray>to inspect abilities.</gray>
```

Do not exceed roughly 9 meaningful lines for root cards.

---

# 20. Progress renderer

Create one shared skill progress renderer usable by:

```text
GUI cards
detail lore
actionbar
```

Default semantics:

```text
poor progress -> red
mid progress -> amber
complete -> green
```

Recommended compact width:

```text
12–16 cells
```

No per-event recalculation of expensive MiniMessage templates where cached components can safely be reused.

---

# 21. Feedback system

Use:

```text
Actionbar -> XP bursts, ability cooldown/ready where useful
Chat      -> detailed unlock/reward/error information
Title     -> major milestone/mastery only
Bossbar   -> only genuinely timed active ability/process if useful
```

Existing coalesced XP actionbar behavior should be preserved and upgraded rather than replaced with event spam.

Feedback must be configurable.

---

# 22. Anti-exploit requirements

Mandatory:

```text
player-placed block rejection where natural origin is required
UNKNOWN block origin fail-closed policy when configured
mature-crop validation
player-planted crop policy explicitly configurable
spawner-mob XP policy
repeated target farming controls
AFK abuse controls where feasible
NPC exclusion
cancelled-event exclusion
PvP default-safe policy
ArmorStand exclusion
world / gamemode gates
no duplicate Core/local processing
```

Combat anti-farm should support bounded per-target or provenance-based rules without retaining entity data indefinitely.

Do not perform database reads during combat/block hot paths.

---

# 23. Persistence schema 2.0

Audit the existing schema before changing it.

Potential new tables/columns:

```text
schema metadata
milestone claim state
skill statistics
ability state where persistence is justified
recent unlock bounded history
```

Requirements:

```text
schema version
transactional migration
backup before migration
idempotent migration
rollback documentation
no silent reset
coalesced writes
bounded queues
shutdown flush
```

Do not persist cosmetic GUI state unless it has gameplay value.

---

# 24. Migration from PlexonSkills 1.0.x

2.0 startup must detect the 1.0 schema.

Migration flow:

```text
detect
validate
backup skills.db
begin transaction
add/transform 2.0 schema
validate row counts / invariants
commit
write schema version
```

Existing total XP for every player/skill must remain unchanged unless a documented curve migration intentionally changes interpretation.

If the XP curve changes incompatibly, provide an explicit configurable migration strategy and report it clearly.

---

# 25. mcMMO migration compatibility

Retain the clean-room migration principles from 1.0.

Do not guess unknown mcMMO schemas.

Source data remains read-only.

2.0 should preserve:

```text
SHADOW
PRIMARY
backup
migration status
diagnostics
safe refusal on unknown schema
```

Do not let Phase 2 premium work reopen the entire Phase 1 migration design unless a concrete defect is found.

---

# 26. Public API

Preserve source/binary compatibility where reasonable for:

```text
PlexonSkillsAPI
PlayerSkillView
SkillProgressView
XP gain event
level-up event
ability activate/end events
```

Add 2.0 APIs only where there is a real consumer value, for example:

```text
milestone view
ability view
mastery state
statistics snapshot
```

Prefer immutable snapshots.

Do not expose mutable profile internals.

---

# 27. PlaceholderAPI

Retain existing placeholders and add useful premium placeholders without breaking old identifiers.

Suggested additions:

```text
%plexonskills_<skill>_level%
%plexonskills_<skill>_xp%
%plexonskills_<skill>_progress%
%plexonskills_<skill>_stage%
%plexonskills_<skill>_next_milestone%
%plexonskills_<skill>_mastered%
%plexonskills_total_level%
%plexonskills_total_xp%
%plexonskills_highest_skill%
```

Avoid synchronous DB-backed placeholders.

---

# 28. Admin GUI

Add `/skillsadmin` GUI entry or `/skillsadmin menu`.

Suggested root:

```text
Skill Configuration
Ability Configuration
Milestones / Rewards
Player Inspector
Migration
Backups
Diagnostics
Reload
```

Routine management actions:

```text
enable/disable
inspect
preview
validate
test feedback
```

Do not implement dangerous bulk reset without confirmation.

Catastrophic actions require explicit target and confirmation.

---

# 29. Configuration model

Use:

```text
config.yml     global runtime / persistence / anti-exploit / feedback
skills.yml     skill definitions and XP sources
abilities.yml  ability definitions
rewards.yml    milestone/reward definitions
messages.yml   localized presentation text
```

Add schema/version markers where useful.

Every file must have:

```text
comments
safe defaults
validation
meaningful errors
reload boundaries
```

Invalid critical configuration should reject reload and preserve the previous runtime.

---

# 30. Default configuration quality

Bundled defaults must demonstrate the product.

Do not ship:

```text
rewards: {}
abilities globally disabled with no examples
placeholder lore
empty premium GUI
```

Provide balanced, conservative examples that an administrator can tune.

---

# 31. Menu architecture

Create a dedicated menu architecture rather than extending one monolithic `SkillsMenu`.

Recommended package direction:

```text
gui/
  SkillsMenuController
  MenuSession
  MenuScreen
  RootSkillsScreen
  SkillDetailScreen
  SkillProgressionScreen
  SkillAbilityScreen
  ProfileScreen
  LeaderboardScreen
  StatisticsScreen
  admin/
```

Use inventory holders/session identifiers rather than relying only on matching raw inventory titles.

Required safety:

```text
cancel click-through
cancel drag theft
session cleanup on close/quit
stable navigation
no stale async callback writing into a newer session
```

---

# 32. Async GUI safety

For leaderboard or other async data:

```text
open immediate loading state
capture session generation/token
fetch async
return to main thread
validate player online + same session/generation
replace only relevant slots
```

Never blindly reopen or mutate an inventory from an obsolete async completion.

---

# 33. Performance implementation rules

Gameplay state may update immediately.

Visual state may be coalesced.

Required principles:

```text
precompute skill definitions
precompute material routing
cache static menu decoration/items when safe
rebuild only dynamic card portions
no per-event config parsing
no per-event MiniMessage template parsing where avoidable
coalesce actionbar XP bursts
bounded leaderboard cache
bounded statistics queues
single persistence coordinator
```

Target no measurable regression to single-block gameplay hot paths.

---

# 34. Diagnostics 2.0

Extend diagnostics with premium subsystems:

```text
loaded profiles
dirty profiles
persistence queue
XP grants/rejections
anti-exploit rejections
ability activations/rejections
milestones awarded/skipped
GUI sessions opened/closed
active GUI sessions
leaderboard cache hits/misses
async GUI stale-result drops
migration state
Core module state
```

Diagnostics must remain cheap.

---

# 35. Testing requirements

Automated tests must cover at minimum:

```text
XP curve boundaries
max-level terminal behavior
skill registry validation
material route uniqueness
placed-block anti-exploit
mature farming rules
combat attribution gates
ability lock/ready/cooldown transitions
milestone threshold crossing
milestone idempotency
reward failure safety
schema 1.x -> 2.0 migration
profile persistence round-trip
shutdown flush behavior
leaderboard ordering/cache logic
GUI session generation safety
config validation/reload rollback
PAPI formatting helpers
Core lifecycle registration/unregister behavior where testable
```

Use unit tests for pure logic and focused integration tests for persistence/runtime coordination.

---

# 36. Manual acceptance test matrix

Where a Paper test server is available, execute:

```text
/skills open
all root cards
profile screen
every skill detail
progression screen
ability screen
leaderboard screen
statistics screen
back/close navigation
rapid click
shift click
drag
inventory close
quit/rejoin
restart
reload
ability activation/cooldown
milestone crossing
max-level presentation
admin menu
backup
migration dry run
```

If runtime server access is not available, report:

```text
NOT EXECUTED
```

Never fabricate runtime evidence.

---

# 37. Performance acceptance

Before stable release, where runtime environment is available:

```text
Spark baseline with 1.x or pre-overhaul build
Spark 2.0 comparison
single-block progression load
combat progression load
GUI open/close stress
leaderboard repeated open
memory retention after session close
30-minute soak
```

Collect:

```text
MSPT
TPS
heap trend
thread count
persistence queue depth
active menu sessions
```

No recurring task should scale linearly as one task per player.

---

# 38. CI / release workflow

Update workflows for 2.0.0.

Build pipeline:

```text
checkout
Java 25
provision pinned PlexonCore 2.0.4
verify Core SHA-256
clean
test
check
javadoc
shadowJar alias / distribution jar
verifyDistribution
writeSha256
upload artifact
```

Release pipeline:

```text
RC tags/releases immutable
stable v2.0.0 immutable
JAR
SHA256SUMS.txt
release notes
```

Do not hard-code old `1.0.0` output paths after version migration.

---

# 39. Documentation output

Update:

```text
README.md
CHANGELOG.md
docs/ARCHITECTURE.md
docs/API.md
docs/CONFIGURATION.md
docs/PERFORMANCE.md
docs/RECOVERY.md
docs/STAGING.md
docs/MIGRATION_MCMMO.md
```

Add:

```text
docs/PREMIUM_UX.md
docs/MIGRATION_1_TO_2.md
docs/ABILITIES.md
docs/MILESTONES.md
```

---

# 40. PR acceptance checklist

The 2.0 PR is ready only when:

```text
[ ] Core baseline is 2.0.4
[ ] build version is 2.0.0 target
[ ] all thirteen enabled skill domains work
[ ] root GUI is premium and navigable
[ ] profile screen exists
[ ] skill detail screen exists
[ ] progression roadmap exists
[ ] abilities are real/configurable
[ ] milestone rewards are real/configurable
[ ] rewards.yml is non-empty and useful
[ ] leaderboards have GUI
[ ] statistics have GUI or clearly scoped implementation
[ ] interaction hints are consistent
[ ] mastery state is intentional
[ ] no title-string-only inventory session identification
[ ] click/drag/session safety is implemented
[ ] anti-exploit rules are tested
[ ] migration is backup-safe
[ ] public API compatibility is reviewed
[ ] PAPI compatibility is reviewed
[ ] diagnostics include 2.0 subsystems
[ ] automated CI is green
[ ] JAR verification passes
[ ] SHA-256 generated
[ ] documentation complete
[ ] runtime-only tests executed or explicitly NOT EXECUTED
```

---

# 41. Implementation order

Implement in this order to reduce regression risk:

```text
1. baseline/version/Core 2.0.4 alignment
2. schema/config model and validation
3. skill presentation + stage/milestone model
4. ability model/runtime
5. reward/milestone persistence and execution
6. premium presentation/rendering helpers
7. safe menu/session architecture
8. root/profile/detail/progression/ability screens
9. leaderboard/statistics screens
10. admin GUI
11. PAPI/API extensions
12. migration 1.x -> 2.0
13. diagnostics
14. tests
15. documentation
16. CI/release conversion
17. PR review and CI repair loop
18. merge only after green acceptance
19. immutable release artifact publication
```

---

# 42. First implementation commit

The first implementation commit after this specification should establish the 2.0 baseline:

```text
- project version -> 2.0.0 development target
- Core JAR pin -> 2.0.4
- Core SHA -> 61d625a717da9f46ee9231e1970d84b4c317ae12cf4090cdf7c9d39b6a1a9baf
- workflows updated to build 2.0 artifact names
- release workflow prevented from accidentally publishing old 1.0 tags
- canonical presentation/model packages introduced
- existing 1.0 behavior kept compiling before gameplay expansion
```

Do not mix an uncontrolled full rewrite into the baseline alignment commit.

---

# FINAL DIRECTIVE

PlexonSkills 2.0.0 must leave Phase 2 as a deliberate premium progression product rather than a Core-native technical proof.

Preserve the safe architectural foundation of 1.0.

Replace the skeletal product layer with:

```text
meaningful progression
abilities
milestones
premium GUI hierarchy
clear feedback
admin tooling
safe data evolution
measurable runtime quality
```

Use PlexonTools as the shared visual/lore grammar reference, PlexonCore 2.0.4 as the lifecycle baseline, and this document as the dedicated implementation contract for the PlexonSkills Phase 2 release.