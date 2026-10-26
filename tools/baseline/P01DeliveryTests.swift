import XCTest
import UIKit

// Runs only in the copied host with a hard-coded isolated synthetic application ID.
final class P01DeliveryTests: XCTestCase {
    private let app = XCUIApplication(bundleIdentifier: "com.motovault.baseline.p00")
    private var originalSystemMotion: String?

    override func setUp() { continueAfterFailure = false }

    override func tearDown() {
        if let original = originalSystemMotion {
            let settings = XCUIApplication(bundleIdentifier: "com.apple.Preferences")
            settings.activate()
            let toggle = settings.switches["Reduce Motion"]
            if settings.navigationBars["Motion"].exists && toggle.exists {
                setSystemMotion(toggle, to: original)
                let restored = XCTAttachment(string: settings.debugDescription)
                restored.name = "restored-os-motion-settings"; restored.lifetime = .keepAlways; add(restored)
            }
        }
        super.tearDown()
    }

    private func setSystemMotion(_ toggle: XCUIElement, to desired: String) {
        if toggle.value as? String == desired { return }
        let before = XCTAttachment(string: toggle.debugDescription)
        before.name = "os-motion-switch-before-tap"; before.lifetime = .keepAlways; add(before)
        toggle.coordinate(withNormalizedOffset: CGVector(dx: 0.94, dy: 0.5)).tap()
        let changed = NSPredicate(format: "value == %@", desired)
        if XCTWaiter.wait(for: [XCTNSPredicateExpectation(predicate: changed, object: toggle)], timeout: 2) != .completed {
            if toggle.value as? String != desired { toggle.tap() }
        }
        XCTAssertEqual(XCTWaiter.wait(for: [XCTNSPredicateExpectation(predicate: changed, object: toggle)], timeout: 5), .completed)
    }

    private func control(_ label: String) -> XCUIElement {
        let status = ["ফোনে জানানোর অনুমতি আছে", "ফোনের সেটিংসে নোটিফিকেশনের অনুমতি নেই", "ফোনে জানানো বন্ধ আছে", "নোটিফিকেশনের অনুমতি যাচাই হচ্ছে"].contains(label)
        let matches = app.descendants(matching: .any).matching(NSPredicate(format: status ? "label CONTAINS %@" : "label == %@", label))
        return matches.allElementsBoundByIndex.last ?? matches.firstMatch
    }

    private func capture(_ name: String) {
        let image = XCTAttachment(screenshot: app.screenshot())
        image.name = name; image.lifetime = .keepAlways; add(image)
        let tree = XCTAttachment(string: app.debugDescription)
        tree.name = name + "-tree"; tree.lifetime = .keepAlways; add(tree)
    }

    private func openNotification(_ label: String, in system: XCUIApplication) {
        let notification = system.buttons.matching(NSPredicate(format: "identifier == %@ AND label CONTAINS %@", "ShortLook.Platter.Content.Seamless", label)).firstMatch
        XCTAssertTrue(notification.waitForExistence(timeout: 5))
        notification.swipeRight()
        if !app.wait(for: .runningForeground, timeout: 2) {
            let open = system.buttons["Open"]
            XCTAssertTrue(open.waitForExistence(timeout: 3))
            open.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        }
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 5))
    }

    func testUndoSurvivesRestart() {
        app.launch()
        let odometer = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "ওডোমিটার")).firstMatch
        XCTAssertTrue(odometer.waitForExistence(timeout: 10))
        odometer.tap()
        let increment = control("+১০")
        XCTAssertTrue(increment.waitForExistence(timeout: 5))
        increment.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        let save = control("সেভ করুন")
        XCTAssertTrue(save.exists)
        save.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        let undo = control("আনডু")
        XCTAssertTrue(undo.waitForExistence(timeout: 3))
        undo.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        let restored = app.buttons.matching(NSPredicate(format: "label CONTAINS %@ AND label CONTAINS %@", "ওডোমিটার", "১২,৩৪০")).firstMatch
        XCTAssertTrue(restored.waitForExistence(timeout: 5))
        capture("undo-restored-12340")
        app.terminate()
        app.launch()
        XCTAssertTrue(restored.waitForExistence(timeout: 10))
        XCTAssertFalse(app.buttons.matching(NSPredicate(format: "label CONTAINS %@ AND label CONTAINS %@", "ওডোমিটার", "১২,৩৫০")).firstMatch.exists)
        capture("undo-after-restart-12340")
    }

    private func openReminderSettings(relaunch: Bool = true) {
        if relaunch { app.launch() }
        else {
            app.activate()
            if control("সেভ করুন").exists { return }
        }
        let me = app.buttons.matching(NSPredicate(format: "label == %@", "আমি")).firstMatch
        XCTAssertTrue(me.waitForExistence(timeout: 10))
        me.tap()
        let entry = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "অনুমতি, সময় ও পরে জানান")).firstMatch
        for _ in 0..<8 {
            if entry.exists && entry.isHittable { break }
            app.swipeUp(velocity: .slow)
            Thread.sleep(forTimeInterval: 1)
        }
        capture("me-reminder-entry")
        XCTAssertTrue(entry.exists && entry.isHittable)
        entry.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        if !control("সেভ করুন").waitForExistence(timeout: 5) {
            entry.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        }
        XCTAssertTrue(control("সেভ করুন").waitForExistence(timeout: 5))
        capture("reminder-before-permission")
    }

    func testNotificationPermissionDenial() {
        openReminderSettings()
        control("ফোনে জানানোর অনুমতি দিন").coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        let system = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        let deny = system.buttons["Don’t Allow"].exists ? system.buttons["Don’t Allow"] : system.buttons["Don't Allow"]
        XCTAssertTrue(deny.waitForExistence(timeout: 5))
        deny.tap()
        XCTAssertTrue(control("ফোনের সেটিংসে নোটিফিকেশনের অনুমতি নেই").waitForExistence(timeout: 5))
        capture("notification-denied")
        app.terminate()
        app.launch()
        capture("notification-denied-after-restart")
    }

    func testOpenSystemNotificationSettings() {
        openReminderSettings()
        control("নোটিফিকেশন সেটিংস খুলুন").tap()
        let settings = XCUIApplication(bundleIdentifier: "com.apple.Preferences")
        XCTAssertTrue(settings.wait(for: .runningForeground, timeout: 10))
        _ = settings.switches["Allow Notifications"].waitForExistence(timeout: 10)
        let screen = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        screen.name = "native-app-notification-settings"; screen.lifetime = .keepAlways; add(screen)
        let tree = XCTAttachment(string: settings.debugDescription)
        tree.name = "native-app-notification-settings-tree"; tree.lifetime = .keepAlways; add(tree)
        XCTAssertTrue(settings.switches["Allow Notifications"].exists)
        app.activate()
        XCTAssertTrue(control("ফোনের সেটিংসে নোটিফিকেশনের অনুমতি নেই").waitForExistence(timeout: 5))
        capture("returned-from-system-settings-denied")
    }

    func testManualSettingsRecovery() {
        let settings = XCUIApplication(bundleIdentifier: "com.apple.Preferences")
        settings.launch()
        let search = settings.searchFields.firstMatch
        XCTAssertTrue(search.waitForExistence(timeout: 5))
        search.tap(); search.typeText("MotoVault")
        Thread.sleep(forTimeInterval: 2)
        let tree = XCTAttachment(string: settings.debugDescription)
        tree.name = "settings-search-results"; tree.lifetime = .keepAlways; add(tree)
        let result = settings.staticTexts["MotoVault"].firstMatch
        XCTAssertTrue(result.waitForExistence(timeout: 5))
        result.tap()
        let notifications = settings.buttons["Notifications"]
        if notifications.waitForExistence(timeout: 3) { notifications.tap() }
        let allow = settings.switches["Allow Notifications"].firstMatch
        XCTAssertTrue(allow.waitForExistence(timeout: 5))
        if allow.value as? String == "0" { allow.tap() }
        XCTAssertEqual(allow.value as? String, "1")
        app.activate()
        openReminderSettings()
        XCTAssertTrue(control("ফোনে জানানো বন্ধ আছে").waitForExistence(timeout: 5) || control("ফোনে জানানোর অনুমতি আছে").exists)
        capture("settings-permission-recovered")
    }

    func testUnknownHistoryDetail() {
        app.launch()
        let gauge = app.buttons.matching(NSPredicate(format: "label CONTAINS %@ AND label CONTAINS %@", "Chain", "আগের সার্ভিস নিশ্চিত নয়")).firstMatch
        _ = app.buttons["আমি"].waitForExistence(timeout: 10)
        for _ in 0..<4 {
            if gauge.exists && gauge.isHittable { break }
            app.swipeUp(velocity: .slow)
        }
        XCTAssertTrue(gauge.exists && gauge.isHittable)
        capture("unknown-large-home")
        gauge.tap()
        XCTAssertTrue(control("সময়সূচির উৎস").waitForExistence(timeout: 5))
        XCTAssertTrue(control("শেষ নিশ্চিত কিমি").exists)
        capture("unknown-large-detail")
    }

    func testSettingsSaveFailure() {
        openReminderSettings()
        for _ in 0..<6 {
            if control("১৪ দিন").exists { break }
            app.swipeUp(velocity: .slow)
        }
        XCTAssertTrue(control("১৪ দিন").exists)
        control("১৪ দিন").tap()
        control("সেভ করুন").tap()
        XCTAssertTrue(control("সেভ হয়নি। আগের তথ্য রাখা আছে। আবার চেষ্টা করুন।").waitForExistence(timeout: 5))
        XCTAssertTrue(control("সেভ করুন").exists)
        capture("settings-save-failed-draft-kept")
    }

    func testEnableAndObserveBackgroundNotifications() {
        openReminderSettings()
        let system = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        if !control("ফোনে জানানোর অনুমতি আছে").exists {
            control("ফোনে জানানোর অনুমতি দিন").coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
            let allow = system.buttons["Allow"]
            if allow.waitForExistence(timeout: 3) { allow.tap() }
        }
        XCTAssertTrue(control("ফোনে জানানোর অনুমতি আছে").waitForExistence(timeout: 5))
        capture("permission-enabled")
        XCUIDevice.shared.press(.home)
        system.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.01))
            .press(forDuration: 0.1, thenDragTo: system.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.75)))
        let service = system.staticTexts.matching(NSPredicate(format: "label CONTAINS %@", "Chain")).firstMatch
        XCTAssertTrue(service.waitForExistence(timeout: 150))
        // iOS groups this fixture's three notifications. Expand the observed stack first.
        service.tap()
        let expiry = system.staticTexts.matching(NSPredicate(format: "label CONTAINS %@", "P01 synthetic expiry today")).firstMatch
        XCTAssertTrue(expiry.waitForExistence(timeout: 10))
        XCTAssertTrue(service.waitForExistence(timeout: 10))
        let tree = XCTAttachment(string: system.debugDescription)
        tree.name = "background-notification-tree"; tree.lifetime = .keepAlways; add(tree)
        let image = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        image.name = "background-notification"; image.lifetime = .keepAlways; add(image)
        openNotification("P01 synthetic expiry today", in: system)
        XCTAssertTrue(control("P01-TEST").waitForExistence(timeout: 10))
        capture("notification-exact-document")
        XCUIDevice.shared.press(.home)
        system.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.01))
            .press(forDuration: 0.1, thenDragTo: system.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.75)))
        XCTAssertTrue(service.waitForExistence(timeout: 5))
        openNotification("Chain", in: system)
        XCTAssertTrue(control("সময়সূচি: Chain").waitForExistence(timeout: 10))
        capture("notification-exact-service")
    }

    func testTapDeliveredNotifications() {
        let system = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        XCUIDevice.shared.press(.home)
        system.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.01))
            .press(forDuration: 0.1, thenDragTo: system.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.75)))
        let document = system.buttons.matching(NSPredicate(format: "identifier == %@ AND label CONTAINS %@", "ShortLook.Platter.Content.Seamless", "P01 synthetic expiry today")).firstMatch
        if !document.waitForExistence(timeout: 2) {
            let group = system.buttons.matching(NSPredicate(format: "label CONTAINS %@", "Chain")).firstMatch
            XCTAssertTrue(group.exists)
            group.tap()
        }
        XCTAssertTrue(document.waitForExistence(timeout: 5))
        openNotification("P01 synthetic expiry today", in: system)
        XCTAssertTrue(control("P01-TEST").waitForExistence(timeout: 5))
        capture("notification-exact-document")
        XCUIDevice.shared.press(.home)
        system.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.01))
            .press(forDuration: 0.1, thenDragTo: system.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.75)))
        let service = system.buttons.matching(NSPredicate(format: "identifier == %@ AND label CONTAINS %@", "ShortLook.Platter.Content.Seamless", "Chain")).firstMatch
        XCTAssertTrue(service.waitForExistence(timeout: 5))
        openNotification("Chain", in: system)
        XCTAssertTrue(control("সময়সূচি: Chain").waitForExistence(timeout: 5))
        capture("notification-exact-service")
    }

    func testIndependentNativeNotificationProbe() {
        let probe = XCUIApplication(bundleIdentifier: "com.motovault.baseline.p01probe")
        probe.launch()
        XCTAssertTrue(probe.buttons["Schedule synthetic notification"].waitForExistence(timeout: 5))
        probe.buttons["Schedule synthetic notification"].tap()
        let system = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        let allow = system.buttons["Allow"]
        if allow.waitForExistence(timeout: 3) { allow.tap() }
        XCTAssertTrue(probe.staticTexts["Synthetic notification queued"].waitForExistence(timeout: 5))
        XCUIDevice.shared.press(.home)
        system.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.01))
            .press(forDuration: 0.1, thenDragTo: system.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.75)))
        let notification = system.buttons.matching(NSPredicate(format: "identifier == %@ AND label CONTAINS %@", "ShortLook.Platter.Content.Seamless", "P01 native probe notification")).firstMatch
        XCTAssertTrue(notification.waitForExistence(timeout: 15))
        notification.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        let foreground = probe.wait(for: .runningForeground, timeout: 5)
        let tree = XCTAttachment(string: system.debugDescription)
        tree.name = "independent-native-probe-after-tap"; tree.lifetime = .keepAlways; add(tree)
        let image = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        image.name = "independent-native-probe-after-tap"; image.lifetime = .keepAlways; add(image)
        XCTAssertTrue(foreground)
        XCTAssertTrue(probe.staticTexts["Native notification response received"].exists)
    }

    func testIndependentNativeSettingsProbe() {
        let probe = XCUIApplication(bundleIdentifier: "com.motovault.baseline.p01probe")
        probe.launch()
        let open = probe.buttons["Open native notification settings"]
        XCTAssertTrue(open.waitForExistence(timeout: 5))
        open.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        let settings = XCUIApplication(bundleIdentifier: "com.apple.Preferences")
        XCTAssertTrue(settings.wait(for: .runningForeground, timeout: 5))
        let toggle = settings.switches["Allow Notifications"]
        _ = toggle.waitForExistence(timeout: 8)
        let tree = XCTAttachment(string: settings.debugDescription)
        tree.name = "independent-native-settings-destination"; tree.lifetime = .keepAlways; add(tree)
        let image = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        image.name = "independent-native-settings-destination"; image.lifetime = .keepAlways; add(image)
        XCTAssertTrue(toggle.exists)
    }

    func testMainHostSwiftNotificationProbe() {
        openReminderSettings()
        if !control("ফোনে জানানোর অনুমতি আছে").exists {
            control("ফোনে জানানোর অনুমতি দিন").coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
            let allow = XCUIApplication(bundleIdentifier: "com.apple.springboard").buttons["Allow"]
            if allow.waitForExistence(timeout: 3) { allow.tap() }
            XCTAssertTrue(control("ফোনে জানানোর অনুমতি আছে").waitForExistence(timeout: 5))
        }
        app.terminate()
        app.launchArguments = ["--motovault-notification-probe"]
        app.launch()
        XCTAssertTrue(app.buttons["আমি"].waitForExistence(timeout: 10))
        XCUIDevice.shared.press(.home)
        let system = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        system.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.01))
            .press(forDuration: 0.1, thenDragTo: system.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.75)))
        let notification = system.buttons.matching(NSPredicate(format: "identifier == %@ AND label CONTAINS %@", "ShortLook.Platter.Content.Seamless", "MotoVault synthetic delivery check")).firstMatch
        XCTAssertTrue(notification.waitForExistence(timeout: 25))
        notification.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 5))
        capture("swift-host-probe-returned-to-app")
    }

    func testOpenExistingSwiftProbe() {
        let system = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        XCUIDevice.shared.press(.home)
        system.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.01))
            .press(forDuration: 0.1, thenDragTo: system.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.75)))
        let notification = system.buttons.matching(NSPredicate(format: "identifier == %@ AND label CONTAINS %@", "ShortLook.Platter.Content.Seamless", "MotoVault synthetic delivery check")).firstMatch
        XCTAssertTrue(notification.waitForExistence(timeout: 5))
        notification.swipeRight()
        if !app.wait(for: .runningForeground, timeout: 2) {
            let open = system.buttons["Open"]
            if open.waitForExistence(timeout: 3) { open.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap() }
        }
        let tree = XCTAttachment(string: system.debugDescription)
        tree.name = "notification-side-swipe-result"; tree.lifetime = .keepAlways; add(tree)
        let image = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        image.name = "notification-side-swipe-result"; image.lifetime = .keepAlways; add(image)
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 5))
        capture("swift-probe-system-open-returned-to-app")
    }

    func testDocumentAndServiceSnooze() {
        for title in ["P01 synthetic expiry today", "Chain"] {
            openReminderSettings()
            let record = app.buttons.matching(NSPredicate(format: "label == %@", title)).firstMatch
            for _ in 0..<8 {
                if record.exists && record.frame.height > 0 && record.frame.midY > 200 && record.frame.midY < 780 { break }
                app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.70))
                    .press(forDuration: 0.1, thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.40)))
            }
            capture("snooze-record-selection-" + title)
            XCTAssertTrue(record.exists && record.frame.height > 0)
            record.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
            if !control("ফিরে যান").waitForExistence(timeout: 3) {
                record.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
            }
            XCTAssertTrue(control("ফিরে যান").waitForExistence(timeout: 5))
            let tomorrow = control("আগামীকাল সকাল ৯টা")
            for _ in 0..<4 {
                if tomorrow.exists && tomorrow.isHittable { break }
                app.swipeUp(velocity: .slow)
            }
            XCTAssertTrue(tomorrow.exists)
            capture("snooze-before-action-" + title)
            tomorrow.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
            if !control("রিমাইন্ডারের সময় সেভ হয়েছে").waitForExistence(timeout: 3) {
                capture("snooze-after-first-action-" + title)
                tomorrow.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
            }
            capture("snooze-after-action-" + title)
            XCTAssertTrue(control("রিমাইন্ডারের সময় সেভ হয়েছে").waitForExistence(timeout: 5))
            XCTAssertTrue(app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", "পরে জানাবেন:")).firstMatch.exists)
            capture(title == "Chain" ? "service-snoozed-without-completion" : "document-snoozed-without-completion")
        }
    }

    private func openTestAppNotificationSettings(_ settings: XCUIApplication) -> XCUIElement {
        let name = "MotoVault P01 Test"
        settings.activate()
        let toggle = settings.switches["Allow Notifications"]
        if toggle.exists && settings.staticTexts[name].exists { return toggle }
        // Return through stock navigation without changing any other app's settings.
        for _ in 0..<4 {
            settings.coordinate(withNormalizedOffset: CGVector(dx: 0.01, dy: 0.5))
                .press(forDuration: 0.1, thenDragTo: settings.coordinate(withNormalizedOffset: CGVector(dx: 0.8, dy: 0.5)))
        }
        let apps = settings.buttons["Apps"]
        for _ in 0..<8 {
            if apps.exists && apps.isHittable { break }
            settings.swipeUp(velocity: .slow)
        }
        XCTAssertTrue(apps.exists && apps.isHittable)
        apps.tap()
        let entry = settings.descendants(matching: .any).matching(NSPredicate(format: "label == %@", name)).firstMatch
        for _ in 0..<10 {
            if entry.exists && entry.isHittable { break }
            settings.swipeUp(velocity: .slow)
        }
        XCTAssertTrue(entry.exists && entry.isHittable)
        entry.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        if !toggle.waitForExistence(timeout: 2) {
            XCTAssertTrue(settings.navigationBars[name].waitForExistence(timeout: 5))
            let owner = XCTAttachment(string: settings.debugDescription)
            owner.name = "verified-isolated-app-settings"; owner.lifetime = .keepAlways; add(owner)
            let notifications = settings.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "Notifications")).firstMatch
            XCTAssertTrue(notifications.waitForExistence(timeout: 5))
            notifications.tap()
        }
        XCTAssertTrue(toggle.waitForExistence(timeout: 5))
        XCTAssertTrue(settings.navigationBars["Notifications"].exists || settings.navigationBars[name].exists)
        return toggle
    }

    func testSystemPermissionRevocationAndRecovery() {
        openReminderSettings()
        XCTAssertTrue(control("ফোনে জানানোর অনুমতি আছে").waitForExistence(timeout: 5))
        let settings = XCUIApplication(bundleIdentifier: "com.apple.Preferences")
        let allow = openTestAppNotificationSettings(settings)
        XCTAssertEqual(allow.value as? String, "1")
        // Settings exposes the whole text row as the switch frame; its thumb is at the trailing edge.
        allow.coordinate(withNormalizedOffset: CGVector(dx: 0.94, dy: 0.5)).tap()
        let denied = NSPredicate(format: "value == %@", "0")
        expectation(for: denied, evaluatedWith: allow)
        waitForExpectations(timeout: 5)
        openReminderSettings(relaunch: false)
        XCTAssertTrue(control("ফোনের সেটিংসে নোটিফিকেশনের অনুমতি নেই").waitForExistence(timeout: 5))
        capture("permission-revoked-after-return")
        control("নোটিফিকেশন সেটিংস খুলুন").coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        XCTAssertTrue(settings.wait(for: .runningForeground, timeout: 5))
        let direct = XCTAttachment(string: settings.debugDescription)
        direct.name = "settings-url-destination"; direct.lifetime = .keepAlways; add(direct)
        let recovered = openTestAppNotificationSettings(settings)
        XCTAssertEqual(recovered.value as? String, "0")
        recovered.coordinate(withNormalizedOffset: CGVector(dx: 0.94, dy: 0.5)).tap()
        expectation(for: NSPredicate(format: "value == %@", "1"), evaluatedWith: recovered)
        waitForExpectations(timeout: 5)
        openReminderSettings(relaunch: false)
        XCTAssertTrue(control("ফোনে জানানোর অনুমতি আছে").waitForExistence(timeout: 5))
        capture("permission-recovered-after-return")
    }

    func testRepresentativeScreens() {
        app.launch()
        let odometer = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "ওডোমিটার")).firstMatch
        XCTAssertTrue(odometer.waitForExistence(timeout: 10))
        Thread.sleep(forTimeInterval: 0.5)
        capture("matrix-home")
        let vault = app.buttons.matching(NSPredicate(format: "label == %@", "কাগজপত্র")).firstMatch
        vault.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        XCTAssertTrue(app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", "Tax token")).firstMatch.waitForExistence(timeout: 5))
        Thread.sleep(forTimeInterval: 0.5)
        capture("matrix-vault")
        let me = app.buttons.matching(NSPredicate(format: "label == %@", "আমি")).firstMatch
        me.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        XCTAssertTrue(control("বাইক বদলান").waitForExistence(timeout: 5))
        Thread.sleep(forTimeInterval: 0.5)
        capture("matrix-me")
        openReminderSettings(relaunch: false)
        XCTAssertTrue(control("সেভ করুন").exists)
        capture("matrix-reminder-settings")
    }

    func testReminderAccessibilityAudit() throws {
        continueAfterFailure = true
        defer { continueAfterFailure = false }
        openReminderSettings()
        capture("reminder-before-accessibility-audit")
        XCTAssertFalse(app.buttons.matching(NSPredicate(format: "label == %@", "আমি")).firstMatch.exists)
        XCTAssertTrue(control("৩০ দিন").exists)
        XCTAssertTrue(control("সকাল ৯টা").exists)
        XCTAssertTrue(control("বন্ধ করুন").exists)
        try app.performAccessibilityAudit(for: .all)
        capture("reminder-after-accessibility-audit")
    }

    private func openReminderRecord(_ title: String) {
        openReminderSettings()
        let record = app.buttons.matching(NSPredicate(format: "label == %@", title)).firstMatch
        for _ in 0..<8 {
            if record.exists && record.frame.height > 0 && record.frame.midY > 180 && record.frame.midY < app.frame.height - 120 { break }
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.70))
                .press(forDuration: 0.1, thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.40)))
        }
        XCTAssertTrue(record.exists && record.frame.height > 0)
        record.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        if !control("ফিরে যান").waitForExistence(timeout: 3) {
            record.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        }
        XCTAssertTrue(control("ফিরে যান").waitForExistence(timeout: 5))
    }

    func testEmptyReminderAccessibilityAudit() throws {
        continueAfterFailure = true
        defer { continueAfterFailure = false }
        openReminderSettings()
        XCTAssertTrue(app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", "কাগজ বা সার্ভিসের সময়সূচি যোগ করলে এখানে দেখা যাবে।")).firstMatch.exists)
        XCTAssertFalse(app.buttons.matching(NSPredicate(format: "label == %@", "Tax token")).firstMatch.exists)
        capture("empty-reminders-before-accessibility-audit")
        try app.performAccessibilityAudit(for: .all)
        capture("empty-reminders-after-accessibility-audit")
    }

    func testDocumentReminderAccessibilityAudit() throws {
        continueAfterFailure = true
        defer { continueAfterFailure = false }
        openReminderRecord("Tax token")
        let last = control("৭ দিন পরে সকাল ৯টা")
        if last.frame.height < 44 {
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.70))
                .press(forDuration: 0.1, thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.63)))
        }
        XCTAssertGreaterThanOrEqual(last.frame.height, 44)
        capture("document-before-accessibility-audit")
        try app.performAccessibilityAudit(for: .all)
        capture("document-after-accessibility-audit")
    }

    func testServiceReminderAccessibilityAudit() throws {
        continueAfterFailure = true
        defer { continueAfterFailure = false }
        openReminderRecord("Chain")
        capture("service-before-accessibility-audit")
        try app.performAccessibilityAudit(for: .all)
        capture("service-after-accessibility-audit")
    }

    func testReminderDragDismissDoesNotSave() {
        openReminderSettings()
        let summaries = app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", "এখন:"))
        let summary = summaries.allElementsBoundByIndex.last ?? summaries.firstMatch
        XCTAssertTrue(summary.exists)
        let originalSummary = summary.label
        let fourteen = control("১৪ দিন")
        for _ in 0..<5 {
            if fourteen.isHittable && fourteen.frame.midY < app.frame.height - 120 { break }
            app.swipeUp(velocity: .slow)
        }
        XCTAssertTrue(fourteen.isHittable)
        fourteen.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        XCTAssertNotEqual(summary.label, originalSummary, "Change a draft before checking that dismissal discards it")
        let close = control("বন্ধ করুন")
        XCTAssertTrue(close.waitForExistence(timeout: 5))
        capture("reminder-before-drag-dismiss")
        let start = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0))
            .withOffset(CGVector(dx: 0, dy: close.frame.midY))
        start.press(forDuration: 0.1, thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.92)))
        XCTAssertEqual(XCTWaiter.wait(for: [XCTNSPredicateExpectation(predicate: NSPredicate(format: "exists == false"), object: control("সেভ করুন"))], timeout: 5), .completed)
        XCTAssertTrue(app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "অনুমতি, সময় ও পরে জানান")).firstMatch.exists)
        capture("reminder-after-drag-dismiss")
        openReminderSettings(relaunch: false)
        let reopenedMatches = app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", "এখন:"))
        let reopened = reopenedMatches.allElementsBoundByIndex.last ?? reopenedMatches.firstMatch
        XCTAssertTrue(reopened.exists)
        XCTAssertEqual(reopened.label, originalSummary, "Dismissed draft must not replace the committed policy")
        capture("reminder-reopened-original-policy")
    }

    func testNumericKeypadSafeAreaAndCancel() {
        app.launch()
        let odometer = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "ওডোমিটার")).firstMatch
        XCTAssertTrue(odometer.waitForExistence(timeout: 10))
        let original = odometer.label
        odometer.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        let close = control("বন্ধ করুন")
        XCTAssertTrue(close.waitForExistence(timeout: 5))
        let bangla = control("১").exists
        let keys = bangla ? ["১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯", "০", "মুছুন"]
                          : ["1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "মুছুন"]
        for key in keys {
            let element = control(key)
            XCTAssertTrue(element.exists && element.isHittable, key)
            XCTAssertGreaterThanOrEqual(element.frame.height, 44, key)
            XCTAssertGreaterThanOrEqual(element.frame.minX, app.frame.minX, key)
            XCTAssertLessThanOrEqual(element.frame.maxX, app.frame.maxX, key)
            XCTAssertLessThanOrEqual(element.frame.maxY, app.frame.maxY - 20, key)
        }
        XCTAssertEqual(app.keyboards.count, 0, "Numeric entry uses the in-app keypad")
        let bump = control(bangla ? "+১০" : "+10")
        XCTAssertTrue(bump.isHittable)
        bump.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        let save = control("সেভ করুন")
        XCTAssertTrue(save.isHittable)
        XCTAssertGreaterThanOrEqual(save.frame.height, 44)
        XCTAssertLessThan(save.frame.maxY, control(bangla ? "১" : "1").frame.minY)
        capture("numeric-keypad-safe-area")
        close.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        XCTAssertTrue(odometer.waitForExistence(timeout: 5))
        XCTAssertEqual(odometer.label, original, "Closing must preserve the confirmed odometer")
        capture("numeric-cancel-restored-home")
    }

    func testSystemReducedMotionLiveReturn() {
        app.launch()
        let odometer = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "ওডোমিটার")).firstMatch
        XCTAssertTrue(odometer.waitForExistence(timeout: 10))
        let settings = XCUIApplication(bundleIdentifier: "com.apple.Preferences")
        settings.activate()
        for _ in 0..<5 {
            if settings.navigationBars["Settings"].exists { break }
            settings.coordinate(withNormalizedOffset: CGVector(dx: 0.01, dy: 0.5))
                .press(forDuration: 0.1, thenDragTo: settings.coordinate(withNormalizedOffset: CGVector(dx: 0.8, dy: 0.5)))
        }
        let accessibility = settings.descendants(matching: .any).matching(NSPredicate(format: "label == %@", "Accessibility")).firstMatch
        for _ in 0..<4 {
            if accessibility.exists && accessibility.isHittable { break }
            settings.swipeDown(velocity: .fast)
        }
        for _ in 0..<8 {
            if accessibility.exists && accessibility.isHittable { break }
            settings.swipeUp(velocity: .slow)
        }
        XCTAssertTrue(accessibility.exists && accessibility.isHittable)
        accessibility.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        let motion = settings.descendants(matching: .any).matching(NSPredicate(format: "label == %@", "Motion")).firstMatch
        XCTAssertTrue(motion.waitForExistence(timeout: 5))
        motion.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        let toggle = settings.switches["Reduce Motion"]
        XCTAssertTrue(toggle.waitForExistence(timeout: 5))
        let original = toggle.value as! String
        originalSystemMotion = original
        let originalNote = XCTAttachment(string: original)
        originalNote.name = "original-os-reduce-motion"; originalNote.lifetime = .keepAlways; add(originalNote)
        for desired in ["0", "1"] {
            setSystemMotion(toggle, to: desired)
            app.activate()
            XCTAssertTrue(odometer.waitForExistence(timeout: 5))
            XCTAssertEqual(UIAccessibility.isReduceMotionEnabled, desired == "1")
            odometer.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
            XCTAssertTrue(control("+১০").waitForExistence(timeout: 5))
            capture(desired == "1" ? "os-reduced-sheet" : "os-normal-sheet")
            control("বন্ধ করুন").coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
            settings.activate()
        }
        setSystemMotion(toggle, to: original)
        let restored = XCTAttachment(string: settings.debugDescription)
        restored.name = "restored-os-motion-settings"; restored.lifetime = .keepAlways; add(restored)
        app.activate()
        XCTAssertTrue(odometer.waitForExistence(timeout: 5))
        capture("os-motion-returned-without-restart")
    }
}
