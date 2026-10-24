#!/usr/bin/env python3
"""Keep original browser screenshot bytes and name them by their actual format."""
from pathlib import Path
import json
import p00

root=p00.ROOT/'docs/roadmap/evidence/p00/prototype'
renamed={}
for path in root.rglob('*.png'):
    if path.read_bytes().startswith(b'\xff\xd8'):
        target=path.with_suffix('.jpg')
        path.rename(target)
        renamed[str(path.relative_to(root))]=str(target.relative_to(root))
capture=root/'motion-capture.json'
data=json.loads(capture.read_text())
for frame in data['frames']:
    frame['file']=renamed.get(frame['file'],frame['file'])
p00.write_json(capture,data)
for path in (p00.ROOT/'docs/roadmap').rglob('*.md'):
    before=path.read_text();after=before
    for old,new in renamed.items():
        if '/' not in old:after=after.replace('p00/prototype/'+old,'p00/prototype/'+new)
    if before!=after:path.write_text(after)
print(f'Renamed {len(renamed)} JPEG screenshots/samples without re-encoding their original bytes.')
