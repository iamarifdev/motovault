# MotoVault visual system review

The visual foundation has recognizable motorcycle identity and compatible light/dark palettes, but the English-first presentation does not yet fit the primary audience: native Bangladeshi riders. Easy, modern Bangla must lead the navigation, tasks, explanations and errors; English is secondary. Refine the visual system around Bangla reading while redesigning the friction-heavy task flows identified in the earlier audit.

This judgment combines current source with the historical screenshots already inspected. It is not fresh rendered-device verification. Proposed sizes below are design starting points, not compliance claims or tested specifications.

## Design concepts

Three concepts operate together: a personalized motorcycle showroom, an ownership dashboard, and a physical document wallet. They fit the product, but compete for priority.

The showroom concept is strongest in onboarding. Real photos and manufacturer identity help recognition and emotional attachment. On the daily Garage screen, the same emphasis becomes less useful: identity and a 190dp photo precede metrics and actions (MotoVault.kt:206-243,265). Keep the emotional connection, but compact it after setup.

The dashboard concept communicates control through odometer, fuel economy and circular maintenance gauges. The gauges look precise but represent schedule estimates. Use labels and explanations that make that distinction obvious (MotoVault.kt:220-239,277-289).

The wallet concept makes documents feel tangible and personal. The negative 36dp card spacing and 80dp secondary previews hide detail (DocumentWallet.kt:27-38). A readable list is better for expiry scanning, duplicates and urgent retrieval. The metaphor can remain in a preview or detail screen.

Judgment: good concepts individually; insufficient discipline about which concept serves each moment. A first visit can emphasize ownership identity. A repeated visit should emphasize the next useful action. Document retrieval should emphasize legibility and speed.

## Components

| Component | What works | What needs refinement |
|---|---|---|
| Brand/model tiles | Recognizable logos, real photos, visible selection, search/manual fallback | Verify consistent logo optical size and long model names across brands. Do not make one manufacturer's logo appear selected simply because it is larger. |
| Primary buttons | Shared full-width shape, 52dp minimum height, clear visual emphasis | Shared label is only 13sp. Make key action text easier to read; explain disabled states nearby. |
| Form fields | Shared outlined fields and 12dp corners create consistency | Standard forms and custom document fields use different construction. Unify labels, focus, errors, required markers and keyboard behavior. |
| Panels/cards | Calm borders and rounded surfaces suit a private ownership tool | Too much information is enclosed, making cards compete. Reserve strong containers for meaningful grouping or interaction. |
| Gauges | Quick comparison and clear status colors | Percentage precision can overstate knowledge. Small status text weakens the explanation. Use accessible text equivalents and consider simpler due-distance/date summaries. |
| Document cards | Attractive wallet identity and original previews | Overlap hides content and may obscure urgency. Add a compact readable mode with title, expiry and status. |
| Navigation | Three labelled destinations are easy to remember | Document-flow navigation visually resembles the main bar but has different behavior. Use explicit flow navigation instead. |
| Dialogs | Scrollable, keyboard-inset aware, clear close control | Long setup/log forms in modal containers feel dense. Use full-screen forms when the task warrants it. |
| Small icon actions | Keep layouts visually light | Custom 36dp plus action needs meaning, accessible labeling and verified effective target size. Prefer an explicit Add document affordance. |

Evidence: Onboarding.kt:165-197; MotoVault.kt:150-153,277-345; DocumentWallet.kt:23-54; DocumentFlow.kt:59,237-241.

## Typography

DM Sans and Barlow Condensed suit the English/Latin portion of the interface, but they are not sufficient evidence of a suitable Bangla typography system. Choose and test a Bangla-capable body family first, with intentional Latin support or pairing. Keep the condensed face only for Latin motorcycle model names if it remains harmonious. Do not rely on accidental system fallback for Bangla (MotoVault.kt:117-123,208).

The problem is scale and role consistency, not the choice of families. The source includes 8,9,10,11,12,13,14,15,16,18,21,22,23,24,25,28,29sp and a conditional 38sp hero. Many roles are specified directly rather than drawn from a small semantic scale. Important supporting text is 9-12sp, while the motorcycle title reaches 38sp.

Examples: Change bike and photo attribution are 9sp; gauge state is 10sp; units and secondary links are 11sp; primary button text is 13sp (MotoVault.kt:211,218,270-271,289,303,311,323-329). This creates an attractive editorial hierarchy but can be uncomfortable outdoors or for older riders.

Recommended starting roles:

| Role | Starting size | Treatment |
|---|---:|---|
| Brand/model display | 28-32sp | Condensed face, used sparingly |
| Screen title | 24-28sp | DM Sans bold/semibold |
| Section title | 18-20sp | Semibold |
| Body/form input | 16sp | Regular, comfortable line height |
| Button/action | 14-16sp | Medium/semibold |
| Supporting metadata | 12-14sp | Regular; keep critical facts near 14sp |
| Decorative eyebrow | 11-12sp | Short uppercase only |

Do not enlarge everything equally. Reduce the hero's dominance while improving functional labels. Keep the large numbers for odometer and useful metrics. Bangla localization is a primary requirement, not a later possibility. The uppercase 9sp eyebrow with 1.6sp tracking is a Latin styling choice; replace it with normal Bangla text at a comfortable size and normal tracking (MotoVault.kt:311). Test conjuncts, vowel marks, mixed Bangla/Latin model names, font scaling and narrow screens. Start body text near 16-18sp with generous line height, then choose final metrics from rendered Bangla rather than copying Latin measurements.

## Spacing and layout

The app looks spacious at the screen level but dense within supporting information. Source padding/spacing includes 4,5,6,7,8,9,10,12,13,14,15,16,18,20,22,24,32,86dp. Many small variants make future consistency difficult. A few values are legitimate special cases: icon strokes, safe-area clearance and overlapping cards should not be judged as ordinary layout spacing.

Main screen horizontal padding is 22dp; content gaps are 20dp (MotoVault.kt:138). Dialogs use 16dp outer padding, 22dp inner padding and 18dp gaps (:340). These values are individually reasonable, but not a clearly shared rhythm.

Use a small layout scale such as 4/8/12/16/24/32dp. Define screen gutter, section gap, card inset and field gap by role. This is a maintenance and consistency improvement, not a rule that every dimension must be a multiple of four.

More important than rounding values: place the useful content earlier, group related labels tightly, separate unrelated sections, and avoid large gaps that make actions appear disconnected. Verify rather than assume that the same gutter works on every supported width.

## Style, shapes and imagery

The overall style is restrained, slightly editorial and brand-driven. Dark charcoal, rounded outlined cards, real product imagery and condensed headings fit a motorcycle ownership app. Light mode's warm canvas avoids a sterile all-white appearance. These decisions should be preserved.

Corner radii include 12dp buttons/fields, 14dp metrics, 16dp photographs, 17dp wallet cards and 22dp dialogs (MotoVault.kt:214,265,323-340; DocumentWallet.kt:30). This does not inherently look wrong; a clearer small/medium/large radius family would make the system easier to extend.

Photographs have strong recognition value. Large light photo backgrounds on a dark screen also become the brightest object, pulling attention away from maintenance. Use a smaller hero or a more integrated image treatment after setup. Keep attribution accessible without making it compete with daily actions.

Avoid adding more gradients, shadows, dashboard rings or decorative badges. Current identity is sufficiently distinctive. The next improvement should come from hierarchy and consistency.

## Color combinations

The palette construction is one of the strongest parts of the design (Appearance.kt:32-48). Neutrals remain stable while the brand accent changes. Dark mode uses brighter accents and light mode deeper accents, which is more appropriate than applying the same brand color everywhere.

| Role | Light | Dark | Judgment |
|---|---|---|---|
| Canvas | #F5F4EF | #101215 | Warm light background and calm dark charcoal work well. |
| Surface | #FFFFFF | #171A1E | Clear separation without heavy shadows. |
| Raised surface | #E9ECF0 | #22262C | Useful secondary grouping; check actual component boundaries. |
| Primary text | #181C22 | #F1F2F3 | Strong readability on the intended surfaces. |
| Secondary text | #535E6B | #9DA5AF | Good selected contrast pairs; size is the larger concern. |
| Healthy | #236D4F | #7BD3AB | Appropriate semantic green. |
| Warning | #805817 | #EFC277 | Distinct amber/brown semantic warning. |
| Error | #AD2938 | #FF929A | Clear semantic red/pink. |

Yamaha blue, CFMOTO teal and Royal Enfield gold are compatible with the neutrals. The red-family Honda/TVS/Hero themes require particular care: brand red and error red can communicate different things while looking related. KTM orange and warning amber have a similar semantic risk. Keep brand color for selection and actions; use text/icons and a distinct treatment for urgency. Color alone should never identify validity or an error.

Selected source-based contrast calculations from the audit: primary text on white 17.10:1; muted text on light canvas 5.99:1; muted on light raised 5.57:1; muted on dark raised 6.10:1. KTM button pairs were 5.82:1 and 7.14:1. These are good selected results, not proof that all themes/states pass. Border contrast, disabled states, focus, overlays and every brand/status pair still need rendered verification.

One concrete semantic inconsistency: the maintenance priority panel uses warning decoration even when Healthy (MotoVault.kt:231-238). Fix this before adjusting hues for personal taste. Document type markers also introduce blue/green/gold that can resemble semantic status colors (DocumentWallet.kt:29). Keep type indicators visually distinct from validity badges.

## Consistency across the whole app

The core app shares a recognizable visual system. The less consistent areas are standard Material forms versus custom document forms, compact functional typography versus oversized editorial display, and normal tabs versus document-flow pseudo-tabs.

Make one specification for each recurring role: heading, supporting label, field, required/error message, primary/secondary action, card, status badge, modal and navigation. Include light/dark, selected, disabled, loading, focus and error behavior. Do not introduce a large abstract component framework just to clean up a small number of values.

## Priority order

1. Make easy, modern Bangla the default and establish Bangla-first typography; English remains available as a secondary language.
2. Increase functional readability and reduce hero dominance.
3. Unify field/button/validation patterns across ownership and document flows.
4. Make documents and urgency easier to scan.
5. Establish semantic type, spacing and radius roles, and audit every brand palette in both modes.

The colors do not call for wholesale replacement. The font system must explicitly support Bangla, and the language, layout priorities, small labels and component behavior require refinement. A consistent Bangla-first system would preserve identity while making the app feel familiar and useful to its intended audience.
