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

- **Coming to the table.** A right-click turns the camera over the table from where the player stands. The table's floating book leaves its place (it is the same book: vanilla's is not drawn while the scene is there) and glides to the reading place, opening as it comes. Leaving, it half shuts and goes back to float over the table.
- **Laying things.** Two places on the table top: the item on the left as the camera sees it, a great beast's shard on the right. The item held when coming is laid by itself once the camera has arrived; a shift-click in the hotbar sends a stack to its place in an arc; a stack dropped from the cursor goes too. Whatever is not taken back returns to the inventory on leaving. The hotbar greys out what the table does not take.
- **The left page: its life.** The item's name and icon, « Éveils : 1 sur 2 », each awakening with its day, its player and the beast of its shard, its three greatest deeds with how many times (« 1 204 minerais extraits », counted apart from the points so a creature counts once whatever its weight), and at the foot where it is on its way, in words and as a line of ink that fills.
- **The right page: its paths.** Ready, « Deux voies s'ouvrent »: each path with its name, what it does (`eveil.fallow.pouvoir.*`), the level it carried before, and « Né de 702 minerais extraits », the deed that weighed most toward it. The path under the cursor lights up; a click awakens it. Not ready, full, or with nothing left to learn, the page says so in one sentence in its middle, and a click makes the book shiver.
- **A shard.** With a shard on the table the right page is the shard's: the beast, its story, the power and its level, and what the table says (pour it, lay the item on the left, it does not take, the item already bears it, no place left).
- **Each new page comes by a page turned**; while the sheets riffle (an item just laid) the last one brings it, and the leaf that goes keeps what it said. Pages with nothing written carry the table's runes, faded, as the floating book does.
- **Others at the table** see the item rise and come down, and the runes stream, with the server's sound.
- Crouching, or with `[eveil] poste = false`, the table keeps the one-row window of before (item in the main hand, shard in the other hand).

## Places by forging quality

An item can carry as many awakenings as its Overgeared quality allows (`[eveil.places]` in `fallow-server.toml`): none or poor or well forged 1, expert or perfect 2, masterwork 3. The number is fixed at the item's first deed and shown in its tooltip (« Éveils : 1 sur 2 »). An item without quality (bow, fishing rod, shield, leather) has 1.

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

A creature counts for its maximum health divided by 20, at most 10: a zombie gives 15 points to a sword, a chicken 3. Walking counts once a second, on the ground, not mounted, at most 12 blocks a second, for every armour piece worn. Every enchantment of the pack has a source, a deed or a great beast, except the curses (by design), Frost Walker (Frostmaw only) and the three enchantments of Alex's Mobs' straddleboard (too narrow an item).

An entry can name the moment with `si` (one condition or a list, all required, `!` to negate): `nether`, `end`, `surface` (the player's dimension), `accroupi`, `pluie` (rain falls on the player), `dans_l_eau` (water or rain), `cible_en_feu`, `armure` (the creature wears armour), `lance` (the blow came from a projectile), `loin` (the creature was more than 20 blocks away), `ecrase` (the player was falling when striking), `sous:<block or #tag>` (the block under the feet). `objets` also takes a list.

A bow's arrow carries a copy of the bow: the kill is counted on the bow in the inventory that has the same id. A thrown trident carries the real one, which comes back.

**Mending without experience.** Fallow has no experience, so vanilla Mending would do nothing. Here an item with Mending mends itself by living: each deed point gives back `raccommodage` (0.2) durability per level, the fraction drawn at random. A Mending I pickaxe regains a point every five stones; a Mending sword regains about three points per zombie. This repair does not count as the deed « repaired ».

The deeds of an item are kept by the world (`data/fallow_eveil.dat`, under the item's id), not on the item: its component changes only when it becomes ready, awakens, forgets or reaches the next quarter of its way, so the item in hand does not twitch at every block. An item that breaks, burns or despawns takes its deeds with it. A copy of an item (creative mode) gets its own id at once and keeps what the original had lived.

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
- The grindstone gives nothing for an awakened item: an awakening is not wiped off.

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

## In-game tests

| # | Test | Expected |
|---|---|---|
| EV1 | Mine stone with a new pickaxe, then `/fallow eveil voir` | The deed `pioche.json#1` grows by one per block; the tooltip shows « Éveils : 0 sur N » |
| EV2 | `/fallow eveil points 2000` with that pickaxe | Note « … a appris quelque chose … », sound, the pickaxe shimmers, tooltip « Un éveil attend » |
| EV3 | Right-click an enchanting table with it | The camera leans over the table, the floating book glides to you and opens, the pickaxe lies on the table; left page: its name, « Éveils : 0 sur N », its deeds with counts; right page: two paths with what they do and « Né de … »; leaving and coming back shows the same two; a path lights under the cursor, a click streams runes into the pickaxe, which rises, turns and comes down; the next page says it has given all it could (or shows its next paths); tooltip line with the day and the player |
| EV4 | Right-click the table with a pickaxe that is not ready, with nothing, with a full item; crouch-click with a ready one | Right page « Cet objet n'a pas encore assez vécu. » with the line of ink at the foot of the left page, « La table attend un objet qui a vécu. » with the invitation on the left, « Cet objet a déjà donné tout ce qu'il pouvait. »; crouching, the one-row window of before |
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
| EV19 | At the table, shift-click a Frostmaw shard then iron boots in the hotbar | Each flies to its place; the right page shows the shard, its story and Frost Walker II; a click pours it: the boots rise, the shard is gone, the left page lists « Semelles givrantes II, jour …, éclat de Frostmaw » |
| EV20 | Watch a friend awaken an item at a table | The item rises, turns and comes down, runes stream from the book, the table's sound |
| EV21 | Leave the table | The book half shuts and goes back over the table; the floating book of vanilla is never seen twice; what lay on the table is back in the inventory |
