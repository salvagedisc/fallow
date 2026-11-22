# The tannery

Idea 4 from Wayfarer ([research](../archive/plan/recherche/2026-10-19/wayfarer.md), recommendation 4). Leather no longer drops ready-made from animals: it takes a hide, a knife, bark, water and time. Since 2026-11-18 the chain is the `tannerie` module of the `fallow` mod (0.34.0), which replaced the Tannery: Better Leather mod (All Rights Reserved, fragile, satchels and studded leather the pack did not use): decision [0038](../decisions/0038-house-tannery.md), after [0014](../decisions/0014-tannery.md).

Status: checked on a dedicated server of the full pack with one player on 2026-11-18, not played.

## The chain, for a player

1. **Get a raw hide** (« Peau brute »). Every pack animal that used to give leather drops raw hides instead, in the same quantities with the same Looting bonus; pigs, sheep and goats drop 0 or 1 (plus 0 to 1 per Looting level).
2. **Scrape it** on a **fleshing beam** (« Chevalet à écharner »: two logs and two sticks). Right-click the beam with the hide to lay it on, then right-click with a knife (any item of `c:tools/knife`: Farmer's Delight's knives) six times. Each stroke wears the knife. The hide pops off as a **scraped hide** (« Peau raclée »), and one time in three leaves a piece of **fat** (Naturalist's). An empty hand takes an unscraped hide back.
3. **Tan it** in a **tanning vat** (« Cuve à tan »: four logs and three planks). Pour in a water bucket, then one **bark** per hide, then up to four scraped hides. They soak for 5 minutes, all together; a hide added during the soak starts it again. An empty hand on the vat says how far it is (« Trempage des peaux à 60 % (2 dans la cuve). ») and, when it is done, takes out the **wet leather** (« Cuir mouillé »); the bark of those hides is used up, the water stays. An empty bucket takes the water back when no hide is in; the bark stays at the bottom for the next water. A vat or a drying frame moved by a piston or a Create contraption carries on where it was.
4. **Dry it** on a **drying frame** (« Séchoir à cuir »: seven sticks and a string), hung on a wall like a ladder (it needs a solid wall behind it and falls if the wall goes), best on a wall in the sun. Right-click with the wet leather to nail it in; an empty hand reads how it is going and why (« Séchage du cuir à 40 %. Le feu le sèche vite. »), and takes the **leather** once dry.

Bark (Farmer's Delight's tree bark) drops half of the time when a log or wood is stripped with an axe, and still comes from Farmer's Delight's cutting board. Fat makes two torches with a stick, or a candle with a string.

## At the station

Since `fallow` 0.42.0 (`postes` module, Diegesis), a right-click on one of the three blocks brings the camera to it, as Hands-On Vanilla does for the crafting table and Hands-On Delight for the cutting board; crouching keeps the old right-click. The hotbar stays open under the block, and the tannery book lies beside it.

- **Laying**: an item taken on the cursor and dropped on the block works as the right-click did (the hide lays on the beam, the knife strokes, water, bark and hides go into the vat, the wet leather stretches on the frame); a bucket or what comes out goes back to the inventory. Shift + click on a slot sends that stack. Coming to the block with a useful item in hand makes the first gesture by itself.
- **Empty hand** on the block: takes what is finished, or reads how far it is, in the caption of the scene.
- **The book** opens at the page of the block (scraping, tanning, drying), with what it takes and the time. Picking that page fills the block from the inventory; Shift on the vat puts in as many barks and hides as it holds. A page of another block says where it is done.

- **What it shows** (0.46.0): the vat's water fills, browns with the bark and the soak and bubbles, bark floats on it, the hides drop in with a splash and turn to leather; on the beam the knife lies on the hide and each click draws a stroke that throws scraps while the hide pales; the leather on the frame drips, lightens and tightens as it dries, steams in the sun or by a fire, and its drying shows on hover. Each block shows faintly what to lay next.

`fallow-common.toml`, `[postes] tannerie = false` brings back the right-click alone.

## Drying

Every 20 seconds the frame gains drying points; at 15 the leather is dry.

| Where | Points | Time |
|---|---|---|
| Open sky, day | 2 | 2 min 30 |
| Under a roof, or at night | 1 | 5 min |
| Lit fire within 4 blocks (block tag `fallow:foyers`, the same as rest by the hearth) | +2 | 1 min 40 sheltered by a fire |
| Where it snows (cold biome, or Ecliptic Seasons' winter, which changes the biome's precipitation) | −1 | a shelter without fire no longer dries in winter; the rack then says « Trop froid : sans feu, il ne sèche pas. » |
| Rain falling on it | −1 (it gets wet again) | |

Breaking a block gives back what is on it (raw hide, scraped hides, wet leather or leather).

## The animals concerned

| Loot table | Change |
|---|---|
| `minecraft:entities/cow`, `mooshroom`, `horse` (and the 15 Icy's Better Horses breeds), `donkey`, `mule`, `llama`, `trader_llama`, `hoglin` (Nether, no effect) | leather → raw hide |
| `naturalist:entities/deer`, `zebra` | leather → raw hide |
| `naturalist:entities/boar`, `elephant`, `rhino`, `komodo_dragon` | Naturalist hide → raw hide |
| `hybrid_aquatic:entities/dugong`, `manatee`, `friendsandfoes:entities/moobloom`, `wildernature:entities/bison`, `deer`, `spawn:entities/sea_cow`, `flukeshroom`, `quark:entities/foxhound` (Nether, no effect) | leather → raw hide |
| `minecraft:entities/pig`, `sheep`, `goat` | raw hide added |

`pack/kubejs/server_scripts/tannerie_butin.js` changes the tables themselves (LootJS), so every mod that points to them follows. The script writes to the server log any creature table that still drops leather (`[tannerie] créature qui lâche encore du cuir`).

Alex's Mobs kangaroo: two kangaroo hides make a raw hide (`fallow:tannerie/peau_de_kangourou`). Koopa's Critters: four reptile hides make a raw hide (`fallow:tannerie/peau_de_reptile`). A Naturalist hide left over makes a raw hide (`fallow:tannerie/peau_de_naturalist`).

## The recipes

In the mod (`data/fallow/recipe`): `chevalet` (two logs diagonally over two sticks), `cuve_a_tan` (logs in a U over three planks), `sechoir` (a frame of sticks around a string).

In the pack (`pack/kubejs/server_scripts/tannerie.js`): kangaroo and Naturalist hides, `fallow:tannerie/torches_au_gras` (fat + stick → 2 torches), `fallow:tannerie/chandelle_de_suif` (fat + string → candle).

Removed, because they give leather outside the tannery: `envelope:mailing/automated_supply_service/leather` (6 rotten flesh by pigeon), `naturalist:leather_from_smelting_hide`, `alexsmobs:kangaroo_hide_to_leather`, `starcatcher:leather_from_boot`, `spelunkery:leather` (salted rotten flesh → 4 leather), `koopascritters:reptilehiderecipe` (replaced, see above). The server log lists the recipes that still give leather (`[tannerie] recette donnant du cuir`).

Kept, because they recycle leather that already exists or come from rare loot: Create's saddle at the mill and horse armours at the crusher, Farm & Charm's leather horse armour at the chopper, Farmer's Delight's leather armour, horse armour and saddle cut back on the cutting board, Quark's bound leather block, Supplementaries' quiver cut back on the board, Cold Sweat's hoglin hide (Nether closed), leather in a few chests and from fishing.

## Configuration

`fallow-common.toml`, `[tannerie]`: `enabled` (true; false stops the work and the bark, the blocks stay), `strokes` (6), `tanningMinutes` (5.0), `dryingPoints` (15), `barkChance` (0.5), `fatChance` (0.33).

## Files

- `mods-custom/fallow/src/main/java/io/github/salvagedisc/fallow/tannerie/`: the module, the three blocks.
- `mods-custom/fallow/src/main/java/io/github/salvagedisc/fallow/postes/` and `assets/fallow/diegesis/scenes/`: the three stations ([mod-fallow](mod-fallow.md#postes-module)).
- `mods-custom/fallow/src/main/resources/`: block states, models, textures, loot of the blocks, recipes, tags `fallow:tanins` (items that tan: Farmer's Delight's bark), `fallow:sans_ecorce` (logs that never give bark: stripped ones), `minecraft:mineable/axe`, texts.
- `pack/kubejs/server_scripts/tannerie.js`, `tannerie_butin.js`: recipes and loot.
- Quest « La tannerie » (`pack/config/questlog/quests/metiers_tannerie.json`): appears with the first raw hide; scraped hide, wet leather, leather.

## In-game tests

- **TA1**: kill a cow, a horse and a pig: raw hides; no leather drops from an animal.
- **TA2**: craft the fleshing beam, lay a raw hide on it (it shows), scrape it six times with a flint knife: a scraped hide pops out, sometimes a fat; the knife lost six uses; an empty hand before the end takes the raw hide back.
- **TA3**: strip a few logs with an axe: bark about one time in two; a stripped log gives nothing.
- **TA4**: tanning vat: a scraped hide without water, then without bark, is refused with a message; water, two barks, two hides: the liquid turns brown, the hides float; after 5 minutes an empty hand takes out two wet leathers.
- **TA5**: the drying frame goes only on a wall, and drops when the wall is broken; in the sun: dry in about 2 min 30; under a roof by a campfire, faster; in the rain it gets wet (the message says so); an empty hand gives the leather.
- **TA6**: fat + stick → 2 torches; fat + string → candle; EMI shows the three blocks and their recipes, in French.
- **TA7**: the quest « La tannerie » appears with the first raw hide and completes with the leather.
- **TA8**: the three stations: a right-click brings the camera to the block, the book beside it at the block's page; a hide dropped from the cursor lays on the beam and a knife strokes it; Shift on the vat's page fills water, bark and hides; the frame reads its drying in the caption; coming with the item in hand makes the first gesture; crouched, the right-click works as before. Since 0.46.0: the water and hides of the vat change colour as they soak, the knife is drawn on the beam's hide with scraps at each stroke, the frame's leather lightens as it dries and its drying shows on hover, and a faint ghost shows what goes next. Since 0.47.0: the vat is a hooped tub, the beam a barked log on trestles, the frame has barked posts on feet and lashed rails; the empty beam shows the hide to lay as a pale pulsing shape; the finished piece lifts off the block and flies to its hotbar slot with « +1 ». Since 0.48.0: the ready leather floats over the vat's water; a click on it takes it. Since 0.49.0: what Shift + click or the book sends flies from the hotbar into the block; an item the block refuses makes it shake. Since 0.50.0: a bar over the vat and the frame fills as the hides soak and the leather dries. Since 0.51.0: an item the block will not take makes it shake at once, the block is outlined while the cursor holds something it takes, the vat's water sinks when bucketed out, and fast knife clicks each give their stroke. Since 0.52.0: a second player watching sees the knife strokes and refusals, the vat's liquid moves in the world, and wet leather steams by a fire or in the sun. Since 0.53.0: dry leather glints and is outlined under the empty hand.
