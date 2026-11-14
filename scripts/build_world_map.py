#!/usr/bin/env python3
"""Offline conversion of Natural Earth 5.1.1 country polygons to an Android vector.

Usage: py -3 scripts/build_world_map.py path/to/ne_10m_admin_0_countries.zip
No download, API key, Python package, or game data is required.
"""
import argparse
import hashlib
from pathlib import Path
import struct
import zipfile

ROOT = Path(__file__).resolve().parents[1]
EXPECTED_SHA = 'ce1ac7036499a0edd641fbc093cd209a98f96a49d2eca8480aaacad35138a7f6'
FILL_COLOR = '#FFF6E0'
STROKE_COLOR = '#EBCF96'
STROKE_WIDTH = '3'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source_zip', type=Path)
    parser.add_argument('--output', type=Path,
                        default=ROOT / 'app/src/main/res/drawable/memoride_korea_map.xml')
    parser.add_argument('--check', action='store_true',
                        help='Compare with output without changing it; fail on any byte difference')
    args = parser.parse_args()
    source = args.source_zip.read_bytes()
    if hashlib.sha256(source).hexdigest() != EXPECTED_SHA:
        raise ValueError('Unexpected source; review version/hash before generating a new asset')
    with zipfile.ZipFile(args.source_zip) as archive:
        dbf = archive.read('ne_10m_admin_0_countries.dbf')
        shp = archive.read('ne_10m_admin_0_countries.shp')
    count, header, record_size = struct.unpack_from('<IHH', dbf, 4)
    fields, cursor = [], 32
    while dbf[cursor] != 13:
        descriptor = dbf[cursor:cursor + 32]
        fields.append((descriptor[:11].split(b'\0')[0].decode(), descriptor[16]))
        cursor += 32
    indexes = []
    for i in range(count):
        values, cursor = {}, header + i * record_size + 1
        for name, length in fields:
            values[name] = dbf[cursor:cursor + length].decode('utf-8').strip('\x00 ')
            cursor += length
        if values['ADM0_A3'] == 'KOR':
            indexes.append(i)
    if len(indexes) != 1:
        raise ValueError('Expected one country record')
    cursor, index = 100, 0
    while index < indexes[0]:
        words = struct.unpack_from('>i', shp, cursor + 4)[0]
        cursor += 8 + words * 2
        index += 1
    body = cursor + 8
    if struct.unpack_from('<i', shp, body)[0] != 5:
        raise ValueError('Expected Polygon geometry')
    parts, points = struct.unpack_from('<ii', shp, body + 36)
    starts = list(struct.unpack_from('<' + 'i' * parts, shp, body + 44)) + [points]
    point_start = body + 44 + parts * 4
    paths = []
    for first, last in zip(starts, starts[1:]):
        commands = []
        for point in range(first, last):
            lon, lat = struct.unpack_from('<dd', shp, point_start + point * 16)
            # Matches WorldMapGeometry. Equirectangular, reference latitude 36 degrees.
            x, y = 12.5735 + (lon - 124.4) * 80.9016994, 10 + (39.0 - lat) * 100
            commands.append(f'{"M" if point == first else "L"}{x:.2f},{y:.2f}')
        paths.append(' '.join(commands) + ' Z')
    output = args.output
    # Keep each XML path well below AAPT's UTF-8 string limit. A single country path
    # was replaced by STRING_TOO_LARGE at packaging time and crashed Compose parsing.
    path_xml = '\n'.join(
        f'    <path android:fillColor="{FILL_COLOR}" android:strokeColor="{STROKE_COLOR}"\n'
        f'        android:strokeWidth="{STROKE_WIDTH}" android:fillType="evenOdd"\n'
        f'        android:pathData="{path}" />' for path in paths)
    if any(len(path.encode('utf-8')) >= 30000 for path in paths):
        raise ValueError('Simplify geometry before packaging an oversized ring')
    rendered = ('''<?xml version="1.0" encoding="utf-8"?>
<!-- Natural Earth 5.1.1 / public domain; see docs/ux_c_map/SOURCE.md. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="640dp" android:height="640dp"
    android:viewportWidth="640" android:viewportHeight="640">
''' + path_xml + '\n</vector>\n').encode('utf-8')
    if args.check:
        if not output.is_file() or output.read_bytes() != rendered:
            raise ValueError('Map differs from pinned geometry/style; regenerate or review the change')
    else:
        output.write_bytes(rendered)
    print(f'{"Verified" if args.check else "Generated"} {output.name}: '
          f'{parts} rings, {points} points; SHA256={hashlib.sha256(rendered).hexdigest()}')


if __name__ == '__main__':
    main()
