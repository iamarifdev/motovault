# P04A-08 app-wide localization and SRP establishment

Status: complete for production code and static verification; runtime language
switching is owned by P08-05.

## Delivered

- Active Compose screens/sheets, native Android/iOS prompts, authentication
  messages, service/document catalogues and date/unit formatting resolve through
  shared or platform resources.
- Domain and catalogue display identity uses stable IDs; labels and units resolve
  at presentation. New service titles persist canonical IDs.
- Bangla copy was reviewed against [localization-style.md](../../engineering/localization-style.md)
  and aligned with the design contract examples; `tools/bangla_copy_lint.py`
  guards known robotic regressions.
- Production files were decomposed below 400 lines; the responsibility baseline
  retains only two test-support files.
- Literal, copy, responsibility, dependency, package-cycle and resource-parity
  gates are part of CI.

## Verification

```text
274 Kotlin tests, 0 failures
Android/iOS shared compilation passed
bangla_copy_lint.py: 15 regression phrases pass
ui_literal_gate.py: 18 data/alias entries
responsibility_gate.py: 2 test-support files
architecture gates and roadmap check pass
```

## Boundary

This task establishes resources, stable presentation boundaries, modern Bangla
copy and the SRP ratchet. P08-05 remains responsible for Android/iOS runtime
language-switch evidence, native surfaces and screenshots.
