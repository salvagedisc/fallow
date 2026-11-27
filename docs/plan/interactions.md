# The interaction language

How a player works with the pack's stations, from the workbench to the anvil, so that mechanics that are alike in vanilla are not each used in their own way. Every station, house module or new mod picks one of the three registers below and follows its grammar. Decision: [0042](../decisions/0042-interaction-language.md).

Status: written on 2026-11-21, after Cinematic Interactions left the pack ([0040](../decisions/0040-removing-cinematic-interactions.md)) because first-person scenes on every vanilla station made everyday play slow. Facts below were read in the jars of the pack on that date. Not played.

## The three registers

| Register | What it is for | How it feels |
|---|---|---|
| **Menu** (« le menu ») | Choosing among many things: a recipe grid or a pattern, a trade, a line of dialogue, a book; storing and sorting | A window that opens at once and closes at once; nothing in it asks for timing |
| **Gesture** (« le geste ») | Transforming a few items oneself at a station where the work shows on the block: cut, scrape, stir, fry, pour, stretch, hammer | Right clicks in the world, with the item or the tool in hand; the block shows what lies on it and how far it is |
| **Craft skill** (« le savoir-faire ») | The few moments where the quality of one result is worth a test: forging a part, landing a fish, stirring a pot | A short timing test on the click; it earns a bonus, it is never the only way to get the result |

A mechanic that is a menu in vanilla stays a menu unless the gesture changes a decision of the player. That is why the vanilla stations keep their windows (0040), while cooking, the tannery and the forge's strikes are done by hand.

## Grammar of the gesture

The same four moves at every gesture station, house or not:

| Move | Meaning | Examples |
|---|---|---|
| Use with the item | Put it on the station | a hide on the fleshing beam, food on the cutting board, wet leather on the drying frame, water and bark in the tanning vat |
| Use with the tool | Work it, one stroke per click | the knife on the beam or the board, the spoon in the pot, the hammer on the anvil |
| Use with an empty hand | Read how far it is, or take back what lies there | the vat (« Trempage des peaux à 60 % (2 dans la cuve). »), the drying frame, the beam |
| Sneak + use | The other way of the same block | Farmer's Delight's own window on the pot, turning stone or an anvil into a forging anvil, starting the forge's strikes |

Waiting happens on the block, never with the player held: the vat soaks, the frame dries and the pot cooks while the player does something else. A station takes a batch where it makes sense (four hides in the vat, a whole pot).

## Grammar of the craft skill

- **One input**: the click. Overgeared's strikes are the use key on the anvil; Starcatcher's minigame takes any mouse button as well as Space; Hands-On Delight's spoon is the mouse.
- **Three words** for a beat: « Parfait ! », « Bien ! », « Raté ! » (Overgeared's own, kept in its translation; Starcatcher counts « prises parfaites » and « ratés » with the same words).
- **A bonus, not a gate**: a good test gives quality (Overgeared: Médiocre to Magistrale), a treasure or a golden fish (Starcatcher), extra servings (Hands-On Delight's stars). Each has a quick path for when the player does not want the test:
  - the pot cooks on its own over time; stirring cooks it all at once and earns the stars (`stirring_required=false`, changed on 2026-11-21);
  - Starcatcher's classic bobber (« flotteur classique ») skips the minigame, at the cost of the other bobbers' modifiers;
  - Overgeared has no per-recipe skip: its switch turns the minigame off for every recipe, so it stays on and the forge is the one test without a quick path.

## Shared conventions

- **Channels.** Hints and states go to the action bar, in one short sentence, at most every few seconds (tools, approach, rest, tannery, Overgeared's anvil); since `fallow` 0.38.0 the `indices` module draws every hint as a note in the left margin, with an icon for Fallow's own, while readings of what the player holds or rides (gauges, altimeter, jukebox, mount) stay above the hotbar ([mod-fallow](mod-fallow.md#indices-module)). The chat carries milestones only (a species mastered). A sound marks each step. No new gauge on the HUD.
- **Voice.** Fallow's own texts address the player as « vous », like the quests and the other mods' French: « Il vous faut une hache. » (the tool hints used « tu » until 2026-11-21).
- **One book per domain.** Recipes in EMI (R and U on any item; Hands-On Delight's book beside its stations shows the same recipes with their times); fauna and the zoo in Archie's guidebook (J); fish in Starcatcher's guide; quests in Questlog; places on the atlas; enchantments in each item's own tooltip (its awakenings, [eveil](eveil.md)). A new mod does not bring a second book for a domain that has one.
- **At a station.** As in Hands-On Vanilla and Hands-On Delight: a faint ghost shows what to lay next; each step has its sound and its particles (a splash, scraps, sparks, a puff of snow); the tool in hand is drawn on the work where it strikes; the work shows how far it is on the block itself (the water browns, the hide pales, the leather lightens), with the exact share on hover.
- **Stations.** A crafting station, whichever mod brings it, is worked on its block as at Hands-On Vanilla's: the camera over it, its slots laid on it, its choices made by clicks on the block ([0058](../decisions/0058-mod-stations-on-the-block.md)); a new one follows the same manner.
- **Windows.** What stays a window (storage, a chest under another name, a station whose switch is off) is a sheet of the books' paper (wells pressed in, pictograms in brown ink, vanilla's buttons as paper cards) laid on the inventory's wooden case, which the base resource pack draws as its chests (`fiches` module, [0057](../decisions/0057-station-windows-as-sheets.md)); never a flat dark or grey panel.
- **Pace.** Opening an everyday window (chest, barrel, the grindstone) costs well under half a second: Spatial GUI's camera move and scale animation are 150 ms (300 ms by default) and closing has no animation (shipped as defaults for new installs in `pack/config/defaultoptions/extra/config/spatial-gui.json5`). The camera leaves the player only at Hands-On Delight's stations, where the hands are the point, and one key brings it back.

## The pack's loops, register by register

| Vanilla mechanic | In Fallow | Register |
|---|---|---|
| Crafting | Crafting table worked by hand with its recipe book beside it (Hands-On Vanilla, [0043](../decisions/0043-hands-on-vanilla.md)); Overgeared's fletching table (the arrow laid on its top) and drafting table (a click on a tool head draws its blueprint) worked on the block; Overgeared's knapping (a rock in each hand) on the rock held, its flakes knocked off by clicks (`postes` module, [0058](../decisions/0058-mod-stations-on-the-block.md)) | Gesture |
| Smelting | Furnace, smoker, blast furnace worked by hand (Hands-On Vanilla); Overgeared's alloy, nether alloy and casting furnaces worked on the block (`postes` module): metals on the top, fuel in the firebox, the cooking bar on the front | Gesture |
| Cooking a dish | Farmer's Delight's pot, board, skillet and stove through Hands-On Delight; campfire | Gesture, stirring as craft skill |
| Smithing | Overgeared's anvil worked as a station (`postes` module): pieces, hammer and blueprint laid on it, a forge book beside it; then sneak + use with the hammer and strikes in the world | Gesture, then craft skill |
| Fishing | Starcatcher | Craft skill (classic bobber: none) |
| Leather | House tannery: beam, vat, frame, each a station (`postes` module) | Gesture |
| Grinding, polishing | Grindstone (Spelunkery's gems, Overgeared's grinding), Create's millstone and crushers | Menu / machines |
| Enchanting | The awakened item brought to the enchanting table, two powers to pick from in a one-row window; a great beast's shard in the other hand | Menu |
| Brewing, fermenting | Farmer's Delight's keg through Hands-On Delight | Gesture |
| Storage | Chests, the bag, sacks | Menu |
| Trading, talking | Archie's trades, the emissaries of the Peoples | Menu |
| Writing | Book and quill or written book in hand, on two pages (Scholar); a book on a lectern is read on the lectern (Hands-On Vanilla) | Menu in hand; gesture at the lectern |
| Photography | Exposure's camera in the world, its lightroom worked on the block (`postes` module): film, paper and dyes on the table, the negative leaning between them, a click prints | Gesture |
| Heating, cooling | Cold Sweat's hearth, boiler and icebox worked as stations (`postes` module): fuel laid before the grate, waterskins on the boiler's top or in the open icebox, gauges on the front; fuel in hand still thrown in by a click | Gesture |
| Sewing | Cold Sweat's sewing table worked on the block (`postes` module): the piece on the cushion, the lining beside it, the result standing | Gesture |
| Rest, sleep, drink | Rest by the hearth, beds and Comforts, Thirst Was Reclaimed | In the world, no station |

## Divergences kept on purpose

- **Knapping is a grid.** Choosing a tool head is choosing a pattern, the menu's job; it is used a few times at the start.
- **Overgeared asks to sneak before striking** (`requireCrouchForForgingGrinding`, on by default). Plain use opens the anvil's window to lay the ingredients; sneak + use with the hammer starts the strikes, which matches the "other way of the same block" move.
- **Starcatcher's minigame takes the screen.** It is short and can be skipped with the classic bobber.

## For new work

Before adding a station, a mod or a module: name its register; if it is a gesture, check the four moves; if it is a craft skill, check the click, the three words and the quick path; check that its texts use « vous », the action bar and no new book. The emissaries' dialogue is a menu: it follows the menu's pace (text shown at once, one key to go on, Escape to leave, no camera lock).

## In-game tests

| # | Test | Expected |
|---|---|---|
| IN1 | Put a soup's ingredients in the pot over a lit stove, leave the station | The soup cooks on its own in Farmer's Delight's time, without stars |
| IN2 | Same, stirring at the station | The whole pot cooks at once, with stars and extra servings |
| IN3 | Open and close a chest, a barrel and the grindstone in a new instance | The window comes in about 150 ms and leaves at once; Spatial GUI's settings show 150 ms |
| IN4 | Bare hands on a log | « Il vous faut une hache. » |
| IN5 | Fish with the classic bobber on the rod | The fish bites and comes in without the minigame |
