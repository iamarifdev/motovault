# P08-01 layout-fix capture provenance

Android emulator (Pixel_10, Android 17/API 37, emulator-5554)
probe APK sha256 e60d3bf49011cea4b48aef711b849e35eb47710b954d04f4c207860968bd5564
(test APK unchanged 7f7c82fbae147c414896f928f57a9629282ed007f418f3c9de98c076701c7713)
Synthetic host: com.motovault.app.privacyprobe / FuelEditFixtureActivity
Captured 2026-11-12 23:32 Asia/Dhaka; 1.0 then in-app text size 'সবচেয়ে বড়' (1.3).

iOS 27 simulator (iPhone 18 Pro Max, 50709DF0-FEA9-4B34-9DE6-6172D868875D)
xcodebuild test -only-testing:MotoVaultUITests/P08DesignMatrixTests/testTextScaleMatrix
final run build/p08-layout-runs/layout-ios2-20261112-233540 (TEST SUCCEEDED, 6 PNGs)
app: isolated com.motovault.app.privacyprobe built from source e6c59bf + working tree.

Files: iOS        6, Android        4

SHA-256:
f17865b9aa285e44037951eccbbbb2502b2cd23e4b9e90f79937b87fb6d050fb  ios/p08-ios-tab-garage-light-bn-scale13.png
15f8f149619e19d479cea8939d50644826b65fe4ccfe654c1a6c011452485349  ios/p08-ios-tab-garage-light-en-scale13.png
df599bf9655e9f06c9816967e91d95a6a44b89c6bcf3ca5baabe91b0705e4955  ios/p08-ios-tab-profile-light-bn-scale13.png
901e8b75436d9a63e6bd189ac1da39417901bba1151c69bd2516b9a6a5c88cb1  ios/p08-ios-tab-profile-light-en-scale13.png
b6fdb16ff0d2fb02a9a466523fd4b42b638ff10bd71580eaeed93f942ba8031f  ios/p08-ios-tab-records-light-bn-scale13.png
1f2b9c636574eb611bb42bcc2d0c4cdd54b11947d7e10f3535ba636f31270e1c  ios/p08-ios-tab-records-light-en-scale13.png
ac52820fdbfc80c9d8639fd8ce63db4a417949075459556d08e69b139ae08c0b  android/layout-android-1.0-profile.png
94ba2d0bc302ec9143ceba37088174efaa8e1925cf504d5a09b0eb040c2b8d6a  android/layout-android-1.0-records.png
14a94d8891e1e782a8d85931a8ee1eb9e6abbcad7f34f5c67fea6d78cfd641e1  android/layout-android-1.3-profile.png
fc8ea818a4955969616af8fa433f04178669fe4635daf672071ca10024db0fe2  android/layout-android-1.3-records.png
