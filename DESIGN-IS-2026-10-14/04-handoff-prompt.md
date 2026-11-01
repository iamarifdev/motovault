```text
/make-plan Redesign MotoVault's task flows and information priority for native Bangladeshi riders. Easy, modern Bangla is the primary default UI language; English is secondary. Current English-first design received a provisional Rams score of 16/30, with weak usefulness, understandability and thoroughness.

Audience requirements: establish a tested Bangla-capable primary font, comfortable Bangla sizing/line height and normal tracking; fully localize owned labels, explanations, errors, notifications, export and recovery; provide a persistent বাংলা / English switch. Use date selection with readable Bangla display and accept both Bangla and Latin digits in numeric input while preserving official identifiers and originals. Keep manufacturer/model names intact. Test practical vocabulary such as আমার বাইক, কাগজপত্র, মিটারের রিডিং and সার্ভিস with actual riders. Avoid duplicating English beneath every Bangla label. Verify Bengali OCR before promising it. Brand palettes survive; existing Latin font pairing is not a complete Bangla typography solution.

Verdict:
REDESIGN the task flows and information priority while preserving MotoVault's visual identity: the provisional 16/30 audit falls below the skill's refinement threshold because routine logging, correction, document navigation and recovery impose avoidable effort.

Why redesign: these are structural journey problems, not merely cosmetic inconsistencies. The review inspected current native source and historical screenshots; no live app interaction or user study was completed. Native resource and innovation scores remain provisional.

Primary user: Bangladesh motorcycle owners. Primary task: see what needs attention, quickly record ownership activity and safely retrieve documents. Stack: existing Compose Multiplatform Android/iOS app. Accessibility floor: labelled controls, usable large text, screen-reader alternatives, appropriate touch targets and tested contrast.

Preserve:
- Three labelled tabs (MotoVault.kt:65,150-153).
- Brand palettes and theme choices (Appearance.kt:18-48,66-87).
- Searchable real-photo catalogue (Onboarding.kt:165-197).
- Owner OCR confirmation, originals access and honest inspection limits (DocumentFlow.kt:253-268,302,318).
- Opt-in reminders and honest owner-entered export limitations (platform RenewalReminders.kt; PassportExportUi.kt:24,43-54).

Discard:
- Mandatory known maintenance baseline before first value (Onboarding.kt:110-145), weakening usefulness.
- Cross-screen odometer and correction detours (VaultState.kt:88,106-111), weakening usefulness.
- Navigation-looking controls that dismiss/discard document drafts (DocumentFlow.kt:59,115-116), weakening understandability.
- Normal onboarding after restore failure (VaultSession.kt:38-47; MotoVault.kt:128-136), weakening thoroughness.

Top moves:
1. Useful (#2): shorten first use and everyday logging, allow unknown service history, and combine service mileage with odometer updates. Evidence: E1/E3, Onboarding.kt:110-145; VaultState.kt:88.
2. Thorough (#8): provide explicit restore recovery and direct record correction before adding features. Evidence: E3/E7, VaultSession.kt:38-47; OwnershipForms.kt:155-167; VaultState.kt:106-111.
3. Understandable (#4): replace document-flow pseudo-navigation with explicit exit and draft protection; add date selection and field-level validation. Evidence: E3/E5, DocumentFlow.kt:59,115-116,259-270.
4. Unobtrusive (#5): put urgent care and quick logging ahead of the large motorcycle photo; make documents scannable without opening every card. Evidence: E2/E5, MotoVault.kt:206-243; DocumentWallet.kt:27-38.
5. Honest and thorough (#6/#8): explain schedule estimates, soften uncertain camera warnings, and make custom fields, add and crop actions accessible. Evidence: E2/E6/E8, MotoVault.kt:220-239; DocumentCapture.kt:53; DocumentFlow.kt:175-176,219-241.

Source citations resolve under composeApp/src/commonMain/kotlin/com/motovault/app/ui/, except VaultSession.kt under storage/. E1-E8 identify evidence groups, with sufficient concrete source citations above.

Plan deliverables: new task-first information hierarchy; low-fidelity current/proposed flows; per-fix target files and exact verification; empty/loading/error/success/focus/disabled checklist; preserved-data migration and correction rules; cutover criteria; physical white-paper/HEIC checks; TalkBack/VoiceOver, narrow-phone and large-text tests. Validate camera risks before assuming reproduction. Retention and speed need actual rider testing.

Out of scope: cloud/social features, streaks, unrelated storage rewrites, wholesale brand restyling and implementation before visual/flow approval. Do not carry the old friction into a new skin or keep duplicate designs indefinitely.
```
