"""Covers of the books read in the world: the recipe books of the stations (Hands-On Vanilla's crafting table and Fallow's
tannery and forge), the lectern's book and the awakening book of the enchanting table.

Each texture follows the layout of the model it dresses (64 x 32 units, Hands-On Vanilla's models with their boards cut to the
pages by Fallow's livres/client/BookShapes: half a pixel beyond the pages all round) but is drawn S times finer, as fine as the paper laid on the pages, so the leather does not look coarse beside
it. Every board is leather: a fine grain and a few softer clouds, the edges rubbed paler, the very edge dark, the corners
rounded (cut out), a double fillet tooled blind a little inside the edge and a small ornament at its inner corners and in the
middle (shown when the book is shut; open, the pages cover all but the rubbed rim). Every book has the same metal corners on
its outer corners, a rivet set in the bare rim so they read open as well as shut: brass on the green crafting book and the
lectern's brown one, aged bronze on the tan workshop books, gold on the night-black awakening book, which also has a
gold sigil (the lectern's and the awakening book's boards also have a gilt fillet). The spine has raised bands and headbands of
the leather's own colours, plain gold thread on the awakening book. The blocks of pages show the edges of the leaves, in fine
lines. Everything is drawn from a fixed seed, so a rerun gives the same files.

Run from the repository root: python3 scripts/books/covers.py
Writes:
  pack/kubejs/assets/handson_vanilla/textures/entity/recipe_book.png
  pack/kubejs/assets/handson_vanilla/textures/entity/enchanting_book.png
  mods-custom/fallow/src/main/resources/assets/fallow/textures/entity/livre_de_recettes.png
  mods-custom/fallow/src/main/resources/assets/fallow/textures/entity/livre_d_eveil.png
"""

from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
FALLOW = ROOT / "mods-custom/fallow/src/main/resources/assets/fallow/textures/entity"
HOV = ROOT / "pack/kubejs/assets/handson_vanilla/textures/entity"


def smooth_noise(rng, w, h, cell):
    """Value noise: a coarse random grid blown up smoothly, from -1 to 1."""
    gw, gh = w // cell + 3, h // cell + 3
    grid = rng.uniform(-1.0, 1.0, (gh, gw)).astype(np.float32)
    img = Image.fromarray(((grid + 1.0) * 127.5).astype(np.uint8), "L").resize((gw * cell, gh * cell), Image.BICUBIC)
    return np.asarray(img, dtype=np.float32)[cell:cell + h, cell:cell + w] / 127.5 - 1.0


def shade(rgb, k):
    """Darker (k < 1) or lighter (k > 1), keeping in range."""
    return np.clip(np.asarray(rgb, np.float32) * k, 0.0, 255.0)


class Sheet:
    """The whole texture, in floats, S texels to a unit of the model."""

    def __init__(self, s, seed):
        self.s = s
        self.w, self.h = 64 * s, 32 * s
        self.rgb = np.zeros((self.h, self.w, 3), np.float32)
        self.alpha = np.zeros((self.h, self.w), np.float32)
        self.rng = np.random.default_rng(seed)

    def box(self, u, v, w, h):
        """Texel bounds of a face given in units of the model."""
        s = self.s
        return int(round(u * s)), int(round(v * s)), max(1, int(round(w * s))), max(1, int(round(h * s)))

    def put(self, x, y, img, alpha):
        hh, ww = img.shape[:2]
        self.rgb[y:y + hh, x:x + ww] = img
        self.alpha[y:y + hh, x:x + ww] = alpha

    def image(self):
        rgb = np.clip(np.rint(self.rgb), 0, 255).astype(np.uint8)
        a = np.clip(np.rint(self.alpha * 255.0), 0, 255).astype(np.uint8)
        return Image.fromarray(np.dstack([rgb, a]), "RGBA")


class Leather:
    """The colours of a leather: its body, its rubbed edge, its dark cut edge, and its tooling."""

    def __init__(self, body, rubbed, cut, metal=None, star=False, ornament=True, gilt=False):
        self.body = np.asarray(body, np.float32)
        self.rubbed = np.asarray(rubbed, np.float32)
        self.cut = np.asarray(cut, np.float32)
        self.metal = metal  # (light, mid, dark) of brass or gilt corners, or None for tooled ones
        self.star = star
        self.ornament = ornament
        self.gilt = gilt  # a gilt fillet inside the edge, for the finer bindings


def leather_ground(rng, w, h, s, leather):
    """The leather's surface: soft clouds, a fine pebbled grain and a few darker pores."""
    clouds = smooth_noise(rng, w, h, max(4, 2 * s)) * 0.6 + smooth_noise(rng, w, h, max(3, s)) * 0.4
    grain = smooth_noise(rng, w, h, 2) * 0.7 + rng.normal(0.0, 0.5, (h, w)).astype(np.float32) * 0.3
    k = 1.0 + clouds * 0.07 + grain * 0.045
    rgb = leather.body[None, None, :] * k[:, :, None]
    pores = rng.random((h, w)) < 0.025
    rgb[pores] *= 0.88
    return rgb


def board(rng, w, h, s, leather, corner_radius=None, outer=(0, 1, 2, 3)):
    """One face of a board, w x h texels: leather, rubbed paler towards the edge, its last texel dark, the corners cut round,
    a double fillet tooled blind half a unit in, and the ornaments. Corners are numbered 0 top left, 1 top right, 2 bottom left,
    3 bottom right in the texture; `outer`: those away from the spine, the only ones a metal corner goes on."""
    rgb = leather_ground(rng, w, h, s, leather)
    alpha = np.ones((h, w), np.float32)
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    edge = np.minimum(np.minimum(xs, w - 1 - xs), np.minimum(ys, h - 1 - ys))
    # Rubbed edges: paler over a third of a unit, unevenly.
    reach = s * 0.34 * (1.0 + 0.5 * smooth_noise(rng, w, h, max(3, s // 2)))
    rub = np.clip(1.0 - edge / np.maximum(reach, 1.0), 0.0, 1.0) ** 1.5 * 0.55
    rgb = rgb * (1.0 - rub[:, :, None]) + leather.rubbed[None, None, :] * rub[:, :, None]
    # The cut edge.
    rgb[edge < 1.0] = rgb[edge < 1.0] * 0.35 + leather.cut * 0.65
    # Rounded corners, cut out.
    r = corner_radius if corner_radius is not None else max(2.0, s * 0.3)
    for cx, cy in ((r, r), (w - 1 - r, r), (r, h - 1 - r), (w - 1 - r, h - 1 - r)):
        inx = (xs < r) if cx == r else (xs > w - 1 - r)
        iny = (ys < r) if cy == r else (ys > h - 1 - r)
        d = np.hypot(xs - cx, ys - cy)
        corner = inx & iny
        alpha[corner & (d > r + 0.35)] = 0.0
        rim = corner & (d > r - 0.75) & (d <= r + 0.35)
        rgb[rim] = rgb[rim] * 0.35 + leather.cut * 0.65
    # The double fillet: a pressed line (dark) with the light catching its far lip, twice, two texels apart.
    inset = int(round(s * 0.5))
    for k, strength in ((inset, 0.30), (inset + 2, 0.18)):
        fillet(rgb, k, w, h, strength)
    if leather.gilt:
        # Bound to be shown: the inner fillet gilt, a fine line of the corners' metal.
        gilt(rgb, inset + 2, w, h, leather.metal)
    if leather.metal is None:
        if leather.ornament:
            for cx, cy in ((inset, inset), (w - 1 - inset, inset), (inset, h - 1 - inset), (w - 1 - inset, h - 1 - inset)):
                fleuron(rgb, cx, cy, s)
    else:
        if leather.ornament:
            for corner in [c for c in range(4) if c not in outer]:
                cx = inset if corner in (0, 2) else w - 1 - inset
                cy = inset if corner in (0, 1) else h - 1 - inset
                fleuron(rgb, cx, cy, s)
        for corner in outer:
            metal_corner(rgb, alpha, w, h, s, corner, leather.metal, leather.star)
    if leather.ornament:
        centre(rgb, w // 2, h // 2, s, leather)
    return rgb, alpha


def fillet(rgb, k, w, h, strength):
    """A blind-tooled rectangle k texels inside the edge: the groove darker, its outer lip lighter."""
    x0, y0, x1, y1 = k, k, w - 1 - k, h - 1 - k
    if x1 - x0 < 4 or y1 - y0 < 4:
        return
    groove = 1.0 - strength
    lip = 1.0 + strength * 0.45
    rgb[y0, x0:x1 + 1] *= groove
    rgb[y1, x0:x1 + 1] *= groove
    rgb[y0 + 1:y1, x0] *= groove
    rgb[y0 + 1:y1, x1] *= groove
    if y0 - 1 >= 0:
        rgb[y0 - 1, x0:x1 + 1] = np.minimum(rgb[y0 - 1, x0:x1 + 1] * lip, 255.0)
    rgb[y1 + 1 if y1 + 1 < h else y1, x0:x1 + 1] = np.minimum(rgb[y1 + 1 if y1 + 1 < h else y1, x0:x1 + 1] * lip, 255.0)
    if x0 - 1 >= 0:
        rgb[y0:y1 + 1, x0 - 1] = np.minimum(rgb[y0:y1 + 1, x0 - 1] * lip, 255.0)
    rgb[y0:y1 + 1, x1 + 1 if x1 + 1 < w else x1] = np.minimum(rgb[y0:y1 + 1, x1 + 1 if x1 + 1 < w else x1] * lip, 255.0)


def gilt(rgb, k, w, h, metal):
    """A gilt rectangle k texels inside the edge: the metal laid in the groove, a touch darker on its lower and right sides."""
    light, mid, dark = (np.asarray(c, np.float32) for c in metal)
    x0, y0, x1, y1 = k, k, w - 1 - k, h - 1 - k
    if x1 - x0 < 4 or y1 - y0 < 4:
        return
    lit = mid * 0.7 + light * 0.3
    shaded = mid * 0.75 + dark * 0.25
    rgb[y0, x0:x1 + 1] = rgb[y0, x0:x1 + 1] * 0.2 + lit * 0.8
    rgb[y0:y1 + 1, x0] = rgb[y0:y1 + 1, x0] * 0.2 + lit * 0.8
    rgb[y1, x0:x1 + 1] = rgb[y1, x0:x1 + 1] * 0.25 + shaded * 0.75
    rgb[y0:y1 + 1, x1] = rgb[y0:y1 + 1, x1] * 0.25 + shaded * 0.75


def press(rgb, x, y, k):
    h, w = rgb.shape[:2]
    if 0 <= x < w and 0 <= y < h:
        rgb[y, x] = np.clip(rgb[y, x] * k, 0.0, 255.0)


def fleuron(rgb, cx, cy, s):
    """Where the fillets turn: a small tooled lozenge and four dots round it."""
    r = max(2, s // 5)
    for dy in range(-r, r + 1):
        for dx in range(-r, r + 1):
            if abs(dx) + abs(dy) <= r:
                press(rgb, cx + dx, cy + dy, 0.74 if abs(dx) + abs(dy) == r else 0.86)
    for dx, dy in ((r + 2, 0), (-r - 2, 0), (0, r + 2), (0, -r - 2)):
        press(rgb, cx + dx, cy + dy, 0.78)


def centre(rgb, cx, cy, s, leather):
    """The middle of the board: a tooled lozenge with a smaller one inside, and a dot; on the awakening book, its gilt seal."""
    if leather.star and leather.metal is not None:
        sigil(rgb, cx, cy, s, leather.metal)
        return
    for r, k in ((int(s * 1.1), 0.76), (int(s * 0.7), 0.82)):
        for t in range(-r, r + 1):
            for x, y in ((cx + t, cy - (r - abs(t)) * 3 // 2), (cx + t, cy + (r - abs(t)) * 3 // 2)):
                press(rgb, x, y, k)
                press(rgb, x, y + 1, 1.0 + (1.0 - k) * 0.4)
    press(rgb, cx, cy, 0.7)
    press(rgb, cx + 1, cy, 0.8)
    press(rgb, cx, cy + 1, 0.8)


def metal_corner(rgb, alpha, w, h, s, corner, metal, star):
    """A corner piece of metal folded over the board, the same on every book: a triangle along the two edges, its legs a pixel
    and a tenth of the model long, its inner edge one clean slant (a texel of the metal's shade, then a texel of bevel), lit along
    its top and left, a fine line engraved along the slant, and a rivet set in the rim the pages leave bare, so the piece reads
    when the book lies open (half a pixel of board shows round the pages) as well as shut. No outline: the metal meets the
    leather by its own shade. The awakening book's rivet is a small star."""
    light, mid, dark = (np.asarray(c, np.float32) for c in metal)
    size = int(round(s * 1.1))
    for dy in range(size + 1):
        for dx in range(size + 1):
            d = dx + dy
            if d > size:
                continue
            x = dx if corner in (0, 2) else w - 1 - dx
            y = dy if corner in (0, 1) else h - 1 - dy
            if alpha[y, x] == 0.0:
                continue
            t = d / size
            c = mid * (1.0 - 0.22 * t) + light * 0.22 * (1.0 - t)
            lit_x = dx == 1 and corner in (0, 2)
            lit_y = dy == 1 and corner in (0, 1)
            if d == size:
                c = mid * 0.45 + dark * 0.55
            elif d == size - 1:
                c = mid * 0.88
            elif d == size - 4:
                c = mid * 0.7 + dark * 0.3
            elif dx == 0 or dy == 0:
                c = mid * 0.78 + dark * 0.22
            elif lit_x or lit_y:
                c = light
            rgb[y, x] = np.clip(c, 0.0, 255.0)
    # The rivet, in the bare rim: a quarter of a pixel in from both edges.
    rx = max(2, int(round(s * 0.25)))
    x = rx if corner in (0, 2) else w - 1 - rx
    y = rx if corner in (0, 1) else h - 1 - rx
    if star:
        for i in (-1, 0, 1):
            rgb[y, x + i] = light
            rgb[y + i, x] = light
        rgb[y, x] = np.asarray((255, 246, 210), np.float32)
    else:
        sx = 1 if corner in (0, 2) else -1
        sy = 1 if corner in (0, 1) else -1
        rgb[y, x] = light
        rgb[y, x + sx] = mid
        rgb[y + sy, x] = mid
        rgb[y + sy, x + sx] = dark


def sigil(rgb, cx, cy, s, metal):
    """The awakening book's seal, gilt in the middle of its boards: a ring, an eight-pointed star inside it, and a dot at its
    heart."""
    light, mid, dark = (np.asarray(c, np.float32) for c in metal)
    h, w = rgb.shape[:2]
    r = s * 1.15
    for y in range(int(cy - r - 2), int(cy + r + 3)):
        for x in range(int(cx - r - 2), int(cx + r + 3)):
            if not (0 <= x < w and 0 <= y < h):
                continue
            d = np.hypot(x - cx, y - cy)
            if abs(d - r) < 0.55:
                rgb[y, x] = rgb[y, x] * 0.15 + (light if y < cy else mid) * 0.85
            elif abs(d - r) < 1.1 and d > r:
                rgb[y, x] = rgb[y, x] * 0.85
    for k in range(8):
        ang = k * np.pi / 4.0
        reach = r * (0.78 if k % 2 == 0 else 0.48)
        for i in range(int(reach) + 1):
            x = int(round(cx + np.cos(ang) * i))
            y = int(round(cy + np.sin(ang) * i))
            if 0 <= x < w and 0 <= y < h:
                rgb[y, x] = rgb[y, x] * 0.1 + (light if i < reach * 0.5 else mid) * 0.9
    cxi, cyi = int(round(cx)), int(round(cy))
    rgb[cyi, cxi] = np.asarray((255, 246, 210), np.float32)


def spine(rng, w, h, s, leather, bands=4):
    """The spine, w x h texels, along its height: leather a shade darker, with raised bands (a light ridge over a dark
    shadow) and a tooled line either side of each, and at both ends the headbands: the little rolls of red and cream silk sewn
    at the head and the tail of the leaves, which show between the pages at the top and the foot of the fold: silk of the leather's
    own colour and of the leaves' edge, so they belong to the book."""
    rgb = leather_ground(rng, w, h, s, leather) * 0.94
    alpha = np.ones((h, w), np.float32)
    cap = max(3, int(round(s * 0.5)))
    inner = h - 2 * cap
    for i in range(bands):
        y = cap + int(round(inner * (i + 1) / (bands + 1)))
        for dy, k in ((-2, 0.86), (-1, 1.16), (0, 1.22), (1, 0.82), (2, 0.9)):
            if 0 <= y + dy < h:
                rgb[y + dy] = np.clip(rgb[y + dy] * k, 0.0, 255.0)
        if leather.metal is not None:
            # A gilt line either side of the band.
            mid = np.asarray(leather.metal[1], np.float32)
            for dy in (-4, 4):
                if 0 <= y + dy < h:
                    rgb[y + dy, 1:-1] = rgb[y + dy, 1:-1] * 0.25 + mid * 0.75
    rgb[:, 0] = rgb[:, 0] * 0.6
    rgb[:, -1] = rgb[:, -1] * 0.6
    red, cream = leather.rubbed * 0.9, leather.rubbed * 0.5 + np.asarray((214, 186, 136), np.float32) * 0.5
    if leather.star and leather.metal is not None:
        # The awakening book's headbands: gold thread.
        red, cream = np.asarray(leather.metal[1], np.float32), leather.body * 1.2
    for end in (0, 1):
        for j in range(cap):
            y = j if end == 0 else h - 1 - j
            if j == 0:
                rgb[y] = leather.cut
                continue
            if j == cap - 1:
                # Its shadow on the spine.
                rgb[y] = rgb[y] * 0.62
                continue
            # A bead: lit along its middle, the silk twisted in short slanting stitches.
            t = (j - 1) / max(1, cap - 3)
            k = 0.78 + 0.36 * np.sin(np.pi * t)
            if leather.star and leather.metal is not None:
                # Gold thread wound smooth, not twisted: a gilt roll, lit along its middle, no stitches to read as stripes.
                rgb[y] = np.clip(red * k, 0.0, 255.0)
                continue
            for x in range(w):
                c = red if ((x + (j if end == 0 else -j)) // 2) % 2 else cream
                rgb[y, x] = np.clip(c * k, 0.0, 255.0)
    return rgb, alpha


def leaves(rng, w, h, along_x, paper, edge):
    """The edges of the leaves of a block of pages, w x h texels: fine lines along the leaves (along x or y), each a shade
    lighter or darker than the next, darker at the two faces of the block."""
    across = h if along_x else w
    lines = np.empty(across, np.float32)
    for i in range(across):
        lines[i] = 1.05 + rng.normal(0.0, 0.035) - (0.10 if i % 2 else 0.0)
    if along_x:
        k = np.repeat(lines[:, None], w, axis=1)
    else:
        k = np.repeat(lines[None, :], h, axis=0)
    k *= 1.0 + smooth_noise(rng, w, h, 6) * 0.03
    # The leaves' edges are warmer than the page, aged where the air reaches them.
    warm = np.asarray(paper, np.float32) * 0.62 + np.asarray(edge, np.float32) * 0.38
    rgb = warm[None, None, :] * k[:, :, None]
    # Darker towards the boards on both faces of the block: the leaves there are older.
    t = np.linspace(0.0, 1.0, across)
    side = np.clip(1.0 - np.minimum(t, 1.0 - t) * 6.0, 0.0, 1.0) * 0.35
    side = side[:, None] if along_x else side[None, :]
    rgb = rgb * (1.0 - side[:, :, None]) + np.asarray(edge, np.float32)[None, None, :] * side[:, :, None]
    return rgb, np.ones((h, w), np.float32)


def page_face(rng, w, h, paper, edge):
    """The top leaf of a block, under the paper the books lay on it: plain paper with a soft edge."""
    rgb = np.asarray(paper, np.float32)[None, None, :] * (1.0 + smooth_noise(rng, w, h, 6) * 0.03)[:, :, None]
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    d = np.minimum(np.minimum(xs, w - 1 - xs), np.minimum(ys, h - 1 - ys))
    t = np.clip(1.0 - d / 3.0, 0.0, 1.0) * 0.4
    rgb = rgb * (1.0 - t[:, :, None]) + np.asarray(edge, np.float32)[None, None, :] * t[:, :, None]
    return rgb, np.ones((h, w), np.float32)


def faces(u, v, w, h, d):
    """Where Minecraft's cubes take each face from: name -> (u, v, width, height), in units."""
    return {
        "up": (u + d, v, w, d), "down": (u + d + w, v, w, d),
        "west": (u, v + d, d, h), "north": (u + d, v + d, w, h),
        "east": (u + d + w, v + d, d, h), "south": (u + d + w + d, v + d, w, h),
    }


def block_of_pages(sheet, u, v, paper, edge):
    """A block of pages 6 x 8 x 1 at (u, v): its two faces paper, its four edges leaves."""
    for name, (fu, fv, fw, fh) in faces(u, v, 6, 8, 1).items():
        x, y, w, h = sheet.box(fu, fv, fw, fh)
        if name in ("north", "south"):
            img, a = page_face(sheet.rng, w, h, paper, edge)
        elif name in ("up", "down"):
            img, a = leaves(sheet.rng, w, h, True, paper, edge)
            if name == "down":
                # Turned from the light, this face is drawn at half light: painted paler, it keeps the leaves' colour.
                img = np.clip(img * 1.25, 0.0, 255.0)
        else:
            img, a = leaves(sheet.rng, w, h, False, paper, edge)
            if name == "east":
                # The side along the spine, also drawn at part light and seen edge on at the head of the fold.
                img = np.clip(img * 1.6, 0.0, 255.0)
        sheet.put(x, y, img, a)


def face(sheet, u, v, w, h, img, a, pad=(True, True, True, True)):
    """Lays a face of w x h units at (u, v) and repeats its edge texels once round it (top, bottom, left, right where `pad`),
    in the same colours: the filtering reaches a texel past the net at its rims, and a model's thin sides take those texels."""
    x, y = int(round(u * sheet.s)), int(round(v * sheet.s))
    hh, ww = img.shape[:2]
    sheet.put(x, y, img, a)
    top, bottom, left, right = pad
    if top and y > 0:
        sheet.rgb[y - 1, x:x + ww], sheet.alpha[y - 1, x:x + ww] = img[0], a[0]
    if bottom and y + hh < sheet.h:
        sheet.rgb[y + hh, x:x + ww], sheet.alpha[y + hh, x:x + ww] = img[-1], a[-1]
    if left and x > 0:
        sheet.rgb[y:y + hh, x - 1], sheet.alpha[y:y + hh, x - 1] = img[:, 0], a[:, 0]
    if right and x + ww < sheet.w:
        sheet.rgb[y:y + hh, x + ww], sheet.alpha[y:y + hh, x + ww] = img[:, -1], a[:, -1]


# The boards' sizes, in units of the model (Fallow's livres/client/BookShapes): half a pixel beyond the pages of 6 x 8 all
# round; the recipe book's boards run an eighth past their hinge and are a whole number of texels wide (80 at 12 to a unit).
# The spines are the boards' height, their headbands showing between the pages at the head and the foot of the fold.
RECIPE_BOARD = (80 / 12, 9.0)
LID = (6.5, 9.0)
SPINE_HIGH = 9.0


def recipe_book(path, s, seed, leather, paper=(242, 232, 206), edge=(200, 172, 124)):
    """Hands-On Vanilla's recipe book model as Fallow shapes it (and Fallow's copy): boards 6.67 x 9 at (0, 1) and (14, 1),
    both faces side by side; the spine (a thin box at (28, 0) and two planes at (0, 12) and (6, 12)), 9 high; blocks of pages
    at (34, 0) and (49, 0); the turning leaves at (34, 10) and (49, 10)."""
    sheet = Sheet(s, seed)
    bw, bh = RECIPE_BOARD
    for u0 in (0, 14):
        for i, u in enumerate((u0, u0 + bw)):
            w, h = int(round(bw * s)), int(round(bh * s))
            # The spine is at the boards' right end: on the face first in the texture, the outer corners are its left ones.
            img, a = board(sheet.rng, w, h, s, leather, outer=(0, 2) if i == 0 else (1, 3))
            face(sheet, u, 1, w, h, img, a, pad=(True, True, i == 0, i == 1))
    # The spine: its patches plain leather, then the banded faces, each 9 units along: the box's two sides at (28, 2), the
    # planes' at (0, 14) and (6, 13).
    for (u, v, w, h) in ((28, 0, 6, 12), (0, 12, 9, 12)):
        x, y, ww, hh = sheet.box(u, v, w, h)
        sheet.put(x, y, leather_ground(sheet.rng, ww, hh, s, leather) * 0.94, 1.0)
    for (u, v, w) in ((28, 2, 2), (30.25, 2, 2), (0, 14, 2), (2, 14, 2), (6, 13, 1), (7, 13, 1)):
        x, y, ww, hh = sheet.box(u, v, w, SPINE_HIGH)
        img, a = spine(sheet.rng, ww, hh, s, leather, bands=3)
        sheet.put(x, y, img, a)
    for u in (34, 49):
        block_of_pages(sheet, u, 0, paper, edge)
        for fu in (u, u + 6):
            x, y, w, h = sheet.box(fu, 10, 6, 8)
            img, a = page_face(sheet.rng, w, h, paper, edge)
            sheet.put(x, y, img, a)
    sheet.image().save(path)


def lectern_book(path, s, seed, leather, paper=(242, 232, 206), edge=(200, 172, 124)):
    """Hands-On Vanilla's enchanting book model as Fallow shapes it (the lectern's, and Fallow's awakening book): lids 6.5 x 9,
    a hundredth thick, at (0, 0) and (19, 0), both faces side by side; the spine 2 x 9 at (14, 0), both faces; blocks of
    pages at (0, 10) and (14, 10)."""
    sheet = Sheet(s, seed)
    lw, lh = LID
    # Which side of each face is the spine (found on the lectern): the left lid's inner face has it on its right, its outer
    # face on its left; the right lid the other way round.
    right_side, left_side = (0, 2), (1, 3)
    for u0, outers in ((0, (right_side, left_side)), (19, (left_side, right_side))):
        for i, (u, outer) in enumerate(zip((u0, u0 + lw), outers)):
            w, h = int(round(lw * s)), int(round(lh * s))
            img, a = board(sheet.rng, w, h, s, leather, outer=outer)
            face(sheet, u, 0, w, h, img, a, pad=(False, True, i == 0, i == 1))
    for i, u in enumerate((14, 16)):
        x, y, w, h = sheet.box(u, 0, 2, SPINE_HIGH)
        img, a = spine(sheet.rng, w, h, s, leather, bands=3)
        face(sheet, u, 0, w, h, img, a, pad=(False, True, i == 0, i == 1))
    for u in (0, 14):
        block_of_pages(sheet, u, 10, paper, edge)
    sheet.image().save(path)


# Leathers: bottle green (Hands-On Vanilla's crafting book), natural tan calf (Fallow's workshop books), dark brown with brass
# corners (the lectern), oxblood with gilt corners (the awakening book, the red of the enchanting table's cloth).
BRASS = ((240, 206, 122), (190, 146, 66), (104, 70, 28))
BRONZE = ((226, 176, 102), (138, 92, 40), (66, 40, 16))
GOLD = ((252, 230, 156), (218, 178, 90), (124, 90, 40))
GREEN = Leather(body=(38, 84, 62), rubbed=(74, 116, 88), cut=(16, 36, 26), metal=BRASS)
TAN = Leather(body=(150, 98, 56), rubbed=(186, 136, 88), cut=(70, 42, 22), metal=BRONZE)
BROWN = Leather(body=(104, 62, 36), rubbed=(140, 94, 60), cut=(44, 24, 12), metal=BRASS, gilt=True)
# The awakening book: a leather near black, a breath of plum in it (the table's world apart), all its tooling gilt, a seal of
# gold in the middle of its boards and stars for its rivets.
NIGHT = Leather(body=(30, 24, 27), rubbed=(60, 50, 54), cut=(10, 8, 9), metal=GOLD, star=True, gilt=True)


def main():
    HOV.mkdir(parents=True, exist_ok=True)
    FALLOW.mkdir(parents=True, exist_ok=True)
    recipe_book(HOV / "recipe_book.png", 12, 41, GREEN)
    recipe_book(FALLOW / "livre_de_recettes.png", 12, 43, TAN)
    lectern_book(HOV / "enchanting_book.png", 16, 47, BROWN)
    lectern_book(FALLOW / "livre_d_eveil.png", 16, 53, NIGHT, paper=(226, 206, 164), edge=(158, 116, 70))


if __name__ == "__main__":
    main()
