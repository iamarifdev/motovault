#!/usr/bin/env python3
"""Run isolated XCTest captures; fixture preparation never targets the owner app."""
from pathlib import Path
import subprocess
import p00

DEVICE='639D8717-57E7-49B8-9DAA-2C365E2F1508'
METHODS={'empty':'testEmpty','partial-fill':'testPartialFill','documents':'testDocuments','unknown-service':'testUnknownService'}


def main():
    out=p00.ROOT/'docs/roadmap/evidence/p00/ios'
    out.mkdir(parents=True,exist_ok=True)
    for scenario,method in METHODS.items():
        p00.ios(DEVICE,scenario)
        result=p00.ROOT/f'build/p00-{scenario}.xcresult'
        if result.exists():raise RuntimeError(f'Result already exists, review it before choosing a new run path: {result}')
        command=['xcodebuild','test-without-building','-project','build/p00-ios-host/MotoVault.xcodeproj',
                 '-scheme','MotoVault','-configuration','Debug','-destination',f'id={DEVICE}',
                 '-derivedDataPath','build/p00-ios-tests-derived',
                 f'-only-testing:MotoVaultUITests/P00BaselineTests/{method}',
                 '-resultBundlePath',str(result),'CODE_SIGNING_ALLOWED=NO']
        import shlex
        with (out/f'{scenario}-xctest.log').open('w') as log:
            process=subprocess.run(['rtk','run','-c',shlex.join(command)],cwd=p00.ROOT,stdout=log,stderr=subprocess.STDOUT)
        if process.returncode:raise RuntimeError(f'XCTest failed for {scenario}; see retained log')
        print(p00.run('xcrun','xcresulttool','export','attachments','--path',str(result),'--output-path',str(out/scenario)))
        print('Captured iOS',scenario,flush=True)


if __name__=='__main__':main()
