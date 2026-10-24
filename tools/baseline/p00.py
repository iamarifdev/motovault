#!/usr/bin/env python3
"""Generate and load synthetic P00 fixtures into isolated baseline identities only."""
from __future__ import annotations

import argparse
import copy
import datetime as dt
import hashlib
import json
from pathlib import Path
import plistlib
import shlex
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
FIXTURES = ROOT / 'docs/roadmap/fixtures/p00'
APP = 'com.motovault.baseline.p00'
SCENARIOS = ('empty', 'overdue', 'partial-fill', 'documents', 'unknown-service')


def run(*args: str, check: bool = True) -> str:
    result = subprocess.run(['rtk', 'run', '-c', shlex.join(args)], cwd=ROOT,
                            capture_output=True, text=True)
    if check and result.returncode:
        raise RuntimeError(f'{args[0]} failed ({result.returncode}): {(result.stdout + result.stderr)[-3000:]}')
    return result.stdout.strip()


def write_json(path: Path, data: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n')


def generate(reference: str) -> None:
    day = dt.date.fromisoformat(reference)
    date = lambda days: (day + dt.timedelta(days=days)).isoformat()
    epoch = lambda days: (day + dt.timedelta(days=days) - dt.date(1970, 1, 1)).days
    native_ride = dict(brandId='yamaha', edition='P00 synthetic', year=2023, odometer=12340.0,
                       modelId='yamaha-r15-v4', modelName='R15 V4', registration='P00-SYNTHETIC',
                       market='BANGLADESH', emission='UNKNOWN')
    proto_ride = dict(brandId='yamaha', modelId='yamaha-r15-v4', name='R15 V4', year=2023,
                      odometer=12340, odometerAt=reference, startedAt=date(-100))
    rules = [dict(id='p00-chain', vehicleId='primary', name='Chain',
                  specification='P00 synthetic owner-defined interval; no factory claim',
                  intervalKm=500.0, intervalDays=30, lastServicedOdometerKm=11520.0,
                  lastServicedEpochDays=epoch(-41), isCritical=False),
             dict(id='p00-oil', vehicleId='primary', name='Engine oil',
                  specification='P00 synthetic owner-defined interval; no factory claim',
                  intervalKm=3000.0, intervalDays=180, lastServicedOdometerKm=11350.0,
                  lastServicedEpochDays=epoch(-45), isCritical=True)]
    proto_rules = [dict(id=r['id'], item='chain_sprocket' if r['name']=='Chain' else 'engine_oil',
                        name='চেইন' if r['name']=='Chain' else 'ইঞ্জিন অয়েল',
                        intervalKm=r['intervalKm'], intervalDays=r['intervalDays'],
                        lastKm=r['lastServicedOdometerKm'],
                        lastDate=(dt.date(1970,1,1)+dt.timedelta(days=r['lastServicedEpochDays'])).isoformat()) for r in rules]
    native_fuel, proto_fuel = [], []
    for idx, (odo, litres, taka, full, offset) in enumerate([
            (11890, 5.0, 600.0, True, -27), (12020, 2.0, 240.0, False, -20),
            (12120, 3.0, 360.0, True, -15), (12340, 4.9, 588.0, True, -8)]):
        rid = f'p00-fuel-{idx+1}'
        native_fuel.append(dict(id=rid,title='Fuel fill-up',detail=f'{litres} L',date=date(offset),
                                odometer=float(odo),kind='fuel',liters=litres,fullTank=full,
                                cost=taka,fuelType='octane',fuelPrice=120.0,locationLabel='P00 synthetic pump'))
        proto_fuel.append(dict(id=rid,type='fuel',date=date(offset),odo=odo,liters=litres,
                               amount=taka,price=120.0,full=full,fuel='octane',place='P00 synthetic pump'))
    native_docs, proto_docs = [], []
    for kind, title, expiry, suffix in [('tax','Tax token',date(-5),'tax'),
                                       ('licence','Driving licence',date(24),'licence'),
                                       ('registration','Registration smart card','','registration')]:
        rid = f'p00-doc-{suffix}'
        native_docs.append(dict(id=rid,title=title,number='P00-TEST-'+suffix.upper(),
                                holder='SYNTHETIC OWNER',expiry=expiry,kind=kind,
                                detailsConfirmed=True,reminder=False))
        proto_docs.append(dict(id=rid,kind=kind,number='P00-TEST-'+suffix.upper(),
                               holder='SYNTHETIC OWNER',expiry=expiry,reminder=False))
    settings = dict(theme='light',digits='bn',haptics=False,reduceMotion=False,textScale=1,
                    gps=False,brandOverride=None,serviceReminders=False,docReminders=False)
    output = []
    for scenario in SCENARIOS:
        use_rules = scenario in ('overdue','partial-fill','documents')
        use_fuel = scenario in ('partial-fill','documents')
        use_docs = scenario == 'documents'
        native = dict(ride=copy.deepcopy(native_ride),rules=copy.deepcopy(rules if use_rules else []),
                      logs=list(reversed(native_fuel)) if use_fuel else [],
                      documents=copy.deepcopy(native_docs if use_docs else []),appearance='LIGHT',
                      autoLocation=False,digits='BN',haptics=False,reduceMotion=False,textScale=1.0,
                      serviceReminders=False,docReminders=False,
                      lastPrices={'octane':120.0,'petrol':115.0})
        proto = dict(ride=copy.deepcopy(proto_ride),rules=copy.deepcopy(proto_rules if use_rules else []),
                     logs=list(reversed(proto_fuel)) if use_fuel else [],docs=copy.deepcopy(proto_docs if use_docs else []),
                     lastPrice={'octane':120.0,'petrol':115.0},lastItemPrice={},workshops=[],places=[],
                     settings=copy.deepcopy(settings))
        fixture = dict(fixtureVersion=1,id=scenario,referenceDate=reference,synthetic=True,
                       native=native,prototype=proto,
                       notes=['All IDs, names, numbers and prices are synthetic.',
                              'Document fixture is metadata-only; no owner images or documents.',
                              'Unknown service uses no rules: current models cannot encode baseline certainty.',
                              'Runtime clocks are observed, not overridden; date differences must be reported.'])
        write_json(FIXTURES/f'{scenario}.json', fixture)
        write_json(FIXTURES/'native'/f'{scenario}.json',native)
        output.append(fixture)
    write_json(ROOT/'design/app-ui-v3/public/p00-fixtures.json',output)
    write_json(FIXTURES/'manifest.json',dict(fixtureVersion=1,referenceDate=reference,synthetic=True,
                nativeAppId=APP,scenarios=list(SCENARIOS),files=[dict(path=str(p.relative_to(ROOT)),
                sha256=hashlib.sha256(p.read_bytes()).hexdigest()) for p in sorted(FIXTURES.rglob('*.json')) if p.name!='manifest.json']))
    print(f'Generated {len(output)} paired synthetic scenarios for {reference}.')


def android(serial: str, scenario: str) -> None:
    if not serial.startswith('emulator-'):
        raise ValueError('P00 fixture loading is restricted to an emulator serial')
    adb = lambda *args, **kw: run('adb','-s',serial,*args,**kw)
    # Never allow caller-supplied package IDs or production data directories.
    if f'package:{APP}' not in adb('shell','pm','list','packages',APP).splitlines():
        raise ValueError('Install the isolated P00 APK first')
    adb('shell','am','force-stop',APP)
    adb('shell','pm','clear',APP)
    source = FIXTURES/'native'/f'{scenario}.json'
    adb('push',str(source),'/data/local/tmp/motovault-p00.json')
    adb('shell','run-as',APP,'mkdir','-p','files')
    adb('shell','run-as',APP,'cp','/data/local/tmp/motovault-p00.json','files/motovault.json')
    print(adb('shell','am','start','-n',f'{APP}/com.motovault.app.MainActivity'))


def ios(device: str, scenario: str) -> None:
    app_path = Path(run('xcrun','simctl','get_app_container',device,APP,'app'))
    with (app_path/'Info.plist').open('rb') as handle:
        if plistlib.load(handle).get('CFBundleIdentifier') != APP:
            raise ValueError('Refusing a non-P00 iOS application')
    run('xcrun','simctl','terminate',device,APP,check=False)
    container = Path(run('xcrun','simctl','get_app_container',device,APP,'data'))
    if 'Containers/Data/Application' not in str(container) or not container.is_dir():
        raise ValueError('Unexpected simulator application container')
    support = container/'Library/Application Support'
    support.mkdir(parents=True,exist_ok=True)
    for name in ['motovault.db','motovault.db-wal','motovault.db-shm','motovault.json']:
        (support/name).unlink(missing_ok=True)
    shutil.copyfile(FIXTURES/'native'/f'{scenario}.json',support/'motovault.json')
    print(f'Prepared isolated iOS {scenario}; existing production app is untouched.')


def prepare_ios_tests() -> None:
    destination = ROOT/'build/p00-ios-host'
    shutil.copytree(ROOT/'iosApp',destination,dirs_exist_ok=True,
                    ignore=shutil.ignore_patterns('xcuserdata','build'))
    project = destination/'MotoVault.xcodeproj/project.pbxproj'
    source = project.read_text().replace('com.motovault.app.uitests',APP+'.uitests').replace('com.motovault.app',APP)
    source = source.replace('$(SRCROOT)/..',str(ROOT)).replace('$SRCROOT/..',str(ROOT))
    project.write_text(source)
    shutil.copyfile(ROOT/'tools/baseline/P00BaselineTests.swift',destination/'MotoVaultUITests/OwnerFlowTests.swift')
    print('Prepared temporary isolated Xcode test host; production project unchanged.')


def check() -> None:
    manifest = json.loads((FIXTURES/'manifest.json').read_text())
    for entry in manifest['files']:
        assert hashlib.sha256((ROOT/entry['path']).read_bytes()).hexdigest() == entry['sha256'], entry['path']
    collection=json.loads((ROOT/'design/app-ui-v3/public/p00-fixtures.json').read_text())
    assert [f['id'] for f in collection]==list(SCENARIOS)
    for fixture in collection:
        assert fixture == json.loads((FIXTURES/f"{fixture['id']}.json").read_text())
        n,p=fixture['native'],fixture['prototype']
        assert n['ride']['odometer']==p['ride']['odometer']
        assert [r['id'] for r in n['rules']]==[r['id'] for r in p['rules']]
        assert [r['id'] for r in n['logs']]==[r['id'] for r in p['logs']]
        assert [r['id'] for r in n['documents']]==[r['id'] for r in p['docs']]
        for nr,pr in zip(n['logs'],p['logs']):
            assert (nr['odometer'],nr['liters'],nr['cost'],nr['fullTank'],nr['date'])==(pr['odo'],pr['liters'],pr['amount'],pr['full'],pr['date'])
    assert sum(not r['fullTank'] for r in collection[2]['native']['logs'])==1
    assert collection[4]['native']['rules']==[]
    print('P00 fixture hashes, source/export equality and paired field parity passed.')


def main() -> None:
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command',choices=['generate','check','android','ios','prepare-ios-tests'])
    parser.add_argument('--date',default='2026-10-24')
    parser.add_argument('--device')
    parser.add_argument('--scenario',choices=SCENARIOS,default='overdue')
    args=parser.parse_args()
    if args.command=='generate':generate(args.date)
    elif args.command=='check':check()
    elif args.command=='prepare-ios-tests':prepare_ios_tests()
    elif not args.device:parser.error('--device is required for native fixture loading')
    elif args.command=='android':android(args.device,args.scenario)
    else:ios(args.device,args.scenario)


if __name__=='__main__':main()
