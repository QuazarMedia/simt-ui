#!/usr/bin/env python3
"""Bounded offline audits and owner-gated unsigned local staging; no signing/upload."""

import argparse
from collections import Counter
from dataclasses import dataclass, field
import hashlib
import io
import json
import os
from pathlib import Path, PurePosixPath
import re
import selectors
import stat
import struct
import subprocess
import time
import xml.etree.ElementTree as ET
import zipfile

GROUP = 'app.simt'
TARGETS = ('app-ui', 'app-ui-android', 'app-ui-iosarm64', 'app-ui-iossimulatorarm64')
MAX_FILE = 32 * 1024 * 1024
MAX_TEXT = 8 * 1024 * 1024
MAX_EXPANDED = 256 * 1024 * 1024
MAX_ENTRIES = 10000
MAX_DEPTH = 4
MAX_OBJECTS = 100000
MAX_COMMAND = 64 * 1024 * 1024
VERSION = re.compile(r'(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-SNAPSHOT)?\Z')
STABLE = re.compile(r'(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)\Z')
HEX = re.compile(r'[0-9a-f]{64}\Z')
PATTERNS = {
    'private-key': re.compile(rb'-----BEGIN (?:RSA |EC |DSA |OPENSSH |ENCRYPTED )?PRIVATE KEY-----'),
    'credential': re.compile(
        rb'(?:\bAKIA[A-Z0-9]{16}\b|\b(?:gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{30,})\b|'
        rb'\bBearer\s+[A-Za-z0-9_.+/=-]{20,}|https?://[^\s/:]+:[^\s/@]+@|'
        rb'[\"\']?(?:password|passwd|api[_-]?key|access[_-]?token|secret[_-]?key|'
        rb'client[_-]?secret|storePassword|keyPassword)[\"\']?\s*[:=]\s*[\"\'][A-Za-z0-9_.+/=@!$%-]{8,}[\"\'])',
        re.I),
    'personal-path': re.compile(
        rb'(?:/(?:Users|home)/[A-Za-z0-9_.-]+/|[A-Za-z]:[\\/]+Users[\\/]+[A-Za-z0-9_.-]+[\\/])'),
}
FORBIDDEN_NAMES = {
    'local.properties', 'google-services.json', 'googleservice-info.plist',
    'signing.properties', 'keystore.properties', 'credentials.json', 'secrets.json',
    '.npmrc', '.netrc', '.pypirc', '.gitconfig', 'id_rsa', 'id_dsa', 'id_ed25519',
    'firebase.json', '.firebaserc', 'wrangler.toml', 'wrangler.json', 'wrangler.jsonc',
}
ARCHIVE_EXT = ('.jar', '.aar', '.klib', '.zip')
EVIDENCE_CHECKS = {
    'iosRuntime': ('rendering', 'insets', 'haptics', 'resources'),
    'androidFloorHaptics': ('floor', 'haptics', 'rendering', 'resources'),
    'consumerIntegration': ('integration',),
    'securityReview': ('security', 'privacy', 'accessibility'),
    'producerQuality': ('producer', 'abi', 'docs'),
}
LICENSE_SHA256 = {
    'LICENSE': 'c71d239df91726fc519c6eb72d318ec65820627232b2f796219e87dcf35d0ab4',
    'LICENSE-DOCS': '9ba9550ad48438d0836ddab3da480b3b69ffa0aac7b7878b5a0039e7ab429411',
}
LEGAL_ENTRIES = frozenset((*LICENSE_SHA256, 'NOTICE', 'app-ui/source-provenance.json'))
LEGAL_KEY = ('legal-notices', 'zip')
POM_LICENSE = {'name': 'Apache License, Version 2.0',
               'url': 'https://www.apache.org/licenses/LICENSE-2.0.txt', 'distribution': 'repo'}
RELEASE_DOCS = ('LICENSE', 'LICENSE-DOCS', 'NOTICE', 'README.md', 'CHANGELOG.md', 'app-ui/Module.md')


class ValidationError(Exception):
    def __init__(self, path, category):
        self.path = safe_label(path)
        self.category = category
        super().__init__(category)


def safe_label(path):
    text = str(path)
    raw = text.encode('utf-8', 'replace')
    if any(pattern.search(raw) for pattern in PATTERNS.values()):
        return '[redacted-path]'
    if (text.startswith('/') or '\\' in text or '..' in text.split('/') or
            not re.fullmatch(r'[A-Za-z0-9_./@+ !\[\]-]{1,500}', text)):
        return '[path]'
    return text


@dataclass
class Audit:
    scope: str
    findings: set = field(default_factory=set)
    inventory: dict = field(default_factory=dict)
    scanned: int = 0

    @property
    def ok(self):
        return not self.findings

    def add(self, path, category):
        self.findings.add((safe_label(path), category))

    def catch(self, error):
        self.add(error.path, error.category)

    def merge(self, other):
        self.findings.update(other.findings)
        self.scanned += other.scanned

    def record(self, path, data):
        self.inventory[str(path)] = {'size': len(data), 'sha256': hashlib.sha256(data).hexdigest()}

    def payload(self):
        result = {'ok': self.ok, 'scope': self.scope, 'scanned': self.scanned,
                  'counts': dict(sorted(Counter(c for _, c in self.findings).items())),
                  'findings': [{'path': p, 'category': c} for p, c in sorted(self.findings)]}
        if self.ok and self.inventory:
            result['selected'] = [{'path': p, **self.inventory[p]} for p in sorted(self.inventory)]
        return result


def validate_relative(relative):
    relative = str(relative)
    if (not relative or '\\' in relative or ':' in relative or '%' in relative or
            '?' in relative or '#' in relative or relative.startswith('/') or
            any(p in ('', '.', '..') for p in relative.split('/')) or
            any(ord(c) < 32 or ord(c) == 127 for c in relative)):
        raise ValidationError(relative, 'unsafe-path')
    return relative


def safe_path(base, relative):
    """Reject traversal and symlinks, including ancestors, without following them."""
    relative = validate_relative(relative)
    base = Path(os.path.abspath(base))
    path = base / relative
    for part in (path, *path.parents):
        if part.is_symlink():
            raise ValidationError(relative, 'symlink')
    return path


def read_file(base, relative, limit=MAX_FILE):
    path = safe_path(base, relative)
    try:
        # O_NOFOLLOW also protects the leaf against replacement between checks.
        fd = os.open(path, os.O_RDONLY | os.O_NOFOLLOW | os.O_NONBLOCK)
        with os.fdopen(fd, 'rb') as stream:
            info = os.fstat(stream.fileno())
            if not stat.S_ISREG(info.st_mode):
                raise ValidationError(relative, 'not-regular-file')
            if info.st_size > limit:
                raise ValidationError(relative, 'file-limit')
            data = stream.read(limit + 1)
        if len(data) > limit:
            raise ValidationError(relative, 'file-limit')
        return data
    except OSError:
        raise ValidationError(relative, 'missing-or-unreadable') from None


def json_data(data, label):
    if len(data) > MAX_TEXT:
        raise ValidationError(label, 'text-limit')
    def unique(pairs):
        result = {}
        for key, value in pairs:
            if key in result:
                raise ValueError('duplicate')
            result[key] = value
        return result
    try:
        return json.loads(data, object_pairs_hook=unique,
                          parse_constant=lambda _: (_ for _ in ()).throw(ValueError('constant')))
    except (ValueError, UnicodeError, RecursionError):
        raise ValidationError(label, 'malformed-json') from None


def xml_data(data, label):
    if len(data) > MAX_TEXT:
        raise ValidationError(label, 'text-limit')
    # Reject declarations even in UTF-16/32; no DTD, entities or external loading.
    compact = data.replace(b'\x00', b'').upper()
    if b'<!DOCTYPE' in compact or b'<!ENTITY' in compact:
        raise ValidationError(label, 'xml-entity')
    try:
        root = ET.fromstring(data)
        pending = [(root, 0)]
        count = 0
        while pending:
            node, depth = pending.pop()
            count += 1
            if count > MAX_ENTRIES or depth > 64:
                raise ValidationError(label, 'xml-limit')
            node.tag = node.tag.rsplit('}', 1)[-1]
            pending.extend((child, depth + 1) for child in node)
        return root
    except (ET.ParseError, ValueError, RecursionError):
        raise ValidationError(label, 'malformed-xml') from None


def confirmed_ui_rights(provenance):
    if not isinstance(provenance, dict):
        return False
    owner = provenance.get('ownerReview')
    return (provenance.get('licenseStatus') == 'confirmed' and
            provenance.get('license') == 'Apache-2.0' and
            provenance.get('publicRedistribution') == 'approved' and isinstance(owner, dict) and
            owner.get('reviewer') == 'Said Elimam' and owner.get('rightsConfirmed') is True and
            owner.get('date') == '2026-11-17')


def validate_legal_document(relative, data, label):
    """Legal grants do not authorize release; validate bytes and owner attribution."""
    if len(data) > MAX_TEXT:
        raise ValidationError(label, 'text-limit')
    if relative in LICENSE_SHA256:
        if hashlib.sha256(data).hexdigest() != LICENSE_SHA256[relative]:
            raise ValidationError(label, 'license-text-sha256')
    elif relative == 'NOTICE':
        try:
            text = ' '.join(data.decode('utf-8').split())
        except UnicodeError:
            raise ValidationError(label, 'owner-notice-invalid') from None
        required = ('SIMT UI', 'Copyright Said Elimam', '2026-11-17',
                    'Apache License, Version 2.0', 'LICENSE-DOCS',
                    'Creative Commons Attribution 4.0', 'QuazarMedia/app-ui',
                    'app-ui/source-provenance.json', 'SIMT Cosmos by Said Elimam',
                    'https://simt.space/cosmos/design')
        if not all(value in text for value in required):
            raise ValidationError(label, 'owner-notice-invalid')
    elif relative == 'app-ui/source-provenance.json':
        provenance = json_data(data, label)
        if not confirmed_ui_rights(provenance):
            raise ValidationError(label, 'redistribution-rights-unconfirmed')
        files = provenance.get('files')
        commit = provenance.get('sourceCommit')
        if (provenance.get('sourceRepository') != 'QuazarMedia/app-ui' or
                not isinstance(commit, str) or not re.fullmatch(r'[0-9a-f]{40,64}', commit) or
                not isinstance(files, list) or not files):
            raise ValidationError(label, 'malformed-provenance')
        seen = set()
        for item in files:
            if (not isinstance(item, dict) or not isinstance(item.get('sourcePath'), str) or
                    not isinstance(item.get('sha256'), str) or not HEX.fullmatch(item['sha256'])):
                raise ValidationError(label, 'malformed-provenance')
            try:
                validate_relative(item['sourcePath'])
            except ValidationError:
                raise ValidationError(label, 'malformed-provenance') from None
            if item['sourcePath'] in seen:
                raise ValidationError(label, 'malformed-provenance')
            seen.add(item['sourcePath'])


def forbidden(label):
    parts = PurePosixPath(label).parts
    for part in parts:
        name = part.lower()
        if (name in FORBIDDEN_NAMES or name == '.env' or name.startswith('.env.') or
                name in ('.git', '.gradle', '.qoder', '.ssh', '.aws', '.gnupg', 'node_modules') or
                name.endswith(('.jks', '.keystore', '.p12', '.pfx', '.pem', '.key', '.mobileprovision')) or
                (name.startswith('service-account') and name.endswith('.json'))):
            return True
    return False


class Scanner:
    def __init__(self, audit):
        self.audit = audit
        self.entries = 0
        self.expanded = 0

    def scan(self, data, label, depth=0):
        if forbidden(label):
            self.audit.add(label, 'forbidden-file')
        self.entries += 1
        self.expanded += len(data)
        self.audit.scanned += 1
        if (len(data) > MAX_FILE or self.entries > MAX_ENTRIES or
                self.expanded > MAX_EXPANDED or depth > MAX_DEPTH):
            raise ValidationError(label, 'scan-limit')
        for category, pattern in PATTERNS.items():
            if pattern.search(data) or pattern.search(label.encode('utf-8', 'replace')):
                self.audit.add(label, category)
        # Filename mentions in prose and scanner regex definitions are not findings.
        name = label.lower()
        if name.endswith(ARCHIVE_EXT) or data.startswith(b'PK\x03\x04'):
            self.archive(data, label, depth)
        elif name.endswith(('.xml', '.pom')):
            document = xml_data(data, label)
            if name.endswith('androidmanifest.xml') and any(
                    node.tag.startswith('uses-permission') for node in document.iter()):
                self.audit.add(label, 'android-permission')
        elif name.endswith(('.json', '.module')):
            json_data(data, label)

    def archive(self, data, label, depth):
        if depth >= MAX_DEPTH:
            raise ValidationError(label, 'archive-depth')
        try:
            with zipfile.ZipFile(io.BytesIO(data)) as archive:
                infos = archive.infolist()
                if len(infos) + self.entries > MAX_ENTRIES:
                    raise ValidationError(label, 'archive-count')
                seen = set()
                for info in infos:
                    entry = info.filename.rstrip('/') if info.is_dir() else info.filename
                    validate_relative(entry)
                    if entry in seen:
                        raise ValidationError(label, 'archive-duplicate')
                    seen.add(entry)
                    mode = info.external_attr >> 16
                    if stat.S_ISLNK(mode) or stat.S_IFMT(mode) not in (0, stat.S_IFREG, stat.S_IFDIR):
                        raise ValidationError(label, 'archive-symlink-or-special')
                    if forbidden(entry):
                        self.audit.add(label, 'forbidden-file')
                    if (info.flag_bits & 1 or info.file_size > MAX_FILE or
                            info.file_size + self.expanded > MAX_EXPANDED or
                            info.file_size > max(1, info.compress_size) * 1000):
                        raise ValidationError(label, 'archive-limit')
                    if info.is_dir():
                        self.entries += 1
                        continue
                    with archive.open(info) as stream:
                        content = stream.read(MAX_FILE + 1)
                    if len(content) != info.file_size:
                        raise ValidationError(label, 'archive-size')
                    # Entry names are untrusted; only the audited outer path is diagnostic.
                    try:
                        self.scan(content, entry, depth + 1)
                    except ValidationError as error:
                        raise ValidationError(label, error.category) from None
                    # Coalesce nested findings at the outer selected path.
                    nested = {(p, c) for p, c in self.audit.findings if p == safe_label(entry)}
                    self.audit.findings.difference_update(nested)
                    self.audit.findings.update((safe_label(label), c) for _, c in nested)
        except ValidationError as error:
            raise ValidationError(label, error.category) from None
        except (OSError, ValueError, RuntimeError, NotImplementedError, zipfile.BadZipFile, EOFError):
            raise ValidationError(label, 'broken-archive') from None


def run_command(root, args, limit=MAX_COMMAND, timeout=120):
    """Fixed argv only; bounded output, discarded stderr, no shell or env overrides."""
    env = {k: v for k, v in os.environ.items() if not k.startswith(('GIT_', 'NODE_'))}
    env.update({'GIT_CONFIG_NOSYSTEM': '1', 'GIT_CONFIG_GLOBAL': os.devnull,
                'GIT_NO_REPLACE_OBJECTS': '1', 'GIT_TERMINAL_PROMPT': '0'})
    try:
        process = subprocess.Popen(args, cwd=root, stdout=subprocess.PIPE,
                                   stderr=subprocess.DEVNULL, stdin=subprocess.DEVNULL, env=env)
    except OSError:
        raise ValidationError('commands', 'command-unavailable') from None
    result = bytearray()
    deadline = time.monotonic() + timeout
    try:
        with selectors.DefaultSelector() as selector:
            selector.register(process.stdout, selectors.EVENT_READ)
            while True:
                remaining = deadline - time.monotonic()
                if remaining <= 0:
                    raise ValidationError('commands', 'command-timeout')
                if not selector.select(remaining):
                    raise ValidationError('commands', 'command-timeout')
                block = os.read(process.stdout.fileno(), 65536)
                if not block:
                    break
                result.extend(block)
                if len(result) > limit:
                    raise ValidationError('commands', 'command-limit')
        if process.wait(timeout=max(0.01, deadline - time.monotonic())) != 0:
            raise ValidationError('commands', 'command-failed')
        return bytes(result)
    except subprocess.TimeoutExpired:
        raise ValidationError('commands', 'command-timeout') from None
    finally:
        if process.poll() is None:
            process.kill()
        process.wait()
        process.stdout.close()


def git(root, *args, limit=MAX_COMMAND):
    return run_command(root, ['git', '--no-replace-objects', *args], limit=limit)


def source_files(root):
    data = git(root, 'ls-files', '--cached', '--others', '--exclude-standard', '-z')
    paths = sorted(set(p.decode('utf-8', 'surrogateescape') for p in data.split(b'\x00') if p))
    if len(paths) > MAX_OBJECTS:
        raise ValidationError('source', 'object-limit')
    return paths


def check_source(root):
    audit = Audit('source-safety')
    scanner = Scanner(audit)
    try:
        for relative in source_files(root):
            try:
                scanner.scan(read_file(root, relative), relative)
            except ValidationError as error:
                audit.catch(error)
        commits = git(root, 'rev-list', '--all').splitlines()
        if not commits or len(commits) > MAX_OBJECTS:
            raise ValidationError('history', 'missing-or-excessive-history')
        shallow = git(root, 'rev-parse', '--is-shallow-repository').strip()
        if shallow != b'false':
            raise ValidationError('history', 'shallow-history')
        seen = set()
        for commit in commits:
            if not re.fullmatch(rb'[0-9a-f]{40,64}', commit):
                raise ValidationError('history', 'invalid-object')
            tree = git(root, 'ls-tree', '-r', '-z', '--full-tree', commit.decode('ascii'))
            for record in tree.split(b'\x00'):
                if not record:
                    continue
                header, path = record.split(b'\t', 1)
                mode, kind, oid = header.split()
                label = 'history/' + path.decode('utf-8', 'surrogateescape')
                if forbidden(label):
                    audit.add(label, 'forbidden-file')
                if mode == b'120000' or kind != b'blob':
                    audit.add(label, 'history-symlink-or-submodule')
                    continue
                identity = (oid, path)
                if identity in seen:
                    continue
                seen.add(identity)
                if len(seen) > MAX_OBJECTS:
                    raise ValidationError('history', 'object-limit')
                try:
                    size = int(git(root, 'cat-file', '-s', oid.decode('ascii'), limit=128))
                    if size > MAX_FILE:
                        raise ValidationError(label, 'file-limit')
                    scanner.scan(git(root, 'cat-file', 'blob', oid.decode('ascii'), limit=MAX_FILE), label)
                except ValidationError as error:
                    audit.catch(error)
    except ValidationError as error:
        audit.catch(error)
    except (ValueError, UnicodeError):
        audit.add('history', 'invalid-object')
    return audit


def logical_name(artifact, version, key):
    classifier, extension = key
    return artifact + '-' + version + ('-' + classifier if classifier else '') + '.' + extension


def required_keys(artifact):
    primary = 'jar' if artifact == 'app-ui' else 'aar' if artifact == 'app-ui-android' else 'klib'
    result = {('', 'pom'), ('', 'module'), ('', primary), ('sources', 'jar'), ('javadoc', 'jar'), LEGAL_KEY}
    if artifact == 'app-ui':
        result.add(('kotlin-tooling-metadata', 'json'))
    if artifact.startswith('app-ui-ios'):
        result.update({('metadata', 'jar'), ('kotlin_resources', 'kotlin_resources.zip')})
    return result


class Publication:
    def __init__(self, repo, version, audit):
        self.repo = Path(repo)
        self.version = version
        self.audit = audit
        self.scanner = Scanner(audit)
        self.selected = {}
        self.modules = {}
        self.contents = {}
        self.legal_contents = None

    def read(self, relative):
        if relative not in self.contents:
            content = read_file(self.repo, relative)
            self.scanner.scan(content, relative)
            self.contents[relative] = content
            self.audit.record(relative, content)
            sidecar = relative + '.sha256'
            digest = read_file(self.repo, sidecar, 256)
            if digest.strip() != hashlib.sha256(content).hexdigest().encode('ascii'):
                raise ValidationError(relative, 'checksum-sidecar')
            self.audit.record(sidecar, digest)
        return self.contents[relative]

    def select(self, artifact):
        directory = 'app/simt/' + artifact + '/' + self.version + '/'
        keys = required_keys(artifact)
        if self.version.endswith('-SNAPSHOT'):
            label = directory + 'maven-metadata.xml'
            metadata = xml_data(self.read(label), label)
            if (metadata.tag != 'metadata' or metadata.findtext('groupId') != GROUP or
                    metadata.findtext('artifactId') != artifact or metadata.findtext('version') != self.version):
                raise ValidationError(label, 'metadata-gav')
            timestamp = metadata.findtext('versioning/snapshot/timestamp', '')
            build = metadata.findtext('versioning/snapshot/buildNumber', '')
            if not re.fullmatch(r'\d{8}\.\d{6}', timestamp) or not re.fullmatch(r'[1-9]\d*', build):
                raise ValidationError(label, 'snapshot-metadata')
            expected = self.version[:-len('SNAPSHOT')] + timestamp + '-' + build
            values = {}
            for entry in metadata.findall('versioning/snapshotVersions/snapshotVersion'):
                key = (entry.findtext('classifier', ''), entry.findtext('extension', ''))
                value = entry.findtext('value', '')
                if (key in values or key not in keys or value != expected or
                        not re.fullmatch(r'\d{14}', entry.findtext('updated', ''))):
                    raise ValidationError(label, 'snapshot-metadata')
                values[key] = value
            if set(values) != keys:
                raise ValidationError(label, 'snapshot-missing-classifier')
            self.selected[artifact] = {key: directory + logical_name(artifact, values[key], key) for key in keys}
        else:
            self.selected[artifact] = {key: directory + logical_name(artifact, self.version, key) for key in keys}
        for relative in sorted(self.selected[artifact].values()):
            self.read(relative)
        self.legal_notices(artifact)

    def legal_notices(self, artifact):
        label = self.selected[artifact][LEGAL_KEY]
        data = self.read(label)  # Scanner already enforced archive/path/expansion bounds.
        with zipfile.ZipFile(io.BytesIO(data)) as archive:
            if set(archive.namelist()) != LEGAL_ENTRIES:
                raise ValidationError(label, 'legal-notices-entries')
            for relative in sorted(LEGAL_ENTRIES):
                validate_legal_document(relative, archive.read(relative), label)
        if self.legal_contents is not None and data != self.legal_contents:
            raise ValidationError(label, 'legal-notices-mismatch')
        self.legal_contents = data

    def file_key(self, artifact, url, label):
        safe_path(self.repo, url)
        for key, relative in self.selected[artifact].items():
            if url in (logical_name(artifact, self.version, key), Path(relative).name):
                return key
        raise ValidationError(label, 'unselected-module-file')

    def link(self, source, target, link, label):
        if not isinstance(link, dict) or target not in TARGETS:
            raise ValidationError(label, 'available-at')
        expected = '../../' + target + '/' + self.version + '/' + logical_name(target, self.version, ('', 'module'))
        if (link.get('group') != GROUP or link.get('module') != target or
                link.get('version') != self.version or link.get('url') != expected or source == target):
            raise ValidationError(label, 'available-at')
        if target not in self.modules:
            raise ValidationError(label, 'missing-target-metadata')

    def pom(self, artifact):
        label = self.selected[artifact][('', 'pom')]
        pom = xml_data(self.read(label), label)
        if (pom.tag != 'project' or pom.findtext('groupId') != GROUP or
                pom.findtext('artifactId') != artifact or pom.findtext('version') != self.version):
            raise ValidationError(label, 'pom-gav')
        licenses = pom.findall('licenses')
        if (len(licenses) != 1 or len(licenses[0]) != 1 or licenses[0][0].tag != 'license' or
                len(licenses[0][0]) != len(POM_LICENSE) or
                {child.tag: child.text for child in licenses[0][0]} != POM_LICENSE):
            raise ValidationError(label, 'pom-license-metadata')
        if pom.find('parent') is not None or pom.find('repositories') is not None:
            raise ValidationError(label, 'private-dependency')
        for dependency in pom.findall('.//dependency'):
            validate_dependency(dependency.findtext('groupId'), dependency.findtext('artifactId'), label)
            if dependency.findtext('scope') == 'import' or dependency.findtext('systemPath'):
                raise ValidationError(label, 'forced-bom-or-system-dependency')

    def module(self, artifact):
        label = self.selected[artifact][('', 'module')]
        module = self.modules[artifact]
        component = module.get('component')
        if not isinstance(component, dict):
            raise ValidationError(label, 'module-gav')
        # Gradle KMP target metadata intentionally uses root GAV plus root backlink.
        target_component = artifact
        if artifact != 'app-ui' and component.get('url') is not None:
            target_component = 'app-ui'
            self.link(artifact, 'app-ui', component, label)
        if (component.get('group') != GROUP or component.get('module') != target_component or
                component.get('version') != self.version or module.get('formatVersion') != '1.1'):
            raise ValidationError(label, 'module-gav')
        variants = module.get('variants')
        if not isinstance(variants, list) or not variants:
            raise ValidationError(label, 'module-variants')
        names = set()
        advertised = set()
        linked = set()
        platform_seen = False
        for variant in variants:
            if not isinstance(variant, dict) or not isinstance(variant.get('name'), str) or variant['name'] in names:
                raise ValidationError(label, 'module-variants')
            names.add(variant['name'])
            attrs = variant.get('attributes', {})
            if not isinstance(attrs, dict):
                raise ValidationError(label, 'module-variants')
            if attrs.get('org.gradle.category') == 'enforced-platform':
                raise ValidationError(label, 'forced-bom')
            for dependency in variant.get('dependencies', []) + variant.get('dependencyConstraints', []):
                if not isinstance(dependency, dict):
                    raise ValidationError(label, 'private-dependency')
                validate_dependency(dependency.get('group'), dependency.get('module'), label)
                if dependency.get('attributes', {}).get('org.gradle.category') in ('platform', 'enforced-platform'):
                    raise ValidationError(label, 'forced-bom')
            if 'available-at' in variant:
                if artifact != 'app-ui' or variant.get('files'):
                    raise ValidationError(label, 'available-at')
                target = variant['available-at'].get('module') if isinstance(variant['available-at'], dict) else None
                self.link(artifact, target, variant['available-at'], label)
                matches = [v for v in self.modules[target].get('variants', []) if v.get('name') == variant['name']]
                if len(matches) != 1 or matches[0].get('attributes', {}) != attrs:
                    raise ValidationError(label, 'target-variant-mismatch')
                linked.add(target)
                continue
            if artifact == 'app-ui-android':
                platform_seen |= attrs.get('org.jetbrains.kotlin.platform.type') == 'androidJvm'
            elif artifact.startswith('app-ui-ios'):
                native = 'ios_arm64' if artifact == 'app-ui-iosarm64' else 'ios_simulator_arm64'
                platform_seen |= (attrs.get('org.jetbrains.kotlin.platform.type') == 'native' and
                                  attrs.get('org.jetbrains.kotlin.native.target') == native)
            else:
                platform_seen |= attrs.get('org.jetbrains.kotlin.platform.type') == 'common'
            files = variant.get('files', [])
            if not isinstance(files, list):
                raise ValidationError(label, 'module-files')
            for item in files:
                if not isinstance(item, dict) or not isinstance(item.get('url'), str) or not isinstance(item.get('name'), str):
                    raise ValidationError(label, 'module-files')
                safe_path(self.repo, item['name'])
                if forbidden(item['name']):
                    raise ValidationError(label, 'forbidden-file')
                key = self.file_key(artifact, item['url'], label)
                relative = self.selected[artifact][key]
                data = self.read(relative)
                if (type(item.get('size')) is not int or item['size'] != len(data) or
                        not isinstance(item.get('sha256'), str) or not HEX.fullmatch(item['sha256']) or
                        hashlib.sha256(data).hexdigest() != item['sha256']):
                    raise ValidationError(relative, 'checksum-or-size')
                for algorithm in ('sha512', 'sha1', 'md5'):
                    if algorithm in item and hashlib.new(algorithm, data).hexdigest() != item[algorithm]:
                        raise ValidationError(relative, 'checksum-or-size')
                advertised.add(key)
        must_advertise = required_keys(artifact) - {('', 'pom'), ('', 'module'), ('javadoc', 'jar'), ('kotlin-tooling-metadata', 'json'), LEGAL_KEY}
        if not must_advertise.issubset(advertised) or not platform_seen:
            raise ValidationError(label, 'missing-platform-or-files')
        if artifact == 'app-ui' and linked != set(TARGETS[1:]):
            raise ValidationError(label, 'missing-target')
        # Attached Maven javadoc and legal-notices artifacts need no Gradle variant.
        docs = self.selected[artifact][('javadoc', 'jar')]
        with zipfile.ZipFile(io.BytesIO(self.read(docs))) as archive:
            html = [i for i in archive.infolist() if i.filename.endswith('.html') and not i.is_dir()]
            pages = [archive.read(i) for i in html]
            if not pages or not any(re.search(rb'<html\b', p, re.I) and re.search(rb'<body\b', p, re.I) and
                                    re.search(rb'(?:dokka|app\.shared\.ui|app/shared/ui)', p, re.I) for p in pages):
                raise ValidationError(docs, 'missing-generated-html-docs')
        sources = self.selected[artifact][('sources', 'jar')]
        with zipfile.ZipFile(io.BytesIO(self.read(sources))) as archive:
            if not any(i.filename.endswith('.kt') and i.file_size > 0 for i in archive.infolist()):
                raise ValidationError(sources, 'empty-sources')


def validate_dependency(group, module, label):
    if not isinstance(group, str) or not isinstance(module, str) or not module:
        raise ValidationError(label, 'private-dependency')
    public = group in ('org.jetbrains.kotlin', 'org.jetbrains.kotlinx') or group.startswith(('org.jetbrains.compose.', 'androidx.'))
    banned = re.search(r'(?:bom|ktor|okhttp|retrofit|firebase|hilt|room|navigation|play-services|simt|host|network)', group + ':' + module, re.I)
    if not public or banned or not re.fullmatch(r'[A-Za-z0-9_.-]+', group + module):
        raise ValidationError(label, 'private-or-network-dependency')


def check_publication(repo, version):
    audit = Audit('private-publication-quality')
    if not isinstance(version, str) or not VERSION.fullmatch(version):
        audit.add('version', 'invalid-version')
        return audit
    publication = Publication(repo, version, audit)
    for artifact in TARGETS:
        try:
            publication.select(artifact)
            label = publication.selected[artifact][('', 'module')]
            module = json_data(publication.read(label), label)
            if not isinstance(module, dict):
                raise ValidationError(label, 'module-structure')
            publication.modules[artifact] = module
        except ValidationError as error:
            audit.catch(error)
    for artifact in TARGETS:
        if artifact not in publication.modules:
            continue
        try:
            publication.pom(artifact)
            publication.module(artifact)
        except ValidationError as error:
            audit.catch(error)
        except (TypeError, KeyError, AttributeError, ValueError, zipfile.BadZipFile):
            audit.add('app/simt/' + artifact + '/' + version, 'malformed-publication')
    return audit


def load_approval(approval):
    if approval is None:
        raise ValidationError('approval', 'missing-owner-approval')
    path = Path(os.path.abspath(approval))
    return json_data(read_file(path.parent, path.name, MAX_TEXT), 'approval')


def check_release_gates(root, version, approval):
    """Early authorization blockers; not a substitute for check_release/stage."""
    audit = Audit('release-owner-gates')
    head = None
    review = None
    if not isinstance(version, str) or not STABLE.fullmatch(version):
        audit.add('version', 'stable-semver-required')
    try:
        head = git(root, 'rev-parse', '--verify', 'HEAD').strip().decode('ascii')
        if not re.fullmatch(r'[0-9a-f]{40,64}', head):
            raise ValidationError('HEAD', 'invalid-head')
        if git(root, 'status', '--porcelain=v1', '-z', '--untracked-files=normal'):
            audit.add('source', 'dirty-tree')
        if STABLE.fullmatch(version or ''):
            try:
                tagged = git(root, 'rev-parse', '--verify', 'refs/tags/v' + version + '^{commit}').strip().decode('ascii')
                if tagged != head:
                    audit.add('HEAD', 'matching-version-tag-required')
            except ValidationError:
                audit.add('HEAD', 'matching-version-tag-required')
    except (ValidationError, UnicodeError) as error:
        if isinstance(error, ValidationError):
            audit.catch(error)
        else:
            audit.add('HEAD', 'invalid-head')
    try:
        review = load_approval(approval)
        owner = review.get('ownerReview', {}) if isinstance(review, dict) else {}
        keys = ('approved', 'sourceCodeRights', 'specCodeRights', 'specDocumentRights',
                'unsignedLocalStage', 'signingSeparateOwnerRequirement')
        if (not isinstance(review, dict) or review.get('schemaVersion') != 1 or not head or
                review.get('head') != head or review.get('version') != version or not isinstance(owner, dict) or
                not all(owner.get(k) is True for k in keys) or not isinstance(owner.get('reviewer'), str) or
                not owner['reviewer'].strip()):
            audit.add('approval', 'owner-approval-unconfirmed-or-stale')
    except ValidationError as error:
        audit.catch(error)
    except (AttributeError, TypeError):
        audit.add('approval', 'malformed-owner-approval')
    for relative, kind in (('app-ui/source-provenance.json', 'source'), ('cosmos-spec/provenance.json', 'cosmos')):
        try:
            data = read_file(root, relative, MAX_TEXT)
            provenance = json_data(data, relative)
            if kind == 'source':
                confirmed = confirmed_ui_rights(provenance)
                digest_key = 'sourceSha256'
            else:
                confirmed = (provenance.get('licenses', {}).get('code', {}).get('status') == 'confirmed' and
                             provenance.get('licenses', {}).get('documents', {}).get('status') == 'confirmed' and
                             provenance.get('distribution', {}).get('scope') == 'public-redistribution' and
                             provenance.get('distribution', {}).get('publicRedistribution') == 'approved')
                digest_key = 'cosmosSha256'
                if provenance.get('knownSchemaDefects') != []:
                    audit.add(relative, 'canonical-schema-blocked')
            if not confirmed:
                audit.add(relative, 'redistribution-rights-unconfirmed')
            if not isinstance(review, dict) or review.get('provenance', {}).get(digest_key) != hashlib.sha256(data).hexdigest():
                audit.add(relative, 'provenance-owner-review-required')
        except ValidationError as error:
            audit.catch(error)
        except (AttributeError, TypeError):
            audit.add(relative, 'malformed-provenance')
    for kind, checks in EVIDENCE_CHECKS.items():
        try:
            reference = review.get('evidence', {}).get(kind) if isinstance(review, dict) else None
            if not isinstance(reference, dict) or not isinstance(reference.get('path'), str):
                raise ValidationError('evidence/' + kind, 'missing-head-tied-evidence')
            data = read_file(root, reference['path'], MAX_TEXT)
            evidence = json_data(data, 'evidence/' + kind)
            if (reference.get('sha256') != hashlib.sha256(data).hexdigest() or not head or
                    evidence.get('head') != head or evidence.get('result') != 'passed' or
                    not all(evidence.get('checks', {}).get(c) is True for c in checks)):
                raise ValidationError('evidence/' + kind, 'failed-or-stale-evidence')
            if kind == 'iosRuntime' and (evidence.get('platform') != 'ios' or
                                        not isinstance(evidence.get('runtime'), str) or not evidence['runtime'].strip()):
                raise ValidationError('evidence/' + kind, 'ios-runtime-required')
            if kind == 'androidFloorHaptics' and (evidence.get('platform') != 'android' or
                    evidence.get('physicalDevice') is not True or type(evidence.get('apiLevel')) is not int or evidence['apiLevel'] != 28):
                raise ValidationError('evidence/' + kind, 'physical-android-floor-haptics-required')
            if kind == 'consumerIntegration' and not all(evidence.get('consumers', {}).get(c) is True for c in
                                                       ('simt-app', 'simt-echo-android', 'simt-echo-ios')):
                raise ValidationError('evidence/' + kind, 'consumer-integration-required')
        except ValidationError as error:
            audit.catch(error)
        except (AttributeError, TypeError):
            audit.add('evidence/' + kind, 'malformed-evidence')
    try:
        # Release requires a fresh strict canonical-schema pass, not an integrity claim.
        script = safe_path(root, 'cosmos-spec/scripts/check.js')
        read_file(root, 'cosmos-spec/scripts/check.js', MAX_TEXT)
        run_command(root, ['node', str(script), 'canonical-schema'])
    except ValidationError:
        audit.add('cosmos-spec', 'canonical-schema-check-failed')
    return audit


def release_documents(root, audit):
    scanner = Scanner(audit)
    paths = set(RELEASE_DOCS)
    try:
        tracked = source_files(root)
        paths.update(p for p in tracked if p.startswith('docs/') and p.endswith('.md'))
        api = [p for p in tracked if p.startswith('app-ui/api/') and p.endswith('.api')]
        if not api:
            audit.add('app-ui/api', 'missing-abi-baseline')
        paths.update(api)
        provenance = json_data(read_file(root, 'cosmos-spec/provenance.json', MAX_TEXT), 'cosmos-spec/provenance.json')
        paths.update(('app-ui/source-provenance.json', 'cosmos-spec/provenance.json'))
        inventory = provenance.get('inventory')
        if not isinstance(inventory, list) or not inventory:
            raise ValidationError('cosmos-spec/provenance.json', 'missing-spec-inventory')
        expected = {}
        for item in inventory:
            relative = 'cosmos-spec/' + item['path']
            safe_path(root, relative)
            if relative in expected or not HEX.fullmatch(item['sha256']):
                raise ValidationError('cosmos-spec/provenance.json', 'malformed-spec-inventory')
            expected[relative] = item['sha256']
            paths.add(relative)
        for relative in sorted(paths):
            try:
                data = read_file(root, relative)
                if not data.strip():
                    raise ValidationError(relative, 'empty-release-document')
                if relative in expected and hashlib.sha256(data).hexdigest() != expected[relative]:
                    raise ValidationError(relative, 'spec-inventory-checksum')
                scanner.scan(data, relative)
                if relative in LEGAL_ENTRIES:
                    validate_legal_document(relative, data, relative)
                audit.record('source/' + relative, data)
            except ValidationError as error:
                audit.catch(error)
    except ValidationError as error:
        audit.catch(error)
    except (AttributeError, KeyError, TypeError):
        audit.add('cosmos-spec/provenance.json', 'malformed-spec-inventory')


def check_release(root, repo, version, approval):
    audit = Audit('release-authorization-unsigned-only')
    audit.merge(check_release_gates(root, version, approval))
    audit.merge(check_source(root))
    publication = check_publication(repo, version)
    audit.merge(publication)
    audit.inventory.update(('maven/' + p, value) for p, value in publication.inventory.items())
    release_documents(root, audit)
    try:
        review = load_approval(approval)
        artifacts = review.get('artifacts')
        expected = [{'path': p, **audit.inventory[p]} for p in sorted(audit.inventory)]
        if not isinstance(artifacts, list) or artifacts != expected or not expected:
            audit.add('approval', 'exact-audited-artifact-review-required')
    except ValidationError as error:
        audit.catch(error)
    except AttributeError:
        audit.add('approval', 'malformed-owner-approval')
    return audit


def integration_state(root):
    head = git(root, 'rev-parse', '--verify', 'HEAD').strip().decode('ascii')
    if not re.fullmatch(r'[0-9a-f]{40,64}', head):
        raise ValidationError('HEAD', 'invalid-head')
    files = [{'path': p, 'sha256': hashlib.sha256(read_file(root, p)).hexdigest()}
             for p in sorted(source_files(root))]
    return {'head': head, 'sourceSha256': hashlib.sha256(json.dumps(files, sort_keys=True).encode()).hexdigest(),
            'dirtyTree': bool(git(root, 'status', '--porcelain=v1', '-z', '--untracked-files=normal'))}


def stage_integration(root, repo, version, output):
    audit = Audit('private-integration-stage')
    try:
        state = integration_state(root)
        audit.merge(check_source(root))
        publication = check_publication(repo, version)
        audit.merge(publication)
        audit.inventory.update(('maven/' + p, value) for p, value in publication.inventory.items())
        scanner = Scanner(audit)
        for relative in sorted(LEGAL_ENTRIES | {'README.md', 'app-ui/Module.md'}):
            data = read_file(root, relative)
            scanner.scan(data, relative)
            if relative in LEGAL_ENTRIES:
                validate_legal_document(relative, data, relative)
            audit.record('source/' + relative, data)
        manifest = {'unsigned': True, 'purpose': 'private-consumer-integration',
                    'publicRedistributionApproved': False, 'runtimeValidationApproved': False,
                    'signing': 'separate-release-owner-requirement', **state}

        def final_check():
            result = check_source(root)
            if integration_state(root) != state:
                result.add('source', 'stage-source-changed')
            return result

        return write_stage(root, repo, version, audit, output, manifest, final_check)
    except ValidationError as error:
        audit.catch(error)
    return audit


def stage_release(root, repo, version, approval, output):
    audit = check_release(root, repo, version, approval)
    audit.scope = 'unsigned-local-stage'
    manifest = {'unsigned': True, 'signing': 'separate-release-owner-requirement'}
    return write_stage(root, repo, version, audit, output, manifest,
                       lambda: check_release_gates(root, version, approval))


def write_stage(root, repo, version, audit, output, manifest, final_check):
    if not audit.ok:
        return audit
    try:
        output = Path(os.path.abspath(output))
        # Local output must stay under the source workspace and cannot replace files.
        try:
            relative = output.relative_to(Path(os.path.abspath(root))).as_posix()
        except ValueError:
            raise ValidationError('stage', 'unsafe-output') from None
        destination = safe_path(root, relative)
        if destination.exists() or not destination.parent.is_dir() or destination.suffix != '.zip':
            raise ValidationError('stage', 'output-exists-or-invalid')
        buffer = io.BytesIO()
        with zipfile.ZipFile(buffer, 'w', compression=zipfile.ZIP_STORED) as archive:
            entries = {}
            for name, checksum in sorted(audit.inventory.items()):
                base, path = (repo, name[6:]) if name.startswith('maven/') else (root, name[7:])
                data = read_file(base, path)
                if len(data) != checksum['size'] or hashlib.sha256(data).hexdigest() != checksum['sha256']:
                    raise ValidationError(name, 'stage-input-changed')
                entries[name] = data
            entries['artifact-checksums.json'] = (json.dumps(
                {**manifest, 'version': version,
                 'artifacts': [{'path': p, **audit.inventory[p]} for p in sorted(audit.inventory)]},
                sort_keys=True, separators=(',', ':')) + '\n').encode('utf-8')
            if sum(len(d) for d in entries.values()) > MAX_EXPANDED:
                raise ValidationError('stage', 'stage-limit')
            for name, data in sorted(entries.items()):
                info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
                info.create_system = 3
                info.external_attr = (stat.S_IFREG | 0o644) << 16
                archive.writestr(info, data)
        # Recheck the applicable gates before any candidate exists.
        audit.merge(final_check())
        if not audit.ok:
            return audit
        with destination.open('xb') as stream:
            stream.write(buffer.getvalue())
        audit.inventory = {relative: {'size': buffer.tell(), 'sha256': hashlib.sha256(buffer.getvalue()).hexdigest()}}
    except ValidationError as error:
        audit.catch(error)
    except OSError:
        audit.add('stage', 'stage-write-failed')
    return audit


class SafeParser(argparse.ArgumentParser):
    def error(self, message):
        raise ValidationError('cli', 'invalid-arguments')


def parser():
    result = SafeParser(description='Offline SIMT UI source/publication audit; fail-closed release authorization. No signing/upload.',
                        epilog='Exit 0: validated; exit 1: blocked. No force/environment bypass. Publication quality is not release approval.')
    sub = result.add_subparsers(dest='command', required=True, parser_class=SafeParser)
    source = sub.add_parser('source', help='Audit Git working files and every committed blob across all refs (ignored files excluded).')
    source.add_argument('--root', required=True, help='Source Git workspace.')
    publication = sub.add_parser('publication', help='Audit only the four exact app.simt app-ui publications for VERSION_NAME.')
    publication.add_argument('--repo', required=True, help='Local Maven repository, normally build/maven-repository.')
    publication.add_argument('--version', required=True, help='Exact VERSION_NAME; stable x.y.z or x.y.z-SNAPSHOT.')
    integration = sub.add_parser('stage-integration', help='Create an audited private-consumer ZIP before integration; never public release approval.')
    integration.add_argument('--root', required=True, help='Source Git workspace; source state is fingerprinted and rechecked.')
    integration.add_argument('--repo', required=True, help='Audited local Maven repository.')
    integration.add_argument('--version', required=True, help='Exact stable or snapshot VERSION_NAME.')
    integration.add_argument('--output', required=True, help='New private .zip inside the workspace; no signing/upload.')
    for name in ('release', 'stage'):
        command = sub.add_parser(name, help='Require owner rights/review, stable tag, clean tree, HEAD evidence, strict Cosmos and fresh artifacts.' +
                                 (' Create local unsigned ZIP only after all gates.' if name == 'stage' else ''),
                                 epilog='Approval schema and required evidence: README.md and EVIDENCE_CHECKS. Missing approval always blocks.')
        command.add_argument('--root', required=True, help='Source Git workspace.')
        command.add_argument('--repo', required=True, help='Fresh local Maven repository.')
        command.add_argument('--version', required=True, help='Stable exact VERSION_NAME x.y.z; tag must be vVERSION at HEAD.')
        command.add_argument('--approval', help='Explicit existing owner-review JSON; never generated here.')
        if name == 'stage':
            command.add_argument('--output', required=True, help='New .zip under an existing workspace directory; not uploaded or signed.')
    return result


def main(argv=None):
    try:
        args = parser().parse_args(argv)
        if args.command == 'source':
            audit = check_source(args.root)
        elif args.command == 'publication':
            audit = check_publication(args.repo, args.version)
        elif args.command == 'release':
            audit = check_release(args.root, args.repo, args.version, args.approval)
        elif args.command == 'stage-integration':
            audit = stage_integration(args.root, args.repo, args.version, args.output)
        else:
            audit = stage_release(args.root, args.repo, args.version, args.approval, args.output)
    except ValidationError as error:
        audit = Audit('validation')
        audit.catch(error)
    except (OSError, ValueError, TypeError, AttributeError, RecursionError):
        audit = Audit('validation')
        audit.add('validation', 'malformed-or-unreadable-input')
    print(json.dumps(audit.payload(), sort_keys=True, separators=(',', ':')))
    return 0 if audit.ok else 1


if __name__ == '__main__':
    raise SystemExit(main())
