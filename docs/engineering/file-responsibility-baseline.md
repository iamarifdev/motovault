# Handwritten file responsibility baseline

Status: P04A-06 responsibility baseline, 2026-11-01. **No production file is
above 400 lines after the localization/SRP work.** Two test-support files remain
above the threshold; P04A-06 turns this into an executable ratchet.

| File | Lines | Owner / reason | Removal or re-review trigger |
| --- | ---: | --- | --- |
| `androidInstrumentedTest/.../NativeAssetInstrumentation.kt` | 568 | Test-only probe entry after fixture split | Keep the entry runner here; stage checks live in `Probe*Checks.kt`. |
| `commonTest/.../BackupTest.kt` | 583 | Backup round-trip fixtures | Split by backup stage on the next test change. |

Files above 600 lines require the recorded exception above. No production file
above 400 lines may grow without updating this table and the tracker.
