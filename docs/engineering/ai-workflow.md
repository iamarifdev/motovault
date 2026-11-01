# Shared AI delivery workflow

This workflow applies equally to Codex, Claude, Gemini and a human contributor. Entry files are adapters, not independent sources of policy. Start from `AGENTS.md`, [the architecture contract](../roadmap/ARCHITECTURE-CONTRACT.md), the design contract and the exact tracker task.

1. **Discover:** inspect branch/base, working changes, task dependencies, callers, affected platforms and persisted formats. Read the current implementation and relevant existing evidence. Preserve unrelated changes.
2. **Diagnose:** separate observed facts, unknowns and architectural violations. Identify the owner of state, data, native operations, UI text and fixed policy values.
3. **Plan:** fill the [change plan](templates/change-plan.md); specify files, boundaries, invariants, fixtures, platform checks and risks. Record material decisions in an ADR. Extend v3 prototype states before introducing new UI states.
4. **Track and implement:** mark a roadmap task `in_progress` with an owner and factual note before feature edits. Make a bounded slice; preserve data and route commands through `rtk`.
5. **Verify and review:** use [the verification guide](verification.md), actual available tools and the task's evidence gates. Review dependency direction, SRP, localized text, named values, state lifetime, data integrity and diff scope.
6. **Handoff:** add a sanitized report, attach evidence using the tracker, run its check, and report precise outcomes and missing gates. Do not promote planned or partial work to done.

Assistant-specific skills may call this workflow but may not redefine it. If an integration is missing, use the shared files and repository commands directly. Obtain permission only when applicable instructions or the user's scope require it.
