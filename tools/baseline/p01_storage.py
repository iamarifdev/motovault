#!/usr/bin/env python3
"""Inspect or fault-inject only the hard-coded synthetic baseline database, with its app stopped."""
import argparse
import json
from pathlib import Path
import shlex
import shutil
import sqlite3
import subprocess
import tempfile
import p00
import p01_ios

SERIAL = 'emulator-5554'


def operate(platform, mode, device=None):
    assert p00.APP == 'com.motovault.baseline.p00'
    with tempfile.TemporaryDirectory(prefix='motovault-p01-') as folder:
        temporary = Path(folder)
        if platform == 'android':
            p00.run('adb', '-s', SERIAL, 'shell', 'am', 'force-stop', p00.APP)
            names = p00.run('adb', '-s', SERIAL, 'shell', 'run-as', p00.APP, 'ls', 'databases').splitlines()
            for name in ['motovault.db', 'motovault.db-wal', 'motovault.db-shm']:
                if name in names:
                    command = ['adb', '-s', SERIAL, 'exec-out', 'run-as', p00.APP, 'cat', 'databases/'+name]
                    with (temporary/name).open('wb') as stream:
                        subprocess.run(['rtk', 'run', '-c', shlex.join(command)], stdout=stream, check=True)
        else:
            p01_ios.validate_device(device)
            p00.run('xcrun', 'simctl', 'terminate', device, p00.APP, check=False)
            container = Path(p00.run('xcrun', 'simctl', 'get_app_container', device, p00.APP, 'data'))
            assert 'Containers/Data/Application' in str(container)
            support = container/'Library/Application Support'
            for name in ['motovault.db', 'motovault.db-wal', 'motovault.db-shm']:
                if (support/name).exists():
                    shutil.copyfile(support/name, temporary/name)
        db = sqlite3.connect(temporary/'motovault.db')
        ride = json.loads(db.execute("SELECT payload FROM vehicle WHERE id='primary'").fetchone()[0])
        assert ride.get('registration') == 'P00-SYNTHETIC', 'Refusing a database without the synthetic ride marker'
        if mode == 'fail':
            db.execute("CREATE TRIGGER IF NOT EXISTS p01_reject_meta BEFORE INSERT ON vault_meta BEGIN SELECT RAISE(ABORT, 'P01 synthetic disk failure'); END")
        if mode == 'recover':
            db.execute('DROP TRIGGER IF EXISTS p01_reject_meta')
        db.commit()
        result = dict(platform=platform, app=p00.APP, synthetic=True,
            odometer=ride['odometer'], odometerConfirmedDate=ride.get('odometerConfirmedDate'),
            preferences=json.loads(db.execute('SELECT preferences FROM vault_meta WHERE id=1').fetchone()[0]),
            documents=[dict(id=r['id'], expiry=r['expiry'], snooze=r.get('reminderSnoozedUntil'))
                for (raw,) in db.execute('SELECT payload FROM document') for r in [json.loads(raw)]],
            rules=[dict(id=r['id'], odometer=r['lastServicedOdometerKm'], day=r['lastServicedEpochDays'],
                        certainty=r.get('baselineCertainty'), snooze=r.get('reminderSnoozedUntil'))
                   for (raw,) in db.execute('SELECT payload FROM schedule') for r in [json.loads(raw)]],
            logCount=db.execute('SELECT COUNT(*) FROM ledger').fetchone()[0])
        db.execute('PRAGMA wal_checkpoint(TRUNCATE)')
        db.close()
        if mode != 'read':
            if platform == 'android':
                p00.run('adb', '-s', SERIAL, 'push', str(temporary/'motovault.db'), '/data/local/tmp/p01-motovault.db')
                p00.run('adb', '-s', SERIAL, 'shell', 'run-as', p00.APP, 'cp', '/data/local/tmp/p01-motovault.db', 'databases/motovault.db')
                p00.run('adb', '-s', SERIAL, 'shell', 'run-as', p00.APP, 'rm', '-f', 'databases/motovault.db-wal', 'databases/motovault.db-shm')
            else:
                shutil.copyfile(temporary/'motovault.db', support/'motovault.db')
                for name in ['motovault.db-wal', 'motovault.db-shm']:
                    (support/name).unlink(missing_ok=True)
        return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('platform', choices=['android', 'ios'])
    parser.add_argument('mode', choices=['read', 'fail', 'recover'])
    parser.add_argument('--device', help='Verified running iOS 27 iPhone 18 Pro/Pro Max UUID; required for iOS')
    args = parser.parse_args()
    print(json.dumps(operate(args.platform, args.mode, args.device), ensure_ascii=False, indent=2))
