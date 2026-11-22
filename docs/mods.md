# Mod registry

Complete list of the pack, updated on 2026-11-14 (after the second cleanup, [0026](decisions/0026-second-cleanup.md), and after PRs #1 to #32 and the change of terrain base, [0019](decisions/0019-leaving-no-mans-land.md)): what is in `pack/mods/`, what remains to be tested and what is dropped. Every mod listed ✅ is in the pack today; nothing is waiting to be added. Updates held back until the in-game tests are listed in the [roadmap](feuille-de-route.md#updates-held-until-the-in-game-tests); the tests themselves are grouped in [tests-en-jeu.md](tests-en-jeu.md). The reasoning behind each choice is in the [plan](plan/README.md), especially chapters [10](archive/plan/10-recentrage.md), [11](archive/plan/11-rendu-et-confort.md) and [12](archive/plan/12-verification.md).

Statuses:
- ✅ in the pack (`pack/mods/`);
- ➕ to add (kept by the plan, not yet in `pack/mods/`);
- 🟡 to test before deciding;
- ⚪ optional;
- ❌ leaves the pack.

Side: C = client, S = server, CS = both. Versions checked on Modrinth for NeoForge 1.21.1 on 2026-10-25.

## World and exploration

| Mod | Slug | Status | Side | Role |
|---|---|---|---|---|
| Tectonic | `tectonic` | ✅ (3.0.28, not seen in game) | CS | The relief of the world: continents, mountain ranges, valleys, underground rivers; height -64 to 320, never to change once the world exists; requires Lithostitched without declaring it ([0011](decisions/0011-tectonic-streams-distant-horizons.md), [0019](decisions/0019-leaving-no-mans-land.md)). Lithosphere is still to be compared with it in game |
| Regions Unexplored | `regions-unexplored` | ✅ (0.6.3, added 2026-11-13, not seen in game) | CS | The biomes on Tectonic's terrain: new woods and plants, cave biomes; MIT; requires Lithostitched ([0019](decisions/0019-leaving-no-mans-land.md)). French translation by the pack; its biomes take the mods' rules of a vanilla model biome (`scripts/biomes/`) |
| Geophilic | `geophilic` | ✅ (3.7, added 2026-11-13, not seen in game) | CS | Vanilla biomes reworked, no new block ([0019](decisions/0019-leaving-no-mans-land.md)) |
| Streams Reflowing | `streams-reflowing` | ✅ (2.17.5, not seen in game) | CS | Streams and lakes that follow the relief, currents for boats and Sable hulls; world settings frozen at creation ([0011](decisions/0011-tectonic-streams-distant-horizons.md)) |
| Spelunkery | `spelunkery` | ✅ (0.4.4, added 2026-11-13, not seen in game) | CS | Underground: ores in every stone, rough gems to polish at the grindstone, raw nuggets, salt, nephrite, cave mushrooms, sluices, rope ladder, glowsticks, parachute; experience storage, locators and Nether items sorted out; requires Moonlight Lib ([0021](decisions/0021-spelunkery-and-dinosaurs.md)). French translation completed in `pack/kubejs/assets/spelunkery/` |
| Surface Samples | `surface-samples` | ✅ (0.2.0, added 2026-11-17, not seen in game) | CS | Small ore stones on the ground; the pack places them above its veins (`fallow` `filons` module, [progression](plan/progression.md#veins-and-outcrops-sixth-step)), broken they give a few nuggets or a coal. Its test, Nether and absent-metal samples sorted out. French translation in `pack/kubejs/assets/surfacesamples/` |
| Sparse Structures | `sparsestructures` | ✅ | CS | Spaced-out structures ([0004](decisions/0004-structures.md)) |
| Ancient Remnants: Monoliths | `ancient-remnants` | ✅ | CS | 6 monuments placed on suitable ground, blessings, codex to piece together from ruins; four guarded by Mowzie's Mobs and Companions! bosses ([monuments](plan/monuments.md)) |
| Lootr | `lootr` | ✅ | CS | Each player finds the chests full |
| Enhanced Celestials 2 (Core + Default Lunar Events) | `enhanced-celestials-2-core`, `enhanced-celestials-2-default-lunar-events` | ✅ | CS | Rare moons; blood moons removed. Replaces Enhanced Celestials 6; Shader Support addon useless without Iris |
| Antique Atlas 4 | jar in `pack/mods/` | ✅ (NeoForge port, [0025](decisions/0025-native-atlas.md)) | C | Hand-drawn map, no coordinates |
| Antique Transport | jar in `pack/mods/` | ✅ (added 2026-10-25, NeoForge port) | CS | Create trains, tracks, stations and Sable ships appear on the atlas |
| Surveyor Map Framework | jar in `pack/mods/` | ✅ (NeoForge port) | CS | Shared exploration for the atlas |
| Icy's Better Horses | `icys-better-horses` | ✅ (2.0.1; 2.1.x held, see the roadmap) | CS | Horses, breeds, bond, carts |
| Fish 'N' Ships | `fish-n-ships` | ✅ (1.1.0, added 2026-11-21, ARR, [0047](decisions/0047-small-boats.md)) | CS | Small craft in every vanilla wood: canoe (three seats, or a chest), sailboat with a banner as its sail, raft and supply raft, fishing boat |
| Better Boat Movement | `better-boat-movement` | ✅ (2.5.4, added 2026-11-21, GPL-3.0) | CS | A boat climbs a step, a slab or a bank out of the water instead of stopping against it |
| BoatView360 | `boatview360` | ✅ (1.0.6, added 2026-11-21, LGPL-3.0) | C | A passenger can look all around, not only 210° |
| BoatHandsFree | `boathandsfree` | ✅ (1.0.3, added 2026-11-21, LGPL-3.0) | C | Hit, use an item or interact while rowing |

## Long-term goals

| Mod | Slug | Status | Side | Role |
|---|---|---|---|---|
| Questlog | `questlog` | ✅ (added 2026-10-26) | CS | Book-style journal of quests and guides, by chapter, editable in game ([quetes](plan/quetes.md)) |
| Exposure | `exposure` | ✅ | CS | Analogue photography: keep a place, discover a species |

## Civilisation and technology

| Mod | Slug | Status | Side | Role |
|---|---|---|---|---|
| Create | `create` | ✅ | CS | Technology |
| Create Aeronautics + Sable | `create-aeronautics`, `sable` | ✅ (Sable 2.0.6) | CS | Airships; requires NeoForge 1.21.1 |
| Create Aeronautics: Automated Logistics | `create-aeronautics-automated-logistics` | ✅ (added 2026-10-25) | CS | Recorded airship routes, airship stations, transponders: an air network as a long-term goal |
| Sable: Physics Compat | `sablecompat` | ✅ (added 2026-10-25) | CS | Mass, buoyancy and friction for the blocks of about 100 mods on Sable ships (its `sable:bouncy` tag fails to load, harmless) |
| Aeronautics Wind Sound | `aeronautics-wind-sound` | ✅ (added 2026-10-25) | C | Wind in flight, by speed and shelter |
| Sable Beyond | `sable_beyond` | ✅ (0.5.0 alpha, added 2026-11-21, MIT) | CS | Create machines on Sable ships: the mechanical arm reaches across sub-levels, encased fans push and pull them, water wheels turning in water drive a boat, fire spreads between a ship and the world and goes out in water; its push from flowing fluids is off (Streams Reflowing already carries hulls) and so are its menu buttons (`config/sable_beyond/`) ([0044](decisions/0044-sable-and-create.md)) |
| Create Aeronautics Addon: Plough & Roll! | `createaeroploughroll` | ✅ (1.2.0 alpha, added 2026-11-21, MIT) | CS | Mechanical ploughs and rollers mounted on a Sable ship work the ground below as it moves; normal rollers replace existing roadbed again |
| Aeronautics Display Link Fix | `aeronautics-display-link-fix` | ✅ (1.3, added 2026-11-21, MIT) | CS | A display link keeps its target when its ship is assembled or set down |
| Sable Fix Mod | `sable-fix-mod` | ✅ (1.1.0, added 2026-11-21, MIT) | S | No more duplicated items, rails, torches, redstone or crops when a ship is assembled or set down |
| Sable Pathfinder | `sable-pathfinder` | ✅ (1.4.0, added 2026-11-21, ARR) | CS | Animals, companions and mounts find their way onto a ship, off it and between ships |
| Sable: Sure Footing | `sable-sure-footing` | ✅ (1.2.1, added 2026-11-21, MIT) | CS | A jump on a moving or turning deck lands where it started; dropped items, animals and smoke stay with the ship |
| Create Aeronautics: Compatibility | `create-aeronautics-compatability` | ✅ (1.1.3, added 2026-11-21, MIT) | CS | Alex's Mobs creatures no longer exhaust the memory trying to path-find on a ship |
| Furniture with Create and Sable compat | `lets-do-furniture-another-furniture-handcrafted-create-sable` | ✅ (5.0, added 2026-11-21, ARR) | CS | Handcrafted, Quark and Every Compat seats stay usable on a Create contraption or a Sable ship |
| Create: Steam 'n' Rails | `create-steam-n-rails-1.21.1` | ✅ | CS | The railway (beta) |
| Overgeared | `overgeared` | ✅ | CS | Forging: blast furnace, hammering, quenching, quality, blueprints |
| Create: Central Kitchen | `create-central-kitchen` | ✅ (2.6.2b, added 2026-11-15) | CS | Create automates Farmer's Delight: deployers cut on the board, the mechanical arm feeds the cooking pot; requires Create: Dragons Plus 1.11.9 ([0030](decisions/0030-farmers-delight-kitchen.md)) |

## Daily life

| Mod | Slug | Status | Side | Role |
|---|---|---|---|---|
| Farmer's Delight | `farmers-delight` | ✅ (1.21.1-1.3.4, added 2026-11-15) | CS | Cooking: cooking pot, cutting board, skillet, stove, keg, knives through the forge ([0030](decisions/0030-farmers-delight-kitchen.md)) |
| Hands-On Delight + Diegesis | `handson-delight`, `diegesis-api` | ✅ (1.6.1 and 1.0.0-beta.14, added 2026-11-15, not seen in game) | CS | Farmer's Delight's stations used by hand in the world, first-person scenes; stirring stars give extra servings (no setting); translated by the pack ([0030](decisions/0030-farmers-delight-kitchen.md)) |
| Hands-On Vanilla | jar in `pack/mods/` | ✅ (1.2.2, added 2026-11-21, seen on the bench) | C | The vanilla stations worked by hand in the world: crafting table with its recipe book beside it, furnace, smoker, blast furnace, stonecutter, smithing table, brewing stand, anvil, lectern, loom, cartography table; the enchanting table (the `fallow` mod's awakening, [0045](decisions/0045-awakening.md)) and the grindstone are left alone (`config/handson_vanilla.properties`); MIT, CurseForge only, by the author of Hands-On Delight; tab names translated by the pack ([0043](decisions/0043-hands-on-vanilla.md)) |
| Starcatcher | `starcatcher` | ✅ (3.3.10, added 2026-11-17, not seen in game) | CS | Fishing: skill minigame per fish, catches by biome, depth, hour, weather and bait, catalogue, trophies, aquarium, tackle box, hooks, bobbers, baits, rod skins; the vanilla rod merges into its rod; End catches removed, Nether ones moved to deep Overworld lava; its selling bin removed (no money) ([0034](decisions/0034-starcatcher-fishing.md)) |
| Hybrid Aquatic + Starcatcher Compatibility | `hybrid-aquatic-starcatcher-compatibility` | ✅ (0.6.3, added 2026-11-17) | CS | Hybrid Aquatic's fish, crates and hooks in Starcatcher; the species cut for another mod's model give that mod's fish or are no longer fished ([0034](decisions/0034-starcatcher-fishing.md)) |
| Starcatcher's Delight | `starcatchers-delight` | ✅ (3.2, added 2026-11-17, not seen in game) | CS | Fish dishes (nigiri, maki, fillets, stews) in Farmer's Delight's pot and on its board; its Farm & Charm copies are not copied again by `cuisine.js` ([0034](decisions/0034-starcatcher-fishing.md)) |
| [Let's Do] Farm & Charm | `lets-do-farm-charm` | ✅ (stay on 1.1.25) | CS | The farm only: fertiliser, sprinkler, silo, plough, cart, coop, troughs, mincer, barley, oat, corn, strawberry, lettuce, tomato, onion; its cooking stations are removed and their recipes moved to Farmer's Delight; its meats, pasta, dough, flour, dog food and rope merge into Farmer's Delight's, Create's and Supplementaries' ([0030](decisions/0030-farmers-delight-kitchen.md)) |
| Ecliptic Seasons | `ecliptic-seasons` | ✅ (0.15.5.1) | CS | Seasons, farming only (`AGRICULTURE`), no heat stroke: Cold Sweat owns the temperature |
| Cold Sweat | `cold-sweat` | ✅ (stays on 2.4.3.1 for Altitude) | CS | Temperature |
| Cold Sweat: Altitude | `cold-sweat-altitude` | ✅ (added 2026-10-25) | CS | Colder as you climb, sheltered areas, windproof lining; supports airships |
| Thirst Was Reclaimed | `thirst-was-reclaimed` | ✅ | CS | Thirst |
| Reliable Backpacks | `reliable-backpacks` | ✅ (added 2026-10-26) | CS | A single simple bag, three rows, worn on the back (Curios), placeable ([0012](decisions/0012-sacs-piles-jours.md)) |
| Comforts | `comforts` | ✅ (back on 2026-10-26) | CS | Sleeping bag and hammock ([0012](decisions/0012-sacs-piles-jours.md)) |
| Sensible Stackables | `sensible-stackables` | ✅ (added 2026-10-26) | CS | Stack sizes by kind of item, set by the `fallow:pile/N` tags ([0012](decisions/0012-sacs-piles-jours.md)) |
| Better Days | `betterdays` | ✅ (added 2026-10-26) | CS | 30 minutes of day, 15 of night ([0012](decisions/0012-sacs-piles-jours.md)) |
| Envelope | `envelope` | ✅ | CS | Mail by pigeon between players, lost letters to find (idea taken from Wayfarer, [research](archive/plan/recherche/2026-10-19/wayfarer.md)) |

## Fauna

| Mod | Slug | Status | Side | Role |
|---|---|---|---|---|
| Naturalist | `naturalist` | ✅ | CS | Realistic base fauna ([0003](decisions/0003-faune.md)) |
| Alex's Mobs Continued | `alexs-mobs-continued` | ✅ | CS | Variety, species by species; seal, bison, raccoon and seagull cut on 2026-11-17 for Spawn's, WilderNature's and Hybrid Birds'; nine creatures with weak animations cut (banana slug, alligator snapping turtle, mungus, triops, sunbird, guster, rocky roller, frilled shark, potoo, [duplicates](plan/doublons.md#creatures-with-weak-animations-2026-10-28)) |
| Hybrid Aquatic | `hybrid-aquatic` | ✅ (biomes disabled) | CS | Oceans; seven near-motionless invertebrates cut (sea urchin, comb jelly, garden eel, nautilus, sea slug, sea cucumber, starfish) |
| Mowzie's Mobs | `mowzies-mobs` | ✅ | CS | Unique, very rare creatures; Wroughtnaut, Frostmaw and Sculptor guard three monuments; natural spawns rarer (`extra_rarity`), only in the dark, two per player at most; Wroughtnaut chambers, Frostmaw and Umvuthana groves no longer generated, boss weapons removed ([pack sort](plan/tri-du-pack.md)) |
| Longwings | `longwings` | ✅ | CS | Butterflies to collect |
| Archie's Dinosaurs | `archies-dinosaurs` | ✅ (1.0.4, added 2026-11-13, not seen in game) | CS | Six dinosaurs to find, tame and ride, amber craters and fossils, the trader Archie; the Nether and End species moved to the redstone caves and the prismachasm; young mod, All Rights Reserved, with a library kept as a jar ([0021](decisions/0021-spelunkery-and-dinosaurs.md)) |
| Koopa's Critters | `koopas-critters` | ✅ (0.5, added 2026-11-17, not seen in game) | CS | About fifty rare and threatened species for the zoo; its termite mounds off, its goblin shark and manta ray replace Hybrid Aquatic's ([0033](decisions/0033-zoo-animal-mods.md)) |
| Spawn | `spawn-mod` | ✅ (4.0.8, added 2026-11-17, not seen in game) | CS | Sea life (sea cow, sunfish, seahorse, octopus, seal, spider crab); islands, octopus cities and ant mounds do not generate; herring, tuna, barracuda, hamster, ant, clam and snail cut as [duplicates](plan/doublons.md#zoo-mods-2026-11-17) ([0033](decisions/0033-zoo-animal-mods.md)) |
| Hybrid Birds | `hybrid-birds` | ✅ (1.3.3, added 2026-11-17, not seen in game) | CS | Peacock, swan, goose, pelican, albatross, puffin, guineafowl, turkey, rooster ([0033](decisions/0033-zoo-animal-mods.md)) |
| [Let's Do] WilderNature | `lets-do-wildernature` | ✅ (1.1.8, added 2026-11-17, not seen in game) | CS | Bison, raccoon, beaver, cassowary, red wolf, swift fox, squirrel, mini sheep, scorpion, trophies; its deer, boar, turkey, hedgehog and savanna animals off (Naturalist's stay), vanilla farm animals kept (`config/wildernature-common.toml`) ([0033](decisions/0033-zoo-animal-mods.md)) |

## Companions

| Mod | Slug | Status | Side | Role |
|---|---|---|---|---|
| Critters and Companions | `critters-and-companions` | ✅ | CS | Tameable small fauna; koi and sea bunny cut (weak animations) |
| Friends & Foes | `friends-and-foes-forge` | ✅ | CS | Peaceful creatures from the mob vote; Iceologer, Illusioner, Wildfire, their structures and the zombie horse trap disabled, moobloom (vanilla cow model, not animated by Fresh Animations) and glare cut, fewer Maulers and crabs (`pack/config/friendsandfoes.json`) |
| Adorable Hamster Pets | `adorable-hamster-pets` | ✅ | CS | Hamsters; throwing, item stealing, diamond sniffing and redstone fever disabled |
| Companion | `companion` | ✅ | CS | Tamed animals no longer get lost |
| Companions! | `companions-mod` | ✅ | CS | Companions with powers; the Sacred Pontiff guards the oak monument; wild spawns about three times rarer |
| Ghost | `ghost` | ✅ | CS | Little ghosts |

## Building and workshops

| Mod | Slug | Status | Side | Role |
|---|---|---|---|---|
| Quark | `quark` | ✅ (4.1-487) | CS | The essentials, selected modules ([0005](decisions/0005-quark.md)) |
| Supplementaries | `supplementaries` | ✅ | CS | The essentials ([0006](decisions/0006-supplementaries.md)); its sconces are enabled again since 2026-11-13 (they were off while No Man's Land had its own) |
| Amendments | `amendments` | ✅ | CS | 3D lanterns, signs, cauldrons |
| Handcrafted | `handcrafted` | ✅ | CS | Furniture |
| Twigs | `twigs` | ✅ | CS | Small decor; rhyolite generation off, tables, Nether blocks and duplicates removed; cobblestone bricks back since 2026-11-13 ([pack sort](plan/tri-du-pack.md)); its pebbles and twigs no longer generate since 2026-11-21, sticks and rocks now come from gestures, shaking leaves and digging ([0052](decisions/0052-gestures-not-debris.md)); its seashells stay |
| Every Compat | `every-compat` | ✅ | CS | Furniture and decor in every wood, including those of Regions Unexplored |
| HT's TreeChop | `treechop` | ✅ | CS | Chop a tree block by block, the trunk falls last |

## Body, camera and gestures

| Mod | Slug | Status | Side | Role |
|---|---|---|---|---|
| Entity Model Features + Entity Texture Features | `entity-model-features`, `entitytexturefeatures` | ✅ | C | Engine of Fresh Animations |
| 3D Skin Layers | `3dskinlayers` | ✅ | C | Skin with relief |
| Shoulder Surfing Reloaded | `shoulder-surfing-reloaded` | ✅ | C | A single third-person view, over the shoulder, camera freely controlled with the mouse (F5: first-person or shoulder) |
| Spatial GUI | `spatial-gui` | ✅ | C | 3D menus, shoulder camera on opening |
| Camera Overhaul | `cameraoverhaul` | ✅ | C | The camera follows movement (set low) |
| ItemPhysic | `itemphysic` | ✅ | C | Dropped items fall, roll and float |
| Smooth Swapping | `smooth-swapping` | ✅ | C | Items slide around in the inventory |
| Punchy! | `punchy-fpa` | ✅ (2.8g, pinned version) | C | Gestures and animated arms in first person, set to a restrained level; alone in animating the first-person arm (EMF option), blacklist for Create and Aeronautics items and Exposure photos; `fallow` 0.4.7 stops its equip animation looping with Sensible Stackables ([fix](plan/mod-fallow.md#punchy-and-sensible-stackables-the-hand-that-kept-jumping)) |
| Better Combat | `better-combat` | ✅ (2.4.0, added 2026-11-17, not seen in game) | CS | A swing of the whole body per weapon type and combos, hits along the blade; tools, fishing rods and forged parts excluded, black steel sword and axe added ([0036](decisions/0036-better-combat.md)) |
| playerAnimator | `playeranimator` | ✅ (2.0.4, added 2026-11-17) | CS | Animation library of Better Combat |
| Sable: Ragdoll Corpse | `sable-ragdoll-corpse` | ✅ (0.4.0, added 2026-11-18, ARR) | CS | On death the player leaves a ragdoll body that keeps the whole inventory, Curios included, until it is emptied; it can be dragged, thrown or lifted by an airship; the recovery compass points to it ([0037](decisions/0037-ragdoll-corpse.md)) |
| Sable: Ragdolls | `sable-ragdolls` | ✅ (0.8.1, added 2026-11-18, Apache-2.0) | CS | Ragdoll library of the body above; the key that throws oneself to the ground is off on the server (`config/sable_player_ragdoll-server.toml`) |
| Sable: Ragdoll Patch | `sableragdoll-patch` | ✅ (1.9.1, added 2026-11-21, MIT) | CS | Fixes for the ragdoll body: Punchy's arm hidden, Curios and capes drawn right, held torches lit (LambDynamicLights), no grabbing a chest or door while dragging a body; bodies have no collision by default (`/nocollide`) ([0044](decisions/0044-sable-and-create.md)) |

## Atmosphere, rendering and sound

| Mod | Slug | Status | Side | Role |
|---|---|---|---|---|
| ATMOSPHERICS | `atmospherics` | ✅ | C | Mist, sky, clouds (two layers), sun, moon and stars per biome ([0007](decisions/0007-ambiance-performance.md)) |
| Particular Reforged | `particular-reforged` | ✅ | C | Water, waterfalls, falling leaves (a little more often), cave dust; its firefly particles are off (Naturalist's fireflies are the pack's, [0041](decisions/0041-gameplay-without-shader.md)) |
| Subtle Effects | `subtle-effects` | ✅ | CS | Fire, smoke, steam, body; pollen also in the flower, meadow and orchard biomes of Regions Unexplored, spores in its fungal fen, its own fireflies off because Particular already has them (`pack/config/subtle_effects/environment.toml`) |
| LambDynamicLights | `lambdynamiclights` | ✅ | C | Light held in hand |
| More Darkness | `more-darkness` | ✅ | CS | Real nights |
| AmbientSounds | `ambientsounds` | ✅ | C | Biome sounds |
| Sound Physics: Aeronautics | `sound-physics-aeronautics` | ✅ (2.0.1, added 2026-11-21, GPL-3.0) | CS | Echo, muffling; a fork of Sound Physics Remastered (removed, same mod id) that also muffles sound through ship hulls and contraptions, adds the Doppler effect and far propeller sound; its wing windrush plays beside Aeronautics Wind Sound, to weigh in game ([0044](decisions/0044-sable-and-create.md)) |
| Separate Sable Render Distance | `ssrd` | ✅ (1.8.8, added 2026-11-21, ARR) | CS | Ships stay visible as far as Distant Horizons draws, not only to the normal view distance; to check in game with Brumaire ([0044](decisions/0044-sable-and-create.md)) |
| True Adaptive Music | `true-adaptive-music` | ✅ | CS | Music by situation (mist, clearing, season, moon, airship), long silences ([0010](decisions/0010-musique.md)) |
| `fallow` music pack | — | ✅ (`pack/trueadaptivemusicpacks/`, [page](plan/musique.md)) | CS | The tree of situations and the 25 tracks chosen by the team (29 until No Man's Land left) |
| Sounds | `sound` | ✅ | C | 170 interface, item and block sounds |
| Auroras | `auroras` | ✅ | C | Aurora in cold biomes |
| Contraption Lights | `contraption-lights` | ✅ | CS | A lantern placed on an airship or contraption lights up the world as it passes (LambDynamicLights mode, colour disabled) |
| Wakes Reforged | `wakes-reforged` | ❌ (removed 2026-11-17) | CS | Wakes behind boats, swimmers and Sable contraptions; its trail and splash planes drew over Brumaire's own wakes, which replace it (Sable contraptions no longer leave a wake) |
| Rainbows | `rainboows` | ✅ | C | Rainbow after the rain |
| Distant Horizons | `distanthorizons` | ✅ (3.3.3, not seen in game) | CS | Distant landscape (LODs), hidden by the mist and revealed where it is lifted by the `horizon` module of the `fallow` mod ([0011](decisions/0011-tectonic-streams-distant-horizons.md)) |
| Falling Leaves Plus | `falling-leaves-plus` | ⚪ | C | Fallback if Particular does not handle the leaves of Regions Unexplored |

## Interface and comfort

| Mod | Slug | Status | Side | Role |
|---|---|---|---|---|
| EMI | `emi` | ✅ | C (S optional) | Recipe viewer, without panels or search (`pack/defaultconfigs/emi.css`): R and U on an item open its recipes |
| JEI | `jei` | ✅ (hidden, stay on 19.51.0.418) | CS | Bridge for mods that only have a JEI plugin; from 19.52, Envelope 0.7.5's mixin no longer finds its target and the game crashes when entering a world |
| Better Advanced Tooltips | `better-advanced-tooltips` | ✅ | C | Tooltips; KubeJS declares it as an optional client dependency, client only |
| Pick Up Notifier | `pick-up-notifier` | ✅ | C | Discreet list of picked-up items |
| Mouse Tweaks | `mouse-tweaks` | ✅ | C | Mouse-driven inventory |
| Controlling + Searchables | `controlling`, `searchables` | ✅ | C | Search in key bindings |
| Default Options | `default-options` | ✅ | C | Key bindings and video settings shipped with the pack |
| Chat Heads | `chat-heads` | ✅ | C | Player head in the chat |
| Lamb Lanterns | `lamb-lanterns` | ✅ | CS | A lantern carried at the hip (Curios slot) gives light as if held in hand |
| Chalk (+ Chalk: Chars) | `chalk-mod`, `chalk-chars` | ✅ (2.0.3 and 1.0.1, added 2026-11-22, not seen in game) | CS | Chalk marks drawn on blocks (arrows, crosses, symbols, letters and figures with Chalk: Chars), in sixteen colours, glowing with glow ink; a chalk box holds several. A way-marking gesture for the mist. By mortuusars (Exposure, Scholar); its library Mortaar comes inside the jar. Missing French by the pack |
| Sooty Chimneys | `sooty-chimneys` | ✅ (1.3.5, added 2026-11-22, not seen in game) | CS | Chimneys in bricks, cobblestone, stone bricks, mud bricks, iron, copper and terracotta that smoke over a lit fire and get covered in soot, scraped with an axe (black dye now and then); an empty-handed click shuts the flue. Its own wind is off (`Wind`, `WindAffectsCampfire` false in `pack/config/sootychimneys-common.toml`): the smoke follows the pack's wind (`vent` module). By mortuusars. French by the pack |
| Scholar | `scholar` | ✅ (1.2.10, added 2026-11-21, not seen in game) | CS | Writable and written books on two pages, with formatting without § codes, undo, bookmarks, import and export to a text file; books dyed like leather; a written book's title on a lectern or in a chiseled bookshelf on hover; zombies, husks and skeletons sometimes carry a plain book and read it. A book on a lectern stays Hands-On Vanilla's (read on the lectern): `replace_screens_on_lectern = false` in `pack/config/scholar-common.toml`, without which Scholar's lectern screen takes over; Scholar's screens are the ones opened from the hand. Same author as Exposure; French by the pack |
| Passable Foliage | `passable-foliage` | ✅ (9.3.2+neoforge, added 2026-11-21, not seen in game) | CS | Leaves are walked through, slowing a little, and a fall into a canopy keeps half its damage (its own cushion is set to nothing, `fallDamageMultiplier: 1.0`, and the `feuillage` module of `fallow` gives half back); mobs path through them too. Its Leaf Walker enchantment (standing on and climbing leaves) is off: `modules.enchantment: false` in `pack/config/passablefoliage-common.yaml`, which also removes its book recipe. Needs Kiwi, already in the pack. French by the pack |

## Rules and scripts

| Mod | Slug | Status | Side | Role |
|---|---|---|---|---|
| KubeJS | `kubejs` | ✅ | CS | Scripts and datapack data |
| KubeJS Create | `kubejs-create` | ✅ | CS | Create recipes in script (beta) |
| LootJS | `lootjs` | ✅ | S | Loot of ruins and monuments |
| MoreJS | `morejs` | ✅ | CS | Extra events for KubeJS |
| In Control! | `in-control` | ✅ | S | Spawn rules (beta): no vanilla monsters in the Overworld, at most 8 hostile creatures per player, Mowzie's and cave creatures of Alex's Mobs only in the dark (`pack/config/incontrol/spawn.json`) |
| Mod `fallow` 0.15.0 | `mods-custom/fallow/` | ✅ (not tested with a player) | CS | What scripts cannot do: `ciel` (moon visible under ATMOSPHERICS, mist; the Friendly Moon parts were removed in 0.7.0), `xp` (anvil without levels, hidden bar and the bars above it lowered, [0009](decisions/0009-xp.md)), `horizon` (Distant Horizons follows the mist, [0011](decisions/0011-tectonic-streams-distant-horizons.md)), `clairiere` (vigil fire and clearing lantern lift the mist), `dissipateur` (Create kinetic block, radius 64), `outils` (wood and rock need the right tool, [first gestures](plan/premiers-gestes.md)), `monuments` (structures placed only on fitting ground) `bord` (the edge of the disc world, [0022](decisions/0022-disc-world.md)), `rendu` (what the shader pack Brumaire needs, [0028](decisions/0028-house-shader.md)) and `relief` (the height of the world for the sun shadows of the `fallow-ambiance` pack, [0029](decisions/0029-look-without-shader.md)) ([mod-fallow](plan/mod-fallow.md)) |

## Performance and technical

| Mod | Slug | Status | Side | Role |
|---|---|---|---|---|
| Sodium | `sodium` | ✅ | C | Rendering |
| Iris Shaders | `iris` | ✅ (bench only) | C | Loads the house shader pack Brumaire, on by default ([0031](decisions/0031-brumaire-is-the-look.md)); 1.8.14-beta.1 is the only build for Sodium 0.8.13 ([0028](decisions/0028-house-shader.md)) |
| Colorwheel | `colorwheel` | ✅ (bench only) | C | Flywheel models (Create) under a shader pack |
| Iris Veil Compat | `iris-veil-compat` | ✅ (0.3.0, pinned; bench only) | C | Veil shaders (Sable, Aeronautics levitite) under a shader pack. Kept on 0.3.0: 0.4.0 drops water and stained glass from the shader's sun shadow map, which turns off the light under water and the light through stained glass |
| ImmediatelyFast | `immediatelyfast` | ✅ | C | Rendering |
| ModernFix | `modernfix` | ✅ | CS | Loading |
| FerriteCore | `ferrite-core` | ✅ | CS | Memory |
| Lithium | `lithium` | ✅ | CS | Server logic |
| Structure Layout Optimizer | `structure-layout-optimizer` | ✅ | CS | Structure generation |
| spark | `spark` | ✅ | CS | Profiling; background profiler on the Java engine (`config/spark/config.json`): async-profiler crashes the JVM on Apple Silicon Macs |
| Dynamic FPS | `dynamic-fps` | ✅ | C | The game slows down in the background |
| Neruina | `neruina` | ✅ | CS | Prevents a stuck entity from crashing the world |
| Simple Backups | `simple-backups` | ✅ | CS | Backups every hour of play, last 24 kept, plus one when everyone leaves (beta) |
| Crash Assistant | `crash-assistant` | ✅ | C | Helps read crashes |

## Resource packs

| Pack | Slug | Status | Role |
|---|---|---|---|
| Fresh Animations | `fresh-animations` | ✅ | Animated creatures |
| FA: Details, Objects, Emissive | `fresh-animations-details`, `fresh-animations-objects`, `fresh-animations-emissive` | ✅ | Details, animated objects, glowing eyes |
| FA: Player Extension | `fa-player-extension` | ✅ | The player in the same style |
| Motschen's Better Leaves | `better-leaves` | ✅ | Bushy foliage |

## Libraries

Already present: Configurable (Neruina), Architectury API, Balm, Cloth Config, CorgiLib, Data Anchor, Fragmentum, MRU, Puzzles Lib, CodxLib, CreativeCore, Curios API, Fzzy Config, GeckoLib, Hybrid API, Kiwi, Teal Lib (Spawn), Knight Lib, Kotlin for Forge, KotlinLangForge (Better Boat Movement, added 2026-11-21), Lithostitched, McQoy, Modonomicon (required by Icy's Better Horses; its stable handbook has no recipe and is hidden from EMI), Moonlight Lib, Patchouli, Resourceful Config, Resourceful Lib, Rhino, YACL, Zeta, Create: Dragons Plus (Create: Central Kitchen), Diegesis (Hands-On Delight, listed with it above). Astemir Re-Lib (Archie's Dinosaurs) is not a packwiz file but a jar committed in `pack/mods/`: its author withdraws each build from Modrinth within a day ([0021](decisions/0021-spelunkery-and-dinosaurs.md)).

## Dropped or rejected

| Mod | Reason |
|---|---|
| Exposure: Space | Removed on 2026-11-22: its finish is far below Exposure's, and its 256 objects are our real sky (the ISS, Hubble, Halley's comet), out of place over a disc world with its own moon |
| Visual Workbench | Removed on 2026-11-21 ([0043](decisions/0043-hands-on-vanilla.md)): it only changed the vanilla crafting table, into a block of its own that Hands-On Vanilla does not open; the pack has no other crafting table. Puzzles Lib stays for Pick Up Notifier |
| Create: Loconautics | Trains as Sable sub-levels (MIT code, art all rights reserved): only on CurseForge for now, its Modrinth page is announced; to add once it is published ([0044](decisions/0044-sable-and-create.md)) |
| Create: Train Functionality | Buildable train carriages on Sable: only on CurseForge, no source ([0044](decisions/0044-sable-and-create.md)) |
| Cold Sweat Sable Compat, Cold Sweat x Aeronautics | Cold Sweat supports Sable itself since 2.4.1 |
| MassWeightCompat, Sable Weighted | Duplicate Sable: Physics Compat for the mods of the pack |
| Small Ships | Cogs, brigantines, galleys and drakkars with cannons: real ships, not the small craft the team wants ([0047](decisions/0047-small-boats.md)) |
| Boatload | Large boats and furnace boats under the restrictive Abnormals licence ([0047](decisions/0047-small-boats.md)) |
| Old Boats | Pre-1.9 boats again: version 0.3, 71 downloads on 2026-11-21, too young ([0047](decisions/0047-small-boats.md)) |
| Sable Assembly Fix, Aeronautics: Addition, Create Aeronautics Roller Fix | Covered by Sable Fix Mod, Sable Beyond and Plough & Roll! |
| Create: Sable Train Overlay Compat | Shows trains on ships on Create's train map, which needs a map mod the pack does not have (the atlas already marks ships) |
| Cinematic Interactions | Removed on 2026-11-20 ([0040](decisions/0040-removing-cinematic-interactions.md)): a jar embedded in the pack because it is only on CurseForge, which held back the first Modrinth version; the vanilla stations open their usual windows again |
| Chipped (with Athena), Beautify! | Removed on 2026-11-01: 6,980 blocks for Chipped, including 1,110 Nether and End variants; Beautify! duplicates Supplementaries, Amendments and Handcrafted ([pack sort](plan/tri-du-pack.md)) |
| Bosses of Mass Destruction, Create Ore Excavation, GraveStone Mod, Cosy Critters & Creepy Crawlies, Macaw's Roofs, Macaw's Windows, Real Camera, MB-3D Items | Removed by the team on 2026-10-19 |
| FallingTree, ItemPhysic Lite | Replaced by HT's TreeChop and ItemPhysic (team choice on 2026-10-19) |
| Entity Culling | Breaks Aeronautics contraptions; removed on 2026-10-19 |
| AA4 Atlas | Crashes the game when opening the atlas with Antique Atlas 3 (`AtlasScreen.prepareToOpen()` no longer returns the screen); removed on 2026-10-19, the atlas opens with its key |
| Clumps | Grouped XP orbs, which no longer exist ([0009](decisions/0009-xp.md)); removed on 2026-10-19 |
| Freeze It and Heat It | Removed by the team on 2026-10-18: no food preservation |
| Easy Anvils | Replaces the anvil that Overgeared needs ([12](archive/plan/12-verification.md)) |
| Leaves Be Gone | On Aeronautics' blacklist |
| Dynamic Surroundings | Touches the sound engine like Sound Physics; duplicates |
| FancyMenu, Drippy Loading Screen (+ Konkrete, Melody) | The house title and loading screens; removed on 2026-11-14, the game's own menu is back ([0026](decisions/0026-second-cleanup.md)) |
| Atmospheric Rain Backport, Cool Rain Reforged, Sable: Cool Rain, Distant Thunders, Soft Imprints (`snow-imprints`), Wavify, Dynamic Surroundings Rebirth | The rain, thunder, footprint, wave and drip mods of [0017](decisions/0017-averse-et-rayons.md); removed on 2026-11-14 with the `averse` and `rayons` modules of the `fallow` mod ([0026](decisions/0026-second-cleanup.md)) |
| Shine | Shine's bloom; removed on 2026-11-14 with Polytone ([0026](decisions/0026-second-cleanup.md)). Polytone came back the same day for the house resource pack ([0029](decisions/0029-look-without-shader.md)) and left again with it on 2026-11-15 ([0031](decisions/0031-brumaire-is-the-look.md)) |
| Chunky | Removed on 2026-11-14: the world is pregenerated with `/dh pregen`, or a server owner adds Chunky; the `horizon` fix for it stays ([0026](decisions/0026-second-cleanup.md)) |
| Supernova (Original Music Pack) | Resource pack with three tracks, removed on 2026-11-14; the music draws on 30 tracks of the game ([music](plan/musique.md), [0026](decisions/0026-second-cleanup.md)) |
| Biolith (separate copy) | Quark ships Biolith 3.0.14 in its jar and no other mod declares it; removed on 2026-11-14 ([0026](decisions/0026-second-cleanup.md)) |
| Puddles & Floods, Forgified Fabric API | Removed on 2026-11-14 (maintainer's choice). Puddles & Floods was the last mod that needed Forgified Fabric API, which goes with it: the pack no longer loads any Fabric layer |
| Dragged's Simulated Tree Chopper | "It's too much" (the team) |
| Not Enough Animations, First-person Model | Replaced by FA: Player Extension (team choice) |
| More Culling, Fog, Particle Rain | Fight with ATMOSPHERICS |
| Better Smoke | Fabric only (no NeoForge build on 2026-11-15, the pack loads no Fabric layer). The `vent` module of the `fallow` mod does the same, written for the pack: campfire smoke that drifts with the pack's wind and weather, grows into one larger plume over several fires, bluish over soul fires ([0032](decisions/0032-house-wind.md)) |
| Where Winds Blow (+ ToucanLib) | Removed on 2026-11-15: under Brumaire it turned its own sway off, and the pack's wind, swaying plants, slanted rain and smoke now come from the `vent` module and the shader ([0032](decisions/0032-house-wind.md)). Its grass variants in world generation leave with it |
| Better Clouds | Draws the clouds in place of ATMOSPHERICS, whose clouds follow the mist, dusk and rain; removed on 2026-10-29 ([0016](decisions/0016-vent-et-lumiere.md)) |
| Cirrus, Simple Clouds, Nuit, Stellar View, Caelum | Cirrus replaces ATMOSPHERICS' fog and sky; Simple Clouds needs OpenGL 4.3 (not on macOS); ATMOSPHERICS cancels the vanilla sky, so skybox mods have no effect ([0016](decisions/0016-vent-et-lumiere.md)) |
| Vibrancy, Gleam, Colorful Lighting, Physics Mod | Lighting or physics engines that are risky with Sodium 0.8 and Sable |
| Capture Nets, Paraglider, GUI Compass, Simple Voice Chat, Hardcore Revival, Create Aeronautics Structures, Cinch's Abandoned Houses, Cattlestrophic!, Easy NPC | Removed by the team on 2026-10-18 |
| Dark Coffee GUI, Dark Coffee GUI - Extras, Modded Coffee GUI | All rights reserved and the author does not answer; replaced on 2026-10-26 by `fallow-interface`, itself removed on 2026-11-13 ([0024](decisions/0024-removing-the-house-resource-packs.md)) |
| Untitled Duck Mod | Removed by the team on 2026-10-19; Naturalist's duck replaces it ([wildlife page, archived](archive/plan/faune-nml.md)) |
| Traveler's Backpack, Sophisticated Backpacks | Too many upgrades and screens; Reliable Backpacks chosen on 2026-10-26 ([0012](decisions/0012-sacs-piles-jours.md)) |
| ParCool! | Disabled at the team's request |
| Sanity: Renewed, TheWatcher | The world is enigmatic, not dark |
| Jade, AppleSkin | Break the mystery |
| Just Zoom, Zoomify | The vanilla spyglass is enough |
| Wildex Bestiary | Removed by the team on 2026-10-29; the `zoo` module keeps its own ledger ([modules-fallow](plan/modules-fallow.md)) |
| Waystones, Explorer's Compass, Nature's Compass | Erase the journey |
| Carry On | Incompatible with Aeronautics |
| Downhill Rivers, Terralith | Streams Reflowing and Tectonic chosen instead ([0011](decisions/0011-tectonic-streams-distant-horizons.md)); Terralith measured again on 2026-11-12, alone or with Tectonic ([0019](decisions/0019-leaving-no-mans-land.md)) |
| No Man's Land | Left the pack on 2026-11-13 at the team's decision of 2026-11-11: Regions Unexplored and Geophilic replace its biomes, the Friendly Moon thread goes with it ([0019](decisions/0019-leaving-no-mans-land.md)) |
| Biomes O' Plenty, Oh The Biomes We've Gone, Nature's Spirit, Lithosphere | Measured against Regions Unexplored on 2026-11-12 and not kept; Lithosphere was compared in game on 2026-11-13 and not kept ([0019](decisions/0019-leaving-no-mans-land.md)) |
| Drip Sounds | Beta build for 1.21.8, duplicates AmbientSounds and Sounds in caves; removed on 2026-10-25 |
| Seamless Loading Screen | Last build 2024-10, Sodium overwrites the screen it draws; removed on 2026-10-25 |
| Mezz config | Orphan library (only JEI ≥ 19.55 needs it, the pack is on 19.51); removed on 2026-10-25 |
| Copycats+, Connected, Factory Logistics, Bells & Whistles, Diesel Generators, Power Loader, Prospecting Picks, Rechiseled | Too many Create addons |
| Create Villager Labor, Open Parties and Claims | Useless among friends |
| Salt's Animal Farm, Animal Genders, Kaleidoscope Tavern, Thirst Canteen, Tide, Nutritional Balance | One more gauge or activity for a role already covered |
| Reactive Music | Silence between tracks capped at 2 min, 295 MB jar, cuts vanilla music ([research](archive/plan/recherche/2026-10-19/musique.md)) |
| Easy Magic | Replaces the vanilla table that the enchanting overhaul depends on ([research](archive/plan/recherche/2026-10-19/enchantement.md)); makes enchanting easier, against [0045](decisions/0045-awakening.md) |
| Immersive Enchanting | Removed on 2026-11-21: a found book unlocked its enchantment for everyone without being used up ([0045](decisions/0045-awakening.md)) |
| Better Archeology, Traveler's Titles, Create: New Beginnings, Let The Adventure Begin, MCG's RPG Campsites | Content without a strong mechanic, or breaks the mystery |
| Kaleidoscope Cookery, Kaleidoscope Compat, KaleidoscopeCookery-Automation | Replaced on 2026-11-15 by Farmer's Delight, Hands-On Delight and Create: Central Kitchen ([0030](decisions/0030-farmers-delight-kitchen.md)) |
| Legendary Survival Overhaul | Cold Sweat and Thirst Was Reclaimed kept |
| Minimaps, waypoints | Contrary to the vision |
| Animal Husbandry, Spoiled | Judged unreliable |
| Let's Do Bakery, Meadow | Duplicates or biomes in conflict with the terrain base (then No Man's Land); not re-evaluated since (WilderNature joined on 2026-11-17, [0033](decisions/0033-zoo-animal-mods.md)) |
| Graze | Makes animal husbandry self-sufficient |
| Integrated Mowzie's Mobs | Too many dependencies |
| Spawn, Environmental | Add biomes (conflict with the terrain base, then No Man's Land); not re-evaluated since |
| Prehistorum, Jurassic Saga, Jurassic Revived, Jurassic Reborn | Prehistory set aside for now |
| YUNG's Better *, Structory, Explorify, Moog's Voyager, Trek, Adorabuild, When Dungeons Arise, Dungeons and Taverns | Too many structures or RPG orientation ([0004](decisions/0004-structures.md)) |
| C2ME, Noisium, Ksyxis, FogRule | Alpha, not up to date or marginal gain ([0007](decisions/0007-ambiance-performance.md)) |
| Critters n' Crawlers | Duplicates Naturalist, horror creature |
