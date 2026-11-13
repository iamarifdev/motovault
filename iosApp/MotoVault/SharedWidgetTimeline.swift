import Foundation
import WidgetKit

struct WidgetSnapshotPayload: Decodable {
    let schemaVersion: Int
    let bikeId: String?
    let bikeName: String
    let odometerKm: Double?
    let taskItemKey: String?
    let taskStatus: String
    let privacyRedacted: Bool
    let language: String
    let digits: String
    let updatedAtEpochMillis: Int64
    let staleAfterHours: Int

    var isStale: Bool {
        if updatedAtEpochMillis <= 0 { return true }
        let nowMillis = Int64(Date().timeIntervalSince1970 * 1000.0)
        return (nowMillis - updatedAtEpochMillis) > Int64(staleAfterHours) * 3_600_000
    }
}

enum SharedWidgetSnapshotReader {
    static let defaultsKey = "motovault.widget.snapshot"
    static let snapshotFilename = "widget-snapshot.json"

    static func readCurrent() -> WidgetSnapshotPayload? {
        if let group = Bundle.main.object(forInfoDictionaryKey: "CaptureAppGroup") as? String,
           let root = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: group) {
            let file = root.appendingPathComponent(snapshotFilename)
            if let data = try? Data(contentsOf: file),
               let payload = try? JSONDecoder().decode(WidgetSnapshotPayload.self, from: data) {
                return payload
            }
        }
        if let raw = UserDefaults.standard.string(forKey: defaultsKey),
           let data = raw.data(using: .utf8),
           let payload = try? JSONDecoder().decode(WidgetSnapshotPayload.self, from: data) {
            return payload
        }
        return nil
    }

    static func deepLinkURL(sheet: String, bikeId: String?) -> URL? {
        var components = URLComponents()
        components.scheme = "motovault"
        components.host = "widget"
        components.path = "/action"
        var items = [URLQueryItem(name: "sheet", value: sheet)]
        if let bikeId, !bikeId.isEmpty {
            items.append(URLQueryItem(name: "bikeId", value: bikeId))
        }
        components.queryItems = items
        return components.url
    }
}
