"""Static and packaging invariants for the Google Fonts contribution workflow."""

from pathlib import Path
import ast
import json
import re
import subprocess
import sys
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = (ROOT / ".github/workflows/google-fonts.yml").read_text()
PACKAGER = ROOT / "simt-font/scripts/package_google_fonts.py"
REPORT_GATE = ROOT / "simt-font/scripts/check_fontspector_report.py"
FONT_SOURCES = ROOT / "simt-font/sources"
sys.path.insert(0, str(FONT_SOURCES))
import latin


def constant_value(node):
    if isinstance(node, ast.Constant):
        return node.value
    if isinstance(node, ast.Dict):
        return {
            constant_value(key): constant_value(value)
            for key, value in zip(node.keys, node.values)
        }
    if (
        isinstance(node, ast.Call)
        and isinstance(node.func, ast.Name)
        and node.func.id == "dict"
        and len(node.args) % 2 == 0
        and all(keyword.arg is not None for keyword in node.keywords)
    ):
        pairs = [
            (node.args[i], node.args[i + 1])
            for i in range(0, len(node.args), 2)
        ]
        pairs += [(ast.Constant(keyword.arg), keyword.value) for keyword in node.keywords]
        return {constant_value(key): constant_value(value) for key, value in pairs}
    raise ValueError(f"unsupported constant expression: {ast.dump(node)}")


BUILD_TREE = ast.parse((FONT_SOURCES / "build.py").read_text())
WEIGHTS_NODE = next(
    node
    for node in BUILD_TREE.body
    if isinstance(node, ast.Assign)
    and any(isinstance(target, ast.Name) and target.id == "WEIGHTS" for target in node.targets)
)
WEIGHTS = constant_value(WEIGHTS_NODE.value)
COMMIT = "0123456789abcdef0123456789abcdef01234567"
DATE_ADDED = "2026-11-27"


class LatinCoreSourceTests(unittest.TestCase):
    def test_every_weight_covers_the_official_core_without_duplicate_names(self):
        self.assertEqual(len(latin.LATIN_CORE_CODEPOINTS), 319)
        self.assertTrue(set(range(0x20, 0x7F)).issubset(latin.LATIN_CORE_CODEPOINTS))
        for style, params in WEIGHTS.items():
            with self.subTest(style=style):
                glyphs = latin.build_latin(params)
                names = [glyph.name for glyph in glyphs]
                self.assertEqual(len(names), len(set(names)))
                covered = {cp for glyph in glyphs for cp in glyph.uni}
                self.assertEqual(latin.LATIN_CORE_CODEPOINTS - covered, set())

    def test_core_letters_have_their_simple_case_counterparts(self):
        for style, params in WEIGHTS.items():
            with self.subTest(style=style):
                covered = {
                    cp for glyph in latin.build_latin(params) for cp in glyph.uni
                }
                for cp in latin.LATIN_CORE_CODEPOINTS:
                    char = chr(cp)
                    if not char.isalpha():
                        continue
                    for counterpart in (char.lower(), char.upper()):
                        if len(counterpart) == 1:
                            self.assertIn(ord(counterpart), covered)

    def test_core_glyphs_have_outlines_unless_they_are_spaces_or_marks(self):
        import unicodedata

        for style, params in WEIGHTS.items():
            with self.subTest(style=style):
                for glyph in latin.build_latin(params):
                    core = set(glyph.uni) & latin.LATIN_CORE_CODEPOINTS
                    if not core:
                        continue
                    char = chr(min(core))
                    if char.isspace() or unicodedata.category(char).startswith("M"):
                        continue
                    self.assertTrue(glyph.contours or glyph.components, glyph.name)


class GoogleFontsWorkflowTests(unittest.TestCase):
    def test_workflow_is_manual_and_read_only_by_default(self):
        self.assertIn("workflow_dispatch:", WORKFLOW)
        self.assertNotIn("\n  push:\n", WORKFLOW)
        self.assertNotIn("\n  pull_request:\n", WORKFLOW)
        self.assertIn("permissions:\n  contents: read", WORKFLOW)

    def test_every_action_is_pinned_to_an_immutable_commit(self):
        actions = re.findall(r"^\s*- uses:\s*(\S+)", WORKFLOW, re.MULTILINE)
        self.assertTrue(actions)
        for action in actions:
            with self.subTest(action=action):
                self.assertRegex(action, r"@[0-9a-f]{40}$")

    def test_qa_uses_the_current_google_fonts_profile_and_pinned_binary(self):
        self.assertIn("FONTSPECTOR_VERSION: '1.9.1'", WORKFLOW)
        self.assertIn("FONTSPECTOR_SHA256:", WORKFLOW)
        self.assertIn("shasum -a 256 --check", WORKFLOW)
        self.assertIn("--profile googlefonts", WORKFLOW)
        self.assertIn('"$package_dir"/ofl/simtfont/OFL.txt', WORKFLOW)
        self.assertIn('"$package_dir"/ofl/simtfont/METADATA.pb', WORKFLOW)
        self.assertNotIn("fontbakery", WORKFLOW.lower())

    def test_qa_artifacts_are_uploaded_before_the_gate(self):
        upload = WORKFLOW.index("- name: Upload package and QA report")
        enforce = WORKFLOW.index("- name: Enforce Google Fonts QA")
        self.assertLess(upload, enforce)
        self.assertIn("continue-on-error: true", WORKFLOW)
        self.assertIn("--json \"$RUNNER_TEMP/fontspector-report.json\"", WORKFLOW)
        self.assertIn("${{ runner.temp }}/fontspector-report.json", WORKFLOW)
        self.assertIn("id: qa_gate", WORKFLOW)
        self.assertIn("check_fontspector_report.py", WORKFLOW)
        self.assertIn("steps.qa.outcome != 'skipped'", WORKFLOW)

    def test_stock_profile_runs_without_exclusions_or_severity_overrides(self):
        self.assertIn("--profile googlefonts", WORKFLOW)
        self.assertNotIn("--configuration", WORKFLOW)
        self.assertNotIn("--exclude-checkid", WORKFLOW)
        self.assertNotIn("--checkid", WORKFLOW)

    def test_submission_is_opt_in_and_isolated_from_packaging(self):
        package_job, submit_job = WORKFLOW.split("\n  submit:\n", 1)
        self.assertNotIn("secrets.", package_job)
        self.assertIn("needs: package", submit_job)
        self.assertIn("if: ${{ inputs.submit && github.ref == 'refs/heads/main' }}", submit_job)
        self.assertIn("environment: google-fonts", submit_job)
        self.assertIn("secrets.GOOGLE_FONTS_TOKEN", submit_job)
        self.assertIn("KNOWN_TOFU_FAILURES: ${{ needs.package.outputs.known_tofu_failures }}", submit_job)
        self.assertIn("https://github.com/fonttools/fontspector/issues/946", submit_job)

    def test_submission_requires_an_issue_and_never_force_pushes(self):
        submit_job = WORKFLOW.split("\n  submit:\n", 1)[1]
        self.assertIn("gh issue view", submit_job)
        self.assertIn("google/fonts", submit_job)
        self.assertIn("gh pr create", submit_job)
        self.assertIn("gh pr edit", submit_job)
        self.assertIn('^[A-Za-z0-9][A-Za-z0-9-]*/[A-Za-z0-9_.-]+$', submit_job)
        self.assertIn('fork_owner="${fork%%/*}"', submit_job)
        self.assertIn('--head "$fork_owner:simtfont"', submit_job)
        self.assertNotIn('--head "$fork:simtfont"', submit_job)
        self.assertNotIn("--force", submit_job)
        self.assertNotIn("pull-requests: write", WORKFLOW)

    def test_build_uses_valid_fonttools_license_keys(self):
        source = (ROOT / "simt-font/sources/build.py").read_text()
        self.assertIn('"licenseDescription":', source)
        self.assertIn('"licenseInfoURL":', source)
        self.assertNotIn('"license":', source)
        self.assertNotIn('"licenseURL":', source)

    def test_package_has_exact_google_fonts_layout(self):
        with tempfile.TemporaryDirectory() as temporary:
            destination = Path(temporary) / "package"
            output = Path(temporary) / "github-output"
            result = subprocess.run(
                [
                    sys.executable,
                    str(PACKAGER),
                    str(destination),
                    "--commit",
                    COMMIT,
                    "--date-added",
                    DATE_ADDED,
                    "--github-output",
                    str(output),
                ],
                capture_output=True,
                text=True,
            )
            self.assertEqual(result.returncode, 0, result.stderr)
            family = destination / "ofl/simtfont"
            actual = {
                str(path.relative_to(destination))
                for path in destination.rglob("*")
                if path.is_file()
            }
            self.assertEqual(
                actual,
                {
                    "ofl/simtfont/SimtFont-Regular.ttf",
                    "ofl/simtfont/SimtFont-Bold.ttf",
                    "ofl/simtfont/OFL.txt",
                    "ofl/simtfont/METADATA.pb",
                    "ofl/simtfont/article/ARTICLE.en_us.html",
                },
            )
            for name in ("SimtFont-Regular.ttf", "SimtFont-Bold.ttf"):
                self.assertEqual(
                    (family / name).read_bytes(),
                    (ROOT / "simt-font/fonts/ttf" / name).read_bytes(),
                )
            metadata = (family / "METADATA.pb").read_text()
            self.assertIn(f'commit: "{COMMIT}"', metadata)
            self.assertIn(f'date_added: "{DATE_ADDED}"', metadata)
            self.assertNotIn("{{", metadata)
            self.assertIn('designer: "Said Elimam"', metadata)
            self.assertIn(
                'copyright: "Copyright 2026 The Simt Font Project Authors '
                '(https://github.com/QuazarMedia/simt-ui); Said Elimam"',
                metadata,
            )
            self.assertEqual(output.read_text(), "version=1.000\n")

    def test_workflow_packages_fresh_source_builds(self):
        self.assertEqual(
            WORKFLOW.count('--font-dir "$RUNNER_TEMP/simt-font-sources/fonts/ttf"'),
            2,
        )
        self.assertEqual(WORKFLOW.count("- name: Build font sources"), 2)

    def test_packager_accepts_a_fresh_build_directory(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            fonts = root / "fonts"
            fonts.mkdir()
            expected = {}
            for name in ("SimtFont-Regular.ttf", "SimtFont-Bold.ttf"):
                expected[name] = name.encode()
                (fonts / name).write_bytes(expected[name])
            destination = root / "package"
            result = subprocess.run(
                [
                    sys.executable,
                    str(PACKAGER),
                    str(destination),
                    "--font-dir",
                    str(fonts),
                    "--commit",
                    COMMIT,
                    "--date-added",
                    DATE_ADDED,
                ],
                capture_output=True,
                text=True,
            )
            self.assertEqual(result.returncode, 0, result.stderr)
            family = destination / "ofl/simtfont"
            for name, data in expected.items():
                self.assertEqual((family / name).read_bytes(), data)

    def test_packager_rejects_invalid_provenance(self):
        cases = (
            ("not-a-sha", DATE_ADDED),
            (COMMIT, "27/11/2026"),
        )
        for commit, date_added in cases:
            with self.subTest(commit=commit, date_added=date_added):
                with tempfile.TemporaryDirectory() as temporary:
                    result = subprocess.run(
                        [
                            sys.executable,
                            str(PACKAGER),
                            str(Path(temporary) / "package"),
                            "--commit",
                            commit,
                            "--date-added",
                            date_added,
                        ],
                        capture_output=True,
                        text=True,
                    )
                    self.assertNotEqual(result.returncode, 0)
                    self.assertIn("error:", result.stderr)


class FontspectorReportGateTests(unittest.TestCase):
    def run_gate(self, report):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            report_path = root / "report.json"
            output_path = root / "github-output"
            report_path.write_text(json.dumps(report))
            result = subprocess.run(
                [
                    sys.executable,
                    str(REPORT_GATE),
                    str(report_path),
                    "--github-output",
                    str(output_path),
                ],
                capture_output=True,
                text=True,
            )
            output = output_path.read_text() if output_path.exists() else ""
            return result, output

    def make_report(self, subresults, check_id="googlefonts/tofu"):
        return {
            "results": {
                "SimtFont-Regular.ttf": {
                    "Google Fonts Checks": [
                        {"check_id": check_id, "subresults": subresults}
                    ]
                }
            }
        }

    def known_result(self, language, severity="FAIL"):
        return {
            "severity": severity,
            "code": "missing-subsetted",
            "message": (
                "The font has codepoints needed to render the specimen 16 sample "
                f"text for language {language}, but although the font contained "
                "all the base exemplars, tofu will still be produced because the "
                "codepoints do not appear in any of the subsets latin:\n\n"
                "* ̱ (U+0331)"
            ),
        }

    def test_accepts_the_five_documented_u0331_results(self):
        languages = ("Amarakaeri", "Chickasaw", "Otomi, Mezquital", "Secoya", "Siona")
        result, output = self.run_gate(
            self.make_report([self.known_result(language) for language in languages])
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("accepted 5 known U+0331", result.stdout)
        self.assertEqual(output, "known_tofu_failures=5\n")

    def test_rejects_every_unreviewed_failure(self):
        cases = (
            {"severity": "BROKEN"},
            self.known_result("Ticuna"),
            self.known_result("Siona", "FATAL"),
            self.known_result("Siona", "ERROR"),
            {**self.known_result("Siona"), "code": "missing-codepoints"},
            {**self.known_result("Siona"), "message": self.known_result("Siona")["message"].replace("U+0331", "U+0330")},
            {**self.known_result("Siona"), "message": self.known_result("Siona")["message"] + "\n* U+0330"},
        )
        for subresult in cases:
            with self.subTest(subresult=subresult):
                result, output = self.run_gate(self.make_report([subresult]))
                self.assertNotEqual(result.returncode, 0)
                self.assertEqual(output, "")
        result, _ = self.run_gate(
            self.make_report([self.known_result("Siona")], "opentype/example")
        )
        self.assertNotEqual(result.returncode, 0)

    def test_rejects_duplicate_known_language_results(self):
        result, _ = self.run_gate(
            self.make_report([self.known_result("Siona"), self.known_result("Siona")])
        )
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Duplicate known-language failures", result.stderr)

    def test_requires_the_tofu_check_to_run(self):
        result, _ = self.run_gate(
            self.make_report([{"severity": "PASS"}], "googlefonts/metadata/family_name")
        )
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("tofu check did not run", result.stderr)

    def test_rejects_missing_or_malformed_reports(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            missing = root / "missing.json"
            malformed = root / "malformed.json"
            invalid_root = root / "invalid-root.json"
            malformed.write_text("not json")
            invalid_root.write_text("[]")
            for report in (missing, malformed, invalid_root):
                with self.subTest(report=report):
                    result = subprocess.run(
                        [sys.executable, str(REPORT_GATE), str(report)],
                        capture_output=True,
                        text=True,
                    )
                    self.assertNotEqual(result.returncode, 0)
                    self.assertIn("Cannot validate Fontspector report", result.stderr)


if __name__ == "__main__":
    unittest.main()
