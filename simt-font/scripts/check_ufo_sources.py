#!/usr/bin/env python3
"""Verify that the editable UFO sources match the Simt TrueType fonts."""

import argparse
from pathlib import Path
import sys
from types import SimpleNamespace

from fontTools.designspaceLib import DesignSpaceDocument
from fontTools.pens.explicitClosingLinePen import ExplicitClosingLinePen
from fontTools.pens.recordingPen import RecordingPen
from fontTools.ttLib import TTFont
from fontTools.ufoLib import UFOReader

MODULE = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(MODULE / "sources"))

from export_ufo import (  # noqa: E402
    draw_cubic,
    glyph_unicodes,
    source_features,
    ufo_info,
)
import build as font_build  # noqa: E402

DEFAULT_FONT_DIR = MODULE / "fonts" / "ttf"
DEFAULT_SOURCE_DIR = MODULE / "sources" / "ufo"
failures = []


def check(label, ok, detail=""):
    status = "ok" if ok else "FAIL"
    print(f"  [{status}] {label}" + (f" — {detail}" if detail else ""))
    if not ok:
        failures.append(label)


def check_designspace(source_dir):
    path = source_dir / "SimtFont.designspace"
    check("SimtFont.designspace present", path.is_file())
    if not path.is_file():
        return
    try:
        document = DesignSpaceDocument.fromfile(path)
    except Exception as error:  # noqa: BLE001 - report any source parse failure
        check("SimtFont.designspace parses", False, str(error))
        return

    check("SimtFont.designspace parses", True)
    check("designspace has one Weight axis", len(document.axes) == 1)
    if len(document.axes) == 1:
        axis = document.axes[0]
        check(
            "designspace Weight axis range",
            (axis.name, axis.tag, axis.minimum, axis.default, axis.maximum)
            == ("Weight", "wght", 400, 400, 700),
        )
    check("designspace has two masters", len(document.sources) == 2)
    for style, weight in (("Regular", 400), ("Bold", 700)):
        matches = [source for source in document.sources if source.styleName == style]
        check(f"designspace {style} master", len(matches) == 1)
        if len(matches) != 1:
            continue
        source = matches[0]
        expected_path = (source_dir / f"SimtFont-{style}.ufo").resolve()
        check(
            f"designspace {style} source path",
            source.path is not None and Path(source.path).resolve() == expected_path,
        )
        check(
            f"designspace {style} location",
            source.location == {"Weight": weight},
        )
        check(
            f"designspace {style} family",
            source.familyName == font_build.FAMILY,
        )


def ttf_outline(glyph, glyf):
    recording = RecordingPen()
    draw_cubic(glyph, glyf, ExplicitClosingLinePen(recording))
    return recording.value


def check_font(style, font_dir, source_dir):
    font_path = font_dir / f"SimtFont-{style}.ttf"
    ufo_path = source_dir / f"SimtFont-{style}.ufo"
    check(f"{style} TTF present", font_path.is_file())
    check(f"{style} UFO present", ufo_path.is_dir())
    if not font_path.is_file() or not ufo_path.is_dir():
        return

    font = TTFont(font_path, recalcTimestamp=False)
    try:
        reader = UFOReader(ufo_path)
        check(f"{style} UFO format", reader.formatVersionTuple == (3, 0))
        glyph_set = reader.getGlyphSet()
        order = font.getGlyphOrder()
        ufo_order = list(glyph_set.keys())
        check(f"{style} glyph coverage", set(ufo_order) == set(order))
        check(
            f"{style} public.glyphOrder",
            reader.readLib().get("public.glyphOrder") == order,
        )

        expected_unicodes = glyph_unicodes(font)
        glyf = font["glyf"]
        width_mismatches = []
        unicode_mismatches = []
        outline_mismatches = []
        for glyph_name in order:
            if glyph_name not in glyph_set:
                continue
            glyph = glyph_set[glyph_name]
            actual = RecordingPen()
            glyph.draw(actual, outputImpliedClosingLine=True)
            if getattr(glyph, "width", 0) != font["hmtx"][glyph_name][0]:
                width_mismatches.append(glyph_name)
            if sorted(getattr(glyph, "unicodes", [])) != expected_unicodes[glyph_name]:
                unicode_mismatches.append(glyph_name)
            if actual.value != ttf_outline(glyf[glyph_name], glyf):
                outline_mismatches.append(glyph_name)

        check(
            f"{style} glyph advances",
            not width_mismatches,
            ", ".join(width_mismatches[:10]),
        )
        check(
            f"{style} Unicode mappings",
            not unicode_mismatches,
            ", ".join(unicode_mismatches[:10]),
        )
        check(
            f"{style} cubic outlines and components",
            not outline_mismatches,
            ", ".join(outline_mismatches[:10]),
        )

        info = SimpleNamespace()
        reader.readInfo(info)
        expected_info = vars(ufo_info(font, style))
        metadata_mismatches = sorted(
            key
            for key, value in expected_info.items()
            if getattr(info, key, None) != value
        )
        check(
            f"{style} font metadata",
            not metadata_mismatches,
            ", ".join(metadata_mismatches),
        )
        features = reader.readFeatures()
        check(f"{style} OpenType feature source", features == source_features(style))
        print(
            f"  [ok] {style} source summary — {len(order)} glyphs, "
            f"{len(font.getBestCmap())} codepoints"
        )
    except Exception as error:  # noqa: BLE001 - report any source parse failure
        check(f"{style} UFO reads", False, str(error))
    finally:
        font.close()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--font-dir", type=Path, default=DEFAULT_FONT_DIR)
    parser.add_argument("--source-dir", type=Path, default=DEFAULT_SOURCE_DIR)
    args = parser.parse_args()

    print("Simt Font editable source check")
    check_designspace(args.source_dir)
    for style in font_build.WEIGHTS:
        check_font(style, args.font_dir, args.source_dir)
    print()
    if failures:
        print(f"FAILED: {len(failures)} check(s): " + "; ".join(failures))
        return 1
    print("PASS: all editable source checks passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
