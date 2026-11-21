# 0046 — Wind on ships, and a headwind at the edge

- **Date**: 2026-11-21
- **Status**: kept (the team's choice of 2026-11-21); the edge part refines [0022](0022-disc-world.md); checked by unit tests and a dedicated-server boot, not played

## Context
The edge of the disc ([0022](0022-disc-world.md)) brought back a player who passed it with a teleport and a message. It read as a wall of text, not as a world, and it let Sable ships through: a player aboard was only warned. The pack's wind ([`vent`](../plan/mod-fallow.md#vent-module)) moved smoke, foliage and the shader, and the beasts' sense of smell, but pushed nothing, while Sable gives ships real physics.

## Options
| Criterion | A. Keep the teleport | B. Stop ships with Sable's velocity handle | C. Forces in Sable's physics step |
|---|---|---|---|
| Feel at the edge | A jump and a message | Ships stop dead | The sea wind rises and pushes back, harder and harder |
| Ships | Pass the edge | Stopped | Pushed back, and drift with the pack's wind |
| Code | Exists | Velocity edits outside the solver | Impulses through Sable's force groups, like its own drag |

## Decision
Option C, in the `fallow` mod 0.59.0.

- **Edge, for players** (`bord` module): from 24 blocks before the circle a headwind adds speed towards the centre to the player, or to the boat or mount they ride, growing with the square of the depth into the band, then faster beyond the edge (up to four times), and it takes away part of what still heads out. A message says « Un vent contraire se lève et vous repousse vers les terres. » The teleport stays only as a safety net, 48 blocks past the edge (a very fast flight). Since `fallow` 0.66.0 the push is applied by the client that moves the player or their vehicle, on the server's word: sent from the server, it wiped the speed the client already had and stopped a boat dead at the start of the band.
- **Edge, for ships** (`navires` module, needs Sable): the same headwind as an acceleration on the ship, over a band of 48 blocks, 3 blocks/s² at the edge, at most 12, plus a brake on the outward speed past the edge. The player aboard is told « Le vent du large repousse le navire vers les terres. »
- **Wind on ships** (`navires`): at each physics step, a ship under open sky gets a force from the pack's background wind (`WindField`, the one smoke and the shader follow), proportional to the face it shows the wind above the water and to the relative wind. An unpowered airship drifts at about the wind's speed; a boat, held by the water, much less. Gusts stay on the client and push nothing. Forces go through the centre of mass: no turning.
- **Settings**: `navires.wind` and `navires.windStrength` in `config/fallow-common.toml`; the edge follows `bord.enabled`.

## Consequences
- A ship left at anchor in the open drifts with the wind. Anchoring or mooring is up to Create Aeronautics' own blocks.
- The visible wind of the client knows nothing of the edge (the circle is server data): near the edge the headwind pushes, but the smoke does not turn. Possible follow-up.
- To see in game: tests Z2 to Z4 and NV1 to NV3 ([tests](../tests-en-jeu.md)).

## Rejected
- Option A: the team's choice is to be pushed back by the wind, not by a message that teleports.
- Option B: changing a body's velocity outside Sable's solver fights its constraints (ropes, hinges, assembled contraptions).
