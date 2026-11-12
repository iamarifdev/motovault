import XCTest

// Pinned iOS 27 visual checkpoints. Compose exposes only its root to XCTest here.
// Runner success proves capture execution. Rendered copy/values need image review.
final class CaptureFlowTests: XCTestCase {
    private let app = XCUIApplication(bundleIdentifier: "com.motovault.app.privacyprobe")
    override func tearDown() { app.terminate(); super.tearDown() }
    private func launch() {
        continueAfterFailure = false
        app.launchArguments = ["--motovault-capture-fixture", "--motovault-voice-capability-probe"]
        app.launch()
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 10))
        XCTAssertEqual(app.windows.firstMatch.frame.width, 440, accuracy: 1)
        XCTAssertEqual(app.windows.firstMatch.frame.height, 956, accuracy: 1)
    }
    private func tap(_ x: Double, _ y: Double) {
        app.coordinate(withNormalizedOffset: CGVector(dx: x, dy: y)).tap()
    }
    private func motoVaultShareTarget() -> XCUIElement {
        let target = app.cells.matching(identifier: "shareCell")
            .matching(NSPredicate(format: "label == %@", "MotoVault")).firstMatch
        if !target.waitForExistence(timeout: 8) { print(app.debugDescription) }
        XCTAssertTrue(target.exists)
        return target
    }
    private func capture(_ name: String) {
        let image = XCTAttachment(screenshot: app.screenshot())
        image.name = name; image.lifetime = .keepAlways; add(image)
        let tree = XCTAttachment(string: app.debugDescription)
        tree.name = name + "-tree"; tree.lifetime = .keepAlways; add(tree)
    }
    func testCapturePhotoReview() {
        launch()
        tap(0.5, 0.247)
        capture("p07-ios-photo-bn-bn-choice-required")
        app.terminate()
    }
    func testCapturePhotoSelectionAndLocaleMatrix() {
        launch()
        tap(0.5, 0.247)
        tap(0.12, 0.695)
        capture("p07-ios-photo-selected")
        tap(0.5, 0.868)
        capture("p07-ios-photo-applied-still-needs-save")
        app.terminate()
        launch()
        tap(0.114, 0.143)
        tap(0.5, 0.247)
        capture("p07-ios-photo-en-bn")
        tap(0.5, 0.06)
        tap(0.40, 0.143)
        tap(0.545, 0.143)
        tap(0.5, 0.247)
        capture("p07-ios-photo-en-en-dark-large")
        app.terminate()
    }
    func testNativeShareExtension() {
        continueAfterFailure = false
        app.launchArguments = ["--motovault-capture-fixture", "--motovault-share-fixture"]
        app.launch()
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 10))
        let target = motoVaultShareTarget()
        capture("p07-ios-native-share-sheet")
        target.tap()
        let english = app.buttons["Prepare draft"].firstMatch
        let bangla = app.buttons["ড্রাফট তৈরি করুন"].firstMatch
        let prepare = english.waitForExistence(timeout: 3) ? english : bangla
        if !prepare.waitForExistence(timeout: 5) { print(app.debugDescription) }
        XCTAssertTrue(prepare.exists)
        prepare.tap()
        let savedEnglish = app.staticTexts["Draft prepared. Close this sheet, open MotoVault and unlock it to review. This has not saved a record."]
        let savedBangla = app.staticTexts["ড্রাফট তৈরি হয়েছে। এই শিট বন্ধ করে MotoVault খুলুন ও আনলক করে মিলিয়ে নিন। কোনো রেকর্ড সেভ হয়নি।"]
        let saved = savedEnglish.waitForExistence(timeout: 3) ? savedEnglish : savedBangla
        if !saved.waitForExistence(timeout: 5) { print(app.debugDescription) }
        XCTAssertTrue(saved.exists)
        capture("p07-ios-native-share-prepared")
        let closeEnglish = app.buttons["Close"].firstMatch
        let close = closeEnglish.exists ? closeEnglish : app.buttons["বন্ধ করুন"].firstMatch
        close.tap()
        capture("p07-ios-native-share-returned")
        app.terminate()
    }
    func testNativeShareExtensionCancellation() {
        continueAfterFailure = false
        app.launchArguments = ["--motovault-capture-fixture", "--motovault-share-fixture"]
        app.launch()
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 10))
        let target = motoVaultShareTarget()
        capture("p07-ios-native-share-cancel-sheet")
        target.tap()
        let englishPrepare = app.buttons["Prepare draft"].firstMatch
        let banglaPrepare = app.buttons["ড্রাফট তৈরি করুন"].firstMatch
        let prepare = englishPrepare.waitForExistence(timeout: 3) ? englishPrepare : banglaPrepare
        XCTAssertTrue(prepare.waitForExistence(timeout: 5))
        let englishClose = app.buttons["Close"].firstMatch
        let banglaClose = app.buttons["বন্ধ করুন"].firstMatch
        let close = englishClose.waitForExistence(timeout: 2) ? englishClose : banglaClose
        XCTAssertTrue(close.waitForExistence(timeout: 5))
        close.tap()
        let extensionClosed = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "exists == false"), object: prepare
        )
        XCTAssertEqual(XCTWaiter.wait(for: [extensionClosed], timeout: 5), .completed)
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 10))
        capture("p07-ios-native-share-cancel-returned")
        app.terminate()
    }
    func testCaptureVoiceReview() {
        launch()
        tap(0.5, 0.381)
        capture("p07-ios-capture-voice-bn")
        app.terminate()
        launch()
        tap(0.114, 0.143)
        tap(0.5, 0.381)
        capture("p07-ios-capture-voice-en")
        app.terminate()
    }
    func testCaptureExactMoneyReview() {
        launch()
        tap(0.5, 0.381)
        capture("p07-ios-capture-exact-money-bn")
        app.terminate()
        launch()
        tap(0.114, 0.143)
        tap(0.5, 0.381)
        capture("p07-ios-capture-exact-money-en")
        app.terminate()
    }
    func testCaptureNativeCameraCancellation() {
        launch()
        tap(0.5, 0.315)
        tap(0.5, 0.586)
        // Allow the native availability callback and its Compose frame to settle.
        RunLoop.current.run(until: Date(timeIntervalSinceNow: 0.8))
        capture("p07-ios-native-camera-open")
        tap(0.13, 0.94)
        RunLoop.current.run(until: Date(timeIntervalSinceNow: 0.3))
        capture("p07-ios-camera-cancelled-manual")
        app.terminate()
    }
    func testCaptureCameraDenied() {
        launch()
        tap(0.5, 0.315)
        tap(0.5, 0.586)
        RunLoop.current.run(until: Date(timeIntervalSinceNow: 0.8))
        capture("p07-ios-camera-denied")
        tap(0.18, 0.595)
        RunLoop.current.run(until: Date(timeIntervalSinceNow: 0.3))
        capture("p07-ios-camera-denied-manual")
        app.terminate()
    }
}
