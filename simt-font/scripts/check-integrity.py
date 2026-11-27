#!/usr/bin/env python3
"""Offline integrity check for the Simt Font module.

Verifies, using only the Python 3 standard library:
  1. SHA-256 of every font binary against checksums.sha256.
  2. Each font's name table: family, style, PS name, and the license /
     license-URL entries that declare the SIL Open Font License 1.1.
  3. The license and notice files are present and intact.

Exit status is 0 only when every check passes.
"""

import hashlib
import re
import struct
import sys
import zlib
from pathlib import Path

MODULE = Path(__file__).resolve().parent.parent

FAMILY = "Simt Font"
AUTHOR = "Said Elimam"
AUTHOR_URL = "https://saidelimam.com"
VERSION = "1.000"
COPYRIGHT = (
    "Copyright 2026 The Simt Font Project Authors "
    "(https://github.com/QuazarMedia/simt-ui); Said Elimam"
)
EXPECTED_STYLE = {
    "fonts/ttf/SimtFont-Regular.ttf": ("Regular", "SimtFont-Regular"),
    "fonts/ttf/SimtFont-Bold.ttf": ("Bold", "SimtFont-Bold"),
    "fonts/webfonts/SimtFont-Regular.woff": ("Regular", "SimtFont-Regular"),
    "fonts/webfonts/SimtFont-Bold.woff": ("Bold", "SimtFont-Bold"),
}
LICENSE_TEXT = (
    "This Font Software is licensed under the SIL Open Font License, Version 1.1. "
    "This license is available with a FAQ at: https://openfontlicense.org"
)
LICENSE_URL = "https://openfontlicense.org"

sys.path.insert(0, str(MODULE / "sources"))
from latin import LATIN_CORE_CODEPOINTS

failures = []


def check(label, ok, detail=""):
    status = "ok" if ok else "FAIL"
    print(f"  [{status}] {label}" + (f" — {detail}" if detail else ""))
    if not ok:
        failures.append(label)


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def read_tables(data):
    if data[:4] == b"wOFF":
        num_tables = struct.unpack_from(">H", data, 12)[0]
        tables = {}
        for i in range(num_tables):
            off = 44 + 20 * i
            tag, offset, comp_len, orig_len, _orig_sum = struct.unpack_from(
                ">4sIIII", data, off
            )
            if comp_len == 0:
                blob = data[offset : offset + orig_len]
            elif comp_len != orig_len:
                blob = zlib.decompress(data[offset : offset + comp_len])
            else:
                blob = data[offset : offset + comp_len]
            tables[tag] = blob
        return tables

    if data[:4] not in (b"\x00\x01\x00\x00", b"true"):
        raise ValueError("not a TrueType font (bad sfnt signature)")
    num_tables = struct.unpack_from(">H", data, 6)[0]
    tables = {}
    for i in range(num_tables):
        off = 12 + 16 * i
        tag, _sum, offset, length = struct.unpack_from(">4sIII", data, off)
        tables[tag] = data[offset : offset + length]
    return tables


def read_cmap(data):
    raw = read_tables(data)[b"cmap"]
    _version, num_tables = struct.unpack_from(">HH", raw, 0)
    codepoints = set()
    for i in range(num_tables):
        platform, encoding, offset = struct.unpack_from(">HHI", raw, 4 + 8 * i)
        if platform == 0 or (platform == 3 and encoding in (1, 10)):
            codepoints.update(_cmap_encoding_codepoints(raw[offset:]))
    return codepoints


def _cmap_encoding_codepoints(raw):
    fmt = struct.unpack_from(">H", raw, 0)[0]
    codepoints = set()
    if fmt == 4:
        seg_count = struct.unpack_from(">H", raw, 6)[0] // 2
        end_codes = struct.unpack_from(f">{seg_count}H", raw, 14)
        start_codes = struct.unpack_from(f">{seg_count}H", raw, 16 + 2 * seg_count)
        deltas = struct.unpack_from(
            f">{seg_count}H", raw, 16 + 4 * seg_count
        )
        range_base = 16 + 6 * seg_count
        ranges = struct.unpack_from(f">{seg_count}H", raw, range_base)
        for i, (start, end, delta, range_offset) in enumerate(
            zip(start_codes, end_codes, deltas, ranges)
        ):
            for cp in range(start, min(end, 0xFFFE) + 1):
                if range_offset == 0:
                    glyph_id = (cp + delta) & 0xFFFF
                else:
                    glyph_offset = (
                        range_base + 2 * i + range_offset + 2 * (cp - start)
                    )
                    glyph_id = struct.unpack_from(">H", raw, glyph_offset)[0]
                    if glyph_id:
                        glyph_id = (glyph_id + delta) & 0xFFFF
                if glyph_id:
                    codepoints.add(cp)
    elif fmt == 12:
        group_count = struct.unpack_from(">I", raw, 12)[0]
        for i in range(group_count):
            start, end, glyph_id = struct.unpack_from(">III", raw, 16 + 12 * i)
            if glyph_id:
                codepoints.update(range(start, end + 1))
    return codepoints


def read_name_table(data):
    """Return {nameID: text} from raw sfnt or WOFF bytes, decoded per platform."""
    raw = read_tables(data)[b"name"]
    count, string_off = struct.unpack_from(">HH", raw, 2)
    out = {}
    for i in range(count):
        (plat, enc, lang, name_id, length, offset) = struct.unpack_from(
            ">HHHHHH", raw, 6 + 12 * i
        )
        s = raw[string_off + offset : string_off + offset + length]
        if (plat, enc) == (3, 10000):
            text = s.decode("utf-16-be")
        elif (plat, enc) == (1, 0):
            text = s.decode("latin-1")
        else:
            continue
        # Prefer Windows Unicode records; first record wins per nameID.
        out.setdefault(name_id, text)
    return out


def main():
    print("Simt Font integrity check")

    manifest = MODULE / "checksums.sha256"
    if not manifest.is_file():
        check("checksums.sha256 present", False)
    else:
        print(f"  [ok] checksums.sha256 present")
    entries = {}
    if manifest.is_file():
        for line in manifest.read_text().splitlines():
            line = line.strip()
            if line and not line.startswith("#"):
                digest, rel = line.split(None, 1)
                entries[rel.strip()] = digest

    print("Font binaries (SHA-256 + name table)")
    for rel, (style, psname) in EXPECTED_STYLE.items():
        path = MODULE / rel
        if not path.is_file():
            check(rel, False, "missing")
            continue
        digest = sha256(path)
        check(
            f"{rel} checksum",
            entries.get(rel) == digest,
            f"{digest[:12]}… expected {entries.get(rel, 'unlisted')[:12]}…",
        )
        data = path.read_bytes()
        try:
            names = read_name_table(data)
        except Exception as e:  # noqa: BLE001 - report any parse failure
            check(f"{rel} name table parses", False, str(e))
            continue
        check(f"{rel} name table parses", True)
        try:
            codepoints = read_cmap(data)
        except Exception as e:  # noqa: BLE001 - report any parse failure
            check(f"{rel} cmap parses", False, str(e))
            continue
        check(f"{rel} cmap parses", True)
        missing_core = LATIN_CORE_CODEPOINTS - codepoints
        check(
            f"{rel} GF Latin Core coverage",
            not missing_core,
            f"{len(LATIN_CORE_CODEPOINTS) - len(missing_core)}/{len(LATIN_CORE_CODEPOINTS)}"
            + (
                "; missing " + ", ".join(f"U+{cp:04X}" for cp in sorted(missing_core))
                if missing_core
                else ""
            ),
        )
        check(f"{rel} U+0331 combining macron below", 0x0331 in codepoints)
        family = names.get(1) or names.get(16)
        check(f"{rel} family", family == FAMILY, f"got {family!r}")
        got_style = names.get(2) or names.get(17)
        check(f"{rel} style", got_style == style, f"got {got_style!r}")
        check(f"{rel} PS name", names.get(6) == psname, f"got {names.get(6)!r}")
        check(f"{rel} version", names.get(5) == f"Version {VERSION}", f"got {names.get(5)!r}")
        check(f"{rel} copyright", names.get(0) == COPYRIGHT, f"got {names.get(0)!r}")
        check(f"{rel} manufacturer", names.get(8) == AUTHOR, f"got {names.get(8)!r}")
        check(f"{rel} designer", names.get(9) == AUTHOR, f"got {names.get(9)!r}")
        check(f"{rel} vendor URL", names.get(11) == AUTHOR_URL, f"got {names.get(11)!r}")
        check(f"{rel} designer URL", names.get(12) == AUTHOR_URL, f"got {names.get(12)!r}")
        license_text = names.get(13, "")
        check(
            f"{rel} license (ID 13)",
            LICENSE_TEXT in license_text,
            f"got {license_text!r}",
        )
        check(
            f"{rel} license URL (ID 14)",
            names.get(14) == LICENSE_URL,
            f"got {names.get(14)!r}",
        )

    print("License and notice files")
    ofl = MODULE / "OFL.txt"
    if ofl.is_file():
        text = ofl.read_text()
        check(
            "OFL.txt: OFL 1.1 text",
            "SIL OPEN FONT LICENSE Version 1.1 - 26 February 2007" in text,
        )
        check(
            "OFL.txt: copyright line",
            re.search(
                r"Copyright \d{4} The Simt Font Project Authors "
                r"\(https://github\.com/QuazarMedia/simt-ui\); Said Elimam",
                text,
            )
            is not None,
        )
    else:
        check("OFL.txt present", False, "missing")

    for name in ("NOTICE", "README.md"):
        p = MODULE / name
        ok = p.is_file() and "SIL Open Font License" in p.read_text()
        check(f"{name} present and references the OFL", ok)

    print()
    if failures:
        print(f"FAILED: {len(failures)} check(s): " + "; ".join(failures))
        return 1
    print("PASS: all integrity checks passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
