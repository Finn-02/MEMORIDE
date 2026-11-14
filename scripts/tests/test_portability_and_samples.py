from pathlib import Path
import copy
import sys
import tempfile
import unittest
from unittest.mock import patch

SCRIPTS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPTS))
from check_package import check_names, check_feature_imports, validate_tree
from validate_content import load
from validate_ui_samples import validate_samples
import doctor


class PortabilityTests(unittest.TestCase):
    def test_case_collision_in_parent_directories(self):
        with self.assertRaises(ValueError):
            check_names(['assets/Icons/a.png', 'assets/icons/b.png'])

    def test_windows_reserved_filename(self):
        with self.assertRaises(ValueError):
            check_names(['assets/CON.png'])

    def test_backslash_is_not_portable(self):
        with self.assertRaises(ValueError):
            check_names(['assets\\body.png'])

    def test_trailing_dot_rejected(self):
        with self.assertRaises(ValueError):
            check_names(['assets/body.'])

    def test_cross_feature_import_rejected(self):
        text = 'package io.kailand.feature.world\nimport io.kailand.feature.island.GameStore\n'
        with self.assertRaises(ValueError):
            check_feature_imports(text, 'World.kt')

    def test_own_feature_import_allowed(self):
        check_feature_imports('package io.kailand.feature.world\nimport io.kailand.feature.world.Card', 'World.kt')

    def test_wrong_case_markdown_link_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            (root/'Readme.md').write_text('[test](Asset.png)\n', encoding='utf-8')
            (root/'asset.png').write_bytes(b'dummy')
            with self.assertRaises(ValueError):
                validate_tree(root)

    def test_bom_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            (root/'data.json').write_bytes(b'\xef\xbb\xbf{}')
            with self.assertRaises(ValueError):
                validate_tree(root)


class UiSamplesTests(unittest.TestCase):
    def setUp(self):
        root = SCRIPTS.parent
        self.catalog = load(root/'content/catalog.json')
        self.data = load(root/'fixtures/ui_samples.json')

    def test_current_samples(self):
        self.assertEqual(validate_samples(self.catalog, self.data), 6)

    def test_unowned_outfit_rejected(self):
        self.data['samples'][0]['game']['avatar']['outfitItemId'] = 'daejeon_hat'
        with self.assertRaises(ValueError):
            validate_samples(self.catalog, self.data)

    def test_receipt_cannot_claim_real_sdk(self):
        self.data['samples'][1]['game']['pendingReceipt']['source'] = 'PLEOS'
        with self.assertRaises(ValueError):
            validate_samples(self.catalog, self.data)

    def test_duplicate_decoration_rejected(self):
        self.data['samples'][1]['game']['decorationSlots']['slot_right'] = 'daejeon_sign'
        with self.assertRaises(ValueError):
            validate_samples(self.catalog, self.data)

    def test_pending_receipt_must_match_ownership(self):
        self.data['samples'][1]['game']['ownedItemIds'].remove('daejeon_bread')
        with self.assertRaises(ValueError):
            validate_samples(self.catalog, self.data)

    def test_null_metrics_valid(self):
        sample = next(s for s in self.data['samples'] if s['id'] == 'missing_metrics')['game']['pendingReceipt']
        self.assertIsNone(sample['distanceMeters'])
        self.assertIsNone(sample['elapsedSeconds'])
        validate_samples(self.catalog, self.data)


class EnvironmentGuardTests(unittest.TestCase):
    def test_unverified_toolchain_blocks_android(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            (root/'config').mkdir()
            (root/'config/toolchain.json').write_text('{"verified": false}', encoding='utf-8')
            with patch.object(doctor, 'ROOT', root), self.assertRaises(ValueError):
                doctor.recorded_toolchain()

    def test_boolean_java_version_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            (root/'config').mkdir()
            config = {key: 'set' for key in doctor.FIELDS}
            config.update(verified=True, verifiedBy='A', verifiedAt='test', javaMajor=True)
            import json
            (root/'config/toolchain.json').write_text(json.dumps(config), encoding='utf-8')
            with patch.object(doctor, 'ROOT', root), self.assertRaises(ValueError):
                doctor.recorded_toolchain()


if __name__ == '__main__':
    unittest.main()
