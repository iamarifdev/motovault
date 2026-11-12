# P08-03 representative-owner pilot kit

Status: **prepared, not run.** No participant observation is recorded here and
none may be invented. This kit makes the pilot executable in one session per
participant once the owner provides participants and consent. It adapts the P07
capture protocol (`docs/roadmap/P07-CAPTURE-PILOT.md`) to P08 requirements.

## Scope and acceptance mapping

P08-03 acceptance: include commuters, a first-time/used-bike owner and an
enthusiast; obtain consent for anonymized observations; track backup/restore
success, reminder usefulness, log effort and cost comprehension; close
data-loss/privacy/incorrect-guidance defects. Required evidence kinds:
`verification` and `review`.

Physical iOS is waived under WD-01
([P08-owner-decisions.md](P08-owner-decisions.md)); run the pilot on the
participants' Android devices (or the owner's connected Android phone for a
facilitated session) with the isolated synthetic build. Anything the participant
uses must be synthetic-state only; no participant records.

## Participants (fill from real recruits)

| Alias | Required profile | Android device/OS | Locale/digits | Consent signed | Session date |
| --- | --- | --- | --- | --- | --- |
| P1 | Daily commuter | | | ☐ | |
| P2 | First-time or used-bike owner | | | ☐ | |
| P3 | Enthusiast / frequent maintenance | | | ☐ | |
| P4 (optional) | Additional commuter | | | ☐ | |

Consent covers: anonymized observations, sanitized screenshots of app screens,
screen-recording-free note-taking, no raw audio retention, no personal documents,
right to stop at any time. The consent text is owned by the owner; do not store
signed forms in the repository.

## Build and isolation

- Use the isolated probe build (`com.motovault.app.privacyprobe`, current APK
  hash recorded in `p08/p08-02/android-device/README.md`) or a participant-facing
  build only after the owner authorizes it.
- Start each session from synthetic seed data (`VaultState.seedData()`); reset
  between participants so no records carry over. Never install over or clear the
  owner app `com.motovault.app`.
- Record build hash, device model, Android version, app language/digit settings
  and text scale per session.

## Session protocol (~35–45 minutes, order varied per participant)

1. **Consent and setup** (5 min): explain the synthetic build, no real
   documents/payments, stop rule.
2. **Manual baseline** (10 min): create one fuel log and one service log by hand;
   note steps and elapsed seconds; check the Records tab cost understanding.
3. **Capture paths** (15 min), each followed by Save and immediate Undo:
   - odometer photo from a printed synthetic target (camera allow, blur/glare
     retry, below-current reading, cancel/retake);
   - explicit text share of a synthetic `ODO …` message, cold and warm app;
   - Bangla voice where the device exposes an offline bn-BD provider; record
     unavailable-provider limitation honestly (no network fallback allowed).
4. **Reminders and backup** (10 min): enable/verify reminders, explain what the
   app promises; create and restore one synthetic backup; note any confusion.
5. **Cost comprehension and effort** (5 min): ask what the Records tab shows,
   where money went, and whether the numbers matched expectations.
6. **Close**: thank, stop any recording, sanitize notes, reset synthetic data.

## Observation record (per participant, fill only observed values)

| Measure | Result |
| --- | --- |
| Manual log steps / seconds (baseline) | |
| Photo path: candidate found / saved / undone | |
| Share path: review shown / saved / undone, cold and warm | |
| Voice path: available locale, outcome, fallback used | |
| Backup create success + restore success | |
| Reminder usefulness (1–5) and confusion notes | |
| Log effort (steps/seconds photo/share vs manual) | |
| Cost comprehension: correct total/category readback | |
| Data-loss/privacy/incorrect-guidance defects observed | |

Free-text notes per participant (sanitized): keep only app-visible facts, no
participant names, documents, messages, audio or home-screen content.

## Release decision log

For each observed defect, record severity, whether it blocks release, the fix
reference and the re-check result. Data-loss, privacy and incorrect-guidance
defects close only with a recorded fix and re-check, not with a promise.

| ID | Participant | Defect | Class | Blocks release? | Fix/recheck |
| --- | --- | --- | --- | --- | --- |
| | | | | | |

## Evidence and limits

- Required evidence: `verification` (completed observations, defects, decisions)
  and `review` (owner/product review of the findings). Do not fabricate
  participants, statistics or retention.
- P08-03 cannot be marked done on this kit alone: it needs real participants and
  consent. iPhone observations are covered by WD-01.
- Keep participant privacy: no signed consent forms, audio, photos of people or
  owner data in the repository.
