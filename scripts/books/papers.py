"""Paper of the books read in the world: the recipe books of the stations (Fallow's and Hands-On Vanilla's), the lectern's
book and the awakening book of the enchanting table.

Each page is drawn texel by texel from a fixed seed, so a rerun gives the same files: soft, cloudy paper, the edges of the leaves
under it showing along its outer side, a little worn (corners rounded and darker, a few nicks, a short tear, a crease, a faint
water ring), shaded towards the fold. Nothing is ruled on the recipe and lectern pages: the cover is their border. The awakening
book alone has one fine rule in violet ink, with a small star at each corner. All the wear keeps to the margins the books leave.

The corner turned down at the foot of a page that has a next one (or a previous one) is a sprite of its own, coin_de_page.png:
the flap of paper folded over, its shadow, and the page under it; its second column is the same corner lifted a little more,
under the cursor; its second row is the awakening book's paper.

The awakening book's cover is Hands-On Vanilla's enchanting book (MIT, kept in scripts/books/source/) with its brown leather
turned to a midnight violet and its gilt a shade softer.

Run from the repository root: python3 scripts/books/papers.py
Writes:
  mods-custom/fallow/src/main/resources/assets/fallow/textures/entity/livre_d_eveil.png
  mods-custom/fallow/src/main/resources/assets/fallow/textures/entity/livre_de_recettes_papier.png
  mods-custom/fallow/src/main/resources/assets/fallow/textures/entity/livre_d_eveil_papier.png
  pack/kubejs/assets/handson_vanilla/textures/entity/recipe_book_paper.png
  pack/kubejs/assets/handson_vanilla/textures/entity/enchanting_book_paper.png
  mods-custom/fallow/src/main/resources/assets/fallow/textures/entity/coin_de_page.png
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
            self.rgb[h - 1, x] = mix(self.rgb[h - 1, x][None, None], rim * 0.86, 0.62)[0, 0]
            self.rgb[0, x] = mix(self.rgb[0, x][None, None], rim, 0.30)[0, 0]

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
    page = Page(72, 96, seed, paper=(246, 237, 211), edge=(208, 178, 128), ink=(118, 82, 46), fold_width=8)
    page.clouds(strength=0.05, light_patches=5)
    page.age(mottle=0.0, grain=0.007, yellowing=0.42, wear=3.5)
    page.fibres(60)
    page.foxing(2, strength=0.08)
    page.fold(depth=0.08)
    # The books write from x 7, y 5 and down to y 92, at least 7 texels from the fold: the wear keeps outside that.
    page.leaves(count=3)
    page.worn_corners(radius=2)
    page.nicks(3)
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
        page = Page(115, 154, seed, paper=(246, 236, 210), edge=(206, 174, 122), ink=(116, 80, 44), fold_width=22)
    page.clouds(strength=0.055, light_patches=7)
    page.age(mottle=0.0, grain=0.008, yellowing=0.45 if not mystic else 0.55, wear=5.0)
    page.fibres(140)
    page.foxing(3, strength=0.08)
    page.fold(depth=0.10)
    page.leaves(count=3)
    page.worn_corners(radius=3)
    page.nicks(4)
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


def mystic_cover(source, path):
    """The awakening book's cover: every brown of the leather becomes a midnight violet of the same depth, the gold paler."""
    import colorsys

    img = Image.open(source).convert("RGBA")
    out = img.copy()
    gilt = {(255, 216, 0): (236, 204, 112), (255, 177, 0): (206, 156, 70)}
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            if not a:
                continue
            if (r, g, b) in gilt:
                out.putpixel((x, y), gilt[(r, g, b)] + (a,))
                continue
            h, sat, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if sat > 0.5 and 0.05 < h < 0.12:
                nr, ng, nb = colorsys.hsv_to_rgb(262 / 360, 0.46, v * 0.6)
                out.putpixel((x, y), (round(nr * 255), round(ng * 255), round(nb * 255), a))
    out.save(path)


def dog_ear(path):
    """The corner turned down, 16 x 16 for the foot of a right page (mirrored for a left one), its corner at the bottom right.
    The fold runs slantwise from the foot to the outer side; beyond it lies the page under, in the flap's shade; the flap is the
    corner itself laid back over the page, a little curled (shorter than flat), palest along the crease, its edges a shade darker,
    with a soft shadow round it. Columns: at rest, lifted further under the cursor. Rows: the recipe and lectern paper, the
    awakening book's."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    papers = (((246, 237, 211), (208, 178, 128)), ((240, 231, 212), (186, 160, 132)))
    folds = (((5.0, 16.0), (16.0, 3.5)), ((3.0, 16.0), (16.0, 1.0)))
    curl = 0.82
    for row, (paper, edge) in enumerate(papers):
        paper = np.asarray(paper, np.float32)
        edge = np.asarray(edge, np.float32)
        for col, (a, b) in enumerate(folds):
            a, b = np.asarray(a), np.asarray(b)
            along = (b - a) / np.linalg.norm(b - a)
            inner = np.array([along[1], -along[0]])
            if np.dot(inner, np.array([0.0, 0.0]) - a) < 0:
                inner = -inner
            for y in range(16):
                for x in range(16):
                    p = np.array([x + 0.5, y + 0.5])
                    d = float(np.dot(p - a, inner))
                    if d < 0:
                        # Beyond the fold: the page under it, darkest along the crease, its own corner worn round.
                        if x + y >= 29:
                            continue
                        t = min(1.0, -d / 4.0)
                        c = paper * (0.78 + 0.12 * t)
                        if x == 15 or y == 15:
                            c = mix(c[None, None], edge * 0.8, 0.45)[0, 0]
                        img.putpixel((col * 16 + x, row * 16 + y), tuple(int(v) for v in np.clip(np.rint(c), 0, 255)) + (255,))
                        continue
                    foot = p - d * inner
                    flat = foot - (d / curl) * inner
                    if flat[0] <= 16.0 and flat[1] <= 16.0:
                        # The flap: the back of the leaf, palest along the crease, darker towards its tip and at its edges.
                        reach = max(0.0, 16.0 - max(flat[0], flat[1]))
                        c = np.minimum(paper * (1.05 - 0.018 * d), 255.0)
                        if d < 1.0:
                            c = np.minimum(paper * 1.08, 255.0)
                        if 16.0 - flat[0] < 1.2 / curl or 16.0 - flat[1] < 1.2 / curl:
                            c = mix(c[None, None], edge * 0.78, 0.55)[0, 0]
                        img.putpixel((col * 16 + x, row * 16 + y), tuple(int(v) for v in np.clip(np.rint(c), 0, 255)) + (255,))
                        continue
                    wide = foot - (d / (curl * 1.22)) * inner
                    if wide[0] <= 16.0 and wide[1] <= 16.0:
                        # Its shadow on the page.
                        c = edge * 0.42
                        img.putpixel((col * 16 + x, row * 16 + y), tuple(int(v) for v in c) + (60,))
    img.save(path)


def main():
    FALLOW.mkdir(parents=True, exist_ok=True)
    HOV.mkdir(parents=True, exist_ok=True)
    recipe_paper(FALLOW / "livre_de_recettes_papier.png", seed=11)
    recipe_paper(HOV / "recipe_book_paper.png", seed=11)
    big_paper(FALLOW / "livre_d_eveil_papier.png", seed=23, mystic=True)
    big_paper(HOV / "enchanting_book_paper.png", seed=31, mystic=False)
    mystic_cover(Path(__file__).parent / "source/enchanting_book.png", FALLOW / "livre_d_eveil.png")
    dog_ear(FALLOW / "coin_de_page.png")


if __name__ == "__main__":
    main()
