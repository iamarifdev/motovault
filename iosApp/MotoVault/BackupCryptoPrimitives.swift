import Foundation
import CryptoKit
import CommonCrypto

// Shared by the application bridge and the standalone Kotlin simulator test host.
enum BackupCryptoPrimitives {
    static func sha256(_ data: Data) -> Data { Data(SHA256.hash(data: data)) }
    static func deriveKey(passphrase: String, salt: Data, iterations: Int32) -> Data? {
        var derived = Data(repeating: 0, count: 32)
        let status = derived.withUnsafeMutableBytes { output in
            salt.withUnsafeBytes { input in
                CCKeyDerivationPBKDF(CCPBKDFAlgorithm(kCCPBKDF2), passphrase, passphrase.utf8.count,
                    input.baseAddress?.assumingMemoryBound(to: UInt8.self), salt.count,
                    CCPseudoRandomAlgorithm(kCCPRFHmacAlgSHA256), UInt32(iterations),
                    output.baseAddress?.assumingMemoryBound(to: UInt8.self), 32)
            }
        }
        return status == kCCSuccess ? derived : nil
    }

    static func encrypt(key: Data, plaintext: Data, aad: Data) -> Data? {
        try? AES.GCM.seal(plaintext, using: SymmetricKey(data: key), authenticating: aad).combined
    }

    static func decrypt(key: Data, combined: Data, aad: Data) -> Data? {
        guard let box = try? AES.GCM.SealedBox(combined: combined) else { return nil }
        return try? AES.GCM.open(box, using: SymmetricKey(data: key), authenticating: aad)
    }
}
