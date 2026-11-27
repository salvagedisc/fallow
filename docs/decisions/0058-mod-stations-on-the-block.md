# 0058 — The mods' crafting stations worked on the block

- **Date**: 2026-11-26
- **Status**: kept; checked on a NeoForge 1.21.1 client with Diegesis, Hands-On Vanilla, Overgeared, Cold Sweat, Exposure and Spatial GUI, without Forager (its terms, [0056](0056-forager.md#licence)). Replaces [0057](0057-station-windows-as-sheets.md) for the crafting stations; the sheets stay for the windows that are not one.

## Context
[0057](0057-station-windows-as-sheets.md) answered the team's wish for menus in harmony with the rest by restyling the mods' station windows as sheets of the books' paper. The owner's answer: the pack is built on Hands-On, so a station is worked on its block, the camera over it and nothing in a window, as Hands-On Vanilla does for the crafting table and the furnaces and as the pack's tannery, forge and hearth already do ([0042](0042-interaction-language.md)). He chose to treat every crafting station that way: Overgeared's alloy, nether alloy and casting furnaces, its fletching and drafting tables and its knapping, Cold Sweat's sewing table, boiler and icebox, Exposure's lightroom.

## Options
| Criterion | A. Diegesis stations on the mods' own menus | B. Our own blocks and menus | C. Keep the sheets |
|---|---|---|---|
| Server | Unchanged: the mod opens its usual menu, the station shows it | New blocks, recipes and logic to keep in step with each mod | Unchanged |
| Look | The mod's own block, its slots laid on it as items, its gauge painted on its front | Ours | A window |
| Work | One small conduct shared by all, a scene per block | A port of every mod | None |
| Hands-On | Same library, same gestures | Same library | No |

## Decision
Option A, in the `postes` module.

- Each block is a Diegesis menu station bound to the mod's own menu (Diegesis is given a chest type it never sees, the real menu accepted besides), opened by a right-click unless crouching, client only like Hands-On Vanilla's. Its slots lie on the block (`PosteDeMod`): metals and flasks flat on the top, fuel in the firebox's mouth, results standing; the work's progress (cooking, fuel, printing) is the stations' bar on the front; fire, steam or cold rise when the block works; pieces fly in from the hotbar, ring and slide as at Hands-On Vanilla's anvil.
- Choices that were buttons become gestures on the block: at the drafting table a steel tool head per kind of tool lies on the sheet, and a click on one draws its blueprint; at the lightroom Exposure paints the chosen negative leaning on the table, a click prints, the wheel goes from frame to frame, a right click changes the process.
- Menus opened with extra data (the furnaces, the fletching table, Cold Sweat's and Exposure's blocks) are replayed as vanilla packets so Diegesis hears them. Overgeared opens its fletching table from its own listener, ahead of Diegesis: on the client only, that listener lets the block's use through.
- Knapping has no block (a rock in each hand). It opens a screen of our own instead of Overgeared's window: the rock held, large, in the middle of the screen, its flakes knocked off by clicks, the world kept in view; not a container screen, so Spatial GUI leaves it before the eyes.
- Each family has its switch in `[postes]` (`fours`, `tables`, `chaudieres`, `labo`, `taille`); off, the mod's window comes back, still a sheet when the resource pack has one.
- Not stations: windows that are storage or a chest under another name (Quark's crate and trough, Supplementaries' present and notice board) and the Create and Aeronautics machines keep [0057](0057-station-windows-as-sheets.md).

## Rejected
- B: ports of every mod, to be followed at each of their updates, for the same result.
- C: what the owner turned down.
