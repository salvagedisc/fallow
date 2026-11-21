# 0049 — Weather on ships

- **Date**: 2026-11-21
- **Status**: kept (the team's choice of 2026-11-21, idea 7 of the Sable study); checked by unit tests and a dedicated-server boot, not played

## Context
Since [0046](0046-wind-on-ships.md) the pack's wind pushes Sable ships, but a storm was only a stronger wind: nothing made flying or sailing in bad weather a risk, while the `climat` module already makes the body feel snow and sand storms. The small boats of [0047](0047-small-boats.md) brought a sailboat whose sail pushed the same in any weather and any heading.

## Options
| Criterion | A. Forces only | B. Forces and real snow | C. Nothing new |
|---|---|---|---|
| Storm | Gusts that push the top of the ship | Same | Steady wind |
| Snow | A weight added by the code, unseen | Real snow layers on the decks, weighed by Sable | None |
| Melting | A timer | Shelter or mild weather removes layers | — |

## Decision
Option B, in the `fallow` mod 0.63.0 (`navires` module, and a mixin on Fish 'N' Ships).

- **Gusts**: in rain and above all in thunder, each ship gets gusts of its own in bursts, roughly across the wind and turning from one to the next, up to 7 blocks/s at sea level and twice that 210 blocks up. They hit 40 % of the ship's height above its centre of mass: the ship drifts by jolts and heels.
- **Snow on the decks**: where it snows (Ecliptic Seasons decides, as for `climat`; the biome without it) and the sky is open above the ship, snow layers settle on the highest block of random columns of the ship, three times faster in a snowstorm, up to `navires.snowMaxLayers` (4). Sable counts their mass: a laden airship sinks unless its balloons or propellers make up for it. Under a roof the snow melts twice as fast; in the open it melts once it stops snowing where the biome is mild enough to rain.
- **Sailboat**: the push of Fish 'N' Ships' sail (a fixed 0.03 block per tick along the heading) follows the pack's wind: nothing within 35° of the wind, 0.3 to 1 close-hauled, the most on a beam or broad reach, 0.9 running; times the wind's strength, from half in a light breeze to two and a half times in a storm.
- **Settings** in `config/fallow-common.toml`: `navires.storms`, `navires.snow`, `navires.snowMaxLayers`, `navires.sailWind`.

## Consequences
- Snow is placed in the ship's plot like any block: a player can shovel it off, and it drops nothing more than vanilla snow.
- The gusts follow the world time like the wind: every player sees the same jolts, but they are not the client's own gusts that bend the grass.
- To see in game: tests NV4 to NV7 ([tests](../tests-en-jeu.md)).

## Rejected
- Option A: an unseen weight would surprise the pilot; snow on the deck explains itself.
- Snow on the plain Sable boats only: airships are where weight matters most.
