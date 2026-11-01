#!/usr/bin/env python3
"""Portable path/link/import checks; not a compiler or security scanner."""
from __future__ import annotations
from pathlib import Path, PurePosixPath
import re
import sys
import unicodedata
from urllib.parse import unquote

ROOT = Path(__file__).resolve().parents[1]
SKIP = {'.git', '.gradle', '.idea', '.venv', 'venv', 'build', '__pycache__', '.kotlin', '.cxx', 'tmp'}
TEXT = {'.md', '.py', '.kt', '.kts', '.json', '.yml', '.yaml', '.xml', '.properties', '.svg', '.mmd'}
DEVICES = {'con', 'prn', 'aux', 'nul', *(f'com{i}' for i in range(1, 10)), *(f'lpt{i}' for i in range(1, 10))}


def portable_name(relative: str) -> None:
    if '\\' in relative or PurePosixPath(relative).is_absolute():
        raise ValueError(f'non-portable path: {relative}')
    for part in relative.split('/'):
        if part in {'', '.', '..'} or part.rstrip(' .') != part:
            raise ValueError(f'invalid path segment: {relative}')
        if any(c in part for c in '<>:"|?*') or any(ord(c) < 32 for c in part):
            raise ValueError(f'Windows-invalid path: {relative}')
        if part.split('.')[0].casefold() in DEVICES:
            raise ValueError(f'Windows reserved name: {relative}')
        if unicodedata.normalize('NFC', part) != part:
            raise ValueError(f'filename must use NFC: {relative}')


def check_names(paths: list[str]) -> None:
    seen = {}
    for relative in paths:
        portable_name(relative)
        parts = relative.split('/')
        for i in range(1, len(parts) + 1):
            prefix = '/'.join(parts[:i])
            key = prefix.casefold()
            if key in seen and seen[key] != prefix:
                raise ValueError(f'case-only path collision: {seen[key]} / {prefix}')
            seen[key] = prefix


def check_feature_imports(text: str, label: str) -> None:
    package = re.search(r'^\s*package\s+io\.kailand\.feature\.(\w+)', text, re.M)
    if not package:
        return
    for imported in re.findall(r'^\s*import\s+io\.kailand\.feature\.(\w+)', text, re.M):
        if imported != package[1]:
            raise ValueError(f'cross-feature import: {label}: {package[1]} -> {imported}')


def validate_tree(root: Path = ROOT) -> dict:
    root = root.resolve()
    candidates = [p for p in root.rglob('*') if not any(x in SKIP for x in p.relative_to(root).parts)]
    if any(p.is_symlink() for p in candidates):
        raise ValueError('source package must not contain symlinks')
    files = [p for p in candidates if p.is_file()]
    names = [p.relative_to(root).as_posix() for p in files]
    check_names(names)
    for p in files:
        relative = p.relative_to(root).as_posix()
        if p.suffix in TEXT or p.name in {'.gitignore', '.gitattributes', 'gradlew'}:
            data = p.read_bytes()
            text = data.decode('utf-8')
            if data.startswith(b'\xef\xbb\xbf') or b'\r\n' in data:
                raise ValueError(f'expected UTF-8 without BOM and LF: {relative}')
            if p.suffix == '.kt':
                check_feature_imports(text, relative)
            if p.suffix == '.md':
                for link in re.findall(r'!?\[[^\]\n]*\]\(([^)\n]+)\)', text):
                    target = link.split(' "', 1)[0].strip().strip('<>')
                    if not target or target.startswith(('#', 'http:', 'https:', 'mailto:', 'sandbox:')):
                        continue
                    target = unquote(target.split('#', 1)[0])
                    if '\\' in target:
                        raise ValueError(f'backslash in Markdown link: {relative}')
                    dest = (p.parent / target).resolve()
                    if not dest.is_relative_to(root):
                        raise ValueError(f'link outside project: {relative}: {target}')
                    current = root
                    for segment in dest.relative_to(root).parts:
                        if not current.is_dir() or segment not in {x.name for x in current.iterdir()}:
                            raise ValueError(f'missing or wrong-case link: {relative}: {target}')
                        current /= segment
    return {'files': len(files), 'longest_relative_path': max(map(len, names), default=0)}


if __name__ == '__main__':
    try:
        result = validate_tree()
    except (ValueError, OSError, UnicodeError) as error:
        print(f'Package static check FAILED: {error}', file=sys.stderr)
        raise SystemExit(1)
    print(f'Package static check passed: {result}')
    print('Heuristic only; not Kotlin compilation, Windows execution or SDK verification.')
