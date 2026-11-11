# P06 design and copy review

Reference: active `design/app-ui-v3`, extended in this P06 working change from
`f2ca12f`. Prototype captures use demo records and never change owner storage.

- History keeps its existing expandable timeline and uses period/category
  controls to filter the summary and records together. The separate cost
  details sheet preserves the selected period/category in native presentation.
- Expense entry uses the established sheet, date field/chips and numeric
  keypad. Parking, toll and custom are stable category codes. Fuel, service
  and renewal retain their established dedicated flows.
- Budgets and optional maintenance allowance are visibly planning amounts,
  separate from recorded spending. Empty budget fields remove those plans.
- Cost coverage names actual reading dates and kilometres. The ratio excludes
  starting-day costs and is suppressed without sufficient mileage. Completed,
  open and broken fuel cycles have separate explanations.
- The first price is explicitly confirmed, a remembered confirmation has an
  effective date, and missed fills can be recorded without fabricating economy.
- Shared English/Bangla copy is reviewed together in `costs.xml`; new prototype
  copy is in its bilingual resource JSON. Native amount display retains poisha,
  supports independent language/digit preferences and uses the existing theme,
  typography, reduced motion and accessible text scaling.

Browser walkthrough observed a ৳12.50 parking entry change the demo recent
total from ৳1810 to ৳1822.50. A ৳2000 budget appeared in details without adding
to spending. Category totals showed fuel ৳1810 and parking ৳12.50. The cost card
also showed the 450 km recorded interval and its ratio coverage rule.

Captures: [costs](P06-prototype-costs.png), [details](P06-prototype-details.png).
The browser formats optional decimal zeroes compactly; native amounts retain
two decimal places. Prototype state is a review fixture, not persistence proof.

Native captures and reviewed results are in [Android](P06-android.md) and
[iOS](P06-ios.md). They cover four language/digit combinations, dark/1.3 text,
exact totals, expense save/Undo and failure/cancellation. The iOS images also
show cost details, category/empty states and budget changes. Common controls
retain the existing theme/tokens and scroll behavior; no motion claim is made
from still images. The iOS checkbox labels are merged with their checkable rows;
Android exercised those semantics directly. The owner explicitly moved actual
iOS VoiceOver verification to [P08-02](P06-ios-accessibility.md). Physical-device,
OS accessibility-text and motion acceptance remain open release checks.
