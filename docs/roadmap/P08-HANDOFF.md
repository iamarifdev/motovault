# P08 session handoff — 13 November 2026

Read `AGENTS.md` first, then this file. Do not open `roadmap.json`, `PROGRESS.md`
or `IMPLEMENTATION-PLAN.md` whole.

## Where the phase stands

| Task | Status | Notes |
| --- | --- | --- |
| P08-01 core regression + design matrix | **Done** | iOS 27 design matrix 8/8 (re-run after layout/swatch fixes); Android emulator + Motorola physical captures; evidence under `docs/roadmap/evidence/p08/`. |
| P08-05 localization + text-debt baseline | **Done** | Resource parity verified; copy signed off by the owner (named review in `P08-05-review.md`); one post-sign-off gap remains in iOS `InfoPlist.strings` (below). |
| P08-02 physical-device accessibility/reliability/performance | **In review** | Android physical slice done: native probe PASS 37, camera allowed path PASS, native notification delivery verified, share/picker/permission paths, performance measured. Remaining listed below. iPhone physical **waived by owner (WD-01)**. |
| P08-03 representative-owner pilot | **Planned** | Kit ready at `docs/roadmap/evidence/P08-03-pilot-kit.md`; needs participants + consent. |
| P08-04 release candidate + handoff | **Planned** | Release pack at `docs/roadmap/evidence/P08-04-release-pack.md`; unsigned release APK verified; needs signing authorization/material + final declarations. |

`python3 tools/roadmap.py next` currently returns no eligible task: P08-02/03/04
are the gate. P09+ are waiting on prerequisites. Do not mark P08 done while the
items below are open.

## Blocking items (owner input required)

1. **Large-vault performance decision** — physical 1,208-log vault: 15.03 % janky
   frames (116/772), root cause is the eager non-virtualized Records list. Choose
   (A) accept a documented large-vault budget or (B) authorize the recommended
   `LazyColumn` virtualization. Analysis: `P08-02-large-vault-performance.md`.
2. **Copy sign-off** — proposed EN `NSLocationWhenInUseUsageDescription` and BN
   `NSPhotoLibraryUsageDescription` (mixed-language permission dialogs today).
   Review doc: `docs/roadmap/evidence/p08/p08-04/ios-release-metadata-review.md`.
3. **P08-03 pilot** — representative owners + consent.
4. **P08-04 signing** — authorization and signing material; then install signed
   candidate + restore/upgrade smoke + distribution proof.
5. Optional owner permissions: device clock change (scheduled reminder delivery),
   USB unplug (battery/energy soak), assisted TalkBack/real-voice session.

## Verified evidence map

- `docs/roadmap/evidence/p08/` — screenshots/recordings/trees for design matrix,
  localization, layout fixes, physical Android runs, emulator pre-work.
- `P08-02-android-device.md`, `P08-02-swatch-accessibility.md`,
  `P08-02-large-vault-performance.md`, `P08-02-ios-device-plan.md`,
  `P08-02-android-remaining-plan.md`.
- `P08-owner-decisions.md` (WD-01 iPhone waiver), `P08-remaining-gates.md`
  (external inputs), `P08-04-release-pack.md` + `p08/p08-04/` (release
  declarations and unsigned build facts).

## Commands for the next session

```bash
python3 tools/roadmap.py status
python3 tools/roadmap.py next
python3 tools/roadmap.py show P08-02   # repeat per task
# portable gates (see docs/engineering/verification.md for the full list)
python3 tools/ui_literal_gate.py && python3 tools/bangla_copy_lint.py \
  && python3 tools/responsibility_gate.py \
  && python3 tools/architecture_dependencies.py \
  && python3 tools/architecture_package_cycles.py \
  && python3 tools/fuel_policy_literals.py \
  && python3 -m unittest discover -s tools/tests -p 'test_*.py' \
  && python3 tools/roadmap.py check
```

Device state left as found: Motorola unlocked, font scale 1.15, night off,
stay-awake 0, notifications permission false, camera granted; emulator and
simulator reused without reset. No owner data, audio or secrets were committed.
