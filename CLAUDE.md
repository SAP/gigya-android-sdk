# gigya-android-sdk — Claude Context

> This file is gitignored. Do not commit it.

## Project

Open-source Android SDK for SAP CDC / CIAM (Gigya). Maintained by the CDC Mobile team.
Repo: https://github.com/SAP/gigya-android-sdk · Developer: Tal (Android), Sagi (iOS/Swift)

## Modules

| Module | Description | Version |
|---|---|---|
| `sdk-core` | Core SDK — accounts, sessions, WebBridge, FIDO | 7.4.1 |
| `sdk-auth` | OIDC / JWT / push-auth / social login | 2.2.0 |
| `sdk-biometric` | Jetpack BiometricPrompt integration | 2.2.0 |
| `sdk-tfa` | Two-factor auth (email, phone, TOTP) | 2.1.1 |
| `sdk-nss` | Native Screen-Sets (Flutter host) — heading toward sunset | 1.9.12 |
| `sdk-nss-engine` | NSS engine (local Maven repo, not published to Central) | — |
| `example` | Demo app — being rewritten to Compose/MVVM in Phase 3 | — |

SDK deps are largely `compileOnly` — consuming apps supply their own versions.

## Build commands

```bash
# Build all modules
./gradlew build

# Tests (all modules)
./gradlew test

# Single module
./gradlew :sdk-core:build
./gradlew :sdk-core:test

# Lint
./gradlew lint

# Assemble release
./gradlew assembleRelease
```

NSS engine is a local Maven repo at `sdk-nss-engine/host/outputs/repo` — must exist locally to compile NSS references.

## Git workflow

```
develop
  └── maintenance/mode-setup                    ← umbrella (long-lived)
        ├── maintenance-feature/phase-N-*        ← phase branch
        │     └── maintenance-task/pN-*          ← task branch (small, single-concern)
```

- Task branch → PR into phase branch → PR into umbrella → PR into `develop` → `main`
- Conventional Commits: `fix:`, `chore:`, `docs:`, `test:`, `ci:`, `build:`, `feat:`
- Tag convention: `<module>-v<semver>` (e.g. `bio-v2.2.0`)
- `main` only receives merges from `develop` at release time

## Publishing

Semi-manual via Sonatype **Central Portal**: Gradle task → curl upload → manually promote in Portal UI.
Automation is planned in Phase 4. Signing config in local `publish-signing.properties` (gitignored).

## Secrets & local config

| File | Status | Purpose |
|---|---|---|
| `example/.../secrets.xml` | gitignored | API keys, credentials |
| `example/.../gigyaSdkConfiguration.json` | gitignored | SDK site config |
| `example/google-services.json` | gitignored | Firebase config — required for push TFA / push auth |
| `secrets.xml.template` | to be added (Phase 2) | Documents required keys |

`facebook_client_token` is currently committed in `example/.../strings.xml` — cleanup is Phase 2.1.

## Known issues / alerts

- **XSS code-scanning alert** — `sdk-core/.../ui/plugin/GigyaWebBridge.java:544` (severity: error). Addressed in Phase 2.5.
- **No CI/CD** — only issue templates in `.github/`. GitHub Actions planned in Phase 4.
- **No E2E tests** — `sdk-auth` and `sdk-tfa` have zero test coverage. Addressed in Phase 3.

## Current status — Maintenance Mode

The SDK is in **maintenance mode**. No active feature development. Work is tracked in:

- **`docs/MAINTENANCE_MODE_PLAN.md`** — authoritative Android task list + cross-session progress tracker (gitignored)

**At the start of every session:** read `docs/MAINTENANCE_MODE_PLAN.md` §0 (Progress Tracker) and §10 (Session Log) to know where to resume.

### Phase summary

| Phase | Title | Status |
|---|---|---|
| Pre | Biometric 2.2.0 | ☑ done — PR #93, tag `bio-v2.2.0` |
| 0 | Setup & baseline | ◐ in progress — branch hierarchy created; baseline commit pending |
| 1 | Dependency updates | ◐ in progress — task 1.1 done (PR #94 merged); task 1.2 done (PR #95 merged) |
| 2 | Core sanitization | ☐ |
| 2.5 | Security triage | ☐ XSS alert |
| 3 | Example app rewrite + E2E | ☑ done — merged into umbrella 2026-08-17 |
| 3.5 | Open issues & PR triage | ☐ #92, #33, #79, #58 |
| 4 | CI/CD & publish automation | ☐ last |

**Current branch:** `maintenance/mode-setup` (umbrella — phase-3 just merged)

**Next action:** Switch to `maintenance-feature/phase-1-dependencies`, create `maintenance-task/p1-target-sdk-35` for SDK modules targetSdk bump (example app already done). See `docs/MAINTENANCE_MODE_PLAN.md` §11 for task checklist.

## Rules

- No published-API signature changes (maintenance mode = stability)
- No deprecation removals (ledger only — deferred to a future major)
- Jira tickets opened **only with explicit team approval**, never automatically
- E2E tests always use the isolated test ENV, never production
- `sdk-nss` README is out of scope (module heading toward sunset)
