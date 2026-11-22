# Awakening: enchanting as a deed

`fallow` 0.44.0, polished in 0.58.0, the table as a station in 0.60.0, `eveil` module, decision [0045](../decisions/0045-awakening.md). Enchanting is rare and earned: an item learns from what it has lived through, the strongest powers come from great beasts, and a worn-out item forgets. Nothing comes out of loot or trade already enchanted, and there are no enchanted books. Immersive Enchanting left the pack with this change.

## What the player lives

1. A freshly forged item bears nothing. From its first deed (a block mined, a creature beaten, a blow taken), it counts. Its tooltip says in words how far it is on the way to its next awakening, by quarters: « Encore étranger à votre main », « Commence à se faire à votre main », « Se fait à votre main », « Sur le point de s'éveiller ».
2. Past a threshold it awakens: a sound, a note « … a appris quelque chose : un éveil vous attend à la table d'enchantement », and the item shimmers.
3. Right-click an enchanting table: the camera leans over it and the table's floating book comes to the player and opens (see « The table » below). The item lies on the table; the left page tells its life, the right page the two powers its deeds lean toward, each with what it does and the deed it was born of. A click on one awakens it: its letters stream into the item as runes, the item rises, turns once and comes down awakened, and its tooltip notes « Efficacité I : jour 143, <player> ».
4. Each further awakening asks three times more than the previous one (2,000, 6,000, 18,000 points in all with the defaults).
5. Great beasts leave a shard (« Éclat de grande bête (Frostmaw) ») that carries one strong enchantment: laid on the table beside the item, the right page tells the beast's story and the power; a click pours it into the item. It is used up and takes one place.
6. When an awakened item has used 90 % of its durability, it warns once: a note « Pioche en fer faiblit. À bout de forces, cet objet oubliera Fortune I. », a low chime, and a red tooltip line until it is repaired.
7. When the item reaches the end of its durability (Overgeared's « broken » state, or one point before breaking), it forgets its last awakening: the enchantment goes back to its former level, a grey note says so, and its deeds drop back to the threshold of the awakening before, so it has to earn it again. It forgets only once until it is repaired.

The two choices are drawn from the item's deeds with a seed tied to the item and to the awakening, so closing and reopening the window shows the same two. An item whose deeds lean only toward powers it can no longer take (maximum level, conflicts) tells « Cet objet n'a plus rien à apprendre ».

## The table

With Diegesis in the pack the enchanting table is a station (`TableEveil`, station `fallow:table_d_eveil`), laid out like Hands-On Vanilla's enchanting table (MIT, its open book and book model ported in `eveil/client`).

- **The table sleeps.** It only wakes for a player who carries an item ready to awaken or a great beast's shard; otherwise a right-click gets a faint, low chime and « La table reste muette : rien de ce que vous portez n'est prêt à s'éveiller. » (`TableEveil.eveille`, checked on both sides by the station's open condition and by `onTable`).
- **The room darkens.** At the table the world darkens around the scene, towards violet (vanilla's vignette laid over it, deeper when the item is ready and deeper still during the rite), a low drone and a breath rise (the beacon's and the portal's ambient sounds, looped by the scene), and runes drift from each bookshelf the table sees towards the book.
- **A place apart from the world** (0.79.0, after references such as Control's Astral Plane, Hollow Knight's dreams and the boss fog of Dark Souls). Sitting down, a low swell sounds and the view widens for two seconds then settles, so the background seems to recede (the cinema's dolly zoom, done with the field of view only). The world goes by degrees (`eveil/client/Isolement`): veiled on arriving (a near-black violet mist from about 2 to 6.5 blocks from the camera, the sky gone), mostly gone when the item is ready, wholly gone during the rite (mist from 1 to 3.5 blocks: only the table, the book and the runes stay); a pale dust comes out of the mist from all sides and is drawn to the book. The world's sounds fall to about a third when veiled and to 8 % during the rite, the table's own (and the interface) keep theirs; they never reach zero, so nothing is cut. During the rite the view tightens by up to 10 % and lets go at the flash. Leaving, the mist withdraws over three seconds and the sounds come back with it. Mixins in `mixin/eveil`: `BrumeDeTableMixin` (fog distance and colour, after ATMOSPHERICS, before the capture that hides Distant Horizons' LODs), `CielDeTableMixin` (no sun, moon or stars, as under blindness), `SilenceDeTableMixin` (volume when a sound starts and while it plays), `VueDeTableMixin` (field of view, after Diegesis's, scaled by the game's FOV effects setting).
- **Coming to the table.** A right-click turns the camera over the table from where the player stands. The table's floating book leaves its place (it is the same book: vanilla's is not drawn while the scene is there) and glides to the reading place, opening as it comes. Leaving, it half shuts and goes back to float over the table.
- **Awakening ink.** No awakening without it: a vial of awakening ink (`fallow:encre_d_eveil`, drawn as vanilla's potion with its liquid tinted lapis violet, until the pack changes resource pack; glass bottle, lapis lazuli, glow ink sac and amethyst shard, shapeless) is used up by each awakening, from a path or a shard. It lies in the inkwell between the item and the shard (up to 16); what is carried comes to it by itself on arriving. With the inkwell empty the paths stay in runes, the foot of the page says « L'encrier est vide, les runes se taisent. » in red, and a click makes the book and the inkwell shiver; the book says it once, nothing else does (the crouching window, which has no book, says « Il manque l'encre d'éveil : sans elle, la table ne peut écrire aucune voie. »). As a path is read, ink rises from the inkwell in wisps to the line. Crouching, the one-row window takes a vial from the inventory. Creative mode uses none.
- **Laying things.** Three places on the table top: the item on the left as the camera sees it, the inkwell in the middle, a great beast's shard on the right. The item held when coming is laid by itself once the camera has arrived; a shift-click in the hotbar sends a stack to its place in an arc; a stack dropped from the cursor goes too. Whatever is not taken back returns to the inventory on leaving. The hotbar greys out what the table does not take.
- **The left page: its life.** The item's name and icon, its places as signs (◆ taken, ◇ free), each awakening with its day, its player and the beast of its shard, its three greatest deeds in words (« Des centaines de minerais extraits », from a count kept apart from the points so a creature counts once whatever its weight), and at the foot where it is on its way, in words (« Prêt à s'éveiller », « Il porte tout ce qu'il peut porter ») and as a line of ink that fills. No number is shown.
- **The right page: its paths.** Ready, « Deux voies s'ouvrent »: each path with its name, what it does (`eveil.fallow.pouvoir.*`), the level it carried before, and « Éveillé par des centaines de minerais extraits », the deed that weighed most toward it. All of it is first written in the table's runes (vanilla's alt font); the lines are cut on the French words and each is then written in runes past what is read, so nothing moves while it is read, and path names breathe with a faint light; there is no band under the cursor: the hovered path's name lights up and its words darken a little, and the path is translated letter by letter as it is read (about 1.4 s, with a faint chime as it goes), and stays translated until the page changes.
- **The rite.** A path is chosen by holding the left click on it (1.8 s; the button is read from the window, vanilla does not follow it while a screen is open). The item rises and turns faster and faster, a ring of runes spins around it and tightens, the book trembles, the room darkens further and a line of violet ink is drawn under the path's name, left to right. Let go early and everything falls back with a dying sound. Held to the end: a white flash over the scene, a ring of runes thrown out from the item, sparks that linger, the inkwell shivering, the beacon's deep sound and the amethyst's ring, and the path streams into the item. A shard is poured the same way, holding the click on its page. Not ready, full, or with nothing left to learn, the page says so in one sentence in its middle, and a click makes the book shiver.
- **A shard.** With a shard on the table the right page is the shard's: the beast, its story, the power and its level, and what the table says (hold to pour it, lay the item on the left, it does not take, the item already bears it, no place left).
- **Each new page comes by a page turned**; while the sheets riffle (an item just laid) the last one brings it, and the leaf that goes keeps what it said. Pages with nothing written carry the table's runes, faded, as the floating book does.
- **Others at the table** see the item rise and come down, and the runes stream, with the server's sound.
- **Never vanilla's window.** Hands-On Vanilla registers its own enchanting table station, switched off in the pack's config, and Diegesis takes the first station registered for a block: when Hands-On's came first, nothing opened and vanilla's enchanting window let anyone enchant with experience. The mixin `eveil.PosteDeTableMixin` makes Diegesis always answer the enchanting table with ours, on both sides; and whatever opens it, vanilla's enchanting window is closed as soon as it opens (`EveilModule.jamaisVanilla`). A cancelled right-click cannot be the guard: Diegesis fires the same event to ask whether it may open.
- Crouching, or with `[eveil] poste = false`, the table keeps the one-row window of before (item in the main hand, shard in the other hand).

## Places by forging quality and metal

An item can carry as many awakenings as its Overgeared quality allows (`[eveil.places]` in `fallow-server.toml`): none or poor or well forged 1, expert or perfect 2, masterwork 3. An item without quality (bow, fishing rod, shield, leather) has 1. Since 0.81.0 the metal adds to it, following the progression ladder ([progression](progression.md)): nothing for iron, copper and gold, one for Overgeared's steel and the pack's black steel (the blued steel), two for diamond, three for orichalcum (netherite), five at most; a cherished item has one more (see below). Archie's armours are beasts, not metal: nothing. The number is shown in the tooltip (« Éveils : 1 sur 2 ») and follows the item (`Lien.places`, each second in the inventory), never below what it already carries.

## Attachment

The owner's wish (2026-11-22): taking care of an item should count. Each item has an attachment to the player, said in one word in its tooltip and after its places in the table's book: *négligé*, nothing, *entretenu*, *chéri* (`Lien`, points kept as notes of `Actes`, the word as the component `fallow:lien`, which only changes when a threshold is passed: below 0, from 30, from 100).

| Gesture | Points |
|---|---|
| Repaired before half its wear | +12 |
| Repaired before the red (90 %) | +4 |
| Named at the anvil (once) | +25 |
| A night spent at home, in an item frame or on an armour stand (once a night) | +5 |
| Picked up again by its player after a death | +10 |
| Let down into the red (once until repaired) | −12 |
| Lying on the ground for more than a minute | −1 every 30 s |
| In fire or lava | −15 every 30 s |
| In other hands than its first owner's | −4 every 10 min |

A repair only counts when it gives back a tenth of the item's durability at least (since 0.84.0): mending a point at a time is not care. Deeds done by a machine holding the item (a Create deployer, a fake player) do not count either.

- **Entretenu**: at the end of its strength it no longer forgets its last awakening; it stops at its last point and waits to be repaired (« … est à bout de forces mais n'a rien oublié : réparez cet objet. »). Since 0.84.0 the stop is exact: the wear that would take an awakened Entretenu item past its last point is held back as it happens (`mixin/eveil/ObjetEveilleMixin` on `ItemStack.hurtAndBreak`), so even a tool worn several points in one second does not break (the wear is held back after `Item.damageItem`, where another mod could still raise it). While it stands there it serves no more: it cannot mine, strike, shoot, block, till or light (the block it is used on keeps its own use, a door still opens); armour keeps protecting at its last point without wearing further. Mended by a point at the edge it does not warn again: only a repair below the 90 % warning makes it able to forget or stop once more. An awakened item burnt, melted or blown up on the ground takes its deeds with it (`mixin/eveil/ObjetBruleMixin`). Its warning at 90 %, in chat and in the tooltip, says it will keep its awakening.
- **Chéri**: one more place.
- A named item is called by its name everywhere already, the book included.

## Repairing without losing

- At the anvil an awakened item keeps everything, and the repair counts as a deed.
- Two items put together in the crafting grid to repair each other make a new one: an item that has started its life (`fallow:eveil`) is refused there (mixin `eveil.ReparationEtabliMixin`).
- After a few anvil repairs vanilla says « Trop usé ! » (the repair cost). Alone on the grindstone, an awakened item now loses that cost and keeps its awakenings; with another item, or with no cost to remove, the grindstone still gives nothing.

## Deeds

The deeds are data, one file per family of items in `data/fallow/eveil/` of the mod (another namespace can add files). Each entry names the deed, its target (a block, creature or damage tag, or a mode), its points per unit and the enchantments it leans toward, with a weight.

| Family (`objets`) | Deed | Points | Leans toward |
|---|---|---|---|
| Pickaxes | an ore mined (`#c:ores`) | 3 | Fortune 3, Efficiency 1, Unbreaking 1 |
| | a block mined (`#minecraft:mineable/pickaxe`) | 1 | Efficiency 3, Unbreaking 1, Silk Touch 0.5 |
| Axes | a log cut, or a TreeChop chop | 1 | Efficiency 3, Unbreaking 1 |
| | a creature beaten (any, undead, arthropod) | 15 | Sharpness 2, Smite 3, Bane of Arthropods 3 |
| Shovels | a block dug | 1 | Efficiency 3, Unbreaking 1, Silk Touch and Fortune 0.5 |
| Hoes | a crop harvested, a block of `mineable/hoe` | 1 | Fortune 3, Efficiency 3, Unbreaking 1 |
| Swords | a creature beaten | 15 | Sharpness 3, Looting, Sweeping Edge, Knockback, Unbreaking; Smite 4 on undead, Bane 4 on arthropods |
| Knives (`#c:tools/knife`) | a creature beaten | 15 | Backstabbing 2, Sharpness 2, Looting 2, Unbreaking 1 |
| Bows, crossbows, tridents | a creature beaten with it | 15 | Power, Punch, Flame, Infinity; Quick Charge, Piercing, Multishot; Impaling, Loyalty, Riptide, Channeling |
| Helmets | damage taken (per point); seconds under water | 5; 3 | Protection, Projectile Protection; Respiration 3, Aqua Affinity 1 |
| Chestplates, leggings | damage taken, by kind (fire, explosion, projectile) | 5 | Protection, Fire, Blast, Projectile Protection |
| Boots | damage taken; falls; blocks walked; blocks swum | 5; 10; 0.05; 1 | Protection, Feather Falling 4; Unbreaking; Depth Strider |
| Shields | damage blocked | 5 | Unbreaking |
| Fishing rods | a catch (`ItemFishedEvent`) | 30 | Luck of the Sea, Lure, Unbreaking |
| Swords (more) | a burning creature beaten; a creature beaten in the Nether | 15 | Fire Aspect 3; Fire Aspect 2 |
| Bows (more) | a creature beaten from more than 20 blocks; burning; in the Nether | 15 | Power 3, Infinity 1; Flame 3; Flame 2 |
| Crossbows (more) | a creature beaten from more than 20 blocks | 15 | Piercing 3 |
| Tridents | a creature beaten; an aquatic one (`#minecraft:sensitive_to_impaling`); with a thrown trident; thrown in the rain; in water, not thrown | 15 | Impaling, Unbreaking; Impaling 4; Loyalty 3; Channeling 3; Riptide 3 |
| Maces | a creature beaten; while falling (a smash); an armoured one; undead; arthropod | 15 | Density, Breach, Unbreaking; Density 3, Wind Burst 2; Breach 3; Smite 3; Bane 3 |
| Chestplates (more) | blows taken from a creature or a player (`mob_attack`, `player_attack`) | 5 | Thorns 2 |
| Leggings (more) | blocks walked crouching | 0.5 | Swift Sneak 3 |
| Boots (more) | blocks walked on soul sand or soul soil | 0.5 | Soul Speed 4 |
| Anything with durability (`#minecraft:enchantable/durability`) | a durability point regained by repair (anvil, grindstone, Overgeared) | 0.5 | Mending 3, Unbreaking 1 |
| Create potato cannon | a creature beaten; a burning one | 15 | Potato Recovery 3, Power, Punch; Flame 3 |
| Create backtanks | seconds under water | 3 | Capacity 3 |
| Supplementaries slingshot | a creature beaten; from far | 15 | Stasis |
| Obsidiceratops armour (Archie) | steps near lava (within 3 blocks); fire damage taken | 0.5; 6 | Fire Protection 3, Protection 1; Fire Protection 4 |
| Chorusoatl armour | fall damage taken; steps at y 120 or higher | 10; 0.3 | Feather Falling 4; Feather Falling 2, Protection 1 |
| Coralosaur armour | seconds under water at y 40 or lower; blocks swum | 4; 1.5 | Respiration 3, Depth Strider 2, Aqua Affinity 1; Depth Strider 3 |
| Therimoss armour | blocks walked where it never rains | 0.1 | Unbreaking 3, Protection 1 |
| Sculksaur armour | steps crouching in the dark (light 4 or less) | 0.8 | Swift Sneak 4, Protection 1 |
| Lushsaur armour | steps crouching near animals (8 blocks) | 0.8 | Swift Sneak 3, Protection 1 |

A creature counts for its maximum health divided by 20, at most 10: a zombie gives 15 points to a sword, a chicken 3. Walking counts once a second, on the ground, not mounted, at most 12 blocks a second, for every armour piece worn. Every enchantment of the pack has a source, a deed or a great beast, except the curses (by design), Frost Walker (Frostmaw only) and the three enchantments of Alex's Mobs' straddleboard (too narrow an item).

An entry can name the moment with `si` (one condition or a list, all required, `!` to negate): `nether`, `end`, `surface` (the player's dimension), `accroupi`, `pluie` (rain falls on the player), `dans_l_eau` (water or rain), `cible_en_feu`, `armure` (the creature wears armour), `lance` (the blow came from a projectile), `loin` (the creature was more than 20 blocks away), `ecrase` (the player was falling when striking), `sous:<block or #tag>` (the block under the feet). `objets` also takes a list.

A bow's arrow carries a copy of the bow: the kill is counted on the bow in the inventory that has the same id. A thrown trident carries the real one, which comes back.

**Mending without experience.** Fallow has no experience, so vanilla Mending would do nothing. Here an item with Mending mends itself by living: each deed point gives back `raccommodage` (0.2) durability per level, the fraction drawn at random. A Mending I pickaxe regains a point every five stones; a Mending sword regains about three points per zombie. This repair does not count as the deed « repaired ».

The deeds of an item are kept by the world (`data/fallow_eveil.dat`, under the item's id), not on the item: its component changes only when it becomes ready, awakens, forgets or reaches the next quarter of its way, so the item in hand does not twitch at every block. An item that breaks, burns or despawns takes its deeds with it. A copy of an item (creative mode) gets its own id at once and keeps what the original had lived.

**Archie's armours ask for a part of their beast.** Besides the ink, an Archie's Dinosaurs armour piece awakens only if the player carries a part of its creature, which the table takes (`PartDeBete`): an obsidian horn (Obsidiceratops), a chorus fossil (Chorusoatl), a coral rib (Coralosaur), a mossy claw (Therimoss), sculk toxin (Sculksaur), a lush core (Lushsaur). Without it, the foot of the page says « Il manque … : la bête doit donner une part d'elle. » in red, and its tooltip names the part. They also keep the deeds of their armour slot.

## Great beasts

| Beast | Shard | Chance |
|---|---|---|
| Frostmaw (Mowzie's Mobs) | Frost Walker II | always |
| Ferrous Wroughtnaut (Mowzie's Mobs) | Unbreaking III | always |
| Umvuthi (Mowzie's Mobs) | Smite IV | always |
| Naga (Mowzie's Mobs) | Respiration III | always |
| Tongbi, the Sculptor (Mowzie's Mobs) | Feather Falling IV | always |
| Sculksaur (Archie's Dinosaurs) | Swift Sneak III | one in four |
| Obsidiceratops (Archie's Dinosaurs) | Fire Protection IV | one in four |

Only when a player killed the beast. The shard falls in sight: it glows through walls, never despawns and cannot burn, a column of light rises over it, a crystal sound rings and a note tells the player « Frostmaw laisse un éclat de grande bête. ». Each shard has its colour (ice blue, rust, sun gold, venom green, jade, sculk teal, lava orange) and a line of its own in its tooltip (« Le givre de Frostmaw y dort encore : l'eau prend en glace sous vos pas. »). A shard sets the enchantment at its level (an item that already has it lower goes up), takes one place, and is used up. Each shard is a loot modifier `fallow:eclat_*` in the mod; more can be added as data (type `fallow:eclat_de_bete`, with `couleur` and `recit` optional).

## Nothing found enchanted

- The loot modifier `fallow:sans_enchantement` runs on every loot table: an item loses its enchantments, an enchanted book becomes a book (chests, Supplementaries urns and galleons, Friends & Foes citadels, Wilder Nature bounties, Enhanced Celestials dungeons, fishing, archaeology, creatures).
- Villagers and the wandering trader no longer offer anything enchanted: such an offer is dropped when it is made.
- The anvil refuses a second item that bears enchantments (`[xp] blockEnchantMerge`, now true by default): powers do not pile up by recycling.
- The vanilla enchanting table has no other use: a right-click never opens its window (Hands-On Vanilla's own enchanting station stays off, `enchanting_table=false`; ours takes the block).
- The equipment of a creature (a zombie's sword, a skeleton's bow, a drowned's trident) falls without its enchantments; an awakened item a creature had picked up keeps its own.
- Whatever else gets in (a chest placed with a structure, a reward, a modded recipe, a command): every second, an item in a player's inventory keeps only what its awakenings gave it, and an enchanted book becomes a book. Not in creative mode.
- The grindstone gives nothing for an awakened item: an awakening is not wiped off (alone, it only loses its anvil repair cost, see « Repairing without losing »).

## Settings (`fallow-server.toml`, `[eveil]`)

| Setting | Default | Meaning |
|---|---|---|
| `enabled` | true | Off: no awakening or shard, loot and trades keep their enchantments, the table is the game's own again |
| `seuil` | 2000 | Points for the first awakening |
| `facteur` | 3.0 | Each awakening asks this much more than the previous one |
| `raccommodage` | 0.2 | Durability given back per deed point and per level of Mending |
| `alerte` | 0.9 | Share of the durability used at which an awakened item warns |
| `poste` | true | The table opens as a station (with Diegesis); off, or crouching, the one-row window of before |
| `places.none` … `places.master` | 1, 1, 1, 2, 2, 3 | Awakenings per forging quality |

## Balance

Estimates for an hour of steady use, with the defaults. Durability counts what the item spends; an iron item lasts about 2,000 points of use with its repairs (four anvil repairs before « Trop usé ! », then Overgeared's grindstone).

| Item | Points an hour | First awakening (2,000) | Durability spent for it |
|---|---|---|---|
| Pickaxe, mining | about 1,300 (1 a stone, 4 an ore) | 1.5 h | about 1,800: an iron pickaxe gets there at the end of its life, if repaired; a diamond one easily |
| Shovel, axe | about 1,000 | 2 h | about 2,000 |
| Sword | about 900 (60 zombies) | 2.2 h | about 500 |
| Bow | about 600 | 3.3 h | about 300 |
| Armour piece | about 750 (100 blows) | 2.7 h | about 270 |
| Boots, walking only | about 400 | 5 h | none |
| Fishing rod | about 1,200 (40 catches) | 1.7 h | about 70: a rod must be repaired once |
| Hoe, harvest | about 600 (300 crops) | 3.3 h | none (crops do not wear a hoe) |

The second awakening asks three times more (6,000 points in all), the third nine times more (18,000): a masterwork item carried for twenty hours or more.

Test commands (operators): `/fallow eveil voir` shows the held item's places, state, quality and points per deed against the next threshold; `/fallow eveil points <n>` adds points to the held item's first deed that is not a repair.

**Checked** on the test bench on 2026-11-21: the full pack's dedicated server starts with the module (15 families of deeds) and no new error. In a development client: a new iron pickaxe counts one point per stone block; with `/fallow eveil points 3000` it shimmers and the note shows; at the table the window shows Unbreaking I and Fortune I, the same two after closing and reopening; a click gives Fortune I with the tooltip line « day 1 » and « Awakenings: 1 of 1 »; a second right-click says the item has given all it could; set to its last point of durability it forgets Fortune I (grey note) and its points fall back to 0; a Frostmaw shard is refused on a sword and gives Frost Walker II to iron boots, then is used up; a stronghold library, a desert temple and an ancient city chest give plain books and an unenchanted hoe and leggings; four novice librarians and two novice weaponsmiths offer no enchanted book or sword. Not checked: Overgeared quality and places, a real kill of a great beast, the anvil, deeds of combat, armour, fishing and TreeChop (tests EV5 to EV9 in play).

**Checked** on the test bench on 2026-11-21 evening (polish pass, 0.58.0): in a development client in survival, an enchanted diamond sword and an enchanted book given by command become a plain sword and a book within a second; two pickaxes with the same id get two ids; a zombie with an enchanted iron sword and helmet drops them plain; a test shard on zombies glows on the ground, raises its column of light and the note « Zombie laisse un éclat de grande bête. », with its colour and its line in the tooltip; 1,000 points on a new pickaxe show « Se fait à votre main »; an awakened pickaxe at 240 of 250 warns with the note and the red line; a Mending I pickaxe regains 20 points for 100 deed points; a repair of 115 points gives 57.5 points to `reparer.json#0`; a burning zombie killed at night with a sword counts on the base, undead and burning entries. Not checked: the grindstone, the soul sand, the trident and mace conditions, the Create and Supplementaries items (tests EV16 to EV18).

**Checked** on the test bench on 2026-11-21 night (the table as a station, 0.60.0), development client and server with Diegesis, at 1280 x 720: a right-click with a ready pickaxe leans the camera over the table, the book glides from where it floats and opens, vanilla's floating book is not drawn meanwhile, and the pickaxe is laid by itself; the left page shows « Pioche en fer », « Éveils : 0 sur 1 », « 700 minerais extraits » and « Prêt à s'éveiller » over a full line of ink; the right page shows Fortune I and Efficacité I with what they do and « Né de 700 minerais extraits »; the path under the cursor lights; a click streams runes into the pickaxe, which rises, turns and comes down, the riffled pages carry the table's runes, and the next page says the pickaxe has given all it could while the left one lists « Fortune I, jour 1, par Banc »; leaving gives the pickaxe back with Fortune I. A Frostmaw shard laid by hand and iron boots sent by a shift-click: the shard page, then Frost Walker II on the boots, the shard used up. A pickaxe at 900 points shows « Cet objet n'a pas encore assez vécu. » and « Commence à se faire à votre main » with its line of ink. Not checked: a second player watching, the crouching fallback, the full pack's client (Hands-On Vanilla, Sodium and Iris loaded).

**Checked** on the test bench on 2026-11-21 night (the rite, 0.62.0), development client and server with Diegesis, at night among bookshelves: with a plain iron pickaxe the table stays shut with « La table reste muette … »; with the pickaxe ready it opens on a darkened room with runes drifting from the shelves, its two paths written in runes; hovering Fortune I turns it to French letter by letter in about a second and a half; holding the click spins the pickaxe in a tightening ring of runes and awakens it with a white flash, the caption « Pioche en fer s'éveille : Fortune I. » and « Il porte tout ce qu'il peut porter » at the foot; let go after 0.9 s the pickaxe falls back and nothing is awakened; a shard shift-clicked from the hotbar shows its power in runes, translated on hover, and held, it gives Unbreaking II with the same flash. The first holds did nothing until the button was read from the window (vanilla's `isLeftPressed` is not updated while a screen is open). Not checked: the sounds (no audio device on the bench).

**Checked** on the test bench on 2026-11-22 (ink and selection, 0.70.0), development client and server with Diegesis, in survival: with a ready pickaxe and no ink the paths stay in runes on hover, the foot of the page is red and a click shows « Il manque l'encre d'éveil … »; given three vials and reopened, they go to the inkwell, reading Fortune I draws wisps of ink and the band covers the path to its last line; held, the awakening throws its ring of runes and sparks and the inkwell shows 2. Not checked: the crouching window with ink (EV24), the sounds.

**Checked** on the test bench on 2026-11-22 (after the first play, 0.71.0): the ink is vanilla's potion tinted violet, in the hotbar and in the inkwell; with the inkwell empty the foot of the page reads « L'encrier est vide, les runes se taisent. » on two whole lines and a click adds no caption; with ink, the hovered path lights with no band behind it, the held click draws its line of ink under the name, and the foot « Lisez une voie du regard, puis tenez le clic pour l'éveiller. » is whole on three lines; a shard laid next to a full pickaxe gives its refusal at the foot, whole.

**Checked** on the test bench on 2026-11-22 (0.73.0), with Hands-On Vanilla 1.2.2 and the pack's `handson_vanilla.properties` in the development run: without the mixin a right-click with a ready pickaxe opened vanilla's enchanting window (reproduced); with the window guard alone it closes at once; with both, our station opens and an awakening goes through.

**Layout** (0.74.0): the paper carries a filleted frame drawn by the book (about 3 and 5 units from its edges); everything written stays at least 8 units from the sides and between 7 and 149 units from the top, the right page's paths end above its foot sentence whatever its length, and the foot sentences are short (« Lisez une voie, puis tenez le clic. », « Lisez la force, puis tenez le clic. »).

**The book's look** (0.75.0): a violet night cover (since 0.86.0 midnight violet leather with a tooled frame and gilt corners on its outer side, each with a small star, drawn by `scripts/books/covers.py`; `livre_d_eveil.png`), cream paper, worn at its edges, with one fine violet rule (about 4 units inside the flat part's edges), a small star at each of its corners and a faint watermark of rings (since 0.77.0; `livre_d_eveil_papier.png`), both drawn by `scripts/books/papers.py`; a page turned or riffled curls over with its writing on it ([mod-fallow](mod-fallow.md#books-livres-client)).

**Checked** on the test bench on 2026-11-22 (a place apart, 0.79.0), development client and server with Diegesis, by day with the bookshelves behind the table removed: sitting, the ground beyond the table fades to violet within a second, and stays veiled; holding a path, the ground and the shelves are gone, only the table, the book and the runes remain; after the flash the veil comes back; leaving, the world is dark violet at 0.4 s, grey-violet at 1.4 s and clear at 2.6 s. A first version timed out the table's aim in milliseconds and let the world come back on a slow frame: it now counts frames. Not checked: the sounds (no audio on the bench), with ATMOSPHERICS, Distant Horizons and Brumaire (EV25).

**Checked** on the test bench on 2026-11-22 (attachment, 0.81.0), development client and server with Diegesis: a diamond pickaxe named « Brise-Roche » gets 25 points; nine repairs from 20 % wear give 12 each, the note goes to Entretenu then Chéri; let down to 95 % wear it loses 12 (121 in all); its places are 4 (1 without quality, 2 for diamond, 1 cherished); its tooltip reads « Chéri : un éveil de plus » in light purple and the table's book « ◇ ◇ ◇ ◇ chéri ». Not checked: the item frame night, the death pickup, the grindstone, the crafting grid refusal, an Entretenu item worn out, Archie's armours (no Archie's Dinosaurs on the bench) (EV26 to EV31).

**Checked** on the test bench on 2026-11-22 (third pass, 0.90.0), development client and dedicated server: an Entretenu iron chestplate stopped at 239 gives 0 armour, 6 once repaired; a pickaxe let into the red loses 12 once, then mended and worn by a few points five times in a row (230, 228, 231, 229, 232) loses nothing more; `/summon` with `peuple` gives a masked Host with its armour; the « Hamster Tips » warning no longer reaches the chat. Not checked: shears, flint and steel and the brush on blocks and creatures, the dispenser, a restart (EV33, EV34). Fourth pass (0.91.0): a stopped Protection IV iron chestplate takes the full 10 damage of `/damage @s 10 minecraft:mob_attack`. Fifth pass (0.92.0): stopped shears open a door, leave a pumpkin whole and do not shear a sheep; ordinary shears carve and shear. Not checked: the smithing anvil, the crafting grid (EV35, EV36). Sixth pass (0.93.0): a stopped smithing hammer and stopped shears open a door, stopped shears leave a pumpkin whole, and a torch in the off hand is placed on stone past them. Known and left: an Entretenu axe already stopped still cuts planks in the grid (the KubeJS recipe cannot test its wear).

## In-game tests

| # | Test | Expected |
|---|---|---|
| EV1 | Mine stone with a new pickaxe, then `/fallow eveil voir` | The deed `pioche.json#1` grows by one per block; the tooltip shows « Éveils : 0 sur N » |
| EV2 | `/fallow eveil points 2000` with that pickaxe | Note « … a appris quelque chose … », sound, the pickaxe shimmers, tooltip « Un éveil attend » |
| EV3 | Right-click an enchanting table with it, by night, among bookshelves | The camera leans over the table, the room darkens, a low drone rises and runes drift from the bookshelves to the book; the floating book glides to you and opens, the pickaxe lies on the table; left page: its name, its places as signs, its deeds in words (« Des centaines de minerais extraits »); right page: two paths written in runes; leaving and coming back shows the same two; a path read under the cursor turns to French letter by letter; holding the click on it raises and spins the pickaxe in a ring of runes, then a white flash, a deep sound, the runes stream in and it comes down awakened; the next page says it has given all it could (or shows its next paths); tooltip line with the day and the player |
| EV4 | Right-click the table with nothing ready in the inventory; then with a pickaxe that is not ready alongside a ready one; with a full item laid; crouch-click with a ready one | Nothing ready: the table does not open, a faint chime and « La table reste muette : rien de ce que vous portez n'est prêt à s'éveiller. »; laid on the table, the pickaxe that is not ready gets « Cet objet n'a pas encore assez vécu. » with the line of ink at the foot of the left page; the full item, « Cet objet a déjà donné tout ce qu'il pouvait. »; crouching, the one-row window of before |
| EV5 | Wear an enchanted pickaxe down to its last point (or Overgeared's broken state) | Grey note « À bout de forces, … oublie Efficacité I. », the enchantment goes, `/fallow eveil voir` shows the points brought back; repaired then worn out again, it forgets the next one |
| EV6 | Kill Frostmaw; at the table, boots in hand and the shard in the other hand | A shard « Éclat de grande bête (Frostmaw) » drops; the boots take Frost Walker II, the shard is used up; on a sword, « Cet éclat ne prend pas sur cet objet. » |
| EV7 | Open ten chests (desert temple, mineshaft, shipwreck), fish, trade with a librarian and a weaponsmith | No enchanted item or book anywhere; books appear as plain books |
| EV8 | Forge a masterwork pickaxe and a poorly forged one, then `/fallow eveil voir` after one block each | 3 places and 1 place |
| EV9 | Anvil: an enchanted sword with another enchanted sword | No result |
| EV10 | In survival, `/give` an enchanted sword and an enchanted book | Within a second the sword is plain and the book a book |
| EV11 | Kill a zombie carrying an enchanted sword and helmet | Both fall without enchantments |
| EV12 | `/fallow eveil points 1000` on a new pickaxe, then its tooltip | « Se fait à votre main » |
| EV13 | An awakened pickaxe at 90 % wear | Gold note « … faiblit. À bout de forces, cet objet oubliera … », a red line in the tooltip; repaired, the line goes |
| EV14 | Repair a worn pickaxe at the anvil, then `/fallow eveil voir` | `reparer.json#0` grew by half the points regained |
| EV15 | A pickaxe with Mending: mine | Its durability comes back about one point in five stones |
| EV16 | Kill a burning zombie with a sword at night | `epee.json#3` (burning creature) grows; walking on soul sand with boots grows `bottes.json#5` |
| EV17 | A great beast's shard falls | It glows through walls, light rises, note « … laisse un éclat de grande bête. », its colour and its line in the tooltip |
| EV18 | Put an awakened sword in a grindstone | No result |
| EV19 | At the table, shift-click a Frostmaw shard then iron boots in the hotbar | Each flies to its place; the right page shows the shard, its story and Frost Walker II; its power is in runes until read; holding the click on the page pours it: the boots rise, the shard is gone, the left page lists « Semelles givrantes II, jour …, éclat de Frostmaw » |
| EV20 | Watch a friend awaken an item at a table | The item rises, turns and comes down, runes stream from the book, the table's sound |
| EV21 | Leave the table | The book half shuts and goes back over the table; the floating book of vanilla is never seen twice; what lay on the table is back in the inventory |
| EV22 | At the table, hold the click on a path, let go after one second | The pickaxe rose and turned, then falls back to its place with a dying sound; nothing is awakened; holding again to the end awakens it |
| EV23 | Craft awakening ink (glass bottle, lapis lazuli, glow ink sac, amethyst shard); open the table with a ready pickaxe and no ink, then with three vials | Without ink: the paths stay in runes on hover, the foot of the right page says the inkwell is empty, holding the click makes the book and the inkwell shiver, with no caption; with ink: the three vials go to the inkwell by themselves, reading a path draws wisps of ink to it, the awakening leaves two |
| EV26 | Name a new iron pickaxe at the anvil, repair it twice before half its wear, then `/fallow eveil voir` | Attachment about 49, tooltip « Entretenu : à bout de forces, il tient bon », a green note; the book shows « entretenu » after its places |
| EV27 | An Entretenu awakened item worn to its last point, quickly (a pickaxe on stone with no Unbreaking) | Grey note « … n'a rien oublié : réparez cet objet. » once, the enchantment stays, the item does not break, the tooltip says it waits to be repaired; it no longer mines, strikes, shoots or tills, a door still opens with it in hand; repaired below 90 % it works again |
| EV28 | Leave an awakened sword in an item frame through a night, then on the ground a few minutes | `voir` shows +5 for the night, then the points falling |
| EV29 | Diamond and orichalcum pickaxes, a steel and a black steel chestplate, all poorly forged | Places 3, 4, 2, 2 |
| EV30 | Two worn awakened pickaxes in the crafting grid; one alone on the grindstone after « Trop usé ! » | No repair result; the grindstone gives it back with its awakenings and the anvil takes it again |
| EV31 | An Obsidiceratops chestplate ready to awaken, at the table without then with an obsidian horn | Without: red foot « Il manque Corne d'obsidienne … », the book shivers; with: it awakens and the horn is gone; walking near lava counted toward Fire Protection |
| EV32 | An Entretenu awakened iron chestplate at 239 of 240 wear (`/item replace … minecraft:damage=239`), then repaired | `/attribute @s minecraft:generic.armor get` gives 0 while it is stopped, 6 once repaired; it never breaks while stopped |
| EV33 | Entretenu shears and flint and steel stopped at their last point: a sheep, a pumpkin, a creeper, TNT; a stopped knife on the fleshing bench; the same shears in a dispenser | Nothing happens and the grey note says why each time; a door still opens and a villager still trades with them in hand; in the dispenser they wear out and break |
| EV34 | A Mending pickaxe at 92 % wear picking up XP orbs, then `/fallow eveil voir` | Attachment lowered by 12 once when it entered the red, not again per orb; after a server restart it is not lowered again |
| EV35 | A stopped Entretenu smithing hammer on Overgeared's anvil with a recipe laid; stopped shears on a door, a chair and a bed; a stopped Protection IV chestplate under `/damage @s 10 minecraft:mob_attack` | The forging does not start and a grey note says why; the door opens, the chair seats you, the bed sleeps; the full 10 damage is taken |
| EV36 | Planks from a log and an Entretenu awakened axe at 1 durability left, in the crafting grid | The planks come, the axe stays at its last point with its awakenings |
| EV25 | At the table by day, outdoors, then hold a path, then leave | Sitting: the view widens then settles, a violet mist veils the ground a few blocks away and the sky, rain, creatures and music fall quiet; during the rite only the table, the book and the runes remain, dust comes in from the dark; after the flash the veil comes back; leaving, the mist and the sounds return over about three seconds |
| EV24 | Crouch-click the table with a ready pickaxe, without ink then with one vial | Without: « Il manque l'encre d'éveil … », no window; with: the window opens and the vial is gone after the choice |
