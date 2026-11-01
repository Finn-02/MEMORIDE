#!/usr/bin/env python3
"""Actual wrapper checks only. Missing app is NOT RUN, never a fake success."""
from pathlib import Path
import os
import subprocess
import sys
ROOT = Path(__file__).resolve().parents[1]

def main() -> int:
    needed = ['app', 'gradlew', 'gradlew.bat', 'gradle/wrapper/gradle-wrapper.jar', 'gradle/wrapper/gradle-wrapper.properties']
    if not all((ROOT/p).exists() for p in needed):
        print('Android checks NOT RUN: A must bootstrap app and all wrapper files.', file=sys.stderr)
        return 2
    tasks = ['--no-daemon', ':app:testDebugUnitTest', ':app:assembleDebug']
    command = ['cmd.exe', '/d', '/c', 'gradlew.bat', *tasks] if os.name == 'nt' else ['sh', './gradlew', *tasks]
    try:
        return subprocess.run(command, cwd=ROOT).returncode
    except OSError as error:
        print(f'Android checks NOT RUN: {error}', file=sys.stderr)
        return 2

if __name__ == '__main__':
    raise SystemExit(main())
