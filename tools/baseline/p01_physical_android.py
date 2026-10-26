#!/usr/bin/env python3
"""Explicit physical Android acceptance, restricted to a marked synthetic package."""
import argparse
import copy
from datetime import datetime, timedelta
import hashlib
import json
from pathlib import Path
import re
import shlex
import subprocess
import sqlite3
import tempfile
import time
import xml.etree.ElementTree as ET

import p00

APP = 'com.motovault.baseline.p00'
MARKER = 'p01-physical-synthetic-only'
APK = p00.ROOT / 'composeApp/build/outputs/apk/debug/composeApp-debug.apk'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--run', required=True)
    parser.add_argument('mode', choices=['prepare', 'inspect', 'tap', 'back', 'scroll', 'scroll-top', 'home',
                                         'open', 'state', 'observe', 'shade', 'open-alert', 'expand-alerts', 'background-kill', 'reboot', 'alarms', 'snapshot', 'restart', 'notification-settings', 'record-start', 'record-stop'])
    parser.add_argument('--label')
    parser.add_argument('--minutes', type=int, default=5)
    parser.add_argument('--seconds', type=int, default=45)
    args = parser.parse_args()
    assert not args.serial.startswith('emulator-'), 'Use the separate emulator runners for emulators'
    assert args.run.replace('-', '').isalnum()
    out = p00.ROOT / 'docs/roadmap/evidence/p01/android-device' / args.run

    def adb(*parts, check=True):
        return p00.run('adb', '-s', args.serial, *parts, check=check)

    assert adb('get-state') == 'device', 'The explicitly selected phone must be authorized and connected'
    assert adb('shell', 'getprop', 'ro.kernel.qemu') != '1'

    def marked():
        assert adb('shell', 'run-as', APP, 'cat', 'files/' + MARKER) == MARKER, 'Refusing unmarked package data'

    def raw_tree():
        remote = '/sdcard/motovault-p01-physical-' + str(time.monotonic_ns()) + '.xml'
        try:
            adb('shell', 'uiautomator', 'dump', remote)
            return ET.fromstring(adb('exec-out', 'cat', remote))
        finally:
            adb('shell', 'rm', '-f', remote, check=False)

    def labels(root):
        return [n.get('text') or n.get('content-desc') for n in root.iter('node')
                if n.get('text') or n.get('content-desc')]

    def capture(name):
        root = raw_tree()
        packages = {n.get('package') for n in root.iter('node')}
        allowed = {APP, 'com.android.settings', 'com.google.android.permissioncontroller',
                   'com.android.permissioncontroller'}
        assert packages & allowed, 'Expected isolated app, its permission prompt or Settings; unlock manually if needed'
        assert not any(n.get('resource-id', '').endswith('notification_stack_scroller') for n in root.iter('node')), 'Do not retain personal notification shade'
        filtered = copy.deepcopy(root)
        for node in filtered.iter('node'):
            if node.get('package') not in allowed:
                node.attrib.pop('text', None)
                node.attrib.pop('content-desc', None)
        stamp = name + '-' + str(time.monotonic_ns())
        (out / (stamp + '.xml')).write_text(ET.tostring(filtered, encoding='unicode'))
        if APP in packages:
            command = ['adb', '-s', args.serial, 'exec-out', 'screencap', '-p']
            with (out / (stamp + '.png')).open('wb') as handle:
                subprocess.run(['rtk', 'run', '-c', shlex.join(command)], stdout=handle, check=True)
        shown = []
        for n in filtered.iter('node'):
            text = n.get('text') or n.get('content-desc')
            if text:
                shown.append(dict(label=text, bounds=n.get('bounds'), clickable=n.get('clickable'),
                                  resource=n.get('resource-id'), checked=n.get('checked')))
        print(json.dumps(dict(capture=stamp, labels=[x['label'] for x in shown]), ensure_ascii=False), flush=True)
        return root

    def tap(root, label):
        parents = {child: parent for parent in root.iter() for child in parent}
        matches = [n for n in root.iter('node') if label in [n.get('text'), n.get('content-desc')]]
        assert matches, 'No exact observed label: ' + str(label)
        node = matches[-1]
        while node.get('clickable') != 'true' and node in parents:
            node = parents[node]
        assert node.get('clickable') == 'true', 'No clickable observed ancestor'
        x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
        assert x2 > x1 and y2 > y1
        adb('shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))

    def state():
        value = adb('shell', 'run-as', APP, 'cat', 'shared_prefs/renewal_reminders.xml', check=False)
        result = dict(enabled=False, plans=[], consumed=[])
        if value.lstrip().startswith('<?xml'):
            pref = ET.fromstring(value)
            for n in pref:
                key = n.get('name')
                if key == 'enabled':
                    result[key] = n.get('value') == 'true'
                elif key == 'plans':
                    result[key] = json.loads(n.text or '[]')
                elif key == 'consumed':
                    result[key] = [x.text for x in n]
        result['notificationPermission'] = [line.strip() for line in adb('shell', 'dumpsys', 'package', APP).splitlines()
                                            if 'android.permission.POST_NOTIFICATIONS:' in line]
        p00.write_json(out / ('state-' + str(time.monotonic_ns()) + '.json'), result)
        print(json.dumps(result, ensure_ascii=False), flush=True)
        return result

    if args.mode == 'prepare':
        assert 2 <= args.minutes <= 30
        out.mkdir(parents=True, exist_ok=False)
        present = adb('shell', 'pm', 'list', 'packages', APP).splitlines()
        if 'package:' + APP in present:
            marked()
        badging = p00.run(str(Path.home() / 'Library/Android/sdk/build-tools/35.0.0/aapt2'), 'dump', 'badging', str(APK))
        assert "package: name='" + APP + "'" in badging, 'Refusing a production APK'
        before_owner = adb('shell', 'pm', 'path', 'com.motovault.app', check=False)
        adb('install', '-r', str(APK))
        assert adb('shell', 'pm', 'path', 'com.motovault.app', check=False) == before_owner
        if 'package:' + APP in present:
            assert adb('shell', 'pm', 'clear', APP) == 'Success'
        adb('shell', 'run-as', APP, 'mkdir', '-p', 'files')
        marker = out / 'synthetic-marker.txt'
        marker.write_text(MARKER)
        adb('push', str(marker), '/data/local/tmp/motovault-p01-physical-marker')
        adb('shell', 'run-as', APP, 'cp', '/data/local/tmp/motovault-p01-physical-marker', 'files/' + MARKER)
        clock = datetime.fromisoformat(adb('shell', 'date', '+%Y-%m-%dT%H:%M:%S'))
        due = (clock + timedelta(minutes=args.minutes)).replace(second=0, microsecond=0)
        value = json.loads((p00.ROOT / 'docs/roadmap/fixtures/p00/native/documents.json').read_text())
        assert value['ride']['registration'] == 'P00-SYNTHETIC'
        value.update(_synthetic=True, serviceReminders=True, docReminders=True,
                     reminderPolicy=dict(warningDays=30, minuteOfDay=due.hour * 60 + due.minute))
        value['ride']['odometerConfirmedDate'] = clock.date().isoformat()
        value['rules'][0]['baselineCertainty'] = 'CONFIRMED'
        value['rules'][1]['baselineCertainty'] = 'UNKNOWN'
        value['documents'].append(dict(id='p01-physical-today', kind='insurance', title='P01 physical synthetic expiry',
            number='P01-PHYSICAL-TEST', holder='SYNTHETIC OWNER', expiry=due.date().isoformat(), reminder=True))
        p00.write_json(out / 'fixture.json', value)
        adb('push', str(out / 'fixture.json'), '/data/local/tmp/motovault-p01-physical.json')
        adb('shell', 'run-as', APP, 'cp', '/data/local/tmp/motovault-p01-physical.json', 'files/motovault.json')
        for remote in ['/data/local/tmp/motovault-p01-physical.json', '/data/local/tmp/motovault-p01-physical-marker']:
            adb('shell', 'rm', '-f', remote)
        properties = {key: adb('shell', 'getprop', name) for key, name in [
            ('model', 'ro.product.model'), ('android', 'ro.build.version.release'),
            ('api', 'ro.build.version.sdk'), ('abi', 'ro.product.cpu.abi'), ('timezone', 'persist.sys.timezone')]}
        installed = adb('shell', 'pm', 'path', APP).removeprefix('package:')
        digest = hashlib.sha256(APK.read_bytes()).hexdigest()
        assert adb('shell', 'sha256sum', installed).split()[0] == digest
        p00.write_json(out / 'metadata.json', dict(physical=True, synthetic=True, applicationId=APP,
            device=properties, baseRevision=p00.run('git', 'rev-parse', 'HEAD'), uncommitted=True,
            apkSha256=digest, fixtureSha256=hashlib.sha256((out / 'fixture.json').read_bytes()).hexdigest(),
            deviceClock=clock.isoformat(), policyDue=due.isoformat(), ownerPackagePathUnchanged=True,
            scope='Physical Android synthetic-only acceptance. Actual policy minute is customized in committed fixture; no injected delivery broadcast.'))
        adb('shell', 'am', 'start', '-n', APP + '/com.motovault.app.MainActivity')
        print('Prepared physical synthetic app; policy due ' + due.isoformat(), flush=True)
        return

    assert out.is_dir()
    marked()
    if args.mode == 'record-start':
        assert not adb('shell', 'pidof', 'screenrecord', check=False).strip(), 'Do not interrupt another recording'
        assert APP in {n.get('package') for n in raw_tree().iter('node')}, 'Record only the isolated app'
        name = 'motion-' + str(time.monotonic_ns())
        remote = '/sdcard/motovault-p01-' + name + '.mp4'
        command = ['adb', '-s', args.serial, 'shell', 'screenrecord', '--size', '540x1200',
                   '--bit-rate', '4000000', '--time-limit', '60', remote]
        with (out / (name + '-encoder.log')).open('w') as log:
            subprocess.Popen(['rtk', 'run', '-c', shlex.join(command)], stdout=log, stderr=subprocess.STDOUT,
                             start_new_session=True)
        time.sleep(0.5)
        pid = adb('shell', 'pidof', 'screenrecord').strip()
        assert pid.isdigit()
        p00.write_json(out / 'recording-live.json', dict(pid=pid, remote=remote, name=name,
            deviceClock=adb('shell', 'date', '+%Y-%m-%dT%H:%M:%S')))
        print('Started bounded isolated-app recording ' + name, flush=True)
        return
    if args.mode == 'record-stop':
        recording = json.loads((out / 'recording-live.json').read_text())
        assert recording['remote'].startswith('/sdcard/motovault-p01-motion-') and recording['pid'].isdigit()
        current = adb('shell', 'pidof', 'screenrecord', check=False).strip()
        if current:
            assert current == recording['pid'], 'Refusing to stop another recording'
            adb('shell', 'kill', '-2', current)
            time.sleep(0.5)
        adb('pull', recording['remote'], str(out / (recording['name'] + '.mp4')))
        adb('shell', 'rm', '-f', recording['remote'])
        (out / 'recording-live.json').unlink()
        print('Retained physical recording ' + recording['name'], flush=True)
        return
    if args.mode == 'snapshot':
        names = adb('shell', 'run-as', APP, 'ls', 'databases').splitlines()
        def read_files():
            result = {}
            for name in ['motovault.db', 'motovault.db-wal']:
                if name in names:
                    command = ['adb', '-s', args.serial, 'exec-out', 'run-as', APP, 'cat', 'databases/' + name]
                    result[name] = subprocess.run(['rtk', 'run', '-c', shlex.join(command)], capture_output=True, check=True).stdout
            return result
        first = read_files()
        second = read_files()
        assert first == second, 'Database changed during capture; retry after durable save settles'
        with tempfile.TemporaryDirectory(prefix='motovault-physical-snapshot-') as folder:
            for name, data in first.items():
                (Path(folder) / name).write_bytes(data)
            db = sqlite3.connect(str(Path(folder) / 'motovault.db'))
            ride = json.loads(db.execute("SELECT payload FROM vehicle WHERE id='primary'").fetchone()[0])
            assert ride['registration'] == 'P00-SYNTHETIC'
            result = dict(synthetic=True, applicationId=APP, ride=ride,
                preferences=json.loads(db.execute('SELECT preferences FROM vault_meta WHERE id=1').fetchone()[0]),
                documents=sorted([json.loads(r[0]) for r in db.execute('SELECT payload FROM document')], key=lambda x: x['id']),
                rules=sorted([json.loads(r[0]) for r in db.execute('SELECT payload FROM schedule')], key=lambda x: x['id']),
                logCount=db.execute('SELECT COUNT(*) FROM ledger').fetchone()[0])
            db.close()
        path = out / ('snapshot-' + str(time.monotonic_ns()) + '.json')
        p00.write_json(path, result)
        print(json.dumps(dict(snapshot=str(path.relative_to(p00.ROOT)), documentCount=len(result['documents']),
                             ruleCount=len(result['rules']), logCount=result['logCount'])), flush=True)
        return
    if args.mode == 'alarms':
        lines = adb('shell', 'dumpsys', 'alarm').splitlines()
        blocks = []
        for i, line in enumerate(lines):
            if re.search(r'RTC_WAKEUP #\d+:', line) and APP in line:
                block = [line.strip()]
                for following in lines[i + 1:]:
                    if re.search(r'^\s*(RTC|ELAPSED).*#\d+:', following) or not following.strip():
                        break
                    block.append(following.strip())
                blocks.append(block)
        result = dict(deviceClock=adb('shell', 'date', '+%Y-%m-%dT%H:%M:%S'),
            bootCount=adb('shell', 'settings', 'get', 'global', 'boot_count'),
            bootCompleted=adb('shell', 'getprop', 'sys.boot_completed'),
            processRunning=bool(adb('shell', 'pidof', APP, check=False).strip()),
            standbyBucket=adb('shell', 'am', 'get-standby-bucket', APP), alarms=blocks,
            timebase=[line.strip() for line in lines if 'nowRTC=' in line or 'mLastTimeChangeClockTime=' in line])
        p00.write_json(out / ('alarms-' + str(time.monotonic_ns()) + '.json'), result)
        print(json.dumps(result), flush=True)
        return
    if args.mode == 'reboot':
        pending = state()
        assert pending['enabled'] and pending['plans'], 'Reboot acceptance needs a live native queue'
        p00.write_json(out / 'reboot-before.json', dict(userAuthorized=True,
            deviceClock=adb('shell', 'date', '+%Y-%m-%dT%H:%M:%S'),
            bootCount=adb('shell', 'settings', 'get', 'global', 'boot_count'),
            pending=pending, scope='Owner explicitly approved this physical phone reboot in chat.'))
        adb('reboot')
        print('Authorized physical Android reboot requested', flush=True)
        return
    if args.mode == 'restart':
        before = state()
        adb('shell', 'am', 'force-stop', APP)
        adb('shell', 'am', 'start', '-n', APP + '/com.motovault.app.MainActivity')
        time.sleep(1)
        after = state()
        assert before['enabled'] == after['enabled']
        assert {p['key'] for p in before['plans']} == {p['key'] for p in after['plans']}
        p00.write_json(out / ('restart-' + str(time.monotonic_ns()) + '.json'), dict(result='Passed',
            before=before, after=after, scope='Physical isolated app force-stop/relaunch preserves native opt-in and pending snooze plans.'))
    elif args.mode == 'notification-settings':
        adb('shell', 'am', 'start', '-a', 'android.settings.APP_NOTIFICATION_SETTINGS',
            '--es', 'android.provider.extra.APP_PACKAGE', APP)
    elif args.mode == 'open':
        adb('shell', 'am', 'start', '-n', APP + '/com.motovault.app.MainActivity')
    elif args.mode == 'back':
        adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')
    elif args.mode == 'home':
        adb('shell', 'input', 'keyevent', 'KEYCODE_HOME')
        return
    elif args.mode == 'background-kill':
        adb('shell', 'input', 'keyevent', 'KEYCODE_HOME')
        time.sleep(1)
        before = adb('shell', 'pidof', APP, check=False).strip()
        adb('shell', 'am', 'kill', APP)
        time.sleep(1)
        after = adb('shell', 'pidof', APP, check=False).strip()
        result = dict(processWasRunning=bool(before), processStillRunning=bool(after),
            deviceClock=adb('shell', 'date', '+%Y-%m-%dT%H:%M:%S'),
            scope='Background cached-process kill only. No force-stop or device reboot; native alarms are not deliberately cancelled.')
        p00.write_json(out / ('background-process-' + str(time.monotonic_ns()) + '.json'), result)
        print(json.dumps(result), flush=True)
        return
    elif args.mode in {'scroll', 'scroll-top'}:
        w, h = map(int, adb('shell', 'wm', 'size').split()[-1].split('x'))
        start, end = (.76, .32) if args.mode == 'scroll' else (.32, .76)
        adb('shell', 'input', 'swipe', str(w // 2), str(int(h * start)), str(w // 2), str(int(h * end)), '350')
    elif args.mode == 'tap':
        assert args.label
        for attempt in range(3):
            observed = raw_tree()
            if args.label in labels(observed):
                break
            time.sleep(0.3)
        else:
            capture('tap-target-missing')
            raise AssertionError('Expected control did not remain visible: ' + args.label)
        tap(observed, args.label)
    elif args.mode == 'state':
        state()
        return
    elif args.mode == 'observe':
        assert 1 <= args.seconds <= 55
        start = time.monotonic()
        active = []
        while time.monotonic() - start < args.seconds:
            dump = adb('shell', 'dumpsys', 'notification', '--noredact')
            active = [line.strip() for line in dump.splitlines() if 'NotificationRecord(' in line and ('pkg=' + APP + ' ') in line]
            if any('p01-physical-today' in x for x in active) and any('service:' in x for x in active):
                break
            time.sleep(3)
        observation = dict(deviceClock=adb('shell', 'date', '+%Y-%m-%dT%H:%M:%S'), active=active,
                           elapsedSeconds=round(time.monotonic() - start, 2),
                           appResumed=any(APP in line for line in adb('shell', 'dumpsys', 'activity', 'activities').splitlines()
                                          if 'mResumedActivity:' in line or 'topResumedActivity=' in line))
        p00.write_json(out / ('delivery-' + str(time.monotonic_ns()) + '.json'), observation)
        print(json.dumps(observation), flush=True)
        return
    elif args.mode in {'shade', 'open-alert', 'expand-alerts'}:
        adb('shell', 'cmd', 'statusbar', 'expand-notifications')
        root = raw_tree()
        wanted = args.label or 'P01 physical synthetic expiry'
        matches = [n for n in root.iter('node') if wanted in (n.get('text', '') + ' ' + n.get('content-desc', ''))]
        if args.mode == 'expand-alerts':
            assert matches
            parents = {child: parent for parent in root.iter() for child in parent}
            group = matches[-1]
            while group.get('resource-id') != 'com.android.systemui:id/notification_children_container':
                assert group in parents, 'No observed synthetic notification group'
                group = parents[group]
            expand = next(n for n in group.iter('node') if n.get('resource-id') == 'android:id/expand_button')
            assert expand.get('content-desc') == 'Expand'
            x1, y1, x2, y2 = map(int, re.findall(r'\d+', expand.get('bounds')))
            adb('shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))
            root = raw_tree()
            matches = [n for n in root.iter('node') if wanted in (n.get('text', '') + ' ' + n.get('content-desc', ''))]
        # Raw shade trees and screenshots are never retained on an owner phone.
        if args.mode == 'open-alert':
            assert matches, 'Expected synthetic notification is not visible'
            selected = matches[-1]
            parents = {child: parent for parent in root.iter() for child in parent}
            row = selected
            while row.get('resource-id') != 'com.android.systemui:id/expandableNotificationRow':
                assert row in parents
                row = parents[row]
            assert row.get('clickable') == 'true', 'Expand the group before tapping an individual notification'
            tap(root, selected.get('text') or selected.get('content-desc'))
        else:
            if not matches:
                matches = [n for n in root.iter('node') if any(token in (n.get('text', '') + ' ' + n.get('content-desc', ''))
                           for token in ['MotoVault', 'P01', 'Chain'])]
            print(json.dumps([dict(label=n.get('text') or n.get('content-desc'), bounds=n.get('bounds'), clickable=n.get('clickable'))
                              for n in matches], ensure_ascii=False))
            parents = {child: parent for parent in root.iter() for child in parent}
            for node in matches:
                chain = []
                for _ in range(5):
                    if node not in parents:
                        break
                    node = parents[node]
                    chain.append(dict(resource=node.get('resource-id'), kind=node.get('class'),
                        bounds=node.get('bounds'), clickable=node.get('clickable'),
                        expand=[dict(resource=x.get('resource-id'), label=x.get('content-desc'), bounds=x.get('bounds'))
                            for x in node.iter('node') if 'expand' in x.get('resource-id', '').lower()]))
                print(json.dumps(dict(ancestors=chain), ensure_ascii=False))
            return
    opened = capture(args.mode)
    if args.mode == 'open-alert':
        expected = 'সময়সূচি: Chain' if args.label == 'Chain' else 'P01-PHYSICAL-TEST'
        assert any(expected in text for text in labels(opened)), 'Notification tap did not reach the exact expected record'
        p00.write_json(out / ('target-' + str(time.monotonic_ns()) + '.json'), dict(result='Passed',
            physical=True, applicationId=APP, expected=expected,
            scope='Actual visible notification tap; no injected navigation intent.'))


if __name__ == '__main__':
    main()
