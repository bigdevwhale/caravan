"""Builds app/src/demo/assets/demo.json: a merchant some weeks into the game in central Moscow, for screenshots.

Times are written relative to BASE; the app shifts them to «now» when it seeds the demo (data/Demo.kt).

    python tools/demo.py [places.json]   # places.json: an Overpass dump of place nodes (cached in tools/demo_places.json)
"""
import json
import math
import random
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CACHE = ROOT / "tools" / "demo_places.json"
OUT = ROOT / "app" / "src" / "demo" / "assets" / "demo.json"
BASE = 1_800_000_000_000  # an arbitrary fixed «now» of the demo
DAY = 86_400_000

# The quarters the demo merchant knows: id -> (kind, second). Everything else stays unexplored.
KNOWN = {
    "2330497992": ("water", "homes"),     # Пресненский – home
    "2330491203": ("old", "bazaar"),      # Арбат
    "2330503343": ("garden", "old"),      # Хамовники
    "2330493066": ("old", "water"),       # Замоскворечье
    "2330505264": ("water", "old"),       # Якиманка
    "2330498613": ("bazaar", "old"),      # Тверской
    "2330496472": ("bazaar", "scholar"),  # Мещанский
    "2330492561": ("gate", "old"),        # Басманный
    "2331500885": ("gate", "bazaar"),     # Красносельский
    "2330643830": ("old", "bazaar"),      # Китай-город
    "2330514392": ("garden", "homes"),    # Беговой
    "3783340452": ("gate", "water"),      # Дорогомилово
    "2331513033": ("garden", "water"),    # Лужники
    "2330679958": ("scholar", "garden"),  # Воробьёвы Горы
    "2331575629": ("homes", "old"),       # Плющиха
    "2330537356": ("forge", "gate"),      # Савёловский
    "2330614170": ("forge", "water"),     # Шелепиха
}
HOME = "2330497992"
HERE = "2330491203"  # standing at the Arbat gate


def dist(a, b):
    return math.hypot((a["lat"] - b["lat"]) * 111.2, (a["lon"] - b["lon"]) * 111.2 * math.cos(math.radians(a["lat"])))


def main():
    src = Path(sys.argv[1]) if len(sys.argv) > 1 else CACHE
    els = json.loads(src.read_text(encoding="utf-8"))
    if src != CACHE:
        CACHE.write_text(json.dumps(els, ensure_ascii=False), encoding="utf-8")
    order = {"suburb": 0, "quarter": 1, "neighbourhood": 2}
    places = []
    for e in sorted(els, key=lambda e: order.get(e["tags"].get("place"), 3)):
        t = e["tags"]
        q = {"id": str(e["id"]), "ru": t.get("name:ru") or t["name"], "en": t.get("name:en") or t["name"], "lat": e["lat"], "lon": e["lon"]}
        if any(dist(q, p) < 0.6 for p in places):
            continue
        places.append(q)
    r = random.Random(7)
    byid = {q["id"]: q for q in places}
    for qid, (k, k2) in KNOWN.items():
        assert qid in byid, qid
        byid[qid].update(kind=k, second=k2, visits=r.randint(2, 14), firstDay=(BASE // DAY) - r.randint(5, 40))
    byid[HOME]["visits"] = 41
    for q in places:
        q.setdefault("kind", ""); q.setdefault("second", ""); q.setdefault("visits", 0); q.setdefault("firstDay", 0)

    known = list(KNOWN)
    trips, deals = [], []
    for i in range(24):
        start = BASE - (i * 0.9 + 0.3) * DAY - r.randint(0, 6) * 3_600_000
        a = HOME if i % 2 == 0 else r.choice(known)
        b = r.choice([k for k in known if k != a])
        path = [a] + r.sample([k for k in known if k not in (a, b)], r.randint(0, 2)) + [b]
        km = round(sum(dist(byid[path[j]], byid[path[j + 1]]) for j in range(len(path) - 1)) * 1.3, 1)
        ride = r.choice(["car", "car", "bus", "metro", "tram"])
        t = {"start": int(start), "end": int(start + km * 150_000), "ride": ride, "meters": km * 1000, "quarters": path,
             "fresh": [], "jamSec": r.choice([0, 0, 240, 600]), "night": i in (3, 11), "underground": 1 if ride == "metro" else 0,
             "renown": 8 + len(path) * 2, "find": r.choice(["", "", "honey", "amber", "tea"]), "curio": "", "broken": [],
             "weather": "rain" if i in (5, 6) else "", "outside": False}
        trips.append(t)
        for j in range(r.randint(0, 3)):
            g = r.choice(["honey", "amber", "silk", "scroll", "cogs", "tea", "glass", "spice"])
            paid = r.randint(9, 26)
            deals.append({"good": g, "quarter": b, "price": paid + r.randint(6, 30), "at": int(t["end"] + 60_000 + j * 1000), "buy": False, "origin": a, "paid": paid})
    trips[0]["curio"] = "lamp"
    trips[2]["fresh"] = ["2330679958"]
    deals.sort(key=lambda d: d["at"])

    pack = [
        {"id": 901, "good": "honey", "origin": "2330503343", "paid": 16, "at": BASE - 5 * 3_600_000, "auto": True},
        {"id": 902, "good": "honey", "origin": "2330503343", "paid": 17, "at": BASE - 5 * 3_600_000, "auto": False},
        {"id": 903, "good": "amber", "origin": HOME, "paid": 21, "at": BASE - 20 * 3_600_000, "auto": False},
        {"id": 904, "good": "silk", "origin": "2330498613", "paid": 27, "at": BASE - 2 * DAY, "auto": False},
        {"id": 905, "good": "fish", "origin": HOME, "paid": 0, "at": BASE - 2 * 3_600_000, "auto": False},
        {"id": 906, "good": "cogs", "origin": "2330537356", "paid": 18, "at": BASE - DAY, "auto": True},
        {"id": 907, "good": "tea", "origin": "2330492561", "paid": 14, "at": BASE - 3 * 3_600_000, "auto": False},
        {"id": 908, "good": "glass", "origin": "2330493066", "paid": 24, "at": BASE - DAY, "auto": False},
        {"id": 909, "good": "dust", "origin": HOME, "paid": 88, "at": BASE - 9 * 3_600_000, "auto": False},
    ]
    routes = [
        {"a": min(HOME, "2330498613"), "b": max(HOME, "2330498613"), "trips": 12, "built": ["stall", "well"]},
        {"a": min(HOME, "2330503343"), "b": max(HOME, "2330503343"), "trips": 6, "built": ["stall"]},
        {"a": min(HOME, "2330491203"), "b": max(HOME, "2330491203"), "trips": 4, "built": []},
        {"a": min("2330537356", "2330492561"), "b": max("2330537356", "2330492561"), "trips": 2, "built": []},
    ]
    curios = ["lamp", "bud", "peacock", "shell", "boatcompass", "pearls", "doorkey", "tile", "mosaic", "astrolabe", "horseshoe",
              "carpet", "beads", "ticket", "staff", "moonshard", "nightflower", "minerlamp", "raindrop"]
    intros = ["i.gate", "i.ontrip", "i.house", "i.map", "i.ledger", "i.guild", "i.market", "i.pack", "i.rides", "i.day", "i.quarter",
              "i.trip", "i.route", "i.merchant", "i.contracts", "i.curios", "i.shop"]
    state = {
        "v": 1, "base": BASE, "onboarded": True, "homeLat": byid[HOME]["lat"], "homeLon": byid[HOME]["lon"],
        "quarters": places, "here": HERE, "gold": 1840, "renown": 430, "packLevel": 2, "pack": pack,
        "trips": trips, "deals": deals, "routes": routes, "day": BASE // DAY, "traded": {}, "orders": [f"{BASE // DAY}:1"],
        "nightUntil": BASE + 8 * 3_600_000, "told": intros, "theme": "", "voice": True, "ride": "car", "nextId": 1000,
        "house": ["gate", "storeroom", "garden", "fountain", "shed"], "giftDay": 0, "curios": curios,
        "mStep": {"water": 4, "old": 1, "bazaar": 2, "garden": 0},
        "mSince": {"old": BASE - 6 * DAY, "bazaar": BASE - DAY, "garden": BASE - 3 * 3_600_000},
        "contracts": [{"id": (BASE // DAY) * 10 - 9, "good": "silk", "n": 3, "where": "forge", "until": BASE + DAY, "deposit": 68, "reward": 199,
                       "since": BASE - 10 * 3_600_000, "done": 0}],
        "encounter": "", "encDay": 0, "encCount": 0,
        "shops": [{"quarter": "2330498613", "stock": [{"id": 950, "good": "amber", "origin": HOME, "paid": 20, "at": BASE - DAY, "auto": False}], "till": 96, "tick": BASE - 3_600_000}],
        "luck": 0, "tut": 6, "btAddr": "demo", "btName": "Skoda Octavia",
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(state, ensure_ascii=False), encoding="utf-8")
    print("demo", len(places), "quarters,", len(trips), "trips")


if __name__ == "__main__":
    main()
