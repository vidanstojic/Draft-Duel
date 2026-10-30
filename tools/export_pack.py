"""Export a ThemePack from the rating database into the app's bundled resources.

The pack format follows spec §5 (id, version, theme, categories, athletes[] with id, name,
rating, categories, avatar) plus two optional athlete fields the app shows on the card:
position (GK/DF/MF/FW) and season (the last season of the rating window, e.g. "2025/26").

Usage (Anaconda Prompt, from the repo root):
    python tools/export_pack.py C:/Users/<you>/Desktop/data/v5/draft_duel.db
    python tools/export_pack.py <db> --pack football-top5 --version 1 --out <path>
"""

from __future__ import annotations

import argparse
import json
import sqlite3
import sys
from pathlib import Path

DEFAULT_OUT = Path(__file__).resolve().parents[1] / "composeApp/src/commonMain/composeResources/files/packs/football.json"
POSITIONS = {"GK", "DF", "MF", "FW"}


def season_label(start_year: int) -> str:
    return f"{start_year}/{(start_year + 1) % 100:02d}"


def load_athletes(db: sqlite3.Connection, pack_id: str, version: int, model: str) -> list[dict]:
    rows = db.execute(
        """
        SELECT pa.athlete_id, pa.name, pa.rating, pa.category,
               pa.avatar_color, pa.avatar_number, pa.avatar_initials,
               p.position,
               (SELECT MAX(s.season_start) FROM player_seasons s WHERE s.player_qid = pa.player_qid) AS last_season
        FROM pack_athletes pa
        JOIN players p ON p.player_qid = pa.player_qid
        JOIN ratings r ON r.player_qid = pa.player_qid AND r.model_version = ?
        WHERE pa.pack_id = ? AND pa.pack_version = ?
        ORDER BY pa.rating DESC, pa.name
        """,
        (model, pack_id, version),
    ).fetchall()
    athletes = []
    for athlete_id, name, rating, category, color, number, initials, position, last_season in rows:
        if not 60 <= rating <= 99:
            raise ValueError(f"{name}: rating {rating} outside 60-99")
        if position not in POSITIONS:
            raise ValueError(f"{name}: unknown position {position!r}")
        athlete = {
            "id": athlete_id,
            "name": name,
            "rating": rating,
            "categories": [category],
            "position": position,
            "avatar": {"color": color, "number": number, "initials": initials},
        }
        if last_season is not None:
            athlete["season"] = season_label(last_season)
        athletes.append(athlete)
    return athletes


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("db", type=Path, help="path to draft_duel.db")
    parser.add_argument("--pack", default="football-top5")
    parser.add_argument("--version", type=int, default=1)
    parser.add_argument("--model", default="v5", help="ratings.model_version to join on")
    parser.add_argument("--theme", default="Football")
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
    args = parser.parse_args()

    db = sqlite3.connect(args.db)
    athletes = load_athletes(db, args.pack, args.version, args.model)
    if not athletes:
        print(f"no athletes for pack {args.pack} v{args.version}", file=sys.stderr)
        return 1

    categories = sorted({c for a in athletes for c in a["categories"]})
    small = [c for c in categories if sum(c in a["categories"] for a in athletes) < 10]
    if small:
        print(f"warning: categories with fewer than 10 athletes cannot be used as a pool: {small}", file=sys.stderr)

    pack = {
        "id": args.pack,
        "version": args.version,
        "theme": args.theme,
        "categories": categories,
        "athletes": athletes,
    }
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(pack, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {len(athletes)} athletes, {len(categories)} categories -> {args.out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
