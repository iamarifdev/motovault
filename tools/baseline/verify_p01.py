#!/usr/bin/env python3
"""Retain and validate P01 evidence without inferring native or device completion."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'docs/roadmap/evidence/p01'
MANIFEST = OUT / 'provenance.json'


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def source_files():
    directories = [ROOT / 'composeApp/src', ROOT / 'iosApp/MotoVault',
                   ROOT / 'design/app-ui-v3/src', ROOT / 'design/app-ui-v3/tokens']
    return sorted(p for folder in directories for p in folder.rglob('*')
                  if p.is_file() and p.suffix in {'.kt', '.swift', '.json', '.tsx', '.ts', '.css'})


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
                 pending=['Physical Android and iPhone background/denial/restart/settings checks, explicitly deferred by owner.',
                          'iOS permission recovery and any unresolved simulator runtime checks in P01-03-verification.md.',
                          'Design acceptance items in P01-design.md; native trees do not prove spoken screen-reader output.'])
    MANIFEST.write_text(json.dumps(value, indent=2, ensure_ascii=False) + '\n')


def check():
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
