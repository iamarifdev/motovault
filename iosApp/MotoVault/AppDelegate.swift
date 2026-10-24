import UIKit
import UserNotifications
import ComposeApp

@main
final class AppDelegate: UIResponder, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // The notification center holds its delegate weakly. UIApplication retains this object.
        UNUserNotificationCenter.current().delegate = self
        #if DEBUG
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
