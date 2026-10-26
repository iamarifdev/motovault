#!/usr/bin/env python3
"""Exercise actual document-then-service alerts only in the isolated emulator app."""
from datetime import datetime, timedelta
import time
import re

import capture_android as ui
import p00
import p01_android_reminders as reminders
import p01_fixtures


def shade(title, name):
    ui.adb('shell', 'cmd', 'statusbar', 'expand-notifications')
    for attempt in range(6):
        time.sleep(0.5)
        tree = ui.tree(name + '-' + str(attempt))
        if title in reminders.labels(tree):
            return tree
    raise RuntimeError('Notification shade did not expose ' + title)


def open_alert(tree, title):
    parents = {child: parent for parent in tree.iter() for child in parent}
    text = next(n for n in tree.iter('node') if title in n.get('text', ''))
    row = text
    while row.get('resource-id') != 'com.android.systemui:id/expandableNotificationRow':
        row = parents[row]
    if row.get('clickable') != 'true':
        group = parents[row]
        while group.get('resource-id') != 'com.android.systemui:id/expandableNotificationRow':
            group = parents[group]
        expand = next(n for n in group.iter('node') if n.get('resource-id') == 'android:id/expand_button'
                      and n.get('content-desc') == 'Expand' and n.get('clickable') == 'true')
        x1, y1, x2, y2 = map(int, re.findall(r'\d+', expand.get('bounds')))
        ui.adb('shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))
        tree = ui.tree('expanded-individual-alert-' + title)
    text = next(n for n in tree.iter('node') if n.get('text', '').startswith(title + ' · '))
    parents = {child: parent for parent in tree.iter() for child in parent}
    row = text
    while row.get('resource-id') != 'com.android.systemui:id/expandableNotificationRow':
        row = parents[row]
    assert row.get('clickable') == 'true', 'Require an individually clickable alert, not a group preview'
    ui.tap_label(tree, text.get('text'))


def main():
    assert p00.APP == 'com.motovault.baseline.p00'
    clock = datetime.fromisoformat(ui.adb('shell', 'date', '+%Y-%m-%dT%H:%M:%S').strip())
    due = clock + timedelta(minutes=2)
    name = 'android-retention-' + clock.strftime('%Y%m%d-%H%M%S')
    out = p00.ROOT / 'docs/roadmap/evidence/p01/android' / name
    out.mkdir(parents=True, exist_ok=False)
    reminders.OUT = ui.OUT = out
    p01_fixtures.generate(due.hour * 60 + due.minute, clock.date().isoformat(), name)
    p00.FIXTURES = p00.ROOT / 'docs/roadmap/fixtures/p01'
    p00.android(ui.SERIAL, name)
    ui.adb('shell', 'cmd', 'statusbar', 'collapse')
    ui.adb('shell', 'pm', 'grant', p00.APP, 'android.permission.POST_NOTIFICATIONS')
    ui.adb('shell', 'am', 'force-stop', p00.APP)
    tree = reminders.open_settings()
    for attempt in range(10):
        if 'ফোনে জানানোর অনুমতি দিন' in reminders.labels(tree):
            break
        time.sleep(0.5)
        tree = ui.tree('permission-ready-' + str(attempt))
    ui.tap_label(tree, 'ফোনে জানানোর অনুমতি দিন')
    print('Awaiting actual policy alerts: ' + due.isoformat(), flush=True)
    verify_delivery(out, name)


def verify_delivery(out, name):
    ui.adb('shell', 'input', 'keyevent', '3')
    time.sleep(1)
    reminders.observe_background('p01-today', 300)
    reminders.observe_background('service:9:p00-chain:', 30)
    tree = shade('P01 synthetic expiry today', 'before-document-tap')
    open_alert(tree, 'P01 synthetic expiry today')
    time.sleep(2)
    tree = ui.tree('exact-document')
    assert 'P01-TEST' in reminders.labels(tree)
    ui.screenshot('exact-document')
    tree = shade('Chain', 'chain-retained-after-document-open')
    assert 'Chain' in reminders.labels(tree)
    open_alert(tree, 'Chain')
    time.sleep(2)
    tree = ui.tree('exact-service')
    assert 'সময়সূচি: Chain' in reminders.labels(tree)
    ui.screenshot('exact-service')
    p00.write_json(out / 'result.json', dict(result='Passed', fixture=name,
        app=p00.APP, synthetic=True, scope='Android emulator actual policy delivery and exact document-then-service routing; physical devices remain pending.'))
    print(str(out), flush=True)


if __name__ == '__main__':
    main()
