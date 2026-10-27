# SPDX-FileCopyrightText: 2026 5thlayer
# SPDX-License-Identifier: MIT

# Temporary cover and icon, the Lampad peering over the title among the lights, in the -works suite's style
# (Voidworks' make-cover.py), until an in-game screenshot or an artist's art replaces them. They carry
# Malcolm Riley's art through the Lampad (NOTICE). Run from the repo root with Pillow
# installed.
import math
import os
import random

from PIL import Image, ImageDraw, ImageFont

T = 'src/main/resources/assets/atouchofdarkness/textures/item/'
BG = (8, 8, 12)
ACCENT = (196, 86, 255)  # violet, the Nightbrand's
FLAME = (255, 168, 64)  # a Torch's flame
SOUL = (96, 220, 236)  # a soul Torch's flame
# each light with the glow it casts. The lit Lamp's item model is vanilla's lantern, which the cover
# can't hold (libworks ADR 0012), so the Lamps stand dark beside the Spent Torch and the Nightbrand.
LIGHTS = [('torch', FLAME), ('soul_torch', SOUL), ('lamp_off', None), ('soul_lamp_off', None),
          ('spent_torch', None), ('nightbrand', None)]


def sprite(name, px):
    return Image.open(f'{T}{name}.png').convert('RGBA').resize((px, px), Image.NEAREST)


def lampad(u):
    # the Lampad seen from the front, each part the front face of its LampadModel cube, u pixels to a
    # model unit: skirt, body, head, the right arm hanging and the left raised forward, so only its
    # top shows, holding the Torch upright with the Nightbrand's dark flame
    tex = Image.open('src/main/resources/assets/atouchofdarkness/textures/entity/lampad/lampad.png').convert('RGBA')
    im = Image.new('RGBA', (24, 46))
    # (u, v, w, h) of the face in the texture, then x, y in model units, the feet at y = 0 and the middle
    # at x = 0, drawn at half units since the head sits half a unit off the grid
    for fu, fv, w, h, x, y in [(29, 1, 7, 8, -3.5, -19), (6, 34, 8, 6, -4, -6), (4, 20, 6, 6, -3, -12), (7, 7, 7, 7, -3.5, -19),
                               (34, 18, 2, 7, -5, -12), (42, 18, 2, 3, 3, -14), (49, 17, 1, 6, 3.5, -20),
                               (54, 18, 2, 2, 3, -22)]:
        face = tex.crop((fu, fv, fu + w, fv + h)).resize((2 * w, 2 * h), Image.NEAREST)
        im.alpha_composite(face, (int(2 * x + 12), int(2 * y + 46)))
    return im.resize((12 * u, 23 * u), Image.NEAREST)


def glow(rgb, r):
    # a pool of light that falls off to nothing, as the Dark closes in around it
    im = Image.new('RGBA', (2 * r, 2 * r))
    for y in range(2 * r):
        for x in range(2 * r):
            d = math.hypot(x - r, y - r) / r
            if d < 1:
                im.putpixel((x, y), rgb + (int(150 * (1 - d) ** 2),))
    return im


def field(W, H, px, seed, clear):
    # the Dark, with a few lights scattered through it, kept clear of the title band and, above it, of
    # the Lampad between the fractions of the width in clear
    rng = random.Random(seed)
    img = Image.new('RGBA', (W, H), BG + (255,))
    d = ImageDraw.Draw(img)
    for _ in range(W * H // 900):
        v = rng.randrange(10, 22)
        x, y = rng.randrange(W), rng.randrange(H)
        d.point((x, y), fill=(v, v, v + 4))
    slots = [(x, y) for y in (0.18, 0.82) for x in (0.12, 0.31, 0.5, 0.69, 0.88)
             if y > 0.5 or not clear[0] <= x <= clear[1]]
    rng.shuffle(slots)
    for (fx, fy), (name, rgb) in zip(slots, LIGHTS * 2):
        cx, cy = int(fx * W + rng.uniform(-0.04, 0.04) * W), int(fy * H)
        if rgb:
            g = glow(rgb, int(px * 2.4))
            img.alpha_composite(g, (cx - g.width // 2, cy - g.height // 2))
        s = sprite(name, px)
        img.alpha_composite(s, (cx - px // 2, cy - px // 2))
    return img


def stand(img, cx, top, bottom, margin):
    # the Lampad behind the band from top to bottom: as large as her head and raised Torch fit above
    # it, the 10 units above her neck, so she peers over it, and no larger than the band hides the rest
    u = min((top - margin) // 10, (bottom - top) // 12)
    l = lampad(u)
    img.alpha_composite(l, (cx - l.width // 2, top + 12 * u - l.height))


def font(names, size):
    for n in names:
        if os.path.exists(n):
            return ImageFont.truetype(n, size)
    raise SystemExit('needs one of ' + ', '.join(names))


def make(W, H, out, title_size, px, lampad_x, stacked=False):
    # stacked puts "A touch of" above the word, for the square icon; lampad_x is where the Lampad
    # stands, a fraction of the width
    img = field(W, H, px, 7, (lampad_x - 0.2, lampad_x + 0.2))
    f1 = font(['/System/Library/Fonts/Supplemental/Impact.ttf', '/usr/share/fonts/TTF/impact.ttf',
               os.path.expanduser('~/.local/share/fonts/msfonts/impact.ttf'),
               '/usr/share/fonts/noto/NotoSans-Black.ttf'], title_size)
    f2 = font(['/System/Library/Fonts/Supplemental/Apple Chancery.ttf', '/usr/share/fonts/gsfonts/Z003-MediumItalic.otf',
               '/usr/share/fonts/urw-base35/Z003-MediumItalic.otf'], int(title_size * (0.62 if stacked else 0.55)))
    # the -works title turned around: the suite's "A touch of" in a script, then the Pillar's word
    w, a = 'A touch of', 'DARKNESS'
    d = ImageDraw.Draw(img)
    b1, b2 = d.textbbox((0, 0), a, font=f1), d.textbbox((0, 0), w, font=f2)
    w1, w2, h1, h2 = b1[2] - b1[0], b2[2] - b2[0], b1[3] - b1[1], b2[3] - b2[1]
    pad, gap, sh = int(title_size * 0.16), int(title_size * 0.1), max(3, title_size // 20)
    bw, ph = w2 + 2 * gap, (h2 + 2 * pad if stacked else h1)
    th = ph + gap + h1 if stacked else h1
    cy, bh = H // 2, int(th + title_size * 0.6)
    stand(img, int(lampad_x * W), cy - bh // 2, cy + bh // 2, H // 50)
    band = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(band).rectangle([0, cy - bh // 2, W, cy + bh // 2], fill=(4, 4, 6, 235))
    img = Image.alpha_composite(img, band)
    d = ImageDraw.Draw(img)
    lt = max(3, title_size // 25)
    d.rectangle([0, cy - bh // 2, W, cy - bh // 2 + lt], fill=ACCENT)
    d.rectangle([0, cy + bh // 2 - lt, W, cy + bh // 2], fill=ACCENT)
    if stacked:
        x, bt = (W - bw) // 2, cy - th // 2
        tx, tt = (W - w1) // 2, bt + ph + gap
    else:
        x, bt = (W - (bw + gap + w1)) // 2, cy - h1 // 2
        tx, tt = x + bw + gap, bt
    sx, sy = x + gap - b2[0], bt + (ph - h2) // 2 - b2[1]
    d.text((sx + sh, sy + sh), w, font=f2, fill=(0, 0, 0))
    d.text((sx, sy), w, font=f2, fill=(240, 232, 214))
    ty = tt - b1[1]
    d.text((tx + sh - b1[0], ty + sh), a, font=f1, fill=(0, 0, 0))
    d.text((tx - b1[0], ty), a, font=f1, fill=ACCENT)
    img.convert('RGB').save(out)


make(1280, 640, 'publish/atouchofdarkness-cover.png', 150, 80, 0.86)
make(512, 512, 'publish/atouchofdarkness-icon.png', 84, 56, 0.5, stacked=True)
