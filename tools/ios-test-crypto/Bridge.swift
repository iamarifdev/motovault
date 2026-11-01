import Foundation

// C ABI used only by the standalone Kotlin simulator test executable.
// All cryptographic operations are the same Swift functions used by the app.
@_cdecl("motovault_test_backup_crypto")
public func backupCrypto(_ request: UnsafePointer<CChar>?) -> UnsafeMutablePointer<CChar>? {
    guard let request, let data = String(validatingUTF8: request)?.data(using: .utf8),
          let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
          let operation = json["operation"] as? String else { return nil }
    func bytes(_ key: String) -> Data? {
        (json[key] as? String).flatMap { Data(base64Encoded: $0) }
    }
    var result: Data?
    switch operation {
    case "sha256":
        if let payload = bytes("payload") { result = BackupCryptoPrimitives.sha256(payload) }
    case "derive":
        if let password = json["password"] as? String, let salt = bytes("salt"),
           let iterations = json["iterations"] as? Int32 {
            result = BackupCryptoPrimitives.deriveKey(passphrase: password, salt: salt, iterations: iterations)
        }
    case "encrypt":
        if let key = bytes("key"), let payload = bytes("payload"), let aad = bytes("aad") {
            result = BackupCryptoPrimitives.encrypt(key: key, plaintext: payload, aad: aad)
        }
    case "decrypt":
        if let key = bytes("key"), let payload = bytes("payload"), let aad = bytes("aad") {
            result = BackupCryptoPrimitives.decrypt(key: key, combined: payload, aad: aad)
        }
    default: break
    }
    guard let result else { return nil }
    return strdup(result.base64EncodedString())
}
