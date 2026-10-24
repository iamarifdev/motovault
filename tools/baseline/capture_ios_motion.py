#!/usr/bin/env python3
import os
from pathlib import Path
import shlex
import signal
import subprocess
import time
import p00

DEVICE='639D8717-57E7-49B8-9DAA-2C365E2F1508'


def main():
    p00.ios(DEVICE,'overdue')
    out=p00.ROOT/'docs/roadmap/evidence/p00/ios'
    result=p00.ROOT/'build/p00-motion-r7.xcresult'
    if result.exists():raise RuntimeError('Review the existing motion result before creating another run')
    movie=out/'motion-normal-and-reduced.mov'
    recording=['xcrun','simctl','io',DEVICE,'recordVideo','--codec=h264','--force',str(movie)]
    recording_log=(out/'motion-recording.log').open('w')
    recorder=subprocess.Popen(['rtk','run','-c','exec '+shlex.join(recording)],cwd=p00.ROOT,
                              stdout=recording_log,stderr=subprocess.STDOUT,start_new_session=True)
    try:
        time.sleep(0.7)
        args=['xcodebuild','test-without-building','-project','build/p00-ios-host/MotoVault.xcodeproj',
              '-scheme','MotoVault','-configuration','Debug','-destination',f'id={DEVICE}',
              '-derivedDataPath','build/p00-ios-tests-derived',
              '-only-testing:MotoVaultUITests/P00BaselineTests/testMotionAndUndo',
              '-resultBundlePath',str(result),'CODE_SIGNING_ALLOWED=NO']
        with (out/'motion-xctest.log').open('w') as log:
            run=subprocess.run(['rtk','run','-c',shlex.join(args)],cwd=p00.ROOT,stdout=log,stderr=subprocess.STDOUT)
    finally:
        os.killpg(recorder.pid,signal.SIGINT)
        recorder.wait(timeout=15)
        recording_log.close()
    print(p00.run('xcrun','xcresulttool','export','attachments','--path',str(result),'--output-path',str(out/'motion-r7')))
    if run.returncode:raise RuntimeError('Motion baseline XCTest failed; inspect retained evidence/log before retrying')
    print('iOS native motion baseline captured; Undo result is an observed baseline outcome, not a product pass claim.')


if __name__=='__main__':main()
