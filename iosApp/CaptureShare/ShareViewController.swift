import UIKit
import UniformTypeIdentifiers

final class ShareViewController: UIViewController {
    private let status = UILabel()
    private let send = UIButton(type: .system)
    private func copy(_ key: String) -> String { NSLocalizedString(key, comment: "") }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground
        status.numberOfLines = 0
        status.text = copy("capture_share_explain")
        send.setTitle(copy("capture_share_prepare"), for: .normal)
        send.addTarget(self, action: #selector(prepareDraft), for: .touchUpInside)
        let cancel = UIButton(type: .system)
        cancel.setTitle(copy("capture_share_cancel"), for: .normal)
        cancel.addTarget(self, action: #selector(close), for: .touchUpInside)
        let stack = UIStackView(arrangedSubviews: [status, send, cancel])
        stack.axis = .vertical
        stack.spacing = 20
        stack.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor, constant: 24),
            stack.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor, constant: -24),
            stack.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 32)
        ])
    }

    @objc private func close() { extensionContext?.completeRequest(returningItems: nil) }

    @objc private func prepareDraft() {
        send.isEnabled = false
        let items = extensionContext?.inputItems as? [NSExtensionItem] ?? []
        let providers = items.flatMap { $0.attachments ?? [] }
        guard providers.count == 1, let provider = providers.first,
              provider.hasItemConformingToTypeIdentifier(UTType.plainText.identifier) else { fail(); return }
        provider.loadItem(forTypeIdentifier: UTType.plainText.identifier, options: nil) { [weak self] item, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let text = (item as? String) ?? (item as? Data).flatMap { String(data: $0, encoding: .utf8) }
                guard error == nil, let text, !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
                      text.utf16.count <= 10_000, !text.contains("\0") else { self.fail(); return }
                self.store(text)
            }
        }
    }

    private func store(_ text: String) {
        guard let group = Bundle.main.object(forInfoDictionaryKey: "CaptureAppGroup") as? String,
              let root = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: group) else { fail(); return }
        let file = root.appendingPathComponent("pending-capture.json")
        var coordinationError: NSError?
        var saved = false
        NSFileCoordinator().coordinate(writingItemAt: file, options: [], error: &coordinationError) { destination in
            if let size = try? destination.resourceValues(forKeys: [.fileSizeKey]).fileSize, size <= 100_000,
               let data = try? Data(contentsOf: destination),
               let previous = try? JSONDecoder().decode(Packet.self, from: data),
               Date().timeIntervalSince1970 - previous.createdAt >= 600 {
                try? FileManager.default.removeItem(at: destination)
            }
            guard !FileManager.default.fileExists(atPath: destination.path) else { return }
            do {
                let packet = Packet(text: text, createdAt: Date().timeIntervalSince1970)
                try JSONEncoder().encode(packet).write(to: destination, options: [.atomic, .completeFileProtection])
                var values = URLResourceValues()
                values.isExcludedFromBackup = true
                var protected = destination
                try protected.setResourceValues(values)
                saved = true
            } catch { try? FileManager.default.removeItem(at: destination) }
        }
        if saved && coordinationError == nil { status.text = copy("capture_share_open_app") }
        else { fail() }
    }

    private func fail() {
        status.text = copy("capture_share_unavailable")
        send.isEnabled = true
    }
    private struct Packet: Codable { let text: String; let createdAt: TimeInterval }
}
