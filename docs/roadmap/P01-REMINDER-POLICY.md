# P01 reminder policy

Implementation base: `86c1598`. Decisions: 24 October 2026, Asia/Dhaka.

The default document warning is 30 calendar days before expiry, followed by one
expiry-day alert. Default delivery is 09:00 in the device's current timezone.
The device may delay delivery. This is a reminder, not confirmation of renewal.
No-expiry, invalid-date and expired documents do not get a new default plan.
An expiry-day late addition gets one catch-up expiry alert. An addition inside
the warning window gets the next local reminder time, unless expiry is sooner.

`ReminderPolicy` stores the global lead (1–365 days) and local minute (0–1439).
A document's nullable `reminderWarningDays` overrides the lead. Its `reminder`
flag and the global `docReminders` flag must both be on. Existing native opt-in
and actual OS permission are additional gates. Neither an import nor a default
true preference grants notification consent. Denial/revocation never enables
the existing native opt-in. Service notifications use their own global flag and
stable schedule IDs; delivery and its visible controls are P01-02/P01-03 work.

Snooze is a stored local date/time override and does not change expiry, service
baseline or mileage. Equal delivery times for one record are deduplicated. The
UI will offer tomorrow/one week choices with a visible date in P01-03. Consumed
events remain suppressed; actionable snooze of an already delivered event needs
a separate request identity in P01-02, not erasure of consumption history.

Request identity contains the full durable document ID, expiry and event kind.
Changing a label preserves identity; changing expiry replaces it. Old 60-day
warning dates are recalculated to 30 days, while previously consumed identities
remain consumed. Existing native reservation/recovery journals remain intact.
Reconciliation rereads committed storage, including global and per-record flags,
and cancels obsolete requests. A failed owner save never schedules its candidate.

## Settings and schema 4

Room adds `vault_meta.preferences`, a non-null JSON settings payload, inside the
normal migration transaction. It contains all v3 preferences and remembered
prices/suggestions, without duplicating owner records. Appearance and location
stay in their existing columns. A malformed settings payload fails restoration
instead of silently replacing preferences.

Schema 3's atomic outbox retains the latest committed full snapshot, including
settings omitted by its reader. Migration copies only preferences from that
snapshot. It does not replace owner rows, asset references, opt-in, OS permission,
outbox identity or revision. Without an outbox (for example schema 1), the empty
settings payload uses backward-compatible defaults. Corrupt outbox JSON fails
migration, leaving the transaction available for recovery. Legacy JSON imports
continue through the normal single transaction and include all preferences.

Recovery: retain a pre-upgrade database and its WAL plus original assets before
release deployment. On migration failure, do not delete storage or use destructive
fallback. Do not open a schema-4 database with a schema-3 app. Roll back a release
with the complete pre-upgrade snapshot, never an in-place downgrade. P00 fixture
copies remain immutable historical evidence, not the current schema contract.
