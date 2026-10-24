#!/usr/bin/env python3
"""Capture the five paired P00 scenarios using the authorized Android developer tools."""
import json
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

import p00

SERIAL='emulator-5554'
OUT=p00.ROOT/'docs/roadmap/evidence/p00/android'


def adb(*args, check=True):
    return p00.run('adb','-s',SERIAL,*args,check=check)


def tree(name):
    remote=f'/sdcard/motovault-capture-{time.monotonic_ns()}.xml'
    result=adb('shell','uiautomator','dump',remote)
    if 'dumped to:' not in result:
        raise RuntimeError(f'Android did not produce a fresh hierarchy: {result}')
    try:
        content=adb('exec-out','cat',remote)
    finally:
        adb('shell','rm','-f',remote,check=False)
    (OUT/f'{name}.xml').write_text(content)
    return ET.fromstring(content)


def tap_label(root, label):
    matches=[n for n in root.iter('node') if label in (n.get('text','')+' '+n.get('content-desc',''))]
    if not matches:raise RuntimeError(f'Missing observed Android control: {label}')
    exact=[n for n in matches if label in [n.get('text','').strip(),n.get('content-desc','').strip()]]
    candidates=exact or matches
    node=next((n for n in candidates if n.get('clickable')=='true'),candidates[-1])
    x1,y1,x2,y2=map(int,re.findall(r'\d+',node.attrib['bounds']))
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))


def screenshot(name):
    with (OUT/f'{name}.png').open('wb') as handle:
        result=subprocess.run(['rtk','run','-c',f'adb -s {SERIAL} exec-out screencap -p'],stdout=handle,stderr=subprocess.PIPE)
    if result.returncode:raise RuntimeError(result.stderr.decode())


def main():
    OUT.mkdir(parents=True,exist_ok=True)
    results=[]
    for scenario in p00.SCENARIOS:
        p00.android(SERIAL,scenario)
        time.sleep(1.5)
        state=tree(scenario+'-initial')
        if scenario in ('partial-fill','documents'):
            tap_label(state,'ইতিহাস' if scenario=='partial-fill' else 'কাগজপত্র')
            time.sleep(0.8)
        screenshot(scenario)
        state=tree(scenario)
        results.append(dict(scenario=scenario,screenshot=f'{scenario}.png',tree=f'{scenario}.xml',
                            labels=[n.get('text') or n.get('content-desc') for n in state.iter('node') if n.get('text') or n.get('content-desc')]))
        print('Captured Android',scenario,flush=True)
    p00.write_json(OUT/'capture-results.json',results)
    print('Android scenario capture complete.',flush=True)


if __name__=='__main__':main()
