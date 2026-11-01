import XCTest

final class OwnerFlowTests: XCTestCase {
    func testEnglishFuelEditUndoAndRelaunch() {
        continueAfterFailure = false
        let app = XCUIApplication(bundleIdentifier: "com.motovault.app.privacyprobe")
        app.launchArguments = ["--seed", "--tab", "me"]
        app.launch()

        let english = app.buttons["English"]
        XCTAssertTrue(english.waitForExistence(timeout: 30))
        english.tap()
        Thread.sleep(forTimeInterval: 1)
        app.terminate()

        app.launchArguments = ["--tab", "history"]
        app.launch()
        let fuel = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "Fuel fill · ৪.৯০ L")).firstMatch
        XCTAssertTrue(fuel.waitForExistence(timeout: 30))
        fuel.tap()
        let edit = app.buttons["Edit fuel record"]
        XCTAssertTrue(edit.waitForExistence(timeout: 10))
        edit.tap()
        let delete = app.buttons["Delete"]
        XCTAssertTrue(delete.waitForExistence(timeout: 10))
        let sheet = XCTAttachment(screenshot: app.screenshot())
        sheet.name = "english-fuel-edit-sheet"
        sheet.lifetime = .keepAlways
        add(sheet)
        for _ in 0..<5 { delete.tap() }
        app.buttons["৬"].tap()
        app.buttons["০"].tap()
        app.buttons["০"].tap()
        let save = app.buttons["Save ৳৬০০"]
        XCTAssertTrue(save.waitForExistence(timeout: 10))
        save.tap()
        let undo = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "Undo")).firstMatch
        XCTAssertTrue(undo.waitForExistence(timeout: 10))
        undo.tap()
        let undone = app.buttons.matching(NSPredicate(format: "label CONTAINS %@ AND label CONTAINS %@", "৪.৯০ L", "১২,৩৪০ km")).firstMatch
        XCTAssertTrue(undone.waitForExistence(timeout: 10))
        edit.tap()
        for _ in 0..<5 { delete.tap() }
        app.buttons["৬"].tap()
        app.buttons["০"].tap()
        app.buttons["০"].tap()
        XCTAssertTrue(save.waitForExistence(timeout: 10))
        save.tap()
        XCTAssertTrue(undo.waitForExistence(timeout: 10))
        app.terminate()
        app.launchArguments = ["--tab", "history"]
        app.launch()
        // The seed also contains another 5-litre, 600-taka fill. The odometer
        // distinguishes the edited record from that unchanged historical row.
        let persisted = app.buttons.matching(NSPredicate(format: "label CONTAINS %@ AND label CONTAINS %@ AND label CONTAINS %@", "৫.০০ L", "৳৬০০", "১২,৩৪০ km")).firstMatch
        XCTAssertTrue(persisted.waitForExistence(timeout: 30))
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "english-fuel-edit-reopened"
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }
}
