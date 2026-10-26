// Synthetic interoperability fixture only. Never use this key or nonce in the app.
import Foundation
import CryptoKit

let key = SymmetricKey(data: Data(repeating: 0, count: 32))
let nonce = try AES.GCM.Nonce(data: Data(repeating: 0, count: 12))
let plaintext = Data("SYNTHETIC document original".utf8)
let aad = Data([77, 86, 65, 1, 1]) + Data("synthetic.pdf".utf8)
let box = try AES.GCM.seal(plaintext, using: key, nonce: nonce, authenticating: aad)
let combined = box.combined!
guard try AES.GCM.open(AES.GCM.SealedBox(combined: combined), using: key, authenticating: aad) == plaintext else {
    fatalError("Synthetic interoperability round trip failed")
}
let expected = "0000000000000000000000009dfe0e6905253f27446ea1bcd986f07d1c1423a545cf4d1dbfc39948ca6010f1dc310997b70af9488f32dc"
guard combined.map({ String(format: "%02x", $0) }).joined() == expected else {
    fatalError("Synthetic interoperability vector changed")
}
print("PASS: CryptoKit synthetic AES-256-GCM interoperability vector")
