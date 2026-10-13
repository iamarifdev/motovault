# Native long Unicode passport fixture

Executed on 14 October 2026 in the existing iPhone 17 Pro iOS 26.0 simulator, after an arm64 Debug host build and app update. No simulator erase, uninstall or vault reset occurred. The explicit `--motovault-passport-fixture` DEBUG hook constructs an isolated passport and never reads or writes the owner vault.

Native console reported 70 service records and digest `57cd6742b51f7414d80d18144484b75f09b240306ec063c731e7cd718da8f552`. The fixture includes a 100-line note with Bengali and emoji. The paired files are `12-native-long-unicode-passport.pdf` and `.json`.

`pdfinfo` reports iOS 26.0 Quartz PDFContext, 16 A4 pages and 114808 bytes. Text extraction retains all 70 service records, `Long note 100`, Bengali and emoji. Page 8 was visually inspected: Bengali and emoji render correctly with legible spacing. Page 16 contains the QR image and footer, not an empty trailing page. The PNG evidence preserves both observations.

The native renderer calls `verifyPassportBundle` before generating the PDF. This proves the valid native helper path; it does not establish the Files-picker verification UI or wrong independent digest UI.

The first restricted Xcode attempt could not see the simulator destination. Elevated read-only simctl located the existing shutdown simulator; elevated Xcode built successfully, then the simulator was booted and the app updated while preserving data. CUA initially hung while simulator services were unavailable; after reset and boot, a bounded native-app read succeeded.
