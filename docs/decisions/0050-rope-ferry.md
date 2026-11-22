# 0050 — The rope ferry

- **Date**: 2026-11-21
- **Status**: kept (the team's choice of 2026-11-21, idea 6 of the Sable study); checked on the full pack's dedicated server (a raft taken on a rope, crossing, stopping at the bank, turning back), not played

## Context
Rivers of Streams Reflowing are wide and flow. Before Create, crossing one with a horse or a load means swimming or bridging. A reaction ferry (« bac à traille ») is a raft held by a rope across the river, pushed over by the current: it fits the pack's early game, and Sable gives rafts real physics.

## Options
| Criterion | A. House blocks and Sable | B. Create Aeronautics alone | C. An entity raft of Fish 'N' Ships on a rope |
|---|---|---|---|
| When | From the first days (logs, planks, a rope) | After Create and its physics assembler | From the first days |
| Load | Anything built on the raft: horses, carts, chests | Same | Two seats, no horse |
| Code | Two blocks, a rope drawn, forces in Sable's physics step | None, but no rope that guides | A mixin on another mod's entity |

## Decision
Option A, `bac` module of the `fallow` mod 0.64.0 (needs Sable).

- **Ferry post** (`fallow:pieu_de_bac`, two logs give two): one on each bank. A rope in hand (Farmer's Delight's or Supplementaries' rope, or a lead: item tag `fallow:cordes_de_bac`) on a post, then on the other: the rope is stretched, one rope per 8 blocks, 96 blocks at most. Shift + click with an empty hand on a post, or breaking it, unties the rope and gives the ropes back.
- **Ferry pulley** (`fallow:poulie_de_bac`): put on a raft built on the water under the rope (at most 400 blocks, not touching the bank: since 0.66.0 a raft that rests on the bank or the bottom, for example through a pier, is refused). A click takes the raft on the rope: Sable makes it a body that floats, marked as a ferry raft; since 0.84.0 only such a raft is taken back on a rope once released, and a ship built another way (Create, Sable) is refused (« Ce navire n'est pas un radeau de bac »). Then a click starts the ferry towards the far bank or stops it, Shift + click turns it back; the message says where it heads.
- **Crossing**: the rope holds the raft (a damped spring towards the rope, across it only) and the raft glides along it at about 1 block/s pulled by hand in still water (a target of 1.4, held back by the water), plus 1.5 × the current across the rope, measured in the water under the raft. It stops 2.5 blocks from a post. If the raft is pulled 8 blocks off the rope, or the rope is untied, the raft is let go and drifts.
- **Switch**: `bac.enabled` in `config/fallow-common.toml`.

## Consequences
- The rope is drawn between the posts by the post where it was tied first, up to 128 blocks away.
- A raft on a rope is a Sable ship like any other: the wind of [0046](0046-wind-on-ships.md) and the weather of [0049](0049-weather-on-ships.md) act on it too; the rope holds it against them.
- Not seen in a client: the look of the posts, the pulley and the rope, and boarding a horse (tests BC1 to BC5, [tests](../tests-en-jeu.md)).

## Rejected
- Option B: comes too late in the game, and nothing keeps the raft on its line.
- Option C: no room for a horse, and it would hang on another mod's entity code.
