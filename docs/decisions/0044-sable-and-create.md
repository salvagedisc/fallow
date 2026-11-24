# 0044 — Sable and Create machines

- **Date**: 2026-11-21
- **Status**: kept (six mods added, five more on the team's word the same evening); Loconautics added on 2026-11-24, once on Modrinth.

## Context
The team asked for the mods that bring Sable, the physics library of Create Aeronautics, into Create's machines and the rest of the pack, Create: Loconautics given as an example. Sable 2.0.6, Create Aeronautics 1.3.3, Sable: Physics Compat and the two ragdoll mods were already in the pack. Research of 2026-11-21: Modrinth searched for every 1.21.1 NeoForge project naming Sable or Aeronautics (about 160), each candidate's jar opened (mod ids, dependencies, mixin targets, config classes) and its overlap with the pack checked.

## Options
| Need | Candidates | Kept |
|---|---|---|
| Mechanical arm, encased fan, basin, water wheel on a ship | Sable Beyond (MIT), Aeronautics: Addition (ARR, arm only) | **Sable Beyond** |
| Plough and roller on a ship | Plough & Roll! (MIT, source), Create Aeronautics Roller Fix (ARR, roadbed only) | **Plough & Roll!** |
| Display link on a ship | Aeronautics Display Link Fix (MIT) | **kept** |
| Duplication on assembly | Sable Fix Mod (MIT: containers, item handlers, attached blocks, crops), Sable Assembly Fix (MIT: non-clearable block entities only) | **Sable Fix Mod** |
| Mods of the pack on a ship | Create Aeronautics: Compatibility (Alex's Mobs path-finding), Furniture with Create and Sable compat (Handcrafted, Quark, Every Compat seats) | **both** |
| Trains as physics bodies | Create: Loconautics, Create: Train Functionality, Ferronautics | none yet |
| Temperature on a ship | Cold Sweat Sable Compat, Cold Sweat x Aeronautics | none: Cold Sweat 2.4.1+ does it |
| Mass of modded blocks | MassWeightCompat, Sable Weighted | none: Sable: Physics Compat covers the pack's mods |

## Decision
- Six mods join the pack, all from Modrinth: Sable Beyond 0.5.0, Plough & Roll! 1.2.0, Aeronautics Display Link Fix 1.3, Sable Fix Mod 1.1.0 (server only), Create Aeronautics: Compatibility 1.1.3, Furniture with Create and Sable compat 5.0.
- Sable Beyond settings (`config/sable_beyond/`): the push of flowing fluids on sub-levels is off, since Streams Reflowing already carries hulls along its currents ([0011](0011-tectonic-streams-distant-horizons.md)); its buttons on the title and pause screens are off. Its entity mass, dynamic mass and basin options stay off as shipped.
- French texts for the three mods that ship English only (`kubejs/assets/<namespace>/lang/fr_fr.json`).
- **Create: Loconautics** 0.1.3 beta, added on 2026-11-24 when it reached Modrinth (the jar is downloaded from Modrinth, so its art, all rights reserved, is never embedded): every Create train car becomes a Sable sub-level that can collide, derail and carry ships. It needs Create 6.0, Sable 2.0.5 and Simulated 1.3 (the pack has 6.0.10, 2.0.6 and 1.3.3 inside Aeronautics) and supports Steam 'n' Rails; its 1,332 texts are translated by the pack. It replaces Create's train contraptions everywhere and calls itself fragile with other train mods: Steam 'n' Rails trains are to be checked in game first.

## Rejected or pending
- **Create: Loconautics** (0.1.4 beta): each train car becomes a Sable sub-level that can collide and derail; it supports Steam 'n' Rails boilers. Was only on CurseForge on 2026-11-21; added on 2026-11-24 (see Decision).
- **Create: Train Functionality**: CurseForge only, no source.
- **Ferronautics**: halted, superseded by Loconautics.
- **Sound Physics Aeronautics**: a fork that replaces Sound Physics Remastered (same mod id) and adds ship acoustics, Doppler and far propellers; it brings 924 new texts to translate and a wing windrush that overlaps Aeronautics Wind Sound. Held at first, then chosen by the team on 2026-11-21: it replaces Sound Physics Remastered, its 924 texts are translated.
- **Separate Sable Render Distance**, **Sable Ragdolls Patch**: client-side, held at first; chosen by the team on 2026-11-21, to check in game (CI only starts the server).

## Second step (2026-11-21, evening)
A study of the Sable mods outside Create (108 projects) led the team to add Sable Pathfinder 1.4.0 and Sable: Sure Footing 1.2.1 with the three held mods above. Its other proposals (falling trees, cave-ins, ragdoll reactions, revival, altitude gravity, Waterworks) wait for the team.
- Aeronautics: Addition, Create Aeronautics Roller Fix, Sable Assembly Fix: covered by the kept mods. MassWeightCompat, Sable Weighted: duplicate Sable: Physics Compat. Create: Sable Train Overlay Compat: needs a map mod the pack does not have.
