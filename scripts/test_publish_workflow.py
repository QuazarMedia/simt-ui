"""Static invariants for the public release workflow and dependency updates."""

from pathlib import Path
import re
import subprocess
import unittest


ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = (ROOT / '.github/workflows/publish.yml').read_text()
DEPENDABOT = (ROOT / '.github/dependabot.yml').read_text()


class PublishWorkflowTests(unittest.TestCase):
    def test_every_action_is_pinned_to_an_immutable_commit(self):
        actions = re.findall(r'^\s*- uses:\s*(\S+)', WORKFLOW, re.MULTILINE)
        self.assertTrue(actions)
        for action in actions:
            with self.subTest(action=action):
                self.assertRegex(action, r'@[0-9a-f]{40}$')

    def test_both_release_units_have_exact_package_guards(self):
        self.assertIn('package="app.simt:app-ui"', WORKFLOW)
        self.assertIn('package="@simt-dev/web-ui"', WORKFLOW)
        self.assertIn("manifest.name !== '@simt-dev/web-ui'", WORKFLOW)
        self.assertIn('manifest.version !== version', WORKFLOW)
        self.assertIn('does not match VERSION_NAME=', WORKFLOW)

    def test_web_publish_uses_only_workflow_bound_oidc(self):
        web_job = WORKFLOW.split('\n  web_ui:\n', 1)[1].split('\n  release:\n', 1)[0]
        self.assertIn('id-token: write', web_job)
        self.assertIn('environment: release', web_job)
        self.assertIn('npm publish --provenance --access public', web_job)
        self.assertNotIn('NPM_TOKEN', web_job)
        self.assertNotIn('NODE_AUTH_TOKEN', web_job)
        self.assertIn('scripts/check_publication.py source --root .', web_job)
        self.assertIn('npm --prefix web-ui run check', web_job)

    def test_web_publish_installs_a_supported_npm_cli(self):
        web_job = WORKFLOW.split('\n  web_ui:\n', 1)[1].split('\n  release:\n', 1)[0]
        self.assertIn("node-version: '24'", web_job)
        self.assertIn('npm install --global npm@11.21.3', web_job)

    def test_current_library_release_notes_are_extractable(self):
        releases = (
            ('app-ui', '0.1.0', 'Maven Central publication'),
            ('web-ui', '0.1.0', 'Published `@simt-dev/web-ui`'),
            ('web-ui', '0.1.1', 'Reusable marquee and finale sections'),
        )
        for library, version, marker in releases:
            with self.subTest(library=library, version=version):
                result = subprocess.run(
                    ['awk', '-v', f'lib={library}', '-v', f'v={version}',
                     '-f', 'scripts/release-notes.awk', 'CHANGELOG.md'],
                    cwd=ROOT, capture_output=True, text=True, check=True,
                )
                self.assertIn(marker, result.stdout)
                self.assertNotIn('\n## ', result.stdout)

    def test_registry_credentials_are_scoped_to_the_maven_publish_step(self):
        self.assertNotIn('Check release secrets', WORKFLOW)
        self.assertEqual(WORKFLOW.count('secrets.'), 4)
        publish_step = WORKFLOW.split('- name: Publish to Maven Central', 1)[1].split('\n  web_ui:', 1)[0]
        for name in ('CENTRAL_PORTAL_USERNAME', 'CENTRAL_PORTAL_PASSWORD', 'SIGNING_KEY', 'SIGNING_PASSWORD'):
            self.assertIn('secrets.' + name, publish_step)

    def test_publication_checks_are_retry_safe(self):
        self.assertIn('Check Maven Central publication', WORKFLOW)
        self.assertIn('Check npm publication', WORKFLOW)
        self.assertIn("steps.central.outputs.published != 'true'", WORKFLOW)
        self.assertIn("steps.npm.outputs.published != 'true'", WORKFLOW)

    def test_release_notes_are_checked_before_publication(self):
        resolve_job = WORKFLOW.split('\n  resolve:\n', 1)[1].split('\n  app_ui:\n', 1)[0]
        self.assertIn('scripts/release-notes.awk', resolve_job)
        self.assertIn('Missing CHANGELOG.md section', resolve_job)

    def test_github_release_requires_notes_and_updates_existing_releases(self):
        release_job = WORKFLOW.split('\n  release:\n', 1)[1]
        self.assertIn('needs: [resolve, app_ui, web_ui]', release_job)
        self.assertIn('Missing CHANGELOG.md section', release_job)
        self.assertIn('gh release view', release_job)
        self.assertIn('gh release edit', release_job)
        self.assertIn('gh release create', release_job)
        self.assertIn('--verify-tag', release_job)

    def test_each_npm_workspace_has_dependency_updates(self):
        for directory in ('/web-ui', '/design-spec', '/design-site'):
            self.assertIn(f'package-ecosystem: "npm"\n    directory: "{directory}"', DEPENDABOT)


if __name__ == '__main__':
    unittest.main()
