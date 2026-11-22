"""Paper of the books read in the world: the recipe books of the stations (Fallow's and Hands-On Vanilla's), the lectern's
book and the awakening book of the enchanting table.

Each page is drawn texel by texel from a fixed seed, so a rerun gives the same files: soft, cloudy paper, the edges of the leaves
under it showing along its outer side, a little worn (corners rounded and darker, a few nicks, a short tear, a crease, a faint
water ring), shaded towards the fold. Nothing is ruled on the recipe and lectern pages: the cover is their border. The awakening
book alone has one fine rule in violet ink, with a small star at each corner. All the wear keeps to the margins the books leave.

The corner turned down at the foot of a page that has a next one (or a previous one) is a sprite of its own, coin_de_page.png:
the flap of paper folded over, its shadow, and the page under it; its second column is the same corner lifted a little more,
under the cursor; its second row is the awakening book's paper. It is drawn two texels to a unit, as are the furniture of the
recipe books' pages, livre_elements.png (tabs, the search slip, wells, the frame round a picked one, the card of a group, the
magnifier, the mark of a new recipe, the rule under a name).

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

    def crease(self, x0, y0, length, sx=-1, sy=1, strength=0.10):
        """A crease left by a corner once folded: a texel of shade along a diagonal and a texel of light beside it."""
        for i in range(length):
            x, y = x0 + sx * i, y0 + sy * i
            if 0 <= x < self.w and 0 <= y < self.h - 1:
                self.rgb[y, x] *= 1.0 - strength
                self.rgb[y + 1, x] = np.minimum(self.rgb[y + 1, x] * (1.0 + strength * 0.6), 255.0)

    def water_ring(self, cx, cy, radius, start, sweep, strength=0.07):
        """Part of a faint ring where a wet cup once stood: the rim of the stain a little darker, its inside a hair paler."""
        d = np.hypot(self.xs - cx, self.ys - cy)
        a = (np.arctan2(self.ys - cy, self.xs - cx) - start) % (2 * np.pi)
        on = a < sweep
        rim = np.clip(1.0 - np.abs(d - radius) / 0.9, 0.0, 1.0) * on
        inside = np.clip((radius - d) / radius, 0.0, 1.0) * on * 0.25
        self.rgb = mix(self.rgb, self.edge * 0.9, rim * strength + inside * strength)

    def dot(self, x, y, strength):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.rgb[y, x] = mix(self.rgb[y, x][None, None], self.ink, strength)[0, 0]

    def hline(self, x0, x1, y, strength, every=1):
        for x in range(x0, x1 + 1):
            if (x - x0) % every == 0:
                self.dot(x, y, strength)

    def vline(self, x, y0, y1, strength, every=1):
        for y in range(y0, y1 + 1):
            if (y - y0) % every == 0:
                self.dot(x, y, strength)

    def star(self, cx, cy, strength):
        """A four-pointed star of five texels, with a fainter halo: the awakening book's corners."""
        for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            self.dot(cx + dx, cy + dy, strength)
        for dx, dy in ((2, 0), (-2, 0), (0, 2), (0, -2)):
            self.dot(cx + dx, cy + dy, strength * 0.45)

    def ring(self, cx, cy, radius, strength, gap=0):
        """A thin circle, dotted when `gap` is set: the watermark of the awakening book."""
        steps = int(radius * 8)
        for i in range(steps):
            if gap and (i // 2) % gap:
                continue
            a = i / steps * np.pi * 2
            self.dot(int(round(cx + np.cos(a) * radius)), int(round(cy + np.sin(a) * radius)), strength)

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
    page.clouds(strength=0.05, light_patches=5)
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
    page.crease(x0=70, y0=1, length=6)
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


def big_paper(path, seed, mystic):
    """The lectern's and the enchanting table's books: a whole sheet of 115 x 154 texels in a 128 x 256 texture, the fold on the
    left, its first 17 texels arching up out of the gutter, what is written on the flat part beyond them."""
    if mystic:
        page = Page(115, 154, seed, paper=(240, 231, 212), edge=(186, 160, 132), ink=(84, 54, 140), fold_width=22)
    else:
        page = Page(115, 154, seed, paper=(249, 240, 215), edge=(210, 176, 120), ink=(116, 80, 44), fold_width=22)
    page.clouds(strength=0.055, light_patches=7)
    page.age(mottle=0.0, grain=0.008, yellowing=0.52 if not mystic else 0.55, wear=6.0)
    page.fibres(140)
    page.foxing(4, strength=0.15)
    page.fold(depth=0.10)
    page.leaves(count=3)
    page.worn_corners(radius=3)
    page.nicks(7)
    page.thumbed(radius=20.0, strength=0.12)
    page.tear(y=97, length=4)
    page.crease(x0=113, y0=1, length=8)
    page.water_ring(cx=104.0, cy=162.0, radius=16.0, start=3.5, sweep=1.4)
    if mystic:
        # The awakening book: one fine rule in violet ink on the flat part, a small star where it turns, and the table's ring
        # as a watermark. What the book writes stays 8 units inside it.
        page.hline(23, 109, 4, 0.34)
        page.hline(23, 109, 149, 0.34)
        page.vline(21, 6, 147, 0.34)
        page.vline(111, 6, 147, 0.34)
        for cx, cy in ((21, 4), (111, 4), (21, 149), (111, 149)):
            page.star(cx, cy, 0.62)
        page.ring(66, 77, 30, 0.06)
        page.ring(66, 77, 24, 0.04, gap=3)
        page.ring(66, 77, 9, 0.05)
    img = Image.new("RGBA", (128, 256), (0, 0, 0, 0))
    img.paste(page.image(), (0, 0))
    img.save(path)


def dog_ear(path, fine=2):
    """The corner turned down, 16 x 16 units for the foot of a right page (mirrored for a left one), its corner at the bottom
    right, drawn `fine` texels to a unit. The fold runs slantwise from the foot to the outer side; beyond it lies the page under,
    in the flap's shade, its cut edge in a fine brown line; the flap is the corner itself laid back over the page, a little curled
    (shorter than flat), palest along the crease, its free edges in the same brown line, with a soft shadow round it. Columns: at
    rest, lifted further under the cursor. Rows: the recipe and lectern paper, the awakening book's."""
    n = 16 * fine
    img = Image.new("RGBA", (2 * n, 2 * n), (0, 0, 0, 0))
    papers = (((248, 240, 216), (208, 178, 128)), ((240, 231, 212), (186, 160, 132)))
    folds = (((5.0, 16.0), (16.0, 3.5)), ((3.0, 16.0), (16.0, 1.0)))
    curl = 0.82
    line = 1.0 / fine
    for row, (paper, edge) in enumerate(papers):
        paper = np.asarray(paper, np.float32)
        edge = np.asarray(edge, np.float32)
        ink = edge * 0.55
        for col, (a, b) in enumerate(folds):
            a, b = np.asarray(a), np.asarray(b)
            along = (b - a) / np.linalg.norm(b - a)
            inner = np.array([along[1], -along[0]])
            if np.dot(inner, np.array([0.0, 0.0]) - a) < 0:
                inner = -inner
            for y in range(n):
                for x in range(n):
                    p = np.array([(x + 0.5) / fine, (y + 0.5) / fine])
                    px, py = col * n + x, row * n + y
                    d = float(np.dot(p - a, inner))
                    if d < 0:
                        # Beyond the fold: the page under it, darkest along the crease, its corner worn round, its edge inked.
                        if p[0] + p[1] >= 29.6:
                            continue
                        t = min(1.0, -d / 4.0)
                        c = paper * (0.80 + 0.12 * t)
                        if p[0] > 16.0 - 1.6 * line or p[1] > 16.0 - 1.6 * line or p[0] + p[1] > 29.6 - 2.2 * line:
                            c = mix(c[None, None], ink, 0.75)[0, 0]
                        if -d < 1.4 * line:
                            c = mix(c[None, None], ink, 0.55)[0, 0]
                        img.putpixel((px, py), tuple(int(v) for v in np.clip(np.rint(c), 0, 255)) + (255,))
                        continue
                    foot = p - d * inner
                    flat = foot - (d / curl) * inner
                    if flat[0] <= 16.0 and flat[1] <= 16.0:
                        # The flap: the back of the leaf, palest along the crease, darker towards its tip, its free edges inked.
                        c = np.minimum(paper * (0.97 - 0.016 * d), 255.0)
                        if d < 1.2:
                            c = np.minimum(paper * 1.02, 255.0)
                        if 16.0 - flat[0] < 1.4 * line / curl or 16.0 - flat[1] < 1.4 * line / curl:
                            c = mix(c[None, None], ink, 0.8)[0, 0]
                        img.putpixel((px, py), tuple(int(v) for v in np.clip(np.rint(c), 0, 255)) + (255,))
                        continue
                    wide = foot - (d / (curl * 1.25)) * inner
                    if wide[0] <= 16.0 and wide[1] <= 16.0:
                        # Its shadow on the page.
                        c = edge * 0.4
                        img.putpixel((px, py), tuple(int(v) for v in c) + (70,))
    img.save(path)


def elements(path):
    """The furniture of a recipe book's pages, two texels to a unit of the page, in one 128 x 128 texture (Fallow's
    livres/client/PageParts reads the same places):
      tabs, 16 x 16 at (0, 0), (16, 0), (32, 0): behind the page, under the cursor, open; drawn in nine pieces, 5 texels a side;
      the search slip, 16 x 16 at (48, 0) and lit at (64, 0), in nine pieces too;
      wells, 34 x 34 at (0, 16) and ready (craftable) at (34, 16); the frame round a picked one at (68, 16);
      the card a group of recipes opens on, 24 x 24 at (0, 52), nine pieces of 8;
      the magnifier, 16 x 16 at (24, 52); the mark of a new recipe, 8 x 8 at (40, 52);
      the gutter's shade, 16 x 8 at (48, 52), the fold on its left: a fine dark line and a shade fading over 8 units;
      the rule under a recipe's name, 106 x 8 at (0, 76).
    Brown ink lines a texel wide (half a unit: finer than the paper's own grain), corners rounded, wells pressed into the paper
    (shaded at the top and left, lit at the bottom and right), as Scholar's screens are drawn, in our own colours."""
    img = np.zeros((128, 128, 4), np.float32)
    PAPER = np.array([247, 238, 212], np.float32)
    ink = np.array([96, 64, 32], np.float32)
    lit_ink = np.array([138, 90, 43], np.float32)

    def put(x, y, rgb, a):
        if 0 <= x < 128 and 0 <= y < 128:
            img[y, x, :3] = rgb
            img[y, x, 3] = a

    def over(x, y, rgb, a):
        """Lays `rgb` at opacity `a` over what is there."""
        if not (0 <= x < 128 and 0 <= y < 128) or a <= 0:
            return
        base_a = img[y, x, 3]
        out_a = a + base_a * (1 - a)
        if out_a <= 0:
            return
        img[y, x, :3] = (np.asarray(rgb, np.float32) * a + img[y, x, :3] * base_a * (1 - a)) / out_a
        img[y, x, 3] = out_a

    def rounded(x0, y0, w, h, r, top_only=False):
        """Whether each texel of a w x h box is inside its rounded outline, and whether it is on that outline."""
        inside = np.ones((h, w), bool)
        rim = np.zeros((h, w), bool)
        for y in range(h):
            for x in range(w):
                cx = r - 0.5 if x < r else (w - r - 0.5 if x >= w - r else None)
                bottom = not top_only
                cy = r - 0.5 if y < r else (h - r - 0.5 if (y >= h - r and bottom) else None)
                if cx is not None and cy is not None:
                    d = np.hypot(x - cx, y - cy)
                    if d > r + 0.15:
                        inside[y, x] = False
                    elif d > r - 0.85:
                        rim[y, x] = True
                else:
                    if x == 0 or x == w - 1 or y == 0 or (y == h - 1 and not top_only):
                        rim[y, x] = True
        return inside, rim

    # Tabs: behind the page (a darker paper, darker still towards the page that hides its foot), under the cursor (warmer),
    # open (the page's own paper, meeting the page with no line).
    for i, (paper, foot) in enumerate((((214, 200, 170), 0.88), ((236, 220, 186), 0.92), ((248, 240, 216), 1.0))):
        inside, rim = rounded(0, 0, 16, 16, 4, top_only=True)
        for y in range(16):
            for x in range(16):
                if not inside[y, x]:
                    continue
                k = 1.0 + (foot - 1.0) * max(0.0, (y - 9) / 6.0)
                c = np.minimum(np.asarray(paper, np.float32) * k, 255)
                put(16 * i + x, y, c, 1.0)
                if rim[y, x]:
                    over(16 * i + x, y, lit_ink if i == 1 else ink, 0.9)
                elif y == 1 or (y < 4 and rim[max(0, y - 1), x]):
                    over(16 * i + x, y, (255, 252, 240), 0.45)
                if y == 15 and i < 2:
                    # Where the tab goes in behind the page: the page's shadow on it.
                    over(16 * i + x, y, (90, 60, 30), 0.35)
    # The search slip: a field of cream in a fine brown line, rounded, a dot of ink inside each corner, the top shaded in.
    for i, (paper, line) in enumerate((((250, 243, 224), ink), ((253, 236, 208), lit_ink))):
        inside, rim = rounded(0, 0, 16, 16, 3)
        for y in range(16):
            for x in range(16):
                if not inside[y, x]:
                    continue
                put(48 + 16 * i + x, y, paper, 1.0)
                if rim[y, x]:
                    over(48 + 16 * i + x, y, line, 0.9)
                elif y == 1:
                    over(48 + 16 * i + x, y, ink, 0.18)
        for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
            over(48 + 16 * i + x, y, line, 0.75)
    # Wells: pressed into the paper. At rest a faint brown; ready (the recipe can be made), a soft sage green.
    for i, (tint, a) in enumerate((((120, 86, 44), 0.12), ((98, 128, 78), 0.15))):
        inside, rim = rounded(0, 0, 34, 34, 3)
        for y in range(34):
            for x in range(34):
                if not inside[y, x]:
                    continue
                X, Y = 34 * i + x, 16 + y
                over(X, Y, tint, a)
                if rim[y, x]:
                    over(X, Y, ink if i == 0 else (62, 96, 52), 0.30 if i == 0 else 0.62)
                elif i == 1 and (y in (1, 32) or x in (1, 32)):
                    # Ready: a fine green line inside the rim.
                    over(X, Y, (120, 160, 96), 0.35)
                elif y in (1, 2) or x in (1, 2):
                    over(X, Y, ink, 0.16 if (y == 1 or x == 1) else 0.08)
                elif y in (31, 32) or x in (31, 32):
                    over(X, Y, (255, 252, 238), 0.35 if (y == 32 or x == 32) else 0.18)
    # The frame round a picked well: a unit of warm brown with a fine light line inside it, rounded.
    inside, rim = rounded(0, 0, 34, 34, 4)
    inner_in, inner_rim = rounded(0, 0, 30, 30, 3)
    for y in range(34):
        for x in range(34):
            if not inside[y, x]:
                continue
            ring = rim[y, x] or not (2 <= x < 32 and 2 <= y < 32) or (2 <= x < 32 and 2 <= y < 32 and not inner_in[y - 2, x - 2])
            if ring:
                put(68 + x, 16 + y, lit_ink, 1.0)
            elif 2 <= x < 32 and 2 <= y < 32 and inner_rim[y - 2, x - 2]:
                put(68 + x, 16 + y, (214, 176, 120), 0.7)
    # The card: a panel a shade darker than the paper, a fine rounded line round it and a fainter one inside.
    inside, rim = rounded(0, 0, 24, 24, 4)
    inner_in, inner_rim = rounded(0, 0, 20, 20, 3)
    for y in range(24):
        for x in range(24):
            if not inside[y, x]:
                continue
            # At least 0.12 opaque (what is under 0.1 is thrown away), in a paler brown that shades as 0.07 of the ink would.
            over(x, 52 + y, PAPER + (np.array((120, 86, 44), np.float32) - PAPER) * (0.07 / 0.12), 0.12)
            if rim[y, x]:
                over(x, 52 + y, ink, 0.55)
            elif 2 <= x < 22 and 2 <= y < 22 and inner_rim[y - 2, x - 2]:
                over(x, 52 + y, ink, 0.22)
    # The magnifier: a ring of brown round a pale glass with a glint, and its handle.
    for y in range(16):
        for x in range(16):
            d = np.hypot(x - 5.5, y - 5.5)
            if d <= 3.4:
                over(24 + x, 52 + y, (250, 246, 232), 0.55)
                if 1.0 < d < 2.6 and x < 6 and y < 6 and x + y in (5, 6):
                    over(24 + x, 52 + y, (255, 255, 255), 0.9)
            if 3.4 < d <= 5.0:
                over(24 + x, 52 + y, ink, 1.0 if d <= 4.6 else 0.5)
    for i in range(6):
        for w in (0, 1):
            over(24 + 9 + i, 52 + 9 + i + w, ink, 1.0)
            over(24 + 9 + i + w, 52 + 9 + i, ink, 1.0)
        over(24 + 10 + i, 52 + 9 + i, lit_ink, 0.8)
    # The mark of a new recipe: a small red lozenge with a glint.
    red, dark = np.array([196, 52, 40], np.float32), np.array([124, 26, 20], np.float32)
    for y in range(8):
        for x in range(8):
            d = abs(x - 3.5) + abs(y - 3.5)
            if d <= 3.6:
                put(40 + x, 52 + y, dark if d > 2.6 else red, 1.0)
    put(40 + 3, 52 + 2, (244, 140, 120), 1.0)
    # The gutter: where the page goes down into the fold, a fine line of shade and a soft shadow fading out from it.
    # Texels under 0.1 opaque are thrown away: the faint tail keeps 0.12 and fades by its colour, towards the paper.
    for x in range(16):
        shade = 0.5 if x == 0 else 0.26 * (1.0 - x / 16.0) ** 1.6
        a = max(0.12, shade)
        colour = PAPER + (np.array((110, 76, 40), np.float32) - PAPER) * (shade / a)
        for y in range(8):
            over(48 + x, 52 + y, colour, a)
    # The rule under a name: a fine line fading out at both ends, a small lozenge in the middle and a dot either side of it.
    for x in range(106):
        t = abs(x - 52.5) / 52.5
        a = 0.62 * min(1.0, (1.0 - t) * 3.0)
        if abs(x - 52.5) > 7:
            over(x, 76 + 4, ink, a)
    for y in range(8):
        for x in range(106):
            d = abs(x - 52.5) + abs(y - 3.5) * 1.2
            if d <= 3.7:
                over(x, 76 + y, ink, 0.9 if d > 2.4 else 0.35)
    for x in (44, 61):
        over(x, 76 + 4, ink, 0.8)
        over(x, 76 + 3, ink, 0.35)
    out = np.clip(np.rint(np.dstack([img[:, :, :3], img[:, :, 3:] * 255.0])), 0, 255).astype(np.uint8)
    Image.fromarray(out, "RGBA").save(path)


def main():
    FALLOW.mkdir(parents=True, exist_ok=True)
    HOV.mkdir(parents=True, exist_ok=True)
    recipe_paper(FALLOW / "livre_de_recettes_papier.png", seed=11)
    recipe_paper(HOV / "recipe_book_paper.png", seed=11)
    big_paper(FALLOW / "livre_d_eveil_papier.png", seed=23, mystic=True)
    big_paper(HOV / "enchanting_book_paper.png", seed=31, mystic=False)
    dog_ear(FALLOW / "coin_de_page.png")
    elements(FALLOW / "livre_elements.png")


if __name__ == "__main__":
    main()
