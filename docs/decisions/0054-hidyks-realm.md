# 0054 — Hidyk's Realm as the base look of the game

- **Date**: 2026-11-22
- **Status**: abandoned on 2026-11-23: the team did not like the look once in the pack, and no answer from its author is expected. Both packs left the pack. Kept on 2026-11-22 (the team's choice), checked on a NeoForge 1.21.1 client without the shader

## Context
After the item packs ([0053](0053-team-disco-items.md)), the team wanted a resource pack for the rest of the game. A study ranked 416 resource packs for 1.21.1, opened 19 of them file by file and compared 10 on the same test world (ore cliff, workshop with Farmer's Delight, Create and Supplementaries, creatures, taiga village, inventory), each above the pack's current resource packs.

## Options
| Criterion | A. Hidyk's Realm | B. Vanilla Experience+ and GUI Experience+ | C. A stylised pack (Ashen, Excalibur, Whimscape) |
|---|---|---|---|
| Look | Close to vanilla, finer and quieter | Vanilla corrected | A new look for the whole game |
| Share of the game redrawn | 65 % of block textures, 82 % of items, 84 % of the interface, half the creatures | Mostly items and interface, 51 block textures | Nearly everything |
| Next to modded blocks | Create, Farmer's Delight and Supplementaries fit | Fit | Modded blocks stay vanilla and clash |
| Engines | Polytone, EMF, ETF, Respackopts, all in the pack | None | Connected textures need Continuity, which the pack cannot run on NeoForge |
| Licence for a modpack | Allowed with credit and a share of download income, agreed with its author | Allowed as a dependency | Allowed for Ashen and Whimscape, unstated for Excalibur |

## Decision
Option A.

- **Hidyk's Realm** (`pack/resourcepacks/hidyksrealm.pw.toml`) is on at first launch, the lowest of the file packs (`config/defaultoptions-common.toml`): the packs chosen one by one (Better Leaves, Team Disco's items, Fancy Crops, Fast Better Grass, RAY's 3D Ladders, Better Flame Particles, Fresh Animations) win over it where both draw the same thing.
- **Fresh Hidyk's Realm** sits on top of every pack: it joins Hidyk's Realm's creature models to Fresh Animations.
- Its grass and leaf colours by biome come through Polytone; they have to be judged under Brumaire.
- Its connected-glass add-on (Con+nections) needs Continuity and is left out. Its interface add-on (Hidyk's Realm UI, by another author) is not added.
- The mods' metal items take its drawing at launch, without any of its images in the repository ([0055](0055-mod-metals-after-the-resource-pack.md)).

## Licence
Hidyk's Realm is all rights reserved. Its terms allow adding it to a modpack "with my authorship indicated in the Creators, Dependencies section on Modrinth with a share of the income from downloads (determined individually, during discussion)". The pack downloads it from Modrinth and does not copy its files, but the terms still apply. Before a Modrinth release that contains it: agree the share with its author, credit him on the project page and list it as a dependency. If no agreement is reached, it leaves the pack and option B takes its place.

## Rejected
- Ashen, Excalibur, Whimscape, Yuushya: modded blocks clash, connected textures lost.
- Reimagined: its options fail on the pack's Respackopts; its connected textures are lost.
- Golden Days: breaks Fresh Animations' villager.
