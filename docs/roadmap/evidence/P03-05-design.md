# P03-05: Cross-Platform UI Design Parity and Local-Beta Experience

- Task and acceptance criteria covered: P03-05 (Cross-platform visual parity, review steps, conflict warnings, native export/picker behavior, and design contract compliance).
- Date/time and timezone: 2026-10-27 06:27:00 (Asia/Dhaka).
- Author/reviewer: Antigravity / Design System & UX Audit.
- Branch, commit and relevant uncommitted changes: `main`, commit `04bfaf5` + working changes.
- App build/version and enabled feature flags: 1.0.0, Room schema version 5.
- Platform, OS, emulator/simulator or physical-device model: Android ARM64, iOS 27.0 Simulator.
- Evidence kind: design.

## Cross-Platform UI Consistency Audit

1. **Sheet Visual Structure & Geometry**:
   - Both Android (`p03_android_backup_sheet.png`) and iOS (`p03_ios_backup_sheet_create.png`) display identical layout hierarchy:
     - Sheet grab handle and close button (`fi_x.svg`).
     - Sheet header: "ব্যাকআপ ও রিস্টোর".
     - Dual-tab segmented control: "ব্যাকআপ তৈরি" / "রিস্টোর".
     - Security info banner with shield icon (`fi_shield.svg`) and light brand tint.
     - Live inventory card detailing active bike model, current odometer, ledger record count, and document count.
     - Password input fields with 1.dp border stroke, rounded corners (14.dp), and reveal toggle.
     - Urgent security warning banner highlighting unrecoverable password policy.

2. **Restore Review & Conflict Banner**:
   - Both Android (`p03_android_restore_sheet.png`) and iOS (`p03_ios_backup_sheet_restore.png`) provide:
     - Clear informative callout explaining the restore process.
     - Prominent dashed archive picker container with cloud upload icon.
     - Side-by-side conflict comparison when active bike or odometer differs from the incoming archive.
     - Destructive overwrite warning banner in bold warning colors.
     - Explicit confirmation switch required before unlock.

3. **Design System & Token Adherence**:
   - Palette: Pure adherence to `SurfaceColor`, `BorderColor`, `BrandColor`, `BrandTintColor`, `TextPrimary`, and `TextSecondary`.
   - Typography: Localized numerals formatted using `f.d()`. Natural Bengali phrasing without transliteration quirks.
   - Reduced Motion & Accessibility: Respects `isReducedMotion` across spring animations and haptic triggers.
