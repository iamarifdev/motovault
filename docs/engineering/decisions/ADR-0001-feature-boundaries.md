# ADR 0001: Feature ownership with inward dependencies

Status: accepted for P03A implementation; target modules are incremental.

MotoVault will organize presentation by feature and place shared rules and user workflows behind domain/application contracts. Platform and data implementations depend on those contracts; the app composition root constructs them. Shared ViewModels own feature screen state where lifecycle behavior requires it. Existing durable writes continue through one coordinated vault owner.

This supports feature ownership and testable business behavior without immediately splitting every package into a Gradle module. The fuel slice is the first migration. P03A-04 separately migrates backup/restore. Existing serialized formats, owner assets, revision conflict handling, Undo, privacy and recovery remain explicit compatibility requirements.

The current source has exceptions listed in [architecture.md](../architecture.md#recorded-transition-exceptions). Their presence is not evidence of completion. Feature, native and quality-gate changes require their own roadmap evidence.
