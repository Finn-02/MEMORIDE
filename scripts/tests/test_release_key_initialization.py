from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import initialize_release_key as initialization


class KeyInitializationTests(unittest.TestCase):
    def test_existing_key_is_never_overwritten_or_read(self):
        with tempfile.TemporaryDirectory() as folder:
            key = Path(folder) / 'memoride-release.p12'
            key.write_bytes(b'not a real key')
            with self.assertRaisesRegex(ValueError, 'nothing will be overwritten'):
                initialization.locations(Path(folder), False)
            self.assertEqual(b'not a real key', key.read_bytes())

    def test_ai_console_stops_before_keytool_or_password_input(self):
        argv = ['initialize_release_key.py', '--confirmed-no-existing-key', '--build-tools', '.']
        with patch.object(sys, 'argv', argv), patch.object(sys.stdin, 'isatty', return_value=False), \
                patch.object(initialization.subprocess, 'run') as run:
            self.assertEqual(2, initialization.main())
            run.assert_not_called()
