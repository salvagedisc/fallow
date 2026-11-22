"""Covers of the books read in the world: the recipe books of the stations (Hands-On Vanilla's crafting table and Fallow's
tannery and forge), the lectern's book and the awakening book of the enchanting table.

Each texture keeps the layout of the model it dresses (64 x 32 texels of Hands-On Vanilla's models, the faces where its
cubes put them) but is drawn S times finer, as fine as the paper laid on the pages, so the leather does not look coarse beside
it. Every board is leather: a fine grain and a few softer clouds, the edges rubbed paler, the very edge dark, the corners
rounded (cut out), a double fillet tooled blind a little inside the edge (it frames the pages when the book lies open) and a
small ornament at each of its corners and in the middle. The lectern's book has brass corners instead of the tooled ones, the
awakening book gilt corners with a star. The spine has raised bands. The blocks of pages show the edges of the leaves, in fine
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

    def __init__(self, body, rubbed, cut, metal=None, star=False, ornament=True):
        self.body = np.asarray(body, np.float32)
        self.rubbed = np.asarray(rubbed, np.float32)
        self.cut = np.asarray(cut, np.float32)
        self.metal = metal  # (light, mid, dark) of brass or gilt corners, or None for tooled ones
        self.star = star
        self.ornament = ornament


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
    if leather.metal is not None:
        # Bound to be shown: the inner fillet gilt, a fine line of the corners' metal.
        gilt(rgb, inset + 2, w, h, leather.metal)
    if leather.metal is None:
        if leather.ornament:
            for cx, cy in ((inset, inset), (w - 1 - inset, inset), (inset, h - 1 - inset), (w - 1 - inset, h - 1 - inset)):
                fleuron(rgb, cx, cy, s)
    else:
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
    """The middle of the board: a tooled lozenge with a smaller one inside, and a dot (a star on the awakening book)."""
    for r, k in ((int(s * 1.1), 0.76), (int(s * 0.7), 0.82)):
        for t in range(-r, r + 1):
            for x, y in ((cx + t, cy - (r - abs(t)) * 3 // 2), (cx + t, cy + (r - abs(t)) * 3 // 2)):
                press(rgb, x, y, k)
                press(rgb, x, y + 1, 1.0 + (1.0 - k) * 0.4)
    if leather.star and leather.metal is not None:
        light, mid, dark = (np.asarray(c, np.float32) for c in leather.metal)
        for i in range(-3, 4):
            for x, y in ((cx + i, cy), (cx, cy + i)):
                rgb[y, x] = mid if abs(i) > 1 else light
        for x, y in ((cx - 1, cy - 1), (cx + 1, cy + 1), (cx + 1, cy - 1), (cx - 1, cy + 1)):
            rgb[y, x] = rgb[y, x] * 0.5 + mid * 0.5
    else:
        press(rgb, cx, cy, 0.7)
        press(rgb, cx + 1, cy, 0.8)
        press(rgb, cx, cy + 1, 0.8)


def metal_corner(rgb, alpha, w, h, s, corner, metal, star):
    """A corner piece of brass (or gilt) folded over the board: a rounded triangle along the two edges, lit from the top left,
    a rivet near its point; the awakening book's carries a small star instead of the rivet."""
    light, mid, dark = (np.asarray(c, np.float32) for c in metal)
    size = int(s * 1.05)
    for dy in range(size + 1):
        for dx in range(size + 1):
            if dx + dy > size:
                continue
            x = dx if corner in (0, 2) else w - 1 - dx
            y = dy if corner in (0, 1) else h - 1 - dy
            if alpha[y, x] == 0.0:
                continue
            t = (dx + dy) / size
            if dx + dy >= size - 1:
                c = dark
            elif dx == 0 or dy == 0:
                c = dark * 0.6 + mid * 0.4
            else:
                # Lit from the top left: paler towards that side of the piece, a highlight band near its edge.
                c = mid * (1.0 - t * 0.25) + light * (0.25 if 0.55 < t < 0.75 else 0.0)
            rgb[y, x] = np.clip(c, 0.0, 255.0)
    rx = int(size * 0.32)
    x = rx if corner in (0, 2) else w - 1 - rx
    y = rx if corner in (0, 1) else h - 1 - rx
    if star:
        for i in (-1, 0, 1):
            rgb[y, x + i] = light
            rgb[y + i, x] = light
    else:
        rgb[y, x] = light
        rgb[y + (1 if corner in (0, 1) else -1), x + (1 if corner in (0, 2) else -1)] = dark


def spine(rng, w, h, s, leather, bands=4):
    """The spine, w x h texels, along its height: leather a shade darker, with raised bands (a light ridge over a dark
    shadow) and a tooled line either side of each."""
    rgb = leather_ground(rng, w, h, s, leather) * 0.94
    alpha = np.ones((h, w), np.float32)
    for i in range(bands):
        y = int(round(h * (i + 1) / (bands + 1)))
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
    rgb = (np.asarray(paper, np.float32) * np.asarray((0.935, 0.905, 0.865), np.float32))[None, None, :] * k[:, :, None]
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
        else:
            img, a = leaves(sheet.rng, w, h, False, paper, edge)
        sheet.put(x, y, img, a)


def recipe_book(path, s, seed, leather, paper=(242, 232, 206), edge=(200, 172, 124)):
    """Hands-On Vanilla's recipe book model (and Fallow's copy): boards 6 x 10 at (1, 1) and (15, 1), both faces; the spine
    (a thin box at (28, 0) and two planes at (0, 12) and (6, 12)); blocks of pages at (34, 0) and (49, 0); the turning leaves at
    (34, 10) and (49, 10)."""
    sheet = Sheet(s, seed)
    for u in (1, 7, 15, 21):
        x, y, w, h = sheet.box(u, 1, 6, 10)
        img, a = board(sheet.rng, w, h, s, leather)
        sheet.put(x, y, img, a)
        # The board's thin sides and the texels the filtering reaches round its face: the texel row above and the column
        # outside it repeat the face's edge, in the cut colour, so the cover's rim never shows a gap.
        sheet.rgb[y - 1, x:x + w] = leather.cut
        sheet.alpha[y - 1, x:x + w] = a[0]
        sheet.rgb[y + h, x:x + w] = leather.cut
        sheet.alpha[y + h, x:x + w] = a[-1]
        outside = x - 1 if u in (1, 15) else x + w
        inside = 0 if u in (1, 15) else w - 1
        sheet.rgb[y:y + h, outside] = leather.cut
        sheet.alpha[y:y + h, outside] = a[:, inside]
    # The spine: its patches plain leather, then the banded faces, each 10 units along: the box's two sides at (28, 2), the
    # planes' at (0, 14) and (6, 13).
    for (u, v, w, h) in ((28, 0, 6, 12), (0, 12, 9, 12)):
        x, y, ww, hh = sheet.box(u, v, w, h)
        sheet.put(x, y, leather_ground(sheet.rng, ww, hh, s, leather) * 0.94, 1.0)
    for (u, v, w) in ((28, 2, 2), (30.25, 2, 2), (0, 14, 2), (2, 14, 2), (6, 13, 1), (7, 13, 1)):
        x, y, ww, hh = sheet.box(u, v, w, 10)
        img, a = spine(sheet.rng, ww, hh, s, leather)
        sheet.put(x, y, img, a)
    for u in (34, 49):
        block_of_pages(sheet, u, 0, paper, edge)
        for fu in (u, u + 6):
            x, y, w, h = sheet.box(fu, 10, 6, 8)
            img, a = page_face(sheet.rng, w, h, paper, edge)
            sheet.put(x, y, img, a)
    sheet.image().save(path)


def lectern_book(path, s, seed, leather, paper=(242, 232, 206), edge=(200, 172, 124)):
    """Hands-On Vanilla's enchanting book model (the lectern's, and Fallow's awakening book): boards 7 x 10 at (0, 0) and (18,
    0), both faces; the spine 2 x 10 at (14, 0), both faces; blocks of pages at (0, 10) and (14, 10)."""
    sheet = Sheet(s, seed)
    # Which side of each face is the spine (found on the lectern): the left board's inner face (u 0) has it on its right, its
    # outer face (u 7) on its left; the right board the other way round.
    right_side, left_side = (0, 2), (1, 3)
    for u, outer in ((0, right_side), (7, left_side), (18, left_side), (25, right_side)):
        x, y, w, h = sheet.box(u, 0, 7, 10)
        img, a = board(sheet.rng, w, h, s, leather, outer=outer)
        sheet.put(x, y, img, a)
    for u in (14, 16):
        x, y, w, h = sheet.box(u, 0, 2, 10)
        img, a = spine(sheet.rng, w, h, s, leather)
        sheet.put(x, y, img, a)
    for u in (0, 14):
        block_of_pages(sheet, u, 10, paper, edge)
    sheet.image().save(path)


# Leathers: bottle green (Hands-On Vanilla's crafting book), natural tan calf (Fallow's workshop books), dark brown with brass
# corners (the lectern), midnight violet with gilt corners and stars (the awakening book).
GREEN = Leather(body=(38, 84, 62), rubbed=(74, 116, 88), cut=(16, 36, 26))
TAN = Leather(body=(150, 98, 56), rubbed=(186, 136, 88), cut=(70, 42, 22))
BROWN = Leather(body=(104, 62, 36), rubbed=(140, 94, 60), cut=(44, 24, 12),
                metal=((246, 214, 132), (196, 152, 70), (112, 78, 30)))
VIOLET = Leather(body=(62, 42, 104), rubbed=(98, 78, 140), cut=(28, 18, 52),
                 metal=((250, 226, 150), (214, 176, 92), (128, 96, 44)), star=True)


def main():
    HOV.mkdir(parents=True, exist_ok=True)
    FALLOW.mkdir(parents=True, exist_ok=True)
    recipe_book(HOV / "recipe_book.png", 12, 41, GREEN)
    recipe_book(FALLOW / "livre_de_recettes.png", 12, 43, TAN)
    lectern_book(HOV / "enchanting_book.png", 16, 47, BROWN)
    lectern_book(FALLOW / "livre_d_eveil.png", 16, 53, VIOLET, paper=(238, 229, 212), edge=(180, 156, 132))


if __name__ == "__main__":
    main()
