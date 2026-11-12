import XCTest

// P08-05 runtime language-switch checkpoints on the pinned iOS 27 iPhone 18 Pro
// Max simulator. Each launch resets the isolated synthetic privacy-probe app to
// VaultState.seedData() (Bangla). The tests switch language through the in-app
// Profile control and capture the affected screens without restarting the app.
// Sheet cases use one launch per case so a missing sheet-close control cannot
// contaminate the next state. A green runner is not a visual assertion: every
// attachment is reviewed as an image.
final class P08LocalizationTests: XCTestCase {
    private let app = XCUIApplication(bundleIdentifier: "com.motovault.app.privacyprobe")

    // Bottom-navigation coordinates: content buttons can share words such as
    // "Documents", so tabs are tapped by position.
    private let garageTab = CGVector(dx: 0.114, dy: 0.930)
    private let vaultTab = CGVector(dx: 0.306, dy: 0.930)
    private let historyTab = CGVector(dx: 0.700, dy: 0.930)
    private let meTab = CGVector(dx: 0.890, dy: 0.930)

    private func settle(_ seconds: TimeInterval) {
        RunLoop.current.run(until: Date(timeIntervalSinceNow: seconds))
    }

    private func launch(_ args: [String] = ["--tab", "me"], continueOnFailure: Bool = false) {
        continueAfterFailure = continueOnFailure
        app.launchArguments = ["--seed"] + args
        app.launch()
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 20))
        XCTAssertEqual(app.windows.firstMatch.frame.width, 440, accuracy: 1)
        XCTAssertEqual(app.windows.firstMatch.frame.height, 956, accuracy: 1)
        settle(1.0)
    }

    private func capture(_ name: String) {
        let image = XCTAttachment(screenshot: app.screenshot())
        image.name = name; image.lifetime = .keepAlways; add(image)
        let tree = XCTAttachment(string: app.debugDescription)
        tree.name = name + "-tree"; tree.lifetime = .keepAlways; add(tree)
    }

    private func tapTab(_ point: CGVector) {
        app.coordinate(withNormalizedOffset: point).tap()
        settle(0.7)
    }

    private func hittableButton(_ prefix: String) -> XCUIElement? {
        let matches = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", prefix))
        for index in 0..<matches.count {
            let candidate = matches.element(boundBy: index)
            if !candidate.frame.isEmpty && candidate.isHittable { return candidate }
        }
        return nil
    }

    // Finds a labelled control, scrolling down then back up if needed, and taps it.
    @discardableResult
    private func tapButton(_ prefix: String, scroll: Bool = false) -> Bool {
        if let target = hittableButton(prefix) { target.tap(); settle(0.7); return true }
        guard scroll else { return false }
        for _ in 0..<8 {
            app.swipeUp(); settle(0.3)
            if let target = hittableButton(prefix) { target.tap(); settle(0.7); return true }
        }
        for _ in 0..<12 {
            app.swipeDown(); settle(0.25)
            if let target = hittableButton(prefix) { target.tap(); settle(0.7); return true }
        }
        return false
    }

    @discardableResult
    private func requireTap(_ prefix: String, scroll: Bool = false) -> Bool {
        let ok = tapButton(prefix, scroll: scroll)
        XCTAssertTrue(ok, "Expected a visible control starting with: \(prefix)")
        return ok
    }

    // Switches the app language from the Profile appearance section (top of the
    // screen after launch). Bangla is the synthetic default; tapping the already
    // selected option is harmless but unnecessary, so only switch when needed.
    @discardableResult
    private func switchLanguage(to language: String) -> Bool {
        let label = language == "en" ? "English" : "বাংলা"
        let ok = tapButton(label)
        settle(0.8)
        return ok
    }

    private func captureTabs(_ language: String, prefix: String) {
        tapTab(garageTab); capture("\(prefix)-garage-\(language)")
        tapTab(vaultTab); capture("\(prefix)-documents-\(language)")
        tapTab(historyTab); capture("\(prefix)-records-\(language)")
        tapTab(meTab); capture("\(prefix)-profile-\(language)")
    }

    // Bangla -> English without restart, then English -> Bangla without restart.
    func testTabSwitchWithoutRestartBothWays() {
        launch()
        capture("p08-ios-l10n-profile-bn")
        XCTAssertTrue(switchLanguage(to: "en"), "English language control not found")
        capture("p08-ios-l10n-profile-en")
        XCTAssertFalse(app.buttons["গ্যারেজ"].exists, "Bangla tab labels still exposed after the English switch")
        captureTabs("en", prefix: "p08-ios-l10n")
        XCTAssertTrue(switchLanguage(to: "bn"), "Bangla language control not found after English switch")
        capture("p08-ios-l10n-profile-bn-after-switchback")
        XCTAssertFalse(app.buttons["Garage"].exists, "English tab labels still exposed after switching back")
        tapTab(garageTab); capture("p08-ios-l10n-garage-bn-after-switchback")
        app.terminate()
    }

    private func captureQuickAdd(_ language: String) {
        tapTab(garageTab)
        requireTap(language == "en" ? "Add a record" : "নতুন রেকর্ড যোগ করুন")
        capture("p08-ios-l10n-quickadd-\(language)")
    }

    private func captureSheet(_ language: String, key: String) {
        launch()
        if language == "en" { XCTAssertTrue(switchLanguage(to: "en")) }
        let en = language == "en"
        switch key {
        case "quickadd":
            captureQuickAdd(language)
        case "fuel":
            tapTab(garageTab)
            requireTap(en ? "Add a record" : "নতুন রেকর্ড যোগ করুন")
            requireTap(en ? "Fuel fill" : "ফুয়েল")
            capture("p08-ios-l10n-fuel-\(language)")
        case "service":
            tapTab(garageTab)
            requireTap(en ? "Add a record" : "নতুন রেকর্ড যোগ করুন")
            requireTap(en ? "Service" : "সার্ভিস")
            capture("p08-ios-l10n-service-\(language)")
        case "document":
            tapTab(garageTab)
            requireTap(en ? "Add a record" : "নতুন রেকর্ড যোগ করুন")
            requireTap(en ? "Document" : "ডকুমেন্ট")
            capture("p08-ios-l10n-document-add-\(language)")
        case "expense":
            tapTab(historyTab)
            requireTap(en ? "Add expense" : "খরচ যোগ করুন")
            capture("p08-ios-l10n-expense-\(language)")
        case "privacy":
            tapTab(meTab)
            requireTap(en ? "Privacy settings" : "প্রাইভেসি সেটিংস", scroll: true)
            capture("p08-ios-l10n-privacy-\(language)")
        case "backup":
            tapTab(meTab)
            requireTap(en ? "Create backup" : "ব্যাকআপ তৈরি", scroll: true)
            capture("p08-ios-l10n-backup-\(language)")
        case "notification":
            tapTab(meTab)
            requireTap(en ? "Allow phone notifications" : "নোটিফিকেশন চালু করুন", scroll: true)
            capture("p08-ios-l10n-notification-permission-\(language)")
        default:
            XCTFail("Unknown sheet case \(key)")
        }
        app.terminate()
    }

    func testSheetsBangla() {
        for key in ["quickadd", "fuel", "service", "document", "expense", "privacy", "backup", "notification"] {
            captureSheet("bn", key: key)
        }
    }

    func testSheetsEnglish() {
        for key in ["quickadd", "fuel", "service", "document", "expense", "privacy", "backup", "notification"] {
            captureSheet("en", key: key)
        }
    }
    // P08-02 pre-work: the brand-colour swatch row must expose accessible names
    // (v3 renders it as a wrapped radiogroup) and wrap instead of scrolling.
    func testBrandColourSwatchAccessibility() {
        launch()
        settle(0.8)
        let labelled = app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", "কালার"))
        XCTAssertGreaterThanOrEqual(labelled.count, 9, "Expected nine labelled brand-colour swatches")
        capture("p08-ios-a11y-brand-swatches")
    }
}
