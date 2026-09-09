# mcMMO migration

PlexonSkills does not guess or reverse-engineer mcMMO's data schema.

Supported workflow:

1. Put PlexonSkills in `SHADOW` mode.
2. Use `/skillsadmin migrate mcmmo scan <path>` against administrator-owned data.
3. Inspect the actual source format/schema and create an explicit mapping.
4. Generate a migration plan preserving levels by default.
5. Back up PlexonSkills and the source data.
6. Execute against staging data first.
7. Verify player counts, levels, partial progress and unsupported fields.
8. Only then repeat on production and switch to `PRIMARY`.

The current 1.0 candidate intentionally refuses `plan`/`execute` when no verified schema adapter exists. This prevents silent data corruption and is why the automated build alone is not sufficient for stable `v1.0.0`.

Never mutate the source mcMMO database during import. Never run both active progression engines simultaneously in production PRIMARY mode.
