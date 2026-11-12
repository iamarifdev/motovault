import UIKit

enum CaptureShareRuntimeFixture {
    static func presentIfRequested(from host: UIViewController?) {
        #if DEBUG
        guard Bundle.main.bundleIdentifier == "com.motovault.app.privacyprobe",
              ProcessInfo.processInfo.arguments.contains("--motovault-share-fixture"), let host else { return }
        let text = NSLocalizedString("capture_share_fixture_text", comment: "")
        DispatchQueue.main.async {
            let share = UIActivityViewController(activityItems: [text], applicationActivities: nil)
            share.popoverPresentationController?.sourceView = host.view
            share.completionWithItemsHandler = { _, completed, _, _ in report(expected: text, completed: completed) }
            host.present(share, animated: true)
        }
        #endif
    }

    private static func report(expected: String, completed: Bool) {
        #if DEBUG
        guard let group = Bundle.main.object(forInfoDictionaryKey: "CaptureAppGroup") as? String,
              let root = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: group),
              let caches = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first else { return }
        let file = root.appendingPathComponent("pending-capture.json")
        let protection = (try? FileManager.default.attributesOfItem(atPath: file.path)[.protectionKey]).map { String(describing: $0) }
        let excluded = (try? file.resourceValues(forKeys: [.isExcludedFromBackupKey]))?.isExcludedFromBackup == true
        let received = SharedCaptureInbox().takeText()
        let result: [String: Any] = ["shareCompleted": completed, "textMatches": received == expected,
            "fileProtection": protection ?? "", "backupExcluded": excluded,
            "deletedAfterIntake": !FileManager.default.fileExists(atPath: file.path)]
        if let data = try? JSONSerialization.data(withJSONObject: result, options: [.prettyPrinted, .sortedKeys]) {
            try? data.write(to: caches.appendingPathComponent("p07-share-runtime.json"), options: .atomic)
        }
        #endif
    }
}
