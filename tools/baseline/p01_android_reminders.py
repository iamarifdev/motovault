#!/usr/bin/env python3
"""Drive P01 permission and snooze flows only in the isolated Android emulator app."""
import argparse
import json
import time
import p00
import capture_android as ui

OUT = p00.ROOT/'docs/roadmap/evidence/p01/android'


def labels(tree):
    return ' '.join(n.get('text', '') + ' ' + n.get('content-desc', '') for n in tree.iter('node'))


def observe_background(target='p01-today', seconds=50):
    started = time.monotonic()
    for _ in range(seconds):
        dump = ui.adb('shell', 'dumpsys', 'notification', '--noredact')
        active = [line.strip() for line in dump.splitlines() if 'NotificationRecord(' in line and p00.APP in line]
        if any(target in line for line in active):
            name = 'background-service-notification' if target.startswith('service:') else 'background-notification'
            p00.write_json(OUT/f'{name}.json', dict(app=p00.APP, active=active, observedAfterSeconds=round(time.monotonic()-started, 2),
                scope='Android emulator background notification from actual isolated vault plan; delivery is inexact'))
            return
        time.sleep(1)
    raise RuntimeError('No active synthetic expiry notification within the bounded observation window')


def open_settings():
    ui.adb('shell', 'am', 'start', '-n', f'{p00.APP}/com.motovault.app.MainActivity')
    tree = ui.tree('permission-entry')
    if 'মেয়াদের কত দিন আগে' in labels(tree) and 'অনুমতি আবার যাচাই করুন' in labels(tree):
        return tree
    for i in range(5):
        if 'আমি' in labels(tree):
            break
        time.sleep(0.2)
        tree = ui.tree(f'permission-entry-wait-{i}')
    ui.tap_label(tree, 'আমি')
    for i in range(8):
        tree = ui.tree(f'permission-scroll-{i}')
        if 'অনুমতি, সময় ও পরে জানান' in labels(tree):
            ui.tap_label(tree, 'অনুমতি, সময় ও পরে জানান')
            for attempt in range(6):
                sheet = ui.tree(f'permission-sheet-{attempt}')
                if 'মেয়াদের কত দিন আগে' in labels(sheet) and 'সেভ করুন' in labels(sheet):
                    return sheet
                time.sleep(0.2)
            raise RuntimeError('Reminder sheet did not open after tapping the observed entry')
        ui.adb('shell', 'input', 'swipe', '540', '1850', '540', '750', '350')
    raise RuntimeError('No observed reminder-settings entry after bounded scrolling')


def main(mode, fixture='runtime'):
    global OUT
    if fixture != 'runtime':
        OUT = OUT/fixture
    OUT.mkdir(parents=True, exist_ok=True)
    ui.OUT = OUT
    if mode == 'prepare':
        p00.FIXTURES = p00.ROOT/'docs/roadmap/fixtures/p01'
        p00.android(ui.SERIAL, fixture)
        tree = open_settings()
    elif mode == 'deny':
        tree = ui.tree('before-native-permission')
        ui.tap_label(tree, 'ফোনে জানানোর অনুমতি দিন')
        dialog = ui.tree('native-permission-dialog')
        print(labels(dialog), flush=True)
        deny = next((label for label in ["Don't allow", 'Don’t allow'] if label in labels(dialog)), None)
        if deny is None:
            raise RuntimeError('Expected observed native notification permission prompt')
        ui.tap_label(dialog, deny)
        tree = ui.tree('permission-denied')
        assert 'নোটিফিকেশনের অনুমতি নেই' in labels(tree)
    elif mode == 'grant':
        ui.adb('shell', 'pm', 'grant', p00.APP, 'android.permission.POST_NOTIFICATIONS')
        ui.adb('shell', 'am', 'force-stop', p00.APP)
        tree = open_settings()
        ui.tap_label(tree, 'ফোনে জানানোর অনুমতি দিন')
        ui.adb('shell', 'input', 'keyevent', '3')
        observe_background('service:9:p00-chain:' if fixture == 'service-runtime' else 'p01-today', 180 if fixture == 'service-runtime' else 50)
        if fixture == 'service-runtime':
            ui.adb('shell', 'cmd', 'statusbar', 'expand-notifications')
            tree = ui.tree('actual-service-notification-shade')
        else:
            ui.adb('shell', 'am', 'start', '-n', f'{p00.APP}/com.motovault.app.MainActivity')
            tree = ui.tree('permission-enabled-after-return')
            assert 'ফোনে জানানোর অনুমতি আছে' in labels(tree)
    elif mode == 'observe':
        observe_background('service:9:p00-chain:' if fixture == 'service-runtime' else 'p01-today')
        ui.adb('shell', 'cmd', 'statusbar', 'expand-notifications')
        tree = ui.tree('actual-notification-shade')
    elif mode == 'tap':
        tree = ui.tree('before-notification-tap')
        ui.tap_label(tree, 'P01 synthetic expiry today')
        tree = ui.tree('notification-exact-document')
        assert 'P01-TEST' in labels(tree) and 'P01 synthetic expiry today' in labels(tree)
    elif mode == 'service-tap':
        tree = ui.tree('before-service-notification-tap')
        ui.tap_label(tree, 'Chain')
        tree = ui.tree('notification-exact-service')
        assert 'সময়সূচি: Chain' in labels(tree)
    elif mode == 'revoke':
        ui.adb('shell', 'input', 'keyevent', '3')
        ui.adb('shell', 'pm', 'revoke', p00.APP, 'android.permission.POST_NOTIFICATIONS')
        open_settings()
        tree = ui.tree('permission-revoked-after-return')
        assert 'নোটিফিকেশনের অনুমতি নেই' in labels(tree)
    elif mode == 'settings':
        tree = ui.tree('before-system-settings')
        ui.tap_label(tree, 'নোটিফিকেশন সেটিংস খুলুন')
        tree = ui.tree('native-notification-settings')
        for i in range(5):
            if any(n.get('package') == 'com.android.settings' for n in tree.iter('node')) and 'MotoVault' in labels(tree):
                break
            time.sleep(0.2)
            tree = ui.tree(f'native-notification-settings-wait-{i}')
        assert 'MotoVault' in labels(tree)
    elif mode == 'settings-recover':
        tree = ui.tree('native-before-notification-enable')
        ui.tap_label(tree, 'All MotoVault notifications')
        tree = ui.tree('native-after-notification-enable')
        assert any(n.get('checkable') == 'true' and n.get('checked') == 'true' for n in tree.iter('node'))
        ui.adb('shell', 'input', 'keyevent', '4')
        tree = open_settings()
        assert 'ফোনে জানানোর অনুমতি আছে' in labels(tree)
    elif mode == 'snooze':
        tree = ui.tree('before-snooze')
        # The current reminder sheet has a scrollable body and a fixed Save action.
        for i in range(6):
            if 'P01 synthetic expiry today' in labels(tree):
                break
            ui.adb('shell', 'input', 'swipe', '540', '1700', '540', '700', '350')
            tree = ui.tree(f'snooze-scroll-{i}')
        ui.tap_label(tree, 'P01 synthetic expiry today')
        tree = ui.tree('document-reminder-sheet')
        ui.tap_label(tree, 'আগামীকাল সকাল ৯টা')
        tree = ui.tree('document-snoozed')
        assert 'পরে জানাবেন:' in labels(tree)
    elif mode == 'fail-save':
        tree = open_settings()
        ui.tap_label(tree, '১৪ দিন')
        ui.tap_label(ui.tree('before-failed-settings-save'), 'সেভ করুন')
        tree = ui.tree('settings-save-failed')
        assert 'সেভ হয়নি। আগের তথ্য রাখা আছে। আবার চেষ্টা করুন।' in labels(tree)
    else:
        raise ValueError(mode)
    ui.screenshot('reminder-'+mode)
    print(labels(tree), flush=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('mode', choices=['prepare', 'deny', 'grant', 'observe', 'tap', 'service-tap', 'revoke', 'settings', 'settings-recover', 'snooze', 'fail-save'])
    parser.add_argument('--fixture', choices=['runtime', 'service-runtime', 'unknown'], default='runtime')
    args = parser.parse_args()
    main(args.mode, args.fixture)
