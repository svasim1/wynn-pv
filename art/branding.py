"""Wynn PV's icon and banner, drawn pixel by pixel in a medieval palette like Wynnav's.

The chosen designs are the pinned note (icon) and the notice on the planks (banner). Run
`python3 art/branding.py` to write them: the mod icon at 4x, the README logo at 16x and banner at
4x, all blown up by whole pixels with no smoothing. The other options are kept and written to
art/preview/branding/ for comparison.
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pixelart import png  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "art", "preview", "branding")
ICON = os.path.join(ROOT, "src", "main", "resources", "assets", "wynnpv", "icon.png")
DOCS = os.path.join(ROOT, "docs")

PAL = {
    ".": None,
    "K": (0x14, 0x0E, 0x0A),  # outline (warm black)
    # gold
    "1": (0xFF, 0xF1, 0xA8), "2": (0xF6, 0xC8, 0x4C), "3": (0xC8, 0x8A, 0x24), "4": (0x8A, 0x55, 0x14),
    # iron
    "a": (0xD6, 0xDA, 0xDE), "b": (0x9C, 0xA4, 0xAE), "c": (0x6A, 0x72, 0x7E), "d": (0x40, 0x46, 0x52),
    # oak, light to dark
    "u": (0xC4, 0x88, 0x4C), "w": (0xA0, 0x68, 0x38), "x": (0x7A, 0x4C, 0x28), "z": (0x55, 0x33, 0x1A), "y": (0x38, 0x22, 0x12),
    # crimson / wax
    "r": (0xE0, 0x4A, 0x46), "s": (0xA8, 0x28, 0x30), "t": (0x6C, 0x16, 0x20),
    # emerald
    "e": (0x9C, 0xF6, 0xB0), "f": (0x3C, 0xD0, 0x6C), "g": (0x1C, 0x8C, 0x44), "h": (0x0C, 0x52, 0x28),
    # parchment
    "P": (0xF6, 0xE6, 0xC0), "Q": (0xE8, 0xD2, 0xA0), "R": (0xC8, 0xA6, 0x6C), "S": (0x8E, 0x6C, 0x40),
    # ink
    "i": (0x4A, 0x32, 0x1C), "j": (0x7A, 0x5C, 0x3C),
    # skin and hair for the portrait
    "k": (0xE8, 0xB8, 0x90), "l": (0xC8, 0x90, 0x6C), "m": (0x5C, 0x3A, 0x22), "n": (0x3A, 0x24, 0x14),
    # glass
    "G": (0xCC, 0xEE, 0xFF), "H": (0x8C, 0xC8, 0xEC), "I": (0x5A, 0x96, 0xC8),
    # leaf green for laurels
    "L": (0x9C, 0xD0, 0x5C), "M": (0x5C, 0x98, 0x34), "N": (0x34, 0x60, 0x20),
    # oxblood leather
    "o": (0x7E, 0x38, 0x2C), "p": (0x5E, 0x27, 0x1F), "q": (0x42, 0x19, 0x13),
    "W": (0xFF, 0xFF, 0xFF),
}


def canvas(w, h):
    return [["." for _ in range(w)] for _ in range(h)]


def put(g, x, y, c):
    if 0 <= y < len(g) and 0 <= x < len(g[0]):
        g[y][x] = c


def get(g, x, y):
    return g[y][x] if 0 <= y < len(g) and 0 <= x < len(g[0]) else "."


def stamp(g, rows, ox, oy, skip="."):
    for j, row in enumerate(rows):
        for i, c in enumerate(row):
            if c != skip:
                put(g, ox + i, oy + j, c)


def rect(g, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(g, x, y, c)


def outline(g, color="K", diagonal=False):
    """Draws `color` on every empty pixel that touches a filled one."""
    src = [r[:] for r in g]
    nb = [(1, 0), (-1, 0), (0, 1), (0, -1)] + ([(1, 1), (-1, -1), (1, -1), (-1, 1)] if diagonal else [])
    for y in range(len(g)):
        for x in range(len(g[0])):
            if src[y][x] == "." and any(get(src, x + dx, y + dy) not in (".", color) for dx, dy in nb):
                g[y][x] = color


def grid(text):
    return [list(r) for r in text.strip("\n").split("\n")]


# Letters for "Wynn PV", 11 pixels tall for capitals; "#" is filled.
GLYPHS = {
    "W": """
###.......###
.##.......##.
.##...#...##.
.##..###..##.
..##.###.##..
..##.#.#.##..
..##.#.#.##..
...###.###...
...###.###...
...##...##...
....#...#....
""",
    "y": """
###..###
.##...##
.##...##
..##.##.
..##.##.
...###..
...###..
...##...
...##...
..##....
####....
""",
    "n": """
###.##..
.###.##.
.##...##
.##...##
.##...##
.##...##
.##...##
###..###
""",
    "P": """
#######..
.##...##.
.##....##
.##....##
.##...##.
.######..
.##......
.##......
.##......
.##......
####.....
""",
    "V": """
###.....###
.##.....##.
.##.....##.
..##...##..
..##...##..
..##...##..
...##.##...
...##.##...
....###....
....###....
.....#.....
""",
}


def word_mask(text):
    cap = 11
    width = 0
    parts = []
    for ch in text:
        if ch == " ":
            width += 4
            continue
        rows = [r for r in GLYPHS[ch].strip("\n").split("\n")]
        top = 0 if ch.isupper() else cap - 8
        parts.append((width, top, rows))
        width += len(rows[0]) + 1
    width -= 1
    m = [[False] * width for _ in range(cap + 3)]
    for x0, top, rows in parts:
        for j, row in enumerate(rows):
            for i, c in enumerate(row):
                if c == "#":
                    m[top + j][x0 + i] = True
    return m


def stamp_word(g, text, ox, oy, light, mid, dark, outline_color="K", shadow=None):
    """Letters lit from the top left, outlined, with an optional drop shadow."""
    m = word_mask(text)
    h, w = len(m), len(m[0])
    on = lambda x, y: 0 <= x < w and 0 <= y < h and m[y][x]
    if shadow:
        for y in range(h):
            for x in range(w):
                if m[y][x]:
                    put(g, ox + x + 1, oy + y + 1, shadow)
    for y in range(h):
        for x in range(w):
            if m[y][x]:
                c = light if not on(x, y - 1) or not on(x - 1, y) else dark if not on(x, y + 1) or not on(x + 1, y) else mid
                put(g, ox + x, oy + y, c)
    if outline_color:
        for y in range(-1, h + 1):
            for x in range(-1, w + 1):
                if not on(x, y) and any(on(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    if get(g, ox + x, oy + y) != shadow or shadow is None:
                        put(g, ox + x, oy + y, outline_color)
    return w, h


# Shared pieces.

FACE = grid("""
.nnnnnn.
nnmmmmnn
nmkkkkmn
nkkkkkkn
nkKkkKkn
nkkkkkkn
nklkklkn
.nkllkn.
""")

NAIL = grid("""
.K.
KbK
.K.
""")

SEAL = grid("""
.ssss.
srrrss
srssts
srssts
ssttts
.ssts.
""")


def note(g, x0, y0, x1, y1, tears=True):
    """A parchment note with a darker aged edge and a few torn notches."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            edge = x in (x0, x1) or y in (y0, y1)
            put(g, x, y, "Q" if edge else "P")
    if tears:
        for (x, y) in ((x0 + 3, y0), (x1 - 4, y0), (x0, y0 + 5), (x1, y1 - 4), (x0 + 5, y1), (x1 - 2, y1)):
            put(g, x, y, ".")
    for y in range(y0 + 1, y1):
        put(g, x1 - 1, y, "Q")
    for x in range(x0 + 1, x1):
        put(g, x, y1 - 1, "Q")


def ink_lines(g, x0, x1, ys, color="j"):
    for y in ys:
        for x in range(x0, x1 + 1):
            put(g, x, y, color)


def planks(g, x0, y0, x1, y1, rows=(5,)):
    """Oak planks running across, with dark seams and grain."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            k = (y - y0) % 6
            c = "y" if k == 5 else ("w" if k == 0 else "x")
            if k in (2, 3) and (x * 7 + y * 3) % 11 == 0:
                c = "z"
            put(g, x, y, c)
    for y in range(y0, y1 + 1):
        seam = x0 + ((y - y0) // 6 * 9 + 5) % max(1, (x1 - x0))
        if (y - y0) % 6 != 5:
            put(g, seam, y, "y")


# Icons, 32x32.

def icon_note():
    """A parchment note with a portrait, nailed to an oak board and sealed in red wax."""
    g = canvas(32, 32)
    planks(g, 1, 1, 30, 30)
    for i in range(32):
        for (x, y) in ((i, 0), (i, 31), (0, i), (31, i)):
            put(g, x, y, "K")
    for (x, y) in ((1, 1), (30, 1), (1, 30), (30, 30)):
        put(g, x, y, "K")
    # shadow, then the note
    rect(g, 8, 6, 26, 28, "y")
    note(g, 6, 4, 24, 26)
    for y in range(4, 27):
        for x in range(6, 25):
            if get(g, x, y) == ".":
                put(g, x, y, "x")
    stamp(g, FACE, 11, 8)
    ink_lines(g, 9, 21, (18,), "i")
    ink_lines(g, 9, 19, (20,))
    ink_lines(g, 9, 17, (22,))
    stamp(g, NAIL, 14, 3)
    stamp(g, SEAL, 18, 21)
    return g


def icon_magnifier():
    """A profile card with a gold-rimmed magnifying glass looking at it."""
    g = canvas(32, 32)
    note(g, 2, 2, 22, 26, tears=False)
    stamp(g, FACE, 5, 5)
    ink_lines(g, 15, 19, (6,), "s")
    ink_lines(g, 15, 18, (8,))
    ink_lines(g, 15, 19, (10,))
    ink_lines(g, 5, 18, (16,), "i")
    ink_lines(g, 5, 15, (18,))
    ink_lines(g, 5, 17, (20,))
    ink_lines(g, 5, 13, (22,))
    # lens: glass inside a gold ring
    cx, cy, r = 19.5, 17.5, 7.2
    for y in range(32):
        for x in range(32):
            d = math.hypot(x - cx, y - cy)
            if d <= r - 1.6:
                lit = (x - cx) + (y - cy) < -3
                put(g, x, y, "G" if lit else "H" if d < r - 3.5 else "I")
            elif d <= r:
                put(g, x, y, "2" if (x - cx) + (y - cy) < 0 else "3")
    for (x, y) in ((16, 14), (17, 14), (16, 15)):
        put(g, x, y, "W")
    # handle
    for i in range(7):
        for w in (0, 1):
            put(g, 24 + i + w, 23 + i, "x" if w else "w")
    put(g, 25, 23, "3")
    put(g, 24, 24, "3")
    outline(g)
    return g


def icon_crest():
    """A crimson shield with a gold star, framed by a laurel wreath: renown."""
    g = canvas(32, 32)
    # Two laurel branches curving up from a gold tie at the bottom: a continuous stem with a leaf
    # every few pixels, alternating outwards and inwards.
    cx = 15.5
    for side in (-1, 1):
        steps = 48
        for k in range(steps):
            t = k / (steps - 1)
            angle = math.radians(-78 + t * 150)
            sx = int(round(cx + side * 13.0 * math.cos(angle)))
            sy = int(round(16.5 - 12.5 * math.sin(angle)))
            put(g, sx, sy, "N")
            if k % 5 == 2 and k < steps - 3:
                out = side if (k // 5) % 2 == 0 else -side
                put(g, sx + out, sy, "M")
                put(g, sx + out, sy - 1, "L")
                put(g, sx + 2 * out, sy - 1, "M")
    for (x, y) in ((14, 29), (15, 29), (16, 29), (17, 29), (15, 30), (16, 30)):
        put(g, x, y, "2")
    # shield
    for y in range(5, 28):
        for x in range(8, 24):
            half = 7.5 - max(0, y - 18) * 0.9
            if abs(x - 15.5) <= half:
                put(g, x, y, "r" if x < 15 else "s")
    # gold trim, judged on the shield as drawn so far
    before = [row[:] for row in g]
    for y in range(5, 28):
        for x in range(8, 24):
            if get(before, x, y) in ("r", "s") and any(get(before, x + dx, y + dy) not in ("r", "s")
                                                         for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                put(g, x, y, "2" if x < 16 else "3")
    # a four-pointed star with an emerald heart
    star = grid("""
....1....
....12...
...1123..
1111e2333
.33fff33.
..3gfg3..
...3g3...
....3....
....3....
""")
    stamp(g, star, 11, 9)
    outline(g)
    return g


def icon_scroll():
    """A half-unrolled scroll with ink lines and a red wax seal on a ribbon."""
    g = canvas(32, 32)
    # parchment sheet
    for y in range(7, 25):
        for x in range(6, 26):
            put(g, x, y, "P")
    for x in range(6, 26):
        put(g, x, 24, "Q")
    # rolled ends top and bottom
    for x in range(4, 28):
        for y, c in ((4, "Q"), (5, "P"), (6, "R"), (25, "Q"), (26, "R"), (27, "S")):
            put(g, x, y, c)
    for y in (4, 5, 6, 25, 26, 27):
        put(g, 3, y, "R")
        put(g, 28, y, "S")
    ink_lines(g, 9, 22, (10,), "i")
    ink_lines(g, 9, 20, (12,))
    ink_lines(g, 9, 22, (14,))
    ink_lines(g, 9, 17, (16,))
    # ribbon and seal
    for y in range(17, 31):
        put(g, 13, y, "s")
        put(g, 14, y, "t")
    for y in range(17, 30):
        put(g, 18, y, "s")
        put(g, 19, y, "t")
    seal = grid("""
..ssss..
.srrrss.
srrssts.
srsrsts.
srssrts.
ssttttss
.sstts..
..ssss..
""")
    stamp(g, seal, 12, 15)
    put(g, 15, 18, "1")
    outline(g)
    return g


# Banners.

def banner_sign():
    """An oak sign hanging on iron chains, with the name carved in gold."""
    W, H = 92, 34
    g = canvas(W, H)
    for x in (14, 77):
        for y in range(0, 7):
            put(g, x, y, "c" if y % 2 else "b")
    for y in range(7, 31):
        for x in range(2, 90):
            k = (y - 7) % 8
            c = "y" if k == 7 else ("u" if k == 0 else "w" if k < 4 else "x")
            if k in (2, 5) and (x * 5 + y) % 13 == 0:
                c = "z"
            put(g, x, y, c)
    for (x, y) in ((5, 10), (86, 10), (5, 27), (86, 27)):
        stamp(g, NAIL, x - 1, y - 1)
    outline(g)
    m = word_mask("Wynn PV")
    stamp_word(g, "Wynn PV", (W - len(m[0])) // 2, 12, "1", "2", "3", outline_color="K", shadow="y")
    return g


def banner_scroll():
    """A parchment scroll with curled ends, the name in dark ink with a red capital."""
    W, H = 92, 34
    g = canvas(W, H)
    for y in range(6, 29):
        for x in range(10, 82):
            wave = y in (6, 28) and (x // 6) % 2 == 0
            put(g, x, y, "Q" if wave or y in (7, 27) else "P")
    # curls: rolled cylinders at both ends, lit down the middle, with rounded caps
    shades = ("S", "R", "Q", "P", "P", "Q", "R", "S")
    for x0 in (3, 81):
        for i, c in enumerate(shades):
            cap = 1 if i in (0, 7) else 0
            for y in range(3 + cap, 32 - cap):
                put(g, x0 + i, y, c)
        # the edge of the sheet curling into the roll
        for y in range(5, 30, 4):
            put(g, x0 + (6 if x0 < 40 else 1), y, "R")
    outline(g)
    m = word_mask("Wynn PV")
    ox = (W - len(m[0])) // 2
    stamp_word(g, "Wynn PV", ox, 11, "i", "i", "n", outline_color=None, shadow="R")
    # red capitals, like a rubric
    w_width = len(GLYPHS["W"].strip("\n").split("\n")[0])
    for y in range(11, 25):
        for x in range(ox, ox + w_width):
            if g[y][x] in ("i", "n"):
                g[y][x] = "s" if g[y][x] == "i" else "t"
    return g


def banner_notice():
    """A torn notice nailed on dark planks with a wax seal, like the profile screen itself."""
    W, H = 92, 34
    g = canvas(W, H)
    planks(g, 1, 1, 90, 32)
    for x in range(W):
        put(g, x, 0, "K")
        put(g, x, H - 1, "K")
    for y in range(H):
        put(g, 0, y, "K")
        put(g, W - 1, y, "K")
    rect(g, 9, 6, 84, 30, "y")
    note(g, 7, 4, 82, 28)
    for y in range(4, 29):
        for x in range(7, 83):
            if get(g, x, y) == ".":
                put(g, x, y, "x")
    stamp(g, NAIL, 9, 5)
    stamp(g, NAIL, 78, 5)
    m = word_mask("Wynn PV")
    stamp_word(g, "Wynn PV", (W - len(m[0])) // 2 - 4, 9, "i", "i", "n", outline_color=None, shadow="R")
    big_seal = grid("""
..ssss..
.srrrss.
srrssts.
srsrsts.
srssrts.
ssttttss
.sstts..
..ssss..
""")
    # pressed onto the note's corner, below the letters
    stamp(g, big_seal, 76, 21)
    return g


def banner_ribbon():
    """A crimson heraldic ribbon with folded swallowtail ends and gold letters."""
    W, H = 92, 34
    g = canvas(W, H)
    # tails behind the band, lower down, with a V cut at the outer end
    for y in range(11, 31):
        for x in range(1, 17):
            notch = abs(y - 21) < 6 - x
            if not notch:
                put(g, x, y, "t" if y > 12 else "s")
        for x in range(75, 91):
            notch = abs(y - 21) < 6 - (90 - x)
            if not notch:
                put(g, x, y, "t" if y > 12 else "s")
    # the folds where the band turns behind itself
    for y in range(27, 31):
        for x in range(13, 17):
            if x - 13 <= y - 27:
                put(g, x, y, "K")
        for x in range(75, 79):
            if 78 - x <= y - 27:
                put(g, x, y, "K")
    # the band, with gold edging
    for y in range(6, 28):
        for x in range(13, 79):
            put(g, x, y, "r" if y < 9 else "s")
    for x in range(13, 79):
        put(g, x, 7, "2")
        put(g, x, 26, "3")
    outline(g)
    m = word_mask("Wynn PV")
    stamp_word(g, "Wynn PV", (W - len(m[0])) // 2, 10, "1", "2", "3", outline_color="K", shadow="t")
    return g


def banner_stone():
    """A grey stone slab with the name chiselled in, cracked and grown over with moss."""
    W, H = 92, 34
    g = canvas(W, H)
    rng = random.Random(4)
    for y in range(2, 32):
        for x in range(2, 90):
            corner = (x in (2, 89) and y in (2, 31))
            if corner:
                continue
            r = rng.random()
            put(g, x, y, "c" if r < 0.10 else "a" if r < 0.15 else "b")
    for x in range(3, 89):
        put(g, x, 3, "a")
        put(g, x, 30, "d")
    for y in range(3, 31):
        put(g, 3, y, "a")
        put(g, 88, y, "d")
    # cracks
    for (x, y) in ((70, 4), (71, 5), (71, 6), (72, 7), (73, 8), (73, 9), (12, 29), (13, 28), (13, 27), (14, 26)):
        put(g, x, y, "d")
    # moss creeping up from the bottom corners
    for (x, y, c) in ((4, 29, "M"), (5, 29, "N"), (6, 30, "M"), (4, 28, "L"), (5, 30, "N"), (7, 30, "M"), (4, 27, "M"),
                      (84, 30, "M"), (85, 29, "M"), (86, 30, "N"), (87, 29, "L"), (87, 28, "M"), (86, 29, "N"), (83, 30, "N")):
        put(g, x, y, c)
    outline(g)
    # Chiselled letters: shadow along the top and left inside each stroke, light along the bottom and right.
    m = word_mask("Wynn PV")
    stamp_word(g, "Wynn PV", (W - len(m[0])) // 2, 10, "K", "d", "c", outline_color=None, shadow=None)
    ox, oy = (W - len(m[0])) // 2, 10
    for y in range(len(m)):
        for x in range(len(m[0])):
            if not m[y][x] and ((x > 0 and m[y][x - 1]) or (y > 0 and m[y - 1][x])):
                if get(g, ox + x, oy + y) in ("b", "c"):
                    put(g, ox + x, oy + y, "a")
    return g


def banner_leather():
    """An oxblood leather label tooled with a gold border, like the cover of an old book."""
    W, H = 92, 34
    g = canvas(W, H)
    for y in range(2, 32):
        for x in range(2, 90):
            if (x in (2, 89) and y in (2, 31)):
                continue
            put(g, x, y, "p" if (x * 7 + y * 3) % 19 else "q")
    for x in range(3, 89):
        put(g, x, 3, "o")
    for y in range(3, 31):
        put(g, 3, y, "o")
    # a double gold line with diamond corner ornaments
    for x in range(7, 85):
        put(g, x, 6, "3")
        put(g, x, 27, "3")
    for y in range(6, 28):
        put(g, 7, y, "3")
        put(g, 84, y, "3")
    for x in range(9, 83):
        put(g, x, 8, "4")
        put(g, x, 25, "4")
    for y in range(8, 26):
        put(g, 9, y, "4")
        put(g, 82, y, "4")
    diamond = grid("""
..2..
.212.
21312
.232.
..2..
""")
    for (x, y) in ((5, 4), (82, 4), (5, 25), (82, 25)):
        stamp(g, diamond, x, y)
    outline(g)
    m = word_mask("Wynn PV")
    stamp_word(g, "Wynn PV", (W - len(m[0])) // 2, 10, "1", "2", "3", outline_color="q", shadow=None)
    return g


def banner_lockup():
    """The icon beside the name in gold: a logo with no background of its own."""
    m = word_mask("Wynn PV")
    W, H = 32 + 6 + len(m[0]) + 2, 34
    g = canvas(W, H)
    stamp(g, icon_note(), 0, 1)
    stamp_word(g, "Wynn PV", 38, 10, "1", "2", "3", outline_color="K", shadow="4")
    return g


ICONS = {"note": icon_note, "magnifier": icon_magnifier, "crest": icon_crest, "scroll": icon_scroll}
BANNERS = {"notice": banner_notice, "ribbon": banner_ribbon, "stone": banner_stone, "leather": banner_leather,
           "lockup": banner_lockup}


def colors(g):
    return [[PAL[c] for c in row] for row in g]


def sheet(items, scale, gap=8, background=(0x2A, 0x2D, 0x33)):
    """All options side by side on a dark background, each blown up by `scale`."""
    width = sum(len(g[0]) * scale for g in items) + gap * (len(items) + 1)
    height = max(len(g) * scale for g in items) + 2 * gap
    out = [[background] * width for _ in range(height)]
    x0 = gap
    for g in items:
        c = colors(g)
        for y in range(len(g) * scale):
            for x in range(len(g[0]) * scale):
                px = c[y // scale][x // scale]
                if px is not None:
                    out[gap + y][x0 + x] = px
        x0 += len(g[0]) * scale + gap
    return out


def column(items, scale, gap=8, background=(0x2A, 0x2D, 0x33)):
    """Options stacked top to bottom on a dark background, each blown up by `scale`."""
    width = max(len(g[0]) * scale for g in items) + 2 * gap
    height = sum(len(g) * scale for g in items) + gap * (len(items) + 1)
    out = [[background] * width for _ in range(height)]
    y0 = gap
    for g in items:
        c = colors(g)
        for y in range(len(g) * scale):
            for x in range(len(g[0]) * scale):
                px = c[y // scale][x // scale]
                if px is not None:
                    out[y0 + y][gap + x] = px
        y0 += len(g) * scale + gap
    return out


def main():
    icon = colors(icon_note())
    banner = colors(banner_notice())
    png(ICON, icon, scale=4)
    png(os.path.join(DOCS, "logo.png"), icon, scale=16)
    png(os.path.join(DOCS, "banner.png"), banner, scale=4)

    os.makedirs(OUT, exist_ok=True)
    icons = {name: f() for name, f in ICONS.items()}
    banners = {name: f() for name, f in BANNERS.items()}
    for name, g in icons.items():
        png(os.path.join(OUT, f"icon-{name}.png"), colors(g))
        png(os.path.join(OUT, f"icon-{name}-4x.png"), colors(g), scale=4)
    for name, g in banners.items():
        png(os.path.join(OUT, f"banner-{name}.png"), colors(g))
        png(os.path.join(OUT, f"banner-{name}-4x.png"), colors(g), scale=4)
    png(os.path.join(OUT, "options-icons.png"), sheet(list(icons.values()), 6))
    png(os.path.join(OUT, "options-banners-new.png"), column(list(banners.values()), 4))


if __name__ == "__main__":
    main()
