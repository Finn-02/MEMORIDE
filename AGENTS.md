# MEMORIDE development instructions

## Goal and scope
Build a small Android vehicle-game prototype in 14 days with no new paid services.
Concept Baseline 1.2 preserves the frozen 1.1 feature scope. The display name is MEMORIDE; keep io.kailand, content IDs and save keys.
This package is planning and seed material, not a runnable Android app yet.

## Read a small working set
- Read your role in roles/, the relevant task in tasks/backlog.md and docs/03_contracts_data.md.
- UI boundary: contracts/ui_contracts.md. Cross-platform commands: docs/15_cross_platform.md.
- Scope: docs/01_product_scope.md; ownership: docs/02_ownership_architecture.md.
- Rules: docs/04_rules_states.md; scenarios: docs/07_simulation_visit.md; tests: docs/09_quality_tests.md.
- Do not load every document or the whole chat. After A01, compiled core/contract is the executable type source.

## Ownership and connection
- A: app wiring, contracts/theme, journey, Pleos, actual gate and builds.
- B: game state/storage/rewards, island/items, character renderer.
- C: companion mock, virtual visits, world UI and demo runner.
- PM: briefing UI/formatting, content/fixture validation; scope and submission are separate management.
- No feature-to-feature imports. AppRoot passes immutable DTOs, callbacks and the avatar slot.
- Only B writes game progress. Only A translates real SDK inputs and gates app-level commands.
- Choose internal design autonomously. Do not refactor another owner's code incidentally.

## Required behavior
- Actual platform restrictions override DEMO travel. UNKNOWN stays blocked. No fake real parking.
- Companion and visits are parked concept simulations, not stock-navigation overlays or real V2V.
- Preserve explicit DEMO/PLEOS sources. Route calculation is not arrival; Accepted is not reward success.
- Persist rewards and original receipts atomically. Replay preserves the original ID. Reset rejects old generations.
- Missing measurements are null, not zero. No speed/braking scoring, backend, paid API, login or Car Care.
- Rejection, expiry and withdrawal never produce a visit. Visits never grant rewards.
- Do not add a DO declaration, overlay permission or an ADB restriction bypass to unlock a game.

## Autonomy and escalation
- Internal code, UI detail and ordinary bug fixes are local decisions.
- A shared contract change needs A and one affected consumer; keep using the current fake while waiting.
- Ask PM only for core scope change, new cost, weaker restrictions, incompatible saves or delay over a day.
- Use templates/communication.md only when needed. Silence never approves a breaking change.

## Verification and OS
- Inspect git status first. One implementation agent per working tree; preserve unrelated work.
- macOS: python3 scripts/check_all.py. Windows: py -3 scripts/check_all.py.
- After A bootstraps the app, add --android. Use the committed wrapper and recorded JDK, not system Gradle.
- Focused feature tests during iteration, broader checks before PR. No wrapper or unknown toolchain means NOT RUN/blocked.
- Never claim a static check is an Android, SDK, release or driving-safety test.
- UTF-8 and repository .gitattributes. No case-only duplicate paths, personal SDK/JDK paths or secrets.

## Git and cost
- Work on a small part branch, submit a PR, keep main runnable; no shared force push or automatic merge.
- Actions starts disabled. Manual Ubuntu checks only when enabled and spending is controlled; no AI workflows.
- No new API keys, paid dependency, remote build service or repeated full-repo model reviews.
- Do not commit keystores, credentials, signed consent forms, generated caches or private user data.
- End with behavior changes, exact tests/results, NOT RUN items, known limits and a short next step.

Part mapping: part 1=A, part 2=B, part 3=C, PM=P. Assignment is config/team.json.
Read docs/18_final_review.md for navigation, action ownership and asynchronous acknowledgement boundaries.
