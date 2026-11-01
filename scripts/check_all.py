#!/usr/bin/env python3
"""Same stdlib checks from macOS, Windows or a manual Linux job."""
from pathlib import Path
import argparse
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument('--android', action='store_true')
    args = parser.parse_args()
    if sys.version_info < (3, 10):
        print('Python 3.10 or newer required', file=sys.stderr)
        return 2
    commands = [['scripts/validate_content.py'], ['scripts/validate_ui_samples.py'], ['scripts/check_package.py'], ['-m', 'unittest', 'discover', '-s', 'scripts/tests', '-v']]
    if args.android:
        commands += [['scripts/doctor.py', '--require-android'], ['scripts/check_android.py']]
    for command in commands:
        print('RUN ' + ' '.join(command), flush=True)
        code = subprocess.run([sys.executable, *command], cwd=ROOT).returncode
        if code:
            print(f'STOP: exit code {code}; remaining checks NOT RUN', flush=True)
            return code
    if not args.android:
        print('Static checks PASS. Android, Pleos, rendered UI, release and safety tests NOT RUN.')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
