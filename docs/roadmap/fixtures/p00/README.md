# P00 paired baseline fixtures

Reference date: 24 October 2026. All records are synthetic. The five scenarios
are `empty`, `overdue`, `partial-fill`, `documents` and `unknown-service`.

Each scenario contains its semantic label, fixed reference date and two explicit
representations: the current native `VaultState` input and v3 prototype input.
Stable IDs, dates, mileage, costs and full/partial flags match. Empty means an
existing bike without records; this is different from first-install onboarding.
Display-name adapters follow each current model: native English default rule/type
names versus prototype Bangla labels. Compare the underlying concepts and
record that presentation difference; do not translate owner-written free text.
Unknown history currently has the same persisted no-rule shape as empty setup,
because the models cannot encode baseline certainty yet. That gap is deliberate
baseline evidence for P04, not a newly implemented unknown-history feature.

The document case contains metadata only. It never uses real identity numbers,
holder details or documents. Separate compatibility fixtures cover synthetic
original/crop/preview bytes and legacy receipts.

## Generation and checks

```sh
rtk run python3 tools/baseline/p00.py generate --date 2026-10-24
rtk run python3 tools/baseline/p00.py check
rtk run python3 tools/baseline/compatibility.py generate
rtk run python3 tools/baseline/compatibility.py
```

The generated `native/*.json` files are legacy-import inputs for isolated test
apps. `design/app-ui-v3/public/p00-fixtures.json` is the local review-panel input;
it contains no account connection or native side effect. Native Kotlin test
strings are generated from the same source and checked for exact equality.
Keep the fixed date for reproducing this baseline. A later dated baseline should
be retained separately, with its own manifests and reports.

## Native isolation

The only fixture target is `com.motovault.baseline.p00`. The helper accepts no
caller-supplied package ID, requires an emulator serial for Android, and checks
the iOS bundle identifier before writing its test container. Never change that
constant to the production package. Android fixture loading clears only the
synthetic baseline package; iOS replaces only that isolated app's test database/
legacy JSON after stopping it.

The [capture reports](../../evidence/P00-04-verification.md) give the actual
commands, device identifiers, build scope and limitations. Recordings are
baseline observations, including mismatches; they do not turn known broken
behavior into a passing feature test.
