# MotoVault text-contrast findings

**Historical report:** these screenshots and source locations describe the pre-theme build. The shared content-color, inspection-action, and Android system-bar issues were corrected on 11 October 2026. See [appearance implementation and verification](/Users/arif/workspace/bbn/motovault/docs/THEMES.md) for the current behavior and screenshots.

Inspected on 10 October 2026 using the unchanged debug APK on the Pixel_10 Android simulator. Source review covered setup, model selection, garage, vault, history, and the shared dialogs. ADB screen navigation and capture were explicitly authorized. Only sample simulator records and this report/screenshot evidence were added; app source was not changed.

## Main finding

The redesign draws dark backgrounds but does not set the default text color for their contents. Many ordinary `Text` calls therefore render black. This is the main reason that headings and values disappear while descriptions, status labels, and buttons remain readable.

This was introduced in the redesign. The earlier compilation and domain-test checks did not verify rendered text colors.

The chain is:

1. [MotoVault.kt:123](/Users/arif/workspace/bbn/motovault/composeApp/src/commonMain/kotlin/com/motovault/app/ui/MotoVault.kt:123) configures `onSurface` and `onBackground` as white.
2. Its child is a `Box` with a manually painted background. There is no root Material `Surface` or `LocalContentColor` provider.
3. [Panel:330](/Users/arif/workspace/bbn/motovault/composeApp/src/commonMain/kotlin/com/motovault/app/ui/MotoVault.kt:330) and [Sheet:346](/Users/arif/workspace/bbn/motovault/composeApp/src/commonMain/kotlin/com/motovault/app/ui/MotoVault.kt:346) also paint backgrounds with `Column` without supplying a matching default content color. Sheet titles have explicit colors, but its body has no default color of its own.
4. Helpers such as [Heading:327](/Users/arif/workspace/bbn/motovault/composeApp/src/commonMain/kotlin/com/motovault/app/ui/MotoVault.kt:327), and many card titles and numeric values, omit an explicit text color.
5. Compose `Text` falls back to `LocalContentColor` when neither the call nor its text style supplies a color. Its default is black. `MaterialTheme` alone does not change that default. This behavior is documented in the [Text source](https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/Text.kt), [ContentColor source](https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/ContentColor.kt), and [MaterialTheme source](https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/MaterialTheme.kt).

Calculated contrast from the app’s color values:

| Text / background | Contrast |
| --- | ---: |
| Default black / dark card `#171A1E` | **1.20:1** |
| Default black / dark canvas `#101215` | **1.12:1** |
| Intended white `#F1F2F3` / dark card | **15.57:1** |
| Explicit muted gray `#8F969E` / dark card | **5.84:1** |

The gray descriptions are not the main contrast failure. They have their color explicitly assigned. Enabled buttons, text fields, menus, and navigation items generally remain readable because those Material components supply their own content colors. Normal-sized text should reach 4.5:1; large text should reach 3:1 under the [WCAG contrast guidance](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html).

## Screen-by-screen findings

| Screen / view | Finding | Evidence |
| --- | --- | --- |
| Brand setup | “What do you ride?”, “Choose a model”, and “Racing orange” are black on dark backgrounds. Wordmark, brand marks, and gray descriptions remain readable. | [Brand setup screenshot](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/contrast-audit/01-current.png) |
| Model picker | “Find the one you ride.” and each model name are black. Photos, model notes, and controls are readable. This directly makes identifying the photo cards difficult. | [Picker screenshot](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/contrast-audit/02-model-picker.png); [Onboarding.kt:151](/Users/arif/workspace/bbn/motovault/composeApp/src/commonMain/kotlin/com/motovault/app/ui/Onboarding.kt:151) |
| Ownership setup | Main heading and the example-schedule confirmation label are black. Field values are white and readable, showing that the issue is color inheritance rather than missing text or a font-loading failure. | [Upper setup](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/contrast-audit/03-ownership-setup.png), [Lower setup](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/contrast-audit/04-ownership-lower.png); [Onboarding.kt:106](/Users/arif/workspace/bbn/motovault/composeApp/src/commonMain/kotlin/com/motovault/app/ui/Onboarding.kt:106) |
| Garage | “My garage”, bike name, odometer, fuel-economy placeholder, component heading, gauge readings/names, and service-priority title are black. Gray units and green health labels remain visible. | [Garage](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/contrast-audit/05-garage.png), [Gauges and actions](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/contrast-audit/06-garage-lower.png); [MotoVault.kt:208](/Users/arif/workspace/bbn/motovault/composeApp/src/commonMain/kotlin/com/motovault/app/ui/MotoVault.kt:208), [Gauge:277](/Users/arif/workspace/bbn/motovault/composeApp/src/commonMain/kotlin/com/motovault/app/ui/MotoVault.kt:277) |
| Fuel form | Dialog heading, descriptions, and input values are readable. The “Filled to a full tank” checkbox label is black. | [Fuel form](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/contrast-audit/07-fuel-dialog.png); [OwnershipForms.kt:47](/Users/arif/workspace/bbn/motovault/composeApp/src/commonMain/kotlin/com/motovault/app/ui/OwnershipForms.kt:47) |
| Service form | The displayed dialog heading, descriptions, selected component, and field values are readable. This does not share the black-title problem seen in the component list. | [Service form](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/contrast-audit/08-service-dialog.png) |
| All components | Each component name is black; dates, intervals, and colored health labels remain readable. | [Component list](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/contrast-audit/09-components.png); [OwnershipForms.kt:65](/Users/arif/workspace/bbn/motovault/composeApp/src/commonMain/kotlin/com/motovault/app/ui/OwnershipForms.kt:65) |
| Schedule editor / odometer editor | Source uses an explicitly colored Sheet title and Material text fields. No additional plain black body label was found in these editors. Unfocused field outlines remain very faint, as in the captured setup and service forms. | Source review of `ScheduleDialog`, `NumberDialog`, and `Field`; these editors were not separately captured. |
| Vault empty state | Page title, main heading, empty-state title, and roadside-inspection title are black. Descriptions and action buttons remain readable. | [Empty vault](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/contrast-audit/10-vault-empty.png) |
| Vault saved documents | The document type/title is black. Document number and expiry status remain readable. | [Populated vault](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/contrast-audit/14-vault-populated.png); [MotoVault.kt:303](/Users/arif/workspace/bbn/motovault/composeApp/src/commonMain/kotlin/com/motovault/app/ui/MotoVault.kt:303) |
| Add/edit document form | Captured title, field labels/values, and attachment actions are readable. No main black-text defect was observed in the form itself. | [Document form](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/contrast-audit/12-document-form.png), [Lower form](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/contrast-audit/13-document-form-lower.png) |
| Dark document details | Holder, number, details, and actions are readable because the view explicitly uses white text. | [Document details](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/contrast-audit/15-document-detail.png); [OwnershipForms.kt:208](/Users/arif/workspace/bbn/motovault/composeApp/src/commonMain/kotlin/com/motovault/app/ui/OwnershipForms.kt:208) |
| History | Page headings, spend totals, economy placeholder, and empty-state title are black. Filters and gray explanatory text remain readable. Populated record titles also omit a color in source, so they have the same defect. | [History](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/contrast-audit/11-history.png); [OwnershipForms.kt:148](/Users/arif/workspace/bbn/motovault/composeApp/src/commonMain/kotlin/com/motovault/app/ui/OwnershipForms.kt:148) |
| Inspection list | Dark text on the light panel is readable. This view correctly assigns its text color explicitly. | [Inspection list](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/contrast-audit/16-inspection-list.png) |
| Inspection document details | Details remain readable, but “All documents” uses the pale brand accent on the light panel. This is a separate contrast defect. | [Inspection details](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/contrast-audit/17-inspection-detail.png); [OwnershipForms.kt:221](/Users/arif/workspace/bbn/motovault/composeApp/src/commonMain/kotlin/com/motovault/app/ui/OwnershipForms.kt:221) |

## Second cause: inspection keeps dark-theme action colors

`Sheet(light = true)` changes the panel background and explicit title color, but leaves the surrounding dark Material color scheme in place. The default `TextButton` color remains the brand’s pale accent.

The captured Yamaha “All documents” action is **2.10:1** against the light `#F5F4EF` panel. Source calculations show **1.61:1–2.39:1** across the nine brand accents. All fall below even the 3:1 large-text threshold. The blue filled “Show back” button remains readable because its dark label sits on the blue button, not directly on the light panel.

This needs a contrasting action color for light inspection surfaces, in addition to fixing the default text color on dark surfaces.

## Third cause: Android status-bar appearance follows the system theme

The main screens show black clock and status icons against the app’s dark top background. Dialog screens instead show white icons. This mismatch is visible in the screenshots.

[MainActivity.kt:11](/Users/arif/workspace/bbn/motovault/composeApp/src/androidMain/kotlin/com/motovault/app/MainActivity.kt:11) calls `enableEdgeToEdge()` with its default automatic system-bar style. The simulator reports `Night mode: no`, while MotoVault always draws its own dark theme. The default style detects the system’s night-mode setting rather than the Compose canvas color, as shown in the [AndroidX edge-to-edge source](https://raw.githubusercontent.com/androidx/androidx/androidx-main/activity/activity/src/main/java/androidx/activity/EdgeToEdge.kt).

The main activity’s status-bar icon appearance needs to match its always-dark background. This is separate from Compose text-color inheritance.

## Other observations

- `Line` is only 7% white and is also supplied as Material `outline`. Unfocused field and card boundaries are very faint. Field text itself is readable; this is a secondary control-visibility issue.
- Metadata uses 9–11 sp text in several places. Its explicit gray color has sufficient measured contrast, but small text can still be harder to read on a physical phone.
- History’s selected filter and the save snackbar retain default Material purple/lavender colors. They are readable, but do not consistently follow the chosen brand theme.
- Disabled save/continue controls are intentionally dim; their appearance should not be confused with enabled content rendered black.

## Recommended correction

1. Provide a matching default content color at the app root and every shared painted container. A Material `Surface` or a scoped `LocalContentColor` provider can establish white content on dark backgrounds.
2. Give light inspection content its own dark text/action colors. A global white-text fix alone is insufficient.
3. Set system-bar icon appearance to match the actual app/window background.
4. Separate subtle decorative borders from input/control outlines, and audit remaining Material color roles.
5. Verify every captured screen after the fix, including ordinary titles, values, checkbox labels, selected/disabled states, and light inspection actions. Add a focused UI regression check for inherited text colors; the existing domain tests do not cover this failure.

No contrast fixes were applied during this investigation. The evidence screenshots are from the unchanged app.
