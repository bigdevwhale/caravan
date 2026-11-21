"""Cuts the Codex sheets in art/sheets into app drawables (the Roamling cutter).

    python tools/cut.py [sheet names]
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image
from scipy import ndimage

ROOT = Path(__file__).resolve().parent.parent
SHEETS = ROOT / "art" / "sheets"
OUT = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"

# Same order as the prompts in gen_art.py and the ids in domain/Catalog.kt.
GOODS_A = ["honey", "herbs", "resin", "amber", "fish", "pearl", "glass", "coins", "candles", "scroll", "ink", "quills"]
GOODS_B = ["cogs", "iron", "copper", "spice", "silk", "rose", "tea", "maps", "salt", "bread", "yarn", "jug"]
RARE = ["pepper", "starsalt", "dust", "clock", "moonlight", "musicbox"]
ICONS = ["purse", "bundle", "seal", "scroll", "ledger", "map", "lantern", "tent", "hourglass", "scales", "key", "sign"]
CRESTS = ["garden", "water", "old", "scholar", "forge", "bazaar", "gate", "homes", "night"]
BUILDINGS = ["stall", "well", "inn", "tower", "warehouse", "fountain"]
RIDES = ["car", "bus", "tram", "metro", "caravan", "chest"]

sys.path.insert(0, str(ROOT / "tools"))
from curios import CURIOS  # noqa: E402
CIDS = [c for c, _ in CURIOS]

SPRITES = [  # sheet, names, prefix, cols, rows, max px
    ("goods_a", GOODS_A, "good", 4, 3, 220), ("goods_b", GOODS_B, "good", 4, 3, 220), ("rare", RARE, "rare", 3, 2, 220),
    ("icons", ICONS, "ic", 4, 3, 140), ("crests", CRESTS, "crest", 3, 3, 260), ("buildings", BUILDINGS, "bld", 3, 2, 300),
    ("rides", RIDES, "ride", 3, 2, 360), ("tabs", ["gate", "map", "ledger", "guild"], "tab", 4, 1, 160),
    ("house_parts", ["hall", "tower", "pavilion", "storeroom", "shed", "garden", "fountain", "gate", "treasury", "banner", "site", "chest"], "hp", 4, 3, 520),
    ("tab_house", ["house", "build"], "tab", 2, 1, 160),
    ("merchants", ["garden", "water", "old", "scholar", "forge", "bazaar", "gate", "homes"], "mer", 4, 2, 420),
    ("curios_a", CIDS[0:12], "cur", 4, 3, 220), ("curios_b", CIDS[12:24], "cur", 4, 3, 220), ("curios_c", CIDS[24:36], "cur", 4, 3, 220),
    ("curios_d", CIDS[36:48], "cur", 4, 3, 220), ("curios_e", CIDS[48:60], "cur", 4, 3, 220),
]
SCENES = {"house_plot": 1000, "gate": 900, "gate_night": 900, "prologue_city": 900, "prologue_trade": 900,
          "banner_market": 960, "banner_ledger": 960, "banner_guild": 960, "banner_map": 960}


def key(im):
    arr = np.array(im.convert("RGBA")).astype(np.int32)
    r, g, b = arr[..., 0], arr[..., 1], arr[..., 2]
    m = np.minimum(r, b) - g
    hard = (m > 90) & (r > 170) & (b > 170)
    soft = (m > 40) & (r > 150) & (b > 150) & ~hard
    k = np.clip((m - 40) / 50, 0, 1)
    out = arr.copy()
    out[..., 0] = np.where(soft, r - (r - g) * k, r)
    out[..., 2] = np.where(soft, b - (b - g) * k, b)
    out[..., 3] = np.where(hard, 0, np.where(soft, (255 * (1 - k)).astype(np.int32), 255))
    return Image.fromarray(out.clip(0, 255).astype(np.uint8))


def biggest_only(cell):
    """Keeps the main object of a cell and anything near it; drops crumbs of neighbours poking in at the edges."""
    arr = np.array(cell)
    lab, n = ndimage.label(arr[:, :, 3] > 30)
    if n == 0:
        return cell
    sizes = ndimage.sum(np.ones_like(lab), lab, range(n + 1))
    big = int(np.argmax(sizes[1:]) + 1)
    edge = set(np.unique(np.concatenate([lab[0], lab[-1], lab[:, 0], lab[:, -1]]))) - {0, big}
    for k in edge:
        arr[lab == k, 3] = 0
    for k in range(1, n + 1):
        if sizes[k] < 40:
            arr[lab == k, 3] = 0
    return Image.fromarray(arr)


def frames(path, cols, rows):
    im = key(Image.open(path))
    w, h = im.size
    cw, ch = w / cols, h / rows
    for j in range(rows):
        for i in range(cols):
            cell = biggest_only(im.crop((int(i * cw), int(j * ch), int((i + 1) * cw), int((j + 1) * ch))))
            box = cell.getchannel("A").point(lambda v: 255 if v > 30 else 0).getbbox()
            yield cell.crop(box) if box else cell


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    want = set(sys.argv[1:])
    for sheet, names, prefix, cols, rows, size in SPRITES:
        f = SHEETS / f"{sheet}.png"
        if not f.exists() or (want and sheet not in want):
            continue
        for name, fr in zip(names, frames(f, cols, rows)):
            fr.thumbnail((size, size), Image.LANCZOS)
            fr.save(OUT / f"{prefix}_{name}.webp", "WEBP", quality=90, method=6)
        print(sheet)
    for name, width in SCENES.items():
        f = SHEETS / f"{name}.png"
        if not f.exists() or (want and name not in want):
            continue
        g = Image.open(f).convert("RGB")
        g = g.resize((width, round(width * g.height / g.width)), Image.LANCZOS)
        g.save(OUT / f"{name}.webp", "WEBP", quality=86, method=6)
        print(name)


if __name__ == "__main__":
    main()
