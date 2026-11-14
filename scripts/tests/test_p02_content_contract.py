"""Reject content that would break the frozen MVP or APK asset lookup."""
import copy
from pathlib import Path
import shutil
import sys
import tempfile
import unittest

SCRIPTS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPTS))
from validate_content import ROOT, load, validate
from validate_ui_samples import validate_samples


class P02ContentContractTests(unittest.TestCase):
    def setUp(self):
        self.catalog = load(ROOT / 'content/catalog.json')
        self.fixtures = load(ROOT / 'fixtures/scenarios.json')
        self.ui = load(ROOT / 'fixtures/ui_samples.json')

    def test_mvp_cannot_silently_add_a_character(self):
        extra = dict(self.catalog['characters'][0], id='test-only-extra-character')
        self.catalog['characters'].append(extra)
        with self.assertRaisesRegex(ValueError, '3 characters'):
            validate(self.catalog, self.fixtures)

    def test_mvp_cannot_silently_add_a_fourth_regional_reward(self):
        extra = dict(self.catalog['items'][0], id='test-only-extra-food')
        self.catalog['items'].append(extra)
        self.catalog['regions'][0]['itemIds'].append(extra['id'])
        with self.assertRaisesRegex(ValueError, '9 items'):
            validate(self.catalog, self.fixtures)

    def test_growth_cap_is_three_not_just_positive(self):
        self.catalog['rules']['dailyGrowthCap'] = 4
        with self.assertRaisesRegex(ValueError, 'growth cap.*3'):
            validate(self.catalog, self.fixtures)

    def test_decoration_slots_remain_three_named_slots(self):
        for slots in [[], ['a', 'b'], ['a', 'b', 'c', 'd'], ['a', 'b', ''], ['a', 'b', True]]:
            with self.subTest(slots=slots):
                self.catalog['rules']['decorationSlotIds'] = slots
                with self.assertRaisesRegex(ValueError, 'decoration slots'):
                    validate(self.catalog, self.fixtures)

    def test_display_names_and_region_theme_are_required(self):
        for section, field in [('characters', 'name'), ('regions', 'name'), ('regions', 'theme')]:
            with self.subTest(section=section, field=field):
                catalog = copy.deepcopy(self.catalog)
                catalog[section][0][field] = '   '
                with self.assertRaisesRegex(ValueError, field + ' required'):
                    validate(catalog, self.fixtures)

    def test_existing_repository_file_is_not_a_packaged_catalog_image(self):
        self.catalog['assets']['char_tori']['path'] = 'README.md'
        with self.assertRaisesRegex(ValueError, 'packaged PNG'):
            validate(self.catalog, self.fixtures)

    def test_existing_nested_png_is_not_in_the_current_root_only_copy(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            shutil.copytree(ROOT / 'assets/starter', root / 'assets/starter')
            nested = root / 'assets/starter/nested/char_tori.png'
            nested.parent.mkdir()
            shutil.copyfile(root / 'assets/starter/char_tori.png', nested)
            self.catalog['assets']['char_tori']['path'] = 'assets/starter/nested/char_tori.png'
            with self.assertRaisesRegex(ValueError, 'packaged PNG'):
                validate(self.catalog, self.fixtures, root)

    def test_current_daily_scenario_cannot_expect_growth(self):
        self.fixtures['scenarios'][0]['expected']['growthDelta'] = 1
        with self.assertRaisesRegex(ValueError, 'D01.*DAILY growth delta must be 0'):
            validate(self.catalog, self.fixtures)

    def test_registered_region_first_completion_must_expect_growth(self):
        self.assertEqual(validate(self.catalog, self.fixtures)['scenarios'], 6)
        self.fixtures['scenarios'][2]['expected']['growthDelta'] = 0
        with self.assertRaisesRegex(ValueError, 'D03.*REGION growth delta must be 1'):
            validate(self.catalog, self.fixtures)

    def test_same_kst_day_and_midnight_use_actual_completion_time(self):
        revisit = self.fixtures['scenarios'][4]
        midnight = 1793458800000  # 2026-11-01 00:00 KST, Oct 31 in UTC.
        revisit['arrival']['completedAtEpochMs'] = midnight - 1
        validate(self.catalog, self.fixtures)
        revisit['expected']['growthDelta'] = 1
        with self.assertRaisesRegex(ValueError, 'D05.*REGION growth delta must be 0'):
            validate(self.catalog, self.fixtures)
        revisit['arrival']['completedAtEpochMs'] = midnight
        validate(self.catalog, self.fixtures)

    def test_region_growth_stops_at_cap_across_distinct_days(self):
        scenarios = self.fixtures['scenarios']
        first = scenarios[-1]
        for offset, delta in enumerate((1, 1, 0), 1):
            scenarios.append({
                'id': f'cap-{offset}', 'dependsOn': scenarios[-1]['id'],
                'arrival': dict(first['arrival'],
                                journeyId=f'00000000-0000-4000-8000-{offset + 100:012d}',
                                completedAtEpochMs=first['arrival']['completedAtEpochMs'] + offset * 86_400_000),
                'expected': dict(growthDelta=delta, grantedItemIds=[], duplicate=False),
            })
        validate(self.catalog, self.fixtures)
        scenarios[-1]['expected']['growthDelta'] = 1
        with self.assertRaisesRegex(ValueError, 'cap-3.*REGION growth delta must be 0'):
            validate(self.catalog, self.fixtures)

    def test_unknown_region_and_item_are_rejected(self):
        for field, message in [('regionId', 'unknown destination'), ('grantedItemIds', 'unknown expected reward')]:
            with self.subTest(field=field):
                fixtures = copy.deepcopy(self.fixtures)
                scenario = fixtures['scenarios'][2]
                if field == 'regionId':
                    scenario['arrival'][field] = 'unknown'
                else:
                    scenario['expected'][field] = ['unknown']
                with self.assertRaisesRegex(ValueError, message):
                    validate(self.catalog, fixtures)

    def test_current_region_sample_allows_one_step_but_not_two(self):
        self.assertEqual(validate_samples(self.catalog, self.ui), 6)
        game = self.ui['samples'][1]['game']
        game['pendingReceipt']['growthAfter'] = 2
        game['dailyGrowth'] = 2
        with self.assertRaisesRegex(ValueError, 'REGION.*growth'):
            validate_samples(self.catalog, self.ui)

    def test_only_explicit_legacy_daily_can_show_daily_growth(self):
        legacy = next(s for s in self.ui['samples'] if s['id'] == 'daily')
        current = next(s for s in self.ui['samples'] if s['id'] == 'daily_current')
        self.assertEqual(legacy['game']['pendingReceipt']['growthAfter'], 1)
        self.assertEqual(current['game']['pendingReceipt']['growthAfter'], 0)
        validate_samples(self.catalog, self.ui)
        del legacy['growthPolicy']
        with self.assertRaisesRegex(ValueError, 'DAILY.*growth'):
            validate_samples(self.catalog, self.ui)

    def test_legacy_policy_cannot_bypass_region_receipt_checks(self):
        self.ui['samples'][1]['growthPolicy'] = 'legacy_daily'
        with self.assertRaisesRegex(ValueError, 'legacy policy requires a DAILY'):
            validate_samples(self.catalog, self.ui)

    def test_preview_rejects_malformed_receipt_and_mismatched_growth(self):
        for receipt, message in [([], 'invalid pending receipt'), (None, 'preview pending growth mismatch')]:
            with self.subTest(message=message):
                ui = copy.deepcopy(self.ui)
                game = ui['samples'][1]['game']
                if receipt is None:
                    game['dailyGrowth'] = 0
                else:
                    game['pendingReceipt'] = receipt
                with self.assertRaisesRegex(ValueError, message):
                    validate_samples(self.catalog, ui)

    def test_daily_ui_receipt_cannot_show_two_growth_steps(self):
        game = next(s['game'] for s in self.ui['samples'] if (s['game'].get('pendingReceipt') or {}).get('kind') == 'DAILY')
        game['pendingReceipt'].update(growthBefore=0, growthAfter=2)
        game['dailyGrowth'] = 2
        with self.assertRaisesRegex(ValueError, 'DAILY.*growth'):
            validate_samples(self.catalog, self.ui)


if __name__ == '__main__':
    unittest.main()
