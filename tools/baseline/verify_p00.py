#!/usr/bin/env python3
"""Audit the retained P00 artifacts without requiring live owner data or devices."""
from pathlib import Path
import hashlib
import json
import struct
import xml.etree.ElementTree as ET
import compatibility
import p00

OUT=p00.ROOT/'docs/roadmap/evidence/p00'


def verify():
    compatibility.verify()
    for platform in ['prototype','android','ios']:
        for scenario in p00.SCENARIOS:
            path=OUT/platform/(scenario+('.jpg' if platform=='prototype' else '.png'))
            contents=path.read_bytes()
            if platform=='prototype':
                assert contents[:2]==b'\xff\xd8',path
                offset=2;width=height=0
                while offset<len(contents):
                    assert contents[offset]==255,path
                    while contents[offset]==255:offset+=1
                    marker=contents[offset];offset+=1
                    length=int.from_bytes(contents[offset:offset+2],'big')
                    if marker in [0xc0,0xc1,0xc2,0xc3,0xc5,0xc6,0xc7,0xc9,0xca,0xcb,0xcd,0xce,0xcf]:
                        height=int.from_bytes(contents[offset+3:offset+5],'big')
                        width=int.from_bytes(contents[offset+5:offset+7],'big');break
                    offset+=length
            else:
                assert contents[:8]==b'\x89PNG\r\n\x1a\n',path
                width,height=struct.unpack('!II',contents[16:24])
            assert width>0 and height>0,path
            suffix='.xml' if platform=='android' else '.txt'
            text=(OUT/platform/(scenario+suffix)).read_text()
            assert len(text)>100,(platform,scenario,'missing hierarchy')
    for platform in ['android','ios']:
        root=ET.parse(OUT/'tests'/f'{platform}-compatibility.xml').getroot()
        assert root.attrib['tests']=='7' and root.attrib['failures']=='0' and root.attrib['errors']=='0'
    for scenario in [*p00.SCENARIOS,'motion-r7']:
        summary=json.loads((OUT/'ios'/f'{scenario}-summary.json').read_text())
        assert summary['failedTests']==0 and summary['passedTests']==1,(scenario,summary)
    observations=json.loads((OUT/'persistence-observations.json').read_text())
    assert observations['ios']['outboxReduceMotion'] is True,'iOS reduced motion was not enabled in the retained final run'
    assert observations['android']['outboxReduceMotion'] is True,'Android reduced motion was not enabled'
    # These are observed baseline failures, explicitly not functional acceptance.
    assert all(v['actualDurableOdometer']==12350 for v in observations.values())
    assert all(v['undoRestored'] is False for v in observations.values())
    frames=json.loads((OUT/'prototype/motion-capture.json').read_text())
    assert len(frames['frames'])==78
    for sample in frames['frames']:
        assert (OUT/'prototype'/sample['file']).is_file(),sample['file']
    media=[json.loads(line) for line in (OUT/'media-inspection.jsonl').read_text().splitlines()]
    assert len(media)==9
    for item in media:
        assert item['decodedFrame'] is True and item['durationSeconds']>0
        assert (p00.ROOT/item['file']).is_file()
    design=json.loads((OUT/'design-source-manifest.json').read_text())
    assert design['changedSincePlanning']==['design/app-ui-v3/src/components/DevPanel.tsx']
    for entry in design['files']:
        assert hashlib.sha256((p00.ROOT/entry['path']).read_bytes()).hexdigest()==entry['sha256']
    manifest=OUT/'artifact-manifest.json'
    if manifest.exists():
        for entry in json.loads(manifest.read_text())['files']:
            assert hashlib.sha256((OUT/entry['path']).read_bytes()).hexdigest()==entry['sha256'],entry['path']
    print('P00 artifact audit passed: 15 baseline screenshots/trees, 14 compatibility tests, six successful iOS capture runs, nine decodable motion files, actual reduced-motion flags and explicitly retained Undo failures.')


if __name__=='__main__':verify()
