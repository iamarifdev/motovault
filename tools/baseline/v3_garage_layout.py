#!/usr/bin/env python3
"""Measure the real Garage stat cards in synthetic emulator fixtures."""
import argparse
import json
import re
import time

import capture_android as ui
import p00
import p01_android_reminders as reminders


def rect(node):
    return tuple(map(int, re.findall(r'\d+', node.get('bounds'))))


def card(tree, label):
    parents = {c: p for p in tree.iter() for c in p}
    node = next(n for n in tree.iter('node') if n.get('text') == label)
    while node.get('clickable') != 'true':
        node = parents[node]
    return node


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', required=True)
    args = parser.parse_args()
    assert args.run.replace('-', '').isalnum()
    out = p00.ROOT / 'docs/roadmap/evidence/p01/android' / ('garage-layout-' + args.run)
    out.mkdir(exist_ok=False)
    original = p00.ROOT / 'docs/roadmap/fixtures/p01/native'
    p00.FIXTURES = out / 'fixtures'
    cases = [('normal-bn', 'runtime', 'bn', 1.0, False),
             ('large-en-unknown', 'unknown-large', 'en', 1.3, False),
             ('large-bn-long', 'runtime', 'bn', 1.3, True),
             ('large-en-long', 'runtime', 'en', 1.3, True)]
    measurements = []
    for case, source, digits, scale, long_value in cases:
        value = json.loads((original / (source + '.json')).read_text())
        value.update(digits=digits.upper(), textScale=scale, reduceMotion=True)
        if long_value:
            value['ride']['odometer'] = 999999.0
        p00.write_json(p00.FIXTURES / 'native' / (case + '.json'), value)
        ui.OUT = out / case
        ui.OUT.mkdir()
        p00.android(ui.SERIAL, case)
        time.sleep(1)
        width, height = map(int, ui.adb('shell', 'wm', 'size').split()[-1].split('x'))
        for attempt in range(10):
            tree = ui.tree('position-' + str(attempt))
            labels = reminders.labels(tree)
            if 'ওডোমিটার' not in labels:
                time.sleep(0.5)
                continue
            left, right = card(tree, 'ওডোমিটার'), card(tree, 'জ্বালানি গড়')
            a, b = rect(left), rect(right)
            if a[1] > height * 0.12 and max(a[3], b[3]) < height * 0.84:
                break
            ui.adb('shell', 'input', 'swipe', str(width // 2), str(int(height * .7)),
                   str(width // 2), str(int(height * .48)), '400')
        else:
            raise AssertionError('Could not bring both complete cards into view')
        assert abs(a[1] - b[1]) <= 1 and abs(a[3] - b[3]) <= 1, (case, a, b)
        assert abs((a[2] - a[0]) - (b[2] - b[0])) <= 1
        for node in list(left.iter('node')) + list(right.iter('node')):
            if node.get('text'):
                x1, y1, x2, y2 = rect(node)
                container = a if node in list(left.iter('node')) else b
                assert container[0] <= x1 < x2 <= container[2] and container[1] <= y1 < y2 <= container[3], (case, node.attrib)
        ui.screenshot('stats')
        label = next(n for n in left.iter('node') if n.get('text') == 'ওডোমিটার')
        lr = rect(label)
        measurements.append(dict(case=case, digits=digits, appTextScale=scale,
            odometerBounds=a, economyBounds=b, labelHeight=lr[3]-lr[1], result='Passed'))
        ui.tap_label(tree, 'জ্বালানি গড়')
        history = ui.tree('history')
        assert 'গত ৩০ দিনের খরচ' in reminders.labels(history)
        ui.screenshot('history')
        headings = [next(n for n in history.iter('node') if n.get('text') == label)
                    for label in ['গত ৩০ দিনের খরচ', 'চালানো', 'জ্বালানি গড়']]
        heading_bounds = [rect(n) for n in headings]
        assert heading_bounds[0][2] <= heading_bounds[1][0] and heading_bounds[1][2] <= heading_bounds[2][0], heading_bounds
        measurements[-1]['historySummaryHeadingBounds'] = heading_bounds
        print(case, 'equal complete card bounds', a, b, flush=True)
    normal = measurements[0]['labelHeight']
    assert all(m['labelHeight'] >= normal * 1.2 for m in measurements[1:]), 'App text-size setting did not visibly scale explicit labels'
    p00.write_json(out / 'result.json', dict(result='Passed', synthetic=True,
        applicationId=p00.APP, cases=measurements,
        scope='Rendered equal card heights/widths, text bounds, app text scaling, long values and unknown economy on an ARM64 emulator. Not full-app visual parity.'))


if __name__ == '__main__':
    main()
