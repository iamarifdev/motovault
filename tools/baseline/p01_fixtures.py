#!/usr/bin/env python3
"""Generate current P01 runtime inputs without modifying the immutable P00 baseline."""
import argparse
import copy
import json
import hashlib
import p00

OUT = p00.ROOT / 'docs/roadmap/fixtures/p01'


def generate(minute=540, date='2026-10-24', name='runtime'):
    value = json.loads((p00.FIXTURES/'native/documents.json').read_text())
    value['_synthetic'] = True
    value['ride']['odometerConfirmedDate'] = '2026-10-16'
    value.update(serviceReminders=True, docReminders=True,
                 reminderPolicy=dict(warningDays=30, minuteOfDay=minute))
    value['rules'][0]['baselineCertainty'] = 'CONFIRMED'
    value['rules'][1]['baselineCertainty'] = 'UNKNOWN'
    for document in value['documents']:
        document['reminder'] = True
    value['documents'].append(dict(id='p01-today', kind='insurance', title='P01 synthetic expiry today',
        number='P01-TEST', holder='SYNTHETIC OWNER', expiry=date, reminder=True))
    p00.write_json(OUT/f'native/{name}.json', value)
    unknown = copy.deepcopy(value)
    for rule in unknown['rules']:
        rule['baselineCertainty'] = 'UNKNOWN'
    unknown['logs'] = []
    if name == 'runtime':
        p00.write_json(OUT/'native/unknown.json', unknown)
        large = copy.deepcopy(unknown)
        large.update(appearance='DARK', digits='EN', textScale=1.3, reduceMotion=True,
                     serviceReminders=False, docReminders=False)
        p00.write_json(OUT/'native/unknown-large.json', large)
        paired = []
        for key, native in [('p01-runtime', value), ('p01-unknown', unknown), ('p01-unknown-large', large)]:
            prototype = copy.deepcopy(json.loads((p00.FIXTURES/'documents.json').read_text())['prototype'])
            prototype['ride']['odometerAt'] = native['ride']['odometerConfirmedDate']
            for rule, source in zip(prototype['rules'], native['rules']):
                rule.update(baseline=source['baselineCertainty'].lower(), source=source['specification'])
            prototype['docs'].append(dict(id='p01-today', kind='insurance', number='P01-TEST', holder='SYNTHETIC OWNER', expiry=date, reminder=True))
            for doc in prototype['docs']:
                doc['reminder'] = True
            prototype['settings'].update(theme=native['appearance'].lower(), digits=native['digits'].lower(),
                textScale=native['textScale'], reduceMotion=native['reduceMotion'],
                serviceReminders=native['serviceReminders'], docReminders=native['docReminders'],
                reminderLeadDays=30, reminderMinute=minute)
            paired.append(dict(id=key, synthetic=True, native=native, prototype=prototype))
        p00.write_json(OUT/'paired.json', paired)
        p00.write_json(p00.ROOT/'design/app-ui-v3/public/p01-fixtures.json', paired)
        p00.write_json(OUT/'manifest.json', dict(synthetic=True, referenceDate=date, files=[
            dict(path=str(path.relative_to(p00.ROOT)), sha256=hashlib.sha256(path.read_bytes()).hexdigest())
            for path in sorted(OUT.rglob('*.json')) if path.name not in {'manifest.json', 'service-runtime.json', 'ios-delivery.json'}]))
    print('Generated isolated synthetic P01 runtime and unknown-baseline fixtures.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--minute', type=int, default=540)
    parser.add_argument('--date', default='2026-10-24')
    parser.add_argument('--name', choices=['runtime', 'service-runtime', 'ios-delivery'], default='runtime')
    args = parser.parse_args()
    assert 0 <= args.minute < 1440
    generate(args.minute, args.date, args.name)
