# Engineering conventions

Apply [the architecture contract](../roadmap/ARCHITECTURE-CONTRACT.md) and the phase-specific roadmap requirements. This is practical guidance for implementing that contract.

- Keep one coherent responsibility per type, function and feature file. Split by behavior and ownership, not line count alone. Review handwritten files over 400 lines and functions over 80 lines; document an owner and removal task for new or expanded files over 600 lines. Generated data is reviewed separately.
- Keep state owners scoped to their destination or flow. UI-only animation and focus state can stay in Compose. Durable operations go through an application contract and the vault coordinator. Expose immutable read-only observable screen state, represent failures and preserve cancellation.
- Use named, owner-scoped business policies and units. UI geometry, colors and motion use the approved token sources. Stable storage keys and protocol values belong to their adapter; configuration and secrets do not belong in source constants.
- User-facing text, including accessibility, notifications and exports, uses localized resources. Shared Compose text uses multiplatform resources; platform-owned permission descriptions use native localization. No new inline translation branches. Use placeholders and plurals instead of concatenating translated fragments.
- Existing language selection, Bangla digits, supported input digit sets and stored numerical values remain compatible. Resolve locale-specific display formatting outside domain rules. Test both languages and lifecycle behavior for changed flows.
- Use typed failures across domain/application boundaries; localize at presentation. Logs and reports must omit passwords, credentials, document contents and owner identifiers.
- Follow `AGENTS.md` for tracker updates, design, data preservation, Podman and `rtk`. Do not perform a broad formatting pass or incidental refactor while migrating a slice.

Existing violations are tracked in [the architecture transition exceptions](architecture.md#recorded-transition-exceptions) and the scoped literal baselines, not silently treated as compliant. Run the executable checks documented in [verification](verification.md) and record their lexical limits.
