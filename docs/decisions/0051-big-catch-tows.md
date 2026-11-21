# 0051 — A big catch tows the boat

- **Date**: 2026-11-21
- **Status**: kept (the team's choice of 2026-11-21, idea 8 of the Sable study); the pull on a Sable raft checked on the full pack's dedicated server, the rest not played

## Context
Starcatcher's fight is a minigame on screen: whatever the fish, the angler and the boat stay where they are. The pack now has small boats (Fish 'N' Ships, [0047](0047-small-boats.md)) and rafts built of blocks that Sable makes float. A tuna of 120 kg or a sturgeon of 100 kg should be felt from a light boat.

## Options
| Criterion | A. The fish tows the boat during the fight | B. The fish also pulls the angler on land | C. Nothing |
|---|---|---|---|
| What it changes | A heavy fish takes a light boat for a ride; a heavy hull holds | The same, and a player on a bank is dragged towards the water | — |
| Risk | None outside boats | Falls, drowning from a minigame screen the player cannot leave | — |
| Code | A server tick, Sable's physics step, a small message to the helmsman's client | The same and more | — |

## Decision
Option A, `prise` module of the `fallow` mod 0.65.0 (needs Starcatcher; Sable for ships of blocks).

- **When**: while a Starcatcher bobber is in its fight state and its angler is aboard a Sable ship or in a boat on water (vanilla or Fish 'N' Ships). Species under 8 kg (Starcatcher's average weight of the species) pull nothing.
- **How fast**: the fish swims off at 2.5 blocks/s, away from the boat at first, then its heading drifts slowly by up to 50° either way; its strength surges and slackens between 0.5 and 1.2. Line taut, fish and boat go together at `2.5 × strength × fish / (fish + boat)` blocks/s. A Sable ship weighs 40 kg per unit of Sable mass (a plank is 0.5); an entity boat 150 kg per square block of its footprint, passenger included (a vanilla boat, about 280 kg).
- **Examples**: a 120 kg tuna takes a six-plank raft (120 kg) along at about 1 block/s, a 48-plank raft at 0.2 block/s, a vanilla boat at about 0.7 block/s; the Quarrish (700 kg) runs away with any small boat.
- **The bobber** follows the fish at the boat's pace, up to 40 blocks from the boat; at a bank the fish turns. Splashes at the bobber when it surges; « Le poisson vous emporte ! » once per fight.
- **Who moves the boat**: a Sable ship is pushed by the server in Sable's physics step (propulsion force group, at most 4 blocks/s²). An entity boat moves on its helmsman's client, so the server sends that client the heading and the speed every 4 ticks and the client pushes the boat (an optional payload: a client without the mod is not refused).
- **Switch**: `prise.enabled` in `config/fallow-common.toml`.

## Consequences
- The fight itself (the minigame, the catch, its weight) is Starcatcher's, unchanged.
- A heavier boat is the way to hold: there is no anchor block yet.
- Not seen in a client: an entity boat towed during the fight, the bobber following, the splashes (tests PR1 to PR4, [tests](../tests-en-jeu.md)).

## Rejected
- Option B: a minigame screen keeps the player from reacting; being dragged into deep water from it would be unfair.
- An anchor block: left for later, if a heavier hull is not enough.
