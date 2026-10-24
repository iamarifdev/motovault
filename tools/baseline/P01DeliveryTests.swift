import XCTest

// Runs only in the copied host with a hard-coded isolated synthetic application ID.
final class P01DeliveryTests: XCTestCase {
    private let app = XCUIApplication(bundleIdentifier: "com.motovault.baseline.p00")

    override func setUp() { continueAfterFailure = false }

    private func control(_ label: String) -> XCUIElement {
        let matches = app.descendants(matching: .any).matching(NSPredicate(format: "label == %@", label))
        return matches.count > 0 ? matches.element(boundBy: matches.count - 1) : matches.firstMatch
    }

    private func capture(_ name: String) {
        let image = XCTAttachment(screenshot: app.screenshot())
        image.name = name; image.lifetime = .keepAlways; add(image)
        let tree = XCTAttachment(string: app.debugDescription)
        tree.name = name + "-tree"; tree.lifetime = .keepAlways; add(tree)
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

    private func openReminderSettings() {
        app.launch()
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
        control("ফোনে জানানোর অনুমতি দিন").coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        let system = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        let allow = system.buttons["Allow"]
        XCTAssertTrue(allow.waitForExistence(timeout: 5))
        allow.tap()
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
        system.buttons.matching(NSPredicate(format: "label CONTAINS %@", "P01 synthetic expiry today")).firstMatch.tap()
        XCTAssertTrue(control("P01-TEST").waitForExistence(timeout: 10))
        capture("notification-exact-document")
        XCUIDevice.shared.press(.home)
        system.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.01))
            .press(forDuration: 0.1, thenDragTo: system.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.75)))
        XCTAssertTrue(service.waitForExistence(timeout: 5))
        let serviceNotification = system.buttons.matching(NSPredicate(format: "label CONTAINS %@", "Chain")).firstMatch
        serviceNotification.tap()
        if !app.wait(for: .runningForeground, timeout: 2) { serviceNotification.tap() }
        XCTAssertTrue(control("সময়সূচি: Chain").waitForExistence(timeout: 10))
        capture("notification-exact-service")
    }

    func testTapDeliveredNotifications() {
        let system = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        XCUIDevice.shared.press(.home)
        system.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.01))
            .press(forDuration: 0.1, thenDragTo: system.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.75)))
        let document = system.buttons.matching(NSPredicate(format: "label CONTAINS %@", "P01 synthetic expiry today")).firstMatch
        if !document.waitForExistence(timeout: 2) {
            let group = system.buttons.matching(NSPredicate(format: "label CONTAINS %@", "Chain")).firstMatch
            XCTAssertTrue(group.exists)
            group.tap()
        }
        XCTAssertTrue(document.waitForExistence(timeout: 5))
        document.tap()
        if !app.wait(for: .runningForeground, timeout: 2) { document.doubleTap() }
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 5))
        XCTAssertTrue(control("P01-TEST").waitForExistence(timeout: 5))
        capture("notification-exact-document")
        XCUIDevice.shared.press(.home)
        system.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.01))
            .press(forDuration: 0.1, thenDragTo: system.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.75)))
        let service = system.buttons.matching(NSPredicate(format: "label CONTAINS %@", "Chain")).firstMatch
        XCTAssertTrue(service.waitForExistence(timeout: 5))
        service.tap()
        if !app.wait(for: .runningForeground, timeout: 2) { service.tap() }
        XCTAssertTrue(control("সময়সূচি: Chain").waitForExistence(timeout: 5))
        capture("notification-exact-service")
    }
}
