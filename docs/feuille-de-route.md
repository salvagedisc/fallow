# Roadmap

Status as of 2026-11-14. The project is split into sub-projects, each with its own spec → plan → implementation cycle (see [archive/conception/](archive/conception/)).

| # | Sub-project | Status |
|---|---|---|
| 1 | Repository & tooling: packwiz, sync from the instance, CI, release | ✅ Done ([spec](archive/conception/specs/2026-09-23-repo-outillage-design.md)) |
| 2 | System choices: farming, fauna, structures, atmosphere, performance | ✅ Decisions 0001 to 0026 made and applied to `pack/`; every mod of the [registry](mods.md) marked ✅ is in the pack |
| 3 | Rules layer: configs, KubeJS scripts, datapack data, custom mod | 🟡 Configs for most systems, KubeJS scripts (26 on 2026-11-01; the Friendly Moon and fauna scripts of No Man's Land have gone since), 6 Questlog chapters, custom mod `fallow` 0.8.0 (sky, xp, far view, clearings, dissipator, screen titles, tools); the eight approved modules, monument guardians and the seasons bridge still to write |
| 4 | Server: host, deployment, backups | 🟡 Backups (Simple Backups) and Neruina in the pack; host and deployment not started |

## Where the pack stands

The pack starts on a dedicated server, but **nobody has played it yet**. The next step is the play sessions of [tests-en-jeu.md](tests-en-jeu.md): eight sessions of about an hour, which gather every in-game test of the plan pages. Everything frozen at world creation (Tectonic, Streams Reflowing, the biome mods, clearings) must pass before the permanent world is pregenerated.

| Date | Milestone |
|---|---|
| 2026-09-23 | Repository and tooling, base pack of 12 mods |
| 2026-10-10 | Cleanup, decisions 0002 to 0007 merged, plan v1 |
| 2026-10-11 and 10-18 | Refocus on the No Man's Land trailer: an almost empty world, the mist that lifts, long-term goals, a polished and connected finish |
| 2026-10-19 | The team's 57 choices applied; everything in French; Wayfarer ideas (Envelope, Friendly Moon, health, tannery, no XP); music; `fallow` 0.1.0 to 0.2.1 |
| 2026-10-25 | Tectonic, Streams Reflowing and Distant Horizons (0011); safety net and 24 updates; six mods from the review; `fallow` 0.4.0 with clearings and the dissipator |
| 2026-10-26 | Research on what is done elsewhere; eight next modules specified; simpler bag, stacks by kind of item, 30-minute days and 15-minute nights (0012) |
| 2026-10-26 and 10-27 | Questlog quests and guides; the `fallow-interface` pack rebuilt on GUI Revision |
| 2026-10-29 and 10-30 | First gestures (0013), Tannery (0014), Vanilla Evolved textures (0015), wind and light without shaders (0016), mist ring with Distant Horizons, reversible clearings, snow and dust storms, Friendly Moon fixed, Wildex removed, painted title screen, NML explosives back |
| 2026-11-01 | 20 weakly animated creatures cut, held-item hand fixed, pack sort (Chipped and Beautify! removed, 313 items hidden), `sac` module removed (`fallow` 0.5.0) |
| 2026-11-11 to 11-13 | The team leaves No Man's Land (11-11); candidates measured on one seed (11-12) and Tectonic + Regions Unexplored + Geophilic chosen ([0019](decisions/0019-leaving-no-mans-land.md)); the Friendly Moon, its fauna rules and its music go; mist preset `fallow_v6`, `biomes.py`, French translation of Regions Unexplored; `fallow` 0.7.0; the mods' biome rules applied to Regions Unexplored, clearings that keep their biome (`fallow` 0.8.0, [0020](decisions/0020-clearings-keep-their-biome.md)) |
| 2026-11-13 | Cleanup: the three house resource packs (interface, textures, ambiance with its mist and free tracks) and the `titres` module removed, Adorable Hamster Pets' announcements, the narrator and the experimental-settings warning turned off (0024, `fallow` 0.10.1) |
| 2026-11-14 | Second cleanup: 15 mods (FancyMenu and Drippy with the house title and loading screens, the rain mods, Shine, Polytone, Chunky and others) and the Supernova pack removed, the `averse` and `rayons` modules removed (`fallow` 0.11.0), Distant Horizons warnings and the Hamster Tips book turned off, music on 30 game tracks (0026) |

## Before the first release (`v0.1.0`)

- [ ] Run the [in-game tests](tests-en-jeu.md) and fix what breaks.
- [ ] Compare `pack/config/quark-common.toml` with the config generated on first launch (the keys were inferred from the Quark 4.1 classes, [decision 0005](decisions/0005-quark.md)).
- [ ] Turn `[Unreleased]` in the changelog into `[0.1.0]` and place the tag.
- [ ] Pregenerate the permanent world once the terrain tests (W) pass, with `/dh pregen` (Chunky left the pack on 2026-11-14, [0026](decisions/0026-second-cleanup.md); a server owner may add it, but it leaves holes in the far view) ([pack sort](plan/tri-du-pack.md)).

## Updates held until the in-game tests

These updates exist on Modrinth but change something that must first be seen in game.

| Mod | In the pack | Held version | Why wait |
|---|---|---|---|
| Spatial GUI | 1.8 | 1.9 | Was held together with Polytone, which left the pack ([0026](decisions/0026-second-cleanup.md)) |
| Supplementaries | 3.9.9 | 4.x | Major version; our config (0006) must be re-read |
| Friends & Foes | 4.0.27 | 5.x | Major version; the creatures disabled by config must stay disabled |
| Iris Veil Compat | 0.3.0 (pinned) | 0.4.0 | 0.4.0 drops water and stained glass from Brumaire's sun shadow map (caustics, light through stained glass); wait for a release that keeps them |
| Envelope | 0.7.5 | 0.8 | Its JEI mixin crashed once (see the changelog); to retest with JEI 19.51 |
| Icy's Better Horses | 2.0.1 | 2.1.x | Revival costs experience levels, which no longer exist ([0009](decisions/0009-xp.md)): a replacement cost is needed |
| Cold Sweat | 2.4.3.1 | 2.4.4 | Breaks Cold Sweat: Altitude until Altitude follows |
| Lithium | 0.15.4 | — | To remove if the world freezes with Sable airships |
| Subtle Effects, More Darkness | 1.15.3, 2.0.1 | — | Client keys to read in game before tuning ([0007](decisions/0007-ambiance-performance.md)) |
| Archie's Dinosaurs + Astemir RE Lib | 1.0.4, 1.2.985 (jar) | 1.0.8, 1.2.987 | The `fallow` mixins into Archie (zoo guide, armour models, Chorusoatl) must be checked against the new classes; the library is only published as snapshots |
| Ghost | 1.3.4 | 1.4.0 | Puts kodama in every forest at world generation and changes the spawns: to look at in game first |
| Overgeared | 1.6.19 | 1.7.x | A rewrite (anvils renamed, tiers moved to `overgeared-anvil-tiers.json`, two hotfixes in six days): our tier-A setup and the Questlog ids must be redone |
| Let's Do Farm & Charm | 1.1.25 | 1.1.29 | Adds a cutting board, cleavers and a stove grill that overlap Farmer's Delight; `cuisine.js` and [0030](decisions/0030-farmers-delight-kitchen.md) to re-read |
| Punchy! | 2.8g | 3.2b | Version pinned on purpose: the 3.x menu and Mix Packs, and the Create and Aeronautics blacklist, to re-check |
| Amendments | 1.21-2.1.11 | 2.2.7 | Goes with the Supplementaries 4.x retest; its new pet teleport must stay off |
| Spelunkery | 0.4.4 | 0.4.5 | No changelog published; to compare with the jar first |
| JEI | 19.51 | 19.64 (release) | 19.55 and later need the Mezz Config library; goes with the Envelope retest |

## Open work items arising from the decisions

### Rules and configs (sub-project 3)
- [x] **Ecliptic Seasons → Cold Sweat bridge** (2026-11-17): the season of Ecliptic warms or cools the body, biome by biome, through the `fallow` mod's `climat` module rather than KubeJS ([climat](plan/climat.md#seasons-first-step)). Snow and sand storms followed (2026-11-18, [climat](plan/climat.md#storms-second-step)); hot and cool dishes followed (2026-11-18, [climat](plan/climat.md#hot-dishes-third-step)). Brumaire does not yet close its mist in a storm.
- [x] **Farm & Charm confined to the farm** (2026-11-15): its stove, cooking pot, roaster and crafting bowl leave the game; their recipes are copied to Farmer's Delight's cooking pot and to the workbench (`cuisine.js`); its tomato and onion are kept, Farmer's Delight's leave ([0030](decisions/0030-farmers-delight-kitchen.md)). Three ropes remain (Farmer's Delight, Farm & Charm, Supplementaries).
- [ ] Lock Create: Central Kitchen's automation behind progression ([0002](decisions/0002-agriculture.md), [0030](decisions/0030-farmers-delight-kitchen.md)).
- [ ] **"Survivor" villagers**: ruined villages, rare survivors to bring back, breeding blocked, trades filtered (no Mending, no bypassing Overgeared) ([0004](decisions/0004-structures.md)).
- [x] **Monument guardians**: Wroughtnaut, Frostmaw, Umvuthi and Sacred Pontiff at four monuments, blessing after their defeat (decision 15 of [12.5](archive/plan/12-verification.md#125-decisions-that-are-added-continuing-1012)); written on 2026-11-13 with the monuments' placement on the new terrain ([monuments](plan/monuments.md)), fights not seen in game.
- [ ] **The zoo**: a measurable long-term goal (species housed, aquariums), still without a mechanic.
- [ ] **Quests and guides** (Questlog): first version in the pack (five chapters, 26 quests, the forge basics included in « Premiers gestes »); next quest lines for the fanal, the monuments and cooking ([quetes](plan/quetes.md)).
- [ ] In Control!: `spawn.json` has no rule for villagers and wandering traders, unlike what the survivors plan assumes; to settle with the `survivants` module.
- [ ] **White moon**: draft in [kubejs-brouillons/](archive/kubejs-brouillons/), needs a sound and texts; the `ciel` module already draws it.
- [ ] Subtle Effects: turn off superfluous effects ([0007](decisions/0007-ambiance-performance.md)).
- [x] Remove hostile vanilla monsters from the Overworld; spawn table per biome; Friends & Foes, hamsters and Companions! tamed by config; End removed and Nether closed ([0003](decisions/0003-faune.md), [0004](decisions/0004-structures.md)). To check in game.
- [x] Clearings and the dissipator: module `clairiere` and `dissipateur` of the `fallow` mod 0.4.0 ([mod-fallow](plan/mod-fallow.md)).

### Custom `fallow` mod
- [ ] A Ponder scene for the dissipator.
- [x] Remove the `sac` module: done in 0.5.0 (Traveler's Backpack has left the pack, [0012](decisions/0012-sacs-piles-jours.md)).
- [ ] The eight modules approved on 2026-10-26, in this order: `voile`, `zoo`, `vie`, `fanal`, `eclaircie`, `sillage`, `echos`, `survivants` ([modules-fallow](plan/modules-fallow.md)). The zoo and survivor items above are now these modules.

### Pending research (sub-project 2)
- [ ] Demanding animal husbandry (needs, diseases, genetics): Animal Husbandry 0.6.0 to re-evaluate ([0002](decisions/0002-agriculture.md)).
- [ ] A mod that replaces vanilla structures with Create-integrated versions; candidates: Create: Structures Arise, Create Rustic Structures ([0004](decisions/0004-structures.md)).
- [ ] Separate Sable Render Distance, now that Distant Horizons is in ([0007](decisions/0007-ambiance-performance.md)).
- [x] ~~Food preservation~~: dropped by the team on 2026-10-18 (Freeze It and Heat It removed).
- [x] Terrain: Tectonic + Streams Reflowing; Distant Horizons driven by the mist ([0011](decisions/0011-tectonic-streams-distant-horizons.md)). Done on No Man's Land, which has since left; the biomes are now Regions Unexplored's and Geophilic's ([0019](decisions/0019-leaving-no-mans-land.md)).
- [x] **Compare Lithosphere in game** under the same biomes: done on 2026-11-13 with two test packs, Tectonic stays ([0019](decisions/0019-leaving-no-mans-land.md)).
- [ ] **Walk the new biomes**: neither Regions Unexplored nor Geophilic has been seen in game; their French names are paper choices.
- [ ] **Choose a new mist**: since [0024](decisions/0024-removing-the-house-resource-packs.md) the mist is ATMOSPHERICS' own preset, with no profile for the clearings, so lighting a clearing no longer shows on screen.
- [ ] Confirm the provisional vanilla recipes of the vigil fire and the clearing lantern, and recompute the life shard frequency with the vanilla sources ([sante](plan/sante.md)).

### Identity and world shape (arising from 0019)
- [ ] **The new main thread** of the pack, in place of the Friendly Moon: not decided.
- [ ] **The finite disc world**: whether to do it, and its radius: not decided.
- [ ] **The pack's new name**: not decided.

### Toward a public release (approved 2026-10-29)
Study of what makes Homestead and Matcha Flavoured popular, kept to what fits the vision. Ordered by weight; none adds a gauge, a biome or a crowd of structures.

In game:
- [ ] **The mist lifts early and in full view**: a first vigil fire within the first hour, led by the first quest; the mist recedes over 10 to 20 seconds, the far view appears, the clearing music starts.
- [ ] **Rebuild a hamlet**: the `survivants` module as a long-term goal; each survivor found in the mist joins a clearing with a trade, and the hamlet grows by milestones like the zoo (Matcha Flavoured: villages are made, not found).
- [ ] **Named quest chapters** in Questlog: 8 to 12 chapters (first fire, forge, zoo, sky, monuments, airship, hamlet, and the new main thread once defined), 150 to 250 quests; the first three in order, the rest free.
- [ ] **A collections page**: zoo species, Longwings butterflies, monument codex pages, lost letters, dishes; a carved figurine of a species per milestone, to place at home.
- [ ] **Dishes with a use**: a few Farmer's Delight dishes give an effect tied to the pack's systems (see further in the mist, warmth for Cold Sweat, thirst delayed, soft steps near wildlife). Hunger stays. Warmth and coolness are done (2026-11-18, [climat](plan/climat.md#hot-dishes-third-step)).
- [ ] **A few hand-made enigmas**: three to five unique places, never horror (a set table with warm soup and nobody, an observatory whose log stops), with hints in letters and `echos`.
- [ ] **Travel as a selling point**: a quest chapter for the atlas, horses, rails and airships; `fanal` lights along the roads.
- [ ] **Letters written by the community**: lost letters and travellers' notebook pages written and signed by players of the Discord, found in ruins.

Around the game:
- [ ] **English next to French**: French stays the source language and `check-fr` stays blocking; English is added for the custom content (quests, `fallow` mod, scripts).
- [ ] **A page and a short manifesto video**: one line of identity, before/after shots of the mist, Fallow's own numbers.
- [ ] **CurseForge and a light profile**: check mod by mod that each one is on CurseForge or may be redistributed; a light profile (shorter far view, fewer particles), recommended memory, a server pack.
- [ ] **Standalone modules**: publish the clearings and the dissipator (then `voile`) as their own mod on Modrinth, linking to the pack.
- [ ] **A stability promise**: an update never breaks a world; migration notes in plain words.

Not taken: hundreds of quests and kitchen-sink density, bundled shaders (Iris breaks Aeronautics), gear tiers past Netherite, plushies and traders everywhere, teleportation, horror.

### Repository housekeeping
- [ ] Delete the 34 merged branches left on GitHub (every branch except `main`; all their commits are in `main`). Only the owner can: the cloud environment gets a 403. "Automatically delete head branches" is now on, so new branches go away when their PR is merged.

### Watch list
- Sinytra Connector (beta) runs Antique Atlas 4 and Surveyor, two Fabric mods: keep an eye on it at every update.
- Steam 'n' Rails is an unofficial NeoForge port (beta).
- KubeJS Create, In Control! and Simple Backups are betas.
