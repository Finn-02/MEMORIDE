#!/usr/bin/env python3
"""Check preview DTO data against the catalog; not live game saves."""
from __future__ import annotations
import uuid
from validate_content import load, require, number, ROOT


def validate_samples(catalog: dict, document: dict) -> int:
    require(type(document.get('schemaVersion')) is int and document['schemaVersion'] == 1, 'UI schemaVersion must be 1')
    require(document.get('contentVersion') == catalog['contentVersion'], 'UI content version mismatch')
    samples = document.get('samples')
    require(isinstance(samples, list) and samples, 'UI samples required')
    item = {x['id']: x for x in catalog['items']}
    regions = {x['id'] for x in catalog['regions']}
    chars = {x['id'] for x in catalog['characters']}
    slots = set(catalog['rules']['decorationSlotIds'])
    seen = set()
    for sample in samples:
        require(isinstance(sample, dict), 'UI sample must be an object')
        sid = sample.get('id')
        require(isinstance(sid, str) and sid and sid not in seen, 'duplicate or missing UI sample id')
        seen.add(sid)
        require(sample.get('gate', {}).get('state') in {'UNKNOWN', 'RESTRICTED', 'PARKED_ALLOWED'}, f'{sid}: invalid gate')
        growth_policy = sample.get('growthPolicy', 'current')
        require(growth_policy in ('current', 'legacy_daily'), f'{sid}: invalid preview growth policy')
        g = sample.get('game')
        require(isinstance(g, dict) and 'pendingReceipt' in g, f'{sid}: invalid game snapshot')
        owned = g.get('ownedItemIds')
        require(isinstance(owned, list) and all(x in item for x in owned) and len(owned) == len(set(owned)), f'{sid}: invalid ownership')
        discovered = g.get('discoveredRegionIds')
        require(isinstance(discovered, list) and all(x in regions for x in discovered) and len(discovered) == len(set(discovered)), f'{sid}: invalid discoveries')
        growth = g.get('dailyGrowth')
        require(type(growth) is int and 0 <= growth <= catalog['rules']['dailyGrowthCap'], f'{sid}: invalid growth')
        avatar = g.get('avatar', {})
        require(avatar.get('characterId') in chars, f'{sid}: unknown character')
        outfit = avatar.get('outfitItemId')
        require(outfit is None or (outfit in owned and item[outfit]['type'] == 'OUTFIT'), f'{sid}: invalid outfit')
        decorations = g.get('decorationSlots')
        require(isinstance(decorations, dict) and set(decorations).issubset(slots), f'{sid}: invalid slot')
        require(len(set(decorations.values())) == len(decorations), f'{sid}: duplicate decoration')
        require(all(x in owned and item[x]['type'] == 'DECOR' for x in decorations.values()), f'{sid}: invalid decoration')
        receipt = g.get('pendingReceipt')
        if receipt is None:
            require(growth_policy == 'current', f'{sid}: legacy DAILY preview requires a receipt')
            continue
        require(isinstance(receipt, dict), f'{sid}: invalid pending receipt')
        require(receipt.get('source') == 'DEMO', f'{sid}: preview receipt must remain DEMO')
        try:
            uuid.UUID(receipt.get('journeyId', ''))
        except (ValueError, AttributeError, TypeError):
            raise ValueError(f'{sid}: invalid receipt UUID')
        require(receipt.get('kind') in {'DAILY', 'REGION'}, f'{sid}: invalid kind')
        region = receipt.get('regionId')
        require(region is None or region in regions, f'{sid}: invalid receipt region')
        require(receipt['kind'] != 'DAILY' or region is None, f'{sid}: DAILY region must be null')
        grants = receipt.get('grantedItemIds')
        require(isinstance(grants, list) and len(grants) == len(set(grants)) and all(x in owned for x in grants), f'{sid}: receipt grant not owned')
        require(all(item[x]['regionId'] == region for x in grants), f'{sid}: receipt reward region mismatch')
        found = receipt.get('discoveredRegionId')
        require(found is None or (found == region and found in discovered), f'{sid}: receipt discovery mismatch')
        before, after = receipt.get('growthBefore'), receipt.get('growthAfter')
        require(type(before) is int and type(after) is int and 0 <= before <= after <= catalog['rules']['dailyGrowthCap'], f'{sid}: receipt growth invalid')
        delta = after - before
        require(growth_policy != 'legacy_daily' or receipt['kind'] == 'DAILY', f'{sid}: legacy policy requires a DAILY receipt')
        if receipt['kind'] == 'DAILY':
            limit = 1 if growth_policy == 'legacy_daily' else 0
            require(delta <= limit, f'{sid}: DAILY receipt growth exceeds the preview policy')
        else:
            require(delta <= (1 if region is not None else 0), f'{sid}: REGION receipt growth requires a registered region and at most one step')
        require(after == growth, f'{sid}: preview pending growth mismatch')
        require(type(receipt.get('completedAtEpochMs')) is int and receipt['completedAtEpochMs'] > 0, f'{sid}: invalid receipt time')
        for field in ['elapsedSeconds', 'distanceMeters']:
            value = receipt.get(field)
            require(value is None or (number(value) and value >= 0), f'{sid}: invalid {field}')
        require(receipt.get('elapsedSeconds') is None or type(receipt['elapsedSeconds']) is int, f'{sid}: elapsed integer required')
    return len(samples)


if __name__ == '__main__':
    try:
        count = validate_samples(load(ROOT/'content/catalog.json'), load(ROOT/'fixtures/ui_samples.json'))
    except (ValueError, KeyError, TypeError, OSError) as error:
        print(f'UI samples FAILED: {error}')
        raise SystemExit(1)
    print(f'UI samples static validation passed: {count}; not rendered UI tests.')
