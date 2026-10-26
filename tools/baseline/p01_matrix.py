#!/usr/bin/env python3
"""Capture paired representative states, restricted to isolated native test apps."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import time

import capture_android as ui
import p00
import p01_android_reminders as reminders
import p01_ios


def ready(name, label):
    for attempt in range(10):
        tree = ui.tree(name)
        if label in reminders.labels(tree):
            return tree
        time.sleep(0.5)
    raise RuntimeError('Expected screen did not become ready: ' + label)


def android(case, out):
    ui.OUT = reminders.OUT = out
    p00.android(ui.SERIAL, case)
    ui.adb('shell', 'cmd', 'statusbar', 'collapse')
    tree = ready('home', 'ওডোমিটার')
    ui.screenshot('home')
    ui.tap_label(tree, 'কাগজপত্র')
    tree = ready('vault', 'Tax token')
    ui.screenshot('vault')
    ui.tap_label(tree, 'আমি')
    ready('me', 'বাইক বদলান')
    ui.screenshot('me')
    reminders.open_settings()
    ready('reminder-settings', 'মেয়াদের কত দিন আগে')
    ui.screenshot('reminder-settings')
    return dict(serial=ui.SERIAL, api=ui.adb('shell', 'getprop', 'ro.build.version.sdk'),
                osAppearance=ui.adb('shell', 'cmd', 'uimode', 'night'))


def ios(case, out, device, run):
    selected = p01_ios.validate_device(device)
    p01_ios.install_host(device)
    p00.ios(device, case)
    result = p00.ROOT / 'build' / ('p01-matrix-' + case + '-' + run + '.xcresult')
    assert not result.exists(), 'Keep existing runs; use a new result name after diagnosis'
    args = ['xcodebuild', 'test-without-building', '-xctestrun',
            str(p00.ROOT / 'build/p01-ios-tests-derived/Build/Products/MotoVault_iphonesimulator27.0-arm64-x86_64.xctestrun'),
            '-destination', 'id=' + device,
            '-only-testing:MotoVaultUITests/P01DeliveryTests/testRepresentativeScreens',
            '-resultBundlePath', str(result), '-collect-test-diagnostics', 'never']
    p00.run(*args)
    summary = json.loads(p00.run('xcrun', 'xcresulttool', 'get', 'test-results', 'summary', '--path', str(result)))
    p00.write_json(out / 'summary.json', summary)
    assert summary['result'] == 'Passed' and summary['passedTests'] == 1
    exported = p00.ROOT / 'build' / ('p01-matrix-' + case + '-' + run + '-attachments')
    p00.run('xcrun', 'xcresulttool', 'export', 'attachments', '--path', str(result), '--output-path', str(exported))
    for test in json.loads((exported / 'manifest.json').read_text()):
        for item in test['attachments']:
            name = item['suggestedHumanReadableName']
            if name.startswith('matrix-'):
                shutil.copyfile(exported / item['exportedFileName'], out / (name.split('_0_')[0] + Path(item['exportedFileName']).suffix))
    selected['osAppearance'] = p00.run('xcrun', 'simctl', 'ui', device, 'appearance')
    return selected


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('platform', choices=['android', 'ios'])
    parser.add_argument('--device')
    parser.add_argument('--run', default='r2')
    parser.add_argument('--system-dark', action='store_true')
    args = parser.parse_args()
    p00.FIXTURES = p00.ROOT / 'docs/roadmap/fixtures/p01'
    if args.platform == 'ios':
        p01_ios.validate_device(args.device)
    def appearance(value=None):
        if args.platform == 'ios':
            return p00.run('xcrun', 'simctl', 'ui', args.device, 'appearance', *([value] if value else []))
        return ui.adb('shell', 'cmd', 'uimode', 'night', *([value] if value else [])).removeprefix('Night mode: ').strip()
    original = appearance() if args.system_dark else None
    try:
        if args.system_dark:
            appearance('dark' if args.platform == 'ios' else 'yes')
        cases = ['system-appearance'] if args.system_dark else ['runtime', 'unknown-large']
        for case in cases:
            capture_case(args, case)
    finally:
        if args.system_dark:
            appearance(original)
            restored = appearance()
            p00.write_json(p00.ROOT / 'docs/roadmap/evidence/p01' / args.platform / ('system-appearance-restoration-' + args.run + '.json'),
                           dict(original=original, restored=restored, scope='Only the selected simulator/emulator OS appearance; owner app records untouched.'))
            assert restored == original


def capture_case(args, case):
    out = p00.ROOT / 'docs/roadmap/evidence/p01' / args.platform / ('matrix-' + case + '-' + args.run)
    out.mkdir(exist_ok=False)
    source = p00.FIXTURES / 'native' / (case + '.json')
    value = json.loads(source.read_text())
    host = android(case, out) if args.platform == 'android' else ios(case, out, args.device, args.run)
    p00.write_json(out / 'capture.json', dict(case=case, applicationId=p00.APP, synthetic=True,
        fixture=str(source.relative_to(p00.ROOT)), fixtureSha256=hashlib.sha256(source.read_bytes()).hexdigest(),
        appearance=value.get('appearance'), digits=value.get('digits'), textScale=value.get('textScale'),
        appReducedMotion=value.get('reduceMotion'), host=host,
        scope='Representative native screenshots and reachable controls, not motion parity or spoken accessibility proof.'))
    print(args.platform + ' ' + case + ' captured', flush=True)


if __name__ == '__main__':
    main()
