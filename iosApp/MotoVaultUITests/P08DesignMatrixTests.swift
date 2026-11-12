import XCTest

// P08-01 iOS design-matrix checkpoints on the pinned iOS 27 iPhone 18 Pro Max
// simulator. Compose exposes only its root in this runtime's XCTest tree, so
// navigation uses normalized coordinate taps measured from simctl captures of
// the 440 x 956 pt window. Each launch starts from VaultState.seedData() with
// synthetic data only; appearance is changed through in-app Profile controls.
// A green runner proves the sequence executed, not that the rendered state is
// correct: every attachment needs image review.
final class P08DesignMatrixTests: XCTestCase {
    private let app = XCUIApplication(bundleIdentifier: "com.motovault.app.privacyprobe")

    // Bottom navigation (normalized centres of the four tab icons).
    private let garageTab = CGVector(dx: 0.114, dy: 0.930)
    private let vaultTab = CGVector(dx: 0.306, dy: 0.930)
    private let historyTab = CGVector(dx: 0.700, dy: 0.930)
    private let meTab = CGVector(dx: 0.890, dy: 0.930)
    private let quickAdd = CGVector(dx: 0.502, dy: 0.915)

    // Profile appearance card, measured in the settled 440 x 956 pt capture.
    private let themeLight = CGVector(dx: 0.524, dy: 0.312)
    private let themeDark = CGVector(dx: 0.804, dy: 0.312)
    private let languageEn = CGVector(dx: 0.713, dy: 0.5725)
    private let digitsEn = CGVector(dx: 0.713, dy: 0.6817)
    private let textScaleExtra = CGVector(dx: 0.805, dy: 0.790)
    private let ktmSwatch = CGVector(dx: 0.111, dy: 0.4175)
    private let sheetClose = CGVector(dx: 0.934, dy: 0.482)

    private func settle(_ seconds: TimeInterval) {
        RunLoop.current.run(until: Date(timeIntervalSinceNow: seconds))
    }

    private func launch(_ args: [String]) {
        continueAfterFailure = false
        app.launchArguments = ["--seed"] + args
        app.launch()
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 20))
        XCTAssertEqual(app.windows.firstMatch.frame.width, 440, accuracy: 1)
        XCTAssertEqual(app.windows.firstMatch.frame.height, 956, accuracy: 1)
        settle(1.0)
    }

    private func tap(_ point: CGVector) {
        app.coordinate(withNormalizedOffset: point).tap()
        settle(0.45)
    }

    private func drag(from start: CGVector, to end: CGVector) {
        let from = app.coordinate(withNormalizedOffset: start)
        let to = app.coordinate(withNormalizedOffset: end)
        from.press(forDuration: 0.18, thenDragTo: to)
        settle(0.7)
    }

    // Compose exposes semantics with real frames only for elements inside the
    // visible scroll viewport, so label queries resolve after scrolling. The
    // nine brand-colour swatches are exposed without labels and stay coordinate
    // taps; announced rows resolve by label.
    private func visibleButton(_ labelPrefix: String) -> XCUIElement? {
        let matches = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", labelPrefix))
        for index in 0..<matches.count {
            let candidate = matches.element(boundBy: index)
            if !candidate.frame.isEmpty && candidate.isHittable { return candidate }
        }
        return nil
    }

    private func scrollUntilButtonFullyVisible(
        _ labelPrefix: String, maxY: CGFloat = 815, maxSteps: Int = 12
    ) -> XCUIElement? {
        for _ in 0..<maxSteps {
            if let candidate = visibleButton(labelPrefix), candidate.frame.maxY < maxY {
                return candidate
            }
            let from = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.62))
            let to = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.50))
            // A fast drag leaves a fling running; the next tap is then spent
            // stopping the fling instead of toggling the row. Slow pan + hold.
            from.press(
                forDuration: 0.35, thenDragTo: to,
                withVelocity: XCUIGestureVelocity(180), thenHoldForDuration: 0.25
            )
            settle(0.35)
        }
        if let candidate = visibleButton(labelPrefix), candidate.frame.maxY < maxY {
            return candidate
        }
        return visibleButton(labelPrefix)
    }

    // Taps the 52x32 pt switch track at the right end of a toggleable settings
    // row. Tapping the row's centre can land on the title column and not change
    // the toggle, so target the track itself from the resolved row frame.
    private func tapSwitchTrack(_ row: XCUIElement) {
        let frame = row.frame
        let x = (frame.maxX - 40.0) / 440.0
        let y = frame.midY / 956.0
        app.coordinate(withNormalizedOffset: CGVector(dx: x, dy: y)).tap()
        settle(0.7)
    }

    // Wait until the resolved row frame stops moving so the next tap toggles
    // instead of interrupting scroll momentum.
    private func waitForStableButton(_ labelPrefix: String, timeout: TimeInterval = 3.0) -> XCUIElement? {
        var previous = CGRect.null
        let deadline = Date(timeIntervalSinceNow: timeout)
        while Date() < deadline {
            if let candidate = visibleButton(labelPrefix) {
                if !candidate.frame.isEmpty,
                   abs(candidate.frame.minY - previous.minY) < 0.5,
                   abs(candidate.frame.minX - previous.minX) < 0.5 {
                    return candidate
                }
                previous = candidate.frame
            }
            settle(0.15)
        }
        return visibleButton(labelPrefix)
    }

    private func waitForSelected(_ labelPrefix: String, timeout: TimeInterval = 2.0) -> Bool {
        let deadline = Date(timeIntervalSinceNow: timeout)
        while Date() < deadline {
            if visibleButton(labelPrefix)?.isSelected == true { return true }
            settle(0.15)
        }
        return false
    }

    // Enables the in-app Reduce motion switch and reports whether the Selected
    // trait is observed afterwards. A first tap can be consumed while residual
    // scroll momentum settles, so retry once before giving up.
    private func enableReduceMotion() -> Bool {
        guard let row = waitForStableButton("মোশন কমান") else { return false }
        tapSwitchTrack(row)
        if waitForSelected("মোশন কমান") { return true }
        if let retry = waitForStableButton("মোশন কমান") { tapSwitchTrack(retry) }
        return waitForSelected("মোশন কমান")
    }

    private func capture(_ name: String) {
        let image = XCTAttachment(screenshot: app.screenshot())
        image.name = name; image.lifetime = .keepAlways; add(image)
        let tree = XCTAttachment(string: app.debugDescription)
        tree.name = name + "-tree"; tree.lifetime = .keepAlways; add(tree)
    }

    // Four tabs in light and dark, all copy in Bangla and Bengali digits.
    func testTabMatrixLightAndDark() {
        launch(["--tab", "me"])
        tap(themeLight)
        capture("p08-ios-tab-profile-light-bn")
        tap(garageTab); capture("p08-ios-tab-garage-light-bn")
        tap(vaultTab); capture("p08-ios-tab-vault-light-bn")
        tap(historyTab); capture("p08-ios-tab-history-light-bn")
        tap(meTab)
        tap(themeDark)
        capture("p08-ios-tab-profile-dark-bn")
        tap(garageTab); capture("p08-ios-tab-garage-dark-bn")
        tap(vaultTab); capture("p08-ios-tab-vault-dark-bn")
        tap(historyTab); capture("p08-ios-tab-history-dark-bn")
    }

    // English copy and both digit styles, independent of language.
    func testLanguageAndDigitMatrix() {
        launch(["--tab", "me"])
        tap(languageEn)
        capture("p08-ios-tab-profile-light-en")
        tap(digitsEn)
        capture("p08-ios-tab-profile-light-en-digits")
        tap(garageTab)
        capture("p08-ios-tab-garage-light-en-digits")
        launch(["--tab", "me"])
        tap(digitsEn)
        capture("p08-ios-tab-profile-light-bn-en-digits")
        tap(garageTab)
        capture("p08-ios-tab-garage-light-bn-en-digits")
    }

    // Text scale 1.0 (other captures) and 1.3 in both languages.
    func testTextScaleMatrix() {
        launch(["--tab", "me"])
        tap(textScaleExtra)
        capture("p08-ios-tab-profile-light-bn-scale13")
        tap(garageTab)
        capture("p08-ios-tab-garage-light-bn-scale13")
        tap(historyTab)
        capture("p08-ios-tab-records-light-bn-scale13")
        launch(["--tab", "me"])
        tap(languageEn)
        tap(textScaleExtra)
        capture("p08-ios-tab-profile-light-en-scale13")
        tap(garageTab)
        capture("p08-ios-tab-garage-light-en-scale13")
        tap(historyTab)
        capture("p08-ios-tab-records-light-en-scale13")
    }

    // Extreme brand accents on Garage: KTM orange and Royal Enfield brown.
    func testExtremeAccentMatrix() {
        launch(["--tab", "me"])
        tap(themeLight)
        tap(ktmSwatch)
        capture("p08-ios-profile-appearance-ktm-light")
        tap(garageTab)
        capture("p08-ios-garage-accent-ktm-light")
        tap(meTab)
        tap(themeDark)
        tap(garageTab)
        capture("p08-ios-garage-accent-ktm-dark")

        launch(["--tab", "me"])
        tap(themeLight)
        capture("p08-ios-profile-swatches-start-light")
        drag(from: CGVector(dx: 0.85, dy: 0.4175), to: CGVector(dx: 0.20, dy: 0.4175))
        capture("p08-ios-profile-swatches-end-light")
        // Royal Enfield (ninth swatch) after the row is scrolled to its end.
        // Measured from the exported end-scroll capture: centre x = 0.884.
        tap(CGVector(dx: 0.884, dy: 0.4175))
        capture("p08-ios-profile-appearance-enfield-light")
        tap(garageTab)
        capture("p08-ios-garage-accent-enfield-light")
        tap(meTab)
        tap(themeDark)
        tap(garageTab)
        capture("p08-ios-garage-accent-enfield-dark")
    }

    // Representative sheets opened through their real in-app entry points.
    func testSheetMatrix() {
        let cases: [(String, [String])] = [
            ("p08-ios-sheet-quickadd", ["--tab", "garage", "--sheet", "quickAdd"]),
            ("p08-ios-sheet-fuel", ["--tab", "garage", "--sheet", "fuel"]),
            ("p08-ios-sheet-service", ["--tab", "garage", "--sheet", "service"]),
            ("p08-ios-sheet-document-scan", ["--tab", "vault", "--sheet", "docAdd"]),
            ("p08-ios-sheet-backup", ["--tab", "me", "--sheet", "backup"]),
            ("p08-ios-sheet-privacy", ["--tab", "me", "--sheet", "privacy"]),
            ("p08-ios-sheet-expense", ["--tab", "history", "--sheet", "expense"]),
        ]
        for (name, args) in cases {
            launch(args)
            capture(name)
        }
    }

    // Preferences below the fold, including the in-app Reduce motion switch.
    func testProfilePreferencesAndReducedMotion() {
        launch(["--tab", "me", "--scroll-bottom"])
        capture("p08-ios-profile-bottom-bn")
        launch(["--tab", "me"])
        guard scrollUntilButtonFullyVisible("অটো লোকেশন", maxY: 815) != nil else {
            XCTFail("Preferences block did not become visible")
            return
        }
        guard waitForStableButton("অটো লোকেশন") != nil else {
            XCTFail("Preferences block did not settle")
            return
        }
        capture("p08-ios-profile-preferences-bn")
        XCTAssertTrue(enableReduceMotion(), "Reduce motion switch is not selected after the tap")
        capture("p08-ios-profile-preferences-reduce-motion-bn")
    }

    // Motion: tab switches plus quick-add sheet, normal motion. Recorded with
    // xcrun simctl io recordVideo around a run of only this method.
    func testMotionSequenceNormal() {
        launch(["--tab", "garage"])
        tap(meTab)
        tap(vaultTab)
        tap(historyTab)
        tap(garageTab)
        tap(quickAdd)
        settle(1.2)
        tap(sheetClose)
        settle(1.0)
    }

    // Motion: same sequence after enabling the in-app Reduce motion switch.
    func testMotionSequenceReduced() {
        launch(["--tab", "me"])
        guard scrollUntilButtonFullyVisible("মোশন কমান", maxY: 815) != nil else {
            XCTFail("Reduce motion switch did not become visible")
            return
        }
        XCTAssertTrue(enableReduceMotion(), "Reduce motion switch is not selected after the tap")
        capture("p08-ios-motion-reduced-enabled-check")
        tap(garageTab)
        tap(meTab)
        tap(vaultTab)
        tap(historyTab)
        tap(garageTab)
        tap(quickAdd)
        settle(1.2)
        tap(sheetClose)
        settle(1.0)
    }
}
