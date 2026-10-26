#!/usr/bin/env python3
"""Retain and validate P01 evidence without inferring native or device completion."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET
from p01_physical_evidence import evaluate as evaluate_physical, evaluate_core

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'docs/roadmap/evidence/p01'
MANIFEST = OUT / 'provenance.json'


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def source_files():
    directories = [ROOT / 'composeApp/src', ROOT / 'iosApp/MotoVault',
                   ROOT / 'design/app-ui-v3/src', ROOT / 'design/app-ui-v3/tokens',
                   ROOT / 'tools/baseline']
    return sorted(p for folder in directories for p in folder.rglob('*')
                  if p.is_file() and p.suffix in {'.kt', '.swift', '.json', '.tsx', '.ts', '.css', '.py', '.gradle'})


def entry(path):
    return dict(path=str(path.relative_to(ROOT)), sha256=digest(path))


def collect():
    base = subprocess.run(['rtk', 'run', '-c', 'git rev-parse HEAD'], cwd=ROOT,
                          capture_output=True, text=True, check=True).stdout.strip()
    products = [ROOT / 'composeApp/build/outputs/apk/debug/composeApp-debug.apk',
                ROOT / 'build/p01-ios-tests-derived/Build/Products/Debug-iphonesimulator/MotoVault.app/MotoVault',
                ROOT / 'build/p01-ios-tests-derived/Build/Products/Debug-iphonesimulator/MotoVault.app/MotoVault.debug.dylib']
    value = dict(baseCommit=base, revision='uncommitted P01 working change on ' + base,
                 applicationId='com.motovault.baseline.p00', syntheticOnly=True,
                 scope='Collected source and artifact fingerprints; hashes alone do not establish runtime parity or physical-device validation.',
                 sources=[entry(p) for p in source_files()],
                 buildProducts=[entry(p) for p in products if p.exists()],
                 reports=[entry(p) for p in sorted((ROOT / 'docs/roadmap/evidence').glob('P01*.md'))],
                 fixtures=[entry(p) for p in sorted((ROOT / 'docs/roadmap/fixtures/p01').rglob('*.json'))],
                 evidence=[entry(p) for p in sorted(OUT.rglob('*')) if p.is_file() and p != MANIFEST],
                 pending=['Physical Android acceptance is partial; see P01-10-physical-android.md. Physical iPhone remains unavailable.',
                          'Any unresolved emulator/simulator runtime checks in P01-03-verification.md.',
                          'Design acceptance items in P01-design.md; native trees do not prove spoken screen-reader output.'])
    MANIFEST.write_text(json.dumps(value, indent=2, ensure_ascii=False) + '\n')


def check():
    physical = evaluate_physical('moto-background-r1')
    retained_physical = json.loads((OUT / 'android-device/moto-background-r1/partial-result.json').read_text())
    assert retained_physical == physical and physical['result'] == 'Partial'
    core = evaluate_core('moto-background-r2')
    retained_core = json.loads((OUT / 'android-device/moto-background-r2/core-result.json').read_text())
    assert core == retained_core and core['result'] == 'Passed' and not core['phaseComplete']
    manifest = json.loads(MANIFEST.read_text())
    assert manifest['syntheticOnly'] and manifest['applicationId'] == 'com.motovault.baseline.p00'
    assert manifest['pending'], 'Do not erase pending gates by collecting fingerprints'
    for group in ['sources', 'fixtures', 'evidence', 'reports']:
        for item in manifest[group]:
            path = ROOT / item['path']
            assert path.is_file() and digest(path) == item['sha256'], 'Changed or missing: ' + item['path']
    for host in ['android', 'ios']:
        files = list((OUT / 'tests' / host).glob('TEST-*.xml'))
        assert files
        roots = [ET.parse(p).getroot() for p in files]
        assert sum(int(r.get('tests', 0)) for r in roots) > 0
        assert all(int(r.get('failures', 0)) == int(r.get('errors', 0)) == 0 for r in roots)
    for name in ['denial-r5', 'undo-r2', 'unknown-large-r1', 'save-failure-r2']:
        result = json.loads((OUT / 'ios' / (name + '-summary.json')).read_text())
        assert result['result'] == 'Passed' and result['failedTests'] == 0, name
    for name, count in [('native-r1', 4), ('large-native-r1', 4),
                        ('navigation-native-r2', 1), ('navigation-large-native-r2', 1),
                        ('empty-native-r1', 1), ('keypad-native-r1', 1),
                        ('keypad-large-native-r1', 1), ('keypad-sheet-policy-r1', 1),
                        ('keypad-large-sheet-policy-r1', 1)]:
        folder = OUT / 'ios' / ('acceptance-' + name)
        result = json.loads((folder / 'summary.json').read_text())
        assert result['result'] == 'Passed' and result['passedTests'] == count and result['failedTests'] == 0, name
        device = result['devicesAndConfigurations'][0]['device']
        assert device['osVersion'].startswith('27.') and device['modelName'] == 'iPhone 18 Pro Max'
        run = json.loads((folder / 'run.json').read_text())
        assert run['synthetic'] and run['applicationId'] == 'com.motovault.baseline.p00'
        assert (folder / 'attachments/manifest.json').is_file()
    for case in ['runtime', 'unknown-large']:
        folder = OUT / 'android' / ('navigation-' + case + '-native-r1')
        result = json.loads((folder / 'result.json').read_text())
        assert result['result'] == 'Passed' and result['synthetic'] and result['architecture'] == 'arm64-v8a'
        assert json.loads((folder / 'before.json').read_text()) == json.loads((folder / 'after.json').read_text())
        keypad = OUT / 'android' / ('keypad-' + case + '-native-r3')
        result = json.loads((keypad / 'result.json').read_text())
        assert result['result'] == 'Passed' and result['synthetic'] and not result['platformKeyboardShown']
        assert len(result['keyBounds']) == 11
        assert result['saveBounds'][3] < min(rect[1] for rect in result['keyBounds'].values())
        assert json.loads((keypad / 'before.json').read_text()) == json.loads((keypad / 'after.json').read_text())
        for kind in ['navigation', 'keypad']:
            current = OUT / 'android' / (kind + '-' + case + '-sheet-policy-r1')
            result = json.loads((current / 'result.json').read_text())
            assert result['result'] == 'Passed' and result['synthetic']
            assert json.loads((current / 'before.json').read_text()) == json.loads((current / 'after.json').read_text())
    current_motion = OUT / 'android/motion-os-native-size-r2'
    motion_policy = json.loads((current_motion / 'observations.json').read_text())
    assert motion_policy['app'] == 'com.motovault.baseline.p00' and motion_policy['synthetic']
    assert motion_policy['originalScale'] == motion_policy['restoredScale']
    assert [row['osAnimatorScale'] for row in motion_policy['observations']] == ['1', '0', '1']
    for name in ['normal', 'os-reduced-live', 'os-restored-live']:
        assert (current_motion / (name + '.mp4')).stat().st_size > 0
    delivery = json.loads((OUT / 'ios/background-r8/summary.json').read_text())
    assert delivery['result'] == 'Passed' and delivery['passedTests'] == 1 and delivery['failedTests'] == 0
    device = delivery['devicesAndConfigurations'][0]['device']
    assert device['osVersion'].startswith('27.') and device['modelName'] in {'iPhone 18 Pro', 'iPhone 18 Pro Max'}
    recovery = json.loads((OUT / 'ios/recovery-r4/summary.json').read_text())
    assert recovery['result'] == 'Passed' and recovery['passedTests'] == 1 and recovery['failedTests'] == 0
    android_retention = json.loads((OUT / 'android/android-retention-20261025-012210/result.json').read_text())
    assert android_retention['result'] == 'Passed' and android_retention['synthetic']
    assert android_retention['app'] == 'com.motovault.baseline.p00'
    motion = json.loads((OUT / 'android/motion-os/observations.json').read_text())
    assert motion['app'] == 'com.motovault.baseline.p00' and motion['synthetic']
    assert motion['originalScale'] == motion['restoredScale']
    assert [(item['name'], item['osAnimatorScale']) for item in motion['observations']] == [
        ('normal', '1'), ('os-reduced-live', '0'), ('os-restored-live', '1')]
    videos = [json.loads(line) for line in (OUT / 'android/motion-os/video-inspection.jsonl').read_text().splitlines()]
    assert len(videos) == 3 and all(item['decodedFrame'] and item['durationSeconds'] > 0 for item in videos)
    ios_motion = OUT / 'ios/motion-os-r3'
    result = json.loads((ios_motion / 'summary.json').read_text())
    assert result['result'] == 'Passed' and result['passedTests'] == 1 and result['failedTests'] == 0
    original = (ios_motion / 'original-os-reduce-motion.txt').read_text().strip()
    assert original in {'0', '1'}
    assert "label: 'Reduce Motion', value: " + original in (ios_motion / 'restored-os-motion-settings.txt').read_text()
    for host in ['android', 'ios']:
        for case in ['runtime', 'unknown-large', 'system-appearance']:
            folder = OUT / host / ('matrix-' + case + '-r3')
            metadata = json.loads((folder / 'capture.json').read_text())
            assert metadata['synthetic'] and metadata['applicationId'] == 'com.motovault.baseline.p00'
            assert digest(ROOT / metadata['fixture']) == metadata['fixtureSha256']
            for screen in ['home', 'vault', 'me', 'reminder-settings']:
                name = ('matrix-' if host == 'ios' else '') + screen
                assert (folder / (name + '.png')).is_file()
                assert (folder / (name + ('-tree.txt' if host == 'ios' else '.xml'))).is_file()
            if host == 'ios':
                result = json.loads((folder / 'summary.json').read_text())
                assert result['result'] == 'Passed' and result['passedTests'] == 1 and result['failedTests'] == 0
                assert metadata['host']['runtime'].startswith('com.apple.CoreSimulator.SimRuntime.iOS-27-')
        restoration = json.loads((OUT / host / 'system-appearance-restoration-r3.json').read_text())
        assert restoration['original'] == restoration['restored']
    snooze = OUT / 'ios/snooze-r1'
    before = json.loads((snooze / 'before.json').read_text())
    after = json.loads((snooze / 'after.json').read_text())
    for field in ['odometer', 'odometerConfirmedDate', 'preferences', 'logCount']:
        assert before[field] == after[field], field
    for group in ['documents', 'rules']:
        assert [{k: v for k, v in item.items() if k != 'snooze'} for item in before[group]] == [
            {k: v for k, v in item.items() if k != 'snooze'} for item in after[group]]
    assert next(item for item in after['documents'] if item['id'] == 'p01-today')['snooze'] == '2026-10-26T09:00'
    assert next(item for item in after['rules'] if item['id'] == 'p00-chain')['snooze'] == '2026-10-26T09:00'
    def test_nodes(node):
        if isinstance(node, dict):
            yield node
            for value in node.values():
                yield from test_nodes(value)
        elif isinstance(node, list):
            for value in node:
                yield from test_nodes(value)
    snooze_tests = json.loads((snooze / 'tests.json').read_text())
    assert any(node.get('nodeIdentifier') == 'P01DeliveryTests/testDocumentAndServiceSnooze()'
               and node.get('result') == 'Passed' for node in test_nodes(snooze_tests))
    for host in ['android', 'ios']:
        assert json.loads((OUT / host / 'before-failed-save.json').read_text()) == json.loads((OUT / host / 'after-failed-save.json').read_text())
    assert json.loads((OUT / 'android/final-before-failed-save.json').read_text()) == json.loads((OUT / 'android/final-after-failed-save.json').read_text())
    paired = json.loads((ROOT / 'docs/roadmap/fixtures/p01/paired.json').read_text())
    assert paired == json.loads((ROOT / 'design/app-ui-v3/public/p01-fixtures.json').read_text())
    for case in paired:
        assert case['synthetic'] and case['native']['ride']['registration'] == 'P00-SYNTHETIC'
        assert [r['id'] for r in case['native']['rules']] == [r['id'] for r in case['prototype']['rules']]
        assert [d['id'] for d in case['native']['documents']] == [d['id'] for d in case['prototype']['docs']]
    print('P01 evidence fingerprints, retained passing tests, failed-save preservation and paired fixture identities valid. Pending gates remain open.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('mode', choices=['collect', 'check'])
    args = parser.parse_args()
    if args.mode == 'collect':
        collect()
    check()
