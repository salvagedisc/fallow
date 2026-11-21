# First gestures

With bare hands, you gather but you do not cut. Wood, raw or worked (trunks, planks, furniture, chests), needs an axe, rock needs a pickaxe, and the first tool is knapped from stone. Dirt, sand, gravel, leaves, plants and crops stay breakable by hand, otherwise nobody could start. Decision: [0013](../decisions/0013-premiers-gestes.md).

Status: written on 2026-10-29. Checked on a test server on 2026-10-29 (NeoForge with KubeJS, LootJS, Overgeared, Twigs and No Man's Land, which has since left the pack, [0019](../decisions/0019-leaving-no-mans-land.md)): the scripts load without error, 15 planks recipes are rewritten (11 vanilla, 4 NML), an oak log and a stone axe give 2 planks and give back the axe with 1 damage, the campfire takes flint, the wooden tools have no recipe. Not yet tested with a player.

## The chain, for a player

1. **Bare hands.** Pick up gravel (flint), since 2026-11-13 the only source of flint on the ground (the No Man's Land pebble, one chance in two of flint, left with the mod), leaves (a stick 8% of the time) and, since 2026-11-21, the [ground debris](#ground-debris): rocks of the ground's stone in the open, fallen wood of the tree's own wood and pine cones under the trees, all picked up empty-handed with a right-click. A trunk or a rock does not give way: the action bar says « Il vous faut une hache » / « Il vous faut une pioche » ("you need an axe / a pickaxe"; « tu » until 2026-11-21, [interactions](interactions.md)).
2. **Knapped stone.** Flint used on bare `minecraft:stone` gives a knappable rock (Overgeared, 30% per strike, the flint breaks 15% of the time). A Twigs pebble or a ground rock (not a gravel chip or an ice chip) also turns into a knappable rock in the inventory grid. Right-click with the rock opens Overgeared's knapping grid: axe, pickaxe, shovel, hoe heads, sword blade, hammer head. Head + stick in the 2 × 2 grid = the stone tool (Overgeared's own recipes).
3. **Wood.** The stone axe fells the tree block by block (TreeChop); each chop costs durability. A log and an axe in the grid give **2 planks**; the axe stays in the grid and loses one point. Create's mechanical saw later gives 6 planks per log.
4. **Fire.** A campfire is three sticks, three logs and **a flint** instead of coal: fire on the first day, before the furnace. It cooks and purifies water (Thirst Was Reclaimed).
5. **Furnace, copper, iron, steel.** All Overgeared, unchanged: furnace from cobblestone; copper hammer (2 copper ingots, 2 sticks); crouch + right-click on stone with a hammer = stone smithing anvil; clay casts fired in the furnace; blast furnace, heated ingots forged on the stone anvil; smithing anvil and alloy furnace for steel.

## The rule

The `outils` module of the `fallow` mod ([mod-fallow](mod-fallow.md#outils-module)). A block needs a tool when it is in the `fallow:outil_requis` block tag (`#minecraft:logs` and `#minecraft:mineable/axe`: every wooden block, crafted or not, from every mod that tags its blocks for the axe) or when it would drop nothing without the correct tool (vanilla `requiresCorrectToolForDrops`: stone, ores, bricks, metal blocks, lanterns). Without the correct tool in the main hand, the mining speed is 0 on both sides (no ghost block), harvesting is refused, and a hint shows at most every two seconds. A tool of the right kind but too weak gets « Votre outil n'est pas assez solide » ("your tool is not strong enough"). The `fallow:sans_outil` tag keeps what grows breakable by hand even when it is tagged for the axe: `#minecraft:sword_efficient` (plants, crops, saplings, pumpkins, melons, cocoa, vines, mushrooms), bamboo, azaleas, cut sugar cane and, since 2026-11-13, Regions Unexplored's cattail (No Man's Land's plants left the tag with the mod), Supplementaries' placed stick, Farm & Charm's rope; and snow. Creative mode and fake players (Create deployers) are not affected.

The team chose on 2026-10-29 that crafted wooden blocks need an axe too (planks, furniture, chests, crafting table, Create's wooden parts). To go back to raw wood only, replace `#minecraft:mineable/axe` by nothing in `fallow:outil_requis` with a datapack (`"replace": true`, `#minecraft:logs` alone); no recompilation.

## The recipes

`pack/kubejs/server_scripts/premiers_gestes.js`:

| What | Before | After |
|---|---|---|
| Wooden tools (5) | Craftable | Recipes removed, removed from chest loot, hidden from EMI (`client_scripts/premiers_gestes.js`) |
| Log → planks | 4 by hand | `PREMIERS_GESTES_PLANCHES_A_LA_HACHE` (2) with any axe in the grid, damaged by 1; every single-log shapeless planks recipe is rewritten (vanilla and the other mods' woods, Regions Unexplored's included), id `fallow:premiers_gestes/planches/<original id>`; the log reports how many |
| Campfire | Sticks + logs + coal | Sticks + logs + flint |
| Twigs pebble | 4 → cobblestone | Kept, plus 1 → `overgeared:knappable_rock` (`fallow:premiers_gestes/galet_a_tailler`); pebbles no longer generate since 2026-11-21 |
| Ground rock (`#fallow:cailloux_a_tailler`) | (new) | 1 → `overgeared:knappable_rock` (`fallow:premiers_gestes/caillou_a_tailler`) |
| Fallen wood (`#fallow:bois_mort`) | (new) | 1 → 2 sticks (recipe of the `fallow` mod); burns 150 ticks |
| Gravel chip | (new) | 4 → 1 gravel (recipe of the `fallow` mod) |
| Pine cone | (new) | Burns 300 ticks (one and a half items), composts at 30% |
| Leaves broken | Stick 2% | Plus an extra stick at `PREMIERS_GESTES_BATONS_FEUILLES` (6%) |

Unchanged on purpose: Overgeared's stone, copper, iron and steel tools; the vigil fire and clearing lantern recipes (reviewed with the mist settings).

## Quests

New chapter « Premiers gestes » (order 1, after Arriver; the other chapters move down by one), one quest per gesture ([quests](quetes.md)): Mains nues (guide), Le silex, La pierre à tailler, La première hache, Des planches (crafting table), Le feu (place a campfire), La pioche, Le four, Le marteau et l'enclume de pierre (copper hammer), La forge (guide, appears with the first iron ingot).

## Rejected

- [Survivalist Essentials](https://modrinth.com/mod/survivalist-essentials) (MIT): configurable tool rule, saws, knife and plant fibre, loose rocks, bandages and bleeding. A second stone and rock system next to Overgeared, bandages duplicating those of another mod, one more book.
- [No Tree Punching Remastered](https://modrinth.com/mod/no-tree-punching-remastered) (GPL-3.0): flint and copper tools, rocks, ceramic vessel. Copper tools duplicate Overgeared's, the vessel duplicates the bag.
- [Primity](https://modrinth.com/mod/primity) (own licence): flint tools, cast iron, harder blocks. Cast iron duplicates Overgeared's steel chain.

## Ground debris

Since 2026-11-21 ([0048](../decisions/0048-ground-debris.md)), the `debris` module of the `fallow` mod ([mod-fallow](mod-fallow.md#debris-module)) lays what a player picks up on the first day, and the ground says where it lies:

- **Under a tree**, fallen wood of that tree's wood, in three sizes (a twig, two, a branch), and under spruces and the conifers of Regions Unexplored (pine, larch, redwood, cypress), pine cones. The tree is read from the lowest leaves above the spot: tags `fallow:debris/sous_<wood>` (`pack/kubejs/data/fallow/tags/block/debris/`) map each leaf of the pack, Regions Unexplored's included, to the nearest of the eight vanilla woods.
- **In the open**, rocks of the ground's stone in four sizes: granite, diorite, andesite, sandstone on sand, red sandstone on red sand, gravel chips on gravel, ice chips on ice, plain rocks on stone and, rarely, on grass and dirt.
- **Picked up empty-handed** (right-click, the gesture « reprendre » of [interactions](interactions.md)); a click with something in hand keeps its use. They break at once, let players walk through, and need solid ground below.

Twigs' pebbles and twigs, the same everywhere, no longer generate (`pack/kubejs/data/twigs/neoforge/biome_modifier/add_pebbles.json` and `add_twigs.json` set to `neoforge:none`); its seashells stay on beaches. How many and where: `pack/kubejs/data/fallow/worldgen/configured_feature/debris_au_sol.json` (rules in order, the first whose ground and cover match wins; a rule that matches but whose chance fails leaves the spot empty) and its placed feature (4 starts per chunk, 3 to 5 tries each within 6 blocks), added to `#minecraft:is_overworld` in `top_layer_modification`, the last step, so that the trees of the chunk are already there (ground covered by snow gets none).

## In-game tests

| Code | Test | Expected |
|---|---|---|
| G1 | New world, bare hands on a log, then on stone | Nothing breaks; « Il vous faut une hache », then « Il vous faut une pioche », in French, not spammed |
| G2 | Stone pickaxe on a diamond ore | « Votre outil n'est pas assez solide », the ore does not break |
| G3 | Dirt, sand, gravel, leaves, snow, flowers, wheat, a pumpkin, cocoa, bamboo, reeds by hand | Break as in vanilla |
| G3b | A crafting table, a chest, planks, a Handcrafted chair by hand, then with an axe | « Il vous faut une hache », then they break |
| G4 | Walk around spawn | A flint within about 5 minutes (gravel; the number to note, since the NML pebble is gone) |
| G5 | Flint on stone, knap an axe head, head + stick | A stone axe; the tree falls with TreeChop; note the durability used per tree |
| G6 | Oak log + axe; Regions Unexplored maple log + axe | 2 planks each, the axe stays in the grid with one point less; the server log says how many planks recipes were rewritten |
| G7 | Craft and place a campfire with flint | Lit campfire; the « Le feu » quest completes |
| G8 | Twigs pebble in the grid | A knappable rock |
| G9 | EMI search "bois" | No wooden tools |
| G10 | Questlog | « Premiers gestes » after Arriver; each quest appears after the previous one and completes |
| G11 | Time to the first furnace | Aim for 20 to 30 minutes |
| G12 | Create deployer holding nothing in front of a log | Not blocked by the rule (fake player) |
| G13 | Walk a new world through a forest, a taiga, a plain and a stony shore | Fallen wood of the right wood under oaks, birches, spruces; pine cones under spruces and pines; rocks of the ground's stone in the open; no Twigs pebble or twig; not so many that the ground looks littered |
| G14 | Right-click a rock, fallen wood and a pine cone empty-handed, then with a block in hand | Empty-handed: picked up with a soft pop; with a block: the block is placed next to it |
| G15 | Fallen wood in the grid; a granite rock in the grid; 4 gravel chips; a pine cone in a furnace | 2 sticks; a knappable rock; 1 gravel; it burns |
| G16 | Break the block under a rock; walk through rocks and wood | The rock pops off; no collision, no stumble |
