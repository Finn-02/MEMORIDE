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

    def test_region_scenario_cannot_also_expect_daily_growth(self):
        scenario = next(s for s in self.fixtures['scenarios'] if s['arrival']['kind'] == 'REGION' and not s.get('replayOf'))
        scenario['expected']['growthDelta'] = 1
        with self.assertRaisesRegex(ValueError, 'REGION.*growth'):
            validate(self.catalog, self.fixtures)

    def test_region_ui_receipt_cannot_also_show_daily_growth(self):
        game = next(s['game'] for s in self.ui['samples'] if (s['game'].get('pendingReceipt') or {}).get('kind') == 'REGION')
        game['pendingReceipt'].update(growthBefore=0, growthAfter=1)
        game['dailyGrowth'] = 1
        with self.assertRaisesRegex(ValueError, 'REGION.*growth'):
            validate_samples(self.catalog, self.ui)

    def test_daily_ui_receipt_cannot_show_two_growth_steps(self):
        game = next(s['game'] for s in self.ui['samples'] if (s['game'].get('pendingReceipt') or {}).get('kind') == 'DAILY')
        game['pendingReceipt'].update(growthBefore=0, growthAfter=2)
        game['dailyGrowth'] = 2
        with self.assertRaisesRegex(ValueError, 'DAILY.*growth'):
            validate_samples(self.catalog, self.ui)


if __name__ == '__main__':
    unittest.main()
