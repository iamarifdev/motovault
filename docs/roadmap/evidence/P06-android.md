# P06 Android runtime

Environment: existing Pixel 10 Android 17/API 37 emulator, arm64, isolated
`com.motovault.app.privacyprobe`. No physical device or owner database was used.
Base `f2ca12f` plus the P06 source change; exact source/artifact hashes are in
[the manifest](P06-artifact-manifest.json).

## Execution and actual persistence

Built the privacy-probe app/test APKs, installed with `adb install -r`, then ran:

```text
am instrument -w -e onlyCosts true
com.motovault.app.privacyprobe.test/com.motovault.app.storage.security.NativeAssetInstrumentation
```

[Native result](P06-android-native.txt), with trailing console blank lines omitted:

```text
PASS P06 Android: native-ledger-legacy-correction-delete-undo-room-reopen-and-encrypted-backup; UI save/Undo and four language/digit combinations
```

The fresh synthetic Room database uses the production `VaultSession` writer.
It starts with legacy ৳1.005 (101 poisha), adds a 125050-poisha parking expense,
corrects that row to a 2501-poisha toll on 31 October, deletes it, then commits
Undo. Reopening Room retains the corrected ID, exact amount, budget and confirmed
price. The independent total is 101 + 2501 = 2602 poisha; November includes only
the legacy 101 poisha. A real encrypted archive preserves the logs and settings
through preview. JSON compatibility is checked separately.

## UI and copy

The isolated UI fixture renders the production History and sheets. Its UI state
is synthetic and held in memory; its screenshots are separate from the Room
assertions above. The initial total is independently 500 + 200 + 300 + 1250 +
0.50 = ৳2250.50. A 150 km full-to-full cycle consumes 2 + 3 litres, giving
30 km/L. Covered spending excludes the opening-day ৳500, so the ratio is
৳1750.50 / 150 = ৳11.67/km.

Verified by direct native assertions and image review:

- English/Bangla switches without restarting and both independent digit styles;
- dark appearance and 1.3 text scale with wrapping and scrollable chips;
- entering ৳12.50 changes the total to ৳2263.00; Undo restores ৳2250.50;
- failed save retains the draft and localized error; dismissal preserves total;
- price confirmation/missed-fill explanations in both languages.

After associating price checkboxes with their labels, only those controls were
rerun with `-e onlyCosts price`. [Result](P06-android-price-controls.txt):

```text
PASS P06 labeled price confirmation controls in English and Bangla
```

The retry also captures the final unknown-document fallback as one renewal
label. It does not repeat the ledger/archive suite. The earlier full workflow
and final checkbox checks are distinct artifact entries in the manifest.

Representative captures: [final Bangla](p06-android-bn-bn-final.png),
[English/Bangla digits](p06-android-en-bn.png),
[English/Latin digits](p06-android-en-en.png),
[Bangla/Latin digits](p06-android-bn-en.png),
[dark/large](p06-android-dark-large.png), [saved](p06-android-saved.png),
[failure](p06-android-save-failure.png),
[confirmed English price](p06-android-price-en-final.png),
[confirmed Bangla price](p06-android-price-bn-final.png).

Limits: emulator evidence only. TalkBack audio, OS-level large text, native
physical-device performance and release signing/distribution remain release
gates; no still image is used as motion evidence.
