#!/usr/bin/env python3
"""Reject Google Fonts QA failures except the known U+0331 subset-data bug."""

import argparse
import json
import re
import sys
from pathlib import Path


KNOWN_LANGUAGES = {
    "Amarakaeri",
    "Chickasaw",
    "Otomi, Mezquital",
    "Secoya",
    "Siona",
}
KNOWN_ISSUE = "https://github.com/fonttools/fontspector/issues/946"
LANGUAGE_RE = re.compile(r"sample text for language (.+?), but although")
CODEPOINT_RE = re.compile(r"\bU\+([0-9A-F]{4,6})\b")


class ReportError(ValueError):
    pass


def subresults(report):
    results = report.get("results")
    if not isinstance(results, dict) or not results:
        raise ReportError("report has no results")
    for sections in results.values():
        if not isinstance(sections, dict):
            raise ReportError("report results contain an invalid section map")
        for checks in sections.values():
            if not isinstance(checks, list):
                raise ReportError("report section does not contain a check list")
            for check in checks:
                if not isinstance(check, dict) or not isinstance(check.get("subresults"), list):
                    raise ReportError("report contains an invalid check result")
                yield check


def validate(report):
    if not isinstance(report, dict):
        raise ReportError("report root is not an object")
    failures = []
    known = []
    tofu_ran = False
    for check in subresults(report):
        check_id = check.get("check_id")
        for result in check["subresults"]:
            if not isinstance(result, dict):
                raise ReportError("report contains an invalid subresult")
            severity = result.get("severity")
            if severity not in (
                "SKIP", "PASS", "INFO", "WARN", "FAIL", "FATAL", "ERROR"
            ):
                raise ReportError("report contains an invalid severity")
            if check_id == "googlefonts/tofu" and severity != "SKIP":
                tofu_ran = True
            if severity not in ("FAIL", "FATAL", "ERROR"):
                continue
            message = result.get("message", "")
            if not isinstance(message, str):
                raise ReportError("failure message is not a string")
            language = LANGUAGE_RE.search(message)
            codepoints = set(CODEPOINT_RE.findall(message))
            is_known = (
                severity == "FAIL"
                and check_id == "googlefonts/tofu"
                and result.get("code") == "missing-subsetted"
                and language is not None
                and language.group(1) in KNOWN_LANGUAGES
                and codepoints == {"0331"}
            )
            if is_known:
                known.append((check.get("filename", ""), language.group(1), message))
            else:
                failures.append((check_id, severity, result.get("code"), message))

    if not tofu_ran:
        failures.append(("googlefonts/tofu", "ERROR", "not-run", "The tofu check did not run"))
    languages = [language for _, language, _ in known]
    if len(languages) != len(set(languages)):
        failures.append(("googlefonts/tofu", "FAIL", "missing-subsetted", "Duplicate known-language failures require review"))
    return known, failures


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("report", type=Path)
    parser.add_argument("--github-output", type=Path)
    args = parser.parse_args()

    try:
        report = json.loads(args.report.read_text())
        known, failures = validate(report)
    except (OSError, json.JSONDecodeError, ReportError) as error:
        print(f"::error::Cannot validate Fontspector report: {error}", file=sys.stderr)
        return 1

    for filename, language, message in known:
        print(
            f"::warning::Known U+0331 subset-data failure for {language} "
            f"({filename}): {KNOWN_ISSUE}"
        )
    if failures:
        for check_id, severity, code, message in failures:
            print(
                f"::error::{severity} {check_id}/{code}: {message}",
                file=sys.stderr,
            )
        print(
            "::error::Google Fonts QA has failures outside the documented U+0331 subset-data issue.",
            file=sys.stderr,
        )
        return 1

    if args.github_output:
        with args.github_output.open("a") as output:
            output.write(f"known_tofu_failures={len(known)}\n")
    print(
        f"Google Fonts QA gate passed; accepted {len(known)} known U+0331 "
        f"subset-data failure(s) and no other FAIL/FATAL/ERROR results."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
