"""Build the bundled football pack (files/packs/football.json) straight from the rating database.

Reads ratings (model v5) and players from draft_duel.db, which tools of the data pipeline
(Desktop/data/build_v5.py) write, and picks PER_LEAGUE players for each of the five leagues
(categories England, Spain, Italy, Germany, France). Every league gets 3 stars (90+), 9 strong
(80-89) and 18 solid players (60-79), spec §5's 10 / 30 / 60 %; a league without 90+ players
gives those slots to its best strong players.

Always in the pack: the players of the previous pack (KEEP, read from the current file) and the
well-known names in FAMOUS, whatever their tier, so stars of the real game are not left out just
because their rating is a point lower. The rest of each tier is filled by rating; the solid tier
is spread from the top to the bottom of its range, and each league keeps at least 3 goalkeepers,
8 defenders, 7 midfielders and 6 forwards.

Usage (from the repo root):
    python tools/build_football_pack.py C:/Users/<you>/Desktop/data/v5/draft_duel.db
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUT = ROOT / "composeApp/src/commonMain/composeResources/files/packs/football.json"
LEAGUES = ["England", "Spain", "Italy", "Germany", "France"]
PER_LEAGUE = 30
QUOTA = {"star": 3, "strong": 9, "solid": 18}
MIN_POSITIONS = {"GK": 3, "DF": 8, "MF": 7, "FW": 6}
SOLID_FLOOR = 65  # no solid pick below this, so every card is someone a fan may know

FAMOUS = {
    "England": ["Mohamed Salah", "Bukayo Saka", "Cole Palmer", "Phil Foden", "Alexander Isak", "Martin Ødegaard",
                "Rodri", "Alisson Becker", "Bruno Fernandes", "Virgil van Dijk", "Florian Wirtz"],
    "Spain": ["Lamine Yamal", "Jude Bellingham", "Vinícius Júnior", "Pedri", "Thibaut Courtois", "Antonio Rüdiger",
              "Julián Alvarez", "Raphinha", "Federico Valverde", "Trent Alexander-Arnold", "Frenkie de Jong"],
    "Italy": ["Nicolò Barella", "Kevin De Bruyne", "Luka Modrić", "Romelu Lukaku", "Dušan Vlahović", "Mike Maignan",
              "Marcus Thuram", "Alessandro Bastoni", "Scott McTominay", "Christopher Nkunku"],
    "Germany": ["Jamal Musiala", "Joshua Kimmich", "Manuel Neuer", "Michael Olise", "Luis Díaz", "Alphonso Davies",
                "Julian Brandt", "Gregor Kobel", "Dayot Upamecano", "Nicolas Jackson"],
    "France": ["Ousmane Dembélé", "Khvicha Kvaratskhelia", "Marquinhos", "Mason Greenwood", "Bradley Barcola",
               "Nuno Mendes", "Warren Zaïre-Emery", "Pierre-Emerick Aubameyang", "Olivier Giroud", "Désiré Doué"],
}

PALETTE = ["#C0392B", "#2471A3", "#1E8449", "#B9770E", "#7D3C98", "#117A65", "#A04000", "#283747",
           "#922B21", "#1F618D", "#196F3D", "#9A7D0A", "#6C3483", "#0E6655", "#873600", "#212F3D"]
SHIRT_NUMBERS = {"GK": [1, 13, 23, 30], "DF": [2, 3, 4, 5, 6, 12, 15, 22], "MF": [6, 8, 10, 14, 16, 18, 20, 21],
                 "FW": [7, 9, 10, 11, 17, 19, 27, 29]}


def tier(rating: int) -> str:
    return "star" if rating >= 90 else "strong" if rating >= 80 else "solid"


def avatar(qid: str, name: str, position: str) -> dict:
    """Same avatar as build_v5.py gives, so cards of the previous pack look the same."""
    h = int(hashlib.sha1(qid.encode()).hexdigest(), 16)
    parts = [p for p in re.split(r"[\s\-]+", name) if p]
    initials = (parts[0][0] + (parts[-1][0] if len(parts) > 1 else "")).upper()
    numbers = SHIRT_NUMBERS[position]
    return {"color": PALETTE[h % len(PALETTE)], "number": numbers[(h // 7) % len(numbers)], "initials": initials}


def pick_league(rows: list[dict], keep: set[str], famous: list[str]) -> list[dict]:
    by_name = {r["name"]: r for r in rows}
    missing = [n for n in famous if n not in by_name]
    if missing:
        print(f"  not in the database: {', '.join(missing)}", file=sys.stderr)
    forced = [r for r in rows if r["qid"] in keep or r["name"] in famous]
    chosen = {r["qid"]: r for r in forced}

    quota = dict(QUOTA)
    if not any(tier(r["rating"]) == "star" for r in rows):
        quota["strong"] += quota.pop("star")
    for name in ("star", "strong"):
        if name not in quota:
            continue
        have = sum(tier(r["rating"]) == name for r in chosen.values())
        for r in rows:  # rows are sorted by rating, best first
            if have >= quota[name]:
                break
            if tier(r["rating"]) == name and r["qid"] not in chosen:
                chosen[r["qid"]] = r
                have += 1

    # Solid: first the positions still short, then evenly over the range for variety.
    free = PER_LEAGUE - len(chosen)
    solid = [r for r in rows if tier(r["rating"]) == "solid" and r["rating"] >= SOLID_FLOOR and r["qid"] not in chosen]
    for position, minimum in MIN_POSITIONS.items():
        short = minimum - sum(r["position"] == position for r in chosen.values())
        for r in [r for r in solid if r["position"] == position][: max(short, 0)]:
            if free > 0:
                chosen[r["qid"]] = r
                free -= 1
    solid = [r for r in solid if r["qid"] not in chosen]
    if free > 0 and solid:
        step = len(solid) / free
        for k in range(free):
            r = solid[min(int(k * step), len(solid) - 1)]
            chosen.setdefault(r["qid"], r)
    return sorted(chosen.values(), key=lambda r: (-r["rating"], r["name"]))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("db", type=Path, help="path to draft_duel.db")
    parser.add_argument("--model", default="v5")
    parser.add_argument("--version", type=int, default=2)
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
    args = parser.parse_args()

    previous = json.loads(args.out.read_text(encoding="utf-8")) if args.out.exists() else {"athletes": []}
    keep = {a["id"].removeprefix("fb-") for a in previous["athletes"]}

    db = sqlite3.connect(args.db)
    athletes = []
    for league in LEAGUES:
        rows = [
            {"qid": q, "name": n, "rating": r, "position": p, "season": s}
            for q, n, r, p, s in db.execute(
                """
                SELECT r.player_qid, p.name, r.rating, p.position,
                       (SELECT MAX(s.season_start) FROM player_seasons s WHERE s.player_qid = r.player_qid)
                FROM ratings r JOIN players p USING (player_qid)
                WHERE r.model_version = ? AND r.category = ?
                ORDER BY r.rating DESC, r.score DESC
                """,
                (args.model, league),
            )
        ]
        print(f"{league}: {len(rows)} rated players")
        picked = pick_league(rows, keep, FAMOUS[league])
        tiers = [sum(tier(r["rating"]) == t for r in picked) for t in QUOTA]
        positions = {p: sum(r["position"] == p for r in picked) for p in MIN_POSITIONS}
        print(f"  picked {len(picked)}: stars / strong / solid {tiers[0]} / {tiers[1]} / {tiers[2]}, "
              f"ratings {picked[-1]['rating']}-{picked[0]['rating']}, {positions}")
        for r in picked:
            athlete = {
                "id": f"fb-{r['qid']}",
                "name": r["name"],
                "rating": r["rating"],
                "categories": [league],
                "position": r["position"],
                "avatar": avatar(r["qid"], r["name"], r["position"]),
            }
            if r["season"] is not None:
                athlete["season"] = f"{r['season']}/{(r['season'] + 1) % 100:02d}"
            athletes.append(athlete)

    lost = keep - {a["id"].removeprefix("fb-") for a in athletes}
    if lost:
        print(f"warning: players of the previous pack left out: {sorted(lost)}", file=sys.stderr)
    athletes.sort(key=lambda a: (-a["rating"], a["name"]))
    pack = {"id": "football-top5", "version": args.version, "theme": "Football", "categories": sorted(LEAGUES), "athletes": athletes}
    args.out.write_text(json.dumps(pack, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {len(athletes)} athletes -> {args.out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
