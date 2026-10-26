#!/usr/bin/env python3
"""Validate retained physical evidence without promoting partial results to a pass."""
import argparse
import copy
from datetime import datetime, timedelta
import hashlib
import json
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
APP = 'com.motovault.baseline.p00'


def evaluate(run):
    assert run.replace('-', '').isalnum()
    folder = ROOT / 'docs/roadmap/evidence/p01/android-device' / run
    read = lambda path: json.loads(path.read_text())
    metadata = read(folder / 'metadata.json')
    fixture_path = folder / 'fixture.json'
    fixture = read(fixture_path)
    assert metadata['physical'] and metadata['synthetic'] and metadata['applicationId'] == APP
    assert metadata['ownerPackagePathUnchanged']
    assert hashlib.sha256(fixture_path.read_bytes()).hexdigest() == metadata['fixtureSha256']
    assert fixture['_synthetic'] and fixture['ride']['registration'] == 'P00-SYNTHETIC'
    expected = {'p01-physical-today', 'p00-chain'}
    states = [read(p) for p in sorted(folder.glob('state-*.json'))]
    assert any(not s['enabled'] and not s['plans'] and not s['consumed'] for s in states)
    scheduled = next(s for s in states if s['enabled'] and {p['documentId'] for p in s['plans']} == expected)
    assert len(scheduled['plans']) == 2
    assert all(p['minuteOfDay'] == fixture['reminderPolicy']['minuteOfDay'] for p in scheduled['plans'])
    keys = {p['key'] for p in scheduled['plans']}
    assert any(keys <= set(s['consumed']) and not s['plans'] for s in states)
    trees = [ET.parse(p).getroot() for p in folder.glob('*.xml')]
    labels = [[n.get('text') or n.get('content-desc') or '' for n in t.iter('node')] for t in trees]
    assert any(any('Allow MotoVault to send you notifications?' in text for text in row) for row in labels)
    assert any('Don’t allow' in row for row in labels)
    assert any('ফোনের সেটিংসে নোটিফিকেশনের অনুমতি নেই' in row for row in labels)
    assert any('All MotoVault notifications' in row for row in labels)
    assert any('ফোনে জানানো বন্ধ আছে' in row for row in labels)
    before = read(folder / 'reboot-before.json')
    assert before['userAuthorized'] and {p['key'] for p in before['pending']['plans']} == keys
    alarms = [read(p) for p in sorted(folder.glob('alarms-*.json'))]
    deferred = next(a for a in alarms if a['standbyBucket'] == '50' and not a['processRunning'])
    assert int(deferred['bootCount']) == int(before['bootCount']) + 1
    assert len(deferred['alarms']) == 2
    assert all(any('app_standby=+364d' in line for line in block) for block in deferred['alarms'])
    assert any(a['standbyBucket'] == '10' and a['processRunning'] for a in alarms)
    deliveries = [read(p) for p in folder.glob('delivery-*.json')]
    assert any(all(any(('tag=' + key) in header for header in d['active']) for key in keys) for d in deliveries)
    return dict(result='Partial', physical=True, synthetic=True, applicationId=APP,
        device=metadata['device'], apkSha256=metadata['apkSha256'], policyDue=metadata['policyDue'],
        verified=['Real notification prompt and denied state', 'Native Settings return with explicit app opt-in boundary',
                  'Two committed document/service plans', 'Authorized reboot restores native alarms',
                  'Foreground recovery delivers both actual-policy alerts and consumes their keys'],
        contradicted=['Uninterrupted post-boot background delivery: OS app_standby deferred both alarms until app reopen'],
        pending=['Actual document and service notification tap destinations and delivered-alert retention',
                 'Ordinary background delivery without reboot', 'Snooze and permission revocation recovery',
                 'Complete physical motion, spoken TalkBack and haptic acceptance', 'Physical iPhone verification'],
        scope='Retained evidence consistency only. Partial physical acceptance remains partial.')


def evaluate_core(run):
    assert run.replace('-', '').isalnum()
    folder = ROOT / 'docs/roadmap/evidence/p01/android-device' / run
    read = lambda p: json.loads(p.read_text())
    metadata = read(folder / 'metadata.json')
    assert metadata['physical'] and metadata['synthetic'] and metadata['applicationId'] == APP
    assert hashlib.sha256((folder / 'fixture.json').read_bytes()).hexdigest() == metadata['fixtureSha256']
    states = [read(p) for p in sorted(folder.glob('state-*.json'))]
    initial = next(s for s in states if len(s['plans']) == 2 and not s['consumed'])
    keys = {p['key'] for p in initial['plans']}
    assert {p['documentId'] for p in initial['plans']} == {'p01-physical-today', 'p00-chain'}
    killed = [read(p) for p in folder.glob('background-process-*.json')]
    assert any(p['processWasRunning'] and not p['processStillRunning'] for p in killed)
    deliveries = [read(p) for p in sorted(folder.glob('delivery-*.json'))]
    assert any(d.get('appResumed') is False and all(any('tag=' + key in h for h in d['active']) for key in keys) for d in deliveries)
    targets = [read(p) for p in folder.glob('target-*.json')]
    assert {t['expected'] for t in targets if t['result'] == 'Passed'} == {'P01-PHYSICAL-TEST', 'সময়সূচি: Chain'}
    service = next(key for key in keys if key.startswith('service:'))
    document = next(key for key in keys if key.startswith('renewal:'))
    assert any(any('tag=' + service in h for h in d['active']) and
               not any('tag=' + document in h for h in d['active']) for d in deliveries)
    snapshots = [read(p) for p in sorted(folder.glob('snapshot-*.json'))]
    original = snapshots[0]
    def without_snooze(value):
        result = copy.deepcopy(value)
        for kind in ['documents', 'rules']:
            for record in result[kind]:
                record.pop('reminderSnoozedUntil', None)
        return result
    core_snapshots = [s for s in snapshots if s['preferences'] == original['preferences']]
    assert len(core_snapshots) >= 5
    assert all(without_snooze(s) == without_snooze(original) for s in core_snapshots)
    snoozed = core_snapshots[2]
    due_date = datetime.fromisoformat(metadata['policyDue']).date()
    assert next(d for d in snoozed['documents'] if d['id'] == 'p01-physical-today')['reminderSnoozedUntil'] == str(due_date + timedelta(days=1)) + 'T09:00'
    assert next(r for r in snoozed['rules'] if r['id'] == 'p00-chain')['reminderSnoozedUntil'] == str(due_date + timedelta(days=7)) + 'T09:00'
    assert core_snapshots[2] == core_snapshots[3] == core_snapshots[4]
    restart = next(read(p) for p in folder.glob('restart-*.json'))
    assert restart['result'] == 'Passed' and restart['before'] == restart['after']
    assert any(s['enabled'] and not s['plans'] and all('granted=false' in p for p in s['notificationPermission']) for s in states)
    assert any(len(s['plans']) == 2 and all(':snooze:' in p['key'] for p in s['plans']) and
               any('granted=true' in p for p in s['notificationPermission']) for s in states)
    assert not states[-1]['enabled'] and not states[-1]['plans']
    last_alarm = read(sorted(folder.glob('alarms-*.json'))[-1])
    assert not last_alarm['alarms']
    awake = read(folder.parent / 'moto-background-r1/awake-setting.json')
    assert awake['restored'] and awake['originalStayOnWhilePluggedIn'] == awake['restoredStayOnWhilePluggedIn'] == '0'
    return dict(result='Passed', phaseComplete=False, physical=True, synthetic=True, applicationId=APP,
        device=metadata['device'], apkSha256=metadata['apkSha256'], policyDue=metadata['policyDue'],
        verified=['Ordinary background actual-policy delivery after cached-process death',
                  'Individual expiry and service notification taps reach exact records',
                  'Service alert remains after document alert opens',
                  'Document/service snooze preserves expiry, baseline, mileage and history',
                  'App restart restores both snooze plans', 'Permission revocation empties queue and native Settings recovery restores it',
                  'Test opt-out cancels pending alarms; temporary awake setting restored'],
        pending=['Post-boot uninterrupted delivery remains negative in r1', 'Full physical motion/parity acceptance',
                 'Spoken TalkBack and physical haptic confirmation', 'Physical iPhone verification'],
        scope='Passed only for the listed physical Android core scenarios while charging. No idle/Doze, reboot reliability, full design or iPhone completion claim.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', required=True)
    parser.add_argument('--core', action='store_true')
    args = parser.parse_args()
    result = evaluate_core(args.run) if args.core else evaluate(args.run)
    target = ROOT / 'docs/roadmap/evidence/p01/android-device' / args.run / ('core-result.json' if args.core else 'partial-result.json')
    target.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(result, ensure_ascii=False, indent=2))
