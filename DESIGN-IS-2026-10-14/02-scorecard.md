# Rams scorecard

Expert heuristic score: **16/30**. Scores describe the examined design, not user satisfaction. Worst representative cases drive scores. Innovation lacks a peer study; understandability lacks first-user testing; native resource use lacks measurements. Those scores are provisional. The skill's web-specific JS-size rubric cannot fairly score this native app; principle 9 uses native attention/motion/theme evidence instead. The total is directional, not a release gate.

| # | Principle | /3 | Evidence and justification |
|---|---|---:|---|
| 1 | Innovative | 2 | E1/E5: brand-specific motorcycle identity paired with original-document inspection improves familiar ownership patterns; no claim of industry novelty. |
| 2 | Useful | 1 | E3: service logging requires an odometer detour and baseline correction requires cross-screen work. Tasks exist but are needlessly difficult. |
| 3 | Aesthetic | 2 | E8: coherent palettes and photography; broad literal type/spacing choices and small important labels weaken consistency. |
| 4 | Understandable | 1 | E5/E7: document-flow tabs behave differently from app tabs; export exposes JSON/digest terminology; disabled Save lacks a local reason. |
| 5 | Unobtrusive | 1 | E2/E5: dominant hero/photo and stacked originals compete with practical actions and renewal attention. |
| 6 | Honest | 2 | E2/E6/E7: explicit owner-entered export and inspection limits are strong; mechanical-health and definite glare labels overstate what heuristics establish. No observed dark pattern. |
| 7 | Long-lasting | 3 | E8: restrained native cards, real photographs and a shared neutral/brand palette are durable; preserve them. |
| 8 | Thorough | 1 | E3/E5/E7/E8: core states exist, but restore recovery, field errors and accessible custom interaction are incomplete. Focus remains unverified. |
| 9 | Environmentally friendly | 2 | E9/E8: no examined idle animation and system dark mode honored; native asset, energy and latency metrics remain unmeasured. Native adaptation of rubric. |
| 10 | As little design as possible | 1 | E2/E5/E7: repeated appearance access, secondary decorative copy, oversized hero and technical default detail can be reduced without weakening tasks. |

## Accessibility calculations and source inventory

Current Appearance.kt:43-48 palette pairs calculated using sRGB relative luminance: light primary/white 17.10:1; muted/light canvas 5.99:1; muted/light raised 5.57:1; dark muted/raised 6.10:1. KTM button pairs: white/light accent 5.82:1; dark foreground/dark orange 7.14:1 (Appearance.kt:71). These selected pairs exceed 4.5:1. Not every brand, status, overlay, focus or rendered pair was checked.

Source spacing literals: 4,5,6,7,8,9,10,12,13,14,15,16,18,20,22,24,32,86dp; separate wallet overlap -36dp. Type literals: 8,9,10,11,12,13,14,15,16,18,21,22,23,24,25,28,29sp, with conditional 38sp hero. There are 46 unique opaque hex references across redesign files, including alternative palettes and camera overlays; this is not simultaneous rendered color count.

Primary buttons specify 52dp minimum; document primary 48dp, secondary 44dp (MotoVault.kt:323-324; DocumentFlow.kt:67-69). Effective accessibility target bounds and focus order remain unmeasured.
