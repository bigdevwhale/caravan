"""Generates Caravan's art with the Codex CLI image tool (the Roamling/Wildling pipeline).

Codex saves images into ~/.codex/generated_images/<session>/; the image of the session named in
the run's output is copied to art/sheets/<id>.png. Already present sheets are skipped.

    python tools/gen_art.py              # everything missing
    python tools/gen_art.py goods_a gate # only these
"""
import re
import shutil
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "art" / "sheets"
GEN = Path.home() / ".codex" / "generated_images"

HEAD = "Use your image generation tool to create ONE image. Do nothing else (no file copying).\n\n"

STYLE = (
    "Style: a Persian and Ottoman miniature painting turned into a cozy fantasy storybook: flat rich colours "
    "(terracotta, deep indigo, saffron yellow, turquoise, cream, a little rose), fine dark ink outlines, delicate gold details, "
    "tiny decorative patterns on fabrics and tiles, flat perspective, no photo realism, no 3D render, no pixel art."
)
ICON_STYLE = STYLE + " Plain objects drawn clearly and readably like inventory icons of a cozy game: NO faces, NO eyes, NO people, NO animals."
GRID = ("on a flat solid pure magenta (#FF00FF) background. One object per cell, centered, filling most of the cell, "
        "with clear margin; nothing touches the cell borders; no text, no letters, no numbers, no grid lines, no shadows on the background.")
NOBODY = "IMPORTANT: NO people, NO animals, NO creatures, NO text, NO letters anywhere."


def sheet(what, cols, rows, items):
    lines = "\n".join(f"{i + 1}) {t}" for i, t in enumerate(items))
    return HEAD + f"Image: a sprite sheet of {len(items)} {what}.\n{ICON_STYLE}\nLayout: {cols} columns x {rows} rows of equal square cells {GRID}\nItems, left to right, top to bottom:\n{lines}"


# Goods: 8 quarter kinds x 3. Order matters (tools/cut.py, domain/Catalog.kt).
GOODS_A = [
    "a clay jar of glowing pale moon honey with a wooden dipper", "a bundle of dried healing herbs tied with string", "a lump of golden tree resin in a leaf",
    "a handful of polished honey-coloured river amber", "a silver fish wrapped in misty blue paper", "an open shell with a soft grey pearl",
    "a pane of coloured stained glass in a lead frame (abstract flower pattern)", "a small stack of old bronze coins with square holes", "three thick beeswax candles bound together",
    "a rolled parchment scroll with a red ribbon and seal", "a crystal ink bottle with dark blue ink full of tiny stars", "a bundle of white quill feathers",
]
GOODS_B = [
    "a small pile of brass cogwheels", "an iron ingot with a stamped mark (no letters)", "a coil of shiny copper wire",
    "an open sack of red and orange spices", "a folded bolt of turquoise silk with gold pattern", "a small glass flask of rose oil",
    "a tin box of tea leaves with a patterned lid", "a rolled map of faraway lands tied with cord", "a small sack of white salt crystals",
    "a round loaf of bread with a braided top", "a skein of soft dyed wool yarn", "a glazed clay jug with blue patterns",
]
RARE = [
    "a pod of fiery red dragon pepper glowing at the tip", "a small jar of glittering star salt", "a pouch of shimmering fairy dust",
    "a small clock-shaped golden compass", "a teardrop of frozen moonlight in a tiny cage", "a patterned egg-shaped music box (an object, no animal)",
]
ICONS = [
    "a leather coin purse with gold coins spilling", "a tied cloth trade bundle", "a round red wax seal with a star",
    "a rolled scroll with a ribbon (orders)", "an open ledger book with columns (no letters)", "a folded city map",
    "a glowing brass lantern (night market)", "a small striped fair tent with a pennant", "a brass hourglass",
    "a weighing scale with two pans", "a big old key", "a signpost with blank arrows",
]
# Quarter crests: same order as Kind in domain/Catalog.kt.
CRESTS = [
    "a green shield with a golden tree", "a blue shield with silver waves and a bridge arch", "a terracotta shield with a domed tower and a bell",
    "an indigo shield with an open book and a star", "a dark red shield with a hammer and an anvil", "a saffron shield with crossed keys over a market awning",
    "a turquoise shield with a gate arch and a winged wheel", "a cream shield with a little house and a sheaf of wheat", "a violet shield with a crescent moon and a lantern",
]
BUILDINGS = [
    "a small market stall with a striped awning and a counter", "a stone well with a little tiled roof", "a two-storey caravanserai inn with arches and a courtyard gate",
    "a slender watchtower with a lantern on top", "a domed warehouse with big wooden doors", "a tiled fountain with a basin and a jet of water",
]
# Carriages: the player's own transport, enchanted and self-moving (no animals pulling).
RIDES = [
    "a small self-moving enchanted covered cart with a striped canopy and lanterns, no animal pulling it (the player's car)",
    "a long enchanted city omnibus carriage with many arched windows and a luggage rack, no animal pulling it (a bus)",
    "a pair of linked tram wagons with a curved roof on rails, brass trim (a tram)",
    "a dwarven mine cart on rails coming out of a stone tunnel arch with a lamp (the underground)",
    "a large decorated caravan wagon with carpets on the roof and bundles, no animal pulling it",
    "a merchant's travelling chest on little wheels with brass corners",
]

BANNER = HEAD + ("Image: a wide banner illustration for a fantasy city-trading app. Landscape about 3:1, full-bleed edge to edge "
                 "(no border, no frame, no text, no letters, no numbers, no logo).\n" + STYLE +
                 "\nKeep the left third calmer and simpler (a title will be written over it).\n" + NOBODY + "\nScene: ")
SCENE = HEAD + ("Image: an illustration for a fantasy city-trading app. PORTRAIT 2:3, full-bleed (no border, no text, no letters, no logo).\n"
                + STYLE + "\n" + NOBODY + "\nScene: ")

from curios import CURIOS  # noqa: E402

MERCHANT_STYLE = (STYLE + " Each portrait is a friendly character from a cozy fantasy bazaar, waist-up, three-quarter view, warm kind face, "
                  "rich patterned clothes, holding the object described. One person per cell, nothing else.")
MERCHANTS = [
    "an elderly woman gardener in a green robe and headscarf holding a basket of roses and herbs",
    "a cheerful boatman with a short beard and a blue turban holding a painted oar",
    "an old keeper of keys with a long white beard and a terracotta robe holding a ring of brass keys",
    "a young astronomer with round spectacles and an indigo star-patterned robe holding a brass astrolabe",
    "a strong blacksmith with a leather apron and rolled sleeves holding a hammer, sparks behind",
    "a confident merchant woman in saffron silk with gold jewellery holding a bolt of turquoise silk and small scales",
    "a weathered caravan leader with a sun-browned face, a teal coat and a walking staff, a rolled map under his arm",
    "a smiling baker woman with flour on her apron holding a round braided loaf",
]

JOBS = {
    "goods_a": sheet("trade goods", 4, 3, GOODS_A),
    "goods_b": sheet("trade goods", 4, 3, GOODS_B),
    "rare": sheet("rare magical trade goods", 3, 2, RARE),
    "icons": sheet("small interface icons", 4, 3, ICONS),
    "crests": sheet("heraldic crests of city quarters, each a single shield", 3, 3, CRESTS),
    "buildings": sheet("small buildings seen from the front, each standing alone on a little patch of ground", 3, 2, BUILDINGS),
    "rides": sheet("vehicles seen from the side, each facing right", 3, 2, RIDES),
    "tabs": sheet("navigation icons for the bottom menu of a fantasy city-trading app, bold simple silhouettes that read well at small size", 4, 1, [
        "a tiled bazaar gate arch with an open wooden door and a small striped awning",
        "a rolled-out illuminated city map with coloured quarters and a compass rose",
        "a thick merchant's ledger book with a brass clasp and a quill",
        "a merchants' guild hall with a dome, a banner and a crest over the door",
    ]),
    "house_plot": SCENE + ("a merchant's empty building plot in a fantasy bazaar town, seen from the front, flat miniature perspective. The top 30% is a calm "
                           "turquoise-and-cream sky with one cloud and far-off domes and towers of the town. Below it a long low ochre clay wall with a tiled top runs "
                           "across the whole picture. The rest, the lower 65%, is a wide EMPTY flat courtyard of pale sandy ground with a few tufts of grass and "
                           "small stones, and a straight paved path from the bottom edge to the middle. IMPORTANT: no buildings, no tents, no trees, no well, "
                           "no gate inside the courtyard – it is empty land, buildings will be placed on it later."),
    "house_parts": sheet("buildings of a merchant's house compound, each a separate piece seen straight from the front, standing on its own small patch of sandy ground, all drawn at the same scale", 4, 3, [
        "the merchant's main hall: a two-storey palace with tiled arches, carved wooden balconies and a big door (the largest piece)",
        "a tall slender tower with a pigeon loft and a pointed roof",
        "a small round domed pavilion with turquoise tiles",
        "a storeroom: a long low building with wooden doors and sacks and barrels by it",
        "a cart shed: an open wooden shed with a striped awning and a cart wheel leaning on it",
        "a small walled garden with pomegranate trees, rose bushes and a low gate",
        "a round tiled fountain with a jet of water",
        "a grand front gate arch in a short piece of wall, with open carved doors and a crest above (no letters)",
        "a small stone treasury vault with an iron-bound door and a golden lock",
        "a tall flagpole with a long saffron-and-indigo banner",
        "a stack of building materials: bricks, planks and tiles (a construction site)",
        "a pile of gold coins in an open chest",
    ]),
    "tab_house": sheet("navigation icons for the bottom menu of a fantasy city-trading app, bold simple silhouettes that read well at small size", 2, 1, [
        "a merchant's house: a small palace with a dome and a flag",
        "a hammer and a trowel crossed over a brick",
    ]),
    "merchants": HEAD + "Image: a sprite sheet of 8 character portraits.\n" + MERCHANT_STYLE + "\nLayout: 4 columns x 2 rows of equal cells " + GRID
                 + "\nPortraits, left to right, top to bottom:\n" + "\n".join(f"{i + 1}) {t}" for i, t in enumerate(MERCHANTS)),
    "curios_a": sheet("curious little treasures", 4, 3, [t for _, t in CURIOS[0:12]]),
    "curios_b": sheet("curious little treasures", 4, 3, [t for _, t in CURIOS[12:24]]),
    "curios_c": sheet("curious little treasures", 4, 3, [t for _, t in CURIOS[24:36]]),
    "curios_d": sheet("curious little treasures", 4, 3, [t for _, t in CURIOS[36:48]]),
    "curios_e": sheet("curious little treasures", 4, 3, [t for _, t in CURIOS[48:60]]),
    "gate": SCENE + ("the gate of a city quarter in a fantasy bazaar town, seen from the front at golden afternoon. A big ornamented tiled archway in the middle "
                     "with an open wooden gate, a striped awning of a trade stall on each side with jars and bolts of cloth, domed roofs and slender towers "
                     "behind, potted plants, a calm turquoise-and-cream sky with one cloud in the top quarter. The lower third is an empty flat paved square in front "
                     "of the arch (a cart will be placed there later)."),
    "gate_night": SCENE + ("the same kind of fantasy bazaar quarter gate at night: an ornamented tiled archway with an open gate, hanging glowing lanterns, "
                           "striped stalls with warm lamplight, domed roofs and towers as dark silhouettes, a deep indigo sky with a crescent moon and stars "
                           "in the top quarter. The lower third is an empty flat paved square in front of the arch."),
    "prologue_city": SCENE + ("a view from a hill over a big fantasy city made of many different quarters at sunrise: gardens, a river with bridges, an old town "
                              "with domes, a university with towers, forges with chimneys, a bazaar with striped awnings, a station with rails; winding streets "
                              "connect them and a soft golden glowing line runs along the streets like a trade route."),
    "prologue_trade": SCENE + ("a merchant's table in a bazaar seen from the front: a brass weighing scale, an open ledger with columns, a coin purse, jars of honey "
                               "and amber, a bolt of turquoise silk, a rolled city map, a wax seal stamp; a striped awning above and warm light."),
    "banner_market": BANNER + "a long bazaar street with striped awnings, stalls with spice sacks, jars, silks and lamps, a tiled arch far away.",
    "banner_ledger": BANNER + "a merchant's desk: an open ledger with columns and no letters, a quill in an inkwell, coins, a seal stamp, a city map and a lantern.",
    "banner_guild": BANNER + "the hall of a merchants' guild: tiled arches, hanging banners with crests (no letters), a long carpet, brass lamps, a big table with scales.",
    "banner_map": BANNER + "an old illuminated map of a fantasy city on a table: quarters painted in different colours, a river, bridges, tiny domes, a compass rose.",
}


def image_of(output):
    m = re.search(r"session id:\s*([0-9a-f-]+)", output)
    if not m:
        return None
    pngs = sorted((GEN / m.group(1)).glob("*.png"), key=lambda p: p.stat().st_mtime)
    return pngs[-1] if pngs else None


def run(job):
    name, prompt = job
    dst = OUT / f"{name}.png"
    if dst.exists():
        print("skip", name, flush=True)
        return
    r = None
    for _ in range(2):
        r = subprocess.run([shutil.which("codex.cmd") or shutil.which("codex") or "codex", "exec", "--skip-git-repo-check", "--sandbox", "read-only", "-"],
                           input=prompt, text=True, encoding="utf-8", errors="replace", capture_output=True, cwd=str(ROOT))
        p = image_of((r.stdout or "") + (r.stderr or ""))
        if p is not None:
            shutil.copy(p, dst)
            print("ok", name, flush=True)
            return
    print("FAIL", name, (r.stdout or "")[-300:], (r.stderr or "")[-300:], flush=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    want = sys.argv[1:]
    jobs = [j for j in JOBS.items() if not want or j[0] in want]
    with ThreadPoolExecutor(4) as ex:
        list(ex.map(run, jobs))


if __name__ == "__main__":
    main()
