import XCTest

final class OwnerFlowTests: XCTestCase {
    func testNativeOwnershipScroll() {
        let app = XCUIApplication(bundleIdentifier: "com.motovault.app")
        app.activate()
        app.swipeUp()
        XCTAssertEqual(app.state, .runningForeground)
        let capture = XCTAttachment(screenshot: app.screenshot())
        capture.name = "native-swipe-ownership"
        capture.lifetime = .keepAlways
        add(capture)
    }
}
