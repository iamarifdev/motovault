# P06 implementation

Base revision: `f2ca12f`. Working changes belong to P06 only. Validation is recorded in
[P06-verification.md](P06-verification.md); this implementation report alone is
not runtime proof.

- Pure `domain/ledger` rules own poisha, periods, categories, recorded-distance
  coverage, expense validation and owner budget/price preferences.
- `RideLog`, settings and their backup mappings add optional fields. Existing
  Room JSON columns and coordinator commits remain authoritative. No schema
  version changes or second writer are introduced.
- Renewal fees contribute through logs, not renewal metadata. General expenses
  do not reset maintenance baselines. New records retain legacy `cost` alongside
  integer poisha; service integer totals keep precedence.
- Fuel confidence uses the same full-to-full calculation for persisted economy
  and presentation. Missing fills break cycles; price confirmation retains an
  effective date and older edits cannot replace a newer confirmed value.
- V3 cost states precede the native History controls, sheets and resources.
  English/Bangla copy is reviewed together against the shared style. Existing
  language, digits, sheets, keypad and calendar input remain in use.

Compatibility, mutation/Undo, native resource switching, screenshots and builds
are recorded in the phase verification report. The owner-approved iOS VoiceOver
handoff remains open under P08-02. New expense mutations operate on log lists;
`domain/vault/ExpenseMutation.kt` applies the pure transition to the aggregate,
keeping `domain/ledger` independent of the vault and avoiding a package cycle.
