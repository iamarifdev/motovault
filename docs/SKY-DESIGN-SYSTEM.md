# Sky Design System adoption

MotoVault consumes the core source and resources from `../apks/sky-design-system` through the local `skyDesignSystem` adapter module. The supplied package is read directly and is unchanged. Another checkout can set `-PskyDesignSystemDir=/absolute/path/to/sky-design-system`.

The adapter uses MotoVault's existing Kotlin 2.1.20, Compose 1.7.0, Android Gradle Plugin 8.7.1 and Coil 3.0.0-rc01. It adds the SVG decoder at the matching Coil version and opts into the older Compose resource API. This avoids requiring the supplied package's Kotlin 2.4 / Compose 1.11 toolchain. Android, iOS ARM64, iOS ARM simulator and Intel iOS simulator are declared; only Android and the ARM simulator framework were verified here.

## Applied UI

- Sky semantic neutral colours, with MotoVault's accessible bike-specific action and status colours.
- Sky typography roles in shared components, using Anek Bangla for Bangla content.
- Sky primary buttons through the existing app helper. Fields use Sky typography, geometry and colours with app-owned accessible labels and a visible focus border.
- Sky gradient headings and menu radius for shared panels.
- App-owned bottom navigation using Sky's supplied home/folder/clock SVGs, marker geometry and typography. Each destination has an equal-width 80-point content area at standard text size, including a 24-point Bangla label slot that grows with font scale. The app shell supplies the bottom safe area once, using the same surface colour.
- Sky page transition timings for the three destinations.
- App-owned sheet presentation inside native dialogs, preserving native Back dismissal and the app's existing form ownership. Sheets use Sky handles, corners and entry/exit motion. Short popups wrap their content; long forms scroll within the current viewport minus its top safe area and toolbar gap. The surface includes its bottom safe-area padding, so there is no exposed strip beneath it. The title and close action remain outside the scrolling body.

The existing storage, form validation, app navigation decisions, native exports and permission adapters remain app-owned. Specialized document displays, inspection mode, bike imagery, onboarding layouts and custom domain cards keep their existing structure; this is shared design-system adoption, not a pixel-for-pixel copy of the banking catalogue. The local sheet adapter handles native dismissal, Sky motion timings and the scroll viewport. It avoids nesting a second full-screen overlay host inside a native dialog. Bottom and horizontal safe areas are applied inside the sheet surface once; the dialog container handles keyboard space.

The core module includes its original asset pack because it is read directly from the provided package. Original rights/provenance remain as documented by that package. MotoVault does not mount bank-specific dashboard artwork or demo authentication flows. Optional `motion-lottie` is not included.

## Verification

Use the native ARM JDK 17:

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
rtk run ./gradlew :composeApp:testDebugUnitTest :composeApp:assembleDebug :composeApp:linkDebugFrameworkIosSimulatorArm64 --console=plain
```

Android/shared tests: 114 tests, zero failures, errors or skipped tests. Android debug APK assembled. iOS ARM simulator framework linked. Existing deprecation and Kotlin expect/actual warnings remain.

APK: `composeApp/build/outputs/apk/debug/composeApp-debug.apk`.

The initial adoption only verified compilation. The subsequent iPhone 18 Pro Max simulator audit found and corrected a screen-overlap regression: `AnimatedContent` supplies a box scope, so destination content needs its own vertical column and scrolling. Sheets now keep their header outside the scrolling body, paired actions share a minimum height, and gauge labels reserve two lines.

Focused iPhone simulator checks cover document selection/manual-entry layouts, compact popups, scrolling sheet headers and navigation geometry. The broader walkthrough did not complete in XCTest. Physical-device and Android runtime UI verification remain separate gates.
