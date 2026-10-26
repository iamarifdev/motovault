#!/usr/bin/env python3
"""Check native Back and unsaved reminder drafts in the isolated emulator app."""
import argparse
import time

import capture_android as ui
import p00
import p01_android_reminders as reminders
import p01_storage


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', required=True)
    args = parser.parse_args()
    assert args.run.replace('-', '').isalnum()
    p00.FIXTURES = p00.ROOT / 'docs/roadmap/fixtures/p01'
    for case in ['runtime', 'unknown-large']:
        out = p00.ROOT / 'docs/roadmap/evidence/p01/android' / ('navigation-' + case + '-' + args.run)
        out.mkdir(exist_ok=False)
        ui.OUT = reminders.OUT = out
        p00.android(ui.SERIAL, case)
        time.sleep(1)
        before = p01_storage.operate('android', 'read')
        p00.write_json(out / 'before.json', before)
        tree = reminders.open_settings()
        exact_labels = [n.get('text') or n.get('content-desc') for n in tree.iter('node')]
        assert 'আমি' not in exact_labels and 'কাগজপত্র' not in exact_labels, 'Background tabs exposed while modal is open'
        ui.tap_label(tree, '১৪ দিন')
        tree = ui.tree('draft-14-days')
        assert 'এখন: ১৪ দিন আগে' in reminders.labels(tree) or 'এখন: 14 দিন আগে' in reminders.labels(tree)
        ui.screenshot('draft-14-days')
        ui.adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')
        time.sleep(0.5)
        tree = ui.tree('after-native-back')
        assert 'সেভ করুন' not in reminders.labels(tree), 'Back did not dismiss reminder sheet'
        assert 'অনুমতি, সময় ও পরে জানান' in reminders.labels(tree), 'Back did not restore Me'
        ui.screenshot('after-native-back')
        after = p01_storage.operate('android', 'read')
        p00.write_json(out / 'after.json', after)
        assert before == after, 'Unsaved draft changed durable data'
        p00.write_json(out / 'result.json', dict(result='Passed', synthetic=True,
            applicationId=p00.APP, case=case, api=ui.adb('shell', 'getprop', 'ro.build.version.sdk'),
            architecture=ui.adb('shell', 'getprop', 'ro.product.cpu.abi'),
            scope='Native Back discards a changed draft and restores Me. Background tab labels absent from modal hierarchy. No spoken TalkBack claim.'))
        print(case + ': native Back and persisted-data preservation passed', flush=True)


if __name__ == '__main__':
    main()
