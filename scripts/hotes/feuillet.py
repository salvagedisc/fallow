"""The Hosts' dialogue drawn in the books' manner: one loose sheet of the books' paper, the portrait set in it as an engraving's
plate, and the answers' pictograms in the books' inks (brown inks, never black; the rubric red).

The sheet is a GUI sprite scaled by nine-slice: at the window's usual size (360 x 155) it is drawn texel for texel, a soft shadow
under it baked in the texture. The paper is made by papers.Page, as every book's, from a fixed seed: a rerun gives the same files.

Run from the repository root: python3 scripts/hotes/feuillet.py
Writes, under mods-custom/fallow/src/main/resources/assets/fallow/textures/gui/:
  sprites/hote/feuillet.png (and its .mcmeta)
  hote/portrait_cadre.png
  hote/icones.png
"""

import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "scripts/books"))
from papers import Page, mix  # noqa: E402

GUI = ROOT / "mods-custom/fallow/src/main/resources/assets/fallow/textures/gui"

INK = (59, 42, 26)
INK_SOFT = (138, 107, 69)
INK_FAINT = (185, 162, 124)
PAPER = (249, 241, 218)
PAPER_SHADE = (234, 220, 184)
EDGE = (212, 180, 126)
TAN = (214, 186, 136)
RED = (163, 50, 42)

W, H = 360, 155
LEFT, TOP, RIGHT, FOOT = 1, 1, 3, 3
BORDER = 12


def sheet(seed=4107):
    """A loose sheet, worn the same on its four sides: clouds and grain, yellowing towards every edge, a darker hairline round
    it, a few nicks and two corners rounded by handling."""
    p = Page(W, H, seed, paper=PAPER, edge=EDGE, ink=INK_SOFT, fold_width=1)
    p.outer = np.minimum(np.minimum(p.ys, H - 1 - p.ys), np.minimum(p.xs, W - 1 - p.xs))
    p.clouds(strength=0.022, light_patches=6)
    p.age(mottle=0.0, grain=0.007, yellowing=0.48, wear=5.0)
    p.fibres(420)
    p.foxing(5, strength=0.13)
    p.worn_corners(radius=2)
    p.nicks(4)
    # The left side gets the same wear, drawn on the sheet turned over.
    p.rgb, p.alpha = p.rgb[:, ::-1].copy(), p.alpha[:, ::-1].copy()
    p.worn_corners(radius=2)
    p.nicks(3, sides=1)
    p.thumbed(radius=16.0, strength=0.09)
    p.rgb, p.alpha = p.rgb[:, ::-1].copy(), p.alpha[:, ::-1].copy()
    rim = np.asarray(EDGE, np.float32) * 0.78
    for x in range(W):
        p.rgb[0, x] = mix(p.rgb[0, x][None, None], rim, 0.50)[0, 0]
        p.rgb[H - 1, x] = mix(p.rgb[H - 1, x][None, None], rim * 0.85, 0.68)[0, 0]
    for y in range(H):
        p.rgb[y, 0] = mix(p.rgb[y, 0][None, None], rim, 0.55)[0, 0]
        p.rgb[y, W - 1] = mix(p.rgb[y, W - 1][None, None], rim * 0.9, 0.62)[0, 0]
    page = p.image()

    # The shadow the sheet casts on the world: one texel up and left, three down and right, soft.
    cw, ch = LEFT + W + RIGHT, TOP + H + FOOT
    a = np.zeros((ch, cw), np.float32)
    mask = (np.asarray(page)[:, :, 3] > 0).astype(np.float32)
    for dx, dy, k in ((1, 1, 0.30), (2, 2, 0.20), (3, 3, 0.10), (0, 1, 0.18), (1, 0, 0.10), (-1, 0, 0.06), (0, -1, 0.06)):
        a[TOP + dy:TOP + dy + H, LEFT + dx:LEFT + dx + W] = np.maximum(a[TOP + dy:TOP + dy + H, LEFT + dx:LEFT + dx + W], mask * k)
    shadow = np.dstack([np.full((ch, cw, 3), (20, 14, 8), np.float32), a * 255.0])
    out = Image.fromarray(np.clip(np.rint(shadow), 0, 255).astype(np.uint8), "RGBA")
    out.alpha_composite(page, (LEFT, TOP))
    path = GUI / "sprites/hote/feuillet.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    out.save(path)
    meta = {"gui": {"scaling": {"type": "nine_slice", "width": cw, "height": ch, "border": BORDER}}}
    path.with_suffix(".png.mcmeta").write_text(json.dumps(meta, indent=2) + "\n")


def frame():
    """The portrait's frame, 50 x 50 round a 40 x 40 picture: an engraving's plate pressed into the paper. The paper outside the
    plate is left clear; a fine soft-ink line, a pressed band a shade darker, and the plate's dark edge round the picture."""
    img = Image.new("RGBA", (50, 50), (0, 0, 0, 0))
    px = img.load()
    for y in range(50):
        for x in range(50):
            d = min(x, y, 49 - x, 49 - y)
            if d == 0:
                c = (*INK_SOFT, 150) if 0 < x < 49 and 0 < y < 49 else None
            elif d in (1, 2):
                # The pressed band: lit on the top and left, shaded on the right and foot.
                lit = (x <= 2 or y <= 2) and not (x >= 47 or y >= 47)
                c = (*(PAPER if lit else PAPER_SHADE), 255) if d == 2 else (*(PAPER_SHADE if lit else TAN), 255)
            elif d == 3:
                c = (*INK_FAINT, 255)
            elif d == 4:
                c = (*INK, 255)
            else:
                c = None
            if c:
                px[x, y] = c
    img.save(GUI / "hote/portrait_cadre.png")


ICONS = [
    # The bundle: a cloth gathered up, two of its corners knotted on top with a rubric-red tie.
    ["...........",
     "..o.....o..",
     ".oLo...oLo.",
     "..oLoroLo..",
     "....oro....",
     "..ooLLLoo..",
     ".oLLLLLLLo.",
     ".oLLLLLLSo.",
     ".oLLLLLSSo.",
     "..ooooooo..",
     "..........."],
    # A question: a written question mark, its point in rubric red.
    ["...........",
     "....ooo....",
     "...o...o...",
     ".......o...",
     "......o....",
     ".....o.....",
     ".....o.....",
     "...........",
     ".....r.....",
     "...........",
     "..........."],
    # Leaving: an arrow out through a doorway.
    ["...........",
     ".ooooo.....",
     ".o...o.....",
     ".o...o.o...",
     ".o...o..o..",
     ".o.ooooooo.",
     ".o...o..o..",
     ".o...o.o...",
     ".o...o.....",
     ".ooooo.....",
     "..........."],
    # Back: an arrow curling back to the left.
    ["...........",
     "...o.......",
     "..o........",
     ".ooooooo...",
     "..o.....o..",
     "...o.....o.",
     ".........o.",
     "........o..",
     "....oooo...",
     "...........",
     "..........."],
    # A question already asked: the same mark in faint ink, ticked.
    ["...........",
     "...fff.....",
     "..f...f....",
     "......f....",
     ".....f.....",
     "....f......",
     "....f....o.",
     "........o..",
     "....fo.o...",
     "......o....",
     "..........."],
    # Nothing to leave: the cloth spread out, empty, its tie undone and lying on it.
    ["...........",
     "...........",
     ".....o.....",
     "...ooLoo...",
     "..oLLLLLo..",
     ".oLrrLLLSo.",
     "..oLLrLSo..",
     "...ooSoo...",
     ".....o.....",
     "...........",
     "..........."],
]


def icons():
    pal = {"o": INK, "s": INK_SOFT, "f": INK_FAINT, "L": TAN, "S": (190, 160, 112), "r": RED}
    img = Image.new("RGBA", (11 * len(ICONS), 11), (0, 0, 0, 0))
    for i, grid in enumerate(ICONS):
        for y, row in enumerate(grid):
            for x, ch in enumerate(row):
                if ch != ".":
                    img.putpixel((i * 11 + x, y), (*pal[ch], 255))
    img.save(GUI / "hote/icones.png")


if __name__ == "__main__":
    sheet()
    frame()
    icons()
