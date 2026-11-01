import XCTest

final class OwnerFlowTests: XCTestCase {
    func testSyntheticFuelEdit() {
        continueAfterFailure = false
        let app = XCUIApplication(bundleIdentifier: "com.motovault.app.privacyprobe")
        app.launchArguments = ["--seed", "--tab", "history"]
        app.launch()
        func capture(_ name: String) {
            let image = XCTAttachment(screenshot: app.screenshot())
            image.name = name; image.lifetime = .keepAlways; add(image)
            let tree = XCTAttachment(string: app.debugDescription)
            tree.name = name + "-tree"; tree.lifetime = .keepAlways; add(tree)
        }
        let fuel = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "৪.৯০ লিটার")).firstMatch
        XCTAssertTrue(fuel.waitForExistence(timeout: 30))
        capture("p03a-history-before-edit")
        fuel.tap()
        capture("p03a-history-expanded")
        let edit = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "জ্বালানির রেকর্ড ঠিক করুন")).firstMatch
        XCTAssertTrue(edit.waitForExistence(timeout: 10))
        edit.tap()
        sleep(1)
        capture("p03a-fuel-edit-open")
        for _ in 0..<5 { app.buttons["মুছুন"].tap() }
        app.buttons["৬"].tap()
        app.buttons["০"].tap()
        app.buttons["০"].tap()
        let save = app.buttons["৳৬০০ সেভ করুন"]
        XCTAssertTrue(save.waitForExistence(timeout: 5))
        capture("p03a-fuel-edit-600")
        save.tap()
        let undo = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "আনডু")).firstMatch
        XCTAssertTrue(undo.waitForExistence(timeout: 10))
        capture("p03a-fuel-saved")
        undo.tap()
        XCTAssertTrue(fuel.waitForExistence(timeout: 10))
        capture("p03a-fuel-undone")
        edit.tap()
        for _ in 0..<5 { app.buttons["মুছুন"].tap() }
        app.buttons["৬"].tap()
        app.buttons["০"].tap()
        app.buttons["০"].tap()
        XCTAssertTrue(save.waitForExistence(timeout: 5))
        save.tap()
        XCTAssertTrue(undo.waitForExistence(timeout: 10))
        app.terminate()
        app.launchArguments = ["--tab", "history"]
        app.launch()
        let persisted = app.buttons.matching(NSPredicate(format: "label CONTAINS %@ AND label CONTAINS %@", "৫.০০ লিটার", "৳৬০০")).firstMatch
        XCTAssertTrue(persisted.waitForExistence(timeout: 30))
        capture("p03a-fuel-reopened")
    }
}
