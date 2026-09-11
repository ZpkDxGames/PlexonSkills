# Commands and Permissions

## Player commands

| Command | Purpose | Permission |
|---|---|---|
| `/skills` | Open the premium skills dashboard | `plexonskills.use` |
| `/skill` | Alias of `/skills` | `plexonskills.use` |
| `/skills <skill>` | Show compact progress for one skill | `plexonskills.use` |
| `/skills stats [player]` | Show loaded-profile stats for self or an online player | `plexonskills.use` |
| `/skills top [skill]` | Asynchronous top-10 leaderboard | `plexonskills.use` |
| `/skills rank [skill]` | Asynchronous rank query | `plexonskills.use` |
| `/skills abilities` | Enter the ability inspection flow | `plexonskills.use` + `plexonskills.abilities` to activate |
| `/skills admin` | Open administrator GUI | `plexonskills.admin.gui` |

`plexonskills.use` and `plexonskills.abilities` default to `true`.

## Administrator commands

| Command | Purpose | Permission |
|---|---|---|
| `/skillsadmin gui` | Open premium admin GUI | `plexonskills.admin.gui` |
| `/skillsadmin reload` | Validate/apply runtime configuration | `plexonskills.admin.reload` |
| `/skillsadmin diagnostics` | Show Core/runtime/persistence/migration diagnostics | `plexonskills.admin.diagnostics` |
| `/skillsadmin addxp <player> <skill> <value>` | Add positive XP to an online player | `plexonskills.admin.xp` |
| `/skillsadmin setxp <player> <skill> <value>` | Set non-negative canonical XP | `plexonskills.admin.xp` |
| `/skillsadmin setlevel <player> <skill> <value>` | Set level by converting to the configured curve's cumulative XP | `plexonskills.admin.xp` |
| `/skillsadmin reset <player> [skill]` | Reset one/all skills | `plexonskills.admin.reset` |
| `/skillsadmin backup` | Create timestamped SQLite backup | `plexonskills.admin.backup` |
| `/skillsadmin migrate status` | Inspect mcMMO migration source state | `plexonskills.admin.migrate` |
| `/skillsadmin migrate scan` | Read-only inspect configured mcMMO source | `plexonskills.admin.migrate` |
| `/skillsadmin migrate plan` | Show clean-room migration plan state | `plexonskills.admin.migrate` |
| `/skillsadmin migrate execute` | Execute only when a validated mapping exists; otherwise refuses | `plexonskills.admin.migrate` |

Player-issued destructive resets are intentionally redirected to the admin GUI so confirmation is required. Console reset remains explicit and requires an online target.

## Permission hierarchy

`plexonskills.admin` defaults to operators and grants:

```text
plexonskills.admin.gui
plexonskills.admin.xp
plexonskills.admin.reset
plexonskills.admin.ability
plexonskills.admin.config
plexonskills.admin.reload
plexonskills.admin.backup
plexonskills.admin.migrate
plexonskills.admin.diagnostics
```

Legacy permission `plexonskills.admin.modify` is retained for existing LuckPerms setups and grants XP, reset and backup scopes.

## Operational recommendations

Grant `plexonskills.admin` only to trusted administrators. For routine staff, grant the narrow child permissions required by role. Keep `admin.reset`, `admin.config`, `admin.reload`, `admin.backup` and `admin.migrate` restricted because they affect persistent/runtime state.
