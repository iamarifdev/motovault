#!/usr/bin/env python3
import json
import shlex
import subprocess
import time
from pathlib import Path
import p00
from capture_android import adb, tree, tap_label, screenshot, OUT, SERIAL


def main():
    p00.android(SERIAL,'overdue')
    adb('shell','am','force-stop',p00.APP)
    previous=adb('shell','pidof','screenrecord',check=False)
    if previous:raise RuntimeError('Another emulator recording is active; refusing to interrupt it')
    command=f'adb -s {SERIAL} shell screenrecord --bit-rate 4000000 --time-limit 90 /sdcard/p00-motion.mp4'
    recorder=subprocess.Popen(['rtk','run','-c',command],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    time.sleep(0.5)
    pid=adb('shell','pidof','screenrecord')
    if not pid.isdigit():raise RuntimeError('Could not identify the recording started by this script')
    events=[]
    start=time.monotonic()
    def event(name):
        events.append(dict(name=name,seconds=round(time.monotonic()-start,3)))
    try:
        event('app-launch-gauge-countup')
        adb('shell','am','start','-n',f'{p00.APP}/com.motovault.app.MainActivity')
        time.sleep(1)
        state=tree('motion-home')
        tap_label(state,'ওডোমিটার');event('open-sheet')
        time.sleep(0.4);screenshot('motion-odometer-sheet')
        state=tree('motion-sheet')
        tap_label(state,'+১০')
        state=tree('motion-increment')
        tap_label(state,'সেভ করুন');event('save-12350')
        state=tree('motion-saved')
        screenshot('motion-save-undo')
        tap_label(state,'আনডু');event('tap-undo-request-12340')
        state=tree('motion-undone')
        text=' '.join(n.get('text','')+' '+n.get('content-desc','') for n in state.iter('node'))
        restored='১২,৩৪০' in text
        p00.write_json(OUT/'undo-observation.json',dict(expectedOdometer=12340,
            restored=restored,observedLabels=text,
            scope='Baseline observation; false is a recorded product defect, not a passing Undo verification'))
        event('undo-observed-restored' if restored else 'undo-observed-NOT-restored')
        screenshot('motion-after-undo')
        tap_label(state,'আমি')
        time.sleep(0.4)
        state=tree('motion-settings-top')
        for _ in range(3):
            if any('অ্যানিমেশন কমান' in (n.get('text','')+' '+n.get('content-desc','')) for n in state.iter('node')):break
            adb('shell','input','swipe','540','1850','540','800','350')
            state=tree('motion-settings-scrolled')
        screenshot('motion-before-reduce')
        tap_label(state,'অ্যানিমেশন কমান');event('enable-reduced-motion')
        screenshot('motion-reduced-enabled')
        state=tree('motion-settings-reduced')
        tap_label(state,'গ্যারেজ')
        state=tree('motion-reduced-home')
        tap_label(state,'ওডোমিটার');event('reduced-motion-open-sheet')
        screenshot('motion-reduced-sheet')
        state=tree('motion-reduced-sheet')
        tap_label(state,'বন্ধ করুন');event('reduced-motion-close-sheet')
        screenshot('motion-reduced-home')
    finally:
        adb('shell','kill','-2',pid,check=False)
        stdout,stderr=recorder.communicate(timeout=15)
        adb('pull','/sdcard/p00-motion.mp4',str(OUT/'motion-normal-and-reduced.mp4'))
        p00.write_json(OUT/'motion-events.json',events)
    print(json.dumps(events,ensure_ascii=False))


if __name__=='__main__':main()
