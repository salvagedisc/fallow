# 0056 — Forager as the base look of the game

- **Date**: 2026-11-23
- **Status**: kept (the team's choice); checked on a NeoForge 1.21.1 client with Brumaire

## Context
After Hidyk's Realm left the pack ([0054](0054-hidyks-realm.md)), the team asked for a wider renewal of the game's textures that keeps the modded blocks in harmony, with the worn armours as the main thing to improve. A study of 33 packs compared the packs the team proposed with 24 others on the same test world, alone, above the pack's current resource packs and, for the final candidates, under Brumaire.

## Options
| Criterion | A. Forager | B. Pixel Perfection Legacy + Armory Conglomery | C. Rappenem's Reforge + Armory Conglomery |
|---|---|---|---|
| Share of the game redrawn | 98 % of blocks, all items, 97 % of the interface, all creatures | 93 % of blocks, 97 % of items, 92 % of the interface | 18 % of blocks, 37 % of items, a quarter of the interface |
| Look | Rustic and warm, close to vanilla, wooden interface | Earthy and muted, brown stone | Vanilla blocks, medieval items and armours |
| Armours | Its own, redrawn | Armory Conglomery's | Armory Conglomery's |
| Licence | CC BY-NC with its own terms (below) | CC BY-SA, all rights reserved | All rights reserved |

## Decision
Option A, the team's choice.

- **Forager** 14.2 (`pack/resourcepacks/forager.pw.toml`) is on at first launch, the lowest of the file packs (`config/defaultoptions-common.toml`): the packs chosen one by one (Better Leaves, Team Disco's items, Fancy Crops, RAY's 3D Ladders, Better Flame Particles, Fast Better Grass, Fresh Animations and its extensions) win over it where both draw the same thing.
- Its creature models need EMF and ETF, already in the pack. Fresh Animations sits above it and keeps the creatures it animates.
- None of its files is copied, changed or used to derive textures for the mods.

## Licence
Forager's terms allow adding it to servers and sharing it with proper credit. They forbid reuploading or selling it, modifying or copying its designs, sharing it without credit, and feeding it into any kind of AI. The pack downloads it from Modrinth and does not copy its files. Before a Modrinth release that contains it: ask its author for permission by direct message, as the terms invite when unsure, since the pack's Modrinth page states that AI was used to make it; credit it on the project page and list it as a dependency. If the author refuses, it leaves the pack.

## Rejected
- Pixel Perfection Legacy + Armory Conglomery: recommended by the study, not chosen.
- Rappenem's Reforge + Armory Conglomery: blocks unchanged.
- Allure, Stupendous Visuals: colours that clash with the modded blocks.
- Squareful: no derivatives allowed and a square motif everywhere.
- Jicklus, Lanostry's Journey: modpacks not allowed.
- f-pack, Vanilla Blooming Blocks: no 1.21.1 version.
