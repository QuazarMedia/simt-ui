"""Build Simt fonts.

Usage: python3 build.py  ->  writes fonts to ./fonts/
"""

import os
import sys
import unicodedata
from fontTools.fontBuilder import FontBuilder
from fontTools.misc.timeTools import timestampSinceEpoch
from fontTools.pens.ttGlyphPen import TTGlyphPen
from fontTools.ttLib import newTable
from fontTools.ttLib.tables._g_l_y_f import OVERLAP_COMPOUND

OVERLAP_SIMPLE = 0x40
from fontTools.feaLib.builder import addOpenTypeFeaturesFromString

from geom import draw_contours, bbox, Contour, poly
import latin
from latin import Glyph, X, CAP, ASC, DESC

VERSION = "1.000"
FAMILY = "Simt Font"
PS_FAMILY = "SimtFont"
AUTHOR = "Said Elimam"
AUTHOR_URL = "https://saidelimam.com"
COPYRIGHT = (
    "Copyright 2026 The Simt Font Project Authors "
    "(https://github.com/QuazarMedia/simt-ui); Said Elimam"
)
LICENSE_DESCRIPTION = (
    "This Font Software is licensed under the SIL Open Font License, Version 1.1. "
    "This license is available with a FAQ at: https://openfontlicense.org"
)
LICENSE_URL = "https://openfontlicense.org"
SOURCE_DATE_EPOCH = int(os.environ.get("SOURCE_DATE_EPOCH", "1795737600"))
BUILD_TIMESTAMP = timestampSinceEpoch(SOURCE_DATE_EPOCH)

WEIGHTS = {
    "Regular": dict(W=82, H=74, g=22, k=62, dot=92, sx=1.0, wght=400),
    "Bold": dict(W=122, H=108, g=26, k=88, dot=126, sx=1.07, wght=700),
}

TYPO_ASC, TYPO_DESC, TYPO_GAP = 980, -400, 0
WIN_ASC, WIN_DESC = 1100, 600


def notdef(P):
    W = 60
    c = poly((60, 0), (500, 0), (500, CAP), (60, CAP))
    inner = Contour(
        [
            (60 + W, W, True),
            (500 - W, W, True),
            (500 - W, CAP - W, True),
            (60 + W, CAP - W, True),
        ],
        hole=True,
    )
    return Glyph(".notdef", None, 560, c + [inner])


def collect(P, with_arabic=True):
    glyphs = [notdef(P)]
    glyphs += latin.build_latin(P)
    feats = ""
    if with_arabic:
        import arabic
        import variants

        # v0.2: redesigned hah family (ج ح خ چ) and kaf family (ك ک گ), option A
        arabic.hah_family = variants.hah_A
        arabic.kaf_family = variants.kaf_A
        ag, feats = arabic.build_arabic(P)
        glyphs += ag
    # zero-width controls
    for name, u in (
        ("uni200C", 0x200C),
        ("uni200D", 0x200D),
        ("uni200E", 0x200E),
        ("uni200F", 0x200F),
    ):
        glyphs.append(Glyph(name, u, 0))
    return glyphs, feats


def build(style, out_dir, with_arabic=True, extra_cmap=None):
    P = WEIGHTS[style]
    glyphs, arabic_fea = collect(P, with_arabic)
    order = [g.name for g in glyphs]
    assert len(order) == len(set(order)), [n for n in order if order.count(n) > 1]
    by = {g.name: g for g in glyphs}

    fb = FontBuilder(1000, isTTF=True)
    # Fixed timestamps keep identical source builds byte-for-byte reproducible.
    fb.font["head"].created = BUILD_TIMESTAMP
    fb.font["head"].modified = BUILD_TIMESTAMP
    fb.setupGlyphOrder(order)
    cmap = {}
    for g in glyphs:
        for u in g.uni:
            cmap[u] = g.name
    if extra_cmap:
        cmap.update(extra_cmap)
    fb.setupCharacterMap(cmap)

    tt = {}
    for g in glyphs:
        pen = TTGlyphPen({n: None for n in order})
        if g.components:
            for cn, dx, dy in g.components:
                pen.addComponent(cn, (1, 0, 0, 1, dx, dy))
        else:
            draw_contours(pen, g.contours)
        tt[g.name] = pen.glyph()
    fb.setupGlyf(tt)
    glyf = fb.font["glyf"]
    for name in order:
        gl = glyf[name]
        if gl.isComposite():
            for comp in gl.components:
                comp.flags |= OVERLAP_COMPOUND
                break
        elif gl.numberOfContours > 1:
            gl.flags[0] |= OVERLAP_SIMPLE

    metrics = {}
    for name in order:
        gl = glyf[name]
        gl.recalcBounds(glyf)
        lsb = gl.xMin if hasattr(gl, "xMin") and (gl.numberOfContours != 0) else 0
        metrics[name] = (by[name].adv, lsb)
    fb.setupHorizontalMetrics(metrics)

    # vertical metrics: typo metrics drive line spacing (USE_TYPO_METRICS);
    # win metrics cover the real extremes so nothing clips on Windows.
    fb.setupHorizontalHeader(
        ascent=TYPO_ASC, descent=TYPO_DESC, lineGap=TYPO_GAP
    )  # hhea = typo: same line height on every platform

    is_bold = style == "Bold"
    psname = f"{PS_FAMILY}-{style}"
    fb.setupNameTable(
        {
            "copyright": COPYRIGHT,
            "familyName": FAMILY,
            "styleName": style,
            "uniqueFontIdentifier": f"{VERSION};NONE;{psname}",
            "fullName": f"{FAMILY} {style}",
            "version": f"Version {VERSION}",
            "psName": psname,
            "manufacturer": AUTHOR,
            "designer": AUTHOR,
            "designerURL": AUTHOR_URL,
            "vendorURL": AUTHOR_URL,
            "description": "Simt Font: a Latin and Arabic typeface on the theme of direction, compass and space.",
            "sampleText": "Simt \u0633\u0645\u062a",
            "licenseDescription": LICENSE_DESCRIPTION,
            "licenseInfoURL": LICENSE_URL,
        }
    )
    fs_sel = 1 << 7  # USE_TYPO_METRICS
    fs_sel |= (1 << 5) if is_bold else (1 << 6)
    fb.setupOS2(
        version=4,
        usWeightClass=P["wght"],
        usWidthClass=5,
        fsType=0,
        achVendID="NONE",
        fsSelection=fs_sel,
        sTypoAscender=TYPO_ASC,
        sTypoDescender=TYPO_DESC,
        sTypoLineGap=TYPO_GAP,
        usWinAscent=WIN_ASC,
        usWinDescent=WIN_DESC,
        sxHeight=X,
        sCapHeight=CAP,
        usDefaultChar=0,
        usBreakChar=32,
        usMaxContext=3,
        ulUnicodeRange1=(1 << 0) | (1 << 1) | (1 << 2) | (1 << 13) | (1 << 31),
        ulUnicodeRange2=(1 << 1),  # bit 33 currency symbols
        ulCodePageRange1=(1 << 0) | (1 << 6),  # 1252 Latin 1, 1256 Arabic
        panose={
            "bFamilyType": 2,
            "bSerifStyle": 11,
            "bWeight": 6 if not is_bold else 8,
            "bProportion": 3,
            "bContrast": 2,
            "bStrokeVariation": 2,
            "bArmStyle": 2,
            "bLetterForm": 2,
            "bMidline": 2,
            "bXHeight": 4,
        },
    )
    fb.font["OS/2"].usFirstCharIndex = min(cmap)
    fb.font["OS/2"].usLastCharIndex = min(max(cmap), 0xFFFF)
    fb.setupPost(keepGlyphNames=True, underlinePosition=-120, underlineThickness=60)
    fb.font["head"].macStyle = 1 if is_bold else 0
    fb.font["head"].fontRevision = float(VERSION)
    # gasp: unhinted font -> smooth everything at all sizes
    gasp = newTable("gasp")
    gasp.gaspRange = {0xFFFF: 0x000F}
    fb.font["gasp"] = gasp
    # meta: design / supported languages
    meta = newTable("meta")
    meta.data = {"dlng": "Latn, Arab", "slng": "Latn, Arab"}
    fb.font["meta"] = meta

    fea = features(glyphs, arabic_fea)
    addOpenTypeFeaturesFromString(fb.font, fea)
    if "ltag" in fb.font:
        del fb.font["ltag"]
    os.makedirs(out_dir, exist_ok=True)
    path = os.path.join(out_dir, f"{psname}.ttf")
    fb.save(path)
    return path, fea


def features(glyphs, arabic_fea):
    names = {g.name for g in glyphs}
    kern = latin_kerning(names)
    head = """languagesystem DFLT dflt;
languagesystem latn dflt;
languagesystem arab dflt;
languagesystem arab URD;
languagesystem arab FAR;
"""
    return head + kern + latin_marks(glyphs) + arabic_fea


LATIN_MARK_ANCHORS = {
    "acutecomb": 150, "gravecomb": 150, "circumflexcomb": 140,
    "tildecomb": 120, "macroncomb": 130, "brevecomb": 150,
    "dotaccentcomb": 150, "dieresiscomb": 150, "ringcomb": 185,
    "hungarumlautcomb": 155, "caroncomb": 170,
}


def latin_marks(glyphs):
    lines = [
        f"markClass {name} <anchor 0 0> @MC_top;"
        for name in LATIN_MARK_ANCHORS
    ]
    lines += ["feature mark {", "  script latn;"]
    for g in glyphs:
        if not g.contours or not any(
            unicodedata.name(chr(u), "").startswith("LATIN ") for u in g.uni
        ):
            continue
        bb = bbox(g.contours)
        width = bb[2] - bb[0]
        x = (bb[0] + bb[2]) / 2
        if g.name in ("I", "L", "l", "r", "f"):
            x = bb[0] + width * 0.08
        elif g.name in ("J", "j"):
            x = bb[2] - width * 0.08
        lines.append(
            f"  pos base {g.name} <anchor {round(x)} {round(bb[3] + 35)}> "
            "mark @MC_top;"
        )
    lines += ["} mark;", "feature mkmk {", "  script latn;"]
    for name, top in LATIN_MARK_ANCHORS.items():
        lines.append(
            f"  pos mark {name} <anchor 0 {top}> mark @MC_top;"
        )
    lines += ["} mkmk;"]
    return "\n".join(lines) + "\n"


def latin_kerning(names):
    groups = {
        "@K_T": "T",
        "@K_V": "V W",
        "@K_Y": "Y",
        "@K_A": "A Agrave Aacute Acircumflex Atilde Adieresis Aring",
        "@K_L": "L",
        "@K_F": "F P",
        "@K_lc_round": "a c d e g o q s agrave aacute acircumflex atilde adieresis aring egrave eacute ecircumflex edieresis ograve oacute ocircumflex otilde odieresis ccedilla oe ae",
        "@K_lc_vy": "v w y",
        "@K_lc_rn": "m n p r u",
        "@K_punct": "period comma",
        "@K_O": "O C G Q",
    }
    out = []
    for k, v in groups.items():
        members = [n for n in v.split() if n in names]
        out.append(f"{k} = [{' '.join(members)}];")
    pairs = [
        ("@K_T", "@K_lc_round", -70),
        ("@K_T", "@K_lc_rn", -50),
        ("@K_T", "@K_A", -55),
        ("@K_T", "@K_punct", -80),
        ("@K_V", "@K_lc_round", -45),
        ("@K_V", "@K_A", -45),
        ("@K_V", "@K_punct", -70),
        ("@K_Y", "@K_lc_round", -70),
        ("@K_Y", "@K_A", -55),
        ("@K_Y", "@K_punct", -80),
        ("@K_A", "@K_T", -55),
        ("@K_A", "@K_V", -45),
        ("@K_A", "@K_Y", -55),
        ("@K_A", "@K_lc_vy", -25),
        ("@K_L", "@K_T", -70),
        ("@K_L", "@K_V", -60),
        ("@K_L", "@K_Y", -70),
        ("@K_F", "@K_punct", -80),
        ("@K_F", "@K_A", -35),
        ("@K_lc_vy", "@K_punct", -50),
        ("@K_O", "@K_punct", -15),
    ]
    body = "\n".join(f"  pos {a} {b} {v};" for a, b, v in pairs)
    return "\n".join(out) + f"\nfeature kern {{\n  script latn;\n{body}\n}} kern;\n"


if __name__ == "__main__":
    with_ar = "--latin-only" not in sys.argv
    ttf_dir = "fonts/ttf"
    woff_dir = "fonts/webfonts"
    os.makedirs(woff_dir, exist_ok=True)
    for style in WEIGHTS:
        p, fea = build(style, ttf_dir, with_arabic=with_ar)
        open(f"fonts/features-{style}.fea", "w").write(fea)
        print("built", p)
        from fontTools.ttLib import TTFont

        f = TTFont(p)
        f.flavor = "woff"
        woff = os.path.join(woff_dir, os.path.basename(p).replace(".ttf", ".woff"))
        f.save(woff)
        print("built", woff)
