# SPDX-FileCopyrightText: 2026 5thlayer
# SPDX-License-Identifier: MIT

# Temporary cover and icon, the Lampad leaning on the title over her Votive among the lights, in the -works suite's style
# (Voidworks' make-cover.py), until an in-game screenshot or an artist's art replaces them. They carry
# Malcolm Riley's art through the Lampad and the Votive (NOTICE). Run from the repo root with Pillow
# installed.
import math
import os
import random

from PIL import Image, ImageDraw, ImageFont

A = 'src/main/resources/assets/atouchofdarkness/textures/'
T = A + 'item/'
BG = (8, 8, 12)
ACCENT = (196, 86, 255)  # violet, the Nightbrand's
FLAME = (255, 168, 64)  # a Torch's flame
SOUL = (96, 220, 236)  # a soul Torch's flame
DUSK = (56, 40, 88)  # not light, neither the Nightbrand nor the Lampad gives any, but a faint haze to lift them off the Dark
# each light with the glow it casts. The Lamps are left out: lit, their item model is vanilla's
# lantern, which the cover can't hold (libworks ADR 0012), and dark they read as vanilla's too.
LIGHTS = [('torch', FLAME), ('soul_torch', SOUL), ('spent_torch', None), ('nightbrand', DUSK)]


def sprite(name, px):
    return Image.open(f'{T}{name}.png').convert('RGBA').resize((px, px), Image.NEAREST)


def lampad(u):
    # the Lampad seen from the front leaning on the band, each part the front face of its LampadModel
    # cube, u pixels to a model unit: head and body above the band's edge at the skirt's top, the right
    # arm hanging so its hand drapes over it, and the left raised forward, so only its top shows,
    # holding the Torch upright with the Nightbrand's dark flame. Returns what stands behind the band,
    # cut off at its edge, and the right cuff and hand, which lie over it, each 14 by 17 units with the
    # edge 16 units down.
    tex = Image.open(A + 'entity/lampad/lampad.png').convert('RGBA')

    def layer(faces):
        # (u, v, w, h) of the face in the texture, then x, y in model units, the band's edge at y = 0
        # and the middle at x = 0, drawn at half units since the head sits half a unit off the grid
        im = Image.new('RGBA', (28, 34))
        for fu, fv, w, h, x, y in faces:
            face = tex.crop((fu, fv, fu + w, fv + h)).resize((2 * w, 2 * h), Image.NEAREST)
            im.alpha_composite(face, (int(2 * x + 14), int(2 * y + 32)))
        return im.resize((14 * u, 17 * u), Image.NEAREST)

    behind = layer([(29, 1, 7, 8, -3.5, -13), (4, 20, 6, 6, -3, -6), (7, 7, 7, 7, -3.5, -13),
                    (34, 18, 2, 7, -5, -6), (42, 18, 2, 3, 3, -8), (49, 17, 1, 6, 3.5, -14), (54, 18, 2, 2, 3, -16)])
    behind.paste((0, 0, 0, 0), (0, 16 * u, 14 * u, 17 * u))
    front = layer([(34, 22, 2, 3, -5, -2)])
    return behind, front


def votive(px):
    # the Votive seen from a little above, its gold top foreshortened over a side shaded as the Dark
    # leaves it
    side = Image.open(A + 'block/votive_side.png').convert('RGBA').resize((px, px), Image.NEAREST)
    side = Image.blend(side, Image.new('RGBA', side.size, BG + (255,)), 0.4)
    top = Image.open(A + 'block/votive_top.png').convert('RGBA').resize((px, px // 3), Image.NEAREST)
    im = Image.new('RGBA', (px, px + px // 3))
    im.alpha_composite(top, (0, 0))
    im.alpha_composite(side, (0, px // 3))
    return im


def glow(rgb, r):
    # a pool of light that falls off to nothing, as the Dark closes in around it
    im = Image.new('RGBA', (2 * r, 2 * r))
    for y in range(2 * r):
        for x in range(2 * r):
            d = math.hypot(x - r, y - r) / r
            if d < 1:
                im.putpixel((x, y), rgb + (int(150 * (1 - d) ** 2),))
    return im


def field(W, H, px, seed, row, votive_x):
    # the Dark, its lights in a row at the height row with the Votive among them, beneath the Lampad at
    # the fraction of the width votive_x: as many lights on either side of her as an even row would
    # put there, spaced evenly between her and the edge
    rng = random.Random(seed)
    img = Image.new('RGBA', (W, H), BG + (255,))
    d = ImageDraw.Draw(img)
    for _ in range(W * H // 900):
        v = rng.randrange(10, 22)
        x, y = rng.randrange(W), rng.randrange(H)
        d.point((x, y), fill=(v, v, v + 4))
    n = len(LIGHTS) + 1
    k = min(range(n), key=lambda i: abs((i + 0.5) / n - votive_x))
    xs = ([votive_x * (i + 0.5) / (k + 0.5) for i in range(k)]
          + [1 - (1 - votive_x) * (i + 0.5) / (n - k - 0.5) for i in reversed(range(n - k - 1))])
    lights = LIGHTS[:]
    rng.shuffle(lights)
    for fx, (name, rgb) in zip(xs, lights):
        cx = int(fx * W)
        if rgb:
            g = glow(rgb, int(px * 2.4))
            img.alpha_composite(g, (cx - g.width // 2, row - g.height // 2))
        s = sprite(name, px)
        img.alpha_composite(s, (cx - px // 2, row - px // 2))
    v = votive(px)
    img.alpha_composite(v, (int(votive_x * W) - px // 2, row + px // 2 - v.height))
    return img


def stand(img, cx, top, margin):
    # the Lampad leaning on the band's top edge, as large as her 16 units above it, to the Torch's
    # flame, fit, with a haze behind her; returns what lies over the band and where
    u = (top - margin) // 16
    behind, front = lampad(u)
    g = glow(DUSK, 9 * u)
    img.alpha_composite(g, (cx - g.width // 2, top - 8 * u - g.height // 2))
    at = (cx - behind.width // 2, top - 16 * u)
    img.alpha_composite(behind, at)
    return front, at


def font(names, size):
    for n in names:
        if os.path.exists(n):
            return ImageFont.truetype(n, size)
    raise SystemExit('needs one of ' + ', '.join(names))


def make(W, H, out, title_size, px, lampad_x, band_y, stacked=False):
    # stacked puts "A touch of" above the word, for the square icon; lampad_x is where the Lampad
    # leans, a fraction of the width or the letter of the word she stands over, and band_y where the
    # band's middle is, a fraction of the height
    f1 = font(['/System/Library/Fonts/Supplemental/Impact.ttf', '/usr/share/fonts/TTF/impact.ttf',
               os.path.expanduser('~/.local/share/fonts/msfonts/impact.ttf'),
               '/usr/share/fonts/noto/NotoSans-Black.ttf'], title_size)
    f2 = font(['/System/Library/Fonts/Supplemental/Apple Chancery.ttf', '/usr/share/fonts/gsfonts/Z003-MediumItalic.otf',
               '/usr/share/fonts/urw-base35/Z003-MediumItalic.otf'], int(title_size * (0.62 if stacked else 0.55)))
    # the -works title turned around: the suite's "A touch of" in a script, then the Pillar's word
    w, a = 'A touch of', 'DARKNESS'
    d = ImageDraw.Draw(Image.new('RGB', (1, 1)))
    b1, b2 = d.textbbox((0, 0), a, font=f1), d.textbbox((0, 0), w, font=f2)
    w1, w2, h1, h2 = b1[2] - b1[0], b2[2] - b2[0], b1[3] - b1[1], b2[3] - b2[1]
    pad, gap, sh = int(title_size * 0.16), int(title_size * 0.1), max(3, title_size // 20)
    bw, ph = w2 + 2 * gap, (h2 + 2 * pad if stacked else h1)
    th = ph + gap + h1 if stacked else h1
    cy, bh = int(band_y * H), int(th + title_size * 0.6)
    if stacked:
        x, bt = (W - bw) // 2, cy - th // 2
        tx, tt = (W - w1) // 2, bt + ph + gap
    else:
        x, bt = (W - (bw + gap + w1)) // 2, cy - h1 // 2
        tx, tt = x + bw + gap, bt
    if isinstance(lampad_x, str):
        i = a.index(lampad_x)
        lampad_x = (tx - b1[0] + (d.textlength(a[:i], font=f1) + d.textlength(a[:i + 1], font=f1)) / 2) / W
    img = field(W, H, px, 7, (cy + bh // 2 + H) // 2, lampad_x)
    front, at = stand(img, int(lampad_x * W), cy - bh // 2, H // 50)
    band = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(band).rectangle([0, cy - bh // 2, W, cy + bh // 2], fill=(4, 4, 6, 235))
    img = Image.alpha_composite(img, band)
    d = ImageDraw.Draw(img)
    lt = max(3, title_size // 25)
    d.rectangle([0, cy - bh // 2, W, cy - bh // 2 + lt], fill=ACCENT)
    d.rectangle([0, cy + bh // 2 - lt, W, cy + bh // 2], fill=ACCENT)
    img.alpha_composite(front, at)
    sx, sy = x + gap - b2[0], bt + (ph - h2) // 2 - b2[1]
    d.text((sx + sh, sy + sh), w, font=f2, fill=(0, 0, 0))
    d.text((sx, sy), w, font=f2, fill=(240, 232, 214))
    ty = tt - b1[1]
    d.text((tx + sh - b1[0], ty + sh), a, font=f1, fill=(0, 0, 0))
    d.text((tx - b1[0], ty), a, font=f1, fill=ACCENT)
    img.convert('RGB').save(out)


make(1280, 640, 'publish/atouchofdarkness-cover.png', 150, 80, 'E', 0.55)
make(512, 512, 'publish/atouchofdarkness-icon.png', 84, 56, 2 / 3, 0.6, stacked=True)
