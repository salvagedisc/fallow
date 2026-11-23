# The Hosts

Six masked peoples live in Fallow's wild places. Each one guards one of Archie's Dinosaurs and wears its armour. Each people sends one recurring emissary, who comes out of the mist, speaks with the player and accepts an offering.

Status: the visit loop is in (`fallow` 0.104.0, `hotes` module): the peoples now come on their own to clearings where a player stands, ask for three things, give back at dawn, and remember. Checked on the bench client on 2026-11-23, not played.

## The six peoples

Each people is named by the way it guards its beast. Names and lines are in French in game (the people's name is a gesture, the emissary's a made-up name).

| Id | Beast (Archie's Dinosaurs) | Emissary – people (fr) | English | Mask | Habitat behind the portrait | Offerings (always; more by season in `data/fallow/hotes/<peuple>.json`) |
|---|---|---|---|---|---|---|
| `ecorce` | Lushsaur | Ounèle – les Discrets | the Unseen | `fallow:masque_de_l_ecorce` | lush cave, bioshrooms | glow berries, honey bottle, bone meal, moss block |
| `braise` | Obsidiceratops | Orrek – les Attiseurs | the Stokers | `fallow:masque_de_la_braise` | redstone caves | charcoal, coal, any cooked meat, magma block |
| `givre` | Chorusoatl | Cilvi – les Chantres | the Cantors | `fallow:masque_du_givre` | prismachasm (Regions Unexplored) | amethyst shard, feather, any wool, snowball |
| `fonds` | Coralosaur | Loubé – les Souffleurs | the Breathgivers | `fallow:masque_des_fonds` | warm reef, light shafts from the surface | cod, kelp, sea pickle, dried kelp |
| `sables` | Therimoss | Kéta – les Guetteurs | the Watchers | `fallow:masque_des_sables` | jungle | melon slice, bamboo, cocoa beans, sugar cane |
| `silence` | Sculksaur | Sifé – les Taiseux | the Hushed | `fallow:masque_du_silence` | deep dark, sculk sensors, one grey candle | any wool, any candle, brown mushroom, string |

## A visit

1. **The sign, the evening before.** At dusk, in a clearing where a player stands, a people whose biomes surround the clearing may hang its mask on a post just outside the edge. The peoples come from the biomes around the clearing (tags `fallow:hotes/<peuple>`, read from the generated biomes at the clearing's height and 24 blocks below, so lush caves and the deep dark count); the two most present take turns. A clearing with none of these biomes around it is visited by the Discrets. A note says who will come tomorrow evening.
2. **The visit, the next evening.** The post goes; the emissary stands in its place, one block outside the edge, with an offering mat laid one block inside. They never cross the edge and never walk up to the player. If no one is in the clearing that evening, the visit waits for an evening when someone is.
3. **Three wishes.** Talking to the emissary shows three wishes drawn from the people's list for the season (two of the season when it has any, the rest from the always list; `data/fallow/hotes/<peuple>.json`). A wish can be an item or any item of a tag (any wool, any cooked meat). Wishes the player cannot fill are greyed. A gift goes from the inventory onto the mat and stays there until dawn; it cannot be taken back.
4. **Dawn.** The emissary returns to the mist.
   - Something was given: the mat holds the counter-gift, one draw per wish filled from the list of the bond's tier (an empty tier uses the tier below). A click on the mat takes it and the mat dissolves.
   - The player declined politely (« Ne rien laisser »): nothing changes, the mat goes. One polite refusal per 24 days (a season with Ecliptic Seasons' default length).
   - Nothing at all: the mat stays rolled up at the edge (a click removes it).
5. **The bond** (per clearing and per people, never shown as a gauge): each gift is worth 2 points, all three 1 more. Tiers: Inconnus (0), Voisins (6), Proches (16), Familiers (32), each with a better counter-gift list, and a note at dawn when a tier is reached. Proches ask one and a half times as much, Familiers twice. The next visit comes after 5 days, one day sooner per tier and one sooner in the people's season, never under 2.
6. **Forgetting.** A visit with nothing on the mat costs 2 points. The second in a row, the mist blows out the campfires and candles near the edge. The third, the people stays away for 24 days and the bond drops a tier.
7. **Hitting an emissary** (a player's hit; a creature's blow or arrow does nothing during a visit) makes them dissolve; the bond drops a tier and the people stays away for 24 days. The mat stays, rolled, with what was on it.

Nothing is destroyed and nothing fights. The sign and the mat have no item, drop nothing and stand up to explosions. A clearing whose fire goes out keeps its bonds for 24 days, so relighting or moving a fire loses nothing. Settings: `[hotes]` in `fallow-common.toml` (`visites`, `intervalle`, `premiereVisite`, `refusPoli`).

Each people's season: Écorce spring, Braise winter, Givre autumn, Fonds and Sables summer, Silence none (they come in every season).

## How it works

- `/fallow hotes appeler <peuple>` (operators): the emissary appears 14 blocks ahead of the player and walks up. Outside a visit they only talk.
- `/fallow hotes visite <peuple>`: a visit right now, at the edge of the player's clearing. `/fallow hotes aube`: dawn right now for that visit. `/fallow hotes lien`: what the peoples remember of the clearing (points, tier, forgettings, next visit, the visit under way).
- The dialogue is drawn like the pack's books (`scripts/hotes/feuillet.py`): a loose sheet of their paper at the foot of the screen, written in their brown inks with the rubric red (the people's name in an ink of its own milieu), the portrait printed in it as an engraving's plate, the answers' pictograms in ink (a tied bundle, a question mark, a doorway, a return arrow, a ticked question, an empty cloth), the chosen answer marked by the books' red lozenge. A wish shows its count in ink after its name (« Charbon ×6 »).
- The meeting is set apart from the world (0.105.0, with the enchanting table's `eveil/client/Isolement`, now a `Scene` any screen can aim at): a mist in the people's colour from about 5 to 18 blocks (2.5 to 11 once it has settled), the sky gone, the world's sounds at 25 %, the view narrowed by up to 12 %; the camera frames the middle of the mask in the window left between the top band and the sheet, so close up the eyes sit in its middle. Two dark bands (a twelfth of the screen each) close in 450 ms, the sheet rises 14 px in 180 ms, an amethyst resonance sounds at the people's pitch (Taiseux lowest, Chantres highest) with a soul's escape under it, and every 220 ms a particle of the people's milieu appears round the emissary, never within two blocks of the eye. While a line is written, its last five letters are in the enchanting table's script (`minecraft:alt`, accents dropped, punctuation in the plain script) before they settle into French. Leaving, the mist and the sounds come back over about 1.6 s.
- Right-clicking the emissary opens the dialogue screen: a live portrait of the emissary turned 25°, framed the same way for every people, over the people's habitat. The title reads « Nom – les Peuple ». The answers are a menu ([0042](../decisions/0042-interaction-language.md)): a click finishes the text, Space or a click continues, Escape leaves, and the camera turns in 150 ms.
- Most lines have variants (`<key>.2`, `<key>.3` in the lang files), picked at random each time.
- The emissaries speak to the player as « tu »: it is in-world speech, not one of Fallow's own hints, which say « vous » ([0042](../decisions/0042-interaction-language.md)).
- An emissary ignores damage that has no attacker (suffocation, cramming, a fall), unless the damage bypasses invulnerability (`/kill`, the void), so it cannot vanish on its own.

## Masks and regalia

- A mask is a real helmet (`ArmorItem`). Its shape is described once, in `assets/fallow/hotes/masques/<peuple>.json`. `MasqueModel` reads it for the worn model, and the item icon is drawn from the same shape. Pieces marked `porte_seulement` are only on the worn mask: they are left out of the item and of its icon.
- Every mask shares one outline: the plate covers x -4..3 and y -9..0, and the ornaments stay inside x -6..5 and y -12..0. Icons are anchored on the same pixel (8, 14) and their outline is always closed, so the six masks line up in the inventory. Item models are rotated by 45° at most.
- The regalia (`assets/fallow/hotes/parures/<peuple>.json`, `ParureLayer`) are the rest of the people's dress; the emissaries keep Archie's boots.
- No red paint on a mask: the Braise's plate carries lava cracks and its collar is obsidian with one lava vein; the Sables' leaves hang down on both sides with a small sprout in the middle.
- The portrait backgrounds share the Écorce's composition: darker sides that frame the bust, the habitat's light behind it in flat steps (no checker dithering), a ceiling and a floor band, five or six small accents of the habitat (`portrait_<peuple>.png`, 40 × 40 inside the common frame).
- Flat planes have a back face that mirrors the front face exactly, otherwise they flicker.

## Next steps

- Relics at the tiers (a lasting gift tied to the pack's modules: approach, climate, rest, fishing), a notebook of what each people likes, the guardian beasts and the consecrated helmet.
- Play a full week of visits with each people in a real world.
