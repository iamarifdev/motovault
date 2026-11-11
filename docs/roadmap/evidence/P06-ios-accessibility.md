# P06 iOS accessibility handoff to P08-02

Owner decision, 12 November 2026: track the iOS VoiceOver check under P08.
This explicitly defers the unverified iOS assistive-technology gate; it is not
recorded as passed by P06 screenshots or Android semantics.

Environment: Compose Multiplatform 1.7.0, Kotlin 2.1.20, iOS 27 iPhone 18 Pro Max
simulator `50709DF0-FEA9-4B34-9DE6-6172D868875D`, isolated debug bundle
`com.motovault.app.privacyprobe`.

The XCTest hierarchy exposed the native windows and only
`AccessibilityElement for SemanticsNode(id=1)`, with no cost labels or controls.
Both typed and generic descendant selectors failed at their first label.
The app rendered its content correctly, and direct screenshot checkpoints drove
its controls, but those checks do not establish VoiceOver reading order, state
announcements, traversal or focus.

The existing debug host already sets `AccessibilitySyncOptions.Always(null)`.
This matches the pre-1.8 configuration described in the
[official Compose accessibility documentation](https://kotlinlang.org/docs/multiplatform/compose-ios-accessibility.html).
No toolchain migration or guessed bridge workaround was introduced in P06.

Android directly verified labeled, checkable price-confirmation rows. Shared
source merges each checkbox with its resource-backed label and checkbox role.
That evidence must not be presented as an iOS VoiceOver pass.

P08-02 must test the production cost/expense/budget/fuel screens on an iPhone
with VoiceOver: category/period selection, prices and missed-fill state, exact
amounts, errors, save/cancel/Undo, focus after sheet dismissal, large text and
reading order. Investigate the Compose 1.7/iOS 27 accessibility bridge using a
minimal reproduction or a separately reviewed compatible toolchain change if
necessary. Keep this gate open until actual native accessibility evidence exists.
