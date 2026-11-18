"""Targeted stdlib fixtures; all secrets assembled, never literal repository leaks."""
import contextlib
import copy
import hashlib
import io
import json
import os
from pathlib import Path
import stat
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import zipfile

import check_publication as audit


HEAD = 'a' * 40
PROJECT_ROOT = Path(audit.__file__).resolve().parents[1]
LEGAL_DOCUMENTS = {p: (PROJECT_ROOT / p).read_bytes() for p in
                   ('LICENSE', 'LICENSE-DOCS', 'NOTICE', 'app-ui/source-provenance.json')}
POM_LICENSE_XML = ('<licenses><license><name>Apache License, Version 2.0</name>'
                   '<url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>'
                   '<distribution>repo</distribution></license></licenses>')


def digest(data):
    return hashlib.sha256(data).hexdigest()


def encoded(value):
    return json.dumps(value, sort_keys=True).encode()


def archive(entries, compression=zipfile.ZIP_STORED):
    result = io.BytesIO()
    with zipfile.ZipFile(result, 'w', compression=compression) as stream:
        for name, data in sorted(entries.items()):
            info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
            info.create_system = 3
            info.external_attr = (stat.S_IFREG | 0o644) << 16
            info.compress_type = compression
            stream.writestr(info, data)
    return result.getvalue()


def secret():
    return 'gh' + 'p_' + 'sensitive' * 5


class Fixture:
    def __init__(self, base, version='1.2.3-SNAPSHOT'):
        self.root = Path(base).resolve()
        self.repo = self.root / 'build' / 'maven-repository'
        self.version = version
        self.actual = version.replace('SNAPSHOT', '20261117.123456-1')
        self.modules = {}
        self.paths = {}
        self.legal_entries = dict(LEGAL_DOCUMENTS)
        self.legal_zip = archive(self.legal_entries)
        for relative, data in self.legal_entries.items():
            self.put(relative, data)
        for target in audit.TARGETS:
            for key in audit.required_keys(target):
                self.paths[target, key] = 'app/simt/' + target + '/' + version + '/' + audit.logical_name(target, self.actual, key)
        for target in audit.TARGETS:
            attrs = {'org.jetbrains.kotlin.platform.type': 'common'}
            if target == 'app-ui-android':
                attrs = {'org.jetbrains.kotlin.platform.type': 'androidJvm'}
            elif target.startswith('app-ui-ios'):
                attrs = {'org.jetbrains.kotlin.platform.type': 'native',
                         'org.jetbrains.kotlin.native.target': 'ios_arm64' if target == 'app-ui-iosarm64' else 'ios_simulator_arm64'}
            variants = []
            for key in sorted(audit.required_keys(target) - {('', 'pom'), ('', 'module')}):
                classifier, extension = key
                if classifier == 'legal-notices':
                    self.write(target, key, self.legal_zip)
                    continue  # Required Maven attachment, deliberately no Gradle variant.
                if classifier == 'sources':
                    data = archive({'commonMain/app/simt/ui/Example.kt': 'package app.simt.ui\nclass Example'})
                elif classifier == 'javadoc':
                    data = archive({'index.html': '<html><body>Dokka documentation for app.simt.ui</body></html>',
                                    'app-ui/app.simt.ui/-example/index.html': '<html><body><h1>Example</h1>app.simt.ui API</body></html>'})
                elif extension == 'json':
                    data = encoded({'schemaVersion': '1.0', 'buildSystem': 'Gradle'})
                elif extension == 'aar':
                    data = archive({'AndroidManifest.xml': '<manifest package="app.simt.ui"/>',
                                    'classes.jar': archive({'app/simt/ui/Example.class': b'compiled-code'})})
                elif extension == 'klib':
                    data = archive({'default/manifest': 'compiler_version=2.4.21', 'default/ir/strings.knt': b'Example'})
                elif extension == 'kotlin_resources.zip':
                    data = archive({'composeResources/app.simt.ui/values/strings.xml': '<resources><string name="clear">Clear</string></resources>'})
                else:
                    data = archive({'META-INF/module.kotlin_metadata': b'metadata'})
                self.write(target, key, data)
                item = {'name': audit.logical_name(target, version, key), 'url': audit.logical_name(target, version, key),
                        'size': len(data), 'sha256': digest(data)}
                variants.append({'name': target + '-' + (classifier or extension), 'attributes': dict(attrs), 'files': [item],
                                 'dependencies': [{'group': 'org.jetbrains.kotlin', 'module': 'kotlin-stdlib', 'version': {'requires': '2.4.21'}}]})
            component = {'group': audit.GROUP, 'module': target, 'version': version}
            if target != 'app-ui':
                component = self.link('app-ui')
            self.modules[target] = {'formatVersion': '1.1', 'component': component, 'variants': variants}
            pom = ('<project><groupId>app.simt</groupId><artifactId>' + target + '</artifactId><version>' + version +
                   '</version>' + POM_LICENSE_XML + '<dependencies><dependency><groupId>org.jetbrains.kotlin</groupId><artifactId>kotlin-stdlib</artifactId>'
                   '<version>2.4.21</version></dependency></dependencies></project>').encode()
            self.write(target, ('', 'pom'), pom)
            if version.endswith('SNAPSHOT'):
                self.metadata(target)
        for target in audit.TARGETS[1:]:
            for variant in self.modules[target]['variants']:
                self.modules['app-ui']['variants'].append({'name': variant['name'], 'attributes': variant['attributes'],
                                                          'available-at': self.link(target)})
        for target in audit.TARGETS:
            self.save_module(target)

    def link(self, target):
        return {'group': audit.GROUP, 'module': target, 'version': self.version,
                'url': '../../' + target + '/' + self.version + '/' + audit.logical_name(target, self.version, ('', 'module'))}

    def put(self, relative, data, sidecar=False):
        data = data.encode() if isinstance(data, str) else data
        path = self.root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
        if sidecar:
            Path(str(path) + '.sha256').write_text(digest(data))
        return path

    def write(self, target, key, data, update_module=False):
        path = self.repo / self.paths[target, key]
        self.put(path.relative_to(self.root), data, sidecar=True)
        if update_module:
            for variant in self.modules[target]['variants']:
                for item in variant.get('files', []):
                    if item['url'] == audit.logical_name(target, self.version, key):
                        item['size'] = len(data)
                        item['sha256'] = digest(data)
            self.save_module(target)
        return path

    def save_module(self, target):
        self.write(target, ('', 'module'), encoded(self.modules[target]))

    def metadata(self, target, transform=None):
        entries = []
        for classifier, extension in sorted(audit.required_keys(target)):
            entries.append('<snapshotVersion><classifier>' + classifier + '</classifier><extension>' + extension + '</extension><value>' +
                           self.actual + '</value><updated>20261117123456</updated></snapshotVersion>')
        text = ('<metadata><groupId>app.simt</groupId><artifactId>' + target + '</artifactId><version>' + self.version +
                '</version><versioning><snapshot><timestamp>20261117.123456</timestamp><buildNumber>1</buildNumber></snapshot>'
                '<snapshotVersions>' + ''.join(entries) + '</snapshotVersions></versioning></metadata>')
        if transform:
            text = transform(text)
        return self.put(self.repo.relative_to(self.root) / ('app/simt/' + target + '/' + self.version + '/maven-metadata.xml'), text, sidecar=True)

    def check(self):
        return audit.check_publication(self.repo, self.version)


class PublicationTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.fixture = Fixture(self.tmp.name)

    def assertBlocked(self, category=None):
        result = self.fixture.check()
        self.assertFalse(result.ok, result.payload())
        if category:
            self.assertIn(category, {c for _, c in result.findings}, result.payload())
        return result

    def test_four_exact_targets_with_generated_docs_and_inventory(self):
        result = self.fixture.check()
        self.assertTrue(result.ok, result.payload())
        for target in audit.TARGETS:
            for key in audit.required_keys(target):
                relative = self.fixture.paths[target, key]
                self.assertIn(relative, result.inventory)
                self.assertEqual(result.inventory[relative]['sha256'], digest((self.fixture.repo / relative).read_bytes()))
        self.assertEqual(result.payload()['selected'], sorted(result.payload()['selected'], key=lambda x: x['path']))

    def test_stable_uses_only_exact_direct_files(self):
        fixture = Fixture(Path(self.tmp.name) / 'stable', '1.2.3')
        result = fixture.check()
        self.assertTrue(result.ok, result.payload())
        self.assertFalse(any('maven-metadata' in p for p in result.inventory))

    def test_legal_zip_is_identical_and_mandatory_without_gradle_variants(self):
        result = self.fixture.check()
        self.assertTrue(result.ok, result.payload())
        for target in audit.TARGETS:
            self.assertIn(('legal-notices', 'zip'), audit.required_keys(target))
            self.assertFalse(any('legal-notices' in v['name'] for v in self.fixture.modules[target]['variants']))
            relative = self.fixture.paths[target, ('legal-notices', 'zip')]
            self.assertEqual((self.fixture.repo / relative).read_bytes(), self.fixture.legal_zip)
            self.assertIn(relative + '.sha256', result.inventory)
        with zipfile.ZipFile(io.BytesIO(self.fixture.legal_zip)) as stream:
            self.assertEqual(stream.namelist(), sorted(LEGAL_DOCUMENTS))
            self.assertTrue(all(i.date_time == (1980, 1, 1, 0, 0, 0) for i in stream.infolist()))
            self.assertEqual(digest(stream.read('LICENSE')), 'c71d239df91726fc519c6eb72d318ec65820627232b2f796219e87dcf35d0ab4')
            self.assertEqual(digest(stream.read('LICENSE-DOCS')), '9ba9550ad48438d0836ddab3da480b3b69ffa0aac7b7878b5a0039e7ab429411')

    def test_missing_legal_classifier_in_each_snapshot(self):
        for target in audit.TARGETS:
            with self.subTest(target=target):
                self.fixture.metadata(target, lambda s: s.replace(
                    '<snapshotVersion><classifier>legal-notices</classifier><extension>zip</extension><value>' + self.fixture.actual +
                    '</value><updated>20261117123456</updated></snapshotVersion>', ''))
                self.assertBlocked('snapshot-missing-classifier')
                self.fixture.metadata(target)

    def test_legal_classifier_requires_zip_extension(self):
        self.fixture.metadata('app-ui', lambda s: s.replace(
            '<classifier>legal-notices</classifier><extension>zip</extension>',
            '<classifier>legal-notices</classifier><extension>jar</extension>'))
        self.assertBlocked('snapshot-metadata')

    def test_missing_legal_zip_in_each_stable_publication(self):
        fixture = Fixture(Path(self.tmp.name) / 'stable', '1.2.3')
        for target in audit.TARGETS:
            with self.subTest(target=target):
                path = fixture.repo / fixture.paths[target, ('legal-notices', 'zip')]
                data = path.read_bytes()
                path.unlink()
                result = fixture.check()
                self.assertFalse(result.ok)
                self.assertIn('missing-or-unreadable', {c for _, c in result.findings})
                path.write_bytes(data)

    def test_legal_zip_exact_entries_required(self):
        for missing in LEGAL_DOCUMENTS:
            with self.subTest(missing=missing):
                entries = dict(self.fixture.legal_entries)
                del entries[missing]
                self.fixture.write('app-ui', ('legal-notices', 'zip'), archive(entries))
                self.assertBlocked('legal-notices-entries')
        for extra in ('cosmos-spec/snapshots/payload.json', 'app-ui/', 'unrelated.txt'):
            with self.subTest(extra=extra):
                self.fixture.write('app-ui', ('legal-notices', 'zip'), archive({**self.fixture.legal_entries, extra: b'{}'}))
                self.assertBlocked('legal-notices-entries')

    def test_legal_zip_cannot_carry_credentials(self):
        entries = {**self.fixture.legal_entries, 'credentials.json': encoded({'token': secret()})}
        self.fixture.write('app-ui', ('legal-notices', 'zip'), archive(entries))
        result = self.assertBlocked('forbidden-file')
        self.assertNotIn(secret(), json.dumps(result.payload()))
        self.assertIn('credential', {c for _, c in result.findings})

    def test_wrong_full_license_bytes_even_with_fresh_sidecars(self):
        for relative in ('LICENSE', 'LICENSE-DOCS'):
            for content in (b'Apache-2.0 / CC-BY-4.0 placeholder', self.fixture.legal_entries[relative][:-1],
                            self.fixture.legal_entries[relative] + b' ', self.fixture.legal_entries['NOTICE']):
                with self.subTest(relative=relative, size=len(content)):
                    entries = {**self.fixture.legal_entries, relative: content}
                    self.fixture.write('app-ui', ('legal-notices', 'zip'), archive(entries))
                    self.assertBlocked('license-text-sha256')

    def test_owner_notice_must_be_meaningful(self):
        for content in (b'', b'NOTICE', b'Copyright Said Elimam', b'\xff',
                        self.fixture.legal_entries['NOTICE'].replace(b'Said Elimam', b'Unknown owner'),
                        self.fixture.legal_entries['NOTICE'].replace(b'LICENSE-DOCS', b'OTHER')):
            with self.subTest(size=len(content)):
                self.fixture.write('app-ui', ('legal-notices', 'zip'), archive({**self.fixture.legal_entries, 'NOTICE': content}))
                self.assertBlocked('owner-notice-invalid')

    def test_false_ui_rights_in_legal_zip_block(self):
        changes = [lambda p: p.update(licenseStatus='unconfirmed'), lambda p: p.update(license='CC-BY-4.0'),
                   lambda p: p.update(publicRedistribution='blocked'), lambda p: p.pop('ownerReview'),
                   lambda p: p['ownerReview'].update(reviewer='Other owner'),
                   lambda p: p['ownerReview'].update(rightsConfirmed=False),
                   lambda p: p['ownerReview'].update(rightsConfirmed=1),
                   lambda p: p['ownerReview'].update(date='2026-11-16')]
        for index, change in enumerate(changes):
            with self.subTest(change=index):
                provenance = json.loads(self.fixture.legal_entries['app-ui/source-provenance.json'])
                change(provenance)
                entries = {**self.fixture.legal_entries, 'app-ui/source-provenance.json': encoded(provenance)}
                self.fixture.write('app-ui', ('legal-notices', 'zip'), archive(entries))
                self.assertBlocked('redistribution-rights-unconfirmed')

    def test_legal_provenance_requires_original_source_records(self):
        for change in (lambda p: p.update(files=[]), lambda p: p.update(sourceCommit='invalid'),
                       lambda p: p['files'][0].update(sha256='invalid'),
                       lambda p: p['files'][0].update(sourcePath='../escape.kt')):
            provenance = json.loads(self.fixture.legal_entries['app-ui/source-provenance.json'])
            change(provenance)
            self.fixture.write('app-ui', ('legal-notices', 'zip'), archive(
                {**self.fixture.legal_entries, 'app-ui/source-provenance.json': encoded(provenance)}))
            self.assertBlocked('malformed-provenance')

    def test_all_targets_must_attach_the_same_legal_zip(self):
        entries = {**self.fixture.legal_entries, 'NOTICE': self.fixture.legal_entries['NOTICE'] + b'\nAdditional attribution.\n'}
        self.fixture.write('app-ui-iosarm64', ('legal-notices', 'zip'), archive(entries))
        self.assertBlocked('legal-notices-mismatch')

    def test_legal_zip_sidecar_cannot_be_stale(self):
        path = self.fixture.repo / self.fixture.paths['app-ui', ('legal-notices', 'zip')]
        path.write_bytes(self.fixture.legal_zip + b'changed')
        self.assertBlocked('checksum-sidecar')

    def test_optional_legal_gradle_variant_checks_its_checksum(self):
        target = 'app-ui'
        key = ('legal-notices', 'zip')
        self.fixture.modules[target]['variants'].append({
            'name': 'legal-notices', 'attributes': {'org.gradle.category': 'documentation'},
            'files': [{'name': audit.logical_name(target, self.fixture.version, key),
                       'url': audit.logical_name(target, self.fixture.version, key),
                       'size': len(self.fixture.legal_zip), 'sha256': digest(self.fixture.legal_zip)}]})
        self.fixture.save_module(target)
        self.assertTrue(self.fixture.check().ok)
        self.fixture.modules[target]['variants'][-1]['files'][0]['sha256'] = '0' * 64
        self.fixture.save_module(target)
        self.assertBlocked('checksum-or-size')

    def test_pom_license_metadata_required_on_every_target(self):
        for target in audit.TARGETS:
            path = self.fixture.repo / self.fixture.paths[target, ('', 'pom')]
            original = path.read_bytes()
            with self.subTest(target=target):
                self.fixture.write(target, ('', 'pom'), original.replace(POM_LICENSE_XML.encode(), b''))
                self.assertBlocked('pom-license-metadata')
                self.fixture.write(target, ('', 'pom'), original)

    def test_wrong_or_duplicate_pom_license_metadata(self):
        target = 'app-ui'
        original = (self.fixture.repo / self.fixture.paths[target, ('', 'pom')]).read_bytes()
        alternatives = [POM_LICENSE_XML.replace('Apache License, Version 2.0', 'CC-BY-4.0'),
                        POM_LICENSE_XML.replace('https://www.apache.org/licenses/LICENSE-2.0.txt', 'https://invalid.example/license'),
                        POM_LICENSE_XML.replace('<distribution>repo</distribution>', '<distribution>manual</distribution>'),
                        POM_LICENSE_XML.replace('<distribution>repo</distribution>', ''),
                        POM_LICENSE_XML + POM_LICENSE_XML,
                        POM_LICENSE_XML.replace('</licenses>', '<license><name>Other</name></license></licenses>')]
        for index, alternative in enumerate(alternatives):
            with self.subTest(change=index):
                self.fixture.write(target, ('', 'pom'), original.replace(POM_LICENSE_XML.encode(), alternative.encode()))
                self.assertBlocked('pom-license-metadata')

    def test_stale_unselected_files_and_versions_ignored(self):
        self.fixture.put(self.fixture.repo.relative_to(self.fixture.root) / 'app/simt/app-ui/1.2.3-SNAPSHOT/app-ui-old.module', '{' + secret())
        self.fixture.put(self.fixture.repo.relative_to(self.fixture.root) / 'app/simt/app-ui/0.0.1-SNAPSHOT/maven-metadata.xml', 'broken')
        self.assertTrue(self.fixture.check().ok)

    def test_compound_native_resource_snapshot_mapping(self):
        result = self.fixture.check()
        for target in audit.TARGETS[2:]:
            relative = self.fixture.paths[target, ('kotlin_resources', 'kotlin_resources.zip')]
            self.assertIn(relative, result.inventory)
            self.assertTrue(relative.endswith('-kotlin_resources.kotlin_resources.zip'))

    def test_missing_target(self):
        (self.fixture.repo / self.fixture.paths['app-ui-iosarm64', ('', 'module')]).unlink()
        self.assertBlocked('missing-or-unreadable')

    def test_missing_snapshot_metadata(self):
        (self.fixture.repo / 'app/simt/app-ui/1.2.3-SNAPSHOT/maven-metadata.xml').unlink()
        self.assertBlocked('missing-or-unreadable')

    def test_malformed_snapshot_xml_redacted(self):
        path = 'app/simt/app-ui/1.2.3-SNAPSHOT/maven-metadata.xml'
        self.fixture.put(self.fixture.repo.relative_to(self.fixture.root) / path, '<broken>' + secret(), sidecar=True)
        result = self.assertBlocked('malformed-xml')
        self.assertNotIn(secret(), json.dumps(result.payload()))

    def test_snapshot_value_not_exact_version_rejected(self):
        self.fixture.metadata('app-ui', lambda s: s.replace('1.2.3-20261117.123456-1', '2.0.0-20261117.123456-1'))
        self.assertBlocked('snapshot-metadata')

    def test_stale_snapshot_entry_rejected(self):
        self.fixture.metadata('app-ui', lambda s: s.replace('<value>1.2.3-20261117.123456-1</value>', '<value>1.2.3-20261116.123456-1</value>', 1))
        self.assertBlocked('snapshot-metadata')

    def test_duplicate_snapshot_classifier_rejected(self):
        self.fixture.metadata('app-ui', lambda s: s.replace('<classifier>sources</classifier>', '<classifier>javadoc</classifier>'))
        self.assertBlocked('snapshot-metadata')

    def test_wrong_metadata_gav(self):
        self.fixture.metadata('app-ui', lambda s: s.replace('<groupId>app.simt</groupId>', '<groupId>other</groupId>'))
        self.assertBlocked('metadata-gav')

    def test_missing_javadoc_classifier(self):
        self.fixture.metadata('app-ui', lambda s: s.replace('<snapshotVersion><classifier>javadoc</classifier><extension>jar</extension><value>' + self.fixture.actual +
                                                         '</value><updated>20261117123456</updated></snapshotVersion>', ''))
        self.assertBlocked('snapshot-missing-classifier')

    def test_root_tooling_metadata_is_a_required_maven_attachment(self):
        self.fixture.modules['app-ui']['variants'] = [v for v in self.fixture.modules['app-ui']['variants']
                                                     if v['name'] != 'app-ui-kotlin-tooling-metadata']
        self.fixture.save_module('app-ui')
        self.assertTrue(self.fixture.check().ok)
        (self.fixture.repo / self.fixture.paths['app-ui', ('kotlin-tooling-metadata', 'json')]).unlink()
        self.assertBlocked('missing-or-unreadable')

    def test_missing_docs_archive(self):
        (self.fixture.repo / self.fixture.paths['app-ui-android', ('javadoc', 'jar')]).unlink()
        self.assertBlocked('missing-or-unreadable')

    def test_text_only_docs_not_generated_html(self):
        self.fixture.write('app-ui', ('javadoc', 'jar'), archive({'README.txt': 'Documentation placeholder'}), True)
        self.assertBlocked('missing-generated-html-docs')

    def test_generated_html_docs_reject_old_only_namespace(self):
        for namespace in ('app.shared.ui', 'app/shared/ui'):
            with self.subTest(namespace=namespace):
                self.fixture.write('app-ui', ('javadoc', 'jar'), archive({
                    'index.html': '<html><body>' + namespace + ' API</body></html>'}), True)
                self.assertBlocked('missing-generated-html-docs')

    def test_generated_html_docs_accept_current_namespace(self):
        for namespace in ('app.simt.ui', 'app/simt/ui'):
            with self.subTest(namespace=namespace):
                self.fixture.write('app-ui', ('javadoc', 'jar'), archive({
                    'index.html': '<html><body>' + namespace + ' API</body></html>'}), True)
                result = self.fixture.check()
                self.assertTrue(result.ok, result.payload())

    def test_empty_sources(self):
        self.fixture.write('app-ui', ('sources', 'jar'), archive({'Example.kt': ''}), True)
        self.assertBlocked('empty-sources')

    def test_actual_pom_gav_not_directory_name(self):
        self.fixture.write('app-ui', ('', 'pom'), b'<project><groupId>app.simt</groupId><artifactId>other</artifactId><version>1.2.3-SNAPSHOT</version></project>')
        self.assertBlocked('pom-gav')

    def test_wrong_module_component(self):
        self.fixture.modules['app-ui']['component']['version'] = '2.0.0'
        self.fixture.save_module('app-ui')
        self.assertBlocked('module-gav')

    def test_gradle_root_component_backlinks_are_validated(self):
        self.fixture.modules['app-ui-iosarm64']['component']['url'] = 'https://private.invalid/root.module'
        self.fixture.save_module('app-ui-iosarm64')
        self.assertBlocked('available-at')

    def test_root_requires_all_advertised_targets(self):
        self.fixture.modules['app-ui']['variants'] = [v for v in self.fixture.modules['app-ui']['variants']
                                                     if v.get('available-at', {}).get('module') != 'app-ui-iosarm64']
        self.fixture.save_module('app-ui')
        self.assertBlocked('missing-target')

    def test_available_at_variant_attributes_match(self):
        link = next(v for v in self.fixture.modules['app-ui']['variants'] if 'available-at' in v)
        link['attributes'] = {'org.jetbrains.kotlin.platform.type': 'wrong'}
        self.fixture.save_module('app-ui')
        self.assertBlocked('target-variant-mismatch')

    def test_native_metadata_required_and_advertised(self):
        target = 'app-ui-iosarm64'
        self.fixture.modules[target]['variants'] = [v for v in self.fixture.modules[target]['variants'] if v['name'] != target + '-metadata']
        self.fixture.save_module(target)
        self.assertBlocked('missing-platform-or-files')

    def test_tampered_module_advertised_sha256(self):
        self.fixture.modules['app-ui']['variants'][0]['files'][0]['sha256'] = '0' * 64
        self.fixture.save_module('app-ui')
        self.assertBlocked('checksum-or-size')

    def test_wrong_advertised_size(self):
        self.fixture.modules['app-ui']['variants'][0]['files'][0]['size'] += 1
        self.fixture.save_module('app-ui')
        self.assertBlocked('checksum-or-size')

    def test_tampered_unadvertised_javadoc_sidecar(self):
        target = 'app-ui'
        self.fixture.modules[target]['variants'] = [v for v in self.fixture.modules[target]['variants'] if v['name'] != target + '-javadoc']
        self.fixture.save_module(target)
        path = self.fixture.repo / self.fixture.paths[target, ('javadoc', 'jar')]
        path.write_bytes(archive({'index.html': '<html><body>dokka changed docs</body></html>'}))
        self.assertBlocked('checksum-sidecar')

    def test_missing_checksum_sidecar(self):
        Path(str(self.fixture.repo / self.fixture.paths['app-ui', ('', 'pom')]) + '.sha256').unlink()
        self.assertBlocked('missing-or-unreadable')

    def test_private_host_and_network_dependencies(self):
        for group, module in (('app.simt', 'host'), ('io.ktor', 'ktor-client'), ('androidx.room', 'room-runtime')):
            with self.subTest(group=group):
                self.fixture.modules['app-ui']['variants'][0]['dependencies'] = [{'group': group, 'module': module}]
                self.fixture.save_module('app-ui')
                self.assertBlocked('private-or-network-dependency')

    def test_forced_bom(self):
        self.fixture.modules['app-ui']['variants'][0]['dependencies'] = [{'group': 'org.jetbrains.compose.ui', 'module': 'ui',
                                                                        'attributes': {'org.gradle.category': 'enforced-platform'}}]
        self.fixture.save_module('app-ui')
        self.assertBlocked('forced-bom')

    def test_module_file_traversal_and_external_urls(self):
        item = self.fixture.modules['app-ui']['variants'][0]['files'][0]
        for value in ('../escape.jar', '/outside.jar', 'https://evil.invalid/a.jar', 'a%2f../b.jar', 'a\\b.jar'):
            with self.subTest(value=value):
                item['url'] = value
                self.fixture.save_module('app-ui')
                self.assertBlocked('unsafe-path')

    def test_selected_file_symlink(self):
        path = self.fixture.repo / self.fixture.paths['app-ui', ('sources', 'jar')]
        data = path.read_bytes()
        path.unlink()
        outside = self.fixture.put('outside.jar', data)
        path.symlink_to(outside)
        self.assertBlocked('symlink')

    def test_selected_directory_symlink(self):
        directory = self.fixture.repo / 'app/simt/app-ui'
        relocated = directory.with_name('other')
        directory.rename(relocated)
        directory.symlink_to(relocated, target_is_directory=True)
        self.assertBlocked('symlink')

    def test_broken_archive(self):
        self.fixture.write('app-ui-iosarm64', ('', 'klib'), b'not an archive')
        self.assertBlocked('broken-archive')

    def test_nested_archive_traversal(self):
        data = archive({'classes.jar': archive({'../escape.txt': b'content'})})
        self.fixture.write('app-ui-android', ('', 'aar'), data, True)
        self.assertBlocked('unsafe-path')

    def test_archive_symlink(self):
        stream = io.BytesIO()
        with zipfile.ZipFile(stream, 'w') as z:
            info = zipfile.ZipInfo('link')
            info.create_system = 3
            info.external_attr = (stat.S_IFLNK | 0o777) << 16
            z.writestr(info, 'target')
        self.fixture.write('app-ui', ('', 'jar'), stream.getvalue(), True)
        self.assertBlocked('archive-symlink-or-special')

    def test_archive_nesting_limit(self):
        data = b'payload'
        for _ in range(audit.MAX_DEPTH + 1):
            data = archive({'nested.jar': data})
        self.fixture.write('app-ui', ('', 'jar'), data, True)
        self.assertBlocked('archive-depth')

    def test_archive_bomb_ratio(self):
        self.fixture.write('app-ui', ('', 'jar'), archive({'huge.bin': b'0' * (2 * 1024 * 1024)}, zipfile.ZIP_DEFLATED), True)
        self.assertBlocked('archive-limit')

    def test_archive_count_limit(self):
        self.fixture.write('app-ui', ('', 'jar'), archive({str(i): b'' for i in range(200)}), True)
        with patch.object(audit, 'MAX_ENTRIES', 100):
            self.assertBlocked('archive-count')

    def test_archive_expanded_size_limit(self):
        self.fixture.write('app-ui', ('', 'jar'), archive({'a': b'a' * 1000, 'b': b'b' * 1000}), True)
        with patch.object(audit, 'MAX_EXPANDED', 1800):
            self.assertBlocked()

    def test_forbidden_filename_inside_nested_archive(self):
        self.fixture.write('app-ui', ('', 'jar'), archive({'nested.jar': archive({'local.properties': b'sdk.dir=relative'})}), True)
        self.assertBlocked('forbidden-file')

    def test_git_history_cannot_be_embedded_in_archive(self):
        self.fixture.write('app-ui', ('', 'jar'), archive({'.git/config': b'operational'}), True)
        self.assertBlocked('forbidden-file')

    def test_android_library_cannot_request_permissions(self):
        self.fixture.write('app-ui-android', ('', 'aar'), archive({'AndroidManifest.xml': '<manifest><uses-permission name="permission"/></manifest>'}), True)
        self.assertBlocked('android-permission')

    def test_xml_external_entities_rejected(self):
        self.fixture.write('app-ui', ('', 'pom'), b'<!DOCTYPE project [<!ENTITY x SYSTEM "file:///private">]><project>&x;</project>')
        self.assertBlocked('xml-entity')

    def test_malformed_json_and_duplicate_keys(self):
        for content in (b'{"component":', b'{"component":{},"component":{}}', b'{"v":NaN}'):
            with self.subTest(content=content):
                self.fixture.write('app-ui', ('', 'module'), content)
                self.assertBlocked('malformed-json')

    def test_credential_personal_path_and_key_redaction_stdout_stderr(self):
        samples = [secret(), 'api_' + 'key="' + 'topsecretvalue' + '"',
                   '/' + 'Users/' + 'privateperson/' + 'workspace', '-' * 5 + 'BEGIN ' + 'PRIVATE KEY' + '-' * 5]
        for sample in samples:
            with self.subTest(category=sample[:2]):
                self.fixture.write('app-ui', ('', 'jar'), archive({'content.txt': sample}), True)
                out, err = io.StringIO(), io.StringIO()
                with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
                    code = audit.main(['publication', '--repo', str(self.fixture.repo), '--version', self.fixture.version])
                self.assertEqual(code, 1)
                self.assertNotIn(sample, out.getvalue() + err.getvalue())
                self.assertEqual(err.getvalue(), '')
                self.assertFalse(json.loads(out.getvalue())['ok'])

    def test_raw_malformed_payload_never_printed(self):
        self.fixture.write('app-ui', ('', 'module'), ('{"oops": ' + secret()).encode())
        out, err = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            self.assertEqual(audit.main(['publication', '--repo', str(self.fixture.repo), '--version', self.fixture.version]), 1)
        self.assertNotIn(secret(), out.getvalue() + err.getvalue())
        self.assertNotIn('oops', out.getvalue() + err.getvalue())


class SourceTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name).resolve()
        self.git('init', '-q')
        self.put('.gitignore', 'build/\nignored/\n')
        self.put('safe.txt', 'ordinary source')
        self.commit('initial')

    def git(self, *args):
        env = dict(os.environ, GIT_AUTHOR_NAME='Fixture', GIT_AUTHOR_EMAIL='fixture@example.invalid',
                   GIT_COMMITTER_NAME='Fixture', GIT_COMMITTER_EMAIL='fixture@example.invalid',
                   GIT_CONFIG_NOSYSTEM='1', GIT_CONFIG_GLOBAL=os.devnull)
        result = subprocess.run(['git', '-C', str(self.root), *args], stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=env, check=True)
        return result.stdout

    def put(self, relative, text):
        path = self.root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text)
        return path

    def commit(self, message):
        self.git('add', '.gitignore', 'safe.txt')
        self.git('commit', '-qm', message)

    def test_cached_and_untracked_excludes_ignored(self):
        self.put('untracked.txt', secret())
        self.put('ignored/bad.txt', secret())
        result = audit.check_source(self.root)
        self.assertIn(('untracked.txt', 'credential'), result.findings)
        self.assertFalse(any('ignored' in p for p, _ in result.findings))

    def test_deleted_history_secret_and_forbidden_path(self):
        self.put('safe.txt', secret())
        self.commit('history leak')
        self.put('safe.txt', 'safe now')
        self.commit('remove leak')
        self.put('local.properties', 'operational')
        self.git('add', 'local.properties')
        self.git('commit', '-qm', 'operational file')
        (self.root / 'local.properties').unlink()
        self.git('add', 'local.properties')
        self.git('commit', '-qm', 'remove operational file')
        result = audit.check_source(self.root)
        self.assertIn(('history/safe.txt', 'credential'), result.findings)
        self.assertIn(('history/local.properties', 'forbidden-file'), result.findings)
        self.assertNotIn(secret(), json.dumps(result.payload()))

    def test_renaming_identical_blob_does_not_hide_manifest_permissions(self):
        content = '<manifest><uses-permission name="permission"/></manifest>'
        self.put('AndroidManifest.xml', content)
        self.git('add', 'AndroidManifest.xml')
        self.git('commit', '-qm', 'manifest')
        (self.root / 'AndroidManifest.xml').unlink()
        self.put('renamed.txt', content)
        self.git('add', '-u')
        self.git('add', 'renamed.txt')
        self.git('commit', '-qm', 'rename identical blob')
        result = audit.check_source(self.root)
        self.assertIn(('history/AndroidManifest.xml', 'android-permission'), result.findings)

    def test_all_refs_not_just_head(self):
        self.git('branch', 'clean')
        self.put('safe.txt', secret())
        self.commit('other ref leak')
        self.git('branch', 'leak-ref')
        # Move HEAD to the existing clean ref in this disposable fixture only.
        self.git('switch', 'clean')
        result = audit.check_source(self.root)
        self.assertIn(('history/safe.txt', 'credential'), result.findings)

    def test_provenance_prose_and_scanner_patterns_are_not_findings(self):
        self.put('prose.json', json.dumps({'note': 'Excluded google-services.json, local.properties, signing.properties, .env.'}))
        self.put('patterns.py', Path(audit.__file__).read_text())
        result = audit.check_source(self.root)
        self.assertTrue(result.ok, result.payload())

    def test_only_standalone_cosmos_wrangler_config_is_allowed(self):
        self.put('cosmos-site/wrangler.jsonc', '{"name":"simt-cosmos"}')
        self.put('wrangler.jsonc', '{"name":"root"}')
        self.put('other/wrangler.jsonc', '{"name":"other"}')
        result = audit.check_source(self.root)
        self.assertNotIn(('cosmos-site/wrangler.jsonc', 'forbidden-file'), result.findings)
        self.assertIn(('wrangler.jsonc', 'forbidden-file'), result.findings)
        self.assertIn(('other/wrangler.jsonc', 'forbidden-file'), result.findings)

    def test_allowed_wrangler_config_is_still_content_scanned(self):
        self.put('cosmos-site/wrangler.jsonc', '{"token":"' + secret() + '"}')
        result = audit.check_source(self.root)
        self.assertIn(('cosmos-site/wrangler.jsonc', 'credential'), result.findings)
        self.assertNotIn(('cosmos-site/wrangler.jsonc', 'forbidden-file'), result.findings)

    def test_exact_wrangler_allowance_applies_to_history(self):
        self.put('cosmos-site/wrangler.jsonc', '{"name":"simt-cosmos"}')
        self.git('add', 'cosmos-site/wrangler.jsonc')
        self.git('commit', '-qm', 'worker config')
        result = audit.check_source(self.root)
        self.assertTrue(result.ok, result.payload())

    def test_worktree_symlink_is_rejected(self):
        (self.root / 'link').symlink_to('safe.txt')
        result = audit.check_source(self.root)
        self.assertIn(('link', 'symlink'), result.findings)

    def test_ignored_framework_not_scanned(self):
        path = self.root / 'build/DEBUG.framework/DEBUG'
        path.parent.mkdir(parents=True)
        with path.open('wb') as stream:
            stream.truncate(355 * 1024 * 1024)
        self.assertTrue(audit.check_source(self.root).ok)

    def test_cli_source_redacts_both_streams(self):
        self.put('safe.txt', secret())
        out, err = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            self.assertEqual(audit.main(['source', '--root', str(self.root)]), 1)
        self.assertNotIn(secret(), out.getvalue() + err.getvalue())
        self.assertEqual(err.getvalue(), '')


class ReleaseTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.fixture = Fixture(self.tmp.name, '1.2.3')
        self.root = self.fixture.root
        for relative in audit.RELEASE_DOCS:
            if relative not in LEGAL_DOCUMENTS:
                self.fixture.put(relative, 'Owner-confirmed release document')
        self.fixture.put('app-ui/api/app-ui.api', 'public class app/simt/ui/Example')
        self.fixture.put('cosmos-spec/scripts/check.js', '// fixed strict schema checker fixture')
        self.fixture.put('cosmos-spec/snapshots/specification.md', 'Approved specification')
        source = json.loads(LEGAL_DOCUMENTS['app-ui/source-provenance.json'])
        cosmos = {'licenses': {'code': {'status': 'confirmed'}, 'documents': {'status': 'confirmed'}},
                  'distribution': {'scope': 'public-redistribution', 'publicRedistribution': 'approved'},
                  'knownSchemaDefects': [], 'inventory': [{'path': 'snapshots/specification.md',
                   'sha256': digest(b'Approved specification')}]}
        self.fixture.put('app-ui/source-provenance.json', encoded(source))
        self.fixture.put('cosmos-spec/provenance.json', encoded(cosmos))
        self.tracked = list(audit.RELEASE_DOCS) + ['app-ui/api/app-ui.api', 'app-ui/source-provenance.json',
                        'cosmos-spec/provenance.json', 'cosmos-spec/snapshots/specification.md',
                        'cosmos-spec/scripts/check.js']
        self.command_calls = []
        self.commands = patch.object(audit, 'run_command', side_effect=self.command)
        self.commands.start()
        self.addCleanup(self.commands.stop)

    def command(self, root, args, **kwargs):
        self.command_calls.append(list(args))
        if args[0] == 'node':
            self.assertEqual(args, ['node', str(self.root / 'cosmos-spec/scripts/check.js'), 'canonical-schema'])
            return b'canonical schemas passed'
        rest = args[2:]
        if rest[:2] == ['rev-parse', '--verify']:
            return HEAD.encode() + b'\n'
        if rest[:1] == ['status']:
            return b''
        if rest[:1] == ['ls-files']:
            return b'\x00'.join(p.encode() for p in self.tracked) + b'\x00'
        if rest == ['rev-list', '--all']:
            return HEAD.encode() + b'\n'
        if rest == ['rev-parse', '--is-shallow-repository']:
            return b'false\n'
        objects = {hashlib.sha1((self.root / p).read_bytes()).hexdigest(): (p, (self.root / p).read_bytes())
                   for p in self.tracked if (self.root / p).exists()}
        if rest[:1] == ['ls-tree']:
            return b''.join(('100644 blob ' + oid + '\t' + p).encode() + b'\x00' for oid, (p, _) in objects.items())
        if rest[:2] == ['cat-file', '-s']:
            return str(len(objects[rest[2]][1])).encode()
        if rest[:2] == ['cat-file', 'blob']:
            return objects[rest[2]][1]
        self.fail('Unexpected fixed command')

    def gates(self, version='1.2.3'):
        return audit.check_release_gates(self.root, version)

    def assertGate(self, category, **kwargs):
        result = self.gates(**kwargs)
        self.assertFalse(result.ok, result.payload())
        self.assertIn(category, {c for _, c in result.findings}, result.payload())
        return result

    def test_positive_release_with_fixed_mock_commands(self):
        result = audit.check_release(self.root, self.fixture.repo, '1.2.3')
        self.assertTrue(result.ok, result.payload())
        self.assertTrue(any(args[0] == 'node' for args in self.command_calls))

    def test_snapshot_and_prerelease_not_stable(self):
        for version in ('1.2.3-SNAPSHOT', '1.2.3-rc.1', '01.2.3', 'v1.2.3'):
            with self.subTest(version=version):
                self.assertGate('stable-semver-required', version=version)

    def test_dirty_tree(self):
        original = self.command
        def dirty(root, args, **kwargs):
            return b' M dirty.txt\x00' if 'status' in args else original(root, args, **kwargs)
        with patch.object(audit, 'run_command', side_effect=dirty):
            self.assertGate('dirty-tree')

    def test_missing_or_wrong_stable_head_tag(self):
        original = self.command
        for missing in (False, True):
            def tag(root, args, **kwargs):
                if any(str(a).startswith('refs/tags/') for a in args):
                    if missing:
                        raise audit.ValidationError('commands', 'command-failed')
                    return b'b' * 40
                return original(root, args, **kwargs)
            with self.subTest(missing=missing), patch.object(audit, 'run_command', side_effect=tag):
                self.assertGate('matching-version-tag-required')

    def test_source_license_independently_unconfirmed(self):
        self.fixture.put('app-ui/source-provenance.json', encoded({'licenseStatus': 'Unconfirmed; owner review required'}))
        self.assertGate('redistribution-rights-unconfirmed')

    def test_false_ui_rights_block_release(self):
        original = json.loads((self.root / 'app-ui/source-provenance.json').read_bytes())
        for change in (lambda p: p.update(license='CC-BY-4.0'),
                       lambda p: p['ownerReview'].update(rightsConfirmed=False),
                       lambda p: p['ownerReview'].update(reviewer='Other owner'),
                       lambda p: p['ownerReview'].pop('date')):
            source = copy.deepcopy(original)
            change(source)
            self.fixture.put('app-ui/source-provenance.json', encoded(source))
            self.assertGate('redistribution-rights-unconfirmed')

    def test_release_documents_require_full_license_bytes_and_owner_notice(self):
        for relative, content, category in (('LICENSE', b'Apache-2.0', 'license-text-sha256'),
                                            ('LICENSE-DOCS', b'CC-BY-4.0', 'license-text-sha256'),
                                            ('NOTICE', b'Copyright Said Elimam', 'owner-notice-invalid')):
            with self.subTest(relative=relative):
                self.fixture.put(relative, content)
                result = audit.check_release(self.root, self.fixture.repo, '1.2.3')
                self.assertFalse(result.ok, result.payload())
                self.assertIn((relative, category), result.findings)
                self.fixture.put(relative, LEGAL_DOCUMENTS[relative])

    def test_missing_document_license_blocks_release(self):
        (self.root / 'LICENSE-DOCS').unlink()
        result = audit.check_release(self.root, self.fixture.repo, '1.2.3')
        self.assertFalse(result.ok)
        self.assertIn(('LICENSE-DOCS', 'missing-or-unreadable'), result.findings)

    def test_cosmos_document_rights_independently_unconfirmed(self):
        path = self.root / 'cosmos-spec/provenance.json'
        data = json.loads(path.read_bytes())
        data['licenses']['documents']['status'] = 'unconfirmed'
        path.write_bytes(encoded(data))
        self.assertGate('redistribution-rights-unconfirmed')

    def test_private_only_provenance_blocks_release(self):
        path = self.root / 'cosmos-spec/provenance.json'
        data = json.loads(path.read_bytes())
        data['distribution']['scope'] = 'private-development-only'
        path.write_bytes(encoded(data))
        self.assertGate('redistribution-rights-unconfirmed')

    def test_known_canonical_schema_defects_block(self):
        path = self.root / 'cosmos-spec/provenance.json'
        data = json.loads(path.read_bytes())
        data['knownSchemaDefects'] = [{'file': 'scoped/rules.json'}] * 4
        path.write_bytes(encoded(data))
        self.assertGate('canonical-schema-blocked')

    def test_fixed_strict_cosmos_command_failure_blocks_not_integrity_pass(self):
        original = self.command
        def strict(root, args, **kwargs):
            if args[0] == 'node':
                raise audit.ValidationError('commands', 'command-failed')
            return original(root, args, **kwargs)
        with patch.object(audit, 'run_command', side_effect=strict):
            self.assertGate('canonical-schema-check-failed')

    def test_missing_release_document_and_abi_block(self):
        (self.root / 'NOTICE').unlink()
        self.tracked.remove('app-ui/api/app-ui.api')
        result = audit.check_release(self.root, self.fixture.repo, '1.2.3')
        self.assertFalse(result.ok)
        self.assertIn('missing-abi-baseline', {c for _, c in result.findings})

    def test_deterministic_sorted_fixed_time_unsigned_local_stage(self):
        expected = audit.check_release(self.root, self.fixture.repo, '1.2.3')
        self.assertTrue(expected.ok, expected.payload())
        artifacts = [{'path': p, **expected.inventory[p]} for p in sorted(expected.inventory)]
        first, second = self.root / 'build/first.zip', self.root / 'build/second.zip'
        for path in (first, second):
            result = audit.stage_release(self.root, self.fixture.repo, '1.2.3', path)
            self.assertTrue(result.ok, result.payload())
        self.assertEqual(first.read_bytes(), second.read_bytes())
        with zipfile.ZipFile(first) as stream:
            self.assertEqual(stream.namelist(), sorted(stream.namelist()))
            self.assertTrue(all(i.date_time == (1980, 1, 1, 0, 0, 0) for i in stream.infolist()))
            manifest = json.loads(stream.read('artifact-checksums.json'))
            self.assertTrue(manifest['unsigned'])
            self.assertEqual(manifest['artifacts'], artifacts)
            for relative in ('LICENSE', 'LICENSE-DOCS', 'NOTICE'):
                self.assertEqual(stream.read('source/' + relative), LEGAL_DOCUMENTS[relative])
            for target in audit.TARGETS:
                self.assertEqual(stream.read('maven/' + self.fixture.paths[target, ('legal-notices', 'zip')]), self.fixture.legal_zip)
            self.assertFalse(any(name.endswith(('.asc', '.sig')) for name in stream.namelist()))

    def test_stage_does_not_replace_existing_output(self):
        output = self.fixture.put('existing.zip', 'existing user data')
        result = audit.stage_release(self.root, self.fixture.repo, '1.2.3', output)
        self.assertFalse(result.ok)
        self.assertEqual(output.read_text(), 'existing user data')

    def test_stage_symlink_and_outside_workspace_output_rejected(self):
        link = self.root / 'linked.zip'
        link.symlink_to('outside.zip')
        for output in (link, self.root.parent / 'outside.zip'):
            with self.subTest(output=output.name):
                self.assertFalse(audit.stage_release(self.root, self.fixture.repo, '1.2.3', output).ok)


class IntegrationStageTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.fixture = Fixture(self.tmp.name)
        for relative in ('README.md', 'app-ui/Module.md'):
            self.fixture.put(relative, 'Private integration documentation')
        # Legal rights are confirmed; final release/runtime approval still is not.
        self.state = {'head': HEAD, 'sourceSha256': 'b' * 64, 'dirtyTree': True}
        self.real_state = audit.integration_state
        state = patch.object(audit, 'integration_state', return_value=self.state)
        state.start()
        self.addCleanup(state.stop)
        source = patch.object(audit, 'check_source', side_effect=lambda root: audit.Audit('source-safety'))
        source.start()
        self.addCleanup(source.stop)

    def stage(self, name='integration.zip'):
        output = self.fixture.root / 'build' / name
        return audit.stage_integration(self.fixture.root, self.fixture.repo, self.fixture.version, output), output

    def test_snapshot_before_approval_and_consumer_evidence_is_private_only(self):
        result, output = self.stage()
        self.assertTrue(result.ok, result.payload())
        with zipfile.ZipFile(output) as stream:
            manifest = json.loads(stream.read('artifact-checksums.json'))
            self.assertEqual(manifest['purpose'], 'private-consumer-integration')
            self.assertFalse(manifest['publicRedistributionApproved'])
            self.assertFalse(manifest['runtimeValidationApproved'])
            self.assertTrue(manifest['unsigned'])
            self.assertEqual(manifest['head'], HEAD)
            self.assertTrue(manifest['dirtyTree'])
            self.assertFalse(any(p.startswith('source/cosmos-spec/') for p in stream.namelist()))
            for relative in ('LICENSE', 'LICENSE-DOCS', 'NOTICE'):
                self.assertEqual(stream.read('source/' + relative), LEGAL_DOCUMENTS[relative])
                self.assertIn('source/' + relative, {entry['path'] for entry in manifest['artifacts']})
            for target in audit.TARGETS:
                self.assertEqual(stream.read('maven/' + self.fixture.paths[target, ('legal-notices', 'zip')]), self.fixture.legal_zip)
            for entry in manifest['artifacts']:
                data = stream.read(entry['path'])
                self.assertEqual(digest(data), entry['sha256'])
                self.assertEqual(len(data), entry['size'])

    def test_repeated_inputs_are_deterministic(self):
        first, one = self.stage('one.zip')
        second, two = self.stage('two.zip')
        self.assertTrue(first.ok and second.ok)
        self.assertEqual(one.read_bytes(), two.read_bytes())

    def test_missing_license_copies_block_private_stage(self):
        for relative in ('LICENSE', 'LICENSE-DOCS', 'NOTICE'):
            with self.subTest(relative=relative):
                path = self.fixture.root / relative
                data = path.read_bytes()
                path.unlink()
                result, output = self.stage()
                self.assertFalse(result.ok)
                self.assertIn((relative, 'missing-or-unreadable'), result.findings)
                self.assertFalse(output.exists())
                path.write_bytes(data)

    def test_invalid_root_legal_documents_block_private_stage(self):
        for relative, content, category in (('LICENSE', b'Apache-2.0', 'license-text-sha256'),
                                            ('LICENSE-DOCS', b'CC-BY-4.0', 'license-text-sha256'),
                                            ('NOTICE', b'Copyright Said Elimam', 'owner-notice-invalid')):
            with self.subTest(relative=relative):
                self.fixture.put(relative, content)
                result, output = self.stage()
                self.assertFalse(result.ok)
                self.assertIn((relative, category), result.findings)
                self.assertFalse(output.exists())
                self.fixture.put(relative, LEGAL_DOCUMENTS[relative])

    def test_false_ui_grant_blocks_stage_without_final_approval(self):
        provenance = json.loads(LEGAL_DOCUMENTS['app-ui/source-provenance.json'])
        provenance['ownerReview']['rightsConfirmed'] = False
        self.fixture.put('app-ui/source-provenance.json', encoded(provenance))
        result, output = self.stage()
        self.assertFalse(result.ok)
        self.assertIn(('app-ui/source-provenance.json', 'redistribution-rights-unconfirmed'), result.findings)
        self.assertFalse(output.exists())

    def test_license_copy_change_after_audit_blocks_before_output(self):
        original = audit.read_file
        reads = 0
        def changed(base, relative, *args):
            nonlocal reads
            data = original(base, relative, *args)
            if str(relative) == 'LICENSE-DOCS':
                reads += 1
                if reads > 1:
                    return data + b'changed'
            return data
        with patch.object(audit, 'read_file', side_effect=changed):
            result, output = self.stage()
        self.assertFalse(result.ok)
        self.assertIn('stage-input-changed', {c for _, c in result.findings})
        self.assertFalse(output.exists())

    def test_bad_publication_blocks_before_output(self):
        (self.fixture.repo / self.fixture.paths['app-ui-android', ('', 'aar')]).write_bytes(b'changed')
        result, output = self.stage()
        self.assertFalse(result.ok)
        self.assertFalse(output.exists())

    def test_source_safety_remains_required(self):
        blocked = audit.Audit('source-safety')
        blocked.add('source', 'credential')
        with patch.object(audit, 'check_source', return_value=blocked):
            result, output = self.stage()
        self.assertFalse(result.ok)
        self.assertFalse(output.exists())

    def test_source_state_change_blocks_before_output(self):
        with patch.object(audit, 'integration_state', side_effect=[self.state, {**self.state, 'sourceSha256': 'c' * 64}]):
            result, output = self.stage()
        self.assertIn(('source', 'stage-source-changed'), result.findings)
        self.assertFalse(output.exists())

    def test_artifact_change_after_audit_blocks_before_output(self):
        original = audit.read_file
        reads = 0
        def changed(base, relative, *args):
            nonlocal reads
            data = original(base, relative, *args)
            if str(relative) == self.fixture.paths['app-ui-android', ('', 'aar')]:
                reads += 1
                if reads > 1:
                    return data + b'changed'
            return data
        with patch.object(audit, 'read_file', side_effect=changed):
            result, output = self.stage()
        self.assertFalse(result.ok)
        self.assertIn('stage-input-changed', {c for _, c in result.findings})
        self.assertFalse(output.exists())

    def test_existing_output_is_never_replaced(self):
        output = self.fixture.put('build/integration.zip', 'existing user data')
        result, _ = self.stage()
        self.assertFalse(result.ok)
        self.assertEqual(output.read_text(), 'existing user data')

    def test_output_cannot_escape_workspace_or_follow_symlink(self):
        link = self.fixture.root / 'build/linked.zip'
        link.symlink_to('other.zip')
        for output in (link, self.fixture.root.parent / 'outside.zip'):
            with self.subTest(output=output.name):
                result = audit.stage_integration(self.fixture.root, self.fixture.repo, self.fixture.version, output)
                self.assertFalse(result.ok)

    def test_source_fingerprint_changes_without_git_status_change(self):
        path = self.fixture.put('tracked.kt', 'first')
        with patch.object(audit, 'source_files', return_value=['tracked.kt']), patch.object(audit, 'git', side_effect=[HEAD.encode(), b' M tracked.kt', HEAD.encode(), b' M tracked.kt']):
            first = self.real_state(self.fixture.root)
            path.write_text('second')
            second = self.real_state(self.fixture.root)
        self.assertNotEqual(first['sourceSha256'], second['sourceSha256'])
        self.assertEqual(first['dirtyTree'], second['dirtyTree'])


class CommandTests(unittest.TestCase):
    def test_force_and_invalid_arguments_are_redacted(self):
        out, err = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            self.assertEqual(audit.main(['release', '--force', secret()]), 1)
        self.assertNotIn(secret(), out.getvalue() + err.getvalue())
        self.assertEqual(err.getvalue(), '')

    def test_documented_help(self):
        out = io.StringIO()
        with contextlib.redirect_stdout(out), self.assertRaises(SystemExit) as exited:
            audit.main(['--help'])
        self.assertEqual(exited.exception.code, 0)
        self.assertIn('source', out.getvalue())
        self.assertIn('publication', out.getvalue())
        self.assertIn('stage', out.getvalue())

    def test_fixed_command_output_limit_and_failure_do_not_leak(self):
        with self.assertRaises(audit.ValidationError) as error:
            audit.run_command(Path(audit.__file__).parent, ['python3', '-c', 'print("x" * 1000)'], limit=20)
        self.assertEqual(error.exception.category, 'command-limit')
        with self.assertRaises(audit.ValidationError) as error:
            audit.run_command(Path(audit.__file__).parent, ['python3', '-c', 'import sys; sys.stderr.write("sensitive"); sys.exit(1)'])
        self.assertEqual(error.exception.category, 'command-failed')


if __name__ == '__main__':
    unittest.main()
