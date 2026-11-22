# 0053 — Team Disco's items, Modefite, Fancy World Animations

- **Date**: 2026-11-22
- **Status**: kept (the team's choice of 2026-11-22); checked on a NeoForge 1.21.1 client, not played in the full pack

## Context
The look of items was only Fresh Animations and Punchy: flat items in hand, no real eating animation. A study compared 3D item packs, eating animations, HUD and block animations. The team chose Team Disco's packs: their 3D items are the best made, though they cover only what they redraw (no potions, for instance), and they become the base the pack's own items will follow.

## Options
| Criterion | A. Team Disco's packs + Modefite | B. MB-3D Items | C. Nothing new |
|---|---|---|---|
| Items in hand | 3D food, buckets, ingots, ores, flowers, discs, paintings | 3D everywhere, vanilla only | Flat |
| Eating | Bites in stages, per food | None | None |
| Inventory | Stays 2D | 3D icons | 2D |
| Licence | All rights reserved (downloaded from Modrinth, not copied) | MIT | — |
| Runs on 1.21.1 | Through Modefite (1.21.4 item definitions) | Yes | — |

## Decision
Option A, with Fancy World Animations:

- **Modefite** (GPL-3.0, client) reads the 1.21.4 item definitions the packs are written in: a model per context (hand, ground, frame, inventory) and a model per time of use, which gives the bites. **Respackopts** and **LibJF** (client) read the packs' options; **Polytone** (client) comes back for Fresh Ores and Ingots, Fresh Flowers and Plants and Fresh Buckets. No Connector is needed: on a NeoForge 1.21.1 client the six packs load, food is eaten in stages and inventory icons stay 2D.
- **Six packs** in `pack/resourcepacks/`: Fresh Food, Fresh Buckets, Fresh Ores and Ingots, Fresh Flowers and Plants, Fresh Music Discs, Fresh Paintings. They are on at first launch (`config/defaultoptions-common.toml`), above Better Leaves and under Fresh Animations. They are made for 1.21.4 (format 46): the game lists them as incompatible and keeps them on once chosen. A player who already launched the pack turns them on by hand in Options → Resource Packs.
- **Fancy World Animations** (MIT, client) animates doors, trapdoors, fence gates, levers, buttons, chests, lecterns, bells, lanterns, chains, hanging signs and more. Its lanterns, chains and hanging signs are left to Amendments, which swings wall lanterns and hanging signs (`config/fwa.json`, `config/amendments-client.toml`). For a few hours on 2026-11-22 it was the other way round, FWA swinging everything that hangs and Amendments' swinging off; the team went back to Amendments' movement the same evening.
- **Four more packs**, chosen the same day: Fancy Crops (fuller crops; modded crops built on the vanilla crop model take its shape but not its textures), Fast Better Grass with its Regions Unexplored compat, RAY's 3D Ladders and Better Flame Particles. Fast Better Grass sits above the other block packs, as its author asks.
- The pack's own items (modded food, potions) will follow Team Disco's grammar, drawn by the pack, nothing copied from their files; a pilot of about ten Farmer's Delight foods comes first.

## Rejected
- MB-3D Items: vanilla only, and 3D icons in the inventory.
- Create: Fresh Items: needs Pommel, Fabric only.
- 3D Items-Delight: non-commercial licence, and its finish is below Team Disco's.
