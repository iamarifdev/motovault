# P08-02 large-vault performance finding and options

Date: 13 November 2026 (Asia/Dhaka). Status: finding recorded; **decision
needed** (budget vs optimization). No production UI change was made.

## Measurements

| Vault size | Device | Cold start (`am start -W`) | Scroll: frames / janky | p90 | PSS |
| --- | --- | --- | --- | --- | --- |
| 8 logs (default seed) | Motorola Edge 50 Fusion (physical) | 943–1,018 ms | 431 / 6 (**1.39 %**) | 14 ms | ~82 MB |
| 1,208 logs + 200 docs (synthetic) | Motorola Edge 50 Fusion (physical) | **1,318 ms** | 772 / 116 (**15.03 %**) | 12 ms | 146→156 MB |
| 1,208 logs + 200 docs | Pixel_10 emulator (pre-work) | 2,119 ms | 284 / 81 (28.52 %) | 18 ms | ~126 MB |

The physical jank exceeds the provisional budget used earlier (≤5 % janky) and
the 90th percentile stays acceptable (12 ms), so the problem is repeated
main-thread frames, not a single stall.

## Root cause (source inspection)

`HistoryScreen` builds the Records list with a plain `Column` + `verticalScroll`
and `groups.forEach { items.forEach { … } }`; the app contains **no
`LazyColumn`/`LazyVerticalGrid` anywhere** in `commonMain` UI. With 1,208 logs,
every row (each with `remember`, `animateFloatAsState`, borders, text layout) is
composed and measured up front, then all of it participates in scrolling. The
month grouping also runs eagerly (`groupBy`) but is `remember`-pinned to the
filtered list.

Consequences: high first-composition cost and memory (PSS +10 MB during scroll),
and repeated frame misses while scrolling; the default 8-log vault is unaffected
because the list is tiny.

## Options

**A. Accept a separate large-vault budget (fast, no code).** Keep the current UI,
define a documented budget for large vaults (for example: start ≤ 2 s, p90 ≤ 16 ms,
janky ≤ 20 % for ≥ 1,000 logs on a mid-range device), and disclose the limitation
in release notes. Risk: real owners with years of records (a bike logged monthly +
fuel can reach hundreds of rows within 1–2 years) may feel scroll stutter.

**B. Virtualize the Records list (recommended if release scope allows).** Convert
the eager `Column`/`forEach` list to a `LazyColumn` with month headers (the v3
prototype's layout and spacing preserved), keeping the row composable unchanged.
Expected: first-composition cost scales with the viewport, not the log count;
memory and jank drop substantially. Scope: one production file change, screenshot
design-parity re-capture, update any tests that assume eager composition, and
re-run the same measurement. Estimated small, reviewable slice.

**C. Hybrid:** virtualization only (B) plus keeping the current scroll position
behaviour and undo state; no data/format change.

## Recommendation

Do **B** before a signed release if large historical vaults are in scope for the
1.0 audience; otherwise record **A** as an explicit, disclosed limit. Either way
the measurement above is the acceptance evidence for "large-vault operations";
the remaining choice is a product/release decision, not more device work.
