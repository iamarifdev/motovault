# P06 iOS runtime

Environment: existing iOS 27 iPhone 18 Pro Max simulator,
`50709DF0-FEA9-4B34-9DE6-6172D868875D`, isolated ad hoc signed debug bundle
`com.motovault.app.privacyprobe`. No physical iPhone or owner database was used.
Base `f2ca12f` plus the P06 change. [Manifest](P06-artifact-manifest.json) records
source and artifact SHA-256 identities.

## Native storage and real archive

The app was built with Xcode for this exact destination, then launched with
`--motovault-ledger-probe`. The debug-only entry uses a new synthetic Room path,
the production vault writer and the production-equivalent Swift crypto bridge
installed by the app delegate. [Verbatim result](P06-ios-native.txt):

```text
PASS P06 iOS: native-ledger-legacy-correction-delete-undo-room-reopen-and-encrypted-backup
```

Assertions match the Android independent fixture: 101 legacy poisha plus a
corrected/backdated 2501-poisha expense gives 2602 total poisha; November has
101. Delete/Undo commits through the writer, Room reopens with the original
expense ID and exact amount, budget and confirmed price. The real encrypted
archive/preview preserves every log and ledger preference. No test-only crypto
behavior was fabricated.

## Visual checkpoints and manual image review

The isolated fixture renders production History, expense, budget, details and
fuel components over synthetic in-memory UI state. Three focused XCTest
checkpoint methods ran, with no runner failures, on the pinned 440 x 956-point
window. The checkpoint runner checks foreground/window geometry and captures
images; it does **not** assert the rendered copy or business totals. The following
outcomes were independently reviewed in the exported screenshots:

| State | Observed |
| --- | --- |
| English/Bangla with both digit styles | Copy switches immediately; total remains ৳2250.50 and each digit style is independent. |
| Summary | 150 km, 30.0 km/L; covered ৳1750.50 / 150 = ৳11.67/km; budget ৳3000 leaves ৳749.50. |
| Details | Fuel ৳1000, renewal ৳1250, parking ৳0.50, other categories zero; recent-period label and coverage match. |
| Expense save | ৳12.50 draft becomes a parking row; total ৳2263.00, budget remaining ৳737.00. |
| Undo | Added row disappears; total returns to ৳2250.50. |
| Failed save/cancel | Draft ৳1 and localized error remain; cancelling returns to unchanged ৳2250.50. |
| Budget | Changing plan to ৳2000 leaves spending ৳2250.50 and shows ৳250.50 over budget. |
| Category filter | Fuel total ৳1000; service total zero with empty timeline; monthly budget remains all-category spending. |
| Fuel controls | Price confirmation changes its label; the following settled capture shows both confirmed-price and missed-fill checkmarks. |
| Dark / 1.3 scale | Dates wrap, essential amounts remain readable, category/action rows remain scrollable. |

Representative captures: [English/Bangla digits](p06-ios-en-bn.png),
[English/Latin digits](p06-ios-en-en.png), [Bangla/Latin digits](p06-ios-bn-en.png),
[dark/large](p06-ios-dark-large.png), [expense](p06-ios-expense.png),
[saved](p06-ios-expense-saved.png), [Undo](p06-ios-expense-undone.png),
[failed save](p06-ios-save-failure.png), [cancelled](p06-ios-cancelled.png),
[budget](p06-ios-budget-saved.png), [details](p06-ios-details.png),
[fuel filter](p06-ios-fuel-filter.png), [empty service](p06-ios-empty-service-filter.png),
[confirmed and missed fill](p06-ios-missed-fill.png).

## Accessibility limit and owner decision

The initial label-driven XCTest attempts exposed only the Compose root. Generic
selectors did not solve it; therefore those attempts are recorded as failures,
not erased or treated as product passes. The replacement checkpoint harness
uses positions inspected in settled captures, and all retained visual outcomes
above were manually checked.

The owner explicitly approved tracking iOS VoiceOver under P08 on 12 November.
[P08-02 handoff](P06-ios-accessibility.md) keeps actual VoiceOver traversal,
state announcements and focus open. Screenshots and Android semantics do not
close that gate. Physical iPhone, OS-level accessibility text, motion recordings,
release signing/distribution and publication are also not established here.
