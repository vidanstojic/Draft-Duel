"""Check the bundled packs before shipping them.

Rating packs (football, movies): ratings 60-99, every category has at least 10 cards, and the
balance of stars (90-99), strong (80-89) and solid (60-79) cards is reported against spec §5.
Recipe packs (food): each category is a dish whose fitting ingredients add up to exactly 100
points and that has enough wrong ingredients to fill a pool of 10.

Usage, from the repo root:
    python tools/validate_pack.py                     # every file in composeResources/files/packs
    python tools/validate_pack.py path/to/pack.json
"""

from __future__ import annotations

import json
import sys
from collections import Counter
from pathlib import Path

PACK_DIR = Path(__file__).resolve().parents[1] / "composeApp/src/commonMain/composeResources/files/packs"
RECIPE_PACKS = {"food"}
POOL_SIZE = 10


def validate(path: Path) -> list[str]:
    pack = json.loads(path.read_text(encoding="utf-8"))
    athletes = pack["athletes"]
    errors: list[str] = []

    ids = Counter(a["id"] for a in athletes)
    errors += [f"duplicate id {i}" for i, n in ids.items() if n > 1]

    by_category: dict[str, list[dict]] = {}
    for a in athletes:
        for c in a["categories"]:
            by_category.setdefault(c, []).append(a)

    if path.stem in RECIPE_PACKS:
        for dish, cards in by_category.items():
            good = [a for a in cards if a["rating"] > 0]
            wrong = [a for a in cards if a["rating"] <= 0]
            total = sum(a["rating"] for a in good)
            if total != 100:
                errors.append(f"{dish}: fitting ingredients add up to {total}, not 100")
            if len(good) >= POOL_SIZE or len(good) + len(wrong) < POOL_SIZE:
                errors.append(f"{dish}: {len(good)} fitting + {len(wrong)} wrong cannot fill a pool of {POOL_SIZE} with both kinds")
        print(f"{path.name}: {len(by_category)} dishes, {len(athletes)} ingredients")
    else:
        errors += [f"{a['name']}: rating {a['rating']} outside 60-99" for a in athletes if not 60 <= a["rating"] <= 99]
        errors += [f"category {c} has {len(v)} cards (< {POOL_SIZE})" for c, v in by_category.items() if len(v) < POOL_SIZE]
        n = len(athletes)
        stars = sum(a["rating"] >= 90 for a in athletes)
        strong = sum(80 <= a["rating"] < 90 for a in athletes)
        solid = n - stars - strong
        print(f"{path.name}: {n} cards, {len(by_category)} categories, "
              f"stars {stars} ({stars / n:.0%}), strong {strong} ({strong / n:.0%}), solid {solid} ({solid / n:.0%}); spec 10/30/60%")
    return errors


def main() -> int:
    paths = [Path(p) for p in sys.argv[1:]] or sorted(PACK_DIR.glob("*.json"))
    failed = False
    for path in paths:
        for error in validate(path):
            failed = True
            print(f"  ERROR {error}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
