#!/usr/bin/env python3
"""Run personally in a local terminal: hidden credentials -> build -> verified candidate.

Never run this interactive command through an AI console. No key is created and
no APK is installed. Existing team key identity must be confirmed beforehand.
"""
from __future__ import annotations

import argparse
from datetime import datetime, timedelta, timezone
import getpass
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys

import verify_release

ROOT = Path(__file__).resolve().parents[1]
BUILD_INPUTS = ['app', 'build.gradle.kts', 'settings.gradle.kts', 'gradle.properties',
                'gradle', 'content', 'assets', 'fixtures', 'config', 'scripts']


def source_sha() -> str:
    dirty = subprocess.run(['git', 'diff', '--quiet', 'HEAD', '--', *BUILD_INPUTS], cwd=ROOT)
    untracked = subprocess.check_output(['git', 'ls-files', '--others', '--exclude-standard',
                                        '--', *BUILD_INPUTS], cwd=ROOT)
    if dirty.returncode or untracked.strip():
        raise ValueError('Commit build-input changes before producing a candidate. Unrelated documents may remain.')
    return subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()


def version_code(text: str) -> int:
    try:
        value = int(text)
    except ValueError as error:
        raise argparse.ArgumentTypeError('version-code must be an integer.') from error
    if not 1 <= value <= 2_100_000_000:
        raise argparse.ArgumentTypeError('version-code must be in 1..2100000000.')
    return value


def build(version: int, key_file: Path | None = None, alias: str | None = None) -> None:
    """Credentials enter only this process and the one Gradle child, never command arguments."""
    key_file = key_file or Path(input('Existing confirmed team keystore path: ').strip()).expanduser()
    alias = alias or input('Team key alias: ').strip()
    if not key_file.is_file() or not alias or alias.casefold() == 'androiddebugkey':
        raise ValueError('An existing team key file and non-debug alias are required.')
    environment = dict(os.environ)
    try:
        environment.update({
            'MEMORIDE_KEYSTORE_FILE': str(key_file.resolve()),
            'MEMORIDE_KEY_ALIAS': alias,
            'MEMORIDE_KEYSTORE_PASSWORD': getpass.getpass('Keystore password (hidden): '),
            'MEMORIDE_KEY_PASSWORD': getpass.getpass('Key password (hidden): '),
        })
        if any(not environment[name] for name in verify_release.SIGNING_ENV):
            raise ValueError('All signing fields are required; no build was started.')
        wrapper = ['cmd.exe', '/d', '/c', 'gradlew.bat'] if os.name == 'nt' else ['sh', './gradlew']
        result = subprocess.run([*wrapper, '--no-daemon', '--no-configuration-cache',
                                 ':app:assembleRelease', f'-PmemorideVersionCode={version}'],
                                cwd=ROOT, env=environment)
        if result.returncode:
            raise ValueError('Signed build failed; no handoff report was issued.')
    finally:
        for name in verify_release.SIGNING_ENV:
            environment.pop(name, None)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--version-code', required=True, type=version_code)
    parser.add_argument('--build-tools', required=True, type=Path)
    parser.add_argument('--expected-cert-sha256', required=True)
    parser.add_argument('--output-dir', type=Path, default=ROOT / 'tmp/a05-candidates')
    parser.add_argument('--keystore-file', type=Path, help='Local PKCS12 path only; never pass passwords as arguments.')
    parser.add_argument('--key-alias', help='Local non-debug alias.')
    args = parser.parse_args()
    try:
        if not sys.stdin.isatty():
            raise ValueError('Run personally in a local interactive terminal; do not send credentials to an AI console.')
        expected = verify_release.fingerprint(args.expected_cert_sha256)
        if not (args.build_tools / 'lib/apksigner.jar').is_file():
            raise ValueError('Provide the local Android SDK Build Tools directory.')
        initial_sha = source_sha()
        build(args.version_code, args.keystore_file, args.key_alias)
        if source_sha() != initial_sha:
            raise ValueError('Build inputs or Git HEAD changed during the build; no candidate was issued.')
        apk = ROOT / 'app/build/outputs/apk/release/app-release.apk'
        metadata = verify_release.verify(apk, args.build_tools, expected, initial_sha)
        if metadata['version_code'] != args.version_code:
            raise ValueError('Built APK version does not match this candidate request.')
        stamp = datetime.now(timezone(timedelta(hours=9))).strftime('%Y%m%d_%H%M%S_KST')
        folder = args.output_dir / f'MEMORIDE_A05_v{args.version_code}_{initial_sha[:8]}_{stamp}'
        folder.mkdir(parents=True, exist_ok=False)
        target = folder / 'MEMORIDE.apk'
        shutil.copyfile(apk, target)
        if hashlib.sha256(target.read_bytes()).hexdigest() != metadata['apk_sha256']:
            raise ValueError('Candidate copy changed; no PASS report was issued.')
        metadata['apk_filename'] = target.name
        with (folder / 'RELEASE.json').open('x', encoding='utf-8', newline='\n') as handle:
            json.dump(metadata, handle, indent=2)
            handle.write('\n')
        print(f'Verified signed candidate created: {folder}')
        print('C PC installation, upgrade/save preservation and final UI acceptance remain NOT RUN.')
        return 0
    except (ValueError, OSError, subprocess.SubprocessError) as error:
        if isinstance(error, ValueError):
            print(str(error), file=sys.stderr)
        else:
            print('Local build/verification could not finish. No candidate acceptance is claimed.', file=sys.stderr)
        return 2


if __name__ == '__main__':
    raise SystemExit(main())
