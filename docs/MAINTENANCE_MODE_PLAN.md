# gigya-android-sdk — Maintenance Mode Design Plan & Work Tracker

**Document status:** ACTIVE — authoritative Android work definition + cross-session progress tracker
**Author:** CDC Mobile team (Tal — Android)
**Created:** 2026-07-20 · **Last updated:** 2026-07-22
**Repo:** https://github.com/SAP/gigya-android-sdk

> **This document is the source of truth for the Android maintenance work.** It is the detailed, executable counterpart to the program-wide `MOBILE_SDK_MAINTENANCE_EXECUTIVE_SUMMARY.md` (which covers Android, Swift/iOS, React Native, Flutter, NSS at the executive level). Where the exec summary sizes and sequences the *program*, this document defines the *Android tasks*, tracks their status across sessions, and is what a new session should load first to resume Android work.
>
> **For a new session:** read §0 (Progress Tracker) to see where things stand, then the relevant PHASE in §4 for task detail. Update §0 and §10 (Session Log) as work completes.

---

## 0. Progress Tracker

**Legend:** ☐ not started · ◐ in progress · ☑ done · ⊘ blocked

### Phase status

| Phase | Title | Status | Notes |
|---|---|---|---|
| Pre | Biometric 2.2.0 (predecessor work) | ☑ done | Merged to `develop` + `main` (PR #93), tag `bio-v2.2.0`. Jetpack BiometricPrompt migration, `getDecryptionCipher` null-fix, example status UI + resume-crash fix. |
| 0 | Setup & baseline | ◐ | Branch hierarchy created; baseline commit pending |
| 1 | Dependency updates | ◐ | Task 1.1 done (PR #94 merged); task 1.2 done (PR #95 merged) |
| 2 | Core sanitization | ☐ | Secrets, stale TODOs, docs |
| 2.5 | Security triage | ☐ | XSS alert in GigyaWebBridge.java:544 |
| 3 | Example app rewrite + E2E | ◐ | Scaffold complete (commit `8fff4963`); flow slices next |
| 3.5 | Open issues & PR triage | ☐ | #92, #33, #79, #58 |
| 4 | CI/CD & publish automation | ☐ | Last |

### Blocking dependencies
- **Test ENV** (new dedicated CDC site) — **blocks Phase 3**. Being provisioned by the team.
- **NSS decision** (sunset vs keep) — affects Phase 3 legacy-screens tail + Phase 3.5 issue #33 framing.

### Current session pointer
> **Next action:** Manual test of OTP / TFA / link / settings flows on device. On sign-off: commit `maintenance-task/p3-flow-login`, then begin `p3-flow-register`. See `docs/PHASE3_REWRITE_PLAN.md` §0 for full flow slice status. Phase 1 tasks (1.3+) still pending in parallel.

---

## 1. Context & Goal

The `gigya-android-sdk` is the live, production Android SDK for SAP CDC / CIAM, built ~6 years ago. Engineering investment was concentrated on a next-generation CIAM SDK; that effort is now **halted** and the CIAM product line is in **maintenance mode** — this SDK continues to be supported rather than sunset. A direct consequence is an accumulated backlog (deferred dependency updates, no automated testing, manual release tooling) that this program pays down.

Because there are **currently no releases in the pipeline**, we have a rare, deadline-free window to invest in the foundation before resuming feature/fix delivery.

### Primary goal
Make the project **easy to maintain** — current, clean, tested, and automated — so that future update/fix work becomes low-friction, and so it is no longer dependent on one developer's manual knowledge.

### Explicitly OUT of scope for this initiative (deferred to later, separate tracks)
- **Removing `@Deprecated` members** — deferred. A deprecation ledger is produced here, but no removals happen; removals belong to a future explicitly-planned major.
- **Legacy FIDO service removal** — the old, Google-unsupported FIDO path is documented for a later formal sunset (deprecate → communicate → remove), not removed here.

> Note: **dependency version updates ARE in scope** (Phase 1) — this aligns with the executive summary's Android "Stage A." Only deprecation/legacy-API *removals* are deferred.

### Guiding principles
1. **Sanitize and test before automating** — CI/CD should validate a clean, tested codebase.
2. **No behavior changes to published APIs** — maintenance mode means stability.
3. **Every change validated** by build + tests before merge.
4. **Incremental & reversible** — phase-based branches; any phase can be paused, reviewed, or rolled back.
5. **Vertical slices in the app-rewrite phase** — each example-app flow is rewritten and E2E-tested together as one complete unit.

---

## 2. Current State Summary (baseline)

| Area | State |
|---|---|
| Modules | `sdk-core` (7.4.1), `sdk-auth` (2.2.0), `sdk-biometric` (2.2.0), `sdk-tfa` (2.1.1), `sdk-nss` (1.9.12), `example`, `sdk-nss-engine` |
| CI/CD | **None** — only issue templates in `.github/` |
| Publishing | **Semi-manual** via Sonatype **Central Portal**: run Gradle task → upload via curl → manually promote/publish in the Portal UI |
| Tests | `sdk-core` decent (33 files); `sdk-nss`/`sdk-biometric` minimal; `sdk-auth`/`sdk-tfa` **zero**; no E2E |
| Example app | XML/Views + Fragments; mixed concerns; not structured for automated testing |
| Secrets | `facebook_client_token` committed in example `strings.xml`; `secrets.xml` + `gigyaSdkConfiguration.json` correctly gitignored |
| Git hygiene | 3 local-only branches never pushed; stale 2019-dated TODOs |
| Docs | Root + per-module READMEs (except `sdk-nss`); no CHANGELOG; no migration guide |

---

## 3. Git Workflow Strategy

### 3.1 Branch model

A single long-lived **maintenance umbrella branch** off `develop`, one **phase branch** per phase, and short-lived **task branches** off each phase branch.

```
develop
  └── maintenance/mode-setup                                  ← umbrella (long-lived)
        ├── maintenance-feature/phase-1-dependencies          ← phase branch (Stage A — deps)
        │     ├── maintenance-task/p1-firebase-messaging
        │     ├── maintenance-task/p1-lifecycle-extensions
        │     ├── maintenance-task/p1-target-sdk-35
        │     ├── maintenance-task/p1-appcompat-align
        │     ├── maintenance-task/p1-credential-manager-fido
        │     ├── maintenance-task/p1-kotlin-2x
        │     ├── maintenance-task/p1-pin-dynamic-versions
        │     └── maintenance-task/p1-material-fb-gson-align
        ├── maintenance-feature/phase-2-sanitization          ← phase branch (hygiene + docs)
        │     ├── maintenance-task/p2-secrets-cleanup
        │     ├── maintenance-task/p2-stale-todos
        │     └── maintenance-task/p2-docs-changelog
        ├── maintenance-feature/phase-2.5-security            ← phase branch (security alert)
        │     └── maintenance-task/p25-security-xss
        ├── maintenance-feature/phase-3-app-e2e               ← phase branch (the core of the effort)
        │     ├── maintenance-task/p3-scaffold                 ← Compose + architecture + E2E harness
        │     ├── maintenance-task/p3-flow-login
        │     ├── maintenance-task/p3-flow-register
        │     ├── maintenance-task/p3-flow-session
        │     ├── maintenance-task/p3-flow-biometric
        │     ├── maintenance-task/p3-flow-tfa
        │     ├── maintenance-task/p3-flow-auth
        │     ├── maintenance-task/p3-cross-team-enablement    ← one-command / CI test run for other teams
        │     └── maintenance-task/p3-legacy-screens-tail      ← port/drop demo-only screens
        ├── maintenance-feature/phase-3.5-backlog             ← phase branch (issues/PRs, release-coupled)
        │     ├── maintenance-task/p35-issue-92-proguard
        │     ├── maintenance-task/p35-issue-33-nss-verify
        │     ├── maintenance-task/p35-pr-79-review
        │     └── maintenance-task/p35-pr-58-review
        └── maintenance-feature/phase-4-cicd                  ← phase branch
              ├── maintenance-task/p4-pr-validation
              ├── maintenance-task/p4-e2e-workflow
              ├── maintenance-task/p4-publish-automation
              └── maintenance-task/p4-dependabot
```

### 3.2 Merge flow
1. Task branch → PR into its **phase branch** (small, reviewable units).
2. Completed + reviewed phase branch → PR into **`maintenance/mode-setup`** umbrella.
3. All phases complete + full regression → umbrella → PR into **`develop`**.
4. `develop` → `main` only when the team decides to cut releases (existing flow; respects branch protection + CLA).

### 3.3 Rules
- Task branches stay small and single-concern.
- Every task PR must build clean and pass existing + new tests.
- No phase branch merges to umbrella with a red build.
- Conventional Commits (`fix:`, `chore:`, `docs:`, `test:`, `ci:`, `build:`, `feat:` for the app rewrite).
- Phase branches rebase on umbrella when it advances, to keep history linear.

---

## 4. Phases

> Effort estimates are relative sizing (S/M/L), not calendar commitments.

---

### PHASE 0 — Setup & Baseline
**Branch:** `maintenance/mode-setup` (off `develop`)

| # | Task | Detail | Size |
|---|------|--------|------|
| 0.1 | Create umbrella branch | Branch `maintenance/mode-setup` off latest `develop` | S |
| 0.2 | Capture baseline | Commit `MAINTENANCE_BASELINE.md`: current build state, per-module test counts, module versions | S |
| 0.3 | Verify green start | Full `./gradlew build` + `test` across all modules | S |

**Exit criteria:** Umbrella branch exists; baseline documented; all modules build and existing tests pass.

---

### PHASE 1 — Dependency Updates (Stage A)
**Branch:** `maintenance/phase-1-dependencies` (off umbrella)

In scope, aligned with the executive summary's Android Stage A. Ranked by priority = staleness × impact. Each task: bump → build → validate → smoke-test; **minimum realistic unit ~3 days** once review + regression are counted. A full audit confirms exact target versions against Maven Central before execution.

| # | Task branch | Update | Priority | Status | Est. |
|---|---|---|---|---|---|
| 1.1 | `maintenance-task/p1-firebase-messaging` | Firebase Messaging (sdk-auth, sdk-tfa: 20.3.0 → 25.0.0) | **Critical** | ☐ | ~1 wk |
| 1.2 | `maintenance-task/p1-lifecycle-extensions` | Remove deprecated `lifecycle-extensions` (discontinued) → granular lifecycle artifacts | **Critical** | ☐ | 3–4 d |
| 1.3 | `maintenance-task/p1-target-sdk-35` | compileSdk/targetSdk 34 → 35 (Play requires target 35 for updates) | **Critical** | ☐ | 3–5 d |
| 1.4 | `maintenance-task/p1-appcompat-align` | AppCompat 1.2.0 → 1.7.1 (sdk-auth, sdk-biometric, sdk-tfa) | **High** | ☐ | 3–4 d |
| 1.5 | `maintenance-task/p1-credential-manager-fido` | Credential Manager / FIDO updates (keep current; document legacy-FIDO sunset) | **High** | ☐ | ~1 wk |
| 1.6 | `maintenance-task/p1-kotlin-2x` | Kotlin 1.9 → 2.x (repo-wide) | **High** | ☐ | ~1 wk |
| 1.7 | `maintenance-task/p1-pin-dynamic-versions` | Pin `+` / `latest.release` → reproducible builds (sdk-nss engine, example linesdk) | **High** | ☐ | 3 d |
| 1.8 | `maintenance-task/p1-material-fb-gson-align` | Material, Facebook SDK, gson alignment across modules | Medium | ☐ | 4–5 d |

> **Guardrail:** SDK deps are largely `compileOnly` — consuming apps supply their own versions — so the risk surface is the example app + tests, not published artifacts. Document any minimum-version implications in module READMEs.
>
> **Legacy FIDO note:** The SDK already uses Credential Manager. The old FIDO service path is no longer supported by Google and must be **formally sunsetted in a later stage** (deprecate → communicate → remove) — documented here, not removed.

**Exit criteria:** All in-scope dependencies updated/aligned; dynamic versions pinned; all modules build + existing tests green; min-version notes added to READMEs where relevant.

---

### PHASE 2 — Core Sanitization (lean)
**Branch:** `maintenance/phase-2-sanitization` (off umbrella)

> Hygiene + docs. No deprecation removals (deferred).

#### 2.1 Secret handling
**Task branch:** `maintenance-task/p2-secrets-cleanup` · **Status:** ☐
- Move `facebook_client_token` (and review `google_client_id`) out of committed `example/.../strings.xml` into the gitignored `secrets.xml`.
- Add committed `secrets.xml.template` documenting required keys.
- Update CONTRIBUTING.md with local-setup steps for `secrets.xml` + `gigyaSdkConfiguration.json`.
- **Validation:** example app compiles with a locally-created `secrets.xml`.

#### 2.2 Stale code hygiene
**Task branch:** `maintenance-task/p2-stale-todos` · **Status:** ☐
- **Remove redundant stale TODOs** — TODOs with distant-past dates (e.g. the 2019-dated ones in `TFAProviderResolver.java`) are considered redundant/dead and are **deleted** as part of code sanitization. No Jira tickets for these.
- Review remaining SDK-source TODOs (FidoApiServiceV23Impl, InterruptionResolverFactory, ExternalProvider, NssAction): delete if clearly stale/dead; if a TODO reflects genuinely pending work, flag it for review — **any Jira ticket is opened only with explicit team approval**, never automatically.
- Produce a **deprecation ledger** (`DEPRECATIONS.md`) inventorying all `@Deprecated` members — for the *future* deprecation track. **No removals now.**
- Coordinate with team, then clean up the 3 local-only branches never pushed (archive to remote first if ownership is unclear).

#### 2.3 Documentation baseline
**Task branch:** `maintenance-task/p2-docs-changelog` · **Status:** ☐
- Add root `CHANGELOG.md`, back-filled per module from git history + tags.
- Add a migration-guide section (root README) for the biometric 2.2.0 `Activity` → `FragmentActivity` change + `setAnimationForPrePieDevices` deprecation.
- Document the release tag convention: `<module>-v<semver>` (e.g. `bio-v2.2.0`).

> Note: `sdk-nss` README is intentionally **out of scope** — the module is heading toward sunset and does not warrant maintenance investment.

**Exit criteria:** No committed secrets; redundant stale TODOs removed; deprecation ledger produced; CHANGELOG + migration guide in place; all builds + tests green.

---

### PHASE 2.5 — Security Triage
**Branch:** `maintenance/phase-2.5-security` (off umbrella)

Resolve flagged security defects on the core module **before** the E2E phase — the codebase should be clean before we build tests against it. Baseline snapshot (as of 2026-07-22):

| Type | Count | Items |
|---|---|---|
| Code-scanning alerts | 1 (error) | `java/xss` in `GigyaWebBridge.java:544` |
| Dependabot alerts | 0 | — |

> Re-poll code-scanning + Dependabot at phase start; triage any new alerts into this phase.

#### 2.5.1 XSS code-scanning alert
**Task branch:** `maintenance-task/p25-security-xss` · **Status:** ☐
- **`java/xss` (severity: error)** — Cross-site scripting flagged at `sdk-core/.../ui/plugin/GigyaWebBridge.java:544`.
- Investigate the flagged data flow (untrusted input reaching a WebView/JS sink). Determine real vs. false positive.
- If real: sanitize/escape the sink or constrain the input; add a regression test. If false positive: dismiss with a documented justification in the code-scanning UI.

**Exit criteria:** Security alert resolved or formally dismissed; no open code-scanning/Dependabot alerts on the core module.

---

### PHASE 3 — Example App Rewrite + E2E Test Infrastructure (core of the effort)
**Branch:** `maintenance-feature/phase-3-app-e2e` (off `maintenance/mode-setup` umbrella, cut at commit `cc08b275` on 2026-08-16)

> **Merge lineage:** phase-3 branch → PR into `maintenance/mode-setup` → umbrella into `develop`. Phase 1 changes will not be present in this branch until phase-1 merges into the umbrella; this is safe because Phase 3 touches `example` only while Phase 1 touches SDK module `build.gradle` files — no conflict risk.

**Test ENV:** Using existing test ENV for the rewrite phase. New dedicated ENV (being provisioned) will be used for final E2E validation before merge.

**Blocking pre-requisite:** ~~A **new dedicated CDC test ENV**~~ Using existing test ENV; new ENV will replace it for final validation.

#### 3.0 Design decisions (locked)
| Decision | Choice |
|---|---|
| Example app UI | **Full rewrite in Jetpack Compose** — simplified, built for testability, NOT a client showcase |
| Architecture | **MVVM + StateFlow** — ViewModel per screen, one thin `GigyaRepository` wrapping the SDK |
| Composables | **Stateless** (state in, events out); **every composable has a `@Preview`** |
| E2E driver | **Headless** — tests exercise ViewModel + repository against the live test ENV (fast, stable, backend-sanity focused). UI-layer tests optional tail-task. |
| Scope | **Flow-covered screens first**; demo-only screens (SSO exchange, NSS, screensets) ported/dropped in a tail-task |

#### 3.1 Scaffold (foundation — do first, everything reuses it)
**Task branch:** `maintenance-task/p3-scaffold` · **Status:** ☐
- Set up Compose in the example module (BOM, compiler, theme, Material3).
- Clean-architecture skeleton:
  ```
  data/GigyaRepository.kt        — single thin wrapper over the Gigya SDK
  ui/theme/                      — Compose theme (simple, minimal)
  ui/common/                     — shared stateless composables (buttons, fields, status row)
  ui/<flow>/<Flow>Screen.kt      — stateless composable + @Preview
  ui/<flow>/<Flow>ViewModel.kt   — StateFlow<UiState>, calls repository
  ```
- E2E harness in `sdk-e2e` (or example `androidTest`): Gigya init against test ENV, login/logout helpers, session assertions, credentials from `secrets.xml`/BuildConfig.
- Deliver **one reference slice** (Login, see 3.2) to prove the pattern end-to-end.

#### 3.2 – 3.7 Flow slices
Each slice is **one vertical unit**. A slice task is "done" only when ALL of these are green:
- Compose screen(s) for the flow — stateless, with `@Preview`
- ViewModel exposing `StateFlow<UiState>`
- Repository method(s) wrapping the relevant SDK calls
- **Headless E2E test** driving the flow against the test ENV, passing end-to-end

| # | Task branch | Flow | Status | Notes |
|---|---|---|---|---|
| 3.2 | `maintenance-task/p3-flow-login` | Login | ☐ | Reference slice; establishes scaffolding patterns |
| 3.3 | `maintenance-task/p3-flow-register` | Register → session → getAccount | ☐ | |
| 3.4 | `maintenance-task/p3-flow-session` | Session persistence: login → restart → restored → logout | ☐ | Core backend-sanity flow |
| 3.5 | `maintenance-task/p3-flow-biometric` | Biometric optIn / lock / unlock / optOut | ☐ | Automate `TESTING_PLAN.md`; include the locked-session-on-resume regression |
| 3.6 | `maintenance-task/p3-flow-tfa` | TFA: email / phone / TOTP | ☐ | First-ever `sdk-tfa` coverage |
| 3.7 | `maintenance-task/p3-flow-auth` | Auth: OIDC / JWT context / push-auth | ☐ | First-ever `sdk-auth` coverage |

#### 3.8 Cross-team test enablement
**Task branch:** `maintenance-task/p3-cross-team-enablement` · **Status:** ☐
- Expose the SDK + test project so **other teams (QA, backend, release) can run the E2E suite themselves** — via a one-command entry point and/or a `workflow_dispatch`-triggerable run — **without depending on a specific mobile developer**.
- Document the run steps in the repo README / CONTRIBUTING.

#### 3.9 Legacy screens tail
**Task branch:** `maintenance-task/p3-legacy-screens-tail` · **Status:** ☐
- Port remaining demo-only screens (SSO exchange, NSS, screensets) to Compose *or* formally drop them from the example.
- No XML/Fragment UI left behind (or documented exceptions, e.g. NSS Flutter host).

**Exit criteria:** Example app is Compose, MVVM, preview-per-view; each flow slice has a passing headless E2E test against the test ENV; `sdk-auth` and `sdk-tfa` have their first automated coverage; other teams can run the suite unaided; documented how to run. This suite is the **backend-deployment sanity harness.**

---

### PHASE 3.5 — Open Issues & PR Triage
**Branch:** `maintenance/phase-3.5-backlog` (off umbrella)

Clear the open community issues and external PRs. Placed **before Phase 4** rather than early, because these are **release-coupled** — there is no version planned yet, so there is no urgency to fix/merge them until we approach the CI/CD + publish work. Baseline snapshot (as of 2026-07-22):

| Type | Count | Items |
|---|---|---|
| Open issues | 2 | #92, #33 |
| Open PRs | 2 | #79, #58 |

> Re-poll the repo at phase start; triage any new issues/PRs into the same buckets.

#### 3.5.1 Open issues
**Task branches:** `maintenance-task/p35-issue-92-proguard` (☐), `maintenance-task/p35-issue-33-nss-verify` (☐)

| Issue | Action |
|---|---|
| **#92 — ProGuard rules not exported** | Add `consumerProguardFiles` to the SDK module(s) so the keep rule (`-keep class com.gigya.android.sdk.** { *; }`) is merged into consuming apps' R8 automatically. Verify with a minified example build. Clean, self-contained fix. |
| **#33 — NSS FlutterEngine cache crash** | Likely **already fixed** by commit `d8bc693a fix(nss): prevent FlutterEngine cache race condition on activity restart` (v1.9.12). Verify the fix covers the reported stack trace, respond to the 10-comment thread, and **close with the fixing version** if confirmed. No new code unless a gap is found. |

#### 3.5.2 Open pull requests (external contributions)
**Task branches:** `maintenance-task/p35-pr-79-review` (☐), `maintenance-task/p35-pr-58-review` (☐)

| PR | State | Action |
|---|---|---|
| **#79 — WebView debugging in debug mode** | MERGEABLE, +4/-1 | Aligns Android with iOS (`isInspectable` when logger is in debug). Small, low-risk. Review, validate against current code, merge or request changes. **Merge candidate.** |
| **#58 — `GigyaPluginBaseFragment` (Fragment vs DialogFragment)** | CONFLICTING, +461, from 2023 | Stale, has merge conflicts, non-English description. Requires a **decision**: accept (resolve conflicts, get author sign-off, adapt to current code) or decline with a courteous explanation. **Decision-first, not automatic.** |

> Any Jira ticket arising from triage is opened **only with explicit team approval** (per §5).

**Exit criteria:** Both open issues fixed or closed-with-explanation; both PRs merged or closed with a documented decision; repo open-item count reflects only genuinely-pending work.

---

### PHASE 4 — CI/CD & Publish Automation (last)
**Branch:** `maintenance/phase-4-cicd` (off umbrella)

#### 4.1 PR validation workflow
**Task branch:** `maintenance-task/p4-pr-validation` · **Status:** ☐
- GitHub Actions on PR to `develop`/`main` and pushes to maintenance branches.
- Steps: build all modules (`assembleRelease`), unit tests (`test`), lint (`lint`).
- Required status check; block merge on failure.

#### 4.2 E2E workflow
**Task branch:** `maintenance-task/p4-e2e-workflow` · **Status:** ☐
- Headless emulator via `reactivecircus/android-emulator-runner`.
- Triggers: `workflow_dispatch` (manual — backend-deployment sanity) + on `develop` → `main` PR.
- Test-ENV credentials from GitHub Actions secrets.
- Runs the Phase 3 headless E2E suite.

#### 4.3 Publish automation
**Task branch:** `maintenance-task/p4-publish-automation` · **Status:** ☐
- Automate the currently semi-manual **Central Portal** publish. Today: Gradle task → curl upload → manual promote in Portal UI.
- Follow the migration guide: https://www.endoflineblog.com/migrate-maven-central-publishing-to-central-portal-for-a-gradle-project
- Adopt a Central-Portal-native Gradle publishing plugin so the upload + promotion is a single automated step.
- Trigger: tag push matching `<module>-v*.*.*` → build + sign + publish the tagged module.
- GPG signing key + Portal credentials stored as GitHub Actions secrets — remove reliance on local `publish-signing.properties`.
- Dry-run against a snapshot/staging before enabling on real tags.

#### 4.4 Dependabot
**Task branch:** `maintenance-task/p4-dependabot` · **Status:** ☐
- `dependabot.yml`: weekly Gradle dependency PRs per module, auto-assigned to CDC Mobile, grouped to reduce noise.
- Keeps dependencies current after the Phase 1 baseline; surfaces future drift automatically.

**Exit criteria:** PRs auto-validated; E2E runnable on-demand in CI; a tag push publishes to Central Portal automatically; Dependabot active.

---

## 5. Cross-Cutting Concerns
- **Versioning:** No version bumps to published artifacts unless publicly observable. Dependency updates (Phase 1) are largely `compileOnly`, so they don't force artifact version bumps; the example-app rewrite doesn't affect published artifacts.
- **Backwards compatibility:** No published-API signature changes. Deprecation removals are deferred (ledger only).
- **Test ENV safety:** E2E uses the new isolated test ENV; never point automated tests at production.
- **Secrets discipline:** All credentials via GitHub Actions secrets or gitignored local files.
- **Jira tickets:** Opened **only with explicit team approval** — never automatically as a side effect of any task.
- **Rollback:** Phase-branch model means any phase can be abandoned by not merging its phase branch to the umbrella.

---

## 6. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Dependency bump breaks example app or a consumer's min version | SDK deps are largely `compileOnly`; validate example app per bump; document min versions in READMEs |
| Compose rewrite scope creep | Flow-covered screens first; strict "vertical slice = done" definition; demo screens deferred to tail-task |
| E2E flakiness in CI | Headless (no UI flake), dedicated test ENV, retry policy, establish stability on core flows first |
| Central Portal automation blocks release | Done last (Phase 4); follow known migration guide; dry-run on snapshot; current semi-manual path stays usable until verified |
| Test ENV not ready | **Blocking** for Phase 3 only — Phases 1, 2, 2.5, 4.1 are independent; provision ENV in parallel with earlier phases |
| First-time `sdk-auth`/`sdk-tfa` tests reveal latent bugs | Expected & valuable; log findings, fix within maintenance scope or ticket for the deferred update track |
| Deferred deprecation work forgotten | Ledger keeps the backlog visible for the post-maintenance track |

---

## 7. Resolved Decisions (from review)

1. **Test ENV** — new dedicated test ENV being provisioned by the team. *(Blocks Phase 3; provision during Phases 1–2.)*
2. **Publishing** — already on Central Portal but semi-manual (Gradle + curl + manual promote). Automate in **Phase 4** via the endoflineblog migration guide.
3. **Dependency updates** — **in scope (Phase 1 / Stage A).** Aligned with the executive summary; Firebase / lifecycle-extensions / targetSdk-35 are Critical.
4. **Deprecations & legacy FIDO removal** — **deferred.** Ledger only; removals handled as a separate post-maintenance track.
5. **Example app** — **full Compose rewrite**, MVVM + StateFlow, stateless composables with `@Preview`, headless E2E, flow-covered screens first. Folded into **Phase 3** so UI and tests co-evolve.
6. **Cross-team enablement** — the E2E suite must be runnable by other teams without a mobile developer (Phase 3.8).

---

## 8. Sequencing Summary

```
Phase 0   Setup & baseline              ── umbrella branch, green baseline
   ↓
Phase 1   Dependency updates (Stage A)  ── Firebase, lifecycle, targetSdk 35, Kotlin 2.x, pins   [provision test ENV in parallel]
   ↓
Phase 2   Core sanitization (lean)      ── secrets, hygiene, docs
   ↓
Phase 2.5 Security triage               ── XSS code-scanning alert (clean core before testing)
   ↓
Phase 3   App rewrite + E2E             ── Compose MVVM + headless E2E + cross-team enablement   [needs test ENV]
   ↓
Phase 3.5 Open issues & PR triage       ── #92, #33, #79, #58 (release-coupled, before CI/CD)
   ↓
Phase 4   CI/CD & publish automation    ── PR validation, E2E workflow, Central Portal automation, Dependabot
   ↓
develop   ← umbrella merged when all phases green + full regression
```

---

## 9. Execution Note

Each task in Section 4 maps to a task branch and a tracked unit of work; update the **§0 Progress Tracker** status marks as tasks move. Phase 3 slices are the primary deliverable and should be executed in order (Login scaffold first). This document aligns with `MOBILE_SDK_MAINTENANCE_EXECUTIVE_SUMMARY.md` — if the program-level plan changes, reconcile here.

**Next step:** provision test ENV → begin Phase 0.

---

## 10. Session Log

> Append a dated entry per working session: what was done, what changed, what's next. Newest at the top. This is the cross-session memory — a new session reads §0 + this log to resume.

### 2026-08-17 (session 2) — Phase 3 flow-login screens built
- **AccountScreen + AccountViewModel** implemented and manually tested (get account, get credentials, logout all verified).
- **TFAScreen + TFAViewModel** — TOTP (QR code → verify) and phone (register → code → verify) flows. All resolver state held in ViewModel; no resolver map.
- **LinkAccountScreen + LinkAccountViewModel** — site (email+password) and social link flows.
- **OTPScreen + OTPViewModel** — two-phase phone OTP login (send → pending → verify).
- **SettingsScreen + SettingsViewModel** — SDK re-initialisation (API key / data center / CNAME).
- **IGigyaRepository** extended with TFA resolver methods, link resolver methods, OTP verify, biometric, push registration.
- **GigyaRepository** implements all new methods with correct `suspendCancellableCoroutine` bridges.
- **AppNavGraph** wired: all screens have real ViewModels; TFA and Link screens receive resolver state from LoginViewModel.
- **BUILD SUCCESSFUL** — `./gradlew :example:assembleDebug` passes clean.
- **Pending:** manual testing of OTP, TFA, link, settings flows on device (scheduled for next session).
- **Next:** commit `p3-flow-login` after manual test sign-off, then move to `p3-flow-register`.

### 2026-08-17 — Phase 3 scaffold complete
- **Compose/MVVM scaffold committed** (commit `8fff4963`) on `maintenance-task/p3-scaffold`.
- Full replacement of Fragment/ViewBinding architecture: `ComponentActivity` + `NavHost`, `sealed class Screen` routes, `AppNavGraph`.
- `data/` layer: `IGigyaRepository` interface, `GigyaRepository` with correct bridge patterns (`suspendCancellableCoroutine` one-shot, `callbackFlow` multi-step), `GigyaSdkException`, `V5ExternalSessionMigrator` moved from `repository/`.
- `ui/theme/`, `ui/common/` (InputField, PrimaryButton, StatusRow, SectionTitle, TestTags), `ui/login/` (LoginScreen + LoginViewModel) delivered as reference slice.
- Stub screens for Account, TFA, LinkAccount, OTP, Settings.
- `secrets.xml.template` + `gigyaSdkConfiguration.json.template` committed; `generateSecrets` Gradle task added for CI.
- **Manually tested on device:** app launches, LoginScreen renders, credentials login/register navigate to AccountScreen stub, all nav paths functional. Back press on AccountScreen exits app (correct — no login back stack).
- **Next:** push branch, begin flow slices on separate task branches starting with `p3-flow-login`.

### 2026-08-16 — Task 1.2 lifecycle-extensions removal
- **Audited `lifecycle-extensions` usage** across all modules: only `sdk-core/build.gradle` had an active `compileOnly` declaration; `sdk-nss` had it commented out; example app was not affected.
- **Source audit confirmed zero actual usage** — no `androidx.lifecycle` imports anywhere in `sdk-core` source. Dependency was declared but never consumed.
- **Removed** `compileOnly 'androidx.lifecycle:lifecycle-extensions:2.2.0'` from `sdk-core/build.gradle`. No replacement artifacts needed.
- **Build verified** — `./gradlew :sdk-core:assembleRelease` passes clean. Pre-existing `powermock` test errors are unrelated.
- **Committed** and **PR #95** opened into `maintenance-feature/phase-1-dependencies`.

### 2026-08-04 — Example app push flows + firebase bump continued
- **Push TFA / Push Auth added to example app:** `push_tfa_opt_in` + `push_auth_opt_in` buttons added to `MyAccountFragment`; `GigyaFirebaseMessagingService`, `PushTFAActivity`, `TFAPushReceiver`, `PushAuthActivity`, `AuthPushReceiver` registered in manifest; `POST_NOTIFICATIONS` permission declared + requested at runtime (Android 13+); `registerForRemoteNotifications()` restored to `MainActivity.onStart()`; `firebase-messaging:25.1.1` added as `implementation` to example (was only `compileOnly` in SDK modules); `google-services.json` copied from backup + gitignored; `google-services` plugin applied to example. Flows tested on device — push TFA opt-in confirmed working end to end.
- **`example/README.md` created** — documents supported flows, required local files, and `google-services.json` disclaimer.
- **Task 1.1 still pending:** firebase-messaging bump compiles clean across all modules; tests required before commit.

### 2026-08-02 — Repo setup + Phase 1 start
- **Repo housekeeping:** `CLAUDE.md` created (gitignored); `MAINTENANCE_MODE_PLAN.md` moved from Desktop → `docs/` (gitignored); branch naming convention established: `maintenance/mode-setup` (umbrella), `maintenance-feature/phase-*` (phase), `maintenance-task/p*` (task).
- **Branch hierarchy created:** `maintenance/mode-setup` → `maintenance-feature/phase-1-dependencies` → `maintenance-task/p1-firebase-messaging`.
- **Task 1.1 in progress:** `firebase-messaging` bumped `20.3.0` → `25.1.1` (`sdk-core`, `sdk-auth`, `sdk-tfa`); `google-services` plugin `4.4.0` → `4.5.0`; `localbroadcastmanager:1.1.0` declared explicitly in `sdk-auth` + `sdk-tfa` (was leaking transitively from firebase 20.x). All three modules compile clean. **Tests required before commit.**
- **Next:** write/run tests for task 1.1, then commit and move to task 1.2.

### 2026-07-22 — Planning & predecessor work
- **Biometric 2.2.0 shipped** (predecessor to maintenance mode): Jetpack `BiometricPrompt` migration replacing split v23/v28 impls; `BiometricKey.getDecryptionCipher()` null-return fixed to throw `EncryptionException`; example app given a biometric status label + fixed the locked-session-on-resume crash (`view.post{}` deferral). Merged to `develop` and `main` via **PR #93**, tagged **`bio-v2.2.0`**. `develop` pushed.
- **This plan authored & aligned** to the executive summary. Dependency updates confirmed **in scope** as Phase 1 (Stage A). Phases renumbered: 1=deps, 2=sanitization, 2.5=security, 3=app+E2E, 3.5=issues/PRs, 4=CI/CD.
- **No maintenance-mode phase started yet.** Next: Phase 0.1 (create umbrella branch).

---

## 11. Task Checklists

> Fine-grained progress within each phase. A new session checks here to know exactly where a task stands before picking it up. Update as steps complete.

### Phase 1 — Dependency Updates

#### 1.1 `firebase-messaging` (branch: `maintenance-task/p1-firebase-messaging`) — Jira: CXCDC-43967
- [x] Audit current versions across all modules
- [x] Identify latest stable version (25.1.1)
- [x] Bump `sdk-core`: `25.0.0` → `25.1.1`
- [x] Bump `sdk-auth`: `20.3.0` → `25.1.1` + declare `localbroadcastmanager:1.1.0` explicitly (was leaking transitively)
- [x] Bump `sdk-tfa`: `20.3.0` → `25.1.1` + declare `localbroadcastmanager:1.1.0` explicitly (was leaking transitively)
- [x] Bump `google-services` plugin: `4.4.0` → `4.5.0`
- [x] Verify all three modules compile clean
- [x] Write / run tests to validate no regression
- [x] Commit on `maintenance-task/p1-firebase-messaging`
- [x] PR into `maintenance-feature/phase-1-dependencies`

#### 1.2 `lifecycle-extensions` (branch: `maintenance-task/p1-lifecycle-extensions`) — Jira: CXCDC-43967
- [x] Audit all usages of `lifecycle-extensions` across all modules
- [x] Identify which granular artifacts are needed — none; dependency was unused
- [x] Remove `lifecycle-extensions` from `sdk-core` (only active declaration; `sdk-nss` already commented out)
- [x] Verify `sdk-core` builds clean (`assembleRelease` successful)
- [x] Commit on `maintenance-task/p1-lifecycle-extensions`
- [x] PR into `maintenance-feature/phase-1-dependencies` — PR #95

#### 1.3 `targetSdk 35` (branch: `maintenance-task/p1-target-sdk-35`)
- [ ] Not started

#### 1.4 AppCompat align (branch: `maintenance-task/p1-appcompat-align`)
- [ ] Not started

#### 1.5 Credential Manager / FIDO (branch: `maintenance-task/p1-credential-manager-fido`)
- [ ] Not started

#### 1.6 Kotlin 2.x (branch: `maintenance-task/p1-kotlin-2x`)
- [ ] Not started

#### 1.7 Pin dynamic versions (branch: `maintenance-task/p1-pin-dynamic-versions`)
- [ ] Not started

#### 1.8 Material / Facebook SDK / gson align (branch: `maintenance-task/p1-material-fb-gson-align`)
- [ ] Not started
