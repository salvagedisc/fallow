# 0055 — Mod metals drawn after the active resource pack

- **Date**: 2026-11-22
- **Status**: kept (the team's request of 2026-11-22, after a steel prototype); checked on a NeoForge 1.21.1 client with Hidyk's Realm, not played in the full pack

## Context
With Hidyk's Realm ([0054](0054-hidyks-realm.md)) the vanilla iron sword, pickaxe, ingot and armour are redrawn, but the mods' metal items keep vanilla's drawing in their own colour: Overgeared's steel tools sat next to Hidyk's Realm's iron tools and looked older. A study of the mods' 14,764 textures found that most metal items made as recolours of vanilla iron can take the resource pack's iron drawing in their own colour. Hidyk's Realm is all rights reserved, so images derived from it cannot be committed to the public repository or shipped as a pack before an agreement with its author.

## Options
| | A. Images made at launch by the `fallow` mod | B. A resource pack of finished images | C. Leave the mods' textures |
|---|---|---|---|
| Look | Mod metals follow the active pack | Same, frozen | Mod metals keep vanilla's drawing |
| Repository | Only a list of texture paths | Derived images in the repository | Nothing |
| Another resource pack | Followed automatically | Redrawn by hand | — |
| Licence | Nothing of Hidyk's Realm is copied; it applies only when a player has it on | Needs its author's agreement first | — |

## Decision
Option A: the `gabarits` module (`fallow` 0.98.0, client).

- A hidden built-in resource pack sits above every pack and holds no files. At each resource reload it reads three images for every texture listed in `mods-custom/fallow/src/main/resources/assets/fallow/gabarits/regles.json`: the template, which is the iron item as the active resource pack draws it; the reference, which is the same item in vanilla; and the mod's own texture.
- The template's metal is what differs between its iron, gold and diamond versions, minus the stick's colours, so handles keep their wood. Its colours are sorted by lightness. Each colour takes the mod texture's hue at its rank, plus the lightness gap between the mod texture and vanilla iron, in CIELAB, one colour at a time, so the pack's grain stays sharp.
- A texture is only remade when the template comes from a resource pack (with no pack, nothing changes), when the mod texture comes from the mod itself (a resource pack that redraws it keeps it), when it is not animated, and when its shape covers at least 80 % of vanilla iron's (50 % for ingots and nuggets). Items with a design of their own, such as Overgeared's crested copper helmet, Create's diving gear or its brass and zinc blocks, are left alone.
- Where the active pack already draws the metal, its image is taken as is. Hidyk's Realm draws copper tools, armour and nuggets, which vanilla 1.21.1 lacks, so Overgeared's copper takes them directly. On armour layers, the mod's pixels where the template is empty stay (a crest, a belt).
- 50 textures in the list: Overgeared's steel (tools, armour, worn armour, ingot, nugget, block), copper (tools, armour, worn armour, nuggets) and heated ingots, Create's brass and zinc ingots and nuggets and copper nugget, Spelunkery's copper nugget, and the pack's black steel (tools, armour, worn armour, ingots). Setting `gabarits.enabled` in `config/fallow-client.toml` turns it off at the next reload.

## Not covered
- Metal items whose drawing differs from vanilla iron (Create's tools and machines, Steam 'n' Rails, Alex's Mobs, Hybrid Aquatic, Companions, Cold Sweat's armours) keep their own texture.
- Blocks other than Overgeared's steel block keep theirs. Hidyk's Realm's random variants of the iron block are not carried over to it.
- Ingots in 3D in hand (Fresh Ores and Ingots) stay vanilla's ingots; the mods' ingots are flat in hand.

## Licence
The repository holds only paths. The images exist only in memory on a player's machine, made from packs that player has on. A Modrinth release that ships Hidyk's Realm still waits for its author's agreement ([0054](0054-hidyks-realm.md)).
