#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 5thlayer
# SPDX-License-Identifier: LGPL-3.0-only
"""Builds the Votive's side and top and the Lampad's sheets from Malcolm Riley's art in data/art/ (NOTICE).

The Votive's side is an aurichalcum plate carved with runes and its top a plain framed plate. Aurichalcum
beside a gold block reads as a darker metal, so each is recoloured to vanilla gold_block's palette by
luminance rank: the plate's distinct luminances, sorted, are spread across the palette's nine colours,
darkest to darkest. The palette is held here as colours, and no texture of Mojang's is copied in (libworks
ADR 0012).

The Lampad's sheet is drawn on data/lampad_template.png, her generated sheet. Its robe and its hair take
a woven fabric each, tiled across the sheet and recoloured to a target: the fabric keeps its weave, its
shades spread about the target as about its own mean. The robe is an embroidered purple turned to her
indigo, and the hair, her hat with it, a black darker than the template's, to stand off the robe. Her
face, skin and gold trim are the template's, and so is the Nightbrand, whose columns are left alone.

Her statue, which she shows while dormant, is her sheet turned grey: its luminances stretched across a
stone grey with a touch of blue.

Run from the repository root; it needs Pillow. With --check it writes nothing and fails when a committed
texture's pixels differ from what it builds.
"""

import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ART = ROOT / "data" / "art"
TEMPLATE = ROOT / "data" / "lampad_template.png"
TEXTURES = ROOT / "src" / "main" / "resources" / "assets" / "atouchofdarkness" / "textures"

# Vanilla gold_block's colours, darkest first.
GOLD = [
    (0xCC, 0x8E, 0x27), (0xD3, 0x96, 0x32), (0xF9, 0xBD, 0x23), (0xF5, 0xCC, 0x27), (0xFF, 0xD8, 0x3E),
    (0xFE, 0xE0, 0x48), (0xFF, 0xEC, 0x4F), (0xFF, 0xFD, 0x90), (0xFE, 0xFF, 0xBD),
]

ROBE = "fabric_purple_ends.png"
HAIR = "fabric_black_sides.png"
INDIGO = (59, 47, 99)
HAIR_COLOUR = (20, 15, 28)
# How far a recoloured fabric's shades spread from its target, against the fabric's own spread.
WEAVE = 0.9
# The Nightbrand and its flame lie at and right of this column, and keep the template's pixels.
NIGHTBRAND = 48

# The statue's greys, darkest and lightest, and the blue that tints them.
STONE_DARK = 74
STONE_LIGHT = 160
STONE_BLUE = 4


def luminance(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def pixels(image):
    return [image.getpixel((x, y)) for y in range(image.height) for x in range(image.width)]


def art(name):
    return Image.open(ART / name).convert("RGBA")


def gilded(plate):
    ranks = sorted({round(luminance(c)) for c in pixels(plate) if c[3]})
    out = plate.copy()
    for y in range(out.height):
        for x in range(out.width):
            c = out.getpixel((x, y))
            if c[3]:
                rank = ranks.index(round(luminance(c)))
                out.putpixel((x, y), GOLD[round(rank / max(1, len(ranks) - 1) * (len(GOLD) - 1))] + (255,))
    return out


def dyed(fabric, target):
    mean = sum(luminance(c) for c in pixels(fabric)) / (fabric.width * fabric.height)
    out = fabric.copy()
    for y in range(out.height):
        for x in range(out.width):
            k = 1 + (luminance(out.getpixel((x, y))) - mean) / mean * WEAVE
            out.putpixel((x, y), tuple(max(0, min(255, round(t * k))) for t in target) + (255,))
    return out


def is_robe(c):
    r, _, b, a = c
    return a and 50 <= r <= 70 and 90 <= b <= 110 and b - r > 30


def is_hair(c):
    r, _, b, a = c
    return a and 35 <= r <= 50 and 50 <= b <= 64 and b - r < 20


def lampad():
    template = Image.open(TEMPLATE).convert("RGBA")
    robe, hair = dyed(art(ROBE), INDIGO), dyed(art(HAIR), HAIR_COLOUR)
    out = template.copy()
    for y in range(template.height):
        for x in range(NIGHTBRAND):
            c = template.getpixel((x, y))
            if is_robe(c):
                out.putpixel((x, y), robe.getpixel((x % robe.width, y % robe.height)))
            elif is_hair(c):
                out.putpixel((x, y), hair.getpixel((x % hair.width, y % hair.height)))
    return out


def statue(sheet):
    shades = [luminance(c) for c in pixels(sheet) if c[3]]
    dark, light = min(shades), max(shades)
    out = sheet.copy()
    for y in range(out.height):
        for x in range(out.width):
            c = out.getpixel((x, y))
            if c[3]:
                g = round(STONE_DARK + (luminance(c) - dark) / (light - dark) * (STONE_LIGHT - STONE_DARK))
                out.putpixel((x, y), (g, g, g + STONE_BLUE, 255))
    return out


def textures():
    sheet = lampad()
    return {
        TEXTURES / "block" / "votive_side.png": gilded(art("block_metal_aurichalcum_rune_3.png")),
        TEXTURES / "block" / "votive_top.png": gilded(art("block_metal_aurichalcum_block_1.png")),
        TEXTURES / "entity" / "lampad" / "lampad.png": sheet,
        TEXTURES / "entity" / "lampad" / "lampad_statue.png": statue(sheet),
    }


def main():
    check = "--check" in sys.argv
    stale = 0
    for path, image in textures().items():
        name = path.relative_to(ROOT)
        if check:
            # Pixels, not the file's bytes, which another Pillow may encode differently.
            current = path.is_file() and Image.open(path).convert("RGBA").tobytes() == image.tobytes()
            print(("ok   %s is current" if current else "FAIL %s is stale -- re-run scripts/build-textures.py") % name)
            stale += not current
        else:
            image.save(path)
            print(f"wrote {name}")
    return 1 if stale else 0


if __name__ == "__main__":
    raise SystemExit(main())
