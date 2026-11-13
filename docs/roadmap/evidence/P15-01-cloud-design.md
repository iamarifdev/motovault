# P15-01 Evidence — Cloud Provider, Account Model & Recovery Design

## Artifacts Reviewed & Produced
- Architecture specification: [P15-CLOUD-BACKUP-SPEC.md](../P15-CLOUD-BACKUP-SPEC.md)
- Existing `.mva` local archive contract reviewed: `BackupUseCase.kt`, `RestoreUseCase.kt`, `BackupCrypto.kt`, `BackupPreferences.kt`.

## Key Design Decisions
1. **Envelope preservation**: Reuse versioned `.mva` client-side encrypted archives rather than introducing plaintext JSON cloud objects.
2. **Owner-scoped atomic publication**: Bind `(accountId, vaultId)` explicitly, require all asset objects to match SHA-256 before manifest publication, enforce `expectedPreviousRevision` CAS, and honor `operationId` idempotency.
3. **Lifecycle safety**: Cancel in-flight uploads on `logout`/`switchAccount`, keep `CloudConnectionStatus.DISCONNECTED` when unconfigured, and block privileged secrets from client builds.
