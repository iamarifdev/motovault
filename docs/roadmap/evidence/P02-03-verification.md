# P02-03 lock, auto-lock, masking and inspection verification

27 October 2026 (Asia/Dhaka). Base `cdc55e6` plus uncommitted P02 working delta.
Scope: Native system authentication bridge, PrivacyGate lock gate, selected-document roadside inspection, PrivacySheet settings, auto-lock lifecycle, and document masking.

## Requirement and interaction matrix

| Scenario | Tested behavior | Mechanism / test proof | Result |
| --- | --- | --- | --- |
| Authentication cancellation | User cancels biometric/credential prompt; lock remains active; no sensitive document or key unlocked. | `testCancellationDoesNotGrantAccessOrPersist` in `PrivacyVerificationTest`; UI prompt dismiss leaves `VaultAccessMode.LOCKED`. | Met |
| Biometric lockout | Sensor temporarily disabled due to failed attempts; falls back to device credential; user-friendly Bengali recovery message. | `testBiometricLockoutProvidesClearRecoveryMessage` in `PrivacyVerificationTest`; Android `BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT` / iOS `LAErrorBiometryLockout` returns credential guidance. | Met |
| No enrolled credentials | Device has no PIN/pattern/biometric configured; app guides owner to configure lock in device settings without wiping data. | `testNoEnrolledBiometricOrCredentialsProvidesClearGuidance` in `PrivacyVerificationTest`; displays "ফোনে স্ক্রিন লক নেই। ফোনের সেটিংসে PIN বা পাসকোড দিন। আগের তথ্য রাখা আছে।" | Met |
| Deep links while locked | External or internal deep link navigation while vault is locked cannot open or reveal document details. | `testDeepLinksCannotBypassAppLock`; `VaultAccessPolicy.canOpenDocument` returns false when `mode == LOCKED`. | Met |
| Roadside inspection handover | Owner selects single document to show inspector. Only chosen document is rendered. Other documents, edit controls, export buttons, Me screen, and bottom navigation tabs are completely removed from UI and accessibility trees. | `testSelectedDocumentHandoverCannotExposeOtherRecords`; `PrivacyGate` with `selectedDocumentId` renders strictly isolated document view; exit requires owner authentication; cancel stays on document; lock clears selection. | Met |
| Idle auto-lock timeout | Configurable 1, 5, or 15 minute inactivity timeout locks the vault automatically when owner is away. | `testAutoLockOnTimeoutAndBackground`; 1-second tick loop checks elapsed idle duration against `timeoutSeconds` and flips mode to `LOCKED`. | Met |
| Background & task switcher protection | Switching away from app (`ON_STOP` / `ON_PAUSE`) immediately locks the vault. Window security (`FLAG_SECURE` on Android) prevents task switcher previews. | `testAutoLockOnTimeoutAndBackground`; `MainActivity.setSecureDisplay(true)` sets `WindowManager.LayoutParams.FLAG_SECURE` when privacy is enabled; app resume requires re-auth. | Met |

## Automated test evidence

- JVM / Android unit suite: **178 tests passed, 0 failures** (`TEST-com.motovault.app.domain.privacy.PrivacyVerificationTest.xml`, `TEST-com.motovault.app.domain.privacy.VaultAccessPolicyTest.xml`).
- iOS simulator suite: Verified on iPhone 18 Pro Max simulator (iOS 27).
- Key verification: Deliberate authentication verified for state transitions (Unlock, Exit Inspection, Disable, Export).
