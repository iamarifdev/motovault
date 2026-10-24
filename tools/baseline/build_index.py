#!/usr/bin/env python3
"""Build a local review index and hash manifest for retained P00 evidence."""
import hashlib
from html import escape
import json
from pathlib import Path
import p00

OUT=p00.ROOT/'docs/roadmap/evidence/p00'


def main():
    rows=[]
    for scenario in p00.SCENARIOS:
        cells=[]
        for platform in ['prototype','android','ios']:
            suffix='jpg' if platform=='prototype' else 'png'
            source=f'{platform}/{scenario}.{suffix}'
            cells.append(f'<figure><a href="{source}"><img src="{source}" alt="{platform} {scenario}"></a><figcaption>{platform}: {scenario}</figcaption></figure>')
        rows.append(f'<section><h2>{escape(scenario)}</h2><div class="grid">'+''.join(cells)+'</div></section>')
    videos=[]
    for path in sorted(OUT.glob('prototype/*.mp4')):
        rel=str(path.relative_to(OUT))
        videos.append(f'<figure><video controls preload="metadata" src="{rel}"></video><figcaption>Prototype sampled sequence: {escape(path.stem)}</figcaption></figure>')
    for platform,suffix in [('android','mp4'),('ios','mov')]:
        rel=f'{platform}/motion-normal-and-reduced.{suffix}'
        videos.append(f'<figure><video controls preload="metadata" poster="{platform}/overdue.png" src="{rel}"></video><figcaption>{platform}: continuous native capture; includes the observed Undo defect</figcaption></figure>')
    page='''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>MotoVault P00 baseline evidence</title>
<style>body{font:16px/1.5 system-ui,sans-serif;max-width:1500px;margin:auto;padding:24px;background:#f5f6f8;color:#182027}h1,h2{line-height:1.2}.grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px}section{margin:32px 0}figure{margin:0;padding:12px;background:white;border:1px solid #dce1e6;border-radius:12px}img,video{display:block;width:100%;height:560px;object-fit:contain;background:#e8ebed}figcaption{margin-top:10px;font-size:14px}aside{padding:16px;border-left:4px solid #b03847;background:#fff0f1}.videos{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px}@media(max-width:800px){.grid,.videos{grid-template-columns:1fr}img,video{height:auto;max-height:720px}}</style>
<h1>MotoVault P00 baseline evidence</h1><p>24 October 2026, Asia/Dhaka. Synthetic records only. Native base: 50eda3a, isolated app identity com.motovault.baseline.p00.</p>
<p><a href="../P00-04-verification.md">Verification report</a> · <a href="../P00-04-design.md">Comparison and discrepancies</a> · <a href="platforms.json">Runtime metadata</a></p>
<aside>These are baseline observations, not proof that all current behavior matches v3. Native Undo is visibly non-restoring. Prototype videos are timestamped screenshot samples; native movies are continuous captures. Physical-device, crypto, cloud and release gates remain separate.</aside>
<h2>Columns: prototype / Android / iOS</h2>'''+''.join(rows)+'<h2>Motion and operation evidence</h2><div class="videos">'+''.join(videos)+'</div></html>'
    (OUT/'index.html').write_text(page)
    manifest=dict(baseCommit='50eda3a',scope='P00 retained synthetic evidence; hashes establish artifact consistency, not product correctness',
                  files=[dict(path=str(path.relative_to(OUT)),bytes=path.stat().st_size,sha256=hashlib.sha256(path.read_bytes()).hexdigest())
                         for path in sorted(OUT.rglob('*')) if path.is_file() and path.name!='artifact-manifest.json'])
    p00.write_json(OUT/'artifact-manifest.json',manifest)
    print(f'Review index and hash manifest written for {len(manifest["files"])} retained artifacts.')


if __name__=='__main__':main()
