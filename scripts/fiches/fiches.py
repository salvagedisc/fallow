"""The mods' station windows drawn as a sheet of the books' paper laid on the inventory's wooden case.

Vanilla's container windows are drawn by the base resource pack (Forager, a wooden inventory). The mods' station windows
(Overgeared's furnaces and tables, Cold Sweat's, the cooking pot, Quark's, Supplementaries', Exposure's, Envelope's...) copy
vanilla's grey panel and stay grey beside it. Here each one becomes two things:

- the station's part, at the top: a loose sheet of the books' paper (papers.Page, as the Hosts' dialogue), its wells pressed
  into it as in the recipe books, its arrows, flames and pictograms in the books' inks (brown, never black);
- the player's inventory, below: no longer in the texture at all (transparent). The `fiches` module of the `fallow` mod draws
  there, before the window's own background, the inventory part of vanilla's chest window
  (minecraft:textures/gui/container/generic_54.png, the strips vanilla's own chest window uses), so it is whatever the active
  resource pack draws for a chest: Forager's wood in the pack. Nothing of Forager is read here.

The geometry of every window (where its inventory sits, how far the sheet goes) is read from the mod's own texture: its slots
are vanilla's 18 x 18 wells, found by their colours. The module reads it back from assets/fallow/fiches.json, written here beside the textures: without the pack's resources,
the module does nothing.

Reads the mods' jars from the cache filled by `npm run check-deps` (.cache/jars). Run from the repository root:
  python3 scripts/fiches/fiches.py
Writes:
  pack/kubejs/assets/<mod>/textures/gui/... (one texture per window and per button sprite, same path as the mod's)
  pack/kubejs/assets/fallow/fiches.json
A window taken out of WINDOWS leaves its texture behind: delete it by hand.
"""

import io
import json
import sys
import zipfile
import zlib
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "scripts/books"))
from papers import Page, mix  # noqa: E402

JARS = ROOT / ".cache/jars"
KUBEJS = ROOT / "pack/kubejs/assets"
OUT_JSON = KUBEJS / "fallow/fiches.json"

# The books' palette (papers.py, feuillet.py).
INK = (59, 42, 26)
INK_SOFT = (138, 107, 69)
INK_FAINT = (185, 162, 124)
PAPER = (249, 241, 218)
PAPER_SHADE = (234, 220, 184)
EDGE = (212, 180, 126)
RIM_DARK = (184, 158, 112)
RIM_LIGHT = (255, 249, 230)
WELL_SHADE = (222, 204, 164)
RED = (163, 50, 42)

# Vanilla's panel greys.
PANEL = (198, 198, 198)
SLOT_FILL = (139, 139, 139)

# One entry per texture: the jar (start of its file name), the texture, the window's size, the screens that draw it, and
# what is special about it.
#   bottom: where the sheet ends, when the screen draws widgets of its own below its drawn content;
#   untitled: the window's title is not written (it would run over the station's drawing);
#   wells: (x, y, size) slots the search misses (a pictogram fills them), drawn as wells;
#   boards: (x, y, w, h, inside) a dark board in the window, drawn as one well of the paper (inside: x, y, w, h);
#   sprites: regions outside the window that the screen draws over it (u, v, w, h, how): "fill" a progress drawn in ink
#            over its empty outline, "flame" a fire kept in its colours, "ink" a pictogram, "cold" a pictogram whose blue-greys are greys.
WINDOWS = [
    dict(jar="overgeared", tex="assets/overgeared/textures/gui/brick_alloy_furnace.png", size=(176, 166),
         screens=["net.stirdrem.overgeared.screen.AlloySmelterScreen"],
         sprites=[(176, 0, 14, 14, "flame"), (176, 14, 24, 17, "fill")]),
    dict(jar="overgeared", tex="assets/overgeared/textures/gui/nether_alloy_furnace.png", size=(176, 166),
         screens=["net.stirdrem.overgeared.screen.NetherAlloySmelterScreen"],
         sprites=[(176, 0, 14, 14, "flame"), (176, 14, 24, 17, "fill")]),
    dict(jar="overgeared", tex="assets/overgeared/textures/gui/cast_furnace.png", size=(176, 166),
         screens=["net.stirdrem.overgeared.screen.CastingSmelterScreen"],
         sprites=[(176, 0, 14, 14, "flame"), (176, 14, 24, 17, "fill")]),
    dict(jar="overgeared", tex="assets/overgeared/textures/gui/fletching_table.png", size=(176, 166),
         screens=["net.stirdrem.overgeared.screen.FletchingStationScreen"]),
    dict(jar="overgeared", tex="assets/overgeared/textures/gui/blueprint_workbench.png", size=(176, 166),
         screens=["net.stirdrem.overgeared.screen.BlueprintWorkbenchScreen"], bottom=76, wells=[(48, 35, 16)]),
    dict(jar="overgeared", tex="assets/overgeared/textures/gui/rock_knapping_gui.png", size=(176, 166),
         screens=["net.stirdrem.overgeared.screen.RockKnappingScreen"], bottom=72),
    dict(jar="ColdSweat", tex="assets/cold_sweat/textures/gui/screen/sewing_gui.png", size=(176, 201),
         screens=["com.momosoftworks.coldsweat.client.gui.SewingScreen"]),
    dict(jar="ColdSweat", tex="assets/cold_sweat/textures/gui/screen/boiler_gui.png", size=(176, 172),
         screens=["com.momosoftworks.coldsweat.client.gui.BoilerScreen"]),
    dict(jar="ColdSweat", tex="assets/cold_sweat/textures/gui/screen/icebox_gui.png", size=(176, 172),
         screens=["com.momosoftworks.coldsweat.client.gui.IceboxScreen"]),
    dict(jar="ColdSweat", tex="assets/cold_sweat/textures/gui/screen/hearth_gui.png", size=(176, 166),
         screens=["com.momosoftworks.coldsweat.client.gui.HearthScreen"]),
    dict(jar="FarmersDelight", tex="assets/farmersdelight/textures/gui/cooking_pot.png", size=(176, 166),
         screens=["vectorwing.farmersdelight.client.gui.CookingPotScreen"],
         sprites=[(176, 0, 17, 15, "flame"), (176, 15, 24, 17, "fill")]),
    dict(jar="Quark", tex="assets/quark/textures/gui/container/feeding_trough.png", size=(176, 166),
         screens=["org.violetmoon.quark.content.automation.client.screen.TroughScreen"]),
    dict(jar="supplementaries", tex="assets/supplementaries/textures/gui/notice_board_gui.png", size=(176, 166),
         screens=["net.mehvahdjukaar.supplementaries.client.screens.NoticeBoardScreen"],
         sprites=[(176, 0, 48, 56, "flame")], untitled=True, boards=[(81, 10, 63, 62, (88, 13, 48, 56))]),
    dict(jar="supplementaries", tex="assets/supplementaries/textures/gui/present_gui.png", size=(176, 166),
         screens=["net.mehvahdjukaar.supplementaries.client.screens.PresentScreen"],
         boards=[(50, 15, 110, 56, (52, 17, 106, 51))]),
    dict(jar="supplementaries", tex="assets/supplementaries/textures/gui/present_empty_gui.png", size=(176, 166),
         screens=["net.mehvahdjukaar.supplementaries.client.screens.PresentScreen"],
         boards=[(50, 15, 110, 56, (52, 17, 106, 51))]),
    dict(jar="supplementaries", tex="assets/supplementaries/textures/gui/trapped_present_gui.png", size=(176, 166),
         screens=["net.mehvahdjukaar.supplementaries.client.screens.TrappedPresentScreen"]),
    dict(jar="supplementaries", tex="assets/supplementaries/textures/gui/cannon_gui.png", size=(176, 166),
         screens=["net.mehvahdjukaar.supplementaries.client.screens.CannonScreen"]),
    dict(jar="exposure", tex="assets/exposure/textures/gui/lightroom.png", size=(176, 209),
         screens=["io.github.mortuusars.exposure.client.gui.screen.LightroomScreen"],
         sprites=[(0, 209, 28, 31, "cold"), (176, 0, 24, 17, "fill"), (200, 0, 20, 10, "ink")]),
    dict(jar="chalk-neoforge", tex="assets/chalk/textures/gui/container/chalk_box.png", size=(176, 166),
         screens=["io.github.mortuusars.chalk.client.gui.screens.ChalkBoxScreen"],
         sprites=[(176, 0, 25, 18, "ink")]),
    dict(jar="envelope", tex="assets/envelope/textures/gui/packing.png", size=(176, 178),
         screens=["io.github.mortuusars.envelope.client.gui.screen.PackingScreen"]),
    dict(jar="envelope", tex="assets/envelope/textures/gui/package.png", size=(176, 178),
         screens=["io.github.mortuusars.envelope.client.gui.screen.PackageScreen"],
         sprites=[(0, 178, 86, 64, "ink")]),
    dict(jar="envelope", tex="assets/envelope/textures/gui/payback_packing.png", size=(176, 178),
         screens=["io.github.mortuusars.envelope.client.gui.screen.PaybackPackingScreen"],
         sprites=[(176, 0, 18, 36, "ink")]),
    dict(jar="envelope", tex="assets/envelope/textures/gui/payback_package.png", size=(176, 178),
         screens=["io.github.mortuusars.envelope.client.gui.screen.PaybackPackageScreen"],
         sprites=[(0, 178, 86, 64, "ink")]),
    # Quark's crate: a six-row chest with a scroll bar on its right (the bar 20 wide beside the window, its handle 12 x 15
    # at u 232 and 244).
    dict(jar="Quark", tex="assets/quark/textures/gui/crate.png", size=(176, 222), kind="coffre", rows=6,
         screens=["org.violetmoon.quark.addons.oddities.client.screen.CrateScreen"],
         bar=(173, 10, 22, 126), track=(176, 17, 14, 112), sprites=[(232, 0, 24, 15, "ink")]),
    dict(jar="fish_n_ships", tex="assets/fish_n_ships/textures/gui/container/ship_3row.png", size=(182, 226), kind="dessus",
         screens=["dev.hybridlabs.fishnships.client.gui.screen.ShipScreen"], split=(16, 133, 132), untitled=True),
    dict(jar="hybrid_aquatic-neoforge", tex="assets/hybrid_aquatic/textures/gui/container/argonaut_3row.png", size=(182, 167),
         kind="dessus", screens=["dev.hybridlabs.aquatic.client.gui.screen.ArgonautScreen"], split=(16, 126, 125)),
]


# Buttons and pictograms the windows draw as sprites of their own, grey in vanilla's manner: drawn again as small cards of the
# books' paper in a line of soft ink, their pictograms in ink (the coloured ones kept). (jar, folder in the jar, file names'
# start.)
BUTTONS = [
    ("supplementaries", "assets/supplementaries/textures/gui/sprites/present/", "button"),
    ("supplementaries", "assets/supplementaries/textures/gui/sprites/trapped_present/", "button"),
    ("supplementaries", "assets/supplementaries/textures/gui/sprites/cannon/", ""),
    ("exposure", "assets/exposure/textures/gui/sprites/lightroom/", ""),
    ("exposure", "assets/exposure/textures/gui/sprites/widgets/", ""),
]


def jar_for(prefix):
    for path in sorted(JARS.glob("*.jar")):
        if path.name.lower().startswith(prefix.lower()):
            return path
    raise SystemExit(f"jar {prefix}* introuvable dans {JARS} : lancer `npm run check-deps` d'abord")


def read_texture(spec):
    with zipfile.ZipFile(jar_for(spec["jar"])) as z:
        data = z.read(spec["tex"])
    return np.asarray(Image.open(io.BytesIO(data)).convert("RGBA"), dtype=np.uint8).copy()


def components(mask):
    """4-connected components of a boolean mask, as lists of (y, x)."""
    h, w = mask.shape
    seen = np.zeros_like(mask)
    out = []
    for y0 in range(h):
        for x0 in range(w):
            if mask[y0, x0] and not seen[y0, x0]:
                stack, comp = [(y0, x0)], []
                seen[y0, x0] = True
                while stack:
                    y, x = stack.pop()
                    comp.append((y, x))
                    for yy, xx in ((y + 1, x), (y - 1, x), (y, x + 1), (y, x - 1)):
                        if 0 <= yy < h and 0 <= xx < w and mask[yy, xx] and not seen[yy, xx]:
                            seen[yy, xx] = True
                            stack.append((yy, xx))
                out.append(comp)
    return out


def find_wells(img, w, h):
    """Vanilla's slot wells inside the window: a square of the slot grey (pictograms may sit in it), a dark line above and on
    the left, a light one below and on the right. Returns (x, y, size) of the inside, the item's place for an 18 well."""
    win = img[:h, :w]
    rgb = win[:, :, :3].astype(int)
    grey = (np.abs(rgb - SLOT_FILL).sum(axis=2) <= 6) & (win[:, :, 3] > 0)
    wells = []
    for comp in components(grey):
        ys = [p[0] for p in comp]
        xs = [p[1] for p in comp]
        x0, y0, x1, y1 = min(xs), min(ys), max(xs), max(ys)
        bw, bh = x1 - x0 + 1, y1 - y0 + 1
        if bw != bh or bw < 16 or bw > 26 or len(comp) < bw * bh * 0.45:
            continue
        top = rgb[y0 - 1, x0:x1 + 1].mean(axis=0) if y0 > 0 else (255, 255, 255)
        foot = rgb[y1 + 1, x0:x1 + 1].mean(axis=0) if y1 + 1 < h else (0, 0, 0)
        if np.mean(top) < 90 and np.mean(foot) > 200:
            wells.append((x0, y0, bw))
    return sorted(wells, key=lambda t: (t[1], t[0]))


def inventory_of(wells):
    """The player's inventory: three rows of nine 16-wells, 18 apart, then the hotbar. Returns (x, y) of its first well's
    inside."""
    small = [(x, y) for x, y, s in wells if s == 16]
    rows = {}
    for x, y in small:
        rows.setdefault(y, []).append(x)
    full = sorted(y for y, xs in rows.items() if len(xs) >= 9)
    last = None
    for y in full:
        if y + 18 in full and y + 36 in full:
            last = y
    if last is None:
        raise ValueError("no player inventory (three rows of nine slots) found in the window")
    first = last
    x = min(rows[first])
    return x, first


def frame_mask(img, w, h, depth=4):
    """The window's frame: the texels of the window within `depth` of its outside."""
    alpha = img[:h, :w, 3] > 0
    inner = alpha.copy()
    for _ in range(depth):
        nxt = inner.copy()
        nxt[1:, :] &= inner[:-1, :]
        nxt[:-1, :] &= inner[1:, :]
        nxt[:, 1:] &= inner[:, :-1]
        nxt[:, :-1] &= inner[:, 1:]
        nxt[0, :] = nxt[-1, :] = False
        nxt[:, 0] = nxt[:, -1] = False
        inner = nxt
    return alpha & ~inner


def ink_of(rgb, alpha, kind="ink"):
    """A pixel of a mod's grey drawing as the books' ink: its darkness gives the ink, colours are printed as they are."""
    r, g, b = (int(c) for c in rgb)
    if alpha == 0:
        return None
    lum = 0.299 * r + 0.587 * g + 0.114 * b
    sat = max(r, g, b) - min(r, g, b)
    if kind == "cold" and b > r:
        sat = 0
    if sat > 40:
        return (r, g, b, alpha)
    if abs(lum - 198) < 8:
        return None
    if kind == "fill":
        return (*INK_SOFT, alpha) if lum > 120 else (*INK, alpha)
    if lum < 75:
        return (*INK, alpha)
    if lum < 120:
        return (*INK_SOFT, alpha)
    if lum < 175:
        return (*INK_FAINT, alpha)
    if lum > 225:
        return (*RIM_LIGHT, int(alpha * 0.7))
    return None


def sheet_paper(w, h, seed):
    """The loose sheet, as the Hosts' dialogue: clouds and grain, yellowing towards every edge, a darker hairline, a few nicks
    and worn corners."""
    p = Page(w, h, seed, paper=PAPER, edge=EDGE, ink=INK_SOFT, fold_width=1)
    p.outer = np.minimum(np.minimum(p.ys, h - 1 - p.ys), np.minimum(p.xs, w - 1 - p.xs))
    p.clouds(strength=0.022, light_patches=3)
    p.age(mottle=0.0, grain=0.007, yellowing=0.48, wear=4.0)
    p.fibres(int(w * h / 90))
    p.foxing(2, strength=0.12)
    p.worn_corners(radius=2)
    p.nicks(2)
    p.rgb, p.alpha = p.rgb[:, ::-1].copy(), p.alpha[:, ::-1].copy()
    p.worn_corners(radius=2)
    p.nicks(1, sides=1)
    p.rgb, p.alpha = p.rgb[:, ::-1].copy(), p.alpha[:, ::-1].copy()
    rim = np.asarray(EDGE, np.float32) * 0.78
    for x in range(w):
        p.rgb[0, x] = mix(p.rgb[0, x][None, None], rim, 0.50)[0, 0]
        p.rgb[h - 1, x] = mix(p.rgb[h - 1, x][None, None], rim * 0.85, 0.68)[0, 0]
    for y in range(h):
        p.rgb[y, 0] = mix(p.rgb[y, 0][None, None], rim, 0.55)[0, 0]
        p.rgb[y, w - 1] = mix(p.rgb[y, w - 1][None, None], rim * 0.9, 0.62)[0, 0]
    return np.dstack([p.rgb, p.alpha.astype(np.float32)])


def put(out, x, y, c):
    """Composites one RGBA texel `c` (alpha 0-255) over `out` (floats)."""
    a = c[3] / 255.0
    base_a = out[y, x, 3] / 255.0
    na = a + base_a * (1 - a)
    if na <= 0:
        return
    out[y, x, :3] = (np.asarray(c[:3], np.float32) * a + out[y, x, :3] * base_a * (1 - a)) / na
    out[y, x, 3] = na * 255.0


def draw_well(out, x, y, size, height=None):
    """A well pressed into the paper, as the recipe books' ready well: a rim of soft dark paper all round, corners notched,
    shaded inside above and on the left, lit below and on the right. (x, y) is the inside, as vanilla's; `height` for a
    well that is not square."""
    nw, nh = size + 2, (size if height is None else height) + 2
    x0, y0 = x - 1, y - 1
    for j in range(nh):
        for i in range(nw):
            if i in (0, nw - 1) and j in (0, nh - 1):
                continue
            if i in (0, nw - 1) or j in (0, nh - 1):
                c = RIM_DARK
            elif i == nw - 2 or j == nh - 2:
                c = RIM_LIGHT
            elif i == 1 or j == 1:
                c = WELL_SHADE
            else:
                c = PAPER_SHADE
            put(out, x0 + i, y0 + j, (*c, 255))


def build(spec, seed):
    img = read_texture(spec)
    w, h = spec["size"]
    wells = find_wells(img, w, h)
    inv_x, inv_y = inventory_of(wells)
    case_x, case_y = inv_x - 8, inv_y - 31
    label_y = inv_y - 12
    frame = frame_mask(img, w, h)

    # What the station draws above its inventory: wells and pictograms, everything that is not the panel or its frame.
    station = [(x, y, s) for x, y, s in wells if y < label_y] + [tuple(t) for t in spec.get("wells", [])]
    content_bottom = 0
    rgb = img[:, :, :3].astype(int)
    for y in range(h):
        for x in range(w):
            if y >= label_y or frame[y, x] or img[y, x, 3] == 0:
                continue
            if np.abs(rgb[y, x] - PANEL).sum() > 6:
                content_bottom = max(content_bottom, y)
    for x, y, s in station:
        content_bottom = max(content_bottom, y + s)
    sheet_top, sheet_left, sheet_right = 1, 3, w - 3
    sheet_bottom = spec.get("bottom") or min(max(content_bottom + 5, case_y + 7), inv_y - 4)

    out = np.zeros(img.shape, np.float32)
    # The sheet's shadow on the case and the world below it.
    sw, sh = sheet_right - sheet_left, sheet_bottom - sheet_top
    for dx, dy, k in ((1, 1, 0.30), (2, 2, 0.20), (3, 3, 0.10), (0, 1, 0.18), (1, 0, 0.10)):
        for y in range(sheet_top + dy, sheet_bottom + dy):
            for x in range(sheet_left + dx, sheet_right + dx):
                if out[y, x, 3] < k * 255:
                    out[y, x] = (20, 14, 8, k * 255)
    paper = sheet_paper(sw, sh, seed)
    for y in range(sh):
        for x in range(sw):
            if paper[y, x, 3] > 0:
                put(out, sheet_left + x, sheet_top + y, tuple(paper[y, x]))

    # The station's pictograms in ink, and its wells pressed in.
    inside = np.zeros((h, w), bool)
    for x, y, s in station:
        inside[y - 1:y + s + 1, x - 1:x + s + 1] = True
    for x, y, bw, bh, _ in spec.get("boards", []):
        inside[y:y + bh, x:x + bw] = True
    for _, _, _, _, (x, y, bw, bh) in spec.get("boards", []):
        draw_well(out, x, y, bw, bh)
    for x, y, s in station:
        draw_well(out, x, y, s)
        for yy in range(y, y + s):
            for xx in range(x, x + s):
                if np.abs(rgb[yy, xx] - SLOT_FILL).sum() > 6:
                    c = ink_of(img[yy, xx, :3], img[yy, xx, 3])
                    if c:
                        put(out, xx, yy, (*INK_FAINT, 200) if c[:3] in (INK, INK_SOFT) else c)
    # Broad flat areas of a mid grey (Envelope's package drawn round its slots): a wash of the wells' shade, not a block
    # of ink.
    win = rgb[:h, :w]
    lum = win @ np.array([0.299, 0.587, 0.114])
    flat = (img[:h, :w, 3] > 0) & (np.ptp(win, axis=2) <= 10) & (lum > 120) & (lum < 192) & ~frame & ~inside
    flat[label_y:, :] = False
    wash = np.zeros((h, w), bool)
    for comp in components(flat):
        if len(comp) >= 100:
            for y, x in comp:
                wash[y, x] = True
    for y in range(sheet_top + 2, sheet_bottom - 1):
        for x in range(sheet_left + 2, sheet_right - 2):
            if frame[y, x] or inside[y, x] or y >= label_y:
                continue
            if wash[y, x]:
                put(out, x, y, (*WELL_SHADE, 255))
                continue
            c = ink_of(img[y, x, :3], img[y, x, 3])
            if c:
                put(out, x, y, c)

    # The sprites the screen draws over the window, outside it.
    for u, v, sw_, sh_, how in spec.get("sprites", []):
        for y in range(v, v + sh_):
            for x in range(u, u + sw_):
                c = ink_of(img[y, x, :3], img[y, x, 3], how)
                if c:
                    out[y, x] = c

    save(spec, out)
    entry = {"x": case_x, "y": case_y, "rows": 0, "fiche": True}
    if spec.get("untitled"):
        entry["title"] = False
    return {s: dict(entry) for s in spec["screens"]}


def slip(out, x0, y0, w, h, seed):
    """A narrow slip of the books' paper, with its shadow, at (x0, y0)."""
    for dx, dy, k in ((1, 1, 0.30), (2, 2, 0.18), (0, 1, 0.16)):
        for y in range(y0 + dy, y0 + h + dy):
            for x in range(x0 + dx, x0 + w + dx):
                if out[y, x, 3] < k * 255:
                    out[y, x] = (20, 14, 8, k * 255)
    paper = sheet_paper(w, h, seed)
    for y in range(h):
        for x in range(w):
            if paper[y, x, 3] > 0:
                put(out, x0 + x, y0 + y, tuple(paper[y, x]))


def build_chest(spec, seed):
    """A window that is a chest in all but name (Quark's crate): the module draws vanilla's chest window of as many rows, and
    the texture keeps only what the chest has not, its scroll bar, on a slip of paper beside the chest."""
    img = read_texture(spec)
    w, h = spec["size"]
    wells = find_wells(img, w, h)
    inv_x, inv_y = inventory_of(wells)
    rows = spec["rows"]
    case_x, case_y = inv_x - 8, inv_y - 31 - rows * 18
    out = np.zeros(img.shape, np.float32)
    x0, y0, sw, sh = spec["bar"]
    slip(out, x0, y0, sw, sh, seed)
    tx, ty, tw, th = spec["track"]
    for y in range(ty, ty + th):
        for x in range(tx, tx + tw):
            edge = x in (tx, tx + tw - 1) or y in (ty, ty + th - 1)
            put(out, x, y, (*(RIM_DARK if edge else PAPER_SHADE), 255))
    for u, v, sw_, sh_, how in spec.get("sprites", []):
        for y in range(v, v + sh_):
            for x in range(u, u + sw_):
                if img[y, x, 3]:
                    out[y, x] = button(img[y:y + 1, x:x + 1], "")[0, 0]
    save(spec, out)
    return {s: {"x": case_x, "y": case_y, "rows": rows, "fiche": False} for s in spec["screens"]}


def build_top(spec):
    """A window whose upper part is already drawn as a thing of its own (Fish 'N' Ships' and Hybrid Aquatic's boats: stone
    and planks): that part is kept, the inventory under it becomes the case. `split`: the screen draws the texture's rows
    from `from_v` on at window height `at_y`, and its columns from `u0` on at window x 0."""
    img = read_texture(spec)
    u0, from_v, at_y = spec["split"]
    w, h = spec["size"]
    region = img[from_v:, u0:u0 + w]
    wells = find_wells(np.ascontiguousarray(region), w, img.shape[0] - from_v)
    inv_x, inv_y = inventory_of(wells)
    out = img.astype(np.float32)
    out[from_v:, u0:u0 + w] = 0
    save(spec, out)
    entry = {"x": inv_x - 8, "y": at_y + inv_y - 31, "rows": 0, "fiche": False}
    if spec.get("untitled"):
        entry["title"] = False
    return {s: dict(entry) for s in spec["screens"]}


def save(spec, out):
    result = Image.fromarray(np.clip(np.rint(out), 0, 255).astype(np.uint8), "RGBA")
    ns = spec["tex"].split("/")[1]
    dest = KUBEJS / ns / spec["tex"].split("/", 2)[2]
    dest.parent.mkdir(parents=True, exist_ok=True)
    result.save(dest)


def button(img, state):
    """A grey button sprite as a card of paper: its face (the commonest grey) the card, lighter greys its lit edge, darker
    ones its shaded edge or, much darker, the ink of its pictogram and outline; colours kept. `state`: "", "highlighted"
    (the card a shade lighter, its outline in full ink) or "disabled" (all in faint ink)."""
    a = img[:, :, 3] > 0
    rgb = img[:, :, :3].astype(int)
    lum = (0.299 * rgb[:, :, 0] + 0.587 * rgb[:, :, 1] + 0.114 * rgb[:, :, 2])
    sat = rgb.max(axis=2) - rgb.min(axis=2)
    # Vanilla's cold blue-grey of a hovered button is a grey here too.
    sat = np.where(rgb[:, :, 2] > rgb[:, :, 0] + 10, 0, sat)
    grey = a & (sat <= 40)
    if not grey.any():
        return img
    vals, counts = np.unique(np.round(lum[grey]).astype(int), return_counts=True)
    face = vals[np.argmax(counts)]
    out = img.copy()
    card = PAPER if state == "highlighted" else PAPER_SHADE
    for y, x in zip(*np.nonzero(a)):
        if sat[y, x] > 40:
            continue
        d = lum[y, x] - face
        if abs(d) <= 6:
            c = card
        elif d > 0:
            c = RIM_LIGHT
        elif d > -45:
            c = RIM_DARK
        elif lum[y, x] < 70 or state == "highlighted":
            c = INK
        else:
            c = INK_SOFT
        if state == "disabled" and c in (INK, INK_SOFT):
            c = INK_FAINT
        out[y, x, :3] = c
    return out


def buttons():
    count = 0
    for jar, folder, start in BUTTONS:
        with zipfile.ZipFile(jar_for(jar)) as z:
            for name in z.namelist():
                base = name[len(folder):] if name.startswith(folder) else None
                if not base or "/" in base or not base.endswith(".png") or not base.startswith(start):
                    continue
                img = np.asarray(Image.open(io.BytesIO(z.read(name))).convert("RGBA"), dtype=np.uint8).copy()
                state = "highlighted" if "highlighted" in base else ("disabled" if "disabled" in base else "")
                dest = KUBEJS / name.split("/", 2)[1] / name.split("/", 2)[2]
                dest.parent.mkdir(parents=True, exist_ok=True)
                Image.fromarray(button(img, state), "RGBA").save(dest)
                count += 1
    return count


# Vanilla's buttons and text fields, in a window that is a sheet: the `fiches` module draws these sprites in their place
# (fallow:fiche/...): the buttons cards of the paper with a line of ink, the fields wells pressed into it, nine-sliced as
# vanilla's (200 x 20).
CARDS = KUBEJS / "fallow/textures/gui/sprites/fiche"


def card(w, h, state):
    """A card of the books' paper, w x h: a line of ink round it (soft; full when highlighted; faint when disabled), corners
    notched, lit above and on the left, shaded below and on the right. A text field is a well pressed into the sheet."""
    out = np.zeros((h, w, 4), np.float32)
    if state.startswith("field"):
        draw_well(out, 1, 1, w - 2, h - 2)
        if state == "field_highlighted":
            for x in range(1, w - 1):
                out[0, x] = (*INK_SOFT, 255)
                out[h - 1, x] = (*INK_SOFT, 255)
            for y in range(1, h - 1):
                out[y, 0] = (*INK_SOFT, 255)
                out[y, w - 1] = (*INK_SOFT, 255)
        return out
    line = {"": INK_SOFT, "highlighted": INK, "disabled": INK_FAINT}[state]
    face = PAPER if state == "highlighted" else PAPER_SHADE
    for y in range(h):
        for x in range(w):
            edge = x in (0, w - 1) or y in (0, h - 1)
            if edge and x in (0, w - 1) and y in (0, h - 1):
                continue
            if edge:
                c = line
            elif state != "disabled" and (y == 1 or x == 1):
                c = RIM_LIGHT
            elif state != "disabled" and (y == h - 2 or x == w - 2):
                c = RIM_DARK
            else:
                c = face
            out[y, x] = (*c, 255)
    return out


# Whole textures of greys a window draws over its sheet (Exposure's film laid in the lightroom): each grey to a colour of the
# paper. (jar, texture, {grey: colour}).
OVERLAYS = [
    ("exposure", "assets/exposure/textures/gui/lightroom_film_overlays.png",
     {105: INK_SOFT, 132: INK_FAINT, 172: WELL_SHADE, 230: RIM_LIGHT}),
]


def overlays():
    for jar, tex, colours in OVERLAYS:
        with zipfile.ZipFile(jar_for(jar)) as z:
            img = np.asarray(Image.open(io.BytesIO(z.read(tex))).convert("RGBA"), dtype=np.uint8).copy()
        for grey, c in colours.items():
            m = (img[:, :, 3] > 0) & (img[:, :, 0] == grey) & (img[:, :, 1] == grey) & (img[:, :, 2] == grey)
            img[m, :3] = c
        dest = KUBEJS / tex.split("/", 2)[1] / tex.split("/", 2)[2]
        dest.parent.mkdir(parents=True, exist_ok=True)
        Image.fromarray(img, "RGBA").save(dest)
    return len(OVERLAYS)


def cards():
    CARDS.mkdir(parents=True, exist_ok=True)
    meta = {"gui": {"scaling": {"type": "nine_slice", "width": 200, "height": 20, "border": 3}}}
    # A field keeps its rim and the shade inside it (vanilla's border is 1).
    field_meta = {"gui": {"scaling": {"type": "nine_slice", "width": 200, "height": 20, "border": 2}}}
    names = {"button": "", "button_highlighted": "highlighted", "button_disabled": "disabled",
             "text_field": "field", "text_field_highlighted": "field_highlighted"}
    for name, state in names.items():
        Image.fromarray(np.clip(np.rint(card(200, 20, state)), 0, 255).astype(np.uint8), "RGBA").save(CARDS / f"{name}.png")
        m = field_meta if state.startswith("field") else meta
        (CARDS / f"{name}.png.mcmeta").write_text(json.dumps(m, indent=2) + "\n")
    return len(names)


def main():
    screens = {}
    for i, spec in enumerate(WINDOWS):
        kind = spec.get("kind", "fiche")
        if kind == "coffre":
            screens.update(build_chest(spec, zlib.crc32(spec["tex"].encode()) % 100000))
        elif kind == "dessus":
            screens.update(build_top(spec))
        else:
            screens.update(build(spec, zlib.crc32(spec["tex"].encode()) % 100000))
    OUT_JSON.write_text(json.dumps({"screens": screens}, indent=2, sort_keys=True) + "\n")
    print(f"{len(WINDOWS)} fenêtres, {len(screens)} écrans, {buttons()} boutons, {cards()} cartes, {overlays()} calques")


if __name__ == "__main__":
    main()
