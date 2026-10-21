import XCTest

final class OwnerFlowTests: XCTestCase {
    func testBengaliDocumentFlowAndInsets() {
        continueAfterFailure = false
        let app = XCUIApplication(bundleIdentifier: "com.motovault.app")
        app.launch()
        func button(_ label: String) -> XCUIElement {
            app.buttons.matching(NSPredicate(format: "label CONTAINS %@", label)).firstMatch
        }
        func capture(_ name: String) {
            Thread.sleep(forTimeInterval: 1)
            let shot = XCTAttachment(screenshot: app.screenshot())
            shot.name = name; shot.lifetime = .keepAlways; add(shot)
            let tree = XCTAttachment(string: app.debugDescription)
            tree.name = name + "-bounds"; tree.lifetime = .keepAlways; add(tree)
        }
        XCTAssertTrue(button("কাগজপত্র").waitForExistence(timeout: 10))
        button("কাগজপত্র").tap()
        XCTAssertTrue(button("কাগজ স্ক্যান করুন").waitForExistence(timeout: 5))
        capture("60-bengali-vault-reminders")
        button("কাগজ স্ক্যান করুন").tap()
        capture("61-bengali-document-edge-to-edge")
        XCTAssertFalse(app.staticTexts["Add a document"].exists)
        XCTAssertFalse(app.staticTexts["What are we saving?"].exists)
        let manual = button("তথ্য নিজে লিখুন")
        if manual.exists {
            manual.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        } else {
            // Compose 1.7 dialog controls are absent from this runtime's XCTest tree.
            // Position verified in capture 61 on the requested iPhone simulator.
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.20, dy: 0.855)).tap()
        }
        capture("62-bengali-manual-details")
        app.swipeUp()
        capture("63-bengali-manual-footer")
        app.terminate()
        app.launch()
    }

    func testSheetAndNavigationGeometry() {
        continueAfterFailure = false
        let app = XCUIApplication(bundleIdentifier: "com.motovault.app")
        app.launch()
        func button(_ text: String) -> XCUIElement {
            app.buttons.matching(NSPredicate(format: "label CONTAINS %@", text)).firstMatch
        }
        func capture(_ name: String) {
            Thread.sleep(forTimeInterval: 0.4)
            let image = XCTAttachment(screenshot: app.screenshot())
            image.name = name; image.lifetime = .keepAlways; add(image)
            let tree = XCTAttachment(string: app.debugDescription)
            tree.name = name + "-bounds"; tree.lifetime = .keepAlways; add(tree)
        }
        XCTAssertTrue(button("গ্যারেজ").waitForExistence(timeout: 10))
        let nav = [button("গ্যারেজ"), button("কাগজপত্র"), button("ইতিহাস")]
        for item in nav {
            XCTAssertEqual(item.frame.height, 80, accuracy: 2)
            XCTAssertEqual(item.frame.width, nav[0].frame.width, accuracy: 1)
            XCTAssertEqual(item.frame.minY, nav[0].frame.minY, accuracy: 1)
        }
        capture("50-navigation-equal-width-80pt")
        button("ওডোমিটার").tap()
        capture("51-short-odometer-popup")
        XCTAssertTrue(button("রিডিং সেভ করুন").waitForExistence(timeout: 5))
        XCTAssertLessThan(button("রিডিং সেভ করুন").frame.maxY, app.windows.firstMatch.frame.maxY)
        XCTAssertGreaterThanOrEqual(button("বন্ধ করুন").frame.height, 40)
        XCTAssertGreaterThan(button("বন্ধ করুন").frame.minY, app.windows.firstMatch.frame.height * 0.5)
        button("বন্ধ করুন").coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        Thread.sleep(forTimeInterval: 0.4)
        button("ইতিহাস").tap()
        XCTAssertTrue(button("জ্বালানি যোগ করুন").waitForExistence(timeout: 10))
        for _ in 0..<5 { if button("জ্বালানি যোগ করুন").isHittable { break }; app.swipeUp() }
        button("জ্বালানি যোগ করুন").tap()
        capture("52-long-fuel-sheet")
        XCTAssertLessThan(button("বন্ধ করুন").frame.minY, app.windows.firstMatch.frame.height * 0.35)
        let headerY = button("বন্ধ করুন").frame.minY
        for _ in 0..<3 { app.swipeUp() }
        capture("53-fuel-footer-safe-area")
        XCTAssertEqual(button("বন্ধ করুন").frame.minY, headerY, accuracy: 1)
        button("বন্ধ করুন").coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        Thread.sleep(forTimeInterval: 0.4)
        button("গ্যারেজ").tap()
    }
    func testSkyDocumentAndPassportScreens() {
        continueAfterFailure = false
        let app = XCUIApplication(bundleIdentifier: "com.motovault.app")
        app.launch()
        func element(_ label: String) -> XCUIElement {
            app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", label)).firstMatch
        }
        func tap(_ label: String) {
            let target = element(label)
            XCTAssertTrue(target.waitForExistence(timeout: 10), label)
            for _ in 0..<12 { if target.isHittable { break }; app.swipeUp() }
            XCTAssertTrue(target.isHittable, label); target.tap()
        }
        func capture(_ name: String) {
            Thread.sleep(forTimeInterval: 0.4)
            let image = XCTAttachment(screenshot: app.screenshot())
            image.name = name; image.lifetime = .keepAlways; add(image)
        }
        tap("কাগজপত্র")
        tap("কাগজ স্ক্যান করুন")
        capture("40-document-choose-settled")
        // Visible manual-entry link in the reviewed iPhone 18 Pro Max screenshot.
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.20, dy: 0.855)).tap()
        capture("41-document-manual-top")
        app.swipeUp()
        capture("42-document-manual-bottom")
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.095, dy: 0.10)).tap()
        tap("ইতিহাস")
        tap("সার্ভিস পাসপোর্ট")
        capture("43-passport-review-top")
        app.swipeUp()
        capture("44-passport-review-bottom")
        tap("বন্ধ করুন")
        tap("পাসপোর্ট যাচাই")
        capture("45-passport-verification")
        tap("বন্ধ করুন")
        tap("থিম ও রঙ বদলান")
        tap("ডার্ক")
        _ = element("রেকর্ড সংরক্ষণ হয়েছে").waitForNonExistence(timeout: 5)
        tap("জ্বালানি যোগ করুন")
        capture("46-fuel-dark")
        tap("বন্ধ করুন")
        for _ in 0..<4 { app.swipeDown() }
        tap("থিম ও রঙ বদলান")
        tap("লাইট")
        tap("গ্যারেজ")
    }
    func testSkyFormsWalkthrough() {
        continueAfterFailure = false
        let app = XCUIApplication(bundleIdentifier: "com.motovault.app")
        app.launch()
        func element(_ label: String) -> XCUIElement {
            app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", label)).firstMatch
        }
        func capture(_ name: String) {
            let image = XCTAttachment(screenshot: app.screenshot())
            image.name = name; image.lifetime = .keepAlways; add(image)
            let tree = XCTAttachment(string: app.debugDescription)
            tree.name = name + "-accessibility"; tree.lifetime = .keepAlways; add(tree)
        }
        func tap(_ label: String) {
            let target = element(label)
            XCTAssertTrue(target.waitForExistence(timeout: 10), label)
            for _ in 0..<10 { if target.isHittable { break }; app.swipeUp() }
            XCTAssertTrue(target.isHittable, label)
            target.tap()
        }
        func close() { tap("বন্ধ করুন") }
        func top() { for _ in 0..<4 { app.swipeDown() } }
        XCTAssertTrue(element("আমার গ্যারেজ").waitForExistence(timeout: 10))
        capture("10-garage-light")
        let photo = element("reference photo")
        let odometer = element("ওডোমিটার")
        XCTAssertLessThan(photo.frame.maxY, odometer.frame.minY, "Photo and odometer must not overlap")
        tap("থিম ও রঙ বদলান")
        capture("11-appearance-light")
        tap("ডার্ক")
        capture("12-garage-dark")
        tap("থিম ও রঙ বদলান")
        tap("লাইট")
        tap("ওডোমিটার")
        capture("13-odometer-sheet")
        close()
        tap("কাগজপত্র")
        XCTAssertTrue(element("কাগজপত্রের ভল্ট").waitForExistence(timeout: 10))
        capture("14-vault-empty")
        XCTAssertLessThan(element("কাগজপত্রের ভল্ট").frame.maxY, element("রাস্তায় বের হওয়ার আগে").frame.minY)
        tap("রোডসাইড দেখানোর মোড")
        capture("15-inspection-empty")
        // Compose 1.7's full-screen iOS dialog is visible but absent from XCTest's tree.
        // This is the visible back control in the captured 440 x 956 layout.
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.095, dy: 0.10)).tap()
        tap("কাগজ স্ক্যান করুন")
        capture("16-document-choose")
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.095, dy: 0.10)).tap()
        tap("ইতিহাস")
        XCTAssertTrue(element("রাইডের ইতিহাস").waitForExistence(timeout: 10))
        capture("18-history-empty")
        tap("জ্বালানি যোগ করুন")
        capture("19-fuel-sheet-top")
        app.swipeUp()
        capture("20-fuel-sheet-bottom")
        XCTAssertTrue(element("বন্ধ করুন").isHittable, "Sheet close stays visible after scrolling")
        close()
        tap("সার্ভিস যোগ করুন")
        capture("21-service-sheet-top")
        app.swipeUp()
        capture("22-service-sheet-middle")
        for _ in 0..<4 { app.swipeUp() }
        capture("23-service-sheet-bottom")
        top(); close()
        tap("গ্যারেজ")
        top()
        tap("সব যন্ত্রাংশ")
        capture("24-components")
        tap("যন্ত্রাংশ যোগ করুন")
        capture("25-schedule")
        close()
    }
    func testSkyScreenWalkthrough() {
        continueAfterFailure = false
        let app = XCUIApplication(bundleIdentifier: "com.motovault.app")
        app.launch()

        func element(_ label: String) -> XCUIElement {
            app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", label)).firstMatch
        }
        func capture(_ name: String) {
            let screenshot = XCTAttachment(screenshot: app.screenshot())
            screenshot.name = name
            screenshot.lifetime = .keepAlways
            add(screenshot)
            let tree = XCTAttachment(string: app.debugDescription)
            tree.name = name + "-accessibility"
            tree.lifetime = .keepAlways
            add(tree)
        }
        func tap(_ label: String) {
            let target = element(label)
            XCTAssertTrue(target.waitForExistence(timeout: 10), label)
            for _ in 0..<12 {
                if target.isHittable { break }
                app.swipeUp()
            }
            XCTAssertTrue(target.isHittable, label)
            target.tap()
        }

        capture("01-onboarding-brand")
        tap("KTM বেছে নিন")
        capture("02-model-picker")
        tap("Duke 125 · 2023 বেছে নিন")
        tap("বেছে এগিয়ে যান")
        capture("03-bike-details-top")
        for _ in 0..<5 { app.swipeUp() }
        capture("04-bike-details-bottom")
        tap("সময় ও শেষ সার্ভিসের তথ্য মিলিয়ে দেখেছি")
        tap("গ্যারেজ তৈরি করুন")
        XCTAssertTrue(element("আমার গ্যারেজ").waitForExistence(timeout: 10))
        capture("05-garage-top")
        app.swipeUp()
        capture("06-garage-actions")
    }
    func testNativeOwnershipScroll() {
        let app = XCUIApplication(bundleIdentifier: "com.motovault.app")
        app.activate()
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.17, dy: 0.95)).tap()
        app.swipeUp()
        XCTAssertEqual(app.state, .runningForeground)
        let chain = app.descendants(matching: .any).matching(
            NSPredicate(format: "label CONTAINS %@", "চেইন · Chain")
        ).firstMatch
        XCTAssertTrue(chain.waitForExistence(timeout: 10))
        let capture = XCTAttachment(screenshot: app.screenshot())
        capture.name = "garage-labels-and-scroll-on-iphone-17"
        capture.lifetime = .keepAlways
        add(capture)
    }

    func testHistoryTabLayout() {
        let app = XCUIApplication(bundleIdentifier: "com.motovault.app")
        app.activate()
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.83, dy: 0.95)).tap()

        let history = app.descendants(matching: .any).matching(
            NSPredicate(format: "label CONTAINS %@", "রাইডের ইতিহাস")
        ).firstMatch
        XCTAssertTrue(history.waitForExistence(timeout: 10))

        let capture = XCTAttachment(screenshot: app.screenshot())
        capture.name = "history-filters-on-iphone-17"
        capture.lifetime = .keepAlways
        add(capture)
    }
}
