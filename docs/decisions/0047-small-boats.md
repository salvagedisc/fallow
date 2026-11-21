# 0047 — Small boats

- **Date**: 2026-11-21
- **Status**: kept (the team's choice of 2026-11-21); boot of the full pack's dedicated server, not played

## Context
The ideas kept on 2026-11-21 after [0046](0046-wind-on-ships.md) (a rope ferry across rivers, storms that rock ships, a big fish that tows the boat) all lean on the boat. The vanilla boat is poor: one shape, it stops against any step, the passenger looks only ahead, the rower's hands are tied. The team wants it better without going to big ships, which Sable and Create Aeronautics already cover.

## Options
Searched on Modrinth for NeoForge 1.21.1 on 2026-11-21. No mod rebuilds rowing itself (paddle animation, inertia).

| Criterion | A. Fish 'N' Ships and three handling mods | B. Handling mods only | C. Small Ships |
|---|---|---|---|
| Craft | Canoe, sailboat, rafts, fishing boat, in every vanilla wood | The vanilla boat only | Cogs, brigantines, galleys, drakkars |
| Size | Small | Small | Big, with cannons |
| Licence | ARR (downloaded from Modrinth, not embedded); handling mods GPL/LGPL | GPL/LGPL | ARR |

## Decision
Option A: Fish 'N' Ships 1.1.0 (canoes, dinghies, sailboats, rafts and supply rafts, a fishing boat), KotlinLangForge (the library of Better Boat Movement), Better Boat Movement 2.5.4, BoatView360 1.0.6 and BoatHandsFree 1.0.3 (the last two on the client only). 159 texts translated in `pack/kubejs/assets/{fish_n_ships,bbm}/lang/fr_fr.json`.

## Consequences
- Fish 'N' Ships' sailboat has a banner for a sail: the pack's wind could fill it (idea 7, to design). Not checked whether the mod has a wind of its own.
- Fish 'N' Ships uses Hybrid API, already in the pack for Hybrid Aquatic.
- To see in game: crafting each craft, a canoe with three players, the sailboat, Better Boat Movement at a river bank, a boat at the disc's edge ([tests](../tests-en-jeu.md)).

## Rejected
- Small Ships: real ships, not small craft.
- Boatload: large boats, Abnormals licence.
- Old Boats: too young (0.3).
- Boat break fixes, Jumpy Boats, Boats Motion Rework: no update for more than a year.
