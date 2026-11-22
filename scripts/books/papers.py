"""Paper of the books read in the world: the recipe books of the stations (Fallow's and Hands-On Vanilla's), the lectern's
book and the awakening book of the enchanting table.

Each page is drawn texel by texel from a fixed seed, so a rerun gives the same files: aged paper, darker and worn towards its
outer edges, a little shade towards the fold, and a frame ruled in ink with knots at its corners. The frame keeps clear of what
the books write: it runs inside the margins they leave (see the constants of each book below).

The awakening book's cover is Hands-On Vanilla's enchanting book (MIT, kept in scripts/books/source/) with its brown leather
turned to a midnight violet and its gilt a shade softer.

Run from the repository root: python3 scripts/books/papers.py
Writes:
  mods-custom/fallow/src/main/resources/assets/fallow/textures/entity/livre_d_eveil.png
  mods-custom/fallow/src/main/resources/assets/fallow/textures/entity/livre_de_recettes_papier.png
  mods-custom/fallow/src/main/resources/assets/fallow/textures/entity/livre_d_eveil_papier.png
  pack/kubejs/assets/handson_vanilla/textures/entity/recipe_book_paper.png
  pack/kubejs/assets/handson_vanilla/textures/entity/enchanting_book_paper.png
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

    def frame(self, left, top, right, bottom, strength, inner=None, inner_strength=0.0):
        """A ruled frame with a hollow square knot where its lines meet, and an optional dotted hairline inside it."""
        self.hline(left + 2, right - 2, top, strength)
        self.hline(left + 2, right - 2, bottom, strength)
        self.vline(left, top + 2, bottom - 2, strength)
        self.vline(right, top + 2, bottom - 2, strength)
        for cx, cy, sx, sy in ((left, top, 1, 1), (right, top, -1, 1), (left, bottom, 1, -1), (right, bottom, -1, -1)):
            # The knot: a 3 x 3 ring over the corner, its middle left as paper, the lines running into it; a short tail of two
            # dots runs from it towards the middle of the page, and each line takes a small tick a little way from it.
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    if dx or dy:
                        self.dot(cx + dx, cy + dy, strength)
            self.dot(cx + 2 * sx, cy + 2 * sy, strength * 0.7)
            self.dot(cx + 3 * sx, cy + 3 * sy, strength * 0.4)
            self.dot(cx + 4 * sx, cy + sy, strength * 0.6)
            self.dot(cx + sx, cy + 4 * sy, strength * 0.6)
        if inner:
            i = inner
            self.hline(left + i + 1, right - i - 1, top + i, inner_strength, every=2)
            self.hline(left + i + 1, right - i - 1, bottom - i, inner_strength, every=2)
            self.vline(left + i, top + i + 1, bottom - i - 1, inner_strength, every=2)
            self.vline(right - i, top + i + 1, bottom - i - 1, inner_strength, every=2)

    def diamond(self, cx, cy, strength, size=1):
        """A small lozenge on a rule: a leaf of ornament halfway along it."""
        for d in range(-size, size + 1):
            r = size - abs(d)
            self.dot(cx + d, cy - r, strength)
            self.dot(cx + d, cy + r, strength)

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
    page = Page(72, 96, seed, paper=(245, 234, 205), edge=(210, 178, 126), ink=(118, 82, 46), fold_width=7)
    page.age(mottle=0.035, grain=0.008, yellowing=0.5, wear=4.0)
    page.fibres(70)
    page.foxing(3, strength=0.10)
    page.fold(depth=0.06)
    page.deckle()
    # The books write from x 7 (the list's grid), y 5 and down to y 92 (the arrows), at least 7 texels from the fold:
    # the frame runs 2 texels from the outer edges and 6 from the fold, its knots never reaching what is written.
    page.frame(left=6, top=2, right=69, bottom=93, strength=0.56)
    page.diamond(69, 48, 0.50)
    page.diamond(6, 48, 0.36)
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
        page = Page(115, 154, seed, paper=(238, 228, 206), edge=(188, 160, 128), ink=(72, 46, 122), fold_width=22)
    else:
        page = Page(115, 154, seed, paper=(245, 233, 204), edge=(208, 174, 120), ink=(116, 80, 44), fold_width=22)
    page.age(mottle=0.045, grain=0.01, yellowing=0.55 if not mystic else 0.65, wear=6.0)
    page.fibres(160)
    page.foxing(4, strength=0.10)
    page.fold(depth=0.10)
    page.deckle()
    # Flat part from x 17: the frame 3 texels inside the outer edges and 3 past the gutter's curve; a dotted hairline 2 inside it.
    page.frame(left=20, top=2, right=112, bottom=151, strength=0.58, inner=2, inner_strength=0.30)
    if mystic:
        # The awakening book: violet ink, a star at each corner and halfway down each side, and the table's ring as a watermark.
        for cx, cy in ((20, 2), (112, 2), (20, 151), (112, 151)):
            page.star(cx, cy, 0.65)
        page.star(112, 76, 0.55)
        page.star(20, 76, 0.42)
        page.star(66, 151, 0.50)
        page.star(66, 2, 0.50)
        page.ring(66, 77, 30, 0.07)
        page.ring(66, 77, 24, 0.05, gap=3)
        page.ring(66, 77, 9, 0.06)
    else:
        page.diamond(112, 76, 0.55, size=2)
        page.diamond(20, 76, 0.40, size=2)
        page.diamond(66, 151, 0.45, size=2)
        page.diamond(66, 2, 0.45, size=2)
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


def main():
    FALLOW.mkdir(parents=True, exist_ok=True)
    HOV.mkdir(parents=True, exist_ok=True)
    recipe_paper(FALLOW / "livre_de_recettes_papier.png", seed=11)
    recipe_paper(HOV / "recipe_book_paper.png", seed=11)
    big_paper(FALLOW / "livre_d_eveil_papier.png", seed=23, mystic=True)
    big_paper(HOV / "enchanting_book_paper.png", seed=31, mystic=False)
    mystic_cover(Path(__file__).parent / "source/enchanting_book.png", FALLOW / "livre_d_eveil.png")


if __name__ == "__main__":
    main()
