from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import verify_release as release


class ReleaseVerificationTests(unittest.TestCase):
    digest = 'a' * 64
    signature = ('Verifies\nVerified using v2 scheme (APK Signature Scheme v2): true\n'
                 'Signer #1 certificate DN: CN=Prototype Team\n'
                 'Signer #1 certificate SHA-256 digest: ' + digest + '\n')
    badging = ("package: name='io.kailand' versionCode='2' versionName='0.1.0'\n"
               "minSdkVersion:'28'\ntargetSdkVersion:'37'\n"
               "launchable-activity: name='io.kailand.app.MainActivity' label='MEMORIDE'\n"
               "native-code: 'arm64-v8a' 'x86_64'\n")
    toolchain = dict(applicationId='io.kailand', minSdk=28, targetSdk=37)

    def test_intended_certificate_and_valid_release_metadata_pass(self):
        self.assertEqual(self.digest, release.certificate(self.signature, ':'.join(['AA'] * 32)))
        info = release.package_info(self.badging, self.toolchain)
        self.assertEqual(2, info['version_code'])
        self.assertEqual(['arm64-v8a', 'x86_64'], info['native_abis'])

    def test_different_signer_cannot_replace_intended_identity(self):
        with self.assertRaisesRegex(ValueError, 'intended team'):
            release.certificate(self.signature, 'b' * 64)

    def test_debug_certificate_is_rejected_even_when_its_fingerprint_matches(self):
        text = self.signature.replace('CN=Prototype Team', 'C=US, O=Android, CN=Android Debug')
        with self.assertRaisesRegex(ValueError, 'debug certificate'):
            release.certificate(text, self.digest)

    def test_v1_only_and_multiple_signers_do_not_pass(self):
        for text in [self.signature.replace('): true', '): false'),
                     self.signature + 'Signer #2 certificate SHA-256 digest: ' + self.digest + '\n']:
            with self.assertRaises(ValueError):
                release.certificate(text, self.digest)

    def test_wrong_package_debuggable_extra_launcher_and_sdk_are_rejected(self):
        invalid = [self.badging.replace("name='io.kailand'", "name='other.app'"),
                   self.badging + 'application-debuggable\n',
                   self.badging + "launchable-activity: name='io.kailand.app.DevelopmentActivity'\n",
                   self.badging.replace("minSdkVersion:'28'", "minSdkVersion:'27'"),
                   self.badging.replace("targetSdkVersion:'37'", "targetSdkVersion:'36'"),
                   self.badging.replace("versionCode='2'", "versionCode='0'")]
        for text in invalid:
            with self.subTest(text=text), self.assertRaises(ValueError):
                release.package_info(text, self.toolchain)

    def test_missing_catalog_and_debug_dex_are_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            apk = Path(folder) / 'candidate.apk'
            for catalog, dex in [(False, b'normal'), (True, b'Lio/kailand/app/DevelopmentActivity;')]:
                with zipfile.ZipFile(apk, 'w') as archive:
                    archive.writestr('AndroidManifest.xml', b'manifest')
                    archive.writestr('classes.dex', dex)
                    if catalog:
                        archive.writestr('assets/content/catalog.json', b'{}')
                with self.assertRaises(ValueError):
                    release.packaged_assets(apk)

    def test_unsigned_tool_failure_never_returns_verification_success(self):
        with patch.object(release.subprocess, 'run') as run:
            run.return_value.returncode = 1
            run.return_value.stderr = 'untrusted diagnostics'
            with self.assertRaisesRegex(ValueError, 'verification did not pass'):
                release.run_tool(['java', 'verify'], 'apksigner verify')

    def test_signing_environment_is_not_forwarded_to_verifier(self):
        private = {key: 'private-test-value' for key in release.SIGNING_ENV}
        with patch.dict(release.os.environ, private), patch.object(release.subprocess, 'run') as run:
            run.return_value.returncode = 0
            run.return_value.stdout = 'Verifies'
            release.run_tool(['java', 'verify'], 'apksigner verify')
            self.assertTrue(release.SIGNING_ENV.isdisjoint(run.call_args.kwargs['env']))

    def test_failed_cli_does_not_issue_a_pass_report(self):
        with tempfile.TemporaryDirectory() as folder:
            report = Path(folder) / 'handoff.json'
            args = ['verify_release.py', 'candidate.apk', '--build-tools', folder,
                    '--expected-cert-sha256', self.digest, '--source-sha', 'a' * 40, '--report', str(report)]
            with patch.object(sys, 'argv', args), patch.object(release, 'verify', side_effect=ValueError('invalid APK')):
                self.assertEqual(2, release.main())
            self.assertFalse(report.exists())


if __name__ == '__main__':
    unittest.main()
