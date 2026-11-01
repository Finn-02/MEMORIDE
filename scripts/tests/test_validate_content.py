import copy
import importlib.util
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('validate_content', ROOT / 'scripts/validate_content.py')
validator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(validator)


class ContentValidationTest(unittest.TestCase):
    def setUp(self):
        self.catalog = validator.load(ROOT / 'content/catalog.json')
        self.fixtures = validator.load(ROOT / 'fixtures/scenarios.json')

    def test_current_data(self):
        self.assertEqual(validator.validate(self.catalog, self.fixtures)['regions'], 3)

    def test_duplicate_item_id_rejected(self):
        self.catalog['items'].append(copy.deepcopy(self.catalog['items'][0]))
        with self.assertRaisesRegex(ValueError, 'duplicate id'):
            validator.validate(self.catalog, self.fixtures)

    def test_unknown_reference_rejected(self):
        self.catalog['regions'][0]['itemIds'][0] = 'missing-item'
        with self.assertRaisesRegex(ValueError, 'inconsistent item references'):
            validator.validate(self.catalog, self.fixtures)

    def test_missing_asset_rejected(self):
        self.catalog['assets']['char_tori']['path'] = 'assets/no-such-file.png'
        with self.assertRaisesRegex(ValueError, 'missing file'):
            validator.validate(self.catalog, self.fixtures)

    def test_path_escape_rejected(self):
        self.catalog['assets']['char_tori']['path'] = '../outside.png'
        with self.assertRaisesRegex(ValueError, 'outside project'):
            validator.validate(self.catalog, self.fixtures)

    def test_fake_source_cannot_claim_pleos(self):
        self.fixtures['scenarios'][0]['arrival']['source'] = 'PLEOS'
        with self.assertRaisesRegex(ValueError, 'must remain DEMO'):
            validator.validate(self.catalog, self.fixtures)

    def test_replay_cannot_grant_reward(self):
        self.fixtures['scenarios'][3]['expected']['growthDelta'] = 1
        with self.assertRaisesRegex(ValueError, 'no new effect'):
            validator.validate(self.catalog, self.fixtures)

    def test_dependency_must_be_earlier(self):
        self.fixtures['scenarios'][1]['dependsOn'] = 'D05'
        with self.assertRaisesRegex(ValueError, 'not earlier'):
            validator.validate(self.catalog, self.fixtures)

    def test_invalid_coordinate_rejected(self):
        self.catalog['regions'][0]['latitude'] = float('nan')
        with self.assertRaisesRegex(ValueError, 'invalid latitude'):
            validator.validate(self.catalog, self.fixtures)

    def test_null_measurements_are_valid(self):
        self.fixtures['scenarios'][0]['arrival']['elapsedSeconds'] = None
        self.fixtures['scenarios'][0]['arrival']['distanceMeters'] = None
        validator.validate(self.catalog, self.fixtures)

    def test_bool_is_not_elapsed_seconds(self):
        self.fixtures['scenarios'][0]['arrival']['elapsedSeconds'] = True
        with self.assertRaisesRegex(ValueError, 'invalid elapsedSeconds'):
            validator.validate(self.catalog, self.fixtures)

    def test_bool_is_not_daily_limit(self):
        self.catalog['rules']['dailyGrantLimit'] = True
        with self.assertRaisesRegex(ValueError, 'integer 1'):
            validator.validate(self.catalog, self.fixtures)

    def test_bool_is_not_schema_version(self):
        self.fixtures['schemaVersion'] = True
        with self.assertRaisesRegex(ValueError, 'integer 1'):
            validator.validate(self.catalog, self.fixtures)

    def test_accidental_journey_reuse_rejected(self):
        self.fixtures['scenarios'][1]['arrival']['journeyId'] = self.fixtures['scenarios'][0]['arrival']['journeyId']
        with self.assertRaisesRegex(ValueError, 'explicit replay'):
            validator.validate(self.catalog, self.fixtures)

    def test_asset_case_is_portable(self):
        self.catalog['assets']['char_tori']['path'] = 'Assets/starter/char_tori.png'
        with self.assertRaisesRegex(ValueError, 'missing file'):
            validator.validate(self.catalog, self.fixtures)

    def test_asset_backslash_rejected(self):
        self.catalog['assets']['char_tori']['path'] = 'assets\\starter\\char_tori.png'
        with self.assertRaisesRegex(ValueError, 'non-portable'):
            validator.validate(self.catalog, self.fixtures)

    def test_daily_cannot_request_region_reward(self):
        self.fixtures['scenarios'][0]['arrival']['regionId'] = 'daejeon'
        with self.assertRaisesRegex(ValueError, 'DAILY region'):
            validator.validate(self.catalog, self.fixtures)

    def test_duplicate_expectation_requires_replay(self):
        self.fixtures['scenarios'][0]['expected']['duplicate'] = True
        with self.assertRaisesRegex(ValueError, 'requires replay'):
            validator.validate(self.catalog, self.fixtures)


if __name__ == '__main__':
    unittest.main()
