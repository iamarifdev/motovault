#!/usr/bin/env python3
"""Retain isolated iOS reminder audits, navigation and numeric layout checks."""
import argparse
import json
import shutil

import p00
import p01_ios


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--device', required=True)
    parser.add_argument('--run', required=True)
    parser.add_argument('--case', choices=['runtime', 'unknown-large', 'empty'], default='runtime')
    selection = parser.add_mutually_exclusive_group()
    selection.add_argument('--navigation-only', action='store_true')
    selection.add_argument('--keypad-only', action='store_true')
    args = parser.parse_args()
    selected = p01_ios.validate_device(args.device)
    assert selected['name'] == 'iPhone 18 Pro Max', 'Use the owner-selected Pro Max'
    assert args.run.replace('-', '').isalnum(), 'Use a simple unique run name'
    out = p00.ROOT / 'docs/roadmap/evidence/p01/ios' / ('acceptance-' + args.run)
    result = p00.ROOT / 'build' / ('p01-acceptance-' + args.run + '.xcresult')
    assert not out.exists() and not result.exists(), 'Preserve prior runs'
    out.mkdir()
    p01_ios.install_host(args.device)
    if args.case == 'empty':
        fixture = 'unknown-service'
    else:
        p00.FIXTURES = p00.ROOT / 'docs/roadmap/fixtures/p01'
        fixture = args.case
    p00.ios(args.device, fixture)
    tests = ['testReminderAccessibilityAudit', 'testDocumentReminderAccessibilityAudit',
             'testServiceReminderAccessibilityAudit', 'testReminderDragDismissDoesNotSave']
    if args.navigation_only:
        tests = ['testReminderDragDismissDoesNotSave']
    elif args.keypad_only:
        tests = ['testNumericKeypadSafeAreaAndCancel']
    elif args.case == 'empty':
        tests = ['testEmptyReminderAccessibilityAudit']
    command = ['xcodebuild', 'test-without-building', '-xctestrun',
               str(p00.ROOT / 'build/p01-ios-tests-derived/Build/Products/MotoVault_iphonesimulator27.0-arm64-x86_64.xctestrun'),
               '-destination', 'id=' + args.device, '-resultBundlePath', str(result),
               '-collect-test-diagnostics', 'never']
    command += ['-only-testing:MotoVaultUITests/P01DeliveryTests/' + test for test in tests]
    try:
        p00.run(*command)
    finally:
        if result.exists():
            summary = json.loads(p00.run('xcrun', 'xcresulttool', 'get', 'test-results', 'summary', '--path', str(result)))
            p00.write_json(out / 'summary.json', summary)
            exported = p00.ROOT / 'build' / ('p01-acceptance-' + args.run + '-attachments')
            p00.run('xcrun', 'xcresulttool', 'export', 'attachments', '--path', str(result), '--output-path', str(exported))
            shutil.copytree(exported, out / 'attachments')
            p00.write_json(out / 'run.json', dict(device=selected, fixture=args.case,
                synthetic=True, applicationId=p00.APP, host=p01_ios.inspect(),
                tests=tests, scope=('XCTest numeric keypad bounds and cancellation only. No spoken VoiceOver or physical-device claim.'
                    if args.keypad_only else 'XCTest automated audits and drag dismissal only. No spoken VoiceOver or physical-device claim.')))
            print(json.dumps({k: summary[k] for k in ['result', 'passedTests', 'failedTests']}, indent=2), flush=True)


if __name__ == '__main__':
    main()
