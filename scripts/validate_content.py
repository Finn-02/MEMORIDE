#!/usr/bin/env python3
"""Small offline content checks. Does not validate Android, SDK behavior or safety."""
from __future__ import annotations
import json
import math
from pathlib import Path
import sys
import uuid

ROOT = Path(__file__).resolve().parents[1]


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def number(value: object) -> bool:
    return isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(value)


def unique(records: list, label: str) -> dict:
    require(isinstance(records, list) and len(records) > 0, f'{label}: non-empty list required')
    result = {}
    for item in records:
        require(isinstance(item, dict), f'{label}: object required')
        item_id = item.get('id')
        require(isinstance(item_id, str) and bool(item_id.strip()), f'{label}: id required')
        require(item_id not in result, f'{label}: duplicate id {item_id}')
        result[item_id] = item
    return result


def validate(catalog: dict, fixtures: dict, root: Path = ROOT) -> dict:
    require(isinstance(catalog.get('contentVersion'), str), 'contentVersion required')
    regions = unique(catalog.get('regions'), 'regions')
    items = unique(catalog.get('items'), 'items')
    characters = unique(catalog.get('characters'), 'characters')
    require(len(regions) == 3, 'MVP requires exactly 3 regions')
    require(len(items) == 9, 'MVP requires exactly 9 items')
    require(len(characters) == 3, 'MVP requires exactly 3 characters')
    assets = catalog.get('assets')
    require(isinstance(assets, dict) and assets, 'assets required')
    root = root.resolve()
    for key, asset in assets.items():
        require(isinstance(asset, dict), f'asset {key}: object required')
        relative = asset.get('path')
        require(isinstance(relative, str), f'asset {key}: path required')
        path = (root / relative).resolve()
        require(path.is_relative_to(root), f'asset {key}: path outside project')
        require('\\' not in relative and ':' not in relative and '..' not in Path(relative).parts, f'asset {key}: non-portable path')
        require(path.is_file(), f'asset {key}: missing file {relative}')
        current = root
        for segment in relative.split('/'):
            require(current.is_dir() and segment in {p.name for p in current.iterdir()}, f'asset {key}: missing file or wrong case {relative}')
            current /= segment
        require(Path(relative).parent == Path('assets/starter') and Path(relative).suffix == '.png',
                f'asset {key}: packaged PNG required at assets/starter/<filename>.png')
    for item_id, item in items.items():
        require(item.get('regionId') in regions, f'{item_id}: unknown region')
        require(item.get('type') in ('FOOD', 'OUTFIT', 'DECOR'), f'{item_id}: invalid type')
        require(item.get('assetKey') in assets, f'{item_id}: unknown asset')
        require(assets[item['assetKey']].get('kind') == item['type'].lower(), f'{item_id}: asset kind mismatch')
        require(isinstance(item.get('name'), str) and item['name'].strip(), f'{item_id}: name required')
    for region_id, region in regions.items():
        for field in ('name', 'theme'):
            require(isinstance(region.get(field), str) and region[field].strip(), f'{region_id}: {field} required')
        for field, low, high in [('latitude', -90, 90), ('longitude', -180, 180)]:
            require(number(region.get(field)) and low <= region[field] <= high, f'{region_id}: invalid {field}')
        item_ids = region.get('itemIds')
        require(isinstance(item_ids, list) and len(item_ids) == len(set(item_ids)), f'{region_id}: invalid itemIds')
        require(set(item_ids) == {i for i, v in items.items() if v['regionId'] == region_id}, f'{region_id}: inconsistent item references')
        require({items[i]['type'] for i in item_ids} == {'FOOD', 'OUTFIT', 'DECOR'}, f'{region_id}: missing item category')
    for cid, character in characters.items():
        require(isinstance(character.get('name'), str) and character['name'].strip(), f'{cid}: name required')
        key = character.get('assetKey')
        require(key in assets and assets[key].get('kind') == 'body', f'{cid}: invalid character body')
    expressions = catalog.get('expressions', {})
    for state in ('idle', 'happy', 'eat'):
        key = expressions.get(state)
        require(key in assets and assets[key].get('kind') == 'face', f'{state}: missing face')
    rules = catalog.get('rules', {})
    require(type(rules.get('dailyGrowthCap')) is int and rules['dailyGrowthCap'] == 3, 'MVP growth cap must be integer 3')
    require(type(rules.get('dailyGrantLimit')) is int and rules['dailyGrantLimit'] == 1, 'MVP daily grant limit must be integer 1')
    require(rules.get('timeZone') == 'Asia/Seoul', 'unexpected time zone')
    slots = rules.get('decorationSlotIds')
    require(isinstance(slots, list) and len(slots) == 3
            and all(isinstance(slot, str) and slot.strip() for slot in slots)
            and len(set(slots)) == 3, 'MVP decoration slots must be 3 unique nonempty strings')
    require(type(fixtures.get('schemaVersion')) is int and fixtures['schemaVersion'] == 1, 'fixture schemaVersion must be integer 1')
    scenarios = unique(fixtures.get('scenarios'), 'scenarios')
    seen = set()
    seen_journeys = set()
    for sid, scenario in scenarios.items():
        predecessor = scenario.get('dependsOn')
        require(predecessor is None or predecessor in seen, f'{sid}: dependency missing or not earlier')
        require(scenario.get('resetBefore') is True or predecessor is not None, f'{sid}: reset or dependency required')
        command = scenario.get('arrival', {})
        require(command.get('source') == 'DEMO', f'{sid}: fixture must remain DEMO')
        require(command.get('evidence') == 'DEMO_FINAL_DESTINATION', f'{sid}: invalid evidence')
        require(command.get('kind') in ('DAILY', 'REGION'), f'{sid}: invalid journey kind')
        try:
            uuid.UUID(command.get('journeyId', ''))
        except (ValueError, AttributeError, TypeError):
            raise ValueError(f'{sid}: invalid journey UUID')
        region = command.get('regionId')
        require(region is None or region in regions, f'{sid}: unknown destination')
        require(command['kind'] != 'DAILY' or region is None, f'{sid}: DAILY region must be null')
        require(type(command.get('routeRevision')) is int and command['routeRevision'] >= 0, f'{sid}: invalid route revision')
        require(type(command.get('completedAtEpochMs')) is int and command['completedAtEpochMs'] > 0, f'{sid}: invalid completion time')
        for field in ('elapsedSeconds', 'distanceMeters'):
            value = command.get(field)
            require(value is None or (number(value) and value >= 0), f'{sid}: invalid {field}')
        elapsed = command.get('elapsedSeconds')
        require(elapsed is None or type(elapsed) is int, f'{sid}: elapsedSeconds must be an integer or null')
        expected = scenario.get('expected', {})
        grants = expected.get('grantedItemIds')
        require(isinstance(grants, list) and all(i in items for i in grants), f'{sid}: unknown expected reward')
        require(len(grants) == len(set(grants)), f'{sid}: duplicate expected reward')
        require(type(expected.get('growthDelta')) is int and 0 <= expected['growthDelta'] <= 1, f'{sid}: invalid growth delta')
        require(type(expected.get('duplicate')) is bool, f'{sid}: duplicate expectation must be bool')
        discovery = expected.get('discoveredRegionId')
        require(discovery is None or discovery == region, f'{sid}: discovery mismatch')
        require(all(items[i]['regionId'] == region for i in grants), f'{sid}: reward region mismatch')
        replay = scenario.get('replayOf')
        require(expected['duplicate'] == (replay is not None), f'{sid}: duplicate expectation requires replay')
        require(command['journeyId'] not in seen_journeys or replay is not None, f'{sid}: duplicate journey requires explicit replay')
        if replay is not None:
            require(replay in seen, f'{sid}: replay target missing')
            require(command == scenarios[replay]['arrival'], f'{sid}: replay command differs')
            require(expected['duplicate'] and not grants and expected['growthDelta'] == 0, f'{sid}: replay must have no new effect')
        require(command['kind'] != 'REGION' or expected['growthDelta'] == 0, f'{sid}: REGION must not expect DAILY growth')
        seen.add(sid)
        seen_journeys.add(command['journeyId'])
    return {'regions': len(regions), 'items': len(items), 'characters': len(characters), 'assets': len(assets), 'scenarios': len(scenarios)}


def load(path: Path) -> dict:
    def reject_constant(value: str):
        raise ValueError(f'non-finite JSON value: {value}')
    with path.open(encoding='utf8') as stream:
        return json.load(stream, parse_constant=reject_constant)


def main() -> int:
    try:
        counts = validate(load(ROOT / 'content/catalog.json'), load(ROOT / 'fixtures/scenarios.json'))
    except (ValueError, KeyError, TypeError, OSError) as error:
        print(f'Content validation FAILED: {error}', file=sys.stderr)
        return 1
    print('Content static validation passed: ' + ', '.join(f'{k}={v}' for k, v in counts.items()))
    print('Not an Android build, SDK test, game-logic test or safety certification.')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
