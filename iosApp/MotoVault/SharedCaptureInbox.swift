import Foundation
import ComposeApp

final class SharedCaptureInbox: NSObject, SharedCaptureBridge {
    func takeText() -> String? {
        guard let group = Bundle.main.object(forInfoDictionaryKey: "CaptureAppGroup") as? String,
              let root = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: group) else { return nil }
        let file = root.appendingPathComponent("pending-capture.json")
        guard FileManager.default.fileExists(atPath: file.path) else { return nil }
        var text: String?
        var coordinationError: NSError?
        NSFileCoordinator().coordinate(writingItemAt: file, options: .forDeleting, error: &coordinationError) { destination in
            defer { try? FileManager.default.removeItem(at: destination) }
            text = read(destination)
        }
        return coordinationError == nil ? text : nil
    }

    private func read(_ file: URL) -> String? {
        guard let size = try? file.resourceValues(forKeys: [.fileSizeKey]).fileSize, size <= 100_000,
              let data = try? Data(contentsOf: file),
              let packet = try? JSONDecoder().decode(SharedCapturePacket.self, from: data),
              Date().timeIntervalSince1970 - packet.createdAt < 600,
              packet.createdAt <= Date().timeIntervalSince1970,
              !packet.text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
              packet.text.utf16.count <= 10_000, !packet.text.contains("\0") else { return nil }
        return packet.text
    }
}

private struct SharedCapturePacket: Decodable {
    let text: String
    let createdAt: TimeInterval
}
