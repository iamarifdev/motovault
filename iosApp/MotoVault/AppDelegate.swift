import UIKit
import UserNotifications
import ComposeApp
import CryptoKit
import Security

// No key is created during an open operation. Provisioning belongs to a verified fresh-store flow.
final class DocumentAssetCrypto: NSObject, IosAssetCryptography {
    private let service: String
    private let account: String
    private let rootOverride: URL?
    private(set) var lastStatus: OSStatus = errSecSuccess

    init(service: String = "com.motovault.document-assets", account: String = "local-key-v1", rootOverride: URL? = nil) {
        self.service = service
        self.account = account
        self.rootOverride = rootOverride
        super.init()
    }

    func deleteSyntheticProbeKey() -> Bool {
        guard service.hasPrefix("com.motovault.synthetic.") else { return false }
        let status = SecItemDelete(query as CFDictionary)
        return status == errSecSuccess || status == errSecItemNotFound
    }

    static func clearTemporaryCopies() {
        guard let caches = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first else { return }
        let directory = caches.appendingPathComponent("document-exports", isDirectory: true)
        guard let attributes = try? FileManager.default.attributesOfItem(atPath: directory.path),
              attributes[.type] as? FileAttributeType == .typeDirectory,
              let files = try? FileManager.default.contentsOfDirectory(at: directory, includingPropertiesForKeys: nil) else { return }
        for file in files where file.lastPathComponent.hasPrefix("copy-") {
            guard let item = try? FileManager.default.attributesOfItem(atPath: file.path),
                  item[.type] as? FileAttributeType == .typeRegular else { continue }
            try? FileManager.default.removeItem(at: file)
        }
    }

    private var query: [String: Any] {
        [kSecClass as String: kSecClassGenericPassword,
         kSecAttrService as String: service,
         kSecAttrAccount as String: account,
         kSecAttrSynchronizable as String: false]
    }

    private func existingKey() -> SymmetricKey? {
        var request = query
        request[kSecReturnData as String] = true
        request[kSecMatchLimit as String] = kSecMatchLimitOne
        var result: CFTypeRef?
        lastStatus = SecItemCopyMatching(request as CFDictionary, &result)
        guard lastStatus == errSecSuccess,
              let data = result as? Data, data.count == 32 else { return nil }
        return SymmetricKey(data: data)
    }

    func prepareFreshStore() -> Bool {
        var request = query
        request[kSecReturnData as String] = true
        var result: CFTypeRef?
        let status = SecItemCopyMatching(request as CFDictionary, &result)
        lastStatus = status
        if status == errSecSuccess { return (result as? Data)?.count == 32 }
        guard status == errSecItemNotFound,
              let root = rootOverride ?? FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first else { return false }
        let directory = root.appendingPathComponent("documents-protected", isDirectory: true)
        // A marker or any protected/staged file means this is no longer a fresh key store.
        for name in ["documents-protected", "documents-recovery-protected"] {
            let existingDirectory = root.appendingPathComponent(name, isDirectory: true)
            if FileManager.default.fileExists(atPath: existingDirectory.path) {
            guard let attributes = try? FileManager.default.attributesOfItem(atPath: existingDirectory.path),
                  attributes[.type] as? FileAttributeType == .typeDirectory,
                  let entries = try? FileManager.default.contentsOfDirectory(atPath: existingDirectory.path),
                  entries.isEmpty else { return false }
            } else if (try? FileManager.default.attributesOfItem(atPath: existingDirectory.path)) != nil {
            return false
            }
        }
        let key = SymmetricKey(size: .bits256)
        var addition = query
        addition[kSecAttrAccessible as String] = kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        addition[kSecValueData as String] = key.withUnsafeBytes { Data($0) }
        lastStatus = SecItemAdd(addition as CFDictionary, nil)
        guard lastStatus == errSecSuccess else { return false }
        do {
            try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true,
                attributes: [.protectionKey: FileProtectionType.complete])
            try Data([1]).write(to: directory.appendingPathComponent("key-v1.initialized"), options: .atomic)
            return true
        } catch { return false }
    }

    func seal(plaintext: Data, associatedData: Data) -> Data? {
        guard let key = existingKey() else { return nil }
        return try? AES.GCM.seal(plaintext, using: key, authenticating: associatedData).combined
    }

    func sha256(data: Data) -> Data { Data(SHA256.hash(data: data)) }

    func open(combined: Data, associatedData: Data) -> Data? {
        guard let key = existingKey(), let box = try? AES.GCM.SealedBox(combined: combined) else { return nil }
        return try? AES.GCM.open(box, using: key, authenticating: associatedData)
    }
}

@main
final class AppDelegate: UIResponder, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        DocumentAssetCrypto.clearTemporaryCopies()
        IosDocumentCrypto.shared.provider = DocumentAssetCrypto()
        // The notification center holds its delegate weakly. UIApplication retains this object.
        UNUserNotificationCenter.current().delegate = self
        #if DEBUG
        if ProcessInfo.processInfo.arguments.contains("--motovault-storage-probe"),
           Bundle.main.bundleIdentifier?.hasSuffix(".privacyprobe") == true,
           let root = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first,
           let caches = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first {
            let resultURL = caches.appendingPathComponent("p02-storage-probe.txt")
            try? FileManager.default.removeItem(at: resultURL)
            let token = UUID().uuidString
            let requestedRun = ProcessInfo.processInfo.arguments.first(where: { $0.hasPrefix("--motovault-storage-probe-run=") }).map { String($0.dropFirst("--motovault-storage-probe-run=".count)) }
            let runID = requestedRun.flatMap { $0.count <= 80 && $0.range(of: "^[A-Za-z0-9-]+$", options: .regularExpression) != nil ? $0 : nil } ?? token
            let keyRoot = root.appendingPathComponent("synthetic-key-provision-\(token)", isDirectory: true)
            let synthetic = DocumentAssetCrypto(service: "com.motovault.synthetic.\(token)", rootOverride: keyRoot)
            let result = IosStorageProbeKt.runIosStorageProbe(provider: synthetic, invalidateSyntheticKey: { _ = synthetic.deleteSyntheticProbeKey() }, cleanupTemporaryCopies: { DocumentAssetCrypto.clearTemporaryCopies() }) + " (syntheticKeychainStatus: \(synthetic.lastStatus), run: \(runID))"
            _ = synthetic.deleteSyntheticProbeKey()
            try? FileManager.default.removeItem(at: keyRoot)
            try? FileManager.default.createDirectory(at: caches, withIntermediateDirectories: true)
            try? result.write(to: resultURL, atomically: true, encoding: .utf8)
        }
        if ProcessInfo.processInfo.arguments.contains("--motovault-passport-fixture") {
            IosRuntimeFixturesKt.generateDebugPassportFixture()
        }
        if ProcessInfo.processInfo.arguments.contains("--motovault-runtime-probe") {
            var previous = UIScreen.main.brightness
            print("MOTOVAULT_BRIGHTNESS initial=\(previous)")
            Timer.scheduledTimer(withTimeInterval: 0.25, repeats: true) { _ in
                let current = UIScreen.main.brightness
                if current != previous {
                    print("MOTOVAULT_BRIGHTNESS value=\(current) state=\(UIApplication.shared.applicationState.rawValue)")
                    previous = current
                }
            }
        }
        if ProcessInfo.processInfo.arguments.contains("--motovault-notification-probe") {
            let center = UNUserNotificationCenter.current()
            center.getNotificationSettings { settings in
                guard settings.authorizationStatus == .authorized else {
                    print("MOTOVAULT_NOTIFICATION probe skipped: owner has not enabled reminders")
                    return
                }
                let content = UNMutableNotificationContent()
                content.title = "MotoVault synthetic delivery check"
                content.body = "TEST ONLY. This checks native delivery, not an owner expiry."
                content.sound = .default
                let request = UNNotificationRequest(identifier: "motovault.debug.delivery-probe", content: content, trigger: UNTimeIntervalNotificationTrigger(timeInterval: 15, repeats: false))
                center.add(request) { error in print("MOTOVAULT_NOTIFICATION probe scheduled error=\(String(describing: error))") }
            }
        }
        #endif
        return true
    }

    func application(_ application: UIApplication, configurationForConnecting connectingSceneSession: UISceneSession, options: UIScene.ConnectionOptions) -> UISceneConfiguration {
        let configuration = UISceneConfiguration(name: "Default Configuration", sessionRole: connectingSceneSession.role)
        configuration.delegateClass = SceneDelegate.self
        return configuration
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        #if DEBUG
        print("MOTOVAULT_NOTIFICATION foreground id=\(notification.request.identifier)")
        #endif
        completionHandler([.banner, .list, .sound])
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        if let payload = response.notification.request.content.userInfo["motovault.reminder"] as? String {
            DispatchQueue.main.async { ReminderNavigation.shared.open(payload: payload) }
        }
        completionHandler()
    }
}

final class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?

    func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
        guard let windowScene = scene as? UIWindowScene else { return }
        let window = UIWindow(windowScene: windowScene)
        window.rootViewController = VaultHostController()
        window.makeKeyAndVisible()
        self.window = window
        if let response = connectionOptions.notificationResponse,
           let payload = response.notification.request.content.userInfo["motovault.reminder"] as? String {
            ReminderNavigation.shared.open(payload: payload)
        }
    }
}

final class VaultHostController: UIViewController {
    private var dark = false
    override var preferredStatusBarStyle: UIStatusBarStyle { dark ? .lightContent : .darkContent }

    override func viewDidLoad() {
        super.viewDidLoad()
        let compose = MainViewControllerKt.MainViewController { [weak self] dark in
            guard let self else { return }
            self.dark = dark.boolValue
            self.setNeedsStatusBarAppearanceUpdate()
        }
        addChild(compose)
        view.addSubview(compose.view)
        compose.view.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            compose.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            compose.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            compose.view.topAnchor.constraint(equalTo: view.topAnchor),
            compose.view.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
        compose.didMove(toParent: self)
        // Never override Compose's userInterfaceStyle: System must track device changes.
    }
}
