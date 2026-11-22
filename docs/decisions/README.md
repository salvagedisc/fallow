# Decisions

One record per settled decision: the context, the options compared, the choice and its conditions. A record is not rewritten after the fact; if a decision changes, a new record replaces it and the old one moves to the status "superseded by NNNN".

| No. | Subject | Status |
|---|---|---|
| [0001](0001-nml-vs-terralith.md) | Worldgen: No Man's Land vs Terralith + Streams Reflowing | Superseded by 0011 |
| [0002](0002-agriculture.md) | Farming & animal husbandry | Farm & Charm, Ecliptic Seasons, KC-Automation kept; animal husbandry and preservation pending |
| [0003](0003-faune.md) | Fauna | Naturalist, Alex's Mobs Continued, Hybrid Aquatic, Mowzie's Mobs and companions kept |
| [0004](0004-structures.md) | Structures & villagers | Sparse Structures kept, End removed, "survivors" concept |
| [0005](0005-quark.md) | Quark: the essentials only | Config provided |
| [0006](0006-supplementaries.md) | Supplementaries: the essentials only | Config provided |
| [0007](0007-ambiance-performance.md) | Atmosphere & performance | Kept; Distant Horizons kept by 0011 |
| [0008](0008-enchantement.md) | Enchanting | Immersive Enchanting kept; Quark's Matrix as fallback (superseded by 0045) |
| [0009](0009-xp.md) | Removal of experience | Kept; `xp` module of the `fallow` mod 0.2.0 |
| [0010](0010-musique.md) | Music | True Adaptive Music and the `fallow` pack kept; Reactive Music rejected |
| [0011](0011-tectonic-streams-distant-horizons.md) | Terrain and far view | Tectonic + Streams Reflowing; Distant Horizons hidden by the mist, `horizon` module; the "on NML" part superseded by 0019 |
| [0012](0012-sacs-piles-jours.md) | Bags, stack sizes and day length | Reliable Backpacks and Comforts, Sensible Stackables, Better Days 30/15; Ecliptic Seasons kept |
| [0013](0013-premiers-gestes.md) | First gestures: tools needed for logs and rock | `outils` module of the `fallow` mod and Overgeared's knapping; no new mod |
| [0014](0014-tannery.md) | Tannery mod instead of the scripted tannery | Tannery: Better Leather, hides for every leather animal, satchels removed (replaced by 0038) |
| [0015](0015-textures.md) | Texture pack for the world | Vanilla Evolved as `fallow-textures`, without its sky, foliage colours and creatures; mod textures left as they are |
| [0016](0016-vent-et-lumiere.md) | Wind, light and sky | Where Winds Blow (removed by 0032) and Shine, no shader pack; Better Clouds removed, ATMOSPHERICS draws clouds, sun, moon and stars; Shine removed by 0026 |
| [0017](0017-averse-et-rayons.md) | Rain, storm and sunlight | Seven client mods (rain sounds and drops, distant thunder, drips, footprints, waves), olive greens, `averse` and `rayons` modules of the `fallow` mod; all removed by 0026 |
| [0018](0018-effets-en-pack-de-ressources.md) | Effects in the resource pack | Rain colour, lightning through the mist and sun rays move into `fallow-ambiance` (core shaders, Polytone post shader); `fallow` 0.6.3 keeps what needs new geometry or game data; the pack and Polytone are gone (0024, 0026) |
| [0019](0019-leaving-no-mans-land.md) | Leaving No Man's Land | Tectonic + Regions Unexplored + Geophilic kept (Lithosphere compared and set aside on 2026-11-13), world of radius 6,000; not played |
| [0020](0020-clearings-keep-their-biome.md) | Clearings keep their biome | A clearing no longer replaces the biome: the server syncs a list of discs, the client shows the mist lifted and the vivid colours; climate, seasons, crops and fauna stay those of the place (`fallow` 0.8.0) |
| [0021](0021-spelunkery-and-dinosaurs.md) | Spelunkery and Archie's Dinosaurs | Both join the pack; the Nether and End dinosaurs move to cave biomes, Spelunkery is sorted like the other mods, the streams keep their thaw, the dinosaurs' library is kept as a jar |
| [0022](0022-disc-world.md) | A disc world with an edge | One continent on a computed disc (coast near 5,700, edge at 6,000), north cold and south hot, `scripts/terrain/disc.py`; the `bord` module of the `fallow` mod turns players back at the edge; the edge seen in a test client on 2026-11-13 (on foot, by teleport, in a boat); the continent mapped by sampling on two seeds, not walked yet |
| 0023 | Withdrawn (replaced by 0039) | — |
| [0024](0024-removing-the-house-resource-packs.md) | The house resource packs leave the pack | `fallow-interface`, `fallow-textures` and `fallow-ambiance` are removed, mist preset and free tracks included; the `titres` module goes (`fallow` 0.10.1); the menus stayed in FancyMenu until 0026 |
| [0025](0025-native-atlas.md) | The atlas runs natively | Surveyor, Antique Atlas 4 and Antique Transport ported to NeoForge in `salvagedisc/fallow-atlas`; Connector removed, Forgified Fabric API kept for Puddles & Floods |
| [0026](0026-second-cleanup.md) | Second cleanup: house effects, menus and leftovers | 15 mods and the Supernova pack removed (FancyMenu and Drippy, the rain mods, Shine, Polytone, Chunky, Biolith copy…), `averse` and `rayons` modules removed (`fallow` 0.11.0), Distant Horizons warnings and Hamster Tips book off, music on 30 game tracks; the mist waits for the rendering study |
| 0027 | Withdrawn (replaced by 0039) | — |
| [0028](0028-house-shader.md) | A house shader pack written from scratch | Brumaire in `pack/shaderpacks/`, off by default; Iris, Colorwheel and Iris Veil Compat; the mist follows the clearings; forks of Complementary and BSL ruled out by their licences |
| [0029](0029-look-without-shader.md) | ~~The default look comes from a house resource pack~~ (replaced by 0031) | `fallow-ambiance` with Polytone, on by default: sun shadows from a height field given by the `fallow` mod, sun shafts, air, under water, grade; Recrafted is the model, none of its files are used; Brumaire stays as an option |
| [0030](0030-farmers-delight-kitchen.md) | Farmer's Delight replaces Kaleidoscope Cookery | Farmer's Delight, Hands-On Delight (+ Diegesis), Create: Central Kitchen and Cinematic Interactions; Kaleidoscope Cookery, Compat and Automation leave; knives still go through the forge |
| [0031](0031-brumaire-is-the-look.md) | Brumaire is the look of the pack | The shader pack is on from the first launch and takes every effect of Recrafted; `fallow-ambiance`, Polytone and the `relief` module leave; ATMOSPHERICS stays for players without shader |
| [0032](0032-house-wind.md) | The pack's own wind | `vent` module of `fallow` 0.16.0: wind from the weather, gusts, campfire smoke after Better Smoke; Brumaire bends grass under steps and slants rain; Where Winds Blow and ToucanLib removed |
| [0033](0033-zoo-animal-mods.md) | Four animal mods for the zoo | Koopa's Critters, Spawn (without its islands and structures), Hybrid Birds and WilderNature; one model per species, the cut ones out of the zoo |
| [0034](0034-starcatcher-fishing.md) | Fishing with Starcatcher | Starcatcher, its Hybrid Aquatic addon and Starcatcher's Delight; one rod, catches fitted to a world without End and with a closed Nether, one model per species |
| [0035](0035-ore-progression.md) | Ores and armour progression | Kept; built in five steps ([progression](../plan/progression.md)) |
| [0036](0036-better-combat.md) | Animated combat with Better Combat | Better Combat and playerAnimator: a swing of the whole body per weapon type, tools and forged parts excluded, black steel weapons added |
| [0037](0037-ragdoll-corpse.md) | The body left behind | Sable: Ragdoll Corpse and Sable: Ragdolls: on death the inventory stays in a ragdoll body, no self-ragdoll key |
| [0038](0038-house-tannery.md) | A house tannery instead of the Tannery mod | `tannerie` module: raw hide scraped on a fleshing beam, tanned in a vat with bark, dried on a frame by sun or fire; Tannery removed |
| [0039](0039-pack-name.md) | The pack's name: Fallow, under the `fallow-pack` slug | Kept: titled Fallow on Modrinth (`fallow-pack`) and CurseForge (`fallow`); every identifier uses `fallow` |
| [0040](0040-removing-cinematic-interactions.md) | Removing Cinematic Interactions | Its jar, embedded because the mod is only on CurseForge, held back the first Modrinth version; the vanilla stations open their usual windows again |
| [0041](0041-gameplay-without-shader.md) | The game does not depend on the shader | Brumaire is a visual layer: the mod gives the clearing mist, the storms and the wind without it; the shader draws no creature or solid thing the world does not hold, nor what a mod already draws |
| [0042](0042-interaction-language.md) | One interaction language for the stations | Menu, gesture, craft skill; the pot cooks without stirring, faster Spatial GUI windows, « vous » in the tool hints |
| [0043](0043-hands-on-vanilla.md) | Hands-On Vanilla for the vanilla stations | Crafting table, furnaces, anvil and other vanilla stations worked by hand; the `etabli` prototype removed |
| [0044](0044-sable-and-create.md) | Sable and Create machines | Six mods bring Create machines and pack mods onto Sable ships; Loconautics waits for Modrinth |
| [0045](0045-awakening.md) | Awakening: enchanting as a deed | An item awakens from its own deeds, great beasts leave shards, nothing is found enchanted; Immersive Enchanting removed (replaces 0008) |
| [0046](0046-wind-on-ships.md) | Wind on ships, and a headwind at the edge | The pack's wind pushes Sable ships; at the disc's edge a rising headwind pushes players, mounts, boats and ships back (refines 0022) |
| [0047](0047-small-boats.md) | Small boats | Fish 'N' Ships (canoe, sailboat, rafts, fishing boat) and three mods that improve boat handling; big ships stay Sable's |
| [0048](0048-ground-debris.md) | Ground debris | House rocks, fallen wood and pine cones that follow the ground and the tree above, after This Rocks!; Twigs' pebbles and twigs no longer generate (superseded by 0052) |
| [0049](0049-weather-on-ships.md) | Weather on ships | Storm gusts jolt and heel Sable ships, real snow settles on their decks and weighs them down, Fish 'N' Ships' sail follows the pack's wind |
| [0050](0050-rope-ferry.md) | The rope ferry | Two posts, a rope and a pulley: a raft becomes a Sable body that glides along the rope, carried over by the current |
| [0051](0051-big-catch-tows.md) | A big catch tows the boat | During Starcatcher's fight a heavy fish takes a light boat or Sable raft along; a heavy hull holds |
| [0052](0052-gestures-not-debris.md) | Gestures, not ground debris | Nothing laid on the ground: sticks from shaking leaves, rocks from digging; the `debris` module removed; Passable Foliage without its climbing enchantment (replaces 0048) |

## Template

```markdown
# NNNN — Subject

- **Date**: YYYY-MM-DD
- **Status**: kept / under test on `test/<subject>` / pending / superseded by NNNN

## Context
Why the question arises, what the vision requires.

## Options
| Criterion | Option A | Option B |
|---|---|---|

## Decision
The choice, and the conditions that go with it (configs, KubeJS scripts, settings).

## Rejected
- Mod: reason.
```
