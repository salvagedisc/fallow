# 0041 — The game does not depend on the shader

- **Date**: 2026-11-21
- **Status**: accepted (maintainer's request of 2026-11-21)

## Context
Brumaire is on from the first launch ([0031](0031-brumaire-is-the-look.md)) and K turns it off. A shader pack costs frame rate, Iris does not run on Apple Silicon Macs, and any player may turn it off. Two questions followed: does the pack still play without it, and does the shader draw things the game does not have?

An inventory of every Brumaire effect (59 passes, `docs/plan/shader.md`) against the `fallow` mod and the mods of the pack found:

- **Gameplay already lives in the game.** Every rule is in the `fallow` mod or another mod and runs on the server: clearings, monsters kept out of them, seasons and storms felt by the body (`climat`), the approach (`approche`), dirty, sandy, snowy and wet armour (`patine`, drawn in its texture), rest by the hearth. None reads the shader.
- **Three signals were seen only one way.**
  1. Without the shader the mist is ATMOSPHERICS' own preset, which knows no clearing ([0024](0024-removing-the-house-resource-packs.md)): lighting a vigil fire changed the colours of the ground but not the mist on screen.
  2. Under the shader the snow and sand storms of `climat` did not close the view: only ATMOSPHERICS' fog did, which Brumaire does not draw.
  3. The wind, which carries the player's scent to the animals, was read on the swaying plants of the shader; without it only campfire smoke and Particular's leaves showed it.
- **The shader drew creatures that did not exist**: butterflies, dragonflies, moths, fireflies, flocks of birds, bats, rising fish, glowworms; and ice that looked solid and was not (icicles, shore ice). The pack already holds real ones: Longwings' butterflies and moths, Naturalist's dragonflies, fireflies and birds, the birds of Alex's Mobs and Hybrid Birds, the game's bats, the fish of Naturalist, Hybrid Aquatic and Spawn.
- **Some effects were drawn twice**, once by a mod and once by the shader: frosty breath (Subtle Effects), frost on the screen (Cold Sweat's freezing vignette, which follows the body), lava sparks and pollen (Subtle Effects), falling leaves (Particular); fireflies were drawn three times (Naturalist's entity, Particular's particles, the shader).

## Decision
- **Brumaire is a visual layer.** It keeps its four profiles and stays on by default; the pack plays the same without it.
- **Rule for the shader**: it draws nothing a player could take for a creature or a solid thing that the world does not hold, and no effect a mod of the pack already draws. The removed effects and their replacements are listed in [shader](../plan/shader.md#what-brumaire-may-draw). Particular's firefly particles are turned off for the same reason.
- **Every signal the game needs is given by the game first**, the shader only draws it better:
  - the `fallow` mod opens and closes ATMOSPHERICS' mist around clearings when no shader pack runs (`ciel` module), from the same share of lifted mist it hands the shader;
  - the mod hands Brumaire the strength of the current storm (`brumaireStorm`), so its mist closes and takes the colour of sand or snow like the fog without shader;
  - the mod sows a few motes that drift with the pack's wind in the open (`vent` module), so the wind can be read without the shader.

## Consequences
- An Apple Silicon Mac, or a player who presses K, keeps the clearings, the storms and the wind.
- No creature in the world is drawn by the shader: what flies, swims or glows can be approached, caught or studied for the zoo.
- Brumaire is lighter by sixteen options and five library files.
- 2026-11-22, on a second check: the shader's rain rings on water and its waterfall spray doubled Particular's rain ripples, waterfall spray and cascade clouds, and left it under the same rule (eighteen options in all).
- New in-game tests: NS1 to NS4 ([tests](../tests-en-jeu.md)).
