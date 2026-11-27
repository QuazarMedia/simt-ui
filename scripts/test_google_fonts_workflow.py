"""Static and packaging invariants for the Google Fonts contribution workflow."""

from pathlib import Path
import re
import subprocess
import sys
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = (ROOT / ".github/workflows/google-fonts.yml").read_text()
PACKAGER = ROOT / "simt-font/scripts/package_google_fonts.py"
COMMIT = "0123456789abcdef0123456789abcdef01234567"
DATE_ADDED = "2026-11-27"


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
        self.assertIn("steps.qa.outcome == 'failure'", WORKFLOW)

    def test_submission_is_opt_in_and_isolated_from_packaging(self):
        package_job, submit_job = WORKFLOW.split("\n  submit:\n", 1)
        self.assertNotIn("secrets.", package_job)
        self.assertIn("needs: package", submit_job)
        self.assertIn("if: ${{ inputs.submit && github.ref == 'refs/heads/main' }}", submit_job)
        self.assertIn("environment: google-fonts", submit_job)
        self.assertIn("secrets.GOOGLE_FONTS_TOKEN", submit_job)

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
        source = (ROOT / "simt-font/source/build.py").read_text()
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
            self.assertEqual(output.read_text(), "version=0.200\n")

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


if __name__ == "__main__":
    unittest.main()
