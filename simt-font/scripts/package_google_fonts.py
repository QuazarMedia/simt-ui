#!/usr/bin/env python3
"""Package released Simt Font files for a google/fonts pull request."""

import argparse
from datetime import date
from pathlib import Path
import re
import shutil
import sys


MODULE = Path(__file__).resolve().parents[1]
FONT_FILES = ("SimtFont-Regular.ttf", "SimtFont-Bold.ttf")


def font_version():
    source = (MODULE / "sources" / "build.py").read_text()
    match = re.search(r'^VERSION = "([0-9]+\.[0-9]+)"$', source, re.MULTILINE)
    if not match:
        raise ValueError("VERSION in sources/build.py must look like 1.000")
    return match.group(1)


def package(destination, commit, date_added, font_dir=None):
    if not re.fullmatch(r"[0-9a-f]{40}", commit):
        raise ValueError("commit must be a full lowercase Git SHA")
    if not re.fullmatch(r"[0-9]{4}-[0-9]{2}-[0-9]{2}", date_added):
        raise ValueError("date-added must look like YYYY-MM-DD")
    date.fromisoformat(date_added)

    family_dir = destination / "ofl" / "simtfont"
    if family_dir.exists():
        raise ValueError(f"refusing to overwrite existing {family_dir}")

    font_dir = font_dir or MODULE / "fonts" / "ttf"
    article_dir = family_dir / "article"
    article_dir.mkdir(parents=True)
    for name in FONT_FILES:
        shutil.copy2(font_dir / name, family_dir / name)
    shutil.copy2(MODULE / "OFL.txt", family_dir / "OFL.txt")
    shutil.copy2(
        MODULE / "google-fonts" / "article" / "ARTICLE.en_us.html",
        article_dir / "ARTICLE.en_us.html",
    )

    metadata = (MODULE / "google-fonts" / "METADATA.pb").read_text()
    metadata = metadata.replace("{{SOURCE_COMMIT}}", commit)
    metadata = metadata.replace("{{DATE_ADDED}}", date_added)
    if "{{" in metadata:
        raise ValueError("METADATA.pb contains an unresolved placeholder")
    (family_dir / "METADATA.pb").write_text(metadata)
    return family_dir


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("destination", type=Path)
    parser.add_argument("--commit", required=True)
    parser.add_argument("--date-added", required=True)
    parser.add_argument("--font-dir", type=Path)
    parser.add_argument("--github-output", type=Path)
    args = parser.parse_args()

    try:
        family_dir = package(
            args.destination, args.commit, args.date_added, args.font_dir
        )
        version = font_version()
    except (OSError, ValueError) as error:
        parser.exit(1, f"error: {error}\n")

    if args.github_output:
        with args.github_output.open("a") as output:
            output.write(f"version={version}\n")
    print(f"Packaged Simt Font {version} at {family_dir}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
