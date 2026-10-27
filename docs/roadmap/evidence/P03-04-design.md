# P03-04: Backup and Restore UI Design & Accessibility Audit

- Task and acceptance criteria covered: P03-04 (Design review against v3 contract, typography, color tokens, layout hierarchy, focus states, large text accessibility, and natural Bangladeshi microcopy).
- Date/time and timezone: 2026-10-27 05:50:00 (Asia/Dhaka).
- Author/reviewer: Antigravity / Design System & UX Audit.
- Branch, commit and relevant uncommitted changes: `main`, commit `04bfaf5` + working changes.
- App build/version and enabled feature flags: 1.0.0, Room schema version 5.
- Platform, OS, emulator/simulator or physical-device model: iOS 27.0 Simulator, iPhone 18 Pro Max (`50709DF0-FEA9-4B34-9DE6-6172D868875D`).
- Evidence kind: design.

## Design Contract Compliance

1. **Tokens & Theming**:
   - Backgrounds adhere strictly to `SurfaceColor`, `BrandTintColor`, and `WarningSurfaceColor`.
   - Borders utilize 1.dp strokes with `BorderColor` / `BrandLineColor`.
   - Corner radiuses standardize on 14.dp and 16.dp for cards and inputs, matching the established v3 sheet paradigm.
   - Contrast ratios exceed WCAG AA guidelines for all text elements in both light and dark themes.

2. **Bangla Microcopy & Typography**:
   - Completely eliminated robotic, awkward translations.
   - Natural Bengali phrasing used across all states:
     - "ব্যাকআপ তৈরি" (Create Backup)
     - "রিস্টোর করুন" (Restore)
     - "নিরাপদ ও এনক্রিপ্টেড ব্যাকআপ" (Secure & Encrypted Backup)
     - "আপনার বাইকের রেকর্ড ও সমস্ত কাগজের ছবি একটি সুরক্ষিত ফাইলে পোর্টেবল আকারে সংরক্ষিত হবে।"
     - "জরুরি সতর্কতা: এই পাসওয়ার্ডটি মনে রাখুন। পাসওয়ার্ড ভুলে গেলে ব্যাকআপ থেকে কোনো ডেটাই পুনরুদ্ধার করা সম্ভব হবে না।"
   - Localized numerals (`১২,৩৪০`, `৮`, `৪`) render via `f.d()` based on user numeral format preference.

3. **Motion & Feedback**:
   - Tab switching triggers `HapticKind.TICK`.
   - Destructive confirmation triggers `HapticKind.WARNING`.
   - Sheets slide smoothly with `VaultMotion.sheetSpring()` and honor the user's `isReducedMotion` setting.

4. **Visual Evidence**:
   - `p03_ios_me_backup_card.png`: Profile / Me screen showing Backup & Restore card with last backup age and entry buttons.
   - `p03_ios_backup_sheet_create.png`: Create Backup sheet with summary inventory, password fields, and security notice.
   - `p03_ios_backup_sheet_restore.png`: Restore sheet with archive file picker and staged flow.
