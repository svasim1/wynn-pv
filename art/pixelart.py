"""Wynn PV's pixel art: an old leather-bound tome. Every sprite is an exact pixel grid in one shared
palette, drawn at one texel per GUI pixel so it stays crisp at every GUI scale. Sprites that grow
with the screen are nine-slice GUI sprites (a .png.mcmeta next to each) whose edges and middle tile.

Run `python3 art/pixelart.py` to regenerate the textures, or add `--preview` to also write enlarged
previews to art/preview/.
"""
import json
import os
import random
import struct
import sys
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SPRITES = os.path.join(ROOT, "src", "main", "resources", "assets", "wynnpv", "textures", "gui", "sprites")
PREVIEW = os.path.join(ROOT, "art", "preview")

# Light comes from the top left.
LEATHER_OUT = (0x1F, 0x0E, 0x0A)
LEATHER_D = (0x48, 0x1D, 0x17)
LEATHER = (0x5A, 0x26, 0x1E)
LEATHER_L = (0x68, 0x2F, 0x25)
LEATHER_HI = (0x80, 0x3D, 0x2F)
STITCH = (0xB8, 0x8E, 0x58)
STITCH_D = (0x7A, 0x56, 0x30)
GOLD_OUT = (0x46, 0x2A, 0x0C)
GOLD_D = (0xA4, 0x70, 0x22)
GOLD = (0xDC, 0xAC, 0x48)
GOLD_HI = (0xFF, 0xE2, 0x8C)
PARCH_HI = (0xF5, 0xE8, 0xC8)
PARCH = (0xED, 0xDB, 0xB2)
PARCH_1 = (0xE3, 0xCE, 0x9F)
PARCH_2 = (0xD5, 0xBC, 0x89)
PARCH_3 = (0xC2, 0xA4, 0x70)
EDGE_L = (0xE4, 0xD0, 0xA2)
EDGE_D = (0xBE, 0xA0, 0x6E)
PAGE_OUT = (0x4E, 0x38, 0x22)
INK_FADED = (0x8A, 0x6A, 0x46)
INK = (0x4A, 0x32, 0x1C)
CLEAR = None


def png(path, pixels, scale=1):
    h = len(pixels)
    w = len(pixels[0])
    raw = bytearray()
    for y in range(h * scale):
        raw.append(0)
        for x in range(w * scale):
            c = pixels[y // scale][x // scale]
            raw += b"\0\0\0\0" if c is None else bytes(c if len(c) == 4 else c + (255,))

    def chunk(t, d):
        return struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d) & 0xFFFFFFFF)

    data = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w * scale, h * scale, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b""))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(data)


def canvas(w, h, color=CLEAR):
    return [[color] * w for _ in range(h)]


def mirror_x(pixels):
    return [list(reversed(row)) for row in pixels]


def nine_slice(name, pixels, left, top, right, bottom):
    """Writes a GUI sprite and its nine-slice metadata; edges and middle tile when drawn larger."""
    path = os.path.join(SPRITES, name + ".png")
    png(path, pixels)
    meta = {"gui": {"scaling": {"type": "nine_slice", "width": len(pixels[0]), "height": len(pixels),
                                "border": {"left": left, "top": top, "right": right, "bottom": bottom}}}}
    with open(path + ".mcmeta", "w") as f:
        json.dump(meta, f, indent=2)
        f.write("\n")
    return pixels


def tiled(name, pixels):
    """Writes a GUI sprite that repeats to fill whatever size it is drawn at."""
    path = os.path.join(SPRITES, name + ".png")
    png(path, pixels)
    meta = {"gui": {"scaling": {"type": "tile", "width": len(pixels[0]), "height": len(pixels)}}}
    with open(path + ".mcmeta", "w") as f:
        json.dump(meta, f, indent=2)
        f.write("\n")
    return pixels


def plain(name, pixels):
    png(os.path.join(SPRITES, name + ".png"), pixels)
    return pixels


def speckle(pixels, rng, base, spots, x0=0, y0=0, x1=None, y1=None):
    """Fills a rectangle with `base` and sprinkles (color, chance) spots over it."""
    x1 = len(pixels[0]) if x1 is None else x1
    y1 = len(pixels) if y1 is None else y1
    for y in range(y0, y1):
        for x in range(x0, x1):
            c = base
            r = rng.random()
            for color, chance in spots:
                if r < chance:
                    c = color
                    break
                r -= chance
            pixels[y][x] = c


# The cover: oxblood leather, a stitched seam and gold corner guards.
def cover():
    size, border = 48, 12
    rng = random.Random(7)
    p = canvas(size, size)
    speckle(p, rng, LEATHER, [(LEATHER_D, 0.10), (LEATHER_L, 0.07)])
    for i in range(size):
        p[0][i] = p[size - 1][i] = p[i][0] = p[i][size - 1] = LEATHER_OUT
    for i in range(1, size - 1):
        p[1][i] = LEATHER_HI
        p[i][1] = LEATHER_HI
        p[size - 2][i] = LEATHER_D
        p[i][size - 2] = LEATHER_D
    # Stitches four pixels in: two on, two off, with a shadow below/right of each.
    inset = 4
    for i in range(inset, size - inset):
        if (i // 2) % 2 == 0:
            for (x, y, sx, sy) in ((i, inset, i, inset + 1), (i, size - 1 - inset, i, size - inset),
                                   (inset, i, inset + 1, i), (size - 1 - inset, i, size - inset, i)):
                p[y][x] = STITCH
                if p[sy][sx] != STITCH:
                    p[sy][sx] = STITCH_D
    # Gold corner guard, a triangle with a rivet, mirrored into each corner.
    guard = canvas(border, border)
    reach = 10
    for y in range(border):
        for x in range(border):
            d = x + y
            if d > reach:
                continue
            if d == reach or x == 0 or y == 0:
                guard[y][x] = GOLD_OUT
            elif x == 1 or y == 1:
                guard[y][x] = GOLD_HI
            elif d >= reach - 2:
                guard[y][x] = GOLD_D
            else:
                guard[y][x] = GOLD
    guard[3][3] = GOLD_OUT
    guard[2][3] = GOLD_D
    guard[3][2] = GOLD_D
    guard[4][4] = GOLD_HI
    for flip_x in (False, True):
        for flip_y in (False, True):
            for y in range(border):
                for x in range(border):
                    c = guard[y][x]
                    if c is None:
                        continue
                    px = size - 1 - x if flip_x else x
                    py = size - 1 - y if flip_y else y
                    p[py][px] = c
    return nine_slice("book/cover", p, border, border, border, border)


# A page: parchment, the stacked edges of the pages below it and a shadow towards the spine.
def page_left():
    w, h = 64, 64
    left, top, right, bottom = 6, 4, 12, 7
    rng = random.Random(11)
    p = canvas(w, h)
    speckle(p, rng, PARCH, [(PARCH_1, 0.07), (PARCH_HI, 0.04), (PARCH_2, 0.01)])
    # Shadow towards the spine, dithered between bands.
    for y in range(h):
        for x in range(w - right, w):
            t = x - (w - right)
            if t >= 10:
                c = PARCH_3
            elif t >= 8:
                c = PARCH_3 if (x + y) % 2 == 0 else PARCH_2
            elif t >= 6:
                c = PARCH_2
            elif t >= 4:
                c = PARCH_2 if (x + y) % 2 == 0 else PARCH_1
            elif t >= 2:
                c = PARCH_1
            else:
                c = PARCH_1 if (x + y) % 2 == 0 else p[y][x]
            p[y][x] = c
    # Top edge.
    for x in range(w):
        p[0][x] = PAGE_OUT
        p[1][x] = PARCH_HI if p[1][x] in (PARCH, PARCH_HI, PARCH_1) else p[1][x]
    # Stacked page edges on the outer side and the bottom.
    for y in range(h):
        p[y][0] = PAGE_OUT
        for x in range(1, left - 1):
            p[y][x] = EDGE_D if x % 2 == 1 else EDGE_L
        p[y][left - 1] = PARCH_2
    for x in range(w):
        p[h - 1][x] = PAGE_OUT
        for y in range(h - bottom + 1, h - 1):
            p[y][x] = EDGE_D if (h - 1 - y) % 2 == 1 else EDGE_L
        if x >= left - 1:
            p[h - bottom][x] = PARCH_2
    # Where the two stacks meet, the edges turn the corner.
    for i in range(1, left - 1):
        for j in range(1, bottom - 1):
            p[h - 1 - j][i] = EDGE_D if min(i, j) % 2 == 1 else EDGE_L
    p[0][0] = CLEAR
    p[h - 1][0] = CLEAR
    return nine_slice("book/page_left", p, left, top, right, bottom)


def page_right(left_pixels):
    return nine_slice("book/page_right", mirror_x(left_pixels), 12, 4, 6, 7)


# A cloth bookmark ribbon sticking up above the book, grey so it can be tinted any colour.
def ribbon():
    w, h = 24, 20
    border = 4
    p = canvas(w, h)
    for y in range(h):
        for x in range(w):
            c = (0xD2,) * 3 if (x + y) % 2 == 0 else (0xC8,) * 3
            if x == 1:
                c = (0xEE,) * 3
            elif x >= w - 3:
                c = (0xA6,) * 3
            if y == 1 and 1 <= x < w - 1:
                c = (0xF2,) * 3
            p[y][x] = c
    for y in range(h):
        p[y][0] = p[y][w - 1] = (0x30,) * 3
    for x in range(w):
        p[0][x] = (0x30,) * 3
    # Rounded top corners.
    p[0][0] = p[0][w - 1] = CLEAR
    # Stitches along the top.
    for x in range(3, w - 3):
        if (x // 2) % 2 == 0:
            p[3][x] = (0xF8,) * 3
    return nine_slice("book/ribbon", p, border, border, border, 2)


# An ink flourish to divide sections: a line with curled ends; the gem marks its middle.
def divider():
    w, h = 32, 5
    p = canvas(w, h)
    curl = [
        "..XX....",
        ".X..X...",
        ".X.XXXXX",
        "..X.....",
        "........",
    ]
    for y, row in enumerate(curl):
        for x, ch in enumerate(row):
            if ch == "X":
                p[y][x] = INK_FADED
    for x in range(8, w - 8):
        p[2][x] = INK_FADED
    right = mirror_x([row[:8] for row in p])
    for y in range(h):
        for x in range(8):
            p[y][w - 8 + x] = right[y][x]
    nine_slice("book/divider", p, 8, 0, 8, 0)
    gem = canvas(9, 5)
    shape = [
        "....X....",
        "...XRX...",
        "..XRRRX..",
        "...XRX...",
        "....X....",
    ]
    for y, row in enumerate(shape):
        for x, ch in enumerate(row):
            if ch == "X":
                gem[y][x] = INK
            elif ch == "R":
                gem[y][x] = (0x9C, 0x2A, 0x22)
    plain("book/divider_gem", gem)
    return p


# A sunken groove for progress bars and the grey fill that is tinted per bar.
def bars():
    frame = canvas(12, 7)
    for y in range(7):
        for x in range(12):
            frame[y][x] = PARCH_2
    for x in range(12):
        frame[0][x] = INK_FADED
        frame[6][x] = INK_FADED
        frame[1][x] = PARCH_3
    for y in range(7):
        frame[y][0] = INK_FADED
        frame[y][11] = INK_FADED
    frame[0][0] = frame[0][11] = frame[6][0] = frame[6][11] = CLEAR
    nine_slice("book/bar", frame, 2, 2, 2, 2)
    fill = canvas(4, 5)
    for x in range(4):
        fill[0][x] = (0xFF,) * 3
        fill[1][x] = (0xE0,) * 3
        fill[2][x] = (0xD0,) * 3
        fill[3][x] = (0xC0,) * 3
        fill[4][x] = (0x98,) * 3
    nine_slice("book/bar_fill", fill, 1, 1, 1, 1)
    return frame


# Tooltips: dark leather with a thin gold edge. Minecraft draws <style>_background and
# <style>_frame 9 pixels around the text.
def tooltip():
    size, border = 32, 9
    bg = canvas(size, size)
    rng = random.Random(3)
    dark = (0x24, 0x13, 0x0E)
    for y in range(3, size - 3):
        for x in range(3, size - 3):
            bg[y][x] = dark if rng.random() > 0.08 else (0x2C, 0x18, 0x11)
    for i in range(3, size - 3):
        bg[2][i] = bg[size - 3][i] = bg[i][2] = bg[i][size - 3] = LEATHER_OUT
    nine_slice("tooltip/leather_background", bg, border, border, border, border)
    frame = canvas(size, size)
    for i in range(4, size - 4):
        frame[3][i] = GOLD
        frame[size - 4][i] = GOLD_D
        frame[i][3] = GOLD
        frame[i][size - 4] = GOLD_D
    for (x, y) in ((3, 3), (size - 4, 3), (3, size - 4), (size - 4, size - 4)):
        frame[y][x] = GOLD_HI
    nine_slice("tooltip/leather_frame", frame, border, border, border, border)
    return bg



# The quest board: dark oak planks, an iron-bound frame, torn notes, signs, nails and a wax seal.
WOOD_OUT = (0x14, 0x0C, 0x07)
FRAME_D = (0x2E, 0x1B, 0x0E)
FRAME = (0x3E, 0x26, 0x15)
FRAME_L = (0x50, 0x33, 0x1D)
FRAME_HI = (0x63, 0x41, 0x26)
PLANK = [(0x5C, 0x3B, 0x22), (0x66, 0x42, 0x27), (0x55, 0x36, 0x1F), (0x61, 0x3F, 0x24)]
PLANK_SEAM = (0x26, 0x17, 0x0C)
IRON_OUT = (0x16, 0x17, 0x1A)
IRON_D = (0x3A, 0x3D, 0x44)
IRON = (0x5E, 0x63, 0x6C)
IRON_HI = (0x9A, 0xA1, 0xAC)
NOTE_EDGE = (0xB8, 0x96, 0x60)
NOTE_AGED = (0xD6, 0xBC, 0x88)
NOTE = (0xEE, 0xDE, 0xB6)
NOTE_HI = (0xF6, 0xEA, 0xCA)
NOTE_SPOT = (0xE2, 0xCD, 0x9E)
WAX_D = (0x6A, 0x12, 0x10)
WAX = (0xA8, 0x22, 0x1C)
WAX_L = (0xC8, 0x3A, 0x2E)
WAX_HI = (0xE8, 0x70, 0x5C)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def planks():
    w, h = 64, 32
    rng = random.Random(21)
    p = canvas(w, h)
    seams = [22, 54, 6, 38]
    for row in range(4):
        base = PLANK[row]
        for y in range(row * 8, row * 8 + 8):
            for x in range(w):
                c = base
                r = rng.random()
                if r < 0.10:
                    c = shade(base, 0.86)
                elif r < 0.14:
                    c = shade(base, 1.08)
                p[y][x] = c
        # Grain: long dark streaks along the plank.
        for _ in range(5):
            gy = row * 8 + rng.randint(1, 6)
            gx = rng.randint(0, w - 1)
            for i in range(rng.randint(8, 22)):
                p[gy][(gx + i) % w] = shade(base, 0.8)
        for x in range(w):
            p[row * 8][x] = shade(base, 1.14)
            p[row * 8 + 7][x] = PLANK_SEAM
        sx = seams[row]
        for y in range(row * 8, row * 8 + 7):
            p[y][sx] = PLANK_SEAM
            p[y][(sx + 1) % w] = shade(base, 1.12)
    # A knot.
    for (x, y, c) in ((44, 11, 0.7), (45, 11, 0.6), (44, 12, 0.6), (45, 12, 0.7), (43, 11, 0.85), (46, 12, 0.85)):
        p[y][x] = shade(PLANK[1], c)
    return tiled("board/planks", p)


def frame():
    size, border = 48, 12
    rng = random.Random(5)
    p = canvas(size, size)
    t = 8  # frame thickness
    for y in range(size):
        for x in range(size):
            d = min(x, y, size - 1 - x, size - 1 - y)
            if d >= t:
                continue
            c = FRAME if rng.random() > 0.15 else FRAME_D
            if d == 0:
                c = WOOD_OUT
            elif d == 1:
                c = FRAME_HI if (x < size // 2 and y < size // 2) or d == y or d == x else FRAME_L
            elif d == t - 1:
                c = WOOD_OUT
            p[y][x] = c
    # A soft shadow cast by the frame onto the planks.
    for y in range(size):
        for x in range(size):
            d = min(x, y, size - 1 - x, size - 1 - y)
            if d == t:
                p[y][x] = (0, 0, 0, 110)
            elif d == t + 1:
                p[y][x] = (0, 0, 0, 50)
    # Iron corner brackets with rivets.
    bracket = canvas(border, border)
    for y in range(border):
        for x in range(border):
            if (x < 10 and y < 4) or (x < 4 and y < 10):
                edge = x == 0 or y == 0 or (y == 3 and x >= 3) or (x == 3 and y >= 3) or x == 9 or y == 9
                bracket[y][x] = IRON_OUT if edge else (IRON_HI if x == 1 or y == 1 else IRON)
    for (x, y) in ((2, 2), (7, 2), (2, 7)):
        bracket[y][x] = IRON_D
    for flip_x in (False, True):
        for flip_y in (False, True):
            for y in range(border):
                for x in range(border):
                    c = bracket[y][x]
                    if c is None:
                        continue
                    p[size - 1 - y if flip_y else y][size - 1 - x if flip_x else x] = c
    return nine_slice("board/frame", p, border, border, border, border)


def note():
    size, border = 32, 7
    rng = random.Random(9)
    p = canvas(size, size)
    # How far the torn edge reaches in, per position along an edge; the middle 18 repeat.
    tear = [2, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 2]
    for y in range(size):
        for x in range(size):
            inner = min(x - tear[y], y - tear[x], size - 1 - tear[y] - x, size - 1 - tear[x] - y)
            if inner < 0:
                continue
            if inner == 0:
                c = NOTE_EDGE
            elif inner == 1:
                c = NOTE_AGED
            elif inner == 2 and rng.random() < 0.5:
                c = NOTE_AGED
            else:
                r = rng.random()
                c = NOTE_SPOT if r < 0.07 else NOTE_HI if r < 0.11 else NOTE
            p[y][x] = c
    return nine_slice("board/note", p, border, border, border, border)


def sign():
    w, h = 32, 15
    rng = random.Random(13)
    p = canvas(w, h)
    for y in range(h):
        for x in range(w):
            c = (0x8A, 0x5A, 0x32) if rng.random() > 0.12 else (0x7C, 0x50, 0x2C)
            if y in (5, 10) and rng.random() < 0.7:
                c = (0x76, 0x4B, 0x29)
            if y == 1:
                c = (0xA8, 0x74, 0x40)
            if y == h - 2:
                c = (0x62, 0x3C, 0x20)
            p[y][x] = c
    for x in range(w):
        p[0][x] = p[h - 1][x] = WOOD_OUT
    for y in range(h):
        p[y][0] = p[y][w - 1] = WOOD_OUT
    p[0][0] = p[0][w - 1] = p[h - 1][0] = p[h - 1][w - 1] = None
    for x in (2, w - 3):
        p[7][x] = IRON_D
        p[6][x] = IRON_HI
    return nine_slice("board/sign", p, 4, 4, 4, 4)


def nail():
    shape = [
        ".KKK.",
        "KHSIK",
        "KSIdK",
        "KIddK",
        ".KKK.",
    ]
    colors = {"K": IRON_OUT, "H": IRON_HI, "S": (0x7E, 0x84, 0x8E), "I": IRON, "d": IRON_D}
    return plain("board/nail", [[colors.get(ch) for ch in row] for row in shape])


def seal():
    size = 15
    p = canvas(size, size)
    c = 7
    # A slightly lumpy blob of wax.
    bumps = {(0, 7): 1, (14, 6): 1, (7, 14): 1, (3, 13): 1}
    for y in range(size):
        for x in range(size):
            d = ((x - c) ** 2 + (y - c) ** 2) ** 0.5
            if d <= 6.6 or (x, y) in bumps:
                p[y][x] = WAX
                if d > 5.6 or (x, y) in bumps:
                    p[y][x] = WAX_D if x + y > 14 else WAX_L
    # The pressed ring and a star in the middle.
    for y in range(size):
        for x in range(size):
            d = ((x - c) ** 2 + (y - c) ** 2) ** 0.5
            if 3.6 <= d <= 4.4:
                p[y][x] = WAX_D if x + y < 14 else WAX_L
    for (x, y) in ((7, 5), (7, 6), (7, 7), (7, 8), (7, 9), (5, 7), (6, 7), (8, 7), (9, 7)):
        p[y][x] = WAX_D
    p[3][5] = WAX_HI
    p[4][4] = WAX_HI
    return plain("board/seal", p)


# Wynncraft's five elements as 7x7 symbols, white so each is tinted in its skill's colour: earth's
# four leaves, thunder's star, a water drop, a flame and gusts of air. Light from the top left.
ELEMENTS = {
    "earth": [
        "..XXX..",
        "..XsX..",
        "XX.X.XX",
        "XsXXXsX",
        "XX.X.XX",
        "..XsX..",
        "..XXX..",
    ],
    "thunder": [
        "...X...",
        "...X...",
        "..XXX..",
        "XXXXXXs",
        "..XXs..",
        "...s...",
        "...s...",
    ],
    "water": [
        "...X...",
        "..XXX..",
        ".XXXXX.",
        ".XXXXX.",
        "XXXXXXs",
        "XXXXXss",
        ".sssss.",
    ],
    "fire": [
        "...X...",
        "..XX...",
        "..XXX.X",
        ".XXXXX.",
        "XXXXXXs",
        "XXXXXss",
        ".sssss.",
    ],
    "air": [
        "..XX...",
        "....X..",
        "XXXX...",
        ".......",
        "XXXXXX.",
        "......X",
        "....XX.",
    ],
}


def elements():
    last = None
    for name, rows in ELEMENTS.items():
        last = plain("element/" + name, [[(0xFF,) * 3 if ch == "X" else (0xB8,) * 3 if ch == "s" else None for ch in row]
                                          for row in rows])
    return last


def main():
    sprites = {
        "cover": cover(),
        "page_left": None,
        "ribbon": ribbon(),
        "divider": divider(),
        "bar": bars(),
        "tooltip": tooltip(),
        "planks": planks(),
        "frame": frame(),
        "note": note(),
        "sign": sign(),
        "nail": nail(),
        "seal": seal(),
        "elements": elements(),
    }
    left = page_left()
    sprites["page_left"] = left
    sprites["page_right"] = page_right(left)
    if "--preview" in sys.argv:
        for name, pixels in sprites.items():
            png(os.path.join(PREVIEW, name + ".png"), pixels, scale=8)


if __name__ == "__main__":
    main()
