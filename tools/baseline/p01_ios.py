#!/usr/bin/env python3
"""Prepare and check only MotoVault's isolated local iOS simulator test host."""
import argparse
import hashlib
import json
from pathlib import Path
import plistlib
import shlex
import shutil
import subprocess

import p00

DISPLAY_NAME = 'MotoVault P01 Test'
HOST = p00.ROOT / 'build/p00-ios-host'
APP = p00.ROOT / 'build/p01-ios-tests-derived/Build/Products/Debug-iphonesimulator/MotoVault.app'


def validate_device(device):
    listing = json.loads(p00.run('xcrun', 'simctl', 'list', 'devices', 'booted', '--json'))
    for runtime, devices in listing['devices'].items():
        for selected in devices:
            if selected['udid'] == device:
                assert runtime.startswith('com.apple.CoreSimulator.SimRuntime.iOS-27-'), 'Use iOS 27 only'
                assert selected['deviceTypeIdentifier'] in {
                    'com.apple.CoreSimulator.SimDeviceType.iPhone-18-Pro',
                    'com.apple.CoreSimulator.SimDeviceType.iPhone-18-Pro-Max'
                }, 'Use an iPhone 18 Pro or Pro Max'
                return dict(udid=device, name=selected['name'], runtime=runtime)
    raise ValueError('Pass the UUID of the verified running iOS 27 Pro/Pro Max simulator')


def prepare():
    p00.prepare_ios_tests()
    info = HOST / 'MotoVault/Info.plist'
    value = plistlib.loads(info.read_bytes())
    value['CFBundleDisplayName'] = DISPLAY_NAME
    info.write_bytes(plistlib.dumps(value))
    shutil.copyfile(p00.ROOT / 'tools/baseline/P01DeliveryTests.swift',
                    HOST / 'MotoVaultUITests/OwnerFlowTests.swift')
    print('Prepared copied simulator host with isolated bundle ID and unique system display name.')


def install_host(device):
    validate_device(device)
    inspect()
    p00.run('xcrun', 'simctl', 'install', device, str(APP))


def inspect():
    info = plistlib.loads((APP / 'Info.plist').read_bytes())
    assert info['CFBundleIdentifier'] == p00.APP, 'Refusing a non-isolated app'
    assert info['CFBundleDisplayName'] == DISPLAY_NAME, 'Use the unique test name to avoid owner-app ambiguity'
    result = subprocess.run(['rtk', 'run', '-c', shlex.join(['codesign', '-d', '--verbose=3', str(APP)])],
                            capture_output=True, text=True, check=True)
    lines = (result.stdout + result.stderr).splitlines()
    identifier = next(line.split('=', 1)[1] for line in lines if line.startswith('Identifier='))
    assert identifier == p00.APP, 'Signing identifier must match the isolated app bundle ID'
    assert any(line.startswith('Info.plist entries=') for line in lines), 'Info.plist must be bound by the local signature'
    p00.run('codesign', '--verify', '--deep', '--strict', str(APP))
    return dict(bundleId=p00.APP, displayName=DISPLAY_NAME, signingIdentifier=identifier,
                scope='Local ad-hoc simulator test packaging only; not a signed device or release build.',
                products=[dict(path=str(p.relative_to(p00.ROOT)), sha256=hashlib.sha256(p.read_bytes()).hexdigest())
                          for p in [APP / info['CFBundleExecutable'], APP / 'MotoVault.debug.dylib']])


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('mode', choices=['prepare', 'sign-host', 'check-host'])
    args = parser.parse_args()
    if args.mode == 'prepare':
        prepare()
    else:
        if args.mode == 'sign-host':
            info = plistlib.loads((APP / 'Info.plist').read_bytes())
            assert info['CFBundleIdentifier'] == p00.APP and info['CFBundleDisplayName'] == DISPLAY_NAME
            p00.run('codesign', '--force', '--deep', '--sign', '-', str(APP))
        print(json.dumps(inspect(), indent=2))
