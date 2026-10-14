import XCTest

final class OwnerFlowTests: XCTestCase {
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
