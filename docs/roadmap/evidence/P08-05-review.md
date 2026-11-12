# P08-05 localization and text-debt review

Date: 12 November 2026 (Asia/Dhaka). Scope: review of the P08-05 localization
changes and the runtime language-switch evidence. This review covers the
gap-audit remediation status, the copy style guide, the responsibility baseline,
persisted-data compatibility, demo/alias data separation, and a visual review of
the captured iOS/Android language switch.

Status: **agent review complete; named human copy sign-off is outstanding.** Per
the delivery workflow the task remains `in_review` until a named human reviews
the Bangla/English strings listed below.

## Sources reviewed

- [Localization style](../../engineering/localization-style.md) (voice, preferred
  terms, numbers/units, punctuation, review checklist).
- [Localization gap audit](../../engineering/localization-gap.md), including the
  current-status section appended for P08-05.
- [Architecture contract](../ARCHITECTURE-CONTRACT.md) localization/data rules
  and [design contract](../DESIGN-CONTRACT.md) typography/language rules.
- `tools/ui-literal-baseline.json`, `tools/ui_literal_gate.py`,
  `tools/bangla_copy_lint.py`, `tools/responsibility_gate.py`.
- Compatibility fixtures (`docs/roadmap/fixtures/compatibility/`) and
  `P00CompatibilityTest`.
- Runtime evidence: [Android notes](p08/localization/android/android-localization-notes.md)
  and [iOS notes](p08/localization/ios/ios-localization-notes.md).

## Gap-audit remediation status

| Gap class | Status in this slice | Evidence |
| --- | --- | --- |
| Direct literals at audited UI sinks | 9 remaining occurrences, all documented data-only glyphs with justification; 0 copy-debt entries | `ui_literal_gate.py`, baseline JSON |
| Copy compositions previously built from literals | 9 moved to shared format resources | resource/code diff, gate output |
| Native UI error copy reaching the screen | localized through shared resources or localized display boundaries | native capture/picker/passport changes, gates |
| Bangla copy style | copy lint passes; changed strings listed for human review | `bangla_copy_lint.py` |
| Large-file/function ratchet | only the 2 baseline test-support files; no production file over 400 lines | `responsibility_gate.py` |
| Persisted display labels | no new persisted translated text; stable IDs/typed presentation; compatibility tests pass | `P00CompatibilityTest`, no storage changes |
| Demo/input/identifier data | documented separately, not counted as UI copy | see below |

## Runtime language-switch review

- Android: same-session BN→EN→BN switches without relaunch (same process ID),
  four tabs, seven sheets, notification-permission card and the localized
  save-failure error in both languages; Garage after switch-back is byte-identical
  to the pre-switch capture. Native probe PASS 37 checks, including
  `native-reminder-language-and-digit-preferences` and
  `native-compose-plural-and-fallback`.
- iOS: `P08LocalizationTests` 3/3 passed; in-app BN→EN→BN switching without
  restart, four tabs, seven sheets, notification-permission card; all 23
  screenshots reviewed. The previous language's tab labels disappear from the
  accessibility tree after each switch. Native storage probe PASS 22 checks,
  including the EN/BN reminder localisation and plural/fallback checks.
- Visual result: sheet titles, bodies, action labels, keypad hints, empty states,
  error copy and the permission card are fully in the selected language in the
  captured states; no clipped or lost copy was seen at the captured 1.0 text
  scale. The P08-01 1.3-scale findings (Records action clipping, Profile theme
  segment) are separate release-review items, unchanged by this task.

## Strings changed or added in this slice (human review list)

The reviewer should check voice, terminology and naturalness against the style
guide. Neutral format-only strings are listed separately because they carry no
words.

| Key | English | Bangla |
| --- | --- | --- |
| `cost_empty_amount` (changed) | Not entered | দেওয়া হয়নি |
| `quickadd_paste_title` (changed) | Add by typing or pasting | লিখে বা পেস্ট করে যোগ করুন (unchanged) |
| `quickadd_paste_desc` (changed) | From a mechanic, bKash or Nagad message | মেকানিক, বিকাশ বা নগদের মেসেজ (unchanged) |
| `settings_reminder_settings` (changed) | Reminder settings | রিমাইন্ডার সেটিংস (unchanged) |
| `vault_reminder_action` (changed) | Reminder settings | রিমাইন্ডার সেটিংস (unchanged) |
| `capture_open_viewer_failed` (new) | Could not open a viewer. You can still view the saved pages here. | ভিউয়ার খোলা যায়নি। সেভ করা পৃষ্ঠাগুলো এখানেই দেখতে পারবেন। |
| `capture_cleanup_failed` (new) | Could not remove the temporary copy. Close the viewer and try again. | সাময়িক কপি মুছে ফেলা যায়নি। ভিউয়ার বন্ধ করে আবার চেষ্টা করুন। |
| `capture_import_failed` (new) | Could not add this file. Use a JPG, PNG or WebP image or a PDF and try again. | ফাইলটি যোগ করা যায়নি। জেপিজি, পিএনজি বা ওয়েবপি ছবি অথবা পিডিএফ দিয়ে আবার চেষ্টা করুন। |
| `capture_camera_failed` (new) | Could not take the photo. Check focus and try again. | ছবি তোলা যায়নি। ফোকাস দেখে আবার চেষ্টা করুন। |
| `capture_capture_save_failed` (new) | Could not save this photo. Try again. | ছবিটি সেভ করা যায়নি। আবার চেষ্টা করুন। |
| `pick_invalid_type` (new) | Choose a JPEG, PNG or WebP photo. | জেপিজি, পিএনজি বা ওয়েবপি ছবি বেছে নিন। |
| `pick_too_large` (new) | Each photo must be 1 MB or smaller. | প্রতিটি ছবি ১ এমবি বা তার ছোট হতে হবে। |
| `pick_empty` (new) | The photo is empty. | ছবিটি খালি। |
| `pick_read_failed` (new) | Could not read this photo. | ছবিটি পড়া যায়নি। |
| `pick_open_failed` (new) | Could not open the photo picker. | ছবি বাছাই করা যায়নি। |
| `passport_open_failed` (new) | Could not open this PDF. | PDF খোলা যায়নি। |
| `passport_share_failed` (new) | Could not open sharing. Use Save to Files in the share sheet to keep both files. | শেয়ার করা যায়নি। দুটি ফাইল রাখতে শেয়ার শিটে Save to Files ব্যবহার করুন। |

Additional copy display change to review: the onboarding progress indicator now
uses the existing `onboarding_step` resource (`Step %1$s of %2$s` /
`ধাপ %1$s / %2$s`) instead of a bare `1 / 3` composition.

Neutral format resources added (placeholders/separators only, no translatable
words): `wallet_next_line`, `schedule_source_line`,
`schedule_applicability_value`, `schedule_interval_summary`,
`schedule_odometer_confirmed`, `odometer_bump_chip`, `history_discount_amount`,
`service_odometer_value`.

## Demo, alias and identifier data (not localized copy)

- `ui/VaultCreation.kt` `seedData()` demo records keep Bangla display names
  (`চেইন`, `ড্রাইভিং লাইসেন্স`, `ট্যাক্স টোকেন`, workshop/pump names). They are
  demo data, are owner-editable, and are not resource strings; the English UI
  therefore shows them unchanged in seeded demo states.
- `ui/Catalog.kt` and the smart-parse alias tables are input/matching data, not
  display copy; labels are resolved by ID at presentation.
- Storage identifiers (record IDs, catalogue IDs, document kinds, asset IDs) stay
  stable and language-independent.
- None of the above is counted as localized UI copy; the P08-05 acceptance item
  for demo/fixture/alias separation is satisfied by this classification.

## Residual observations (not blockers for this task)

1. The expense date field renders `2026-11-12` in Latin digits under the Bengali
   number style. Recorded in P08-01; needs a release decision.
2. English UI in seeded demo states shows Bangla demo record names (above). A
   release-candidate decision is needed on whether demo data is shown at all.
3. The English Profile reminder row copy was "Permission, time and later"; it is
   now "Reminder settings" in both keys (included in the review list). Bangla was
   already correct.
4. System permission alerts, document picker and share sheet follow the OS
   locale, not the in-app language; the app-owned pre-permission copy is
   localized and captured.

## Human sign-off

- **Reviewer: Ariful Islam (repository owner), 12 November 2026.**
- Decision: **approved as written** — (a) the changed English strings,
  (b) the new/updated Bangla error and permission copy, and (c) the
  `onboarding_step` wording in the table above.
- Recorded from the owner's direct reply on 12 November 2026 ("Approve as
  written"). The agent's screenshot/copy review remains as supporting evidence;
  the named-human gate is now satisfied.
- P08-05 can move to `done`; the runtime and static evidence is complete.
