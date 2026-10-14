# MotoVault appearance

Implemented 11 October 2026.

The app now offers **System**, **Light**, and **Dark** through the Appearance action on setup, garage, vault, and history. System is the default for both new installs and records saved before this feature. The selected preference is stored with the local records and retained when replacing the bike.

System follows the device’s appearance. Explicit Light and Dark choices override it. Android handles appearance configuration changes without restarting the activity, so the current screen and in-progress form state can remain in place.

## Bike identity and contrast

Both modes preserve the selected model’s photograph, brand marks, and brand color family. Dark mode uses the prototype’s bright accents. Light mode uses deeper shades of those same hues on warm, pale surfaces.

| Brand | Dark accent | Light accent |
| --- | --- | --- |
| KTM | `#FF7900` | `#AA4700` |
| CFMOTO | `#69D2DB` | `#00717A` |
| Yamaha | `#88A8FF` | `#315ABE` |
| Honda | `#FF7B84` | `#B42A38` |
| Suzuki | `#7EBAF5` | `#246294` |
| Bajaj | `#96B9FA` | `#355D9E` |
| TVS | `#F4828B` | `#A52B41` |
| Hero | `#FF8983` | `#B1362C` |
| Royal Enfield | `#DBB77C` | `#805E28` |

Shared semantic colors define canvas, surface, text, secondary text, borders, input outlines, health, warning, error, and button labels. A scoped `LocalContentColor` supplies readable text to ordinary Compose Text calls. Material surface/container roles are also assigned to prevent unrelated default purple colors from appearing in menus, filters, or snackbars.

Inspection remains a light document view even when the rest of the app is dark. It gets a complete light color scheme, including a deeper brand-colored “All documents” action. Android system-bar icons follow the app’s selected appearance.

## Verification

- Android debug assembly passed.
- Android unit tests: **41 passed**, zero failures.
- iOS ARM64 shared-code compilation passed.
- iOS simulator tests: **41 passed**, zero failures.
- Palette tests check all nine brands in both modes. Primary, secondary, accent, healthy, warning, and error text are checked at 4.5:1 or higher against canvas/surface/raised backgrounds. Enabled button labels are checked at 4.5:1; input outlines at 3:1.
- Simulator checks confirmed the default System choice, live system appearance changes in both directions, explicit Light overriding a dark system, explicit Dark overriding a light system, and persistence after restart.
- Visual checks covered setup and the model picker in both modes, garage, vault, history, and light inspection detail.
- The simulator’s original system setting is restored after testing, and MotoVault is left on System.

Screenshots:

- [System light garage](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/theme-verification/01-system-light.png)
- [Appearance chooser with System selected](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/theme-verification/02-appearance-picker.png)
- [Explicit dark garage](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/theme-verification/03-explicit-dark.png)
- [Dark preference after restart](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/theme-verification/04-dark-after-restart.png)
- [Dark vault](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/theme-verification/05-dark-vault.png)
- [Readable light inspection actions](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/theme-verification/06-inspection-action.png)
- [Dark history](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/theme-verification/07-dark-history.png)
- [System dark](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/theme-verification/08-system-dark.png)
- [System light after device appearance change](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/theme-verification/09-system-light-after-change.png)
- [Explicit light with a dark system](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/theme-verification/10-light-overrides-system-dark.png)
- [Light setup](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/theme-verification/12-light-setup.png)
- [Light model picker](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/theme-verification/13-light-model-picker.png)
- [Dark setup](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/theme-verification/14-dark-setup.png)
- [Dark model picker](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/theme-verification/15-dark-model-picker.png)
- [History stays open during System change to Dark](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/theme-verification/16-history-live-system-dark.png)
- [History stays open during System change back to Light](/Users/arif/workspace/bbn/motovault/docs/archive/evidence/theme-verification/17-history-live-system-light.png)

The existing checkout has no iOS host app. Shared screen colors and appearance logic compile for iOS; UIKit status-bar styling still belongs to that future host. No physical-device iOS rendering is claimed.
