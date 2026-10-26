#!/usr/bin/env python3
"""Record only the isolated iOS host, on an explicitly validated iOS 27 device."""
import argparse
import json
import os
import shlex
import shutil
import signal
import subprocess
import time

import p00
import p01_ios


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--device', required=True)
    parser.add_argument('--run', default='r2')
    args = parser.parse_args()
    p01_ios.validate_device(args.device)
    p01_ios.install_host(args.device)
    p00.FIXTURES = p00.ROOT / 'docs/roadmap/fixtures/p01'
    p00.ios(args.device, 'runtime')
    out = p00.ROOT / 'docs/roadmap/evidence/p01/ios' / ('motion-os-' + args.run)
    out.mkdir(exist_ok=False)
    result = p00.ROOT / 'build' / ('p01-ios-os-motion-' + args.run + '.xcresult')
    assert not result.exists()
    command = ['xcrun', 'simctl', 'io', args.device, 'recordVideo', '--codec=h264', str(out / 'live-os-motion.mov')]
    with (p00.ROOT / 'build/p01-ios-os-motion-recording.log').open('w') as log:
        recorder = subprocess.Popen(['rtk', 'run', '-c', 'exec ' + shlex.join(command)],
                                    stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
        try:
            time.sleep(0.7)
            p00.run('xcodebuild', 'test-without-building', '-xctestrun',
                    str(p00.ROOT / 'build/p01-ios-tests-derived/Build/Products/MotoVault_iphonesimulator27.0-arm64-x86_64.xctestrun'),
                    '-destination', 'id=' + args.device,
                    '-only-testing:MotoVaultUITests/P01DeliveryTests/testSystemReducedMotionLiveReturn',
                    '-resultBundlePath', str(result), '-collect-test-diagnostics', 'never')
        finally:
            os.killpg(recorder.pid, signal.SIGINT)
            recorder.wait(timeout=15)
            if result.exists():
                summary = json.loads(p00.run('xcrun', 'xcresulttool', 'get', 'test-results', 'summary', '--path', str(result)))
                p00.write_json(out / 'summary.json', summary)
                exported = p00.ROOT / 'build' / ('p01-ios-os-motion-' + args.run + '-attachments')
                p00.run('xcrun', 'xcresulttool', 'export', 'attachments', '--path', str(result), '--output-path', str(exported))
                for test in json.loads((exported / 'manifest.json').read_text()):
                    for item in test['attachments']:
                        name = item['suggestedHumanReadableName']
                        if name.startswith(('original-os-', 'restored-os-', 'os-reduced-', 'os-normal-', 'os-motion-')):
                            source = exported / item['exportedFileName']
                            shutil.copyfile(source, out / (name.split('_0_')[0] + source.suffix))
    print('iOS live OS motion test completed; review summary and restored preference evidence.', flush=True)


if __name__ == '__main__':
    main()
