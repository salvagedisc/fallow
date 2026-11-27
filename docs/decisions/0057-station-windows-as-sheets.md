# 0057 — The mods' station windows as sheets of the books' paper

- **Date**: 2026-11-23
- **Status**: kept for the windows that are not crafting stations; superseded for the crafting stations by [0058](0058-mod-stations-on-the-block.md) (worked on their block, Hands-On style). Checked on a NeoForge 1.21.1 client with Spatial GUI, without Forager (its terms, [0056](0056-forager.md#licence)); the wooden case under Forager is to be checked in game

## Context
The team asked for every crafting station's window and the other menus to be rethought so that they feel part of the world and in harmony with the rest: the books (cream paper, brown inks never black, the rubric red), the emissaries' dialogue, and Forager's wooden interface ([0056](0056-forager.md)). Vanilla's windows are drawn by Forager. The mods' station windows (Overgeared's furnaces and tables, Cold Sweat's, Farmer's Delight's cooking pot, Quark's trough and crate, Supplementaries', Exposure's lightroom, Chalk's box, Envelope's packing) copy vanilla's grey panel and stayed grey beside Forager's wood. The stations that are worked by hand (the crafting table, the furnaces, the tannery, the forge, the hearth: [0042](0042-interaction-language.md)) already have no window.

## Options
| Criterion | A. Sheet on the case | B. Recolour the grey panels | C. A house interface pack (as before [0024](0024-removing-the-house-resource-packs.md)) |
|---|---|---|---|
| Look | The station's part a loose sheet of the books' paper, its wells pressed in, its pictograms in ink; the inventory below is the wooden case of a chest | Each window tinted brown | Every window, vanilla's too, redrawn by the pack |
| Harmony with Forager | Its wood drawn by Forager itself under the sheet | Copies Forager's colours from its files | Replaces Forager |
| Forager's terms | Nothing of it is read or copied | Breaks them (its designs copied) | Undoes the team's choice |
| Work | A generator and a small client module | A generator | Every window by hand |

## Decision
Option A.

- `scripts/fiches/fiches.py` reads each window from its mod's jar, finds its slots by vanilla's colours, and writes in `pack/kubejs/assets/` a texture of the same path: the station's part as a sheet (the Hosts' dialogue paper), the inventory part transparent. It writes `pack/kubejs/assets/fallow/fiches.json`, where each screen class says where the case goes.
- The `fiches` module of the `fallow` mod (client) draws there, under the window, the inventory strips of vanilla's chest window (`generic_54.png`): whatever the active resource pack draws for a chest, Forager's wood in the pack. In a sheet the name « Inventaire » is not written, the title is in the books' ink, and vanilla's buttons and text fields are cards and wells of the paper with ink letters.
- Windows that are a chest under another name (Quark's crate) are drawn as a real chest; windows with their own art above (Fish 'N' Ships' ship, Hybrid Aquatic's argonaut) keep it, only their inventory becomes the case.
- Create's and Aeronautics' machines keep their own style, which already matches their blocks.

## Rejected
- B: copies Forager's colours, which its terms forbid.
- C: replaces the look the team chose, and brings back a house pack the team removed in 0024.
