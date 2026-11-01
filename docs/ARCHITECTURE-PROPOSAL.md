# MotoVault production architecture and AI delivery plan

Status: target architecture adopted for roadmap planning; implementation remains tracked and incomplete. Prepared 2026-10-27.
Inspection baseline: `860c3ec04dac73bc10d6aba65798aff31887fdbb` plus the current working tree.
Scope: Android and iOS, Kotlin Multiplatform with shared Compose UI and native integrations.

## 1. Decision

Adopt feature-oriented modules, Clean Architecture dependency rules, and shared ViewModels with unidirectional data flow. Use constructor injection and an explicit composition root. Keep Room and the existing native integrations. Retain the existing visual system and public behavior.

These choices solve different problems: feature modules establish ownership; dependency rules isolate business behavior; ViewModels own screen behavior; reusable composables own rendering. SRP means one coherent reason to change, not one arbitrary line limit.

This is a target architecture. Existing toolchain compatibility must be proven before adopting a particular lifecycle API or upgrading dependencies. Do not combine a dependency upgrade with a structural migration unless it is a demonstrated prerequisite.

## 2. Current evidence

- Gradle includes `composeApp` and `skyDesignSystem`; business features are not separate Gradle modules.
- `Sheets.kt` has approximately 1,826 lines covering fuel, service, odometer, document and import flows.
- `Screens.kt` has approximately 1,579 lines covering Garage, Vault, History, Me and Onboarding.
- `MotoVault.kt` mixes application coordination, navigation, privacy, save/Undo, branding, formatting and UI primitives.
- `VaultState.kt` combines persisted models, business operations, serialization, demo data and storage declarations.
- Storage and backup domain code import UI-owned models. Domain backup code also imports concrete storage.
- `VaultSession` already provides observable snapshots, revision checks, save coordination and Undo. Preserve these guarantees.
- Reusable Vault controls and Sky components exist and are used. Extract and organize them rather than rebuilding them.
- Sky source is loaded from a sibling directory. Make its provenance and version reproducible before calling builds self-contained.

These observations establish architecture gaps, not a complete security audit or runtime assessment.

## 3. Target module map

Introduce modules incrementally as their code is migrated. Do not create empty modules to satisfy a diagram.

| Module | Responsibility | Allowed project dependencies |
| --- | --- | --- |
| `composeApp` | Android/shared composition root, root navigation, platform assembly | Feature modules, application, domain, data, platform, core UI, Sky |
| Existing iOS host | iOS application lifecycle and hosting the shared UI | Exported ComposeApp framework |
| `core:domain` | Business models, value types, pure rules and business-facing ports | No other production project module |
| `core:application` | User workflows, cross-feature orchestration and infrastructure ports | Domain |
| `core:data` | Room, versioned persistence DTOs, repository implementations, migrations, sync outbox | Domain, application |
| `core:platform` | Native implementations for crypto, assets, auth, reminders, OCR, location and export | Domain, application |
| `core:ui` | MotoVault-wide UI primitives, formatting and presentation utilities | Domain only where a shared domain type is needed, Sky |
| `skyDesignSystem` | Product-independent tokens, typography, controls and motion primitives | No MotoVault production module |
| `feature:garage` | Garage and onboarding presentation | Application, domain, core UI, Sky |
| `feature:maintenance` | Schedules, wear, service drafts and service entry presentation | Application, domain, core UI, Sky |
| `feature:ledger` | Fuel entry, history, costs and filtering presentation | Application, domain, core UI, Sky |
| `feature:documents` | Capture review, document wallet, renewal and inspection presentation | Application, domain, core UI, Sky |
| `feature:backup` | Backup/restore selection, preview and progress presentation | Application, domain, core UI, Sky |
| `feature:settings` | Preferences, privacy settings and feature entry points | Application, domain, core UI, Sky |
| `core:testing` | Test fixtures and reusable fakes | Test dependencies only; never on production classpaths |

Feature modules do not depend on each other or on concrete data/platform modules. Settings emits a backup navigation request; it does not import backup UI. The root assembles destinations. Application workflows coordinate shared business behavior without UI dependencies.

Data and platform implementations receive collaborators through constructors. The composition root supplies them, so neither implementation module needs to depend on the other. UI-bound native launchers belong in feature platform source sets or the host and report results through explicit callbacks.

Start with a single domain and application module, grouped into meaningful packages. Split a further business module only when ownership or dependency isolation justifies it. Avoid a miscellaneous `common` or `utils` dumping ground.

## 4. Dependency and responsibility rules

1. Domain has no Compose, Room, Koin, Android, UIKit, storage or UI imports. Explicitly allow only needed Kotlin, coroutine/Flow and time dependencies.
2. Application depends on domain contracts, never concrete adapters or presentation.
3. Data maps stored DTOs/entities to domain objects. UI state is never the persisted contract.
4. Platform-specific APIs stay in Android/iOS source sets or hosts. Prefer narrow interfaces; use `expect`/`actual` at platform construction boundaries when useful.
5. Feature code does not acquire global repositories, databases or sessions. Constructor dependencies are explicit.
6. Root code assembles navigation and dependencies. It does not calculate fuel economy, validate service records or implement backup rules.
7. Reuse a helper when behavior is shared and stable. Do not extract single-line wrappers or create a use case for every property getter.
8. Default implementation visibility is `internal` or `private`. Public APIs are intentional contracts.
9. No unexplained magic numbers or embedded application strings in production behavior. Use owner-scoped named constants, typed values, generated design tokens, configuration or localization resources according to their purpose. Do not create a global constants dumping ground.

### Constants, configuration and localization

This policy is part of the shared architecture contract and must be copied into the future conventions, AI workflow and automated acceptance gates.

| Value category | Required owner and representation |
| --- | --- |
| Business limits, warning windows and conversion factors | Named constants or typed policy/value objects beside the owning domain rule, with units, rationale and boundary tests |
| Spacing, radii, colors, typography, animation and haptic parameters | Existing generated design tokens or a named component/motion specification; change token sources rather than generated outputs |
| Environment-dependent endpoints, flags and operational settings | Validated build/runtime configuration supplied at assembly; never embed secrets in constants or shipped resources |
| Storage keys, protocol identifiers, paths and format versions | Named adapter-owned constants or typed identifiers; preserve exact serialized values and compatibility |
| Finite business categories and navigation destinations | Enums, sealed types or typed identifiers; avoid repeated string comparisons |
| User-visible labels, errors, hints, notifications and accessibility descriptions | Shared localized resources, not Kotlin string constants or inline language branches |

Do not replace a magic value with an equally unclear name such as `VALUE_30`. A name must explain meaning and units. Obvious structural literals such as an empty collection or a zero-based index do not require artificial constants; any exception must not conceal business policy. Fixture and generated-data literals remain in their clearly separated owners. Brand names and externally defined identifiers retain their authoritative spelling rather than being translated arbitrarily.

Use the official Compose Multiplatform resource system for shared Android/iOS presentation: `commonMain/composeResources/values/strings.xml` for a documented default language, explicit `values-en` and `values-bn` resources where needed for correct language/plural selection, generated resource accessors, and the pinned-version-compatible `stringResource`/`pluralStringResource` APIs. Choose the default from the existing product/design language contract; do not change the startup language as an incidental refactor. Feature resources stay with their feature, genuinely shared control text stays in core UI, and Sky owns only generic design-system text.

- Remove inline Bangla/English text, manual `if (language)` translations and ad hoc translation maps from production presentation as each flow migrates. Moving visible text to a Kotlin `const val` is not localization.
- Use complete translated messages with positional placeholders and quantity resources. Do not concatenate translated fragments, append fixed English unit labels or construct plurals with conditionals.
- Keep domain/application failures as typed codes and structured arguments. Presentation maps them to resources. Native notifications and exports resolve messages using the selected locale through a resource-aware presentation adapter, without adding resource dependencies to domain.
- Define one language preference policy covering system-following behavior, explicit Bangla/English selection, persistence, fallback and immediate UI refresh. Verify its implementation on both hosts against the pinned Compose version; do not introduce internal/experimental locale APIs without a documented compatibility decision.
- Format dates, times, distances, money, decimal/grouping separators and digit presentation through locale-aware formatters behind shared contracts with platform implementations where needed. Preserve the existing Bangla digit and input-parsing requirements explicitly. Store canonical values independently of localized display text.
- Localize validation feedback, empty/loading/error states, Undo messages, content descriptions, notification text, export labels and permission explanations. Native OS permission prompts remain system-owned; app-supplied manifest/plist usage descriptions require their platform localization mechanisms.
- Test key coverage, placeholder signatures, plural categories, fallback, language switching, formatting, text expansion, large fonts, accessibility and Android/iOS output. Support layout direction when adding an RTL locale; keep identifiers and owner-entered content intact.

Resource files are intentionally the source of translated text; domain policy/configuration and design token files are intentionally the source of fixed values. The rule prohibits unexplained embedded literals in consuming code, not the existence of authoritative data sources.

### Feature file convention

Example: `feature:ledger/fuel`.

- `FuelRoute.kt`: obtains the ViewModel, observes state and connects navigation/native launchers.
- `FuelScreen.kt`: renders state and reports user actions; no persistence or service lookup.
- `FuelUiState.kt`: immutable screen state and explicit loading/saving/error status.
- `FuelAction.kt`: the meaningful actions accepted by the state owner.
- `FuelViewModel.kt`: manages screen state, draft validation feedback and application calls.
- `components/FuelSummary.kt`: cohesive visual sections, extracted where useful.
- `LogFuelUseCase` in application: validates and performs the business workflow.
- Fuel calculations and invariants in domain; storage implementation in data.

Keep keyboard focus, scroll and animation state local to Compose. Keep editable drafts, save progress and workflow state in the ViewModel where lifecycle requirements warrant it. Specify which draft fields survive navigation, rotation and process death. Never persist secrets or document bytes through saved UI state.

## 5. State, concurrency and lifetime

Flow: user action -> ViewModel -> application workflow -> repository/native port -> committed result -> observable domain data -> UI state.

- Expose read-only `StateFlow<UiState>` from shared ViewModels. Observe it according to the actual Compose host lifecycle on both platforms.
- Scope screen ViewModels to their destination or feature flow. Scope the vault coordinator/database to the active vault/session. Give every scope an explicit cancellation owner.
- Keep one authoritative durable writer, initially backed by the existing session implementation. Feature ViewModels never own competing copies of durable vault state.
- Apply commands to the latest authoritative revision inside the coordinator. Reject stale writes or return a typed conflict for refresh. Never silently overwrite from an old screen snapshot.
- Preserve transactional commits, asset ownership and revision-bound Undo. The UI reports success only after durable commit.
- Durable user outcomes are represented in state. Navigation requests and transient effects need defined consumption behavior; do not assume an unbuffered event stream survives backgrounding.
- Propagate cancellation. Map expected failures to typed outcomes such as validation, conflict, storage unavailable or permission denied. Map messages to the active language in presentation.
- Inject clock, identifier source and dispatchers when behavior or tests require control. Put blocking/CPU-heavy work on an appropriate dispatcher.
- Validate business invariants in domain/application even when the UI already validates input.

Use existing Koin only at assembly boundaries if compatibility is confirmed. Prefer explicit factories over adding DI complexity solely for consistency. No Koin imports or service lookup in domain/application; stateless value helpers may remain objects.

## 6. Data and native service ownership

Retain the current Room database and serialized formats during initial moves. Moving a Kotlin class or renaming a serialized property can alter compatibility and must be evaluated explicitly.

- Define focused query contracts for feature needs and a coordinated mutation contract for atomic operations. Do not expose DAO APIs or a generic CRUD repository everywhere.
- Persisted DTOs own field names, defaults, schema versions and compatibility decoding. Domain and UI models evolve independently.
- Preserve document originals, crops and previews separately. Test content/reference integrity, interrupted writes, cleanup, failed restore and recovery.
- Keep serialization compatibility fixtures for existing Room payloads, legacy JSON and backup archives. A source-only refactor must not require users to reset data.
- Backup is an application workflow. Crypto, archive encoding, file I/O and record replacement have distinct contracts and implementations. Restore validates first, then uses a documented recoverable commit protocol across database and filesystem.
- Native auth, secure assets, OCR, location, notification delivery and sharing have narrow ports. Domain policies decide what is allowed; adapters perform native operations.
- Existing reminder reconciliation and sync outbox effects retain their consistency and retry guarantees. Keep local persistence authoritative; remote sync is not evidence that cloud infrastructure is deployed.
- Logs and diagnostics exclude document contents, authentication secrets, backup passwords and personal identifiers.
- Define exact money representation and rounding before changing financial models; preserve legacy values with compatibility tests. Define distance units, clock and timezone semantics explicitly.

## 7. Reusable UI ownership

Three levels:

1. Sky: product-independent buttons, tokens, surfaces, typography and generic motion.
2. Core UI: MotoVault controls, keypads, sheet shell, localized presentation helpers and shared accessibility behavior.
3. Feature components: document preview, fuel summary, service line editor and other domain-specific sections.

Move `Primary`, `Panel`, `Field` and similar primitives out of the root file. Split `Components.kt` by coherent component family. Keep feature-specific document artwork and gauges with their owning feature unless real cross-feature consumers justify promotion.

Preserve `design/app-ui-v3/`, token generation, nine native brand logos, Bangla/English behavior, four tabs, quick-add, sheet geometry, haptics, reduced motion and Undo. New visible states go into the prototype before their native UI implementation.

Line count policy: initially flag handwritten files above 400 lines and functions above 80 lines for responsibility review; require a documented exception for new/expanded files above 600 lines. These are proposed review triggers, not proof of a defect. Exclude generated token/icon/catalogue data appropriately. Ratchet existing debt instead of failing the entire legacy tree immediately.

## 8. One model-independent AI workflow

The shared `docs/roadmap/ARCHITECTURE-CONTRACT.md` and assistant entry points now
exist. P03A-01 tracks completing the detailed engineering documents and workflow
below; P03A-05 tracks executable checks:

```text
docs/engineering/architecture.md
docs/engineering/conventions.md
docs/engineering/verification.md
docs/engineering/ai-workflow.md
docs/engineering/decisions/ADR-*.md
docs/engineering/templates/change-plan.md
docs/engineering/templates/verification-report.md
tools/quality/...
AGENTS.md
CLAUDE.md
GEMINI.md
```

The existing roadmap architecture contract is authoritative during transition.
P03A-01 must reconcile it with the four proposed engineering documents without
creating conflicting sources. Assistant entry files direct their respective tools
to the shared contract and roadmap. Tool-specific skills, if later created, are
thin adapters to this workflow. Do not duplicate policy in each tool's prompt or
rely on tool-private memory for project rules. Local/private skills are optional
conveniences, not prerequisites for completing a task.

### Required workflow stages

| Stage | Required output and gate |
| --- | --- |
| Discover | Read rules; identify task, branch/base, dirty files, actual callers, affected platforms and data contracts. No unrelated edits. |
| Diagnose | List evidence-backed violations; distinguish defects from preferences; classify behavior, data and platform risks. |
| Plan | State acceptance, exact files/modules, dependency changes, preserved contracts, tests, recovery and unknowns. Record structural decisions in an ADR. |
| Track | Use the existing roadmap tool; mark implementation work in progress before feature edits with owner and factual scope. |
| Implement | Make one bounded behavior-preserving change. Add abstractions only for a demonstrated boundary. Extend the prototype first for new UI states. |
| Verify | Run changed-scope quality checks, meaningful regression tests and required platform checks. Preserve full logs outside chat. |
| Review | Check dependency rules, SRP, UI reuse, lifecycle, cancellation, privacy, data compatibility and diff scope. A separate reviewer is optional, never a substitute for evidence. |
| Handoff | Record revision, dirty-tree/diff identity, commands, results, skipped checks, blockers and evidence. Run roadmap check. Leave incomplete validation in review. |

Every model uses the same templates and commands. If a required tool is unavailable, report the missing capability and retain the task's incomplete status; do not silently weaken acceptance. Routine reversible work proceeds within authorized scope; ask only for unresolved product choices, destructive operations or writes that require approval under applicable instructions.

Workflow commands must route shell execution through `rtk`, using its dedicated commands or `rtk run`. Use Podman if containers are needed. Follow the existing writing-voice instructions before producing anything under the owner's name.

## 9. Automated enforcement and evidence

Build a version-compatible quality entry point, with quick, targeted and release profiles. The exact command names are proposed and do not exist yet.

- Module graph checks: forbid cycles and disallowed project dependencies, including accidental production dependencies on test support.
- Kotlin structure checks: catch forbidden imports and qualified references, global service lookup and responsibility thresholds with expiring scoped exceptions. Prefer Kotlin-aware analysis where text rules would be misleading.
- Formatting/static analysis: choose compatible formatter and Kotlin analysis versions, baseline existing debt, then block newly introduced violations. Do not autoformat unrelated files.
- Constants/localization checks: flag unexplained business/UI numeric literals, embedded user-facing strings, manual language branches and repeated protocol keys. Check required translation keys, placeholder compatibility and plural definitions. Scope exceptions to generated data, fixtures and documented authoritative identifiers; do not use a blanket string-literal ban or mass extraction without semantic review.
- Core tests: pure rules, workflow failures/cancellation/conflicts, deterministic dates/IDs, Undo, restore and recovery.
- Storage/native contracts: Room upgrade fixtures, serialized format fixtures, asset consistency, adapter behavior and platform integration tests.
- Presentation tests: state transitions, duplicate-save prevention, failed commits, background/resume, draft restoration and transient-effect handling.
- UI evidence: settled screenshots, important interactions, semantics, large text, Bangla/English, keyboard, sheets, reduced motion and haptics where observable.
- Locale evidence: verify both supported languages, selected/system language behavior, fallback, localized numbers/dates/money, plurals and native notification/export text. Include tests that switching language leaves persisted business values unchanged.
- CI: Android checks on suitable runners; iOS build/tests on macOS with explicit destinations. Fast checks on every change; full regression before removing the legacy implementation and at release gates.

Keep simulator, emulator, physical-device, cloud, signed-release and store-publication evidence separate. For this project use only the authorized iOS 27 iPhone 18 Pro/Pro Max test destinations, verify the running identity, and preserve the existing simulator's data unless a clean-state test is explicit. Current testing restrictions do not establish support for every deployment-target OS.

Each report records task, commit, working diff identity, toolchain, platform/OS/device, commands and exit status, assertions, artifacts, limitations and remaining gates. Do not attribute an uncommitted build to HEAD alone. Sanitize fixtures and reports.

## 10. Migration sequence

| Phase | Concrete work | Exit gate |
| --- | --- | --- |
| A. Baseline and rules | Establish current Android/iOS behavior, compatibility fixtures, module map, ADRs, AI templates, constants/localization inventory and quality baseline | Existing behavior and failures recorded; common workflow reproducible; language/default and literal ownership policies documented |
| B. Dependency foundation | Extract neutral domain models/rules and contracts; wrap the current coordinator behind application ports; maintain persisted DTO compatibility | No inward dependency violations in migrated core; existing data restores unchanged |
| C. Pilot fuel slice | Move fuel rules/workflow and screen into the ledger feature; introduce ViewModel/Route/Screen, named policies and resource-backed text; retain coordinator as writer | Save/edit/failure/conflict, draft lifecycle, derived economy, Undo and Bangla/English formatting verified on both platforms |
| D. Shared UI | Move reusable controls from root/general files into component families; make Sky source reproducible | Existing v3 appearance, semantics, keyboard and motion checks pass |
| E. Remaining features | Move maintenance, garage/onboarding, history/settings, then document/backup flows in bounded changes | Each migrated feature passes its acceptance and compatibility checks before legacy removal |
| F. Native/data isolation | Finish Room/native adapter extraction, composition root, restore protocol and lifecycle ownership | Domain/application have no UI/concrete infrastructure imports; recovery tests and platform contracts pass |
| G. Enforcement and release qualification | Remove proven-unused legacy paths, tighten baselines, run full regression and evaluate physical/release gates | Architecture acceptance passes; any unverified production/release scope remains explicitly open |

Fuel is the pilot because it exercises forms, business rules, persistence, derived state and Undo without first moving original document assets. Do not migrate every feature at once. Move only the native/data adapters required by each slice; phase F completes what remains rather than delaying all boundary work until the end.

The tracker now records the initiative as P03A, with phase-specific application
for P03–P16 and explicit prerequisites for feature implementation and release.
P03 historical completion is unchanged; P03A-04 tracks its separate migration.
Complete P03A before beginning P04–P07. Its fuel pilot preserves existing fuel
and money semantics and stored formats, allowing P03A to finish independently.
P06-01 later defines broader money/distance semantics before those persisted
representations are changed.

Recovery strategy: keep small reversible commits and old format readers; retain asset originals; remove old paths only after equivalent evidence. Any persisted-format migration needs a compatible rollback or recoverable export path. A Git revert alone does not roll back an upgraded database. Do not use a dual-write migration unless reconciliation and failure behavior are explicitly designed and tested.

## 11. Architecture acceptance

- No production module cycles or forbidden dependency edges.
- Domain/application import no presentation or concrete infrastructure types.
- No feature reads or writes Room/native services directly.
- One authoritative mutation coordinator preserves revisions, transactions and Undo across features.
- No source-only move changes existing database, JSON or backup compatibility.
- Every migrated flow has an explicit state/lifetime owner and meaningful failure coverage.
- Existing reusable UI and v3 behavior are retained, with new states covered in the prototype.
- Large-file exceptions identify a coherent responsibility, owner and review trigger.
- Migrated production behavior contains no unexplained magic numbers or embedded user-facing strings. Fixed values have clear owners; translated text uses shared resources with verified placeholders, plurals and locale behavior.
- The repository builds using documented, versioned inputs without an unrecorded sibling checkout dependency.
- A fresh Codex, Claude or Gemini session can locate the same rules, produce the same required artifacts and run the same gates without private memory.
- Required Android/iOS evidence is recorded; missing device/cloud/release proof is never reported as complete.

## 12. Sources and limits

Project authority: `AGENTS.md`, `docs/roadmap/README.md`, `docs/roadmap/DESIGN-CONTRACT.md`, current Gradle configuration and inspected source.

External guidance:

- [Android architecture recommendations](https://developer.android.com/topic/architecture/recommendations): separation of concerns, state holders and UDF.
- [Multiplatform ViewModel](https://kotlinlang.org/docs/multiplatform/compose-viewmodel.html): common ViewModel support; use host-appropriate ownership and factories.
- [Platform-specific APIs](https://kotlinlang.org/docs/multiplatform/multiplatform-connect-to-apis.html): platform boundaries and implementations.
- [Localizing strings](https://kotlinlang.org/docs/multiplatform/compose-localize-strings.html) and [multiplatform resource usage](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-usage.html): shared localized strings, resource accessors, formatting placeholders and plurals.

The stricter domain-port dependency direction and module boundaries here are MotoVault design decisions, not a claim that official guidance requires this exact structure. Current documentation examples may use newer library versions than this checkout. Validate against the pinned Kotlin 2.1.20, Compose 1.7.0 and lifecycle 2.8.3 stack; do not paste newer APIs into the existing project without compatibility evidence.

The proposal and roadmap contract do not establish that target modules, AI
skills, automated gates or production readiness already exist. Their actual
implementation and evidence remain tracked in P03A and the affected phases.
