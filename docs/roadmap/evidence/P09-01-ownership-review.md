# P09-01 Ownership Model & Migration Review

Date: 13 November 2026
Reviewer: Ariful Islam (under workflow and WD-02)
Contract: [P09-GARAGE-OWNERSHIP-CONTRACT.md](../P09-GARAGE-OWNERSHIP-CONTRACT.md)

## Reviewed Scenarios

| Case | Rule Verified | Result |
| --- | --- | --- |
| Single-bike Room v6 & JSON upgrade | Defaults (`Ride.id = "primary"`, `archived = false`, `RideLog.vehicleId = "primary"`, `garageBikes = emptyList()`) keep existing `VaultFormatProbe` and `VaultDatabaseTest` lossless at Room `version = 6`. | Verified |
| Two bikes with identical brand & model name (e.g. two Yamaha R15 V4) | `switchToBike`, `setBikeArchived`, `mutateBike`, capture drafts, and reminders resolve exclusively by `Ride.id`, never by display label or index. | Verified |
| Shared owner document (`Driving licence`) vs bike document (`Tax Token`) | `isOwnerScoped` keeps Driving licence visible across bike switches and present after archiving a bike; bike documents move with their `GarageBikeRecord` and never leak to another bike. | Verified |
| Shared asset garbage collection across bikes | `state.allDocuments()` and `state.allDocumentAssetIds()` cover both active `documents` and `garageBikes.flatMap { it.documents }` so non-active/archived attachments are preserved in Room `asset_reference`, `VaultCoordinator`, `RecoverySnapshotStore`, and `.mvbk` archives. | Verified |
| Archiving active bike & last-bike guard | Archiving active bike auto-promotes the next non-archived bike; attempting to archive the final remaining active bike is rejected cleanly. | Verified |
