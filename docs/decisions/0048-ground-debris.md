# 0048 — Ground debris: rocks, fallen wood and pine cones

- **Date**: 2026-11-21
- **Status**: superseded by [0052](0052-gestures-not-debris.md) the same day (too present on the ground); was `fallow` 0.61.0, removed in 0.67.0

## Context
The team looked at the Bogwood modpack and kept the idea of This Rocks!: little rocks, sticks and pine cones on the ground that make the world feel natural. This Rocks! is Fabric only (no NeoForge 1.21.1 build on 2026-11-21). Twigs, already in the pack, lays pebbles and twigs, but the same grey pebble and the same twig everywhere, whatever the stone or the tree.

## Options
| Criterion | A. Twigs as it is | B. This Rocks! | C. House debris, models from This Rocks! |
|---|---|---|---|
| NeoForge 1.21.1 | Yes | No | Yes (`fallow` mod) |
| What lies on the ground | One pebble, one twig | A rock per stone, a stick per wood, by biome | A rock of the ground's stone, wood of the tree above, pine cones under conifers |
| Regions Unexplored's trees and stones | Ignored | Ignored | Its leaves mapped by tags |
| First gestures | Pebble → knappable rock | None | Rock → knappable rock, wood → 2 sticks, pine cone as fuel |
| New textures | None | Two (starfish, seashell) | None: vanilla textures on This Rocks!' models (MIT) |

## Decision
Option C, the `debris` module of `fallow` 0.61.0 ([premiers-gestes](../plan/premiers-gestes.md#ground-debris), [mod-fallow](../plan/mod-fallow.md#debris-module)):

- 17 blocks: 8 rocks (stone, granite, diorite, andesite, sandstone, red sandstone, gravel chip, ice chip), fallen wood of the 8 vanilla woods, the pine cone; picked up empty-handed, broken at once, no collision;
- the feature type `fallow:debris` reads the ground and the lowest leaves above each spot; where and how many are data of the pack;
- Twigs' pebbles and twigs no longer generate; the Twigs items and the pebble's knapping recipe stay.

## Rejected
- Starfish (This Rocks!): an animal drawn as a block, against the rule that nothing that looks alive is fake.
- Seashells (This Rocks!): Twigs already lays them on beaches.
- Geysers (This Rocks!): a mechanic of their own (a jet that throws players up), off the subject.
- A stick block per wood of Regions Unexplored: about fifteen more blocks for bark that the eight vanilla woods come close to.
