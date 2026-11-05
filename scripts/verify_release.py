#!/usr/bin/env python3
"""Verify a handoff APK without reading a keystore or changing a device.

Requires the intended public certificate SHA-256. This is artifact verification,
not install/UI/SDK acceptance or proof of the caller-declared source commit.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SIGNING_ENV = {
    'MEMORIDE_KEYSTORE_FILE', 'MEMORIDE_KEYSTORE_PASSWORD',
    'MEMORIDE_KEY_ALIAS', 'MEMORIDE_KEY_PASSWORD',
}
DEBUG_CLASSES = (
    'io/kailand/app/DevelopmentActivity', 'io/kailand/app/DevelopmentGameActivity',
    'io/kailand/app/PleosProbeActivity', 'io/kailand/feature/island/AvatarRuntimePreviewKt',
    'androidx/compose/ui/tooling/PreviewActivity',
)


def fingerprint(text: str) -> str:
    value = re.sub(r'[:\s]', '', text).lower()
    if not re.fullmatch(r'[0-9a-f]{64}', value):
        raise ValueError('Expected certificate SHA-256 must contain exactly 64 hexadecimal digits.')
    return value


def certificate(stdout: str, expected: str) -> str:
    if re.search(r'certificate DN:.*CN\s*=\s*Android Debug(?:\s*[,\n]|$)', stdout, re.I):
        raise ValueError('Android debug certificate is not a team release certificate.')
    digests = re.findall(r'^Signer #\d+ certificate SHA-256 digest:\s*([0-9a-fA-F]+)\s*$', stdout, re.M)
    if len(digests) != 1:
        raise ValueError('Exactly one APK signer is required for this prototype handoff.')
    actual = fingerprint(digests[0])
    if actual != fingerprint(expected):
        raise ValueError('APK certificate does not match the intended team certificate.')
    if not re.search(r'^Verified using v(?:2|3|3\.1) scheme[^\n]*:\s*true\s*$', stdout, re.M):
        raise ValueError('A verified APK v2 or v3 signature is required.')
    return actual


def package_info(stdout: str, toolchain: dict) -> dict:
    line = re.search(r'^package:\s*(.*)$', stdout, re.M)
    values = dict(re.findall(r"(\w+)='([^']*)'", line[1] if line else ''))
    if values.get('name') != toolchain['applicationId']:
        raise ValueError('APK applicationId does not match the project.')
    if 'application-debuggable' in stdout:
        raise ValueError('A debuggable APK cannot be handed off as release.')
    launchers = re.findall(r"^launchable-activity: name='([^']+)'", stdout, re.M)
    if launchers != ['io.kailand.app.MainActivity']:
        raise ValueError('Release must expose only the regular MainActivity launcher.')
    minimum = re.search(r"^(?:minSdkVersion|sdkVersion):'(\d+)'", stdout, re.M)
    target = re.search(r"^targetSdkVersion:'(\d+)'", stdout, re.M)
    if not minimum or int(minimum[1]) != toolchain['minSdk'] or not target or int(target[1]) != toolchain['targetSdk']:
        raise ValueError('APK min/target SDK does not match the recorded toolchain.')
    version = values.get('versionCode', '')
    if not version.isdecimal() or not 1 <= int(version) <= 2_100_000_000:
        raise ValueError('APK versionCode is invalid.')
    native = re.search(r'^native-code:\s*(.*)$', stdout, re.M)
    return {
        'application_id': values['name'], 'version_code': int(version),
        'version_name': values.get('versionName', ''),
        'min_sdk': int(minimum[1]), 'target_sdk': int(target[1]),
        'native_abis': re.findall(r"'([^']+)'", native[1]) if native else [],
    }


def packaged_assets(apk: Path) -> None:
    with zipfile.ZipFile(apk) as archive:
        required = {'AndroidManifest.xml', 'assets/content/catalog.json'}
        if not required.issubset(archive.namelist()):
            raise ValueError('APK is missing the regular manifest or packaged Catalog.')
        if not any(name.endswith('.dex') for name in archive.namelist()):
            raise ValueError('APK contains no executable DEX.')
        for name in archive.namelist():
            if name.endswith('.dex'):
                dex = archive.read(name)
                if any(('L' + item + ';').encode() in dex for item in DEBUG_CLASSES):
                    raise ValueError('APK contains a debug-only activity or gallery.')


def run_tool(command: list[str], label: str) -> str:
    # Signature verification does not need the signing operator's private environment.
    environment = {k: v for k, v in os.environ.items() if k not in SIGNING_ENV}
    try:
        result = subprocess.run(command, capture_output=True, text=True, encoding='utf-8',
                                errors='replace', timeout=120, env=environment)
    except (OSError, subprocess.TimeoutExpired) as error:
        raise ValueError(f'{label} could not run; check the recorded local toolchain.') from error
    if result.returncode:
        # Never echo arbitrary certificate subjects, local paths or tool diagnostics into a handoff.
        raise ValueError(f'{label} failed (exit {result.returncode}); release verification did not pass.')
    return result.stdout


def verify(apk: Path, build_tools: Path, expected: str, source_sha: str) -> dict:
    expected = fingerprint(expected)
    if not re.fullmatch(r'[0-9a-f]{40}', source_sha):
        raise ValueError('Declare the full 40-character source Git SHA.')
    if not apk.is_file():
        raise ValueError('APK file does not exist.')
    toolchain = json.loads((ROOT / 'config/toolchain.json').read_text(encoding='utf-8'))
    java = Path(os.environ['JAVA_HOME']) / 'bin' / ('java.exe' if os.name == 'nt' else 'java') if os.environ.get('JAVA_HOME') else Path('java')
    # java -version writes to stderr, unlike apksigner/aapt2.
    environment = {k: v for k, v in os.environ.items() if k not in SIGNING_ENV}
    result = subprocess.run([str(java), '-version'], capture_output=True, text=True, timeout=20, env=environment)
    match = re.search(r'version "(\d+)', result.stderr + result.stdout)
    if result.returncode or not match or int(match[1]) != toolchain['javaMajor']:
        raise ValueError('JAVA_HOME must use the recorded JDK major version.')
    signer = build_tools / 'lib' / 'apksigner.jar'
    aapt = build_tools / ('aapt2.exe' if os.name == 'nt' else 'aapt2')
    signature = run_tool([str(java), '-jar', str(signer), 'verify', '--verbose', '--print-certs',
                          '--min-sdk-version', str(toolchain['minSdk']), str(apk)], 'apksigner verify')
    actual = certificate(signature, expected)
    info = package_info(run_tool([str(aapt), 'dump', 'badging', str(apk)], 'aapt2 dump badging'), toolchain)
    packaged_assets(apk)
    return {
        'schema_version': 1, 'apk_filename': apk.name,
        'apk_sha256': hashlib.sha256(apk.read_bytes()).hexdigest(),
        'signer_certificate_sha256': actual, 'artifact_verification': 'PASS',
        'declared_source_sha': source_sha, 'source_provenance': 'operator-declared, not verified from APK',
        **info, 'device_install_ui_sdk_acceptance': 'NOT RUN',
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk', type=Path)
    parser.add_argument('--build-tools', required=True, type=Path)
    parser.add_argument('--expected-cert-sha256', required=True)
    parser.add_argument('--source-sha', required=True)
    parser.add_argument('--report', type=Path, help='New public metadata JSON; refuses to overwrite an existing report.')
    args = parser.parse_args()
    try:
        result = verify(args.apk, args.build_tools, args.expected_cert_sha256, args.source_sha)
        data = json.dumps(result, indent=2) + '\n'
        if args.report:
            with args.report.open('x', encoding='utf-8', newline='\n') as handle:
                handle.write(data)
        print(data, end='')
        return 0
    except ValueError as error:
        print(f'Release verification FAILED: {error} No PASS report was issued.', file=sys.stderr)
        return 2
    except (OSError, zipfile.BadZipFile, subprocess.TimeoutExpired):
        print('Release verification FAILED. Check signing identity, package and recorded tools; no PASS report was issued.', file=sys.stderr)
        return 2


if __name__ == '__main__':
    raise SystemExit(main())
