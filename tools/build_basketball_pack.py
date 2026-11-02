"""Build the bundled basketball pack (files/packs/basketball.json) from the rating tables.

Inputs are tools/basketball/nba_igraci.csv and evroliga_igraci.csv (columns: ime, prezime,
rejting, pozicija, klub). Ratings there are hand-assigned by the author (spec §5: "autor,
ručno"), one scale per league. The pack takes 50 players per league as two categories, NBA and
EuroLeague, in four tiers like the football pack: 6 elite, 14 strong, 20 solid, 10 role players.
Club names stay out of the pack (spec §5); the card shows the league instead.

Usage (from the repo root):
    python tools/build_basketball_pack.py
    python tools/build_basketball_pack.py --version 2 --out <path>
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "tools/basketball"
DEFAULT_OUT = ROOT / "composeApp/src/commonMain/composeResources/files/packs/basketball.json"
SEASON = "2025/26"  # the season the ratings are based on
PACK_SIZE = 50  # per league

# Picked by hand: well-known names in each tier, spread over clubs and positions.
PICKS = {
    "NBA": (
        "nba_igraci.csv",
        [
            # stars (90+)
            "Nikola Jokic", "Shai Gilgeous-Alexander", "Luka Doncic", "Victor Wembanyama",
            "Giannis Antetokounmpo", "Stephen Curry",
            # strong (80-89)
            "Cooper Flagg", "Joel Embiid", "LeBron James", "Kyrie Irving", "Trae Young", "Ja Morant",
            "Zion Williamson", "LaMelo Ball", "Jimmy Butler III", "Darius Garland", "Paul George",
            "Damian Lillard", "DeMar DeRozan", "Kristaps Porzingis", "Draymond Green", "Nikola Vucevic",
            # solid (79 and below)
            "Klay Thompson", "Al Horford", "Dennis Schroder", "AJ Dybantsa", "Nikola Jovic", "Deandre Ayton",
            "Marcus Smart", "Harrison Barnes", "Tobias Harris", "Steven Adams", "Buddy Hield",
            "Tim Hardaway Jr.", "Bogdan Bogdanovic", "Jusuf Nurkic", "Kevin Huerter", "Mike Conley",
            "Gary Payton II", "Clint Capela", "Jordan Clarkson", "Caris LeVert", "Kyle Anderson",
            "Andre Drummond", "Kevon Looney", "Ben Simmons", "DeAndre Jordan", "Bronny James",
            "Tacko Fall", "Moritz Wagner",
        ],
    ),
    "EuroLeague": (
        "evroliga_igraci.csv",
        [
            # stars (90+)
            "Sasha Vezenkov", "Kendrick Nunn", "Facundo Campazzo", "Edy Tavares",
            # strong (80-89)
            "Nadir Hifi", "Jonas Valanciunas", "Vasilije Micic", "Mathias Lessort", "Kevin Punter",
            "Dzanan Musa", "Shane Larkin", "Mike James", "Chima Moneke", "Filip Petrusev", "TJ Shorts",
            "Nigel Hayes-Davis", "Evan Fournier", "Jordan Nwora", "Kostas Sloukas", "Marko Guduric",
            "Luca Vildoza", "Sergio Llull",
            # solid (79 and below)
            "Nikola Kalinic", "Kostas Papanikolaou", "Alberto Abalde", "Alessandro Pajola",
            "Joffrey Lauvergne", "Daniel Hackett", "Dejan Davidovac", "Patty Mills", "Rodions Kurucs",
            "Aleksej Nedeljkovic", "Furkan Korkmaz", "Kai Jones", "Duane Washington Jr.", "Vladimir Lucic",
            "Nikola Djurisic", "Stefan Miljenovic", "Uros Mijailovic", "Nemanja Dangubic",
            "Patrick Baldwin Jr.", "Kevarrius Hayes", "Tonye Jekiri", "Mario Nakic", "Gabriele Procida",
            "Chuma Okeke", "Johannes Voigtmann", "Niels Giffey", "Marius Grigonis", "Edgaras Ulanovas",
        ],
    ),
}

# The tables are plain ASCII; the card shows these names with their own letters.
DISPLAY = {
    "Nikola Jokic": "Nikola Jokić", "Luka Doncic": "Luka Dončić", "Nikola Vucevic": "Nikola Vučević",
    "Nikola Jovic": "Nikola Jović", "Bogdan Bogdanovic": "Bogdan Bogdanović",
    "Dennis Schroder": "Dennis Schröder", "Jonas Valanciunas": "Jonas Valančiūnas",
    "Vasilije Micic": "Vasilije Micić", "Dzanan Musa": "Džanan Musa", "Filip Petrusev": "Filip Petrušev",
    "Dario Saric": "Dario Šarić", "Marko Guduric": "Marko Gudurić", "Vanja Marinkovic": "Vanja Marinković",
    "Ognjen Dobric": "Ognjen Dobrić", "Nikola Kalinic": "Nikola Kalinić", "Jusuf Nurkic": "Jusuf Nurkić",
    "Vladimir Lucic": "Vladimir Lučić", "Nikola Djurisic": "Nikola Đurišić",
    "Stefan Miljenovic": "Stefan Miljenović", "Uros Mijailovic": "Uroš Mijailović",
    "Nemanja Dangubic": "Nemanja Dangubić", "Mario Nakic": "Mario Nakić",
    "Aleksej Nedeljkovic": "Aleksej Nedeljković",
}

POSITIONS = {"PG": "G", "SG": "G", "SF": "F", "PF": "F", "G": "G", "F": "F", "C": "C"}
COLORS = ["#1F5E5A", "#B9770E", "#212F3D", "#7B2D26", "#2E4A7D", "#4A5D23", "#5B2C6F", "#8C4F2B"]


def slug(text: str) -> str:
    return "".join(c if c.isalnum() else "-" for c in text.lower()).strip("-").replace("--", "-")


def athlete(league: str, row: dict) -> dict:
    key = f"{row['ime']} {row['prezime']}"
    name = DISPLAY.get(key, key)
    digest = int(hashlib.sha1(key.encode()).hexdigest(), 16)
    rating = int(row["rejting"])
    if not 60 <= rating <= 99:
        raise ValueError(f"{key}: rating {rating} outside 60-99")
    return {
        "id": f"bb-{'nba' if league == 'NBA' else 'el'}-{slug(key)}",
        "name": name,
        "rating": rating,
        "categories": [league],
        "position": POSITIONS[row["pozicija"]],
        "season": SEASON,
        "avatar": {
            "color": COLORS[digest % len(COLORS)],
            "number": digest % 100,  # decorative, not the real jersey number
            "initials": (row["ime"][:1] + row["prezime"][:1]).upper(),
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--version", type=int, default=1)
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
    args = parser.parse_args()

    athletes = []
    for league, (file, picks) in PICKS.items():
        if len(picks) != PACK_SIZE or len(set(picks)) != len(picks):
            print(f"{league}: need {PACK_SIZE} different picks, got {len(set(picks))}", file=sys.stderr)
            return 1
        with open(DATA / file, encoding="utf-8-sig", newline="") as f:
            rows = {f"{r['ime']} {r['prezime']}": r for r in csv.DictReader(f)}
        missing = [p for p in picks if p not in rows]
        if missing:
            print(f"{league}: not in {file}: {missing}", file=sys.stderr)
            return 1
        chosen = sorted((athlete(league, rows[p]) for p in picks), key=lambda a: (-a["rating"], a["name"]))
        ratings = [a["rating"] for a in chosen]
        print(f"{league}: {len(chosen)} players, ratings {min(ratings)}-{max(ratings)}, "
              f"average {sum(ratings) / len(ratings):.1f}")
        athletes += chosen

    pack = {
        "id": "basketball-v1",
        "version": args.version,
        "theme": "Basketball",
        "categories": list(PICKS),
        "athletes": athletes,
    }
    args.out.write_text(json.dumps(pack, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {len(athletes)} athletes to {args.out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
