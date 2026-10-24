import XCTest
import UIKit
import ImageIO

// Copied into a temporary test host by tools/baseline/p00.py. Never launches owner data.
final class P00BaselineTests: XCTestCase {
    private let app = XCUIApplication(bundleIdentifier: "com.motovault.baseline.p00")

    private func button(_ text: String) -> XCUIElement {
        app.buttons.matching(NSPredicate(format: "label CONTAINS %@", text)).firstMatch
    }

    private func control(_ text: String, exact: Bool = false) -> XCUIElement {
        // Compose exposes some clickable labels as Other children of unnamed Buttons.
        app.descendants(matching: .any).matching(NSPredicate(format: exact ? "label == %@" : "label CONTAINS %@", text)).firstMatch
    }

    private func capture(_ name: String) {
        let image = XCTAttachment(screenshot: app.screenshot())
        image.name = name; image.lifetime = .keepAlways; add(image)
        let tree = XCTAttachment(string: app.debugDescription)
        tree.name = name + "-tree"; tree.lifetime = .keepAlways; add(tree)
    }

    private func enableReducedMotionFromObservedScreenshot() {
        // The captured hierarchy reports a zero-sized frame for this visible row.
        // Locate the first off pill in Me's Behavior section from the actual screenshot.
        let shot = app.screenshot()
        let source = CGImageSourceCreateWithData(shot.pngRepresentation as CFData, nil)!
        let image = CGImageSourceCreateImageAtIndex(source, 0, nil)!
        XCTAssertEqual(image.bitsPerPixel, 32)
        let data = image.dataProvider!.data! as Data
        let x = Int(Double(image.width) * 0.90)
        var spans: [(Int, Int)] = []
        var start: Int? = nil
        for y in Int(Double(image.height) * 0.20)..<Int(Double(image.height) * 0.84) {
            let offset = y * image.bytesPerRow + x * 4
            let pixel = Array(data[offset..<(offset + 4)])
            // Exact light-palette lineStrong fill (#737E8B), not arbitrary gray text.
            let expected = [115, 126, 139]
            let gray = (0..<4).contains { omitted in
                let rgb = (0..<4).filter { $0 != omitted }.map { Int(pixel[$0]) }.sorted()
                return zip(rgb, expected).allSatisfy { abs($0.0 - $0.1) <= 5 }
            }
            if gray && start == nil { start = y }
            if !gray, let first = start {
                if y - first > 30 { spans.append((first, y)) }
                start = nil
            }
        }
        XCTAssertFalse(spans.isEmpty, "No observed off toggle pill in the Behavior section")
        guard let span = spans.first else { return }
        let ratio = Double(span.0 + span.1) / 2 / Double(image.height)
        let note = XCTAttachment(string: "Screenshot-driven reduce-motion toggle tap at x=0.90, y=\(ratio). PNG \(image.width)x\(image.height); raw UIImage orientation \(shot.image.imageOrientation.rawValue); window \(app.windows.firstMatch.frame). Visible row's accessibility frame was zero; pill located from current image.")
        note.name = "reduced-motion-toggle-location"; note.lifetime = .keepAlways; add(note)
        let window = app.windows.firstMatch
        func coloredSpan(_ image: CGImage, column: Double, target: [Int]) -> Double? {
            let bytes = image.dataProvider!.data! as Data
            let pixelX = Int(Double(image.width) * column)
            var first: Int? = nil
            let low = max(0, Int(Double(image.height) * (ratio - 0.035)))
            let high = min(image.height, Int(Double(image.height) * (ratio + 0.035)))
            for y in low..<high {
                let offset = y * image.bytesPerRow + pixelX * 4
                let pixel = Array(bytes[offset..<(offset + 4)])
                let matches = (0..<4).contains { omitted in
                    let rgb = (0..<4).filter { $0 != omitted }.map { Int(pixel[$0]) }.sorted()
                    return zip(rgb,target.sorted()).allSatisfy { abs($0.0-$0.1) <= 5 }
                }
                if matches && first == nil { first = y }
                if !matches, let begin = first {
                    if y-begin>30 { return Double(begin+y)/2/Double(image.height) }
                    first = nil
                }
            }
            return nil
        }
        for attempt in 0..<4 {
            let current = app.screenshot()
            let source = CGImageSourceCreateWithData(current.pngRepresentation as CFData,nil)!
            let pixels = CGImageSourceCreateImageAtIndex(source,0,nil)!
            if coloredSpan(pixels,column:0.82,target:[49,90,190]) != nil {
                let confirmation = XCTAttachment(string: "Rendered Yamaha-blue on-pill confirmed at the intended reduced-motion row after \(attempt) touch attempts. Durable payload is audited separately.")
                confirmation.name = "reduced-motion-on-confirmed"; confirmation.lifetime = .keepAlways; add(confirmation)
                return
            }
            guard let currentRatio = coloredSpan(pixels,column:0.90,target:[115,126,139]) else {
                XCTFail("Intended reduced-motion pill moved or cannot be observed; refusing a blind tap")
                return
            }
            // Tap the labelled Row's middle, not the decorative thumb. Re-observe after every touch.
            window.coordinate(withNormalizedOffset:.zero).withOffset(CGVector(dx:window.frame.width*0.5,dy:window.frame.height*currentRatio)).press(forDuration:0.12)
            Thread.sleep(forTimeInterval:0.45)
        }
        XCTFail("Reduced-motion row never reached its observed on-state")
    }

    private func start(_ tag: String, tab: String = "home") {
        continueAfterFailure = false
        app.launchArguments = ["--tab", tab]
        app.launch()
        XCTAssertTrue(button("গ্যারেজ").waitForExistence(timeout: 20))
        Thread.sleep(forTimeInterval: 1.2)
        capture(tag)
    }

    func testEmpty() { start("empty-home") }
    func testOverdue() { start("overdue-home") }
    func testPartialFill() { start("partial-fill-history", tab: "history") }
    func testDocuments() { start("documents-vault", tab: "vault") }
    func testUnknownService() { start("unknown-service-home") }

    func testMotionAndUndo() {
        start("motion-home")
        let odometer = button("ওডোমিটার")
        XCTAssertTrue(odometer.exists)
        odometer.tap()
        Thread.sleep(forTimeInterval: 0.5)
        capture("odometer-sheet")
        let increment = control("+১০", exact: true)
        XCTAssertTrue(increment.waitForExistence(timeout: 5))
        increment.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        let save = control("সেভ করুন")
        XCTAssertTrue(save.exists)
        save.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        Thread.sleep(forTimeInterval: 0.25)
        capture("after-save-undo")
        let undo = control("আনডু", exact: true)
        XCTAssertTrue(undo.waitForExistence(timeout: 3))
        undo.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        let outcome = XCTAttachment(string: "Expected restored odometer 12340. Observed label: \(button("ওডোমিটার").label). This captures the baseline; a mismatch is a product defect, not a successful Undo check.")
        outcome.name = "undo-observation"; outcome.lifetime = .keepAlways; add(outcome)
        capture("after-undo")
        button("আমি").tap()
        Thread.sleep(forTimeInterval: 0.5)
        app.swipeUp(velocity: .slow)
        // Compose fling continues after XCTest's UIKit-idle heuristic returns.
        Thread.sleep(forTimeInterval: 2.0)
        capture("settings-motion")
        enableReducedMotionFromObservedScreenshot()
        capture("reduced-motion-enabled")
        button("গ্যারেজ").tap()
        Thread.sleep(forTimeInterval: 1.0)
        button("ওডোমিটার").coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        XCTAssertTrue(button("বন্ধ করুন").waitForExistence(timeout: 5))
        capture("reduced-motion-sheet")
        button("বন্ধ করুন").tap()
        capture("reduced-motion-home")
    }
}
