#!/usr/bin/env python3
"""Collect reproducible, synthetic-only P00 provenance and retained test reports."""
from pathlib import Path
import hashlib
import json
import plistlib
import shutil
import xml.etree.ElementTree as ET
import p00

OUT=p00.ROOT/'docs/roadmap/evidence/p00'


def main():
    (OUT/'tests').mkdir(exist_ok=True)
    for source,name in [
        ('composeApp/build/test-results/testDebugUnitTest/TEST-com.motovault.app.ui.P00CompatibilityTest.xml','android-compatibility.xml'),
        ('composeApp/build/test-results/iosSimulatorArm64Test/TEST-com.motovault.app.ui.P00CompatibilityTest.xml','ios-compatibility.xml')]:
        shutil.copyfile(p00.ROOT/source,OUT/'tests'/name)
    for source,name in [('android-build','android-build.log'),('ios-build','ios-build.log'),
                        ('compatibility-tests','compatibility-gradle.log'),('prototype-build','prototype-build.log')]:
        source_path=Path('/private/tmp/motovault-p00-'+source+'.log')
        if source_path.exists():
            lines=source_path.read_text(errors='replace').splitlines()
            (OUT/'tests'/name).write_text('\n'.join(lines[-70:])+'\n')
    for scenario in p00.SCENARIOS:
        source=p00.ROOT/f'build/p00-{scenario}.xcresult'
        summary=p00.run('xcrun','xcresulttool','get','test-results','summary','--path',str(source))
        p00.write_json(OUT/'ios'/f'{scenario}-summary.json',json.loads(summary))
    runs=[('p00-motion','harness-r1-failure')]+[(f'p00-motion-r{n}',f'motion-r{n}') for n in range(2,8)]
    for run,label in runs:
        summary=p00.run('xcrun','xcresulttool','get','test-results','summary','--path',str(p00.ROOT/f'build/{run}.xcresult'))
        p00.write_json(OUT/'ios'/f'{label}-summary.json',json.loads(summary))
    android={key:p00.run('adb','-s','emulator-5554','shell','getprop',key) for key in
             ['ro.product.model','ro.build.version.release','ro.build.version.sdk','ro.build.fingerprint']}
    android.update(serial='emulator-5554',avd='Pixel_10',applicationId=p00.APP,
                   display=p00.run('adb','-s','emulator-5554','shell','wm','size'),
                   clock=p00.run('adb','-s','emulator-5554','shell','date'))
    device='639D8717-57E7-49B8-9DAA-2C365E2F1508'
    listing=json.loads(p00.run('xcrun','simctl','list','devices','--json'))
    selected=next(dict(d,runtime=runtime) for runtime,items in listing['devices'].items() for d in items if d['udid']==device)
    # Retain identifiers/model/runtime, not unrelated simulator inventories or owner data paths.
    ios={key:selected[key] for key in ['udid','name','state','deviceTypeIdentifier','runtime']}
    ios['applicationId']=p00.APP
    app=Path(p00.run('xcrun','simctl','get_app_container',device,p00.APP,'app'))
    with (app/'Info.plist').open('rb') as handle:info=plistlib.load(handle)
    ios['bundleVersion']=info['CFBundleVersion'];ios['bundleIdentifier']=info['CFBundleIdentifier']
    ios['executableSha256']=hashlib.sha256((app/info['CFBundleExecutable']).read_bytes()).hexdigest()
    p00.write_json(OUT/'platforms.json',dict(baseCommit='50eda3a',nativeBehaviorSourceDelta='No main-source changes; isolated identities, fixture inputs and test harness only',
        android=android,ios=ios,prototype=dict(url='http://localhost:5273/',viewport=dict(width=440,height=956),
        motion='78 CUA screenshot samples with measured timestamps; not a continuous high-frame-rate recording')))
    initial=json.loads((p00.ROOT/'docs/roadmap/design-reference.json').read_text())
    changed=[];current=[]
    for entry in initial['files']:
        path=p00.ROOT/entry['path'];digest=hashlib.sha256(path.read_bytes()).hexdigest()
        current.append(dict(path=entry['path'],sha256=digest))
        if digest!=entry['sha256']:changed.append(entry['path'])
    p00.write_json(OUT/'design-source-manifest.json',dict(baseCommit='50eda3a',files=current,
        changedSincePlanning=changed,approvedDelta='DevPanel adds synthetic fixture selector outside the phone UI; native feature UI/styles/tokens remain unchanged'))
    for label in ['motion-r7']:
        directory=OUT/'ios'/label
        for item in json.loads((directory/'manifest.json').read_text()):
            for artifact in item['attachments']:
                if artifact['suggestedHumanReadableName'].startswith('undo-observation'):
                    shutil.copyfile(directory/artifact['exportedFileName'],OUT/'ios/undo-observation.txt')
    print('Collected native/prototype build provenance, 14 compatibility results, six successful capture summaries and the initial harness failure separately.')


if __name__=='__main__':main()
