import XCTest

// Pinned iOS 27 iPhone 18 Pro Max visual checkpoints. Compose 1.7 exposes only
// its root in this runtime's XCTest tree. These captures require image review;
// a green runner is not an assertion that the rendered amounts/copy are right.
final class CostFlowTests: XCTestCase {
    private let app = XCUIApplication(bundleIdentifier: "com.motovault.app.privacyprobe")

    private func launch(_ extra: [String] = []) {
        continueAfterFailure = false
        app.launchArguments = ["--motovault-cost-fixture"] + extra
        app.launch()
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 10))
        XCTAssertEqual(app.windows.firstMatch.frame.width, 440, accuracy: 1)
        XCTAssertEqual(app.windows.firstMatch.frame.height, 956, accuracy: 1)
    }

    private func tap(_ x: Double, _ y: Double) {
        app.coordinate(withNormalizedOffset: CGVector(dx: x, dy: y)).tap()
    }

    private func capture(_ name: String) {
        let image = XCTAttachment(screenshot: app.screenshot())
        image.name = name; image.lifetime = .keepAlways; add(image)
        let tree = XCTAttachment(string: app.debugDescription)
        tree.name = name + "-tree"; tree.lifetime = .keepAlways; add(tree)
    }

    func testCaptureCostLocaleMatrix() {
        launch()
        capture("p06-ios-bn-bn")
        tap(0.09, 0.088)
        capture("p06-ios-en-bn")
        tap(0.37, 0.088)
        capture("p06-ios-en-en")
        tap(0.24, 0.088)
        capture("p06-ios-bn-en")
        tap(0.53, 0.088)
        capture("p06-ios-dark-large")
        app.terminate()
    }

    func testCaptureCostSheets() {
        launch()
        // Positions come from the settled 440 x 956 fixture capture.
        tap(0.19, 0.742)
        capture("p06-ios-expense")
        app.terminate()
        launch()
        tap(0.11, 0.14)
        capture("p06-ios-price-bn")
        app.terminate()
        launch()
        tap(0.09, 0.088)
        tap(0.11, 0.14)
        capture("p06-ios-price-en")
        app.terminate()
        launch()
        tap(0.49, 0.742)
        capture("p06-ios-budget")
        app.terminate()
        launch()
        tap(0.80, 0.742)
        capture("p06-ios-details")
        app.terminate()
    }
    func testCaptureExpenseMutationsAndPriceConfirmation() {
        launch()
        tap(0.19, 0.742)
        // Keypad and Save positions were inspected in p06-ios-expense.
        tap(0.19, 0.735); tap(0.50, 0.735); tap(0.19, 0.916); tap(0.50, 0.795)
        capture("p06-ios-expense-draft")
        tap(0.50, 0.672)
        capture("p06-ios-expense-saved")
        tap(0.66, 0.088)
        capture("p06-ios-expense-undone")
        tap(0.37, 0.365)
        capture("p06-ios-fuel-filter")
        tap(0.54, 0.365)
        capture("p06-ios-empty-service-filter")
        app.terminate()
        launch(["--motovault-cost-failure"])
        tap(0.19, 0.742); tap(0.19, 0.735); tap(0.50, 0.672)
        capture("p06-ios-save-failure")
        tap(0.93, 0.152)
        capture("p06-ios-cancelled")
        app.terminate()
        launch()
        tap(0.49, 0.742)
        tap(0.50, 0.735); tap(0.50, 0.916); tap(0.50, 0.916); tap(0.50, 0.916)
        capture("p06-ios-budget-draft")
        tap(0.50, 0.672)
        capture("p06-ios-budget-saved")
        app.terminate()
        launch()
        tap(0.09, 0.088); tap(0.11, 0.14)
        tap(0.075, 0.458)
        capture("p06-ios-price-confirmed")
        tap(0.075, 0.497)
        capture("p06-ios-missed-fill")
        app.terminate()
    }

}
