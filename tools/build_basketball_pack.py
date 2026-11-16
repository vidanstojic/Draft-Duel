"""Build the bundled basketball pack (files/packs/basketball.json) from the rating tables.

Inputs are tools/basketball/nba_igraci.csv and evroliga_igraci.csv (columns: ime, prezime,
rejting, pozicija, klub). Ratings there are hand-assigned by the author (spec §5: "autor,
ručno"), one scale per league. The pack has two categories, NBA (150 players) and EuroLeague
(100), split 10 / 30 / 60 % into stars, strong and solid players (spec §5). The hand-picked
well-known names (PICKS) are always in; the rest of each tier is filled from the tables, highest
rating first and spread over the clubs, so every team is in the pack. Club names stay out of the
pack (spec §5); the card shows the league instead.

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
# Players per league and the lowest rating that counts as a star in that league's scale.
TARGETS = {"NBA": (150, 90), "EuroLeague": (100, 88)}
STRONG_LINE = 80
TIER_SHARE = {"star": 0.10, "strong": 0.30, "solid": 0.60}

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


def tier(rating: int, star_line: int) -> str:
    if rating >= star_line:
        return "star"
    return "strong" if rating >= STRONG_LINE else "solid"


def fill(league: str, rows: dict[str, dict], picks: list[str]) -> list[str]:
    """The picks plus the best of the rest per tier, one club at a time, up to the tier's share."""
    size, star_line = TARGETS[league]
    chosen = list(picks)
    for name, share in TIER_SHARE.items():
        want = round(size * share)
        have = [p for p in chosen if tier(int(rows[p]["rejting"]), star_line) == name]
        if len(have) > want:
            raise ValueError(f"{league}: {len(have)} picked {name} players, the tier holds {want}")
        rest = sorted(
            (k for k, r in rows.items() if k not in chosen and tier(int(r["rejting"]), star_line) == name),
            key=lambda k: (-int(rows[k]["rejting"]), k),
        )
        # Round robin over clubs: each club gives its best remaining player in turn.
        by_club: dict[str, list[str]] = {}
        for k in rest:
            by_club.setdefault(rows[k]["klub"], []).append(k)
        added: list[str] = []
        while len(have) + len(added) < want and any(by_club.values()):
            for club in sorted(by_club, key=lambda c: (-int(rows[by_club[c][0]]["rejting"]) if by_club[c] else 0, c)):
                if by_club[club] and len(have) + len(added) < want:
                    added.append(by_club[club].pop(0))
        if len(have) + len(added) < want:
            raise ValueError(f"{league}: only {len(have) + len(added)} {name} players for {want}")
        chosen += added
    return chosen


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--version", type=int, default=2)
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
    args = parser.parse_args()

    athletes = []
    for league, (file, picks) in PICKS.items():
        if len(set(picks)) != len(picks):
            print(f"{league}: picks repeat a name", file=sys.stderr)
            return 1
        with open(DATA / file, encoding="utf-8-sig", newline="") as f:
            rows = {f"{r['ime']} {r['prezime']}": r for r in csv.DictReader(f)}
        missing = [p for p in picks if p not in rows]
        if missing:
            print(f"{league}: not in {file}: {missing}", file=sys.stderr)
            return 1
        names = fill(league, rows, picks)
        chosen = sorted((athlete(league, rows[p]) for p in names), key=lambda a: (-a["rating"], a["name"]))
        ratings = [a["rating"] for a in chosen]
        star_line = TARGETS[league][1]
        tiers = [sum(tier(r, star_line) == t for r in ratings) for t in TIER_SHARE]
        clubs = len({rows[p]["klub"] for p in names})
        print(f"{league}: {len(chosen)} players from {clubs} clubs, ratings {min(ratings)}-{max(ratings)}, "
              f"average {sum(ratings) / len(ratings):.1f}, stars / strong / solid {tiers[0]} / {tiers[1]} / {tiers[2]}")
        athletes += chosen

    pack = {
        "id": "basketball-v2",
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
