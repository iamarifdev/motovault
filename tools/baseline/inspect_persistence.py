#!/usr/bin/env python3
"""Inspect only the isolated baseline vault after the recorded Undo attempts."""
from pathlib import Path
import json
import shlex
import shutil
import sqlite3
import subprocess
import tempfile
import p00


def inspect(path):
    db=sqlite3.connect(path)
    ride=json.loads(db.execute("SELECT payload FROM vehicle WHERE id='primary'").fetchone()[0])
    fields=[r[1] for r in db.execute('PRAGMA table_info(vault_meta)')]
    pending=db.execute('SELECT payload FROM sync_outbox WHERE id=1').fetchone()
    queued=json.loads(pending[0]) if pending else {}
    db.close()
    return dict(expectedAfterUndo=12340,actualDurableOdometer=ride['odometer'],
                undoRestored=ride['odometer']==12340,vaultMetaColumns=fields,
                outboxPayloadFields=list(queued),outboxReduceMotion=queued.get('reduceMotion'),
                scope='Synthetic baseline only; Room read mapper and durable state inspected, no production owner data')


def main():
    output={}
    with tempfile.TemporaryDirectory(prefix='motovault-p00-persistence-') as temporary:
        temp=Path(temporary)
        p00.run('adb','-s','emulator-5554','shell','am','force-stop',p00.APP)
        names=p00.run('adb','-s','emulator-5554','shell','run-as',p00.APP,'ls','databases').splitlines()
        for name in ['motovault.db','motovault.db-wal','motovault.db-shm']:
            if name not in names:continue
            target=temp/name
            args=['adb','-s','emulator-5554','exec-out','run-as',p00.APP,'cat','databases/'+name]
            with target.open('wb') as stream:
                subprocess.run(['rtk','run','-c',shlex.join(args)],stdout=stream,check=True)
        output['android']=inspect(temp/'motovault.db')
    with tempfile.TemporaryDirectory(prefix='motovault-p00-ios-persistence-') as temporary:
        device='639D8717-57E7-49B8-9DAA-2C365E2F1508'
        p00.run('xcrun','simctl','terminate',device,p00.APP,check=False)
        container=Path(p00.run('xcrun','simctl','get_app_container',device,p00.APP,'data'))
        if 'Containers/Data/Application' not in str(container):raise ValueError('Unexpected isolated app path')
        temp=Path(temporary)
        for name in ['motovault.db','motovault.db-wal','motovault.db-shm']:
            source=container/'Library/Application Support'/name
            if source.exists():shutil.copyfile(source,temp/name)
        output['ios']=inspect(temp/'motovault.db')
    p00.write_json(p00.ROOT/'docs/roadmap/evidence/p00/persistence-observations.json',output)
    print(json.dumps(output,ensure_ascii=False,indent=2))


if __name__=='__main__':main()
