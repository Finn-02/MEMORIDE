#!/usr/bin/env python3
"""Personal local terminal only: initialize a confirmed first team key, then build.

Keytool handles all passwords directly on the operator's console. This script
never receives a password, reads private-key bytes, or sends credentials to AI.
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

import build_signed_release
import verify_release

ROOT = Path(__file__).resolve().parents[1]
ALIAS = 'memoride-release'
STATUS = ROOT / 'tmp/a05/PRIVATE_STEP_STATUS.json'


def locations(folder: Path, resume: bool) -> tuple[Path, Path]:
    folder = folder.expanduser().resolve()
    if any((parent / '.git').exists() for parent in (folder, *folder.parents)):
        raise ValueError('Store the team key outside every Git working tree.')
    key = folder / 'memoride-release.p12'
    cert = folder / 'memoride-release.cer'
    if resume:
        if not key.is_file():
            raise ValueError('No existing initialized key is available to resume.')
    elif key.exists() or cert.exists():
        raise ValueError('Existing signing material found; nothing will be overwritten. Use the existing-key flow.')
    return key, cert


def status(phase: str, **fields) -> None:
    STATUS.parent.mkdir(parents=True, exist_ok=True)
    temporary = STATUS.with_suffix('.tmp')
    temporary.write_text(json.dumps({'phase': phase, **fields}, indent=2) + '\n', encoding='utf-8')
    temporary.replace(STATUS)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--confirmed-no-existing-key', action='store_true')
    group.add_argument('--resume-existing', action='store_true')
    parser.add_argument('--build-tools', required=True, type=Path)
    parser.add_argument('--key-directory', type=Path,
                        default=Path(os.environ.get('LOCALAPPDATA', str(Path.home() / '.local/share'))) / 'MEMORIDE/signing')
    parser.add_argument('--version-code', type=build_signed_release.version_code, default=2)
    args = parser.parse_args()
    try:
        if not sys.stdin.isatty():
            raise ValueError('Use your personal interactive terminal, never the AI console.')
        source = build_signed_release.source_sha()
        key, cert = locations(args.key_directory, args.resume_existing)
        java_home = Path(os.environ.get('JAVA_HOME', ''))
        java = java_home / 'bin' / ('java.exe' if os.name == 'nt' else 'java')
        keytool = java_home / 'bin' / ('keytool.exe' if os.name == 'nt' else 'keytool')
        if not java.is_file() or not keytool.is_file():
            raise ValueError('Set JAVA_HOME to the recorded local JDK before starting.')
        version = subprocess.run([str(java), '-version'], capture_output=True, text=True)
        expected = json.loads((ROOT / 'config/toolchain.json').read_text())['javaMajor']
        if not re.search(r'version "' + str(expected) + r'(?:[.\"])', version.stderr + version.stdout):
            raise ValueError('The recorded JDK major is required.')
        if not (args.build_tools / 'lib/apksigner.jar').is_file():
            raise ValueError('Provide the local SDK Build Tools directory.')
        environment = {k: v for k, v in os.environ.items() if k not in verify_release.SIGNING_ENV}
        key.parent.mkdir(parents=True, exist_ok=True)
        if not args.resume_existing:
            print('Create the FIRST team key. Enter a new password twice; input stays hidden.')
            print('Keep this password privately. PKCS12 uses it for both store and key passwords.')
            status('AWAITING_PRIVATE_KEYTOOL_INPUT', key_created=False)
            result = subprocess.run([str(keytool), '-genkeypair', '-alias', ALIAS, '-keyalg', 'RSA',
                                     '-keysize', '3072', '-validity', '10000', '-storetype', 'PKCS12',
                                     '-dname', 'CN=MEMORIDE Release, OU=Prototype, O=MEMORIDE, C=KR',
                                     '-keystore', str(key)], env=environment)
            if result.returncode:
                status('KEY_CREATION_NOT_CONFIRMED', key_created=key.is_file())
                return 2
        status('TEAM_KEY_PRESENT_CERT_EXPORT_PENDING', key_created=True)
        if not cert.exists():
            print('Enter the same store password once to export the PUBLIC certificate.')
            result = subprocess.run([str(keytool), '-exportcert', '-alias', ALIAS,
                                     '-storetype', 'PKCS12', '-keystore', str(key), '-file', str(cert)], env=environment)
            if result.returncode:
                return 2  # Keep the key; never regenerate or overwrite on a retry.
        digest = hashlib.sha256(cert.read_bytes()).hexdigest()  # Public X.509 certificate only.
        status('TEAM_KEY_READY_SIGNING_INPUT_PENDING', key_created=True, certificate_sha256=digest)
        print('Next, enter the SAME password at both hidden signing prompts.')
        result = subprocess.run([sys.executable, str(ROOT / 'scripts/build_signed_release.py'),
                                 '--version-code', str(args.version_code), '--build-tools', str(args.build_tools),
                                 '--expected-cert-sha256', digest, '--keystore-file', str(key), '--key-alias', ALIAS],
                                cwd=ROOT, env=environment)
        status('SIGNED_CANDIDATE_READY' if result.returncode == 0 else 'KEY_READY_BUILD_NOT_CONFIRMED',
               key_created=True, certificate_sha256=digest, build_exit_code=result.returncode,
               declared_source_sha=source, c_pc_install='NOT RUN')
        print('Private key remains outside Git. Back it up privately; share only APK/public metadata.')
        return result.returncode
    except (ValueError, OSError, subprocess.SubprocessError) as error:
        print(str(error) if isinstance(error, ValueError) else 'Local initialization did not complete.', file=sys.stderr)
        return 2


if __name__ == '__main__':
    raise SystemExit(main())
