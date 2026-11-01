#!/usr/bin/env python3
"""Read-only local report. No installs, environment or billing changes."""
from __future__ import annotations
import argparse
import json
import os
from pathlib import Path
import platform
import re
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
FIELDS = ['javaMajor', 'androidStudio', 'agp', 'gradle', 'kotlin', 'compose', 'compileSdk', 'targetSdk']


def recorded_toolchain() -> dict:
    data = json.loads((ROOT/'config/toolchain.json').read_text(encoding='utf-8'))
    if data.get('verified') is not True or any(data.get(k) is None or data[k] == '' for k in FIELDS):
        raise ValueError('A has not recorded a verified toolchain; Android checks NOT READY')
    if type(data['javaMajor']) is not int or not 11 <= data['javaMajor'] <= 99:
        raise ValueError('javaMajor must be a verified integer')
    if not data.get('verifiedBy') or not data.get('verifiedAt'):
        raise ValueError('verifiedBy and verifiedAt required')
    return data


def wrapper_ready() -> None:
    for rel in ['gradlew', 'gradlew.bat', 'gradle/wrapper/gradle-wrapper.properties', 'gradle/wrapper/gradle-wrapper.jar', 'app']:
        if not (ROOT/rel).exists():
            raise ValueError(f'missing Android project file: {rel}')


def java_major() -> int | None:
    home = os.environ.get('JAVA_HOME')
    candidate = Path(home)/'bin'/('java.exe' if os.name == 'nt' else 'java') if home else None
    java = str(candidate) if candidate is not None else shutil.which('java')
    if not java:
        return None
    try:
        result = subprocess.run([java, '-version'], capture_output=True, text=True, timeout=10)
    except (OSError, subprocess.TimeoutExpired):
        return None
    match = re.search(r'(?:openjdk|java) version "(?:1\.)?(\d+)', result.stdout+'\n'+result.stderr)
    return int(match[1]) if match else None


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument('--require-android', action='store_true')
    parser.add_argument('--ci-output', action='store_true')
    args = parser.parse_args()
    try:
        if args.ci_output:
            data = recorded_toolchain()
            wrapper_ready()
            output = os.environ.get('GITHUB_OUTPUT')
            if not output:
                raise ValueError('GITHUB_OUTPUT unavailable; flag is for the manual workflow')
            with open(output, 'a', encoding='utf-8', newline='\n') as stream:
                stream.write(f"java_major={data['javaMajor']}\n")
            print('Verified Java major exported. No Android build performed yet.')
            return 0
        current_java = java_major()
        print(f'OS={platform.system()} CPU={platform.machine()} Python={platform.python_version()}')
        print(f'Git available={shutil.which("git") is not None}; shell Java major={current_java}')
        print('IDE Gradle JDK and Daemon JVM criteria require separate confirmation.')
        if args.require_android:
            data = recorded_toolchain()
            wrapper_ready()
            if current_java != data['javaMajor']:
                raise ValueError(f"shell Java major {current_java} != recorded {data['javaMajor']}")
            print('Android prerequisites recorded; actual Gradle checks run next.')
        else:
            print('Diagnostic only; no install, SDK call or Android success claim.')
        return 0
    except (OSError, ValueError, json.JSONDecodeError) as error:
        print(f'Environment NOT READY: {error}', file=sys.stderr)
        return 2


if __name__ == '__main__':
    raise SystemExit(main())
