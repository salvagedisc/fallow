"""Paper of the books read in the world: the recipe books of the stations (Fallow's and Hands-On Vanilla's), the lectern's
book and the awakening book of the enchanting table.

Each page is drawn texel by texel from a fixed seed, so a rerun gives the same files: soft, cloudy paper, the edges of the leaves
under it showing along its outer side, a little worn (corners rounded and darker, a few nicks, a short tear, a faint water
ring, foxing, the outer corner of the foot greyed by thumbs), shaded towards the fold. Nothing is ruled on any page: the
cover is their border, the page furniture and the ink do the rest. All the wear keeps to the margins the books leave.

The corner turned down at the foot of a page that has a next one (or a previous one) is a sprite of its own, coin_de_page.png:
the flap of paper folded over, its shadow, and the page under it; its second column is the same corner lifted a little more,
under the cursor; its second row is the lectern's and the awakening book's paper. It is drawn one texel to a unit, the scale of
the paper, the icons and the letters, as is the furniture of the recipe books' pages, livre_elements.png (tabs, the search slip,
wells, the frame round a picked one, the card of a group, the magnifier, the mark of a new recipe, the rule under a name), in
one palette for every book (brown inks, never black; the paper and its shades; the rubric red).

Run from the repository root: python3 scripts/books/papers.py
Writes:
  mods-custom/fallow/src/main/resources/assets/fallow/textures/entity/livre_de_recettes_papier.png
  mods-custom/fallow/src/main/resources/assets/fallow/textures/entity/livre_d_eveil_papier.png
  pack/kubejs/assets/handson_vanilla/textures/entity/recipe_book_paper.png
  pack/kubejs/assets/handson_vanilla/textures/entity/enchanting_book_paper.png
  mods-custom/fallow/src/main/resources/assets/fallow/textures/entity/coin_de_page.png
  mods-custom/fallow/src/main/resources/assets/fallow/textures/entity/livre_elements.png

The covers are drawn by covers.py.
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
    a = np.asarray(img, dtype=np.float32)[cell:cell + h, cell:cell + w] / 127.5 - 1.0
    return a


def mix(base, colour, amount):
    """Blends `colour` over `base` (H x W x 3 floats) by `amount` (H x W, or a number)."""
    amount = np.asarray(amount, dtype=np.float32)
    if amount.ndim == 2:
        amount = amount[:, :, None]
    return base * (1.0 - amount) + np.asarray(colour, dtype=np.float32) * amount


class Page:
    """One page, the fold on its left (x 0), drawn in floats and written as RGBA."""

    def __init__(self, w, h, seed, paper, edge, ink, fold_width):
        self.w, self.h = w, h
        self.rng = np.random.default_rng(seed)
        self.rgb = np.zeros((h, w, 3), np.float32) + np.asarray(paper, np.float32)
        self.alpha = np.full((h, w), 255, np.uint8)
        self.edge = np.asarray(edge, np.float32)
        self.ink = np.asarray(ink, np.float32)
        self.fold_width = fold_width
        ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
        self.xs, self.ys = xs, ys
        # Distance to the outer edges (top, bottom, the side away from the fold), in texels.
        self.outer = np.minimum(np.minimum(ys, h - 1 - ys), w - 1 - xs)

    def age(self, mottle=0.05, grain=0.025, yellowing=0.55, wear=4.0):
        """Uneven paper: cloudy patches, a fine grain, yellowing that grows towards the outer edges and the corners."""
        n = smooth_noise(self.rng, self.w, self.h, 12) * 0.65 + smooth_noise(self.rng, self.w, self.h, 5) * 0.35
        self.rgb *= (1.0 + n * mottle)[:, :, None]
        g = self.rng.normal(0.0, 1.0, (self.h, self.w)).astype(np.float32)
        self.rgb *= (1.0 + g * grain)[:, :, None]
        # Yellowing: strongest on the edge, fading over `wear` texels, a little uneven along the edge.
        reach = wear * (1.0 + 0.35 * smooth_noise(self.rng, self.w, self.h, 9))
        t = np.clip(1.0 - self.outer / reach, 0.0, 1.0) ** 1.6
        corner = np.clip(1.0 - (np.minimum(self.ys, self.h - 1 - self.ys) + (self.w - 1 - self.xs)) / (wear * 4.5), 0.0, 1.0)
        self.rgb = mix(self.rgb, self.edge, np.clip(t * yellowing + corner * 0.35, 0.0, 0.85))

    def fibres(self, count, darker=0.06, lighter=0.05):
        """Short fibres of the pulp, one or two texels, a shade darker or lighter: no more than the paper's grain."""
        for _ in range(count):
            x = int(self.rng.integers(2, self.w - 3))
            y = int(self.rng.integers(2, self.h - 3))
            length = int(self.rng.integers(1, 3))
            dx, dy = (1, 0) if self.rng.random() < 0.7 else (0, 1)
            k = 1.0 - darker if self.rng.random() < 0.6 else 1.0 + lighter
            for i in range(length):
                self.rgb[y + dy * i, x + dx * i] *= k

    def foxing(self, spots, strength=0.12):
        """A few round, faint brown stains, kept towards the outer margins where nothing is written."""
        for _ in range(spots):
            r = float(self.rng.uniform(1.2, 2.6))
            side = self.rng.integers(0, 3)
            if side == 0:
                cx, cy = self.rng.uniform(self.w * 0.3, self.w - 3), self.rng.uniform(1.5, 5)
            elif side == 1:
                cx, cy = self.rng.uniform(self.w * 0.3, self.w - 3), self.rng.uniform(self.h - 5, self.h - 1.5)
            else:
                cx, cy = self.rng.uniform(self.w - 5, self.w - 1.5), self.rng.uniform(6, self.h - 6)
            d = np.hypot(self.xs - cx, self.ys - cy)
            amount = np.clip(1.0 - d / r, 0.0, 1.0) ** 0.7 * strength
            self.rgb = mix(self.rgb, self.edge * 0.92, amount)

    def fold(self, depth=0.12):
        """A soft shade towards the fold: the paper bends down into the gutter there."""
        t = np.clip(1.0 - self.xs / self.fold_width, 0.0, 1.0) ** 2
        self.rgb *= (1.0 - t * depth)[:, :, None]

    def deckle(self):
        """The outermost texels: a darker, uneven edge, and the two outer corners rounded off."""
        h, w = self.h, self.w
        rim = self.edge * 0.9
        for x in range(w):
            for y in (0, h - 1):
                self.rgb[y, x] = mix(self.rgb[y, x][None, None], rim, 0.55 + 0.25 * self.rng.random())[0, 0]
        for y in range(h):
            self.rgb[y, w - 1] = mix(self.rgb[y, w - 1][None, None], rim, 0.55 + 0.25 * self.rng.random())[0, 0]
        for y in (0, h - 1):
            self.alpha[y, w - 1] = 0
            self.rgb[y, w - 2] = mix(self.rgb[y, w - 2][None, None], rim, 0.3)[0, 0]
        self.rgb[1, w - 1] = mix(self.rgb[1, w - 1][None, None], rim, 0.3)[0, 0]
        self.rgb[h - 2, w - 1] = mix(self.rgb[h - 2, w - 1][None, None], rim, 0.3)[0, 0]

    def clouds(self, strength=0.045, light_patches=5):
        """Large soft clouds in the pulp, and a few paler patches where the paper was pressed thinner."""
        n = smooth_noise(self.rng, self.w, self.h, 22) * 0.6 + smooth_noise(self.rng, self.w, self.h, 9) * 0.4
        self.rgb *= (1.0 + n * strength)[:, :, None]
        for _ in range(light_patches):
            cx, cy = self.rng.uniform(0, self.w), self.rng.uniform(0, self.h)
            r = float(self.rng.uniform(self.w * 0.12, self.w * 0.3))
            d = np.hypot((self.xs - cx) * 1.3, self.ys - cy)
            self.rgb = mix(self.rgb, np.minimum(self.rgb * 1.04, 255.0), np.clip(1.0 - d / r, 0.0, 1.0) ** 2 * 0.8)

    def leaves(self, count=3):
        """The leaves under this one, seen along its outer side: a darker hairline every other texel, each running most of the
        height with breaks where a leaf is shorter, and the outermost texel wavy, some of it bitten away."""
        h, w = self.h, self.w
        rim = self.edge * 0.88
        for k in range(count):
            x = w - 1 - 2 * k
            strength = 0.34 - 0.10 * k
            y = int(self.rng.integers(1, 4))
            while y < h - 1:
                run = int(self.rng.integers(18, 60))
                for yy in range(y, min(h - 1, y + run)):
                    self.rgb[yy, x] = mix(self.rgb[yy, x][None, None], rim, strength)[0, 0]
                y += run + int(self.rng.integers(1, 3))
        # The outermost texel: in and out by one, as a cut stack of leaves is.
        y = 0
        while y < h:
            run = int(self.rng.integers(3, 11))
            if self.rng.random() < 0.3:
                for yy in range(y, min(h, y + run)):
                    self.alpha[yy, w - 1] = 0
                    self.rgb[yy, w - 2] = mix(self.rgb[yy, w - 2][None, None], rim, 0.45)[0, 0]
            y += run
        # The head and the foot: a fine darker edge, the foot's a little stronger, in the shade of the light from above.
        for x in range(w):
            self.rgb[h - 1, x] = mix(self.rgb[h - 1, x][None, None], rim * 0.72, 0.70)[0, 0]
            self.rgb[0, x] = mix(self.rgb[0, x][None, None], rim * 0.80, 0.52)[0, 0]
        # The outer side's last texel left standing: the same fine line of shade.
        for y in range(h):
            if self.alpha[y, w - 1]:
                self.rgb[y, w - 1] = mix(self.rgb[y, w - 1][None, None], rim * 0.78, 0.55)[0, 0]

    def worn_corners(self, radius=2):
        """The two outer corners rounded off and darkened, as a corner thumbed many times."""
        h, w = self.h, self.w
        rim = self.edge * 0.8
        for cy, sy in ((0, 1), (h - 1, -1)):
            for dy in range(radius + 3):
                for dx in range(radius + 3):
                    x, y = w - 1 - dx, cy + sy * dy
                    d = np.hypot(dx, dy)
                    if dx + dy < radius:
                        self.alpha[y, x] = 0
                    elif d < radius + 3:
                        self.rgb[y, x] = mix(self.rgb[y, x][None, None], rim, 0.5 * (1.0 - d / (radius + 3)))[0, 0]

    def thumbed(self, radius, strength=0.10):
        """The outer corner of the foot, where the thumb turns the leaves: a soft greyish patch, darkest at the corner."""
        d = np.hypot(self.w - 1 - self.xs, (self.h - 1 - self.ys) * 1.2)
        n = 1.0 + 0.4 * smooth_noise(self.rng, self.w, self.h, 4)
        amount = np.clip(1.0 - d / (radius * n), 0.0, 1.0) ** 1.3 * strength
        self.rgb = mix(self.rgb, self.edge * np.asarray((0.80, 0.78, 0.78), np.float32), amount)

    def nicks(self, count):
        """Small bites out of the outer side, the head and the foot, their bottoms a shade darker."""
        h, w = self.h, self.w
        rim = self.edge * 0.82
        for _ in range(count):
            side = self.rng.integers(0, 3)
            size = int(self.rng.integers(1, 3))
            if side == 0:
                y = int(self.rng.integers(8, h - 8))
                for dy in range(size + 1):
                    self.alpha[y + dy, w - 1] = 0
                    self.rgb[y + dy, w - 2] = mix(self.rgb[y + dy, w - 2][None, None], rim, 0.6)[0, 0]
            else:
                x = int(self.rng.integers(w // 2, w - 6))
                y, sy = (0, 1) if side == 1 else (h - 1, -1)
                for dx in range(size + 1):
                    self.alpha[y, x + dx] = 0
                    self.rgb[y + sy, x + dx] = mix(self.rgb[y + sy, x + dx][None, None], rim, 0.6)[0, 0]

    def tear(self, y, length=4):
        """A short tear from the outer side, slanting in: its dark lip and, beside it, the lifted paper a shade lighter."""
        w = self.w
        for i in range(length):
            x, yy = w - 1 - i, y + i // 2
            self.rgb[yy, x] = mix(self.rgb[yy, x][None, None], self.edge * 0.62, 0.75 - 0.12 * i)[0, 0]
            self.rgb[yy + 1, x] = np.minimum(self.rgb[yy + 1, x] * 1.05, 255.0)
        self.alpha[y, w - 1] = 0

    def water_ring(self, cx, cy, radius, start, sweep, strength=0.07):
        """Part of a faint ring where a wet cup once stood: the rim of the stain a little darker, its inside a hair paler."""
        d = np.hypot(self.xs - cx, self.ys - cy)
        a = (np.arctan2(self.ys - cy, self.xs - cx) - start) % (2 * np.pi)
        on = a < sweep
        rim = np.clip(1.0 - np.abs(d - radius) / 0.9, 0.0, 1.0) * on
        inside = np.clip((radius - d) / radius, 0.0, 1.0) * on * 0.25
        self.rgb = mix(self.rgb, self.edge * 0.9, rim * strength + inside * strength)

    def image(self):
        rgb = np.clip(np.rint(self.rgb), 0, 255).astype(np.uint8)
        return Image.fromarray(np.dstack([rgb, self.alpha]), "RGBA")


def ribbon(canvas, x, colours):
    """The bookmark ribbon, 6 x 13 at the top of the texture: two folds of silk and a forked, frayed end."""
    dark, mid, light, shine = colours
    for y in range(13):
        for dx in range(6):
            c = mid
            if dx == 0 or dx == 5:
                c = dark
            elif dx == 2:
                c = light
            elif dx == 3 and y % 4 == 1:
                c = shine
            canvas.putpixel((x + dx, y), c + (255,))
    # The forked end: a notch in the middle of the last three rows, the points frayed.
    for y in range(10, 13):
        for dx in range(2, 4) if y == 10 else range(1, 5):
            canvas.putpixel((x + dx, y), (0, 0, 0, 0))
    canvas.putpixel((x + 0, 12), dark + (255,))
    canvas.putpixel((x + 5, 12), dark + (255,))


def recipe_paper(path, seed):
    """The recipe books: a page of 72 x 96 texels in the corner, the fold on the left, plain paper to its right for the slip and
    the tabs, and the ribbon and its lit twin at u 80 and 88."""
    page = Page(72, 96, seed, paper=(249, 241, 218), edge=(212, 180, 126), ink=(118, 82, 46), fold_width=8)
    page.clouds(strength=0.02, light_patches=3)
    page.age(mottle=0.0, grain=0.007, yellowing=0.50, wear=4.5)
    page.fibres(60)
    page.foxing(3, strength=0.15)
    page.fold(depth=0.08)
    # The books write from x 7, y 5 and down to y 92, at least 7 texels from the fold: the wear keeps outside that.
    page.leaves(count=3)
    page.worn_corners(radius=2)
    page.nicks(6)
    page.thumbed(radius=13.0, strength=0.12)
    page.tear(y=58, length=3)
    page.water_ring(cx=66.0, cy=101.0, radius=11.0, start=3.6, sweep=1.5)
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    img.paste(page.image(), (0, 0))
    # Plain paper beside the page, under the slip of the search (it reaches u 77) and the tabs: the page's middle, no frame.
    plain = Page(16, 96, seed + 1, paper=(245, 234, 205), edge=(210, 178, 126), ink=(118, 82, 46), fold_width=1)
    plain.age(mottle=0.035, grain=0.008, yellowing=0.0, wear=1.0)
    img.paste(plain.image().crop((0, 0, 8, 96)), (72, 0))
    ribbon(img, 80, ((120, 26, 22), (170, 42, 34), (196, 64, 50), (214, 92, 74)))
    ribbon(img, 88, ((150, 40, 32), (204, 70, 54), (228, 100, 80), (240, 132, 108)))
    img.save(path)


def big_paper(path, seed):
    """The lectern's and the enchanting table's books: a whole sheet of 115 x 154 texels in a 128 x 256 texture, the fold on the
    left, its first 17 texels arching up out of the gutter, what is written on the flat part beyond them. The awakening book is
    paper like the others (another seed): no frame, the page furniture and the ink do the rest."""
    page = Page(115, 154, seed, paper=(249, 240, 215), edge=(210, 176, 120), ink=(116, 80, 44), fold_width=22)
    page.clouds(strength=0.025, light_patches=4)
    page.age(mottle=0.0, grain=0.008, yellowing=0.52, wear=6.0)
    page.fibres(140)
    page.foxing(4, strength=0.15)
    page.fold(depth=0.10)
    page.leaves(count=3)
    page.worn_corners(radius=3)
    page.nicks(7)
    page.thumbed(radius=20.0, strength=0.12)
    page.tear(y=97, length=4)
    page.water_ring(cx=104.0, cy=162.0, radius=16.0, start=3.5, sweep=1.4)
    img = Image.new("RGBA", (128, 256), (0, 0, 0, 0))
    img.paste(page.image(), (0, 0))
    img.save(path)


def mystic_paper(path, seed):
    """The awakening book's vellum, as big_paper lays it out (115 x 154 texels in a 128 x 256 texture, the fold on the left):
    warmer and darker than paper, cloudier, browned in a broad band towards its edges as a page kept by candlelight, scorched
    along the very edge, the outer corners darker still; a few flecks of gilt caught in the skin. No tear, no ring, no crease: a book kept with care."""
    page = Page(115, 154, seed, paper=VELLUM, edge=(196, 156, 100), ink=(116, 80, 44), fold_width=22)
    page.clouds(strength=0.05, light_patches=3)
    page.age(mottle=0.03, grain=0.010, yellowing=0.45, wear=7.0)
    page.fibres(120, darker=0.05, lighter=0.04)
    page.foxing(3, strength=0.10)
    # The browning: a band some 20 texels deep along the head, the foot and the outer side, uneven, deepest at the edge.
    reach = 20.0 * (1.0 + 0.3 * smooth_noise(page.rng, page.w, page.h, 11))
    t = np.clip(1.0 - page.outer / reach, 0.0, 1.0) ** 1.7
    page.rgb = mix(page.rgb, VELLUM_BURN, t * 0.7)
    # And nearest the edge a scorch, as if the candle had come too close: deep umber, a few texels deep, its inner edge ragged.
    scorch = 5.0 * (1.0 + 0.6 * smooth_noise(page.rng, page.w, page.h, 5))
    u = np.clip(1.0 - page.outer / scorch, 0.0, 1.0) ** 1.4
    page.rgb = mix(page.rgb, SCORCH, u * 0.6)
    corner = np.clip(1.0 - np.hypot(np.minimum(page.ys, page.h - 1 - page.ys), page.w - 1 - page.xs) / 30.0, 0.0, 1.0) ** 1.6
    page.rgb = mix(page.rgb, SCORCH, corner * 0.4)
    page.fold(depth=0.22)
    # Its edges darkened, never cut: a bitten edge or a rounded corner would show the paler block of leaves under the sheet.
    rim = np.asarray(VELLUM_BURN, np.float32)
    for x in range(page.w):
        page.rgb[0, x] = mix(page.rgb[0, x][None, None], rim, 0.45)[0, 0]
        page.rgb[page.h - 1, x] = mix(page.rgb[page.h - 1, x][None, None], rim, 0.55)[0, 0]
    for y in range(page.h):
        page.rgb[y, page.w - 1] = mix(page.rgb[y, page.w - 1][None, None], rim, 0.5)[0, 0]
        page.rgb[y, page.w - 3] = mix(page.rgb[y, page.w - 3][None, None], rim, 0.18)[0, 0]
    # Gilt flecks: a few single texels, kept to the margins.
    for _ in range(14):
        x = int(page.rng.integers(4, page.w - 3))
        y = int(page.rng.integers(3, page.h - 3))
        if page.outer[y, x] < 14 or x < 24:
            page.rgb[y, x] = np.asarray((214, 172, 92), np.float32) * 0.6 + page.rgb[y, x] * 0.4
    img = Image.new("RGBA", (128, 256), (0, 0, 0, 0))
    img.paste(page.image(), (0, 0))
    img.save(path)


# The books' palette: inks are browns, never black; one accent, the rubric red.
INK = (59, 42, 26)
INK_SOFT = (138, 107, 69)
INK_FAINT = (185, 162, 124)
PAPER_RGB = (248, 240, 216)
PAPER_SHADE = (234, 220, 184)
PAPER_EDGE = (208, 178, 128)
RIM_DARK = (184, 158, 112)
RIM_LIGHT = (255, 249, 230)
SELECT = (90, 58, 28)
ACCENT = (163, 50, 42)
# The awakening book's vellum: warmer and darker than the paper of the other books, browned towards its edges.
VELLUM = (234, 216, 174)
VELLUM_BURN = (134, 92, 52)
# The darkest of it, where the edge was nearly burnt.
SCORCH = (84, 52, 28)


def dog_ear(path, size=14):
    """The corner turned down, `size` texels for `size` units of the page (one texel to a unit, the paper's and the icons'
    scale), for the foot of a right page (mirrored for a left one), its corner at the bottom right. Along a fold slanting
    across the corner the corner of the leaf is laid back over the page: the flap, the back of the leaf, palest along the crease
    where the light catches it and greying towards its tip, which stands a little off the page; its free edges only a shade
    darker than itself (paper, never an ink line), and its shadow on the page beyond them, deepest under the tip. Where the corner
    was, nothing is drawn but the flap's shade along the crease: the page under it is paper as this one is. No box round it: a
    triangle laid on the page. Columns: at rest, turned further down under the cursor. Rows: the recipe and lectern paper, the awakening book's vellum."""
    n = size
    img = np.zeros((2 * n, 2 * n, 4), np.float32)
    papers = (np.array((246, 237, 212), np.float32), np.array(VELLUM, np.float32))
    shadow = np.array((70, 46, 24), np.float32)
    for row in range(2):
        paper = papers[row]
        for col, turned in enumerate((8, 10)):
            f = 2 * n - turned  # the fold: x + y = f, `turned` texels in from the corner along both edges
            tip = f - n  # the flap's tip, the corner laid back: (tip, tip)
            span = max(1.0, f - 1 - 2 * tip)
            for y in range(n):
                for x in range(n):
                    px, py = col * n + x, row * n + y
                    s_ = x + y
                    if s_ >= f:
                        # Where the corner was: the page under it, which is paper as this one is (left bare), and the
                        # flap's shade along the crease.
                        if s_ <= f + 1:
                            img[py, px] = (*shadow, 0.26 if s_ == f else 0.12)
                    elif x >= tip and y >= tip:
                        t = (f - 1 - s_) / span  # 0 at the crease, 1 at the tip
                        # A shade under the page's own paper (the page is drawn shaded, the flap is not), deeper at the tip.
                        c = paper * (0.93 * (1.0 - t) + 0.84 * t)
                        if s_ == f - 1:
                            c = paper * 0.97
                        elif x == tip or y == tip:
                            c = c * 0.95
                        img[py, px] = (*c, 1.0)
                    else:
                        # The flap's shadow on the page, two texels beyond its free edges, deepest by the tip.
                        d = max(tip - x, tip - y) if (x >= tip - 2 and y >= tip - 2) else 9
                        if d in (1, 2) and min(x, y) >= tip - 2:
                            # Only by the tip, which stands off the page; none along the crease, where the flap lies flat.
                            k = 1.0 - (s_ - 2 * tip) / span
                            k = max(0.0, min(1.0, k)) ** 1.5
                            a = (0.36 if d == 1 else 0.16) * k
                            if a >= 0.1:
                                img[py, px] = (*shadow, a)
    out = np.clip(np.rint(np.dstack([img[:, :, :3], img[:, :, 3:] * 255.0])), 0, 255).astype(np.uint8)
    Image.fromarray(out, "RGBA").save(path)


# The tabs' pictograms, 9 x 9 texels, in the order PageParts.Glyph lists them: drawn in the books' inks (# ink, o soft ink,
# r rubric, g gilt), never an item's own picture, so every tab belongs to the book.
TAB_GLYPH_ORDER = ("all", "recent", "building", "redstone", "equipment", "misc", "filter", "banner", "tools", "armour", "ingot")
GLYPHS = {
 "tools": [  # a pickaxe
  "..#####..",
  ".#ooooo#.",
  "#o..g..o#",
  "#...g...#",
  "....g....",
  "....g....",
  "....g....",
  "....g....",
  "....g....",
 ],
 "armour": [  # a breastplate
  ".........",
  ".##...##.",
  "#o#####o#",
  "#ooooooo#",
  ".#ooooo#.",
  ".#ooooo#.",
  ".#ooooo#.",
  ".#######.",
  ".........",
 ],
 "ingot": [  # an ingot
  ".........",
  ".........",
  "..#####..",
  ".#ggggg#.",
  "#ggggggg#",
  "#########",
  ".........",
  ".........",
  ".........",
 ],
 "all": [   # a compass rose: four points, the north one red
  ".........",
  "..#####..",
  ".#..r..#.",
  ".#.rrr.#.",
  ".#..#..#.",
  ".#.ooo.#.",
  ".#..o..#.",
  "..#####..",
  ".........",
 ],
 "recent": [  # an hourglass, sand running
  ".#######.",
  "..#...#..",
  "..#ggg#..",
  "...#g#...",
  "....#....",
  "...#g#...",
  "..#.g.#..",
  "..#ggg#..",
  ".#######.",
 ],
 "building": [  # a wall of bricks
  ".........",
  "#########",
  "#rrr#rrr#",
  "#########",
  "#r#rrr#r#",
  "#########",
  "#rrr#rrr#",
  "#########",
  ".........",
 ],
 "redstone": [  # a torch, its flame red
  "....r....",
  "...rrr...",
  "...rgr...",
  "....r....",
  "...###...",
  "....#....",
  "....#....",
  "....#....",
  "...###...",
 ],
 "equipment": [  # a sword, point up
  ".........",
  "......##.",
  ".....#o#.",
  "....#o#..",
  ".#.#o#...",
  "..#o#....",
  "..##.....",
  ".g..#....",
  "g........",
 ],
 "misc": [  # a stoppered flask
  "...ooo...",
  "...#o#...",
  "...#.#...",
  "..#...#..",
  ".#.....#.",
  ".#rrrrr#.",
  ".#rrrrr#.",
  "..#####..",
  ".........",
 ],
 "filter": [  # the crafting grid, three by three
  ".........",
  ".#######.",
  ".#o#o#o#.",
  ".#######.",
  ".#o#o#o#.",
  ".#######.",
  ".#o#o#o#.",
  ".#######.",
  ".........",
 ],
 "banner": [  # a banner on its pole
  "#########",
  ".#rrrrr#.",
  ".#rrrrr#.",
  ".#rrgrr#.",
  ".#rgrgr#.",
  ".#rrgrr#.",
  ".#rrrrr#.",
  ".#r#.#r#.",
  ".##...##.",
 ],
}


def elements(path):
    """The furniture of a recipe book's pages, one texel to a unit of the page (the scale of the paper, the icons and the
    letters), in one 128 x 128 texture (Fallow's livres/client/PageParts reads the same places):
      tabs, 16 x 16 at (0, 0), (16, 0), (32, 0): behind the page, under the cursor, open; nine pieces, 3 texels a side;
      the search slip, 16 x 16 at (48, 0) and lit at (64, 0), nine pieces of 3;
      wells, 17 x 17 at (0, 16) (a recipe out of reach, or a thing not at hand) and ready at (17, 16);
      the frame of a picked well, 17 x 17 at (34, 16), on the well's own rim;
      the mark of a well holding variants, 17 x 17 at (52, 16), a second rim inside it;
      the card a group of recipes opens on, 16 x 16 at (0, 36), its shadow in its last row and column, nine pieces of 4;
      the magnifier, 8 x 8 at (16, 36); the mark of a new recipe, 4 x 4 at (24, 36);
      the gutter's shade, 16 x 8 at (48, 52), the fold on its left;
      the rule under a name, 31 x 5 at (0, 76);
      the red head of a tab with something new, 16 x 2 at (80, 0);
      the tabs' pictograms, 9 x 9 from (0, 88) in the order of TAB_GLYPH_ORDER.
    One palette for every book: brown inks (never black), paper and its shades, the rubric red for what is new. Lines are a texel,
    corners notched by a texel, wells pressed into the paper (darker rim above and left, lit below and right), nothing green."""
    img = np.zeros((128, 128, 4), np.float32)

    def put(x, y, rgb, a=1.0):
        img[y, x, :3] = rgb
        img[y, x, 3] = a

    def box(x0, y0, w, h, fill, line, fill_a=1.0, line_a=1.0, notch=True, open_foot=False, light=None):
        """A box of `fill` in a texel of `line`, its corners notched; `open_foot`: no line along its foot; `light`: a texel of
        light inside its head."""
        for y in range(h):
            for x in range(w):
                corner = (x in (0, w - 1)) and (y in (0, h - 1))
                if notch and corner and not (open_foot and y == h - 1):
                    continue
                on_line = x in (0, w - 1) or y == 0 or (y == h - 1 and not open_foot)
                if on_line:
                    put(x0 + x, y0 + y, line, line_a)
                elif light is not None and y == 1:
                    put(x0 + x, y0 + y, light, fill_a)
                else:
                    put(x0 + x, y0 + y, fill, fill_a)

    # Tabs: behind the page a darker paper, under the cursor the page's paper, open the page's paper with no line at its foot.
    box(0, 0, 16, 16, (226, 208, 170), INK_SOFT, light=(236, 222, 190))
    box(16, 0, 16, 16, PAPER_RGB, INK_SOFT, light=RIM_LIGHT)
    box(32, 0, 16, 16, PAPER_RGB, INK_SOFT, open_foot=True, light=RIM_LIGHT)
    for x in range(1, 15):
        # Where the tabs behind go in under the page: its shadow on them.
        put(x, 15, (184, 158, 112))
        put(16 + x, 15, (200, 176, 130))
    # The search slip: a field of the lightest paper in a texel of ink, the top a shade in.
    box(48, 0, 16, 16, RIM_LIGHT, INK_SOFT, light=PAPER_SHADE)
    box(64, 0, 16, 16, RIM_LIGHT, SELECT, light=PAPER_SHADE)
    # Wells, 17 square. Out of reach: a faint rim only, the paper bare. Ready: pressed in, a shade darker, rim dark above and
    # left, lit below and right.
    for y in range(17):
        for x in range(17):
            corner = x in (0, 16) and y in (0, 16)
            if corner:
                continue
            if x in (0, 16) or y in (0, 16):
                put(x, 16 + y, RIM_DARK, 0.55)
    for y in range(17):
        for x in range(17):
            corner = x in (0, 16) and y in (0, 16)
            if corner:
                continue
            if x == 0 or y == 0:
                c = RIM_DARK
            elif x == 16 or y == 16:
                c = RIM_LIGHT
            elif x == 1 or y == 1:
                c = (222, 204, 164)
            else:
                c = PAPER_SHADE
            put(17 + x, 16 + y, c, 1.0)
    # The frame of a picked well, on the well itself (17 square, its rim): a texel of dark ink with the corners notched, a
    # texel of light inside it and the corners of a second line of ink a texel further in, so the pick is framed without
    # touching the wells beside it.
    for y in range(17):
        for x in range(17):
            edge = x in (0, 16) or y in (0, 16)
            inner = x in (1, 15) or y in (1, 15)
            if x in (0, 16) and y in (0, 16):
                continue
            if edge:
                put(34 + x, 16 + y, SELECT)
            elif inner and not (x in (1, 15) and y in (1, 15)):
                put(34 + x, 16 + y, RIM_LIGHT, 0.9)
            elif (x in (2, 14) and y in (2, 3, 13, 14)) or (y in (2, 14) and x in (2, 3, 13, 14)):
                put(34 + x, 16 + y, SELECT, 0.75)
    # A well holding several variants: a second rim a texel inside its right and bottom sides, as the edges of cards stacked
    # under it, 17 x 17 at (52, 16); it keeps inside the well, clear of the wells beside it.
    for i in range(3, 16):
        put(52 + 15, 16 + i, RIM_DARK, 0.75)
        put(52 + i, 16 + 15, RIM_DARK, 0.75)
    put(52 + 15, 16 + 15, RIM_DARK, 0.9)
    # The card: one line of soft ink round the lightest paper, notched, and a texel of shadow below and right.
    for y in range(15):
        for x in range(15):
            if x in (0, 14) and y in (0, 14):
                continue
            put(1 + x, 36 + 1 + y, (90, 60, 30), 0.4)
    box(0, 36, 15, 15, RIM_LIGHT, INK_SOFT)
    # The magnifier: a ring of ink round a pale glass with a glint, and its handle.
    glass = ["..###...",
             ".#oo.#..",
             "#o...#..",
             "#....#..",
             ".####...",
             ".....#..",
             "......#.",
             ".......#"]
    for y, rowtxt in enumerate(glass):
        for x, ch in enumerate(rowtxt):
            if ch == "#":
                put(16 + x, 36 + y, INK)
            elif ch == "o":
                put(16 + x, 36 + y, (255, 255, 255), 0.9)
            elif ch == "." and 1 <= x <= 4 and 1 <= y <= 3:
                put(16 + x, 36 + y, RIM_LIGHT, 0.7)
    # The mark of a new recipe: a lozenge of the rubric red, 4 texels.
    for x, y in ((1, 0), (2, 0), (0, 1), (1, 1), (2, 1), (3, 1), (0, 2), (1, 2), (2, 2), (3, 2), (1, 3), (2, 3)):
        put(24 + x, 36 + y, ACCENT)
    put(24 + 1, 36 + 1, (214, 110, 92))
    # What is new on a tab: its head drawn in the rubric red, 16 x 2 at (80, 0), the corners notched as the tab's are; it
    # stops where the tab's pictogram begins.
    for x in range(16):
        for y in range(2):
            if y == 0 and x in (0, 15):
                continue
            c = (126, 34, 28) if y == 0 or x in (0, 15) else ACCENT
            put(80 + x, y, c)
    # The tabs' pictograms, a row of 9 x 9 at v 88.
    colours = {"#": INK, "o": INK_SOFT, "r": ACCENT, "g": (190, 146, 66)}
    for i, name in enumerate(TAB_GLYPH_ORDER):
        for y, rowtxt in enumerate(GLYPHS[name]):
            for x, ch in enumerate(rowtxt):
                if ch in colours:
                    put(i * 9 + x, 88 + y, colours[ch])
    # The gutter: where the page goes down into the fold, a fine line of shade and a soft shadow fading out from it.
    # Texels under 0.1 opaque are thrown away: the faint tail keeps 0.12 and fades by its colour, towards the paper.
    paper = np.array(PAPER_RGB, np.float32)
    for x in range(16):
        shade = 0.40 if x == 0 else 0.20 * (1.0 - x / 16.0) ** 1.8
        a = max(0.12, shade)
        colour = paper + (np.array((110, 76, 40), np.float32) - paper) * (shade / a)
        for y in range(8):
            put(48 + x, 52 + y, colour, a)
    # The rule under a name, 31 texels: a line of soft ink thinning out at both ends, a small lozenge in its middle.
    for x in range(31):
        d = abs(x - 15)
        if d <= 2:
            continue
        a = 1.0 if d < 10 else (0.6 if d < 13 else 0.3)
        put(x, 78, INK_SOFT, a)
    for x, y in ((15, 77), (14, 78), (16, 78), (15, 79)):
        put(x, y, INK_SOFT)
    put(15, 78, ACCENT)
    out = np.clip(np.rint(np.dstack([img[:, :, :3], img[:, :, 3:] * 255.0])), 0, 255).astype(np.uint8)
    Image.fromarray(out, "RGBA").save(path)


def main():
    FALLOW.mkdir(parents=True, exist_ok=True)
    HOV.mkdir(parents=True, exist_ok=True)
    recipe_paper(FALLOW / "livre_de_recettes_papier.png", seed=11)
    recipe_paper(HOV / "recipe_book_paper.png", seed=11)
    mystic_paper(FALLOW / "livre_d_eveil_papier.png", seed=23)
    big_paper(HOV / "enchanting_book_paper.png", seed=31)
    dog_ear(FALLOW / "coin_de_page.png")
    elements(FALLOW / "livre_elements.png")


if __name__ == "__main__":
    main()
