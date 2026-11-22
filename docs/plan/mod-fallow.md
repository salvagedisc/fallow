# The custom `fallow` mod

The mod proposed in [12.4](../archive/plan/12-verification.md#124-the-custom-fallow-mod) and retained by decision 17 of [12.5](../archive/plan/12-verification.md#125-decisions-that-are-added-continuing-1012). It is made of modules that are enabled one by one. Version 0.31.0 contains the **ciel** (sky), **xp**, **horizon** (Distant Horizons), **clairiere** (clearings), **dissipateur** (dissipator), **outils** (tools), **monuments**, **bord** (world edge), **rendu** (shader link), **vent** (wind), **relief** (height of the world for the resource pack), **zoo** (stamps for every creature in Archie's guidebook), **patine** (armour that gets dirty, takes sand, snow and water, and washes off in water) **acier_noir** (black steel, between steel and diamond), **minage** (a well-forged pickaxe gives more ore), **filons** (ore veins with outcrops above them), **climat** (the season felt by the body, snow and sand storms, with Cold Sweat and Ecliptic Seasons) and **ecran** (the interface fades when the player does nothing) modules, plus [compatibility fixes](#compatibility-fixes) (Punchy! and Sensible Stackables, Streams Reflowing and Spelunkery, Lithostitched, Archie's Dinosaurs' Chorusoatl armour and armour models). The **titres** module (each screen's name on a plate drawn by the `fallow-interface` pack) left in 0.10.1 with that pack ([0024](../decisions/0024-removing-the-house-resource-packs.md)); the **averse** (rain, storm, leaves in the wind) and **rayons** (glints on water) modules left in 0.11.0, with their textures ([0026](../decisions/0026-second-cleanup.md)): rain, storms and foliage are those of the game and of Where Winds Blow.

> **Status as of 2026-10-26.** Version 0.4.1 compiles (`./gradlew build`, against Enhanced Celestials 2, Distant Horizons 3.3.3 and Create 6.0.10) and the full pack starts on a dedicated server with it; the clearing conversion and the dissipator were checked on that server with commands. **Nothing has been tested in game with a player**. Everything below comes from reading the code of the targeted mods (jars opened with `javap`). The list of [in-game tests](#in-game-tests) says what must be checked before relying on it.

## Building and updating

| What | Where |
|---|---|
| Sources | `mods-custom/fallow/`: official ModDevGradle 1.21.1 MDK, Gradle wrapper 9.2.1, Java 21 |
| Java package | `io.github.salvagedisc.fallow` |
| Jar in the pack | `pack/mods/fallow-<version>.jar`, a raw jar (no `.pw.toml`) |
| CI | `mod-fallow` job of `.github/workflows/build.yml`: it compiles the mod and keeps the jar as an artifact |

To release a version:

```sh
cd mods-custom/fallow
./gradlew build                     # jar in build/libs/fallow-<version>.jar
cp build/libs/fallow-<version>.jar ../../pack/mods/   # remove the old jar
cd ../../pack && packwiz refresh
```

The version is set in `gradle.properties` (`mod_version`). The targeted mods are used only at compile time (`compileOnly`, Modrinth Maven repository). `build.gradle` reads their version from `pack/mods/<slug>.pw.toml`: the mod is always compiled against the exact versions in the pack, and an update of one of these mods shows up at the next compilation.

## Principles

- **One module = one switch.** Each module has its `enabled` boolean in the config: `config/fallow-client.toml` for `ciel`, `config/fallow-common.toml` for most modules, and the world's `serverconfig/fallow-server.toml` for the switches the client must share with the server (`xp`, `outils`): NeoForge sends that file to every player who joins. The values are re-read at each use, so a change in game takes effect immediately. Exceptions: `dissipateur` follows the `clairiere` switch; `monuments`, `acier_noir`, `minage`, `filons` and `glanage` have none, because they register content (a structure type, items, a tool tier, a feature) that saved worlds and the pack's data name, or are pure data rules; turning them off would break a world rather than disable a feature.
- **A module loads only if its mods are present.** At startup, `Fallow` checks with `ModList` the mods each module depends on. It writes "Module X chargé" or "Module X ignoré" to the log. Mixins go through `FallowMixinPlugin`, which applies them only if the targeted mods are in `LoadingModList`. The only hard dependency is NeoForge.
- **No copied code.** The mod goes through the targeted mods' public APIs and through targeted mixins, on vanilla code when possible. Two exceptions: the recipe book of the `postes` module is taken from Hands-On Vanilla 1.2.2 (MIT, by Pocky), with its licence in `META-INF/LICENSE_handson_vanilla` and its notice at the top of each file.

| Module | Side | Required mods | Mixins |
|---|---|---|---|
| `ciel` | client | ATMOSPHERICS; Enhanced Celestials for the moon; Iris for the shader sky | 6, all client (4 from 0.7.0 to 0.26.0) |
| `xp` | both (the anvil on both sides, the bar on the client) | none | 1, common (`xp.AnvilMenuMixin`, on vanilla code) |
| `horizon` | both (the fog on the client, the Chunky fix on both sides) | Distant Horizons (Chunky for its fix, only if a server owner adds it) | 5: `horizon.FogRendererMixin`, `horizon.VanillaFadeMixin`, `horizon.FarFadeMixin` and `horizon.GLStateMixin` client, `horizon.ChunkyAccessorMixin` common |
| `clairiere` | both (the blocks and the list of clearings on the server, their look on the client) | none; ATMOSPHERICS, True Adaptive Music and Sodium each for its part of the look | 5, all client: `clairiere.ClientLevelTintMixin`, `clairiere.BiomeLookMixin` (vanilla code), `clairiere.SodiumTintMixin`, `clairiere.AtmosphericsBiomeMixin`, `clairiere.MusicBiomeMixin` |
| `dissipateur` | both | Create | none |
| `bord` | both (the edge on the server, the headwind applied by the client that moves the player or their vehicle) | none; Sable for the players aboard a ship | none |
| `navires` | both (everything happens on the server, except the sail on the helmsman's side) | Sable; Ecliptic Seasons for where it snows; Fish 'N' Ships for the sail (works without Sable) | 1, common (`navires.VoileMixin`) |
| `bac` | both (the ferry on the server, the rope drawn on the client) | Sable | none |
| `prise` | both (the fish and Sable ships on the server, entity boats on the helmsman's client) | Starcatcher; Sable for ships of blocks | none |
| `vent` | client | none | 2, client (`vent.CampfireBlockMixin`, `vent.CampfireSmokeParticleMixin`, on vanilla code) |
| `rendu` | client | Iris | 1, client (`rendu.IrisUniformsMixin`) |
| `zoo` | both (stamps and species list on the server, the guidebook pages on the client) | none for the stamps; Archie's Dinosaurs for the pages, Exposure for the photo stamp | 2, client: `zoo.GuidebookDataMixin`, `zoo.CategoryTabsMixin` |
| `ecran` | client | none; Thirst Was Reclaimed and Cold Sweat each for its gauge | none |
| `indices` | client | none | 1, client (`indices.GuiOverlayAccessor`, an accessor on vanilla code) |
| `hotes` | both (the emissaries and masks on the server, the dialogue window, masks and regalia on the client) | none; Archie's Dinosaurs for the beasts' armour | none |
| `postes` | both (the stations and their work on the server, the scenes and the book on the client) | Diegesis; Overgeared for the forge; Cold Sweat for the hearth | 3, client: `postes.MenuOuvertureMixin` (NeoForge) and `postes.MenuCreationMixin` (vanilla code) with Diegesis, `postes.ForgeRendererMixin` (vanilla code) with Overgeared too |
| (compatibility fix) | client | Punchy! and Sensible Stackables | 1, client (`compat.PunchyEquipMixin`) |
| (compatibility fix) | both | Streams Reflowing and Spelunkery | 1 (`compat.IceThawMixin`) |
| (compatibility fix) | both | Lithostitched (used by Regions Unexplored) | 1 (`compat.InStructureMixin`) |

## `ciel` module

### The mist without shader

Under Brumaire the mist and its clearings are the shader's. Without a shader pack (Apple Silicon Mac, K pressed, Iris absent) the mist is ATMOSPHERICS' own preset, which knows no clearing and stays light by day: lighting a vigil fire did not change it ([0041](../decisions/0041-gameplay-without-shader.md)). `PlainMist` takes ATMOSPHERICS' smoothed fog before `FogCache.smooth` (same `AtmosphericsFadeMixin` hook) and, only when Iris runs no shader pack, in the Overworld, with the camera in the open (the column's top under four blocks above it) and not in a fluid:

- outside clearings, the end of the fog is brought down to `mistDistance` (128 blocks by day), except in desert and badlands biomes (`c:is_desert`, `c:is_badlands`);
- in a clearing, it opens up to `clearingDistance` (480 blocks by day), from the share of lifted mist `ClearingClient.liftAt` gives (0 at the edge, 1 eight blocks inside, eased over 0.8 s), the same rule the `rendu` module hands the shader;
- at night both distances fall to 55 %; the start keeps ATMOSPHERICS' proportion; storms (`StormView`) and the slow clearing fade still apply after it. The far landscape of Distant Horizons follows, through the `horizon` module.

Config (`fallow-client.toml`, `[ciel]`): `mistWithoutShader` (true), `mistDistance` (128, 32 to 512), `clearingDistance` (480, 128 to 1,024).

### Enhanced Celestials' moon under ATMOSPHERICS

**The problem.** ATMOSPHERICS cancels `LevelRenderer.renderSky` (injection at the start) and redraws the whole sky. Its moon still uses the vanilla `moon_phases.png` texture, at size 20 and in white. Enhanced Celestials' mixins on `renderSky` therefore no longer run: the white moon, the blood moon and the other events keep the appearance of the vanilla moon. The white moon is also smaller (14) than the vanilla moon: redrawing it on top is not enough, ATMOSPHERICS' moon must be hidden first.

**The mechanism** (`MoonOverlay` and three mixins):

1. `LevelRendererMixin` wraps the call to `renderSky` in `renderLevel` (`@WrapOperation`). Just before the call, the module arms itself if the dimension has Enhanced Celestials lunar data.
2. The same mixin spots the first `Runnable.run()` of `renderSky`: this is the fog setup, at the start of the vanilla body. If it runs, ATMOSPHERICS has not taken over; Enhanced Celestials then draws its moon itself, and `fallow` touches nothing.
3. Otherwise, `RenderSystemMixin` replaces, during ATMOSPHERICS' sky only, the `moon_phases.png` texture with a transparent texture (`fallow:textures/environment/hidden_moon.png`). ATMOSPHERICS' moon quad no longer draws anything.
4. After `renderSky`, if the moon was indeed hidden, `fallow` draws the Enhanced Celestials one at the same place, with the same matrices:
   - the texture of the current event;
   - the size blended between the previous and the current event, as Enhanced Celestials does;
   - the color, through `ECWorldRenderer.changeMoonColor`, the function Enhanced Celestials calls in the vanilla sky (it also handles fading by rain);
   - the scale (`moonTextureScale`) and rotation (`moonTextureRotation`) set in the ATMOSPHERICS config.

If a step is missing (ATMOSPHERICS does not draw a moon, Enhanced Celestials absent…), nothing is hidden and nothing is drawn. A drawing error is logged once, without crashing the rendering.

Config: `ciel.redrawMoon` (true by default).

**Known limitations.**
- The redrawn moon comes after all of ATMOSPHERICS' sky, hence on top of its clouds or its sky veil if it draws any after the moon. To be checked in game.
- It is not drawn when it is below the horizon (time-based test, with a margin for large moons).

### Clearing ATMOSPHERICS' mist cache

ATMOSPHERICS computes the mist from the biomes around the player. It keeps the result in a cache (`BiomeBlendUtil`) as long as the player stays in the same 4-block cell. If the biomes change under a stationary player (conversion into a clearing, `/fillbiome`), the mist only reacts on the next movement.

- **Automatic**: `ClientPacketListenerMixin` clears the cache every time the server sends new biomes (`ClientboundChunksBiomesPacket`, the one `/fillbiome` produces). Config: `ciel.clearFogOnBiomeUpdate` (true by default).
- **By hand**: a public static method, usable later from KubeJS (client scripts):

  ```js
  Java.loadClass('io.github.salvagedisc.fallow.api.FallowSky').clearFogCache()
  ```

  It returns `true` if the cache was cleared, or scheduled to be cleared on the render thread. It returns `false` if ATMOSPHERICS is absent, if the module is off or if we are on a server. `FallowSky.isFogCacheAvailable()` answers the same question without clearing anything. KubeJS may require the class to be allowed in its class filter. A server script that wants to trigger the clearing on players' machines must send them a packet, for example with `NetworkEvents.dataReceived` on the client side.

The cache is cleared by `BiomeBlendUtil.resetCache()`, a private ATMOSPHERICS method, reached through a mixin accessor (`@Invoker`).

### Shader packs: the pack draws the sky again

**The problem** (seen on 2026-11-17 on the bench and by the showcase captures). ATMOSPHERICS cancels `renderSky` and draws its own dome. Under Iris with Brumaire, the pack paints the sky itself (deferred pass) and only keeps the stars and the moon of the game's sky; with ATMOSPHERICS' dome instead, the night had no stars and a hollow grey square in place of the moon, and depending on the order in which mods apply their mixins the whole sky could turn dark grey with stars at noon.

**The fix** (0.27.0). ATMOSPHERICS already steps aside when No Man's Land is loaded and its friendly moon is up (`Atmospherics.isNoMansLandLoaded` and `isFriendMoonActive`, read at the start of its sky). `ShaderSkyMixin` wraps the `renderSky` call in `renderLevel` and marks it when `IrisApi.isShaderPackInUse()` is true; during that call only, `AtmosphericsSkyGateMixin` makes both methods answer true. The vanilla sky then runs, Iris hands it to the pack, and Enhanced Celestials draws its own moon (`MoonOverlay` sees the vanilla body and stays out of it). Elsewhere (fog colour, sky colour) and without a shader pack, ATMOSPHERICS is unchanged. `require = 0`; applied only with ATMOSPHERICS and Iris.

**Checked** on the bench on 2026-11-17 (Brumaire, ATMOSPHERICS 2.6.5, Enhanced Celestials): at midnight, before the fix no stars and a hollow square moon off its place; after it, Brumaire's stars, the moon in its phase at its place with its halo. Noon and dawn unchanged. No mixin error in the log.

### The Friendly Moon under ATMOSPHERICS

Removed in 0.7.0. History: in the first in-game test (2026-10-28) the Friendly Moon of No Man's Land never appeared, because ATMOSPHERICS and that mod each waited for the other to start the sky. `fallow` 0.4.10 (2026-10-29) made the call itself (`FriendMoonRelay`, mixins `ciel.FriendMoonSkyMixin` and `ciel.AtmosphericsDreamMixin`, config key `ciel.friendMoon`), and it was checked in a development client.

No Man's Land left the pack on 2026-11-13 ([0019](../decisions/0019-leaving-no-mans-land.md)): `fallow` 0.7.0 removes `FriendMoonRelay`, both mixins and the `friendMoon` key. The module keeps the moon of Enhanced Celestials and the mist cache. The description of the mechanism is in the archived [friendly moon page](../archive/plan/lune-amie.md).

## `xp` module

Fallow no longer has experience ([xp.md](xp.md)). Orbs and point gains are disabled by `pack/kubejs/server_scripts/sans_xp.js`. The module does the two things KubeJS cannot do cleanly.

**The anvil without levels.** Vanilla only returns the item if `player.experienceLevel >= cost` and `cost > 0` (`AnvilMenu.mayPickup`). `AnvilMenuMixin` replaces, in that method only, the read of `experienceLevel` with `Integer.MAX_VALUE` (MixinExtras `@ModifyExpressionValue`). The rest of the computation does not change:

- the cost is still at least 1, and the limit of 40 set by `createResult` remains. It becomes **wear**: "Usure : n" (wear: n), then "Trop usé !" (too worn!) (vanilla keys `container.repair.cost` and `container.repair.expensive`, translated in `pack/kubejs/assets/minecraft/lang/`);
- the materials (ingots, sacrificed item) are still taken; `onTake` still removes `cost` levels, which does nothing at 0;
- Enhanced Celestials' blue moon, which divides the cost in `createResult`, therefore reduces wear.

The mixin applies on both sides: the client also refuses to take the item if it thinks the levels are missing, and it colors the cost red. Client and server must have the same version of the mod and the same `xp.enabled` value. No mod in the pack patches `mayPickup`.

**Enchantment merging (optional).** With `xp.blockEnchantMerge`, `XpModule` cancels `AnvilUpdateEvent` when the right-hand item carries enchantments (enchanted book or enchanted item): the anvil gives no result. On by default since 0.44.0 ([0045](../decisions/0045-awakening.md)): an enchantment comes only from the item's awakening.

**The hidden bar.** On the client, `XpClient` cancels the `VanillaGuiLayers.EXPERIENCE_BAR` and `EXPERIENCE_LEVEL` layers in `RenderGuiLayerEvent.Pre`. The horse jump meter (`JUMP_METER`) is another layer and stays. The space the bar left is taken back: `Gui.leftHeight` and `rightHeight`, reset to 39 at each frame, are set to 34 in the same event, just before the health and food layers read them. Hearts, armor, hunger, air and what mods stack above them with these fields (Thirst Was Reclaimed's thirst) come down 5 pixels, leaving one pixel above the hotbar's selection frame. Not while the jump meter is shown (on a horse), where everything keeps its vanilla height. Cold Sweat places its body temperature icon by itself: `pack/config/coldsweat/client.toml` moves it down by the same 5 pixels (`Body Temperature Icon Offset`, `Body Temperature Readout Offset`).

Config (`serverconfig/fallow-server.toml`, `[xp]` section): `enabled` (true), `blockEnchantMerge` (true since 0.44.0); `fallow-common.toml`, `[xp]` section: `hideBar` (true). The last two only matter if `enabled` is true. To roll back: `enabled = false` and delete `sans_xp.js`.

## `horizon` module

Decision [0011](../decisions/0011-tectonic-streams-distant-horizons.md). Distant Horizons (DH) 3.3.3, API 7.2.0, read with `javap`.

### The far landscape follows the mist

**The problem.** DH draws its LODs (simplified far terrain) after the normal terrain, then applies its own fog to them, in a full-screen pass. That fog is measured as a fraction of the LOD radius and knows nothing about ATMOSPHERICS. By default DH also pushes the vanilla fog out to infinity (`MixinFogRenderer`), which would erase the pack's mist: the shipped config sets `enableVanillaFog = true`.

**The mechanism.**
1. `FogRendererMixin` reads, at the end of `FogRenderer.setupFog` for the terrain (`FOG_TERRAIN`), the fog start and end left in `RenderSystem` (`TerrainFog`). Its priority (1500) places it after ATMOSPHERICS and DH at the same point: it reads the final mist, already smoothed over time by ATMOSPHERICS.
2. After DH's initialisation (`DhApiAfterDhInitEvent`), `HorizonClient` registers two DH events, if the DH API major version is 7.
3. `DhApiBeforeFogRenderEvent`, every frame: `HorizonFog` sets DH's far fog to linear, from `start / R` to `end / R` (R = LOD radius in blocks, read from DH's config), thickness 0 to 1. A LOD at distance d is therefore as fogged as a block of terrain at the same distance.
4. `DhApiBeforeRenderEvent`: when the mist ends before the edge of the normal terrain (render distance × 16 blocks), every LOD is beyond the end of the mist, so invisible; the module cancels the whole LOD rendering of the frame. In misty areas DH costs no GPU time.
5. `VanillaFadeMixin` skips, in those same frames, DH's fade between the normal terrain and the LODs (`ClientApi.renderFadeOpaque` and `renderFadeTransparent`). That fade blends the edge of the normal terrain with DH's LOD texture, which is only cleared and redrawn by the LOD rendering the module has just cancelled: it would blend in the last LOD image drawn, frozen, for example the clearing's mountains after walking back into the mist.
6. **The mist ring** (`FarFadeMixin`). A LOD drowned in the mist takes the mist colour, which is not the colour of the sky behind it: without more, the far mountains stay visible as mist-coloured silhouettes up to the end of the LOD radius (1,536 blocks), and the clearing has no horizon. DH already has a far fade that dissolves LODs into the sky, from 50 % to 90 % of its far clip plane. The mixin hands it the ring distance instead: the LODs dissolve into the sky from 55 % to 100 % of the end of the mist. The ring is also capped (`ringMaxDistance`, 512 blocks): even where the mist ends further away (a future preset), the landscape always closes in a ring. The far fog of step 3 uses the same capped end.
7. Underwater, in lava or powder snow, blinded or in darkness, the module does nothing: DH already drowns its LODs.

**What it gives with the `fallow_v3` preset.** Misty (end 52) and veiled (end 96) families: no LOD drawn. Clearings (end 500 by day, 260 at night): the landscape appears progressively beyond the normal terrain, then dissolves into the sky between 280 and 500 blocks by day (145 and 260 at night). The transition follows ATMOSPHERICS' smoothing when walking in or out of a clearing.

**Checked in game on 2026-10-29** (development client with only DH 3.3.3, ATMOSPHERICS and `fallow`, `fallow_v3` preset, render distance 12, LOD radius 48 chunks, software OpenGL): from a hill in a clearing, a mountain range about 700 blocks away was visible as a silhouette before the ring, and is gone with it; the landscape up to about 450 blocks stays visible and dissolves into the haze; same result at dusk with the 260-block night mist. The frozen-image case of step 5 comes from reading DH's code; it was not seen on that client.

**Fallback.** The shipped `DistantHorizons.toml` has `farFogMin = farFogMax = 1`: without the module (switched off, error, other API version) every LOD is fully in the fog. No regression: the pack looks as it did without DH.

Config (`fallow-client.toml`, `[horizon]` section): `enabled` (true), `skipHiddenLods` (true), `ringMaxDistance` (512 blocks, 64 to 4,096).

### Two fixes for DH 3.3.3

- **Crash when creating the world with Chunky** (`ChunkyAccessorMixin`, common). Chunky left the pack on 2026-11-14 ([0026](../decisions/0026-second-cleanup.md)); the fix stays and applies only if a server owner adds Chunky to the server. DH binds to Chunky's events on the first chunk loaded. On NeoForge the spawn chunks load before Chunky has started: `ChunkyProvider.get()` throws "Chunky is not loaded" and the server crashes during `createLevels` (observed on the test server, dedicated server; single player goes through the same code). The mixin catches the error and puts DH's `listenerBound` flag back to false: DH tries again on the next chunk, once Chunky is ready. Verified: with it, the full pack starts.
- **Native memory overflow on Apple Silicon** (`GLStateMixin`, client). `GLState.saveState` reads `GL_POLYGON_MODE` with `glGetInteger`, which reserves one integer; Apple's OpenGL driver writes two, which corrupts native memory and makes the JVM crash at random (DH issue #1346, fixed upstream after 3.3.3). The mixin reads that value into an array of two integers and keeps the first. Not verified on a Mac.

Both use `require = 0`: if a future DH no longer has the targeted code, they do nothing.

## `clairiere` module

Plan [10.3](../archive/plan/10-recentrage.md#103-the-mist-that-lifts), layer 3: the mist lifts where the players decide. It replaces the KubeJS drafts `clairieres.js` and `clairiere.json` of `kubejs-brouillons/`.

**Blocks.**

| Block | Radius | Recipe |
|---|---|---|
| Vigil Fire (`fallow:feu_de_veille`), a stone brazier on a foot with embers and a pale flame | 16 | A honeycomb, two smooth stone slabs, a charcoal, a smooth stone (provisional, to be confirmed by the team; before 0.7.0: a No Man's Land resin oil bottle, polished stone slabs, polished stone) |
| Clearing Lantern (`fallow:lanterne_de_clairiere`), a standing lantern of blackened iron around a glowing crystal | 32 | Two Overgeared iron plates, two glass panes, an amethyst shard (provisional, to be confirmed by the team; before 0.7.0: a No Man's Land quartzite cluster) |
| Dissipator (`fallow:dissipateur`, `dissipateur` module) | 64 | Encased fan, brass casing, propeller, two precision mechanisms |

Both have their own model and textures since 0.4.12 (`textures/block/feu_de_veille_*`, `lanterne_de_clairiere_*`; the flame is animated and, like the embers and the crystal, lit at full brightness through NeoForge's per-face light). Before, the fire was a cobblestone campfire and the lantern the vanilla lantern, and players mixed them up with those.

**A clearing does not touch the world** (0.8.0). Until 0.7.0 a clearing replaced the biome of its disc with one of three clearing biomes. Those have no tags and their own climate, so everything a mod decides from the biome changed with them: the rain and the crop humidity of Ecliptic Seasons, the temperature of Cold Sweat, the fauna, the atlas tile. Since 0.8.0 the biome stays the one the generator placed. The server only keeps the list of clearings (centre, radius, opening time) in the world's saved data (`fallow_clairieres`, `ClearingSources`) and sends it to the clients (`ClearingSync`: the whole list at login, then each opening and closing). F3, `/locate biome`, the seasons, the crops, the temperature and the animals all see the real biome.

**What a clearing changes.** Only the biomes of the `fallow:peut_devenir_clairiere` tag clear: land biomes and rivers, not oceans, beaches, deserts or caves, which keep their mist inside the disc. The tag lists 59 surface biomes of Regions Unexplored next to the vanilla ones; the tag `fallow:sans_outil` has `regions_unexplored:cattail` among the plants that need no tool.

- On the server, one rule: monsters do not spawn naturally in a clearing (`MobSpawnEvent.PositionCheck`, natural spawns of the monster category). Spawners, raids and monsters walking in are untouched.
- On the client (`ClearingClient`), what is seen and heard takes the look of a clearing biome, chosen from the temperature of the real biome: `fallow:clairiere_froide` below 0.3, `fallow:clairiere_chaude` above 1.0, `fallow:clairiere` otherwise. The three biomes still exist in the registry, but only as looks: nothing is generated or converted into them.
  - **Mist.** ATMOSPHERICS reads the biome around the player with `ClientLevel.getBiome`; `AtmosphericsBiomeMixin` hands it the look biome for the mist, the air haze, the weather and the horizon veil, so the `fallow_v3` clearing profile applies (40 → 500 blocks by day) and Distant Horizons shows the far landscape (`horizon` module). Its biome particles (desert dust, tumbleweeds) keep the real biome.
  - **Colours.** Where a block tint is computed (`ClientLevel.calculateBlockTint`, and Sodium's `LevelColorCache.updateColorBuffers`), the colour rule still receives the real biome, but for the time of the call its base grass, foliage and water colours are those of the look biome (`ClearingClient.tint`, a per-thread marker read by `BiomeLookMixin` in `Biome.getGrassColor`, `getFoliageColor` and `getWaterColor`). The seasonal tint Ecliptic Seasons adds from the real biome therefore stays: a clearing is vivid and still turns with the seasons of its biome. The hook is on the call and not on the three `BiomeColors` constants, because Ecliptic Seasons and Polytone both replace those. Distant Horizons colours its far terrain per biome, not per position: seen from far away, a clearing keeps the colours of its biome.
  - **Music.** `MusicBiomeMixin` gives True Adaptive Music the look biome, so node 8 of the [music](musique.md) plays as before.

**The mist comes back** (0.4.12, reworked in 0.8.0). When a fire or a lantern leaves the world (broken, blown up, replaced: `onRemove`), or a charged dissipator is broken (`destroy`, called by Create), its clearing is removed from the list and the clients close it. Where another clearing still covers the ground, nothing changes. The nearest player within 8 blocks reads "La brume revient."

**The transition** (0.4.12, client side since 0.8.0). A clearing does not appear at once: its radius grows ring by ring, 4 blocks every `ringTicks` ticks (6), from the centre outwards when the mist lifts, from the edge inwards when it comes back. A lantern (32 blocks) takes about 2.7 seconds, a dissipator (64 blocks) about 5. The wave is computed on each client from the game time of the opening or the closing, so a player who arrives in the middle of it sees it at the right place and nothing is lost if the server stops. Each ring redraws the chunks it crosses and sends a few cloud puffs (`CLOUD` particles, at most 160 per ring, within 160 blocks of the edge) that drift outwards when the mist leaves and inwards when it returns. `AtmosphericsFadeMixin` slows the fog distance for 15 seconds after each ring (`FogCache.slowFade`): the fog density (1 / end distance) moves towards its target at a bounded speed, so going from full mist to a clear sky takes `clearingFadeSeconds` (10; the visible mist goes in the first 4 or 5). Without it, ATMOSPHERICS fades the distance linearly in about a second and the mist seems to vanish in the first few per cent.

**Worlds made before 0.8.0** still hold clearing biome cells. Their sources are read from the same saved data (opening time 0), so the mist stays lifted there, and the old cells count as clear ground. `/fallow brume <radius>` gives them back to the biome the generator places there (`OldClearings`, `BiomeSource.getNoiseBiome` with the world's sampler, 2 chunks per tick); placing the fire or the lantern again then opens a clearing of the new kind.

Limits: a Create contraption that picks up a charged dissipator removes it without `destroy`, so its clearing stays. A player without the mod (or with the module off) sees no clearing.

Operators can open a clearing by command: `/fallow clairiere <radius>` around the position of the command (4 to 256), and give the mist back with `/fallow brume <radius>`, which closes the clearings whose centre is in the disc.

Config (`fallow-common.toml`, `[clairiere]` section): `enabled` (true; false keeps the blocks but they no longer open anything, and monsters spawn again), `ringTicks` (6, ticks between two rings; the server sends it to the clients). Client (`fallow-client.toml`, `[ciel]` section): `clearingFadeSeconds` (10; 0 gives back the ATMOSPHERICS fade).

## `dissipateur` module

The third tier, a real Create kinetic block (plan [12.4](../archive/plan/12-verification.md#124-the-custom-fallow-mod)). A brass casing crossed by a shaft on the axis chosen when placing it. It needs 32 RPM or more (`SpeedLevel.MEDIUM`) and consumes 8 stress units per RPM, shown by engineer's goggles. After ten seconds of continuous rotation without overstress it converts the disc of radius 64 around it, once, until it is broken; the goggles show the progress, then "The mist has lifted around it". It keeps blowing white mist while it turns. Broken, it gives the mist back to its disc; placed elsewhere, it charges again and converts where it is. No Ponder scene yet.

Verified on the test server on 2026-10-25: driven by a creative motor at 64 RPM it is part of the Create network (stress 512), charges, converts and keeps `FallowDone` after a restart (the check of that time, `execute if biome … fallow:clairiere`, no longer applies: since 0.8.0 the biome does not change).

## `outils` module

Server and client, no mod required ([first gestures](premiers-gestes.md)). With bare hands, you gather but you do not cut. A block needs a tool when it is in the `fallow:outil_requis` block tag (`#minecraft:logs` and `#minecraft:mineable/axe`, shipped in the jar) or when it would drop nothing without the correct tool (`BlockState.requiresCorrectToolForDrops()`: stone, ores, bricks, metal blocks). If the main-hand item is not correct for it (`ItemStack.isCorrectToolForDrops`), `PlayerEvent.BreakSpeed` sets the speed to 0 on both sides, so the client never predicts a break the server refuses, and `PlayerEvent.HarvestCheck` refuses the drops. On the client, a hint shows in the action bar at most every 40 ticks: « Il vous faut une hache / une pioche / une pelle / une houe / un outil », chosen from the vanilla `mineable/*` tags, or « Votre outil n'est pas assez solide » when the held item is of the right kind but too weak. The `fallow:sans_outil` tag keeps what grows (`#minecraft:sword_efficient`, bamboo, azaleas, a few mod plants) and snow breakable by hand. Creative players and `FakePlayer`s (Create deployers) are left alone. Both tags can be extended by datapack without recompiling.

Config (`serverconfig/fallow-server.toml`, `[outils]` section): `enabled` (true; false gives vanilla breaking back). The server sends it to the players, so a client can no longer disagree with it and stop mining altogether.

## `monuments` module

Client and server, no mod required. Registers the structure type `fallow:terrain_checked`: it wraps another structure and only places it where the ground suits it (slope, water, room under a floating base, ceiling of the world). The Ancient Remnants structures use it; rules, values and measurements are in [monuments](monuments.md). The type is always registered, because the pack's structure files name it and a world does not load if a type is missing. No config switch: the rules are in the structure files.

## `bord` module

Client and server, no mod required; the server decides, the client applies the headwind. The world is a disc ([0022](../decisions/0022-disc-world.md)) and this module keeps its edge.

- **The edge** is a circle in the Overworld, read at each data load from `data/fallow/monde/bord.json` (`center_x`, `center_z`, `radius`). [`scripts/terrain/disc.py`](../../scripts/terrain/disc.py) writes that file with the shape of the continent, so the two always agree. Without the file the module does nothing; the log says which case applies ("bord : cercle de rayon 6000.0 autour de 0.0, 0.0").
- **A headwind** ([0046](../decisions/0046-wind-on-ships.md), since 0.59.0). From 24 blocks before the circle the server adds speed towards the centre to the player, or to the root vehicle they ride (boat, horse, any mount): `0.03 × push` blocks per tick, where `push` (`BordModule.push`) grows with the square of the depth into the band to 1 at the edge, then by 3 per band width beyond, up to 4. Past the edge a share of the outward speed is taken away too (`min(0.5, 0.15 × push)`). A player's movement, and that of the boat or mount they lead, is computed by their client, and a motion sent by the server would replace the speed the client already has (a boat would stop dead at the start of the band). So since 0.66.0 the server sends `BordVent` (direction of the centre, `push`, distance past the edge) every tick in the band, and `BordClient` adds the gain (`BordModule.gain`) to the player on foot or to the root vehicle when this client controls it. Message every five seconds: « Un vent contraire se lève et vous repousse vers les terres. »
- **Safety net**: what is still 48 blocks past the edge (a very fast flight) is brought back two blocks inside, on the line to the centre, with its vehicle (`ClientboundMoveVehiclePacket`), and « Le vent du large vous rejette vers les terres. » A player aboard a Sable ship that is still 48 blocks past the edge (the `navires` module off, for example) is taken off it and brought back the same way (0.66.0).
- **64 blocks before the edge**, a message every five seconds: « La brume s'épaissit. Le monde s'arrête un peu plus loin. »
- **Nothing is drawn.** Beyond the edge there is only open sea, and a wall would break the mist.
- **Spectators pass.** Creative players do not.
- **Ships of Sable** (boats and airships of Create Aeronautics). Sable keeps the blocks of a ship in a plot grid far from the world, and a player seated aboard has coordinates there: `Ships` asks Sable for the real position (`SableCompanion.projectOutOfSubLevel`, found by reflection, no dependency). A player aboard a ship is not pushed: the `navires` module pushes the ship, and the player reads « Le vent du large repousse le navire vers les terres. »
- **Switch**: `bord.enabled` in `config/fallow-common.toml`.

**Checked** in the test client on 2026-11-13 (mod 0.10.0, full pack, a world whose new chunks follow the disc): the first message shows at 5,990; a player teleported to 6,010 is back at 5,998; a player swimming east from 5,994 ends at 5,998; a boat driven east from 5,990 for 14 seconds is at 5,998.4 with its rider still seated and the second message on screen; a spectator teleported to 6,010 stays there. The log says that Sable was found. Not checked: a horse, elytra, an airship (tests Z2 to Z4). This was the teleport of 0.10.0; the headwind (0.59.0, applied by the client since 0.66.0) is not checked in a client yet.

## `navires` module

Client and server, needs Sable; everything happens on the server ([0046](../decisions/0046-wind-on-ships.md), since 0.59.0). `NaviresModule` listens to Sable's physics step (`SableEventPlatform.onPhysicsTick`) and, for each ship of the Overworld (`ServerSubLevel`) at least 3 blocks long in one dimension for the wind and gusts (ragdoll bodies are smaller), and for every ship at the edge (since 0.66.0: a small raft could cross it before), adds an impulse at its centre of mass through Sable's `drag` force group, in the ship's own frame. The ship's speed is read from its rigid body (`getPhysicsHandle(ship).getLinearVelocity`) since 0.66.0: `latestLinearVelocity` is computed from the pose of the previous tick, so at the second substep of a tick it is about half the real speed, which made every speed term too weak (the same change in `bac` and `prise`). The maths is in `ShipForces`, tested without Sable.

- **Wind**: if the sky is open above the ship's box, the pack's background wind (`WindField.velocity`, 6 blocks/s at strength 1, the same as the `vent` module's without the gusts) blows on the face of the box it meets, above sea level only, at most 600 blocks². Force = `0.12 × windStrength × area × (wind − ship's horizontal speed)`: a ship running with the wind is no longer pushed, one sailing into it is held back.
- **Edge**: within 48 blocks of the circle of the `bord` module, an acceleration towards the centre of `3 × push` blocks/s² (the same curve as for players, over 48 blocks), plus `0.8 ×` the outward speed past the edge, at most 12 blocks/s². Follows `bord.enabled`.
- **Gusts** ([0049](../decisions/0049-weather-on-ships.md), since 0.63.0): in rain and thunder, `Gros.gust` gives each ship (seeded by its UUID) bursts of wind from the world time, up to 7 blocks/s × `0.3 rain + 0.7 thunder`, doubled at 210 blocks; the force (same coefficient and area as the wind) is applied 40 % of the ship's height above the centre of mass, so it heels.
- **Snow** (`Neige`, every 40 ticks): where it snows on an open ship (Ecliptic Seasons' `getRainOrSnow`, else the biome), 2 random columns of the plot (6 in a snowstorm) get one more snow layer on their highest block, up to `snowMaxLayers`; Sable weighs them. Sheltered, 6 columns lose a layer; in the open, 3, once it no longer snows and the biome is warm enough to rain. Columns outside the plot are never touched.
- **Sailboat of Fish 'N' Ships** (`mixin.navires.VoileMixin`, any side, needs Fish 'N' Ships only): the constant 0.03 of `SailboatEntity.moveWithSailDown` becomes `0.03 × Gros.sail(heading, wind) × clamp(windSpeed / 3, 0.5, 2.5) × windStrength`.
- **Settings**: `navires.wind` (on), `navires.windStrength` (1.0, 0 to 4), `navires.storms` (on), `navires.snow` (on), `navires.snowMaxLayers` (4, 1 to 8) and `navires.sailWind` (on) in `config/fallow-common.toml`.

## `bac` module

Client and server, needs Sable ([0050](../decisions/0050-rope-ferry.md), since 0.64.0). Two blocks, a block entity and Sable's physics step.

- **Ferry post** (`PieuBlock`, block entity `PieuBlockEntity`): a rope of the item tag `fallow:cordes_de_bac` used on a post remembers it in the player's persistent data, then on a second post of the same dimension within 96 blocks takes `ceil(length / 8)` ropes (from the inventory and the off hand) and links both. The first post is the *owner*: it keeps the cost, registers the line in `Cordes` when its chunk loads and draws it (`CordeRenderer`, two crossed ribbons sagging by 3 % of the length, texture `corde_de_bac`, seen from 128 blocks). Shift + click with an empty hand, or breaking either post, unlinks both and drops the ropes (Farmer's Delight's rope, else leads). A post moved with its data (assembled into a Sable ship, carried by a machine) unties the far post without giving ropes back; the copy keeps them, and is drawn and taken by a pulley only if the far post still points back to it (`PieuBlockEntity.tied`), since 0.66.0 (moving a post used to give the ropes twice).
- **Ferry pulley** (`PoulieBlock`, no block entity). Not on a ship: the nearest line within 4 blocks horizontally and from 2 below to 10 above is looked up; the raft is gathered from the pulley through every block that is not air, liquid, replaceable, shovel-mineable or overworld stone (`SubLevelAssemblyHelper.gatherConnectedBlocks`, 400 at most), must float (water under one of its blocks) and must not rest on anything (no block of it over ground that is not water, air or replaceable: a raft touching a pier would take the pier along; refused since 0.66.0 with « Le radeau touche la rive ou le fond… »), and is assembled (`assembleBlocks`). The line and the direction go into the ship's user data (`fallow_bac`: `a`, `b`, `sens`), which Sable saves with it. On a ship: a click starts towards the far bank or stops, Shift + click reverses.
- **Physics** (`Bac.onPhysicsTick`, maths in `Traille`): from the ship's centre of mass, a spring of 6/s² and damping 4/s across the rope (at most 15 blocks/s²), and along the rope `4 × (target − speed)` (at most 6 blocks/s²), target `1.4 + 1.5 × |current across the rope|` blocks/s (1.2 before 0.66.0, when the speed was misread), the current being the fluid flow under the raft × 1.5. Stop 2.5 blocks from a post; let go past 8 blocks off the rope or when the line is gone: since 0.66.0 the first post's block entity is read (a missing post, or one tied elsewhere), not the `Cordes` registry, which is only filled at the tick after its chunk loads and would let a raft go on every reload. `Cordes` is cleared when the server stops. Impulses go to Sable's `propulsion` force group.
- **Checked** on the full pack's dedicated server on 2026-11-21 (a temporary test scene, not kept): a 3 × 3 raft of planks with its pulley under a 48-block rope in still water was taken (mass 5.5), stayed within 0.15 block of the rope, crossed at 0.99 blocks/s, stopped against the far bank, and came back at 0.96 blocks/s once turned. Again after the fixes of 0.66.0: a raft resting on a pier planted on the bottom was refused, one over kelp was taken, and a 3 × 3 raft in still water crossed at 0.97 block/s.
- **Switch**: `bac.enabled` in `config/fallow-common.toml`.

## `prise` module

Client and server, needs Starcatcher ([0051](../decisions/0051-big-catch-tows.md), since 0.65.0); Sable for ships of blocks.

- **The fight** (`PriseModule`, server tick): for each player, the bobber named by Starcatcher's `fishing_bob` attachment, if it is a `FishingBobEntity` whose synced `STATE` is 4 (fight) and whose `fpToFish` weighs at least 8 kg (`sizeWeight().weightAverage()`, in grams). Each fight gets a run: its starting heading (from the boat to the bobber), a seed from the bobber's UUID and its start tick.
- **Maths** (`Prise`): heading = start ± 0.9 rad of slow noise (period about 4 s); strength 0.5 to 1.2 (period about 1.5 s); target speed `2.5 × strength × fish / (fish + boat)` blocks/s; acceleration `5 × (target − speed along the heading)`, between 0 and 4 blocks/s² (the fish never holds back).
- **Sable ship** (`PriseSable`, only loaded with Sable): the ship of `SableCompanion.getTrackingOrVehicleSubLevel(player)`, boat weight `40 × mass`. Only when water touches the bottom of the hull (under its middle or one of its corners, since 0.66.0): an airship or a stranded ship is not towed. The tow is queued for 2 ticks and applied in Sable's physics step at the centre of mass (propulsion force group).
- **Entity boat**: the player's vehicle if it is a boat (vanilla `Boat`, or Fish 'N' Ships' `BaseBoatEntity`; since 0.66.0, a swimming horse no longer counts) on water, boat weight `150 × width²`. Every 4 ticks `PriseTow` (heading, speed; optional payload) goes to the controlling passenger; `PriseClient` keeps it 8 ticks and adds the acceleration to the vehicle's motion when it is controlled locally.
- **Bobber**: moved by the boat's target speed along the heading while it stays within its line of the boat (since 0.66.0, its distance at the start of the fight plus 6 blocks, at most 40: it waits for a boat that lags) and water lies ahead; otherwise the fish turns by 90° to 180°. Splash particles every 5 ticks when the strength is over 1; « Le poisson vous emporte ! » (`message.fallow.prise.emporte`) once per fight when the target passes 0.4 block/s.
- **Checked** on the full pack's dedicated server on 2026-11-21 (a temporary test scene, not kept): with a 120 kg pull at full strength, a 2 × 3 raft of planks (mass 3) reached 0.88 block/s for a target of 1.25, a 6 × 8 raft (mass 24) 0.18 block/s for 0.28 (measured again with the true speed of 0.66.0). `RUNS` and the queued tows are cleared when the server stops.
- **Switch**: `prise.enabled` in `config/fallow-common.toml`.

## `vent` module

Client, no other mod. `Wind` is the pack's wind, ticked on the client: a direction that turns slowly and a strength that follows the weather (a breeze, stronger in rain and thunder, stronger with altitude), both computed from the world time so every player of a server sees the same wind; gust fronts cross the landscape around each camera, more often in bad weather. The shader pack reads it through the `rendu` module.

`Smoke` replaces the campfire smoke of `CampfireBlock.makeParticles` (client only): the same particle, led by the wind once it has risen, larger and longer-lived when several lit campfires stand within two blocks (their smokes draw together into one plume), bluish over a soul campfire; a block of Farm & Charm's `suppress_campfire_smoke_particles` tag above the fire still hides it. Campfire smoke sent by another mod, Sooty Chimneys' chimneys first, is taken by the same wind on its first tick, keeping its own look (since 0.68.0; Sooty Chimneys' own wind is off in `pack/config/sootychimneys-common.toml`). Built after the features of Better Smoke (MIT, Fabric only), without its code.

`WindMotes` (0.36.0): without a shader pack, nothing else shows the wind that carries the player's scent to the animals (`approche`). In the open on the Overworld surface, fine pale motes are born near the ground within 12 blocks of the camera and drift along `Wind.velocity`: about one every eight ticks in a calm, up to three a tick in strong wind, half as many under thunder, darker at night, none under a storm's own dust. Under Brumaire the swaying plants show it and no mote is made.

Switches in `config/fallow-client.toml`, `[vent]`: `enabled` (the smoke; the wind itself always runs), `motes` (true).

## `rendu` module

Client, needs Iris. It gives the house shader pack ([shader](shader.md)) what GLSL cannot read. `rendu.IrisUniformsMixin` adds, at the end of Iris' `CommonUniforms.generalCommonUniforms`, uniforms that any program of a shader pack may declare; `ShaderUniforms` computes them once per frame:

- `brumaireLink`: 1 when the module runs, so the shader knows the others are live;
- `brumaireMist`: x is the share of mist lifted around the camera (0 in the mist, 1 eight blocks inside a clearing, eased over 0.8 s); y is the height of the ground around the camera (world surface on five columns, never above the camera, eased over 2 s), where the shader lays the foot of its mist;
- `brumaireClearing0..3`: the four clearings whose edge is nearest (centre seen from the camera, radius with the opening wave, strength), only for biomes that clear;
- `brumaireWind` and `brumaireGust0..1`: direction, strength, turbulence and the two nearest gust fronts of the pack's wind (`vent` module);
- `brumairePush0..3`: the four living beings nearest the camera (feet seen from the camera, radius of grass they push aside), so the shader bends the grass under their steps.
- `brumaireWet`: how wet the feet of those four beings are, in the same order: 1 while they touch water, then less with every block walked (dry after 24) and every second spent out of it (dry after 90 s), so the shader leaves wet prints behind them.
- `brumaireStorm` (0.36.0): the strength of the sand storm (x) and the snow storm (y) seen from the camera, from `StormView` (`climat` module, zero without it), so Brumaire closes its mist in a storm as ATMOSPHERICS does without shader;
- `brumaireWake0..5` and `brumaireWakeSpeed0..1`: the six boats, swimmers and wading animals nearest the camera within 28 blocks that straddle a water surface (point of the surface under them seen from the camera, half width, speed over the water); a passenger does not count, its boat does. The shader ripples the water and leaves a wake behind them.

Switch: `rendu.enabled` in `config/fallow-client.toml`. Another shader pack never declares these uniforms and is not affected.

## `zoo` module

Client and server. Design and reasons in [modules-fallow](modules-fallow.md#zoo-the-stamps-of-archies-guidebook); this section is the reference for what the code does.

- **Stamps** (`Stamp`): `apercevoir` (spot), `photographier` (photograph), `heberger` (tame or house), `maitriser` (master). They are kept per player in the `fallow:zoo` data attachment (`ZooRecord`: a bit mask per species and the ticks of observation), copied on death, and sent to the client with the `fallow:zoo` payload (`ZooSync`: the whole list at login and after `/reload`, one species at a time after).
- **Spot and observe** (`Zoo.tick`, every 10 ticks): each `Mob` within `spotDistance` blocks, inside a 55° half-cone around the look direction and in line of sight, is spotted; within 16 blocks it also counts as observed. Any stamp also gives the spot stamp.
- **Photograph** (`ZooPhoto`, only when Exposure is present): each entity of Exposure's `FrameAddedEvent` (server side) is photographed by the player who took the picture.
- **Tame or house**: `AnimalTameEvent`, `BabyEntitySpawnEvent` caused by a player, or a water bucket used on a `Bucketable`. Only for species that are `OwnableEntity`, `Animal` or `Bucketable`; the others never show that stamp.
- **Master**: given as soon as the species has its other stamps and `observeSeconds` of observation.
- **Species** (`ZooSpecies`, at `ServerStartedEvent` and after each `/reload`): the mobs of the spawn settings of every Overworld biome, minus the entity type tags `fallow:zoo/absents` and `fallow:zoo/exclus`, plus `fallow:zoo/inclus` and the species found by sight outside the list (saved in the `fallow_zoo` world data). One sample of each type, made in the Overworld, gives its habitat: the tag `fallow:zoo/<terre|eaux|ciel|creatures>` if any, else `Enemy` → creatures, water mob (water animal or category, `IN_WATER` spawn placement, or water-only navigation) → eaux, `FlyingAnimal` or `AmbientCreature` → ciel, else terre. The tags are in `pack/kubejs/data/fallow/tags/entity_type/zoo/`.
- **Guidebook** (client, only with Archie's Dinosaurs): `zoo.GuidebookDataMixin` adds the `fallow_zoo` category before the credits when `GuidebookData.loadData` returns, and patches the parsed XML of Archie's pages (`GuidePatches`, from `assets/fallow/archie/guide/<language>.json`: page, label index and a word that must be in the original text, else nothing changes). `zoo.CategoryTabsMixin` adds the green bookmark at the bottom (the way back to the welcome spread) and one left tab per habitat; the J key opens the guidebook on the welcome spread. The pages are our widgets (`ZooIntroWidget`, `ZooSpreadWidget`) on Archie's UI classes; creatures are drawn with `InventoryScreen.renderEntityInInventory`, tinted with ink until spotted, after being measured once off screen (`Portraits`: drawn into a 256 x 256 target before the guidebook screen renders, alpha bounds read back) so each one is centred and sized to its frame; tooltips go through `Screen.setTooltipForNextRenderPass`, because a page is drawn into a buffer that would cut them. The images are composed at runtime by `ZooArt` from the loaded resources (Archie's guidebook sheets, Exposure's camera, hand-drawn `ZooGlyphs` signs for the habitat tabs) and registered as dynamic textures by `ZooTextures`; they are recomposed after a resource reload. Stamp icons are found by hue (whatever inside the diamond is not the bright habitat colour), lifted off a background rebuilt from the other stamps of the row, and set in the middle of the diamond; the grey camera takes, rank by brightness, the colours of Archie's grey icons. The habitat tabs carry hand-drawn signs (`ZooGlyphs`); the ribbons' fabric is rewoven as a dither of Archie's colours following his light-to-dark gradient; the eggs of Archie's own dinosaur tabs are recentred in a copy of his `markers` sheets registered in memory under his texture names (recomposed after a resource reload); a creature's scale also keeps every pixel inside its diamond frame (L1 reach measured off screen). Nothing of Archie's is stored in the repository. The layout (`ZooPage`) follows Archie's stamp page: banner at (87, 23), ribbon tops at 93/153/213 on the left page and 33/93/153/213 on the right, frame axis at x 94, stamps from x 117 every 38, right page shifted by 252.
- **Key**: "Ouvrir le guide d'Archie" (J by default, because Iris and KubeJS already use K; category Fallow) opens the guidebook without holding it.
- **Commands** (level 2): `/fallow zoo especes` (species per habitat), `/fallow zoo tamponner <players> <species> <stamp>`, `/fallow zoo effacer <players>`.

Config (`fallow-common.toml`, `[zoo]` section): `enabled` (true), `spotDistance` (25), `observeSeconds` (60).

**Checked** in the test client on 2026-11-15 (mod 0.17.0 with Archie's Dinosaurs, no other mod): 41 species (19 land, 8 water, 2 sky, 12 creatures); the bookmark opens the welcome spread, a habitat tab opens its species with silhouettes and the given stamps, tooltips show over the book; the Obsidiceratops page names the redstone caves. Not checked: the full pack's species list, the photo stamp with Exposure, taming, the bucket, mastery by observation (tests ZO1 to ZO6).

## `patine` module

Client and server, no other mod. Worn armour gets dirty and washes off in water, in the spirit of the reference look's washable armour weathering. The dirt is a component of the item, `fallow:patine` (0 to 100): it stays on the armour when it changes hands, goes into a chest or onto an armour stand.

- **Getting dirty** (`Patine`, server): every second, each worn armour piece has a chance to gain a point. The chance gives a full cuirass after `minutesToFull` minutes of walking (40), riding or flying count as walking; standing still adds nothing; sprinting ×1.5, walking on dirt, sand or soul sand ×1.6, a fight in the last ten seconds adds the equivalent of two walks. Boots get dirty 1.5 times faster, leggings 1.2, the helmet 0.7. Drawing lots instead of counting keeps no state per player, and each piece gets dirty at its own pace. Creative and spectator players are left alone.
- **Washing**: in water a piece loses 12 points per second (clean in under ten seconds), the helmet only head under water; rain takes off a point now and then.
- **Monsters** that spawn with armour wear it already dirty (25 to 90).
- **The look** (`PatineTextures`, client): three steps (from 12, 35 and 65). The texture that NeoForge gives to the armour layer (`ClientHooks.getArmorTexture`, `patine.ArmorTextureMixin`) is replaced by a soiled copy, made on demand from the original (the game's or a mod's) and kept until the next resource reload. Earth stains about three pixels wide with a pixel fringe, in two tones, spread a little more toward the bottom of the sheet (legs, boots); the same pattern grows from one step to the next and depends only on the texture name, so every player sees the same armour. On bare metal (bright, unsaturated, not leather) a few rust specks at the last step. Trims and the enchantment glint stay clean. Under Brumaire the soiled copy is lit like any armour.
- **Sand, snow and water** (0.28.0, component `fallow:coat`, three amounts from 0 to 100). Each second, for each worn piece:
  - *sand*: walking on sand (the `minecraft:sand` block tag, red sand included) adds 3 points a second to the boots, 1.8 to the leggings, 0.8 to the chestplate and 0.4 to the helmet, ×1.5 sprinting; off sand it falls off (0.4 a second standing, 1 walking, 3 in the rain);
  - *snow*: falling snow (raining in a snowy biome, sky above the head) adds 3 points a second to the helmet, 2.4 to the chestplate, 1 to the leggings and 0.6 to the boots; walking in snow adds 3 to the boots and 1 to the leggings; powder snow 6 to every piece (3 to the helmet). Standing in snow keeps it; out of it, it melts: 2 points a second, 4 in a hot biome, 0.3 in a cold one, 3 in the rain, at once when burning;
  - *water*: in water a piece is soaked (100) and loses 20 points of sand and snow a second; the rain wets it gradually; then it dries, 2 points a second (about fifty seconds), 4 in a hot biome, 1 in a cold one.
  Fractions are drawn by lot, like the earth. Monsters that spawn with armour on sand, or in a biome where it snows, also wear sand or snow.
- **The look of sand, snow and water** (`ArmorWear`, client, same soiled copy as the earth): sand is a pale grain in two tones, rising from the boots at three steps (10, 35, 65), with a fine dust over the whole armour at the last one; snow sits on the top of the helmet, the shoulders and the chest, spills over the top rows of their sides and packs around the bottom of the boots, with a blue shade under each clump for relief; water darkens the armour (more for leather) and puts bright drops with a short dark trickle under them on the sides, from 8 (damp) and 55 (soaked). Dyed leather keeps the earth and the darkening on its dyed layer; sand, snow and drops go on its undyed overlay layer, painted over the whole armour, otherwise the dye would turn them brown. The texture of each combination of steps is made on first use and kept until the next resource reload.
- **Tooltip**: « Un peu sale », « Sale » or « Couverte de boue », with « se lave dans l'eau »; « Ensablée », « Couverte de neige » and « Mouillée » when they show.

Config (`fallow-common.toml`, `[patine]` section): `enabled` (true; false stops the dirt and draws every armour clean), `minutesToFull` (40).

**Checked** on the test bench on 2026-11-15 (full pack, Brumaire on): iron armour on armour stands at 0, 20, 50 and 90 shows the three steps, rust at the last one; chainmail, diamond, gold, netherite and leather are soiled too; a player in survival moving for about two minutes gained 17 points on the boots and 10 on the chestplate, none while sitting on a pig that stands still; ten seconds in water took 90 points off every piece. Movement is read from the position one second earlier, because the server does not know the speed of a walking player. Not checked: a fight, the rain, a zombie spawned with armour, a modded armour (tests PA1 to PA5).

**Sand, snow and water checked** on the test bench on 2026-11-17 (Brumaire on): iron, leather and netherite armour on armour stands at sand 50 and 90, snow 50 and 90 and wet 100 show each look, the shadow under the snow clumps keeps it readable on bright iron, and dyed leather keeps white snow and pale sand; a player in survival walking about ten seconds on sand gained 21 points of sand on the boots, and on snow blocks 15 on the boots and 5 on the leggings. Not checked: falling snow, powder snow, the rain, drying in a hot biome, a monster spawned in a desert (tests PA6 to PA9).

## `acier_noir` module

Client and server, no other mod for the items; the recipes need Overgeared and Spelunkery. Black steel is the step between Overgeared's steel and diamond ([progression](progression.md#black-steel-second-step)). The module only registers what data cannot: items, the block, the tool tier and the armour material. Recipes, tags and heat are data of the pack (`kubejs/data/`).

- **Items** (`AcierNoirModule`): raw and heated raw black steel, heated and cold ingot, plate, block, four tool heads and a sword blade, pickaxe, axe, shovel, hoe, sword, and four armour pieces. The heated items are plain items in `overgeared:heated_metals`: Overgeared burns the hand that holds them without tongs and cools them in water.
- **Tier**: durability 900, speed 7.5, attack bonus 3, enchantability 12, repaired with an ingot. What it cannot mine is the tag `fallow:incorrect_for_black_steel_tool` (what needs a netherite tool or Spelunkery's compression blast miner).
- **Armour**: 3/7/6/3 (19), toughness 1.5 per piece, no knockback resistance, durability ×30 (chestplate 480), equip sound of iron. Overgeared's quality applies to every piece like to its own.
- **Textures** (provisional, to be approved by the team): Overgeared's steel textures (MIT, StirDrem) recoloured to a dark blue-grey; notice in `assets/fallow/textures/CREDITS.txt`.

**Checked** on the test bench on 2026-11-17 (full pack): the module loads; the 28 black steel recipes and the 10 Overgeared recipes that now take black steel are read without error; a deepslate diamond ore and Spelunkery's rough diamond block are in `overgeared:incorrect_for_steel_tool` and not in the black steel tag; obsidian is no longer refused to steel; ancient debris is refused to black steel. Not checked: the forging itself, which needs a player (tests AN1 to AN5).

## `minage` module

Server, no other mod; without Overgeared nothing changes. It registers one global loot modifier type, `fallow:rendement_de_la_pioche` (`RendementDeLaPioche`), and the mod's data turns it on (`data/fallow/loot_modifiers/rendement_de_la_pioche.json`, listed in NeoForge's `global_loot_modifiers.json`). When a block of `#c:ores` is broken with a tool that carries Overgeared's `overgeared:forging_quality`, each item of the drop gains one with the chance set for that quality in `chances` (expert 0.1, perfect 0.2, master 0.3; poor and well nothing). The component is read through the registry, so the mod does not compile against Overgeared. A drop that holds the ore block itself (silk touch) is left alone.

**Checked** on the test bench on 2026-11-17: 150 iron ores and 150 coal ores mined with `/loot ... mine` and an iron pickaxe: 1.00 item each without quality, 1.31 and 1.29 with master quality. Not checked: a player's pickaxe, Spelunkery's nugget drops (test PG10).

## `filons` module

Server-side generation, no other mod; the feature type is registered on both sides because the pack's data names it. It registers one feature type, `fallow:filon` (`FilonFeature`, `FilonConfig`), and the veins are data in `pack/kubejs/data/fallow/worldgen/` ([progression](progression.md#veins-and-outcrops-sixth-step)). A vein starts `profondeur` blocks under the ground at its chunk (never above 8 blocks under it, never below `y_min`), then winds for `longueur` steps, each a ball of `rayon`; inside, rock that matches one of `cibles` (the same rule tests as a vanilla ore) becomes ore with a chance of `densite` at the core, less at the edge, and `chance_brut` of it becomes the raw block. It stays within 12 blocks of its start horizontally so it never writes outside the chunks generation allows. If anything was placed, `affleurements` copies of `echantillon` (a Surface Samples block) are set on the ground within `etendue` blocks of the start, on a sturdy top face, in the open or under water (waterlogged), their rock matching the ground: red sandstone, sandstone or stone.

## `ecran` module

Client only, no other mod. The interface fades when the player does nothing, so the landscape fills the screen: after `idleSeconds` (6) without a gesture, the hotbar, health, food, armour, air and the gauges next to them fade out in under a second, and come back in a quarter of a second.

- **What brings it back** (`EcranClient`, every tick): a screen open (inventory, chat, a chest), the attack, use, pick, drop or swap keys held, another slot or another item in either hand (a tool wearing down does not count), a change of health, absorption, food, armour, thirst or mount, a hit taken.
- **What keeps it**: health at 30 % or less, food or thirst at 6 or less, head under water or air not full, a body temperature of ±50 or more on Cold Sweat's gauge (damage starts at ±100). Walking does not keep it unless `showWhileMoving` is on.
- **Layers**: only the NeoForge GUI layers listed in `layers` fade: the game's hotbar group (hotbar, jump meter, experience, health, armour, food, mount health, air, item name), Create's backtank air, Cold Sweat's body and vague temperature gauges. Thirst is drawn by Thirst Was Reclaimed inside the food layer and fades with it. The crosshair, chat, titles, action bar messages, effects, boss bars and every interactive overlay (Create's goggles and toolbox, Overgeared's anvil, Supplementaries' quiver) are not listed and never fade. The layers of the full pack were listed on the bench (55 layers).
- **The fade**: during the fade only, each listed layer is drawn into a separate image cleared to transparent, then laid on the screen with the current opacity (`ONE, ONE_MINUS_SRC_ALPHA`, the image holds colours already multiplied by their opacity). Lowering the shader colour alone is not enough: hotbar items are drawn opaque and would stay solid inside a half-faded bar. Outside the fade a layer is drawn as usual (opacity 1) or cancelled (opacity 0), so nothing costs anything at rest. The listener runs last (`LOWEST`): a layer another mod or the `xp` module cancels is left alone.
- **Mod gauges** (`EcranGauges`, by reflection, no dependency): thirst from Thirst Was Reclaimed's `thirst:player_thirst` attachment (`getThirst`), body temperature from Cold Sweat's `Overlays.BLEND_BODY_TEMP`, the value its gauge shows. A failed read logs a warning and drops that gauge.

Config (`fallow-client.toml`, `[ecran]` section): `enabled` (true; false: the game's interface, always shown), `idleSeconds` (6), `showWhileMoving` (false), `layers` (the list above).

**Checked** on the test bench on 2026-11-17 (full pack, Brumaire on): after a slot change the hotbar, hearts, food, thirst drops and Cold Sweat's gauge show, then fade together, items included, and are gone; the chat stays; a body temperature set to 70 kept the interface while the gauge read 56 and let it fade once it fell to 35; thirst read 17 to 18. The bench runs at a few frames per second, so the timing of the fades was not judged there (tests EC1 to EC5).

## `indices` module

Client only, no other mod. The action bar messages leave the space above the hotbar and become discreet notes in the left margin ([interactions](interactions.md#shared-conventions)): a thin gold rule, an icon for Fallow's own messages, the text in parchment colour over a dark shade that fades to the right. A note slides in from the left in 180 ms, stays long enough to be read (3 to 6.5 seconds, by length), then fades out in half a second. Up to three stack, the newest on top, from 40 % of the screen height down; a fourth sends the oldest away.

- **What is caught**: everything that reaches the game's action bar, from the server or from a mod on the client (about fifty mods of the pack write there: Create, Overgeared, Farmer's Delight, Cold Sweat, Starcatcher…), through `Gui.overlayMessageString` and `overlayMessageTime`, read every frame (`indices.GuiOverlayAccessor`); a message set again (its timer back to 60) counts as new. The game's `overlay_message` layer is cancelled while a note is shown.
- **Readouts stay in the bar** (`Routage`, tested without the game, since 0.39.0): a hint becomes a note, but a reading tied to what the player holds or rides stays above the hotbar, where the eye already is and where it updates every tick: Spelunkery's depth gauge and magnetite, Supplementaries' altimeter, compass, speedometer and cannon, the jukebox's « En cours de lecture », a mount's dismount key, Create's track and train placement, Better Combat's mining toggle, ItemPhysic's throw power, Sable's camera, Longwings' readings. A message stays there when its translation key starts with one of `inBar`, when it has no key (a text assembled on the fly: Icy's horses, Cold Sweat's thermometers), or when the same key comes back three times in two seconds; it turns back into a note after five quiet seconds. Fallow's own messages are always notes.
- **One note per message that changes**: a message with the same translation key as the newest note, or the same text once its numbers are removed, replaces it in place (Overgeared's « Coups restants : 4, 3, 2 » is one note); one already shown lower comes back to the top (`Notes`, tested without the game).
- **Icons** (`IndicesClient.icon`, vanilla items only): the tool hints show the tool they ask for, the approach a feather, rest a campfire, the tannery leather, clearings and the world edge a lantern, the zoo a spyglass. Other mods' messages have no icon.
- Hidden with the HUD (F1) and under the debug screen (F3); never faded by `ecran`. Lines wrap at 280 GUI pixels.

Config (`fallow-client.toml`, `[indices]` section): `enabled` (true; false: the game's action bar); `inBar` (the key prefixes of readouts kept in the bar, listed above).

**Checked** on the dev client on 2026-11-21 (the `fallow` mod alone, a data pack sending messages with `/title @a actionbar`): the tool hint, the approach hint and a drying frame reading stack with their icons; « Coups restants : 5, 4, 3 » stays one note; notes slide in, fade out, and nothing shows above the hotbar (tests IN6 to IN8).

## `climat` module

Server side (the integrated server in single player), with Cold Sweat (`cold_sweat`) and Ecliptic Seasons (`eclipticseasons`). The climate weighs on the body ([climat](climat.md)): Cold Sweat's body temperature follows Ecliptic's season.

- **The modifier** (`SaisonTempModifier`, `fallow:saison`) is registered on Cold Sweat's `TempModifierRegisterEvent` and given to every entity with a temperature on `DefaultTempModifiersEvent`, right after Cold Sweat's biome modifier, refreshed every 60 ticks for a player and 400 for an animal, as Cold Sweat does for Serene Seasons. It adds Ecliptic's temperature offset of the biome under the entity for the current solar term (`BiomeClimateManager.getBiomeClimateSettings(...).getTemperatureChange`), slid towards the next term over the term's days, times `seasonStrength`. It adds 0 where Ecliptic counts no season (`SolarDataManager.isValidDimension`).
- **Why not Cold Sweat's own seasonal settings.** Cold Sweat reads them only if a seasons mod answered its `FetchSeasonsModsEvent` before its world config was built, during mod construction; the order of construction is not guaranteed, and its curve (one start, middle and end value per season) is the same in every biome. Ecliptic's own offsets already differ by biome and term.

- **Storms** (`Storm`, `TempeteTempModifier` as `fallow:tempete`, `SoifDeSable`, `client.StormClient`, `client.StormView`): the same rule on both sides reads the world's rain and thunder and Ecliptic's precipitation for the biome (`EclipticUtil.getRainOrSnow`): snow, snow storm (snow and thunder), or sand storm (no precipitation, no rain in the biome's own data, base temperature 1.0 or more). `fallow:tempete` sits after `fallow:saison`, every 20 ticks for a player and 200 for an animal: cold of falling snow, plus snow settled on the clothes (kept in the modifier's NBT). With Thirst Was Reclaimed (`thirst`), `SoifDeSable` adds to the `thirst:player_thirst` attachment's exhaustion every second in the open in a sand storm (`IThirst.setExhaustion`, not `addExhaustion`, whose factor runs away with a cold body). On the client, `StormClient` eases a sand and a snow strength, spawns dust (`DustParticleOptions`) and flakes (`SNOWFLAKE`) along `vent.Wind`, and `StormView`, which depends on no other mod, closes and tints ATMOSPHERICS' smoothed fog from the `ciel` module's `AtmosphericsFadeMixin`, after the clearing fade.

Config (`fallow-common.toml`, `[climat]` section): `enabled` (true), `seasonStrength` (1.5), `snowCold` (0.12), `snowWet` (0.2), `sandThirst` (0.15); `fallow-client.toml`, `[climat]`: `stormVisuals` (true).

**Checked** on the bench on 2026-11-17 (development server, Cold Sweat 2.4.3.1, Ecliptic Seasons 0.15.5, one player): see [climat](climat.md#seasons-first-step).

## `approche` module

Server side (the integrated server in single player), no other mod. Shy wild animals smell, see and hear the player and flee ([approche](approche.md)).

- **The wind** (`vent.WindField`, common): the direction and strength of the pack's background wind, a function of the world time, rain, thunder and height only. `vent.Wind` (client) eases toward it and adds the gusts; the server reads it directly, so both agree with no packet.
- **The animals** (`ApprocheModule`): every `PathfinderMob` whose type is in `fallow:farouches` gets a `FuiteGoal` at priority 1 when it joins a server level. `Approche.wild` leaves out leashed, ridden, AI-less animals and any with an owner (`OwnableEntity`) or a tamed horse.
- **The rule** (`Approche`, every 10 ticks): for each player in survival or adventure, the animals of the tag within reach (1.3 × the longest range) test hearing, then scent, then sight with a line of sight (`perceives`). A first notice from afar sets an alert of 30 ticks (the goal stops the animal and turns its head to the player); a notice that persists after it, or one within 1.5 × the hearing range, starts a flight of 140 ticks, renewed while the animal still perceives the player, and spreads to the same species within 12 blocks. The state lives in a weak map on the server thread; nothing is saved.
- **The goal** (`FuiteGoal`, flags MOVE and LOOK): during the alert, navigation stopped and the look on the player; during the flight, a position 20 blocks away from where the player was, chosen again each second once reached, at 1.5 × speed.
- **Hints**: action bar keys `approche.fallow.indice.{odeur,vue,ouie}`, once per player every 90 s.
- **Spyglass** (`zoo.Zoo`): while the player scopes, the spot distance becomes `spyglassDistance` and the view cone 8°.

Config (`fallow-common.toml`): `[approche]` `enabled` (true), `scentRange` (32), `sightRange` (14), `hearingRange` (6), `hints` (true); `[zoo]` `spyglassDistance` (64).

**Checked** on the bench on 2026-11-18 (full pack, one player, Naturalist deer): see [approche](approche.md#checked-on-the-bench).

## `repos` module

Client and server (the effect is registered on both sides), no other mod. Rest by the hearth ([repos](repos.md)).

- **The effect** (`ReposeEffect`, `fallow:repose`, beneficial, ember orange, icon `textures/mob_effect/repose.png`): every 120 ticks it heals 1 point if food is at 6 or more. `mixin.repos.PlayerExhaustionMixin` multiplies `Player.causeFoodExhaustion` by 0.75 while the player has it.
- **The rule** (`Repos`, every 40 ticks for each player): still (under 0.6 blocks since the last check) or a passenger, `canSeeSky` false above the eyes, a block of `fallow:foyers` within 5 blocks and 2 up or down, lit if it has a `lit` property. After `restSeconds` the duration is `baseMinutes` + `minutesPerComfort` × the families found by `Confort.count` (17 × 9 × 17 blocks, stopped early once all are found); the effect is renewed whenever it has fallen 4 seconds below that. An action bar message the first time in each rest.
- **Comfort** (`Confort`): nine block tags `fallow:confort/*`, a bit per family.

Config (`fallow-common.toml`, `[repos]`): `enabled` (true), `restSeconds` (20), `baseMinutes` (2.0), `minutesPerComfort` (1.5).

**Checked** on the bench on 2026-11-18: see [repos](repos.md).

## `tannerie` module

Client and server (blocks and items are registered on both sides), no other mod. The house tannery ([tannery](tannerie.md), [0038](../decisions/0038-house-tannery.md)).

- **Items**: `peau_brute`, `peau_raclee`, `cuir_mouille`; **blocks** (axe, wood): `chevalet`, `cuve_a_tan`, `sechoir`, all their state in block states (no block entity).
- **`ChevaletBlock`** (`facing`, `peau`, `raclage` 0 to 15): a raw hide lays on it; each right-click with an item of `c:tools/knife` wears it by one and counts a stroke; at `strokes` the scraped hide pops up, with Naturalist's `fat` at `fatChance`.
- **`CuveBlock`** (`eau`, `tanin` 0 to 8, `peaux` 0 to 4, `tannage` 0 to 5): water bucket in and out, one item of `fallow:tanins` per level, a scraped hide needs `tanin` > `peaux`; a scheduled tick every `tanningMinutes` / 5 moves `tannage`; at 5 an empty hand takes out the wet leather and uses up that much `tanin`.
- **`SechoirBlock`** (`facing`, `etat` vide/humide/sec, `sechage` 0 to 15): a scheduled tick every 400 ticks adds `rate`: −1 under rain with the sky above, otherwise 2 by day under the sky or 1, +2 with a lit `fallow:foyers` block within 4 blocks, −1 where the biome is cold enough to snow or its precipitation is snow (Ecliptic Seasons' winter). Dry at `dryingPoints`.
- **Bark** (`TannerieModule.onStrip`, `BlockToolModificationEvent`, server, not simulated, `AXE_STRIP` that changes the block and is not in `fallow:sans_ecorce`): Farmer's Delight's `tree_bark` at `barkChance`.

Config (`fallow-common.toml`, `[tannerie]`): `enabled` (true), `strokes` (6), `tanningMinutes` (5.0), `dryingPoints` (15), `barkChance` (0.5), `fatChance` (0.33).

**Checked** on the bench on 2026-11-18: see [tannery](tannerie.md).

## `postes` module

Client and server, needs Diegesis (the library of Hands-On Vanilla and Hands-On Delight, by Pocky). Fallow's own blocks and the pack's other mods' blocks worked by hand in the world, on the model of those two mods, so that every station plays the same ([interactions](interactions.md)): the three blocks of the tannery ([tannery](tannerie.md#at-the-station)), Overgeared's smithing anvils and Cold Sweat's hearth. Farm & Charm's silo and mincer have no window to replace: they are already worked by hand in the world and stay as they are.

- **`PostesTannerie`**: one Diegesis station per block (`fallow:chevalet`, `fallow:cuve_a_tan`, `fallow:sechoir`), no slots of its own (empty schema), opened by a right-click unless crouching (the block then keeps its usual right-click) or when `[postes] tannerie` is off.
- **`Ouvrage`** (in `tannerie`): the work of each block, shared by the right-click in the world and the station: `travailler` (an item works on the block; what replaces it and what comes out are handed back) and `mainVide` (take what is finished, or read how far it is).
- **`OuvrageLogic`** (server): three custom actions. `travailler` takes the item on the cursor, or from inventory slot `depuis`; what comes out goes to the inventory, or to the ground when it is full. `main_vide` is the empty hand. `garnir` fills the block from the inventory, for the page picked in the book: the beam a raw hide, the frame a wet leather, the vat its water if missing, then one bark and one scraped hide (with `tout`, Shift: up to four).
- **Client** (`postes.client`): the scenes in `assets/fallow/diegesis/scenes/` (camera, the block's clickable surface, the book's place), `TannerieBehavior` (the cursor laid on the block, Shift + click in the inventory, the empty hand on the block, coming to the block with a useful item in hand makes the first gesture once the camera has arrived, the inventory greys what the tannery does not use), `TannerieBook` (three pages: scraping, tanning, drying; the page of the block where one is; a page of another block says where it is done). While a station is open the action bar is hidden, so the blocks' messages become the scene's caption.
- **How the stations look and move** (0.46.0, after a study of Hands-On Vanilla and Hands-On Delight): the three tannery stations hide their block (`hidesBlock`) and the scene draws a copy of it (`visual.block`, `lit_as_block`) with the parts that change as their own nodes. Each block has an `Allure` (`CuveAllure`, `ChevaletAllure`, `SechoirAllure`) that `TannerieBehavior` calls every frame, every tick and on each gesture, and that reads the block's state (no new server data):
  - **Vat**: a Diegesis tank node (`eau`, schema tank of 100) shows the water as a liquid (`fallow:eau_de_cuve`, registered on the client) whose colour goes from the biome's water to tan with the bark and the soak, agitated while the hides soak, with bark floating (`Floater.SHRED`/`LEAF`, one per bark not yet used) and bubbles; up to four hides (`peau_0` to `peau_3`, models `diegesis/peau_raclee` and `diegesis/cuir_mouille`) drop in with a splash, tint towards leather as they soak and bob; a ripple or a splash on each change, a glint and three chimes when the vat is ready.
  - **Fleshing beam**: the hide on the beam (`diegesis/peau_chevalet_brute`) with the scraped one over it (`diegesis/peau_chevalet_raclee`), more opaque stroke after stroke; while a knife is on the cursor and a hide on the beam, the station hides the cursor stack (`setCarriedHidden`) and draws the knife on the hide where the cursor points; a click strokes it towards the player with scraps (`DUST`), a shake and a shear sound.
  - **Drying frame**: the wet leather (`diegesis/cuir_tendu_humide`) with the dry one over it (`diegesis/cuir_tendu_sec`), more opaque and 4 % smaller as it dries; drops fall from its lower edge (fewer as it dries), steam rises in the sun or by a fire; its drying and its cause are the surface's tooltip; a glint and a leather sound when it is dry; dry, it glints every 1.8 s and the leather is outlined while the empty hand hovers the surface (0.53.0).
  - **What goes next**: a faint ghost of the next item: on the empty beam and frame, the hide or leather itself as a translucent model whose alpha pulses (`fantome`); a knife over a hide waiting for one (`fantome_couteau`); water, bark or a scraped hide over the vat. Ghost items are scaled to Hands-On's measures (about 3.5 to 4.5 px).
  - **Progress** (0.50.0, `Jauge`, after Hands-On Delight's pot badge): a bar 8 px by 1 (node `jauge`, painted with `textures/misc/white.png` in `entityCutoutNoCull`, full bright) over the vat's far rim while the hides soak and over the frame while the leather dries, filling from one colour to the next, grown in as it appears and easing towards the block's step; the vat's water (`surface`) has the soaking in its tooltip (`tannerie.fallow.cuve.trempe`).
  - **Arrivals and refusals** (0.49.0, `Arrivee`, after Hands-On Vanilla's `StationHands.fly`, MIT): what reaches a block without the cursor (Shift + click, recorded by `Arrivee.depuis`; the book's `garnir`) flies from its hotbar slot (`screenOn` at the bottom of the screen) to the block in 0.32 s with an arc of 2.5 px and `ITEM_FRAME_ADD_ITEM` rising in pitch, drawn by `arrivee_0` to `arrivee_2` taken in turn; what was dropped from the cursor (`Arrivee.parLeCurseur`, half a second) does not fly, nor do arrivals for players who only watch the station. Since 0.51.0 a refusal is known at once from the block's own rules (`CuveBlock.accepte`, `ChevaletBlock.accepte`, `SechoirBlock.accepte`, the same conditions as `travailler`): the block's copy and what it carries shake with `WOOD_HIT`, and the server's caption says why. While the cursor holds something the block takes, the block is outlined in warm cream (`setOutlined`, `outline_color` `#FFFFE8C0`; Diegesis draws outlines only with `hover_outline` on, so the three tannery scenes turn it on).
  - **Final check** (0.55.0): the first gesture on arrival and Shift + click go through `TannerieBehavior.montre` (refusal or `Allure.geste`), the item in hand flies from its hotbar slot (`Arrivee.depuis`); `Allure.gesteAutre` plays another player's gesture (the beam draws their knife, plays no sound and swings no arm); the beam's stroke sound is `BRUSH_GENERIC`, played by the server with the worker excluded and by the worker's client (station or world); the outline needs the cursor on `surface`; `Arrivee.Volee` queues arrivals and starts them 0.06 s apart on its three nodes; no fuel flight when the hearth is full of that fuel; no refusal shown for a knife stroke the server dropped.
  - **EMI** (0.56.0): Hands-On Vanilla gates its EMI integration (side panel, recipe and usage keys, the slide of the viewer) on `StationHover.isOurs(screen)`, a namespace check; `mixin/postes/StationHoverMixin` (`@Pseudo`, needs Diegesis and Hands-On Vanilla) also answers true when the focused station's type is in the `fallow` namespace.
  - **Loops** (0.54.0, `Son.boucle`, `loopSound` called every frame): the vat bubbles (`BUBBLE_COLUMN_UPWARDS_AMBIENT`, 0.08) while the hides soak; the hearth's fire roars (`FIRE_AMBIENT`, 0.2 to 0.5 with the hot fuel) while it burns.
  - **Seen by others** (0.52.0): after each item worked, `OuvrageLogic` plays the effect `fallow:geste` (`objet`, `pris` = the block state changed, `acteur`) for everyone who sees the station; `TannerieBehavior.onEffect` plays the gesture (`Allure.geste`; the beam draws the other player's knife for its stroke) or the refusal for watchers, and for the worker only a refusal it had not predicted. In the world, `eau_de_cuve` and `jus_de_tan` are 16-frame interpolated animations, and `SechoirBlock.animateTick` adds `WHITE_SMOKE` over wet leather by a fire or in the sun (the fire search one time in four).
  - **Vat water and strokes** (0.51.0): bucketed out, the water stays drawn 1.2 s so its level eases down; the pale hint over the vat fades while the cursor holds it. Knife clicks faster than a stroke wait their turn (up to 8) and are played one after the other.
  - **Ready leather** (0.48.0): once the vat is ready, the leather (count = hides) floats over the water (`pret`, tilted 50° to face the camera, painted by `Flottant`); a click on it does what an empty hand on the surface does.
  - **What comes out** (0.47.0, `Envol`): when a hide or leather leaves a block, a hidden `envol` node draws it at its place in the scene, lifts it 3 px and shrinks it away in 0.25 s; Diegesis's own `gains` then fly it to its hotbar slot with « +1 » (a flight of our own to the slot would double the library's).
  - **Blocks** (redrawn in the vanilla style after the owner's reviews, last in 0.72.0): every part sits on whole pixels, nothing is rotated and nothing leaves the block, and every face uses vanilla textures at one texel per pixel, grain along the piece. The vat is a barrel tub: two runners, a floor, walls 1 px thick (1 to 2 and 14 to 15) with `barrel_side` outside (its staves and hoops) and `spruce_planks` inside, `barrel_bottom` underneath, and a 2 px `stripped_spruce_log` rim at 15 to 16; its inner volume is unchanged (inside 2 to 14, floor at 2). The fleshing beam is a straight 4 × 3 `spruce_log` beam (x 6 to 10, y 10 to 13, the whole block long) on two posts and two runners; the hide's top is at 13 and the knife lies on it (`ChevaletAllure.DESSUS`). The drying frame stands upright in the middle of the block (posts at z 7 to 9 on runners, the lower rail on the ground, `stripped_spruce_log`), its shape 8 px deep; the leather is a plane at z 8 between the posts and rails (`SechoirAllure.DEVANT`, `SechoirBlock.surCuir` for the world particles). Hides and leather use the items' own textures on flat cutout planes, 0.01 off the wood they lie on: on the beam the middle four columns of the sprite on top and its sides folded down 4 px, their ragged edge hanging 1 px below the beam, the raw hide held by two 1 px straps of dark leather (a texel of `minecraft:item/leather`); in the frame the whole sprite, 12 × 14, between posts and rails; in the vat `peau_raclee` and `cuir_mouille` lie flat on the liquid, 12 × 12, their two ends sunk under the walls, and at the station the hides are stacked a quarter turn apart. Since 0.69.0 the three item textures are vanilla leather recoloured, shade for shade (raw hide brown, scraped hide cream, wet leather dark); since 0.72.0 Alex's Mobs' kangaroo hide and Koopa's Critters' reptile hide are too, in their mods' own colours (`pack/kubejs/assets`), and the reptile hide's model, which pointed at a missing texture, is replaced. The forge hammer's head keeps `marteau_tete`.
- **Recipe book** (`RecipeBook`, `RecipeBookModel`, `BookTabs`, `IconLight`, `BookMath`): taken from Hands-On Vanilla 1.2.2 (MIT), its own textures under `textures/entity/livre_de_recettes*.png`; it remembers its page in `config/fallow-livres.properties`.
- **The forge** (`PostesForge`, client only, as Hands-On Vanilla's stations): two menu stations bound to Overgeared's own menus, so the server sees the usual anvil window. `fallow:enclume_de_pierre` (the stone anvil: hammer, nine cells, result at menu slots 0, 1 to 9, 10) and `fallow:enclume` (the steel, tier A and tier B anvils: hammer, blueprint, cells, result at 0, 1, 2 to 10, 11; Overgeared's `enableBlueprintForging` must stay on). Scenes `enclume.json` and `enclume_de_pierre.json`: the grid on the middle of the anvil's top (the square both directions of the anvil share, as the station turns to the player), the hammer and the result floating to the right, the blueprint to the left behind the book. The station opens on a right-click, not crouching, and with the hammer in hand only while the grid is empty: crouch + use with the hammer still starts Overgeared's strikes, and use with the hammer strikes. `ForgeBook`: the forging recipes of the anvil's tier or lower, by Overgeared's tabs (tool heads, armour, misc), with their grid, strikes and blueprint; a page picked lays one piece per cell from the inventory with plain slot clicks, and says what is missing. `ForgeBehavior`: a ring when a piece lands (and a hiss when it is hot) and a slide when it leaves, with Hands-On Vanilla's anvil sounds; hot pieces (Overgeared's `hot_items` and `heated_metals` tags) smoke in their cell; what the grid will make turns faintly where the result will come (node `fera`), with a glint and a chime when a recipe forms; the pieces a chosen page still lacks show faintly in their empty cells (since 0.50.0, nodes `manque_0` to `manque_8`, `ForgeBook.attendus()`, forgotten when the piece is forged or the station closes); a right-click on the anvil with a forging piece in hand (not the hammer) lays it once the camera has arrived (`quickMoveFromInventory`); pieces laid without the cursor (Shift + click, the book) fly from the hotbar to their cell (since 0.49.0, `Arrivee.caseDepuisLaBarre`: the cell's node itself hops in; a drop in the cursor count marks a piece laid by hand); the forged piece arrives in a shower of sparks and floats over its place (`ouvrage`, a plain node with a collider painted by `Flottant`, adapted from Hands-On Vanilla's `HoveringResult`, MIT: grows in, bobs and sways, swells and lights up when hovered, the item's tooltip); a click on it plays Hands-On Vanilla's anvil strike (since 0.48.0): the hammer (`coup`, model `diegesis/marteau_de_forge`, anchored at its grip) hops in from its place, which empties meanwhile, rises, strikes the middle of the anvil, and the real click on the result slot (menu slot 10 on the stone anvil, 11 on the others; Shift = quick move) leaves at the blow, with sparks, `ANVIL_USE` and the piece rising 3 px off the anvil for 0.3 s (`leve`; on the stone anvil, since 0.51.0, dust, `STONE_HIT` and a few sparks only from a hot piece, and `STONE_HIT` when a piece lands); a click that cannot take it (another item in hand) shakes the piece with a dull ring; the caption « <result> : accroupissez-vous, le marteau en main, et frappez l'enclume. » when the grid makes a recipe, kept as the tooltip of the result's place until the piece is forged (0.51.0), the inventory greying what does not forge here.
- **The hearth** (`PostesFoyer`, client only): one menu station, `fallow:foyer`, on Cold Sweat's `hearth_bottom`, bound to Cold Sweat's own `cold_sweat:hearth` menu (its one fuel slot, menu slot 0). Scene `foyer.json`: the fuel slot stands in front of the grate, with faint coal, lava, ice and snowball; the two fuel gauges of Cold Sweat's window, hot on the left and cold on the right seen from the front, are painted flat on the brick above the grate with Cold Sweat's own gauge textures, their share in a tooltip (`FoyerBehavior`, read from the hearth each tick), sliding to a new share. The grate's slits glow (node `braises`, painted): bright orange while the hearth is lit, dull red while hot fuel only smoulders, pale blue when the cold fuel dominates; small flames, crackles and some smoke follow the hot fuel, a frosty breath falls from the grate when it cools; fuel going in throws sparks, cold fuel a puff of snow. Fuel shift-clicked in flies from the hotbar to the grate (0.51.0: the hearth burns it at once, so the flight starts on the Shift + click, drawn by `arrivee_0` to `arrivee_2`, and the sparks or the snow wait for it to land; a rise of the raw fuel, not of the percentage, triggers them). The station opens on a right-click, not crouching, unless the item in hand is a hearth fuel: that one is still thrown into the hearth by the click, Cold Sweat's gesture. A right-click on the hearth's top (its chimney) aims at the bottom, so it opens the station too, except with a smokestack in hand. The inventory greys what neither burns nor cools here (potions and milk pass: the hearth spreads them). Cold Sweat's switch for the hearth's smoke particles stays in its window only.
- **Menus with extra data and Diegesis**: Overgeared (its anvils) and Cold Sweat (its hearth) open their menus through NeoForge's menu with extra data (the block's position), which Diegesis does not hear (beta.14 in the pack, still so in beta.15): it waits for the vanilla open packet. `MenuOuvertureMixin` replays that NeoForge payload as the vanilla packet for these five menus only, and `MenuCreationMixin` makes the menu with its extra data while it is created, by Diegesis or by the vanilla screen. `ForgeRendererMixin` stops Overgeared's drawing of the pieces stacked on the anvil while the scene shows them in their cells.

Config (`fallow-common.toml`, `[postes]`): `tannerie` (true), `forge` (true), `foyer` (true). Client (`fallow-client.toml`, `[postes]`, 0.51.0): `soundVolume` (1.0), scaling every station sound (`Son.joue`) and the books' sounds; the blocks' own world sounds do not follow it.

**Checked** on the bench on 2026-11-21: each of the three tannery stations, by the cursor, by the book (Shift on the vat) and coming with the item in hand; the caption of the frame. The steel anvil: shears laid from the book, the caption, crouch + hammer started the strikes, three strikes forged them, taken from the station; the stone anvil opens with its four pages and takes the hammer in its slot. The hearth: coal and ice thrown by the click as before, the station opened with bread in hand and from the chimney, a coal stack laid in the slot raised the hot gauge to 94 %, shown in its tooltip; the forge station still opens.

## `glanage` module

Client and server, no mod required. What the first day finds without anything laid on the ground ([premiers-gestes](premiers-gestes.md#gestures-nothing-on-the-ground), [0052](../decisions/0052-gestures-not-debris.md)). `GlanageModule` listens to `PlayerInteractEvent.RightClickBlock`: main hand empty or holding a stick (`#c:rods/wooden`), not crouching, on `#minecraft:leaves`, it takes the click (the arm swings on the client) and, on the server, plays the leaves' break sound softly and sends a few leaf particles; then, if that block has not been shaken usefully in the last game day (24,000 ticks, remembered per dimension in memory, forgotten on restart), a stick pops out of the clicked face with a 30 % chance. The rocks found when digging are block loot set by the pack (`premiers_gestes.js`).

**Checked** on the bench client on 2026-11-22: 30 leaf blocks shaken one by one gave 9 sticks, all picked up, the first landing in the hand and the next shakes made with it; shaking the same blocks again gave nothing.

The `debris` module of 0.61.0 (ground rocks, fallen wood and pine cones as blocks, [0048](../decisions/0048-ground-debris.md)) was removed in 0.67.0 with its blocks, models and world generation.

## `hotes` module

Client and server, no required mod (the emissaries wear Archie's Dinosaurs' armour when it is there). The six masked peoples and their emissaries, a pilot ([hotes](hotes.md)): the `fallow:hote` entity, one mask helmet per people, the dialogue window with its portrait, the worn masks and regalia drawn from `assets/fallow/hotes/`. Emissaries come only through `/fallow hotes appeler <peuple>` (operators).

**Checked** on the bench client on 2026-11-21: see [hotes](hotes.md).

## `eveil` module

Client and server, no required mod (Overgeared's forging quality and TreeChop's chops are read when present). Enchanting as a deed ([eveil](eveil.md), [0045](../decisions/0045-awakening.md)): the components `fallow:eveil` (the item's id, places, awakenings, ready and faded states) and `fallow:eclat`, the item `fallow:eclat_de_bete`, the deed files `data/*/eveil/*.json` (reload listener `HautsFaits`), the deeds kept by the world (`Actes`, `data/fallow_eveil.dat`), the loot modifiers `fallow:sans_enchantement` (every table) and `fallow:eclat_de_bete` (seven `eclat_*` files), the trade filter, the enchanting table taken over (`RightClickBlock`, high priority, always cancelled) and its one-row choice window built on a server-side `ChestMenu`. Config `[eveil]` in `fallow-server.toml`.

Since 0.58.0: deed entries take conditions (`si`, read against a `HautsFaits.Contexte`: player, creature, damage source, block underfoot) and a new deed `reparer`; a bow's copy in an arrow is traced back to the real bow (`Eveil.reel`); Mending mends by deeds (`raccommodage`), there being no experience; the component carries a quarter `palier` for the tooltip; once a second `EveilModule.inventaire` strips what no awakening gave (`Eveil.purifier`, enchanted books to books, not in creative), re-ids copies, counts repairs and warns at `alerte` wear; `LivingDropsEvent` strips creature equipment and makes a shard glow; `GrindstoneEvent.OnPlaceItem` refuses awakened items; `PlayerDestroyItemEvent`, `ItemExpireEvent` and a killed item entity drop the deeds (`Actes.oublier`). The shard is tinted per beast (`Eclat.couleur`, layer 0; layer 1 untinted highlights) and carries a `recit` key.

Since 0.60.0, with Diegesis: the enchanting table is a station, `fallow:table_d_eveil` (`TableEveil`, `PLAYER_FREE`, exclusive, hides the block), with two slots `objet` and `eclat` (one each, returned on leaving) and server logic that writes the pages into the station data (`etat`, `voies` with `ne_de` and `fois`, `faits`, `avancee`, `eclat_etat`, a `cle` that changes with each new page) and takes the actions `choisir`, `eclat` and `poser`; the effect `fallow:eveil` plays the awakening for everyone watching. `onTable` no longer cancels the right-click when the station opens (not crouching, `[eveil] poste`). On the client `TableBehavior` draws the book (`OpenBook` and `AwakeningBookModel`, ported from Hands-On Vanilla, MIT) gliding from the table's book to the reader, writes the pages, lights the hovered path and streams glyph particles (`fallow:glyphe_*`, `fallow:glyphe_volant_*`, vanilla's `sga_*` letters); `TableClient` registers it and turns the table's overlay messages into scene captions; the mixin `eveil.LivreDeTableMixin` stops vanilla's floating book from being drawn while the scene draws it. `Actes` also counts how many times each deed happened (keys `#…`, outside the points), for the book.

Since 0.62.0, the rite: the station opens only for a player carrying a ready item or a shard (`TableEveil.eveille`, in the open condition; `onTable` answers otherwise with the `eveil.fallow.table.muette` overlay). `TableBehavior` darkens the room (`obscurite`, drawn by `TableClient.assombrir` on `ScreenEvent.Render.Pre` with vanilla's vignette), loops the beacon's and the portal's ambient sounds, sends runes from the bookshelves the table counts (`EnchantingTableBlock.BOOKSHELF_OFFSETS`), writes paths and shard powers in the alt font until they are read (`Contenu.revele`, `runique`), and chooses by a held click (`tenir`, the left button read through GLFW; `accomplir` sends `choisir` or `eclat` and sets the white `eclair`). Counts and places are words and signs, not numbers.

Since 0.70.0, awakening ink: the item `fallow:encre_d_eveil` (recipe `data/fallow/recipe/encre_d_eveil.json`), a third slot `encre` (up to 16) on the station, the `encre` scene node between the item and the shard; `TableEveil.encrer` takes a vial for `choisir` and `eclat`, `EveilModule.prendreEncre` for the crouching window and the offhand shard. On the client the paths are laid out by `texteDeVoie` and `lignes` (cut on the French text, written in runes line by line) so the hover band fits them, the reveal needs ink and draws `fallow:volute_d_encre` particles from the inkwell, and the awakening throws `glyphe_volant` runes and `fallow:etincelle_d_eveil` sparks.

**Checked** on the test bench on 2026-11-21: the full pack's dedicated server starts with the module (15 families of deeds) and no new error. In a development client: a new iron pickaxe counts one point per stone block; with `/fallow eveil points 3000` it shimmers and the note shows; at the table the window shows Unbreaking I and Fortune I, the same two after closing and reopening; a click gives Fortune I with the tooltip line « day 1 » and « Awakenings: 1 of 1 »; a second right-click says the item has given all it could; set to its last point of durability it forgets Fortune I (grey note) and its points fall back to 0; a Frostmaw shard is refused on a sword and gives Frost Walker II to iron boots, then is used up; a stronghold library, a desert temple and an ancient city chest give plain books and an unenchanted hoe and leggings; four novice librarians and two novice weaponsmiths offer no enchanted book or sword. Not checked: Overgeared quality and places, a real kill of a great beast, the anvil, deeds of combat, armour, fishing and TreeChop (tests EV5 to EV9 in [eveil](eveil.md)).

## Compatibility fixes

Fixes between two mods of the pack, outside any module: they have no switch and apply as soon as both mods are present. The fix for the inverted bell message of No Man's Land (`compat.InvertedBellMessageMixin`, 0.4.x) was removed in 0.7.0 with the mod.

### Punchy! and Sensible Stackables: the hand that kept jumping

**Symptom** (reported on 2026-10-29): with most items in hand, the first-person hand went down and up again without end and the item never showed. Tools, weapons and armour (items with durability) were not affected.

**Cause.** Punchy! plays its "hand out, hand in" animation when the held item changes. To decide, `HandEquipStateMachine.hasItemChanged` walks the vanilla components of the new stack and compares each value with the one `get` returns on the previous stack. Sensible Stackables gives each item the pack's stack size by wrapping `PatchedDataComponentMap.get` for `minecraft:max_stack_size`, but walking the components still yields the item's base value. For stone, the walk gives 64 and `get` gives 32 (decision 0012): the item "changes" every tick and Punchy! restarts the animation every tick. Items whose stack size the pack does not change (the `fallow:pile/64` tag, items with durability) never differ, hence the tools that stayed steady.

**Fix.** `compat.PunchyEquipMixin` makes Punchy!'s `shouldIgnoreComponentForEquipDiff` also ignore `minecraft:max_stack_size`, as it already ignores wear (`minecraft:damage`). A stack size never changes what the hand holds, so no real item change is missed. Applied only with Punchy! (`punchy`) and Sensible Stackables (`sensible_stackables`).

**Checked** in a development client with only Punchy! 2.8g, Sensible Stackables and their libraries, with the pack's configs: before the fix, a probe logging every tick showed `max_stack_size` read as 64 by the walk and 32 by `get` with stone in hand, and the hand stayed lowered and moving with no stone drawn; after it, the stone is drawn and only Punchy!'s light idle sway remains. An iron sword was steady in both cases.

### Streams Reflowing and Spelunkery: the ice of a stream

**Symptom** (test server log, 2026-11-13): `Mixin apply for mod streamsreflowing failed streamsreflowing.mixins.json:IceMeltMixin`, as soon as Spelunkery is in the pack.

**Cause.** When ice melts, Streams Reflowing replaces the water that `IceBlock.melt` places with stream water where a stream runs (`StreamIceMelt.meltsInto`), so that a stream frozen by winter flows again in spring. Spelunkery's `IceMixin` rewrites `melt` in full (it also lets salt melt ice). Mixin does not let a mixin inject into a method another mixin of the same priority has merged, and both have the default priority: Streams Reflowing's hook was dropped and the ice of a stream thawed into still water.

**Fix** (0.9.1). `compat.IceThawMixin`, priority 1100, wraps the same `setBlockAndUpdate` call inside the method Spelunkery merged and asks Streams Reflowing's own `StreamIceMelt.meltsInto` (looked up by reflection) which water to place. Salt still melts ice. `require = 0`; applied only with Streams Reflowing (`streamsreflowing`) and Spelunkery (`spelunkery`).

**Checked** on the test server: the debug log shows `Mixing compat.IceThawMixin from fallow.mixins.json into net.minecraft.world.level.block.IceBlock` with no error after it. The thaw itself has not been seen in game (test W11). If Streams Reflowing renames `StreamIceMelt`, the log says "Dégel des ruisseaux : règle de Streams Reflowing introuvable" and ice melts into plain water.

### Lithostitched and Regions Unexplored: generation that stalls

**Symptom.** A Chunky pregeneration slows to one or two chunks per second, or stops for minutes, with no error in the log. Seen four times on the terrain bench on 2026-11-12 and again on the full pack on 2026-11-13 (stopped at 32 % of 10,201 chunks).

**Cause.** Eight cave decorations of Regions Unexplored 0.6.3 (lava deltas and falls, dropleaves, cave bioshrooms, redstone buds and bulbs, ash vents, prismarite clusters) are filtered by the block predicate `lithostitched:in_structure`, up to a hundred times per chunk. In Lithostitched 1.8.1 that predicate reads the neighbouring chunks and their structures from the server level (`ServerLevel.getChunk`, then `ServerLevel.structureManager()`), while it runs on the world generation thread: each read is handed to the server thread and waited for. When the server thread is busy, which is the case during a pregeneration (it spends its spare time in `ChunkMap.processUnloads`), it answers about one read per tick. The thread dump shows `Worker-Main` parked in `ServerChunkCache.getChunk`, called from `InStructurePredicate.test`. Nothing here is specific to Chunky: a player exploring cave biomes can meet the same slowdown.

**Fix** (0.9.2). `compat.InStructureMixin` answers the predicate from the region being generated when there is one (`WorldGenRegion.getChunk`, as the game does for its own features): the structure references of the chunk and of its direct neighbours and the structure starts up to eight chunks away are already there, and no thread is waited for. Outside generation (a command, bone meal) the original code runs. `require = 0`; applied only with Lithostitched.

**Checked** on the test server, same world and same request (square of radius 800, 10,201 chunks, full pack): without the fix the task stopped at 3,259 chunks; with it the 10,201 chunks were generated in 13 min 06 s at a steady 12 to 15 chunks per second, with no error in the log. The debug log shows `Mixing compat.InStructureMixin from fallow.mixins.json into dev.worldgen.lithostitched.worldgen.blockpredicate.InStructurePredicate`. A structure start nine chunks away, seen from a neighbouring chunk, is outside the region and is left out: a first version read it and stopped the generation with "Requested chunk unavailable during world generation".

### Archie's Dinosaurs: one armour model per layer

`dinos.ArmorModelCacheMixin` (needs Archie's Dinosaurs, client): `ADArmorItem.initArmorModel`, which the mod calls every time a piece of its armour is drawn, baked the whole armour layer again each time: four new models per wearer per frame, garbage a moment later, and Entity Model Features gave up on the model after 64 creations ("model attempted creation more than 64 times"). The mixin keeps one model per `ModelLayerLocation` and returns it. Sharing is safe: NeoForge copies the pose and part visibility of the wearer's model onto the custom model before each draw, as vanilla does with its shared armour models. The cache is dropped when a resource reload replaces the layer definitions (`dinos.EntityModelSetAccessor` reads `EntityModelSet.roots`). Checked on the test bench on 2026-11-20: the armour renders as before and the EMF warning no longer appears.

### Archie's Dinosaurs: the Chorusoatl armour no longer teleports

`dinos.ChorusoatlArmorMixin` (needs Archie's Dinosaurs): `ChorusoatlArmorItem.hasAbility` answers false, so the armour key gives the mod's "no ability" message, and `activateServerSetAbility` does nothing in case a client sends the request anyway. A 45-block jump every 15 seconds would erase the heights that the set is meant to open ([progression](progression.md#dinosaur-armour-third-step)). Slow falling stays. The tooltip text is replaced in the pack (`kubejs/assets/ec_ad/lang/`). Checked on the test bench on 2026-11-17: the mixin applies without error; the key itself needs a player (PG7).

## In-game tests

None of this has been done. These tests are also gathered with those of the other pages, in play-session order, in [tests-en-jeu.md](../tests-en-jeu.md).

### Sky

| # | Test | Expected |
|---|---|---|
| C1 | Normal clear night, in the Overworld | A single moon, in its place, with the right phase. Same with a scale and rotation changed in the ATMOSPHERICS config |
| C2 | `/setLunarEvent` to the white moon (may require a clear sky) | The white moon visible, smaller, without the vanilla moon behind it |
| C3 | Blood moon, blue moon, harvest moon, super moon (size 40) | Color and size specific to each event, and fade at the transition |
| C4 | Rain, then thunderstorm | The moon fades with the rain, like the rest of the sky |
| C5 | Underwater, in powder snow, with blindness | No double moon and no vanishing moon |
| C7 | `ciel.redrawMoon = false`, then `ciel.enabled = false` | Back to ATMOSPHERICS' vanilla moon |
| C8 | `/fillbiome` around a stationary player, with `clearFogOnBiomeUpdate` true then false | The mist changes immediately, then only after a step |
| C9 | KubeJS call of `FallowSky.clearFogCache()` | Returns `true`, the mist is recomputed |
| C10 | Log at startup | "Module ciel chargé", no `fallow` mixin warning |
| C11 | Frames per second at night, with and without the module | No visible difference |
| C12 | Instance without Enhanced Celestials, then without ATMOSPHERICS | The game starts; moon mixins not applied, or module ignored |
| C13 | Brumaire on, clear night then noon; then shaders off (Iris menu) | With Brumaire: stars, the moon in its phase, a blue sky at noon. Shaders off: ATMOSPHERICS' sky as before |

### XP

| # | Test | Expected |
|---|---|---|
| X1 | Log at startup | "Module xp chargé (aucun mod requis)", no `fallow` mixin warning |
| X2 | At level 0: naming a tag, repairing a pickaxe with ingots, merging two enchanted pickaxes | Cost in green ("Usure : n"), the item can be taken, the materials are consumed |
| X3 | Repairing the same item up to wear 40 | "Trop usé !", no result (except in creative) |
| X4 | Dedicated server | The client takes the item without the slot rolling back |
| X5 | `xp.enabled = false` | The anvil asks for levels again, the bar returns |
| X6 | `xp.blockEnchantMerge = true` (default), enchanted item on the right | No result; a repair with ingots still works |
| X7 | On foot, on horseback, aiming a Supplementaries cannon | No XP bar or number; the jump meter stays on horseback |
| X8 | On foot, with thirst and Cold Sweat shown; then on a horse | On foot, hearts and hunger sit just above the hotbar, thirst above hunger, Cold Sweat's icon between them, nothing overlaps; on a horse, the bars go back up above the jump meter |
| X8 | Enhanced Celestials blue moon | The displayed cost goes down |

### Horizon

| # | Test | Expected |
|---|---|---|
| H1 | Log at startup, client and dedicated server | "Module horizon chargé", then on the client "horizon : brume lointaine de Distant Horizons 3.3.3 reliée"; no `fallow` mixin warning; the server reaches "Done" |
| H2 | Misty forest, then veiled plain, F3 | The mist looks as without DH; no LOD visible, even from a summit |
| H3 | `/fillbiome` a clearing (or `/locate biome` once clearings exist), stand in it | The far landscape appears beyond 128 blocks, without a seam at the edge of the terrain |
| H4 | Walk from the mist into a clearing and back | Visibility changes smoothly, no popping |
| H5 | Sunrise and sunset in a clearing | LOD fog colour follows the sky (DH reads the world fog colour) |
| H6 | Underwater, blindness, powder snow | No sharp LOD in the distance |
| H7 | `horizon.enabled = false` | All LODs hidden (shipped config) |
| H8 | `skipHiddenLods` true then false, in the mist, F3 | Same image; fewer FPS with false |
| H9 | ATMOSPHERICS clouds in a clearing | Clouds drawn over the LODs, no double clouds |
| H10 | Mac Apple Silicon, 1 hour of play with DH | No JVM crash (`hs_err_pid*.log`) |
| H11 | Only if a server owner adds Chunky: Chunky `start` on the server, then a player exploring | DH pauses its generation during Chunky and resumes; no holes in the LODs (DH #607) |
| H12 | Seasons (Ecliptic) | LODs take the colours of the nearby terrain |
| H13 | From a hill in a clearing, by day, look at mountains more than 600 blocks away | They are not visible: the landscape dissolves into the haze before 500 blocks, no mist-coloured silhouette against the sky |
| H14 | Same place at night, then `ringMaxDistance = 128` in `fallow-client.toml` | The ring closes at 260 blocks, then at 128 |
| H15 | Spend a minute in a clearing on a hill, then walk back into the mist and turn around | No frozen relief at the edge of the terrain in the mist |

### Terrain (Tectonic, Streams Reflowing)

Started on the test server on 2026-10-25 (with No Man's Land): the `tectonic` datapack was enabled and the Monoliths were found by `/locate`. Since 2026-11-13 the biomes are Regions Unexplored's and Geophilic's ([0019](../decisions/0019-leaving-no-mans-land.md)): the same tests apply, and the terrain of the new base has not been seen in game.

| # | Test | Expected |
|---|---|---|
| W1 | Explore 3,000 blocks (atlas) | Large continents, mountain ranges, river valleys; Regions Unexplored biomes present (`/locate biome` for a few) |
| W3 | The six Ancient Remnants monuments (`/locate structure ancient_remnants:…`) | Whole, on the ground, not cut by water, monoliths clear of hills ([monuments](monuments.md)) |
| W4 | A Streams stream in a swamp | Note which biome it carries; no error in the log |
| W5 | Highest peak (F3) | Y ≤ 320; airships can cross a range (otherwise lower `vertical_scale`) |
| W6 | Winter (Ecliptic) at Y 70 and Y 150 | Snow consistent with altitude; streams freeze like rivers |
| W7 | Boat, Sable hull on a stream, airship above | The hull drifts (`vesselDriftSpeed`), the airship in flight does not |
| W8 | Create pump and boiler on a stream | Water accepted, Create channels do not overflow |
| W9 | Pregeneration of radius 1,000 (`/dh pregen`, or Chunky if added) with spark | Generation speed noted, compared with Tectonic alone; the rate never falls to one or two chunks per second for minutes (`compat.InStructureMixin`) |
| W10 | A clearing crossed by a stream | Note whether the stream stays misty (river biome not convertible); if so add the river biomes to `fallow:peut_devenir_clairiere` |
| W11 | A stream frozen by winter, then spring; ice placed on a stream next to a torch | The ice thaws into flowing stream water, not into a still pool. The log still shows Streams Reflowing's `IceMeltMixin` warning at start: expected, `compat.IceThawMixin` does its work |

### Clearings

| # | Test | Expected |
|---|---|---|
| K1 | Place a vigil fire in a misty forest | "La brume se retire." above the hotbar; within seconds the mist becomes light around you, the grass turns vivid, without reconnecting |
| K2 | F3 inside the disc, then at its edge | The real biome inside and outside (never `fallow:clairiere`); the vivid grass stops on a round edge of about 16 blocks |
| K3 | Lantern in a snowy taiga, fire in a savanna | Cold look (bluish green, snow still falls), warm look (yellow green) |
| K4 | Clearing on a coast | The ocean and the beach keep their mist and colours; a river crossing the disc clears |
| K5 | Leave the fire in place, leave and come back, restart the server | The clearing stays |
| K12 | Break the fire, then a lantern | "La brume revient."; the mist comes back over its disc within seconds |
| K13 | A fire inside a lantern's disc, break the fire; then the lantern | Nothing changes; then the whole disc goes back to the mist |
| K14 | Charged dissipator, broken | The mist comes back over 64 blocks |
| K15 | The fire and the lantern side by side, by day and at night | Neither looks like a vanilla campfire or lantern; the pale flame flickers, the embers and the crystal glow |
| K16 | Place a lantern, wait, then break it, watching the edge of the disc | The grass turns green ring by ring from the lantern outwards, cloud puffs drift away and the mist thins over 4 or 5 seconds; on breaking, the reverse, from the edge inwards |
| K6 | Clearing music | Node "Clairière" of True Adaptive Music after 30 s |
| K7 | Far view from a clearing on a hill | Distant Horizons landscape beyond 128 blocks (H3) |
| K8 | Dissipator driven at 16 RPM, then 64 RPM, then overstressed | Nothing at 16; at 64 the goggles count up to 100 % and the disc of 64 converts; overstress resets the charge |
| K9 | Dissipator on an airship (Sable) | Note what happens: it converts where the ship is when it finishes charging |
| K10 | Ecliptic Seasons and Cold Sweat in a clearing, compared with the same biome ten blocks outside | Same temperature, same rain or snow, same crop humidity and growth; the grass and leaves follow the season in both |
| K17 | A night in a clearing, then a night just outside | No monster spawns inside; they do outside and can walk in |
| K18 | The biome's animals in a clearing | The fauna of the real biome spawns as elsewhere (not the farm animals of the former clearing biome) |
| K19 | A world made before 0.8.0 with a clearing: `/fallow brume 64` on it, then place the lantern again | F3 shows the generator's biome again, then the clearing opens as in K1 |
| K20 | F3+T (resource reload) and a season change inside a clearing | The vivid colours stay |
| K11 | Recipes in EMI | The three recipes show, with their French names and tooltips, and the new vanilla ingredients (the fire: honeycomb, smooth stone slabs, charcoal, smooth stone; the lantern: iron plates, glass panes, amethyst shard) |

### World edge and disc

| # | Test | Expected |
|---|---|---|
| Z1 | Walk or swim to the edge (`/tp @s 5970 70 0`, then east) | A message 64 blocks before; from about 5,976 a headwind slows the player, at 6,000 it pushes back harder than one walks or swims, with « Un vent contraire se lève… »; no fall, no damage, no teleport |
| Z2 | The same in a boat, then on a horse along a beach near the edge if one exists | The boat or the horse is pushed back with its rider, who stays seated; the motion is smooth, no jerk |
| Z3 | The same with elytra or in creative flight | Slowed and pushed back the same way; a fast dive past 6,048 is set back inside; a spectator passes |
| Z4 | An airship flown full speed at the edge | From about 5,952 the ship slows, then is pushed back towards land; the pilot stays aboard and reads « Le vent du large repousse le navire… » |
| Z5 | Sail around the continent | The coast never comes closer than about 150 blocks to the edge; no land beyond the edge with Distant Horizons |
| Z6 | Walk from the north coast to the south coast | Cold in the north, hot in the south, with no straight limit between climates |
| Z7 | A new world with another seed | Still one continent on the disc; the inland seas differ |
| NV1 | An airship without engines in the open, by calm weather, then in a storm (`/weather thunder`) | It drifts the way the campfire smoke leans, faster in the storm |
| NV2 | The same airship under a big roof or in a cave | It stays still |
| NV3 | A small Sable boat on open water, sails or not | It drifts much less than the airship; `navires.wind = false` stops all drift |
| NV4 | The airship of NV1 in a thunderstorm, 150 blocks up | It drifts by jolts and heels with each gust; at sea level the jolts are weaker |
| NV5 | An airship in a snowy biome while it snows (`/weather rain`, then `/weather thunder`) | Snow layers settle on its deck and balloons, faster in the storm; it sinks slowly unless it climbs harder |
| NV6 | The laden airship moored under a roof, then out in a mild biome after the snow | The snow melts, faster under the roof; the airship lightens |
| BC1 | Two posts on the two banks of a Streams river, a rope on one then the other | The rope hangs between them; one rope per 8 blocks left the inventory; Shift + click with an empty hand on a post gives them back |
| BC2 | A raft of planks on the water under the rope, the pulley on it, a click | « Le radeau est pris à la corde » ; the raft floats and holds under the rope |
| BC3 | Click the pulley, then again; Shift + click | It heads to the far bank (the message names the direction), faster where the river flows, stops at the bank; a click stops it, Shift + click turns it back |
| BC4 | Lead a horse onto the raft and cross | The horse stays aboard (Sable Pathfinder) and walks off on the far bank |
| BC5 | Break a post while the raft crosses | « La corde du bac a été défaite » ; the raft drifts with the current |
| NV7 | Fish 'N' Ships' sailboat, sail down, on each heading | Against the wind it stops; across it, it goes fastest; in a storm faster than in a breeze; `navires.sailWind = false` brings back the mod's own push |
| PR1 | In a vanilla boat on a lake, hook a big fish (a tuna or a sturgeon) and play the fight | « Le poisson vous emporte ! » ; the boat slides along, the bobber goes ahead of it, its heading wanders, splashes when it pulls hard |
| PR2 | The same with a small fish (under 8 kg) | Nothing moves |
| PR3 | The same from a six-plank Sable raft, then a big Sable hull | The raft is taken along faster than the boat; the big hull barely moves |
| PR4 | The same near a bank, then with `prise.enabled = false` | The fish turns at the bank; with the switch off nothing moves |

### Tools

The G tests of [premiers-gestes](premiers-gestes.md#in-game-tests).

### Zoo

| # | Test | Expected |
|---|---|---|
| ZO1 | `/fallow zoo especes` in a world of the full pack | Wildlife of the mods counted, no vanilla monster refused by In Control, no dinosaur, villager or golem |
| ZO2 | Look at a fox from 20 blocks, then turn away | The spot stamp in the action bar with a sound, once; nothing for a fox behind a wall or behind you |
| ZO3 | Take a photo of a cow with an Exposure camera | The photograph stamp for the cow (and the spot stamp if missing) |
| ZO4 | Tame a wolf, breed two cows, catch a cod in a water bucket | The third stamp for each; a creeper or a bat shows no third stamp in the guidebook |
| ZO5 | With the three other stamps, stay a minute near the species | The master stamp and a gold line in the chat; the hover shows the seconds counted before that |
| ZO6 | J; Archie's home tab, then the green bookmark; each left tab; die and come back | The welcome spread, the habitats, the stamps kept after death; Archie's Obsidiceratops and Chorusoatl pages name no Nether or End |

### Armour patina

| # | Test | Expected |
|---|---|---|
| PA1 | `/give @s iron_chestplate[fallow:patine=20]`, then 50, then 90; put them on an armour stand | Earth stains growing from one to the next, the last one with a few rust specks; the tooltip says « Un peu sale », « Sale », « Couverte de boue » |
| PA2 | Wear clean iron armour and walk or fight for a while | The boots get dirty first, the helmet last |
| PA3 | Jump into water, then stand in the rain | In water the armour is clean in a few seconds (the helmet only head under); the rain cleans it slowly |
| PA4 | A zombie with armour, a modded armour, dyed leather armour | Zombie armour already dirty; the modded armour soiled the same way; leather keeps its dye and gets no rust |
| PA5 | `patine.enabled = false` in `fallow-common.toml` | Every armour drawn clean again, no more dirt |
| PA6 | Walk on a beach or in a desert in iron armour, then on grass | Sand grains climb from the boots within a few seconds, « Ensablée » in the tooltip; on grass they fall off in about a minute |
| PA7 | Stand under falling snow, then walk into a plains biome | Snow settles on the helmet and shoulders and packs around the boots; it melts in under a minute away from the cold, at once near lava |
| PA8 | Swim, then walk out in the sun; then stand in the rain | Armour dark with bright drops, drying in about fifty seconds; the rain wets it again |
| PA9 | Same with dyed leather armour | The snow and the sand stay white and pale, not tinted by the dye; the leather darkens when wet |

### Black steel

| # | Test | Expected |
|---|---|---|
| AN1 | Alloy furnace: a steel ingot, two raw magnetite, a charcoal | One raw black steel; in a blast furnace it turns heated |
| AN2 | Hammer the heated raw black steel on a smithing anvil, then cool the ingot in water | A black steel ingot; holding the heated one without tongs burns |
| AN3 | Nine ingots → a block; sneak and right-click the block with a smithing hammer | The block becomes the master anvil (« Enclume de maître »); plates, heads and armour are forged there only |
| AN4 | Steel pickaxe on diamond ore, on rough diamond, on obsidian; then the black steel pickaxe | Steel: no drop on diamond, obsidian mined; black steel: diamond and rough diamond drop, ancient debris refused |
| AN5 | Smithing table: a black steel piece, the diamond template and a diamond | The diamond piece; a steel piece is refused |

### Fading interface

| # | Test | Expected |
|---|---|---|
| EC1 | In survival, change slot, then do nothing for 6 seconds | The hotbar, hearts, food, thirst and temperature gauge fade out together in under a second, items included; the crosshair and chat stay |
| EC2 | Scroll the hotbar, hit, place a block, eat, get hit | The interface is back within a quarter of a second each time |
| EC3 | Walk around for a minute without touching the hotbar | The interface stays hidden; with `showWhileMoving = true` it stays shown while walking |
| EC4 | Dive under water, go down to 3 hearts, get hungry or thirsty, stand in a desert at noon | The interface stays shown as long as the air, health, food, thirst or body temperature need watching |
| EC5 | Ride a horse, open Create's goggles on a machine, use Overgeared's anvil | Jump meter and horse health follow the hotbar; the goggles tooltip and the anvil minigame are never faded |

### Margin notes

| # | Test | Expected |
|---|---|---|
| IN6 | Bare hands on a log, then on stone, a few seconds apart | Two notes on the left with an axe and a pickaxe icon, newest on top; nothing above the hotbar |
| IN7 | Forge at Overgeared's anvil | The remaining hits stay one note that counts down; the minigame overlay is not covered |
| IN8 | Open the chat, press F1, press F3 | The notes stay clear of the chat; hidden with F1 and under F3 |
| IN9 | Hold Spelunkery's depth gauge, then Supplementaries' altimeter; play a disc in a jukebox; ride a horse | The readings stay above the hotbar and update; no note on the left |
| IN10 | Approach a deer standing, then read a tanning vat and a drying frame | « Cerf : vous avez été vu. Accroupissez-vous pour approcher. », « Trempage des peaux à … % (… dans la cuve). », « Séchage du cuir à … %. … » as notes |

### Climate

| # | Test | Expected |
|---|---|---|
| CL1 to CL5 | Seasons felt by the body | See [climat.md](climat.md#in-game-tests) |
| CL6 to CL10 | Snow and sand storms | See [climat.md](climat.md#in-game-tests) |
| CL11 to CL13 | Hot dishes (pack data, not the mod) | See [climat.md](climat.md#in-game-tests) |

### Approach

| # | Test | Expected |
|---|---|---|
| AP1 to AP7 | Scent, sight, hearing, herd, spyglass | See [approche.md](approche.md#in-game-tests) |

### Rest

| # | Test | Expected |
|---|---|---|
| RE1 to RE7 | Rest by the hearth, comfort, effect | See [repos.md](repos.md#in-game-tests) |

### Tannery

| # | Test | Expected |
|---|---|---|
| TA1 to TA8 | Hides, fleshing beam, bark, vat, drying frame, fat, quest, the three stations | See [tannerie.md](tannerie.md#in-game-tests) |

### Forge station

| # | Test | Expected |
|---|---|---|
| FO1 | Right-click a stone anvil, then a steel anvil, empty-handed | The camera comes over the anvil; the forge book beside it lists the recipes of that anvil (fewer on the stone one), with French tabs; the hammer's place shows a faint hammer, the steel anvil's blueprint place a faint blueprint |
| FO2 | Pick a page with the ingots in the inventory | One ingot per cell, in the recipe's shape; the caption says to crouch and strike; a page with a missing piece says which |
| FO3 | Leave, crouch + use with the hammer, then strike | Overgeared's strikes as before; once done, a right-click (hammer in hand or not) opens the station with the result floating to the right; a click takes it |
| FO5 | Lay a heated ingot in a cell, then a recipe's pieces | A clear ring, a hiss and smoke from the hot ingot; once the grid makes a recipe, its result turns faintly where the result comes, with a chime |
| FO6 | Strike the recipe through, then open the station | The forged piece floats over the result place and swells under the cursor; when it arrives with the station open, sparks and a ring |
| FO8 | Pick a page with none of its pieces, then come to the anvil with a heated ingot in hand | The caption says what is missing and the missing pieces show faintly in their cells; the ingot in hand flies to the anvil when the camera arrives |
| FO7 | Click the floating piece, then Shift + click another | The hammer comes from its place, strikes the middle of the anvil with sparks, the piece rises off it and is on the cursor; with Shift, it goes into the inventory with « +1 » |
| FO4 | `[postes] forge = false` | Overgeared's window, as before |

### Hearth station

| # | Test | Expected |
|---|---|---|
| FY1 | Right-click Cold Sweat's hearth with coal, then with ice | The fuel goes into the hearth as before, no station |
| FY2 | Right-click it empty-handed, then on its chimney | The camera comes in front of the hearth; a faint fuel stands before the grate; the hot gauge on the left, the cold one on the right, on the brick above |
| FY3 | Lay coal from the hotbar in the slot, hover the gauges | The hot gauge rises; its tooltip reads « Combustible chaud : N % »; bread and other items are greyed in the inventory |
| FY6 | Shift + click coal in the hotbar | The coal flies from its slot to the grate; sparks and a whoosh as it lands |
| FY5 | Lay coal in the slot, then ice | Sparks and a whoosh from the grate, then a puff of snow; the gauges slide up; the slits glow dull red (bright orange once the hearth burns), with small flames and crackles |
| FY4 | `[postes] foyer = false` | Cold Sweat's window, as before |

### Held items

| # | Test | Expected |
|---|---|---|
| A1 | Hold stone, dirt, a torch, bread, then stand still for 10 seconds | The item is drawn in hand and stays there; only Punchy!'s light sway |
| A2 | Scroll the hotbar between those items and a sword | One lowering then raising of the hand per change, no more |
| A3 | Place stone blocks until the stack shrinks, eat bread | No hand-out animation when the count drops; the eating animation plays normally |

## Maintenance

At each update of ATMOSPHERICS, Enhanced Celestials, Traveler's Backpack, Thirst Was Reclaimed or NeoForge:

1. recompile the mod (the CI does it). An error shows that an API has changed;
2. launch the game and read the log: a mixin that no longer finds its target leaves a warning there;
3. rerun the tests of the module concerned.

Known fragile points:
- ATMOSPHERICS: its mixin at the head of `renderSky`, the `moon_phases.png` texture, the name of `BiomeBlendUtil.resetCache`;
- Enhanced Celestials 2: `ECWorldRenderer.changeMoonColor`, `LunarEvent.getMoonTextureLocation` and `getMoonSize` (mod id `enhancedcelestials2core`);
- Traveler's Backpack: the `EffectFluid` constructor and the moment it empties its registry;
- Thirst Was Reclaimed: `WaterPurity` and `ThirstHelper`; for `ecran`, the `thirst:player_thirst` attachment and its `getThirst` method.
- Cold Sweat: for `ecran`, the `Overlays.BLEND_BODY_TEMP` field and the names of its GUI layers (`cold_sweat:body_temp`, `cold_sweat:vague_temp`).
- Distant Horizons: the `DhApiBeforeFogRenderEvent` and `DhApiBeforeRenderEvent` events (API 7), `GLState.saveState`, `AbstractChunkyAccessor.tryRunFirstTimeSetup` and its `listenerBound` field.
- Punchy!: the `HandEquipStateMachine.shouldIgnoreComponentForEquipDiff` method (class `punchy.client.state.HandEquipStateMachine`). If an update of Punchy! or Sensible Stackables fixes the mismatch on its side, the mixin can go.
- Create: `KineticBlockEntity` (`write`/`read`, `addToGoggleTooltip`), `RotatedPillarKineticBlock`, `BlockStressValues.IMPACTS`; the compile classpath pins Ponder and Flywheel to the versions Create bundles (`build.gradle`).
- Clearings: the names of the ATMOSPHERICS methods that read the biome (`BiomeBlendUtil.collect`, `AtmosphericEnvironmentVisuals.getBiomeId`, `AirHazeBiomeSettings.smooth`, `HorizonHazeCometRenderer.renderHaze` and `getCurrentBiomeSettings`), True Adaptive Music's `BiomePredicate.test`, Sodium's `LevelColorCache.updateColorBuffers`. All use `require = 0`: a renamed method leaves that part of the look off (mist, music or colours) without an error, so check K1, K6 and K2 after an update.
- Minecraft and NeoForge: `ClientLevel.calculateBlockTint`, `Biome.getGrassColor`, `getFoliageColor` and `getWaterColor`, `MobSpawnEvent.PositionCheck`, `ChunkAccess.fillBiomesFromNoise` and `ChunkMap.resendBiomesForChunks` (clearings, the last two for old worlds only); the read of `Player.experienceLevel` in `AnvilMenu.mayPickup`, the names of the `EXPERIENCE_BAR` and `EXPERIENCE_LEVEL` layers and of the hotbar group faded by `ecran`, the cancellation of `AnvilUpdateEvent`.

## Next modules

Eight modules approved on 2026-10-26 (`voile`, `zoo` (built in 0.17.0), `vie`, `fanal`, `eclaircie`, `sillage`, `echos`, `survivants`) are designed in [modules-fallow.md](modules-fallow.md), with their order of work. The modules of [12.4](../archive/plan/12-verification.md#124-the-custom-fallow-mod) not yet written (grave, tracks, blueprints, finishing touches) stay possible. Each module will have its section here, its switch and its tests.

