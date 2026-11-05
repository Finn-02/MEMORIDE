from pathlib import Path
import argparse
import os
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import build_signed_release as private


class PrivateReleaseBuildTests(unittest.TestCase):
    def test_noninteractive_ai_console_never_prompts_for_credentials(self):
        argv = ['build_signed_release.py', '--version-code', '2', '--build-tools', '.',
                '--expected-cert-sha256', 'a' * 64]
        with patch.object(sys, 'argv', argv), patch.object(sys.stdin, 'isatty', return_value=False), \
                patch.object(private, 'build') as build:
            self.assertEqual(2, private.main())
            build.assert_not_called()

    def test_candidate_version_must_fit_android_range(self):
        self.assertEqual(2, private.version_code('2'))
        for value in ['0', '-1', '2100000001', 'not-a-number']:
            with self.assertRaises(argparse.ArgumentTypeError):
                private.version_code(value)

    def test_failed_build_does_not_keep_signing_fields_or_modify_parent_environment(self):
        with tempfile.TemporaryDirectory() as folder:
            placeholder = Path(folder) / 'placeholder'
            placeholder.write_text('not a real key')
            before = dict(os.environ)
            with patch('builtins.input', side_effect=[str(placeholder), 'test-team']), \
                    patch.object(private.getpass, 'getpass', return_value='test-only'), \
                    patch.object(private.subprocess, 'run') as run:
                run.return_value.returncode = 1
                with self.assertRaises(ValueError):
                    private.build(2)
                self.assertTrue(private.verify_release.SIGNING_ENV.isdisjoint(run.call_args.kwargs['env']))
                self.assertFalse(any('test-only' in str(arg) for arg in run.call_args.args[0]))
            self.assertEqual(before, dict(os.environ))

    def test_stale_apk_version_never_creates_a_candidate(self):
        with tempfile.TemporaryDirectory() as folder:
            tools = Path(folder) / 'tools'
            (tools / 'lib').mkdir(parents=True)
            (tools / 'lib/apksigner.jar').write_text('placeholder')
            output = Path(folder) / 'output'
            argv = ['build_signed_release.py', '--version-code', '2', '--build-tools', str(tools),
                    '--expected-cert-sha256', 'a' * 64, '--output-dir', str(output)]
            with patch.object(sys, 'argv', argv), patch.object(sys.stdin, 'isatty', return_value=True), \
                    patch.object(private, 'source_sha', return_value='b' * 40), \
                    patch.object(private, 'build'), \
                    patch.object(private.verify_release, 'verify', return_value={'version_code': 1}):
                self.assertEqual(2, private.main())
            self.assertFalse(output.exists())
