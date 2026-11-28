#!/usr/bin/env python3
"""Export the Simt TrueType fonts as editable UFO 3 sources."""

import argparse
from datetime import datetime, timezone
from pathlib import Path
import shutil
import sys
import tempfile
from types import SimpleNamespace

from fontTools.designspaceLib import (
    AxisDescriptor,
    DesignSpaceDocument,
    SourceDescriptor,
)
from fontTools.misc.timeTools import epoch_diff
from fontTools.pens.basePen import BasePen
from fontTools.pens.pointPen import SegmentToPointPen
from fontTools.pens.reverseContourPen import ReverseContourPen
from fontTools.ttLib import TTFont
from fontTools.ufoLib import UFOWriter
from fontTools.ufoLib.glifLib import Glyph

import build as font_build

MODULE = Path(__file__).resolve().parents[1]
DEFAULT_FONT_DIR = MODULE / "fonts" / "ttf"
DEFAULT_SOURCE_DIR = MODULE / "sources" / "ufo"
PANOSE_ATTRIBUTES = (
    "bFamilyType",
    "bSerifStyle",
    "bWeight",
    "bProportion",
    "bContrast",
    "bStrokeVariation",
    "bArmStyle",
    "bLetterForm",
    "bMidline",
    "bXHeight",
)


class TrueTypeToCubicPen(BasePen):
    def __init__(self, out_pen):
        super().__init__(None)
        self.out_pen = out_pen

    def _moveTo(self, point):
        self.out_pen.moveTo(point)

    def _lineTo(self, point):
        self.out_pen.lineTo(point)

    def _curveToOne(self, control1, control2, point):
        self.out_pen.curveTo(control1, control2, point)

    def _closePath(self):
        self.out_pen.closePath()

    def _endPath(self):
        self.out_pen.endPath()

    def addComponent(self, glyph_name, transform):
        self.out_pen.addComponent(glyph_name, transform)


def draw_cubic(glyph, glyf, pen):
    # BasePen materializes implied on-curve points in all-off-curve TrueType contours.
    glyph.draw(TrueTypeToCubicPen(ReverseContourPen(pen)), glyf)


def glyph_unicodes(font):
    unicodes = {name: [] for name in font.getGlyphOrder()}
    for codepoint, name in font.getBestCmap().items():
        unicodes[name].append(codepoint)
    for values in unicodes.values():
        values.sort()
    return unicodes


def name(font, name_id):
    return font["name"].getDebugName(name_id)


def set_bits(value, count):
    return [bit for bit in range(count) if value & (1 << bit)]


def ufo_info(font, style):
    head = font["head"]
    hhea = font["hhea"]
    os2 = font["OS/2"]
    post = font["post"]
    family = name(font, 16) or name(font, 1)
    version_major = int(head.fontRevision)
    created = datetime.fromtimestamp(head.created + epoch_diff, timezone.utc)
    return SimpleNamespace(
        familyName=family,
        styleName=name(font, 17) or name(font, 2),
        styleMapFamilyName=family,
        styleMapStyleName="bold" if style == "Bold" else "regular",
        unitsPerEm=head.unitsPerEm,
        ascender=hhea.ascent,
        descender=hhea.descent,
        xHeight=os2.sxHeight,
        capHeight=os2.sCapHeight,
        copyright=name(font, 0),
        versionMajor=version_major,
        versionMinor=round((head.fontRevision - version_major) * 1000),
        postscriptFontName=name(font, 6),
        postscriptUnderlinePosition=post.underlinePosition,
        postscriptUnderlineThickness=post.underlineThickness,
        openTypeNameDesigner=name(font, 9),
        openTypeNameDesignerURL=name(font, 12),
        openTypeNameManufacturer=name(font, 8),
        openTypeNameManufacturerURL=name(font, 11),
        openTypeNameLicense=name(font, 13),
        openTypeNameLicenseURL=name(font, 14),
        openTypeNameDescription=name(font, 10),
        openTypeNameSampleText=name(font, 19),
        openTypeNameRecords=[
            {
                "nameID": record.nameID,
                "platformID": record.platformID,
                "encodingID": record.platEncID,
                "languageID": record.langID,
                "string": record.toUnicode(),
            }
            for record in font["name"].names
        ],
        openTypeOS2WeightClass=os2.usWeightClass,
        openTypeOS2WidthClass=os2.usWidthClass,
        openTypeOS2TypoAscender=os2.sTypoAscender,
        openTypeOS2TypoDescender=os2.sTypoDescender,
        openTypeOS2TypoLineGap=os2.sTypoLineGap,
        openTypeOS2WinAscent=os2.usWinAscent,
        openTypeOS2WinDescent=os2.usWinDescent,
        openTypeOS2VendorID=os2.achVendID,
        openTypeOS2FamilyClass=[os2.sFamilyClass >> 8, os2.sFamilyClass & 0xFF],
        openTypeOS2UnicodeRanges=set_bits(os2.ulUnicodeRange1, 32)
        + set_bits(os2.ulUnicodeRange2, 32)
        + set_bits(os2.ulUnicodeRange3, 32)
        + set_bits(os2.ulUnicodeRange4, 32),
        openTypeOS2CodePageRanges=set_bits(os2.ulCodePageRange1, 32)
        + set_bits(os2.ulCodePageRange2, 32),
        openTypeOS2Selection=[
            bit
            for bit in (1, 2, 3, 4, 7, 8, 9)
            if os2.fsSelection & (1 << bit)
        ],
        openTypeOS2Panose=[getattr(os2.panose, key) for key in PANOSE_ATTRIBUTES],
        openTypeHheaAscender=hhea.ascent,
        openTypeHheaDescender=hhea.descent,
        openTypeHheaLineGap=hhea.lineGap,
        openTypeHeadCreated=created.strftime("%Y/%m/%d %H:%M:%S"),
        openTypeHeadFlags=set_bits(head.flags, 14),
        openTypeHeadLowestRecPPEM=head.lowestRecPPEM,
    )


def source_features(style):
    glyphs, arabic_features = font_build.collect(
        font_build.WEIGHTS[style], with_arabic=True
    )
    return font_build.features(glyphs, arabic_features)


def export_font(font_path, style, ufo_path):
    font = TTFont(font_path, recalcTimestamp=False)
    try:
        glyf = font["glyf"]
        order = font.getGlyphOrder()
        unicodes = glyph_unicodes(font)
        writer = UFOWriter(ufo_path, formatVersion=3, fileCreator="org.simt.font")
        glyph_set = writer.getGlyphSet()
        for glyph_name in order:
            glyph = Glyph(glyph_name, glyph_set)
            glyph.width = font["hmtx"][glyph_name][0]
            glyph.unicodes = unicodes[glyph_name]

            def draw_points(point_pen, name=glyph_name):
                draw_cubic(glyf[name], glyf, SegmentToPointPen(point_pen))

            glyph_set.writeGlyph(
                glyph_name, glyphObject=glyph, drawPointsFunc=draw_points
            )
        glyph_set.writeContents()
        writer.writeLayerContents()
        writer.writeInfo(ufo_info(font, style))
        writer.writeFeatures(source_features(style))
        writer.writeLib({"public.glyphOrder": order})
        writer.close()
    finally:
        font.close()


def write_designspace(source_dir):
    document = DesignSpaceDocument()
    axis = AxisDescriptor()
    axis.name = "Weight"
    axis.tag = "wght"
    axis.minimum = 400
    axis.default = 400
    axis.maximum = 700
    document.addAxis(axis)

    for style, weight in (("Regular", 400), ("Bold", 700)):
        source = SourceDescriptor()
        source.filename = f"SimtFont-{style}.ufo"
        source.familyName = font_build.FAMILY
        source.styleName = style
        source.location = {"Weight": weight}
        source.copyInfo = style == "Regular"
        source.copyLib = style == "Regular"
        source.copyFeatures = style == "Regular"
        document.addSource(source)

    document.write(source_dir / "SimtFont.designspace")


def export(font_dir, source_dir):
    font_dir = font_dir.resolve()
    source_dir = source_dir.resolve()
    for style in font_build.WEIGHTS:
        if not (font_dir / f"SimtFont-{style}.ttf").is_file():
            raise FileNotFoundError(f"missing {font_dir / f'SimtFont-{style}.ttf'}")

    source_dir.parent.mkdir(parents=True, exist_ok=True)
    temporary = Path(tempfile.mkdtemp(prefix=".simt-ufo-", dir=source_dir.parent))
    try:
        for style in font_build.WEIGHTS:
            export_font(
                font_dir / f"SimtFont-{style}.ttf",
                style,
                temporary / f"SimtFont-{style}.ufo",
            )
        write_designspace(temporary)
        temporary.chmod(0o755)
        if source_dir.exists():
            shutil.rmtree(source_dir)
        temporary.replace(source_dir)
    finally:
        if temporary.exists():
            shutil.rmtree(temporary)

    for style in font_build.WEIGHTS:
        print(f"exported {source_dir / f'SimtFont-{style}.ufo'}")
    print(f"exported {source_dir / 'SimtFont.designspace'}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--font-dir", type=Path, default=DEFAULT_FONT_DIR)
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_SOURCE_DIR)
    args = parser.parse_args()
    try:
        export(args.font_dir, args.output_dir)
    except (OSError, ValueError) as error:
        parser.exit(1, f"error: {error}\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
