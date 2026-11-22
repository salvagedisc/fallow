# 0052 — Gestures, not ground debris

- **Date**: 2026-11-21
- **Status**: kept (the team's choice of 2026-11-21, replacing [0048](0048-ground-debris.md)); `fallow` 0.67.0, checked on the full pack's dedicated server, not played

## Context
The ground debris of [0048](0048-ground-debris.md) (rocks, fallen wood and pine cones as small blocks) looked too present once seen in game: about 2.5 per chunk, on every forest floor. The team wanted something more discreet. The first day still needs sticks and a rock to knap, and the pack has no other source of rocks than flint on stone.

## Options
| Criterion | A. Debris kept as they are | B. Fewer debris, off the grass | C. Nothing on the ground, gestures |
|---|---|---|---|
| What the world shows | Debris on every floor | Wood under trees, rocks on bare stone and sand | Nothing |
| Sticks | Fallen wood → 2 sticks | Same, rarer | Shaking leaves empty-handed, now and then |
| Rocks | Picked up | Picked up, rarer | Found when digging dirt and gravel |
| Code | 17 blocks, a feature type | Same, data changed | One event, three loot modifiers |
| Register ([0042](0042-interaction-language.md)) | « Reprendre » | « Reprendre » | « Reprendre » (leaves), work (digging) |

## Decision
Option C. The `debris` module, its 17 blocks, its models (This Rocks!' with their licence) and its world generation are removed in `fallow` 0.67.0. In their place:

- the `glanage` module: a right-click on leaves, empty-handed or with a stick in hand, shakes them (sound, falling leaves) and drops a stick 30 % of the time, at most one try per leaf block and per game day;
- in `pack/kubejs/server_scripts/premiers_gestes.js`, dirt, coarse dirt, rooted dirt, grass and podzol give a knappable rock 5 % of the time, gravel 12 %;
- Twigs' pebbles and twigs stay off; the first gesture quests speak of shaking leaves and digging.

Passable Foliage comes with it, asked the same evening: leaves are walked through. Its Leaf Walker enchantment, which lets a player stand on and climb leaves, is off at the team's request (`modules.enchantment: false`). Its cushion, which took almost all of a fall into leaves away, is replaced on 2026-11-22 by the `feuillage` module of `fallow` 0.83.0, chosen by the team: a fall into a canopy keeps half its damage.

## Rejected
- Option A: the reason for this decision.
- Option B: still things on the ground, and the forest floor was the very place found too busy.
- Pine cones as an item from spruce leaves: one more item for a fuel the pack does not need.
