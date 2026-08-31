<!--
  ~ Copyright 2011-2026 The OTP authors
  ~
  ~ Permission is hereby granted, free of charge, to any person obtaining a copy
  ~ of this software and associated documentation files (the "Software"), to deal
  ~ in the Software without restriction, including without limitation the rights
  ~ to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  ~ copies of the Software, and to permit persons to whom the Software is
  ~ furnished to do so, subject to the following conditions:
  ~
  ~ The above copyright notice and this permission notice shall be included in all
  ~ copies or substantial portions of the Software.
  ~
  ~ THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  ~ IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  ~ FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  ~ AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  ~ LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  ~ OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  ~ SOFTWARE.
  -->

# OTP — Working Notes for Claude

Groovy/Grails project (One Touch Pipeline) for managing NGS sequencing data and
running analysis workflows. This file captures the rules and orientation that matter
for day-to-day coding — kept lean on purpose.

## Architecture

- Grails layering: controllers → services (business logic) → domain (GORM). Keep
  logic in services, not in controllers or domain classes.
- `ngsdata` and `dataprocessing` are the monolith core — nearly everything depends
  on them; changes there are high-blast-radius.
- Flow: a metadata TSV is imported; several preparation steps register it as OTP
  domain data (samples, sequencing tracks, raw files) and hand it off to the workflow
  system, which then aligns the data and runs downstream analysis (variant calling,
  QC, CellRanger). Data can also enter as a directly-imported pre-aligned BAM, which
  skips the alignment step.
- Workflow config is merged from two sources: the imported metadata and project-level
  settings (`ConfigPerProjectAndSeqType`).

## Commands

```
# Build & run
./gradlew bootRun                          # start OTP locally (default port 8080; override via OTP_PORT in .env)
./gradlew test                             # unit tests
./gradlew integrationTest                  # integration tests (real DB + Spring context)
./gradlew workflowTest --tests <Class>     # workflow tests — SLOW (hours in full); ALWAYS scope with --tests
./gradlew check                            # all tests + checks

# Lint
./devScripts/codenarc-changed-files.sh     # Groovy lint, changed files only (fast — run before pushing)
./gradlew codenarcAll                      # full Groovy CodeNarc
./gradlew esLintExport                     # JS/TS lint

# Cypress (OTP must be running)
./gradlew runCypressTestsInWindow          # open the Cypress runner

# Database (dev only)
./gradlew dbmUpdate                         # apply migrations to the dev DB
./devScripts/postgres/recreateDatabase.sh   # fresh dev DB
```

## Code style

- Use explicit types, not `def` — services are `@CompileStatic`, and typed
  declarations catch errors at compile time. Use Groovy idioms otherwise (closures,
  GStrings, collection methods).
- Follow existing patterns in the file you edit — but first question whether the
  pattern fits the new case; existing code may be over-engineered for a simpler one.
- CodeNarc must pass. Spock for all tests.
- Services (`grails-app/services/`, `src/main/`, `src/init/`) get `@CompileStatic`
  applied globally. GORM dynamic finders (`findAllByX`, `findByX`) do not compile
  statically — use explicit criteria/`where` queries or navigate domain
  associations instead. `@CompileDynamic` on services is disallowed by CodeNarc.

## Testing expectations

- All `public`/`protected` methods need unit or integration coverage; new tests are Spock.
- Prefer `Mock` over `Spy`/`GroovyMock`/`GroovySpy`; no `metaclass` mocking.
- Prefer unsaved objects (`new X()`, or `DomainFactory...(..., false)`) over
  persisting fixtures when the DB row isn't the point of the test.
- GUI changes need Cypress coverage (or a documented manual test), checked across
  roles: normal user (PI / department head / deputy / extended / standard privilege),
  operator, admin.
- Cypress: isolate test data via the endpoint-backed DB transaction — `cy.beginPage()`
  (spec-level, auto per spec) and opt-in `cy.beginTest()` (per-test, independent specs
  only) — not manual data cleanup. Needs `otp.testing.endpoints.enabled=true`.
- Workflow changes need the relevant scoped `workflowTest --tests` run — not the
  full suite, except for a final pre-merge pass.
- `test` / `integrationTest` run against an in-memory H2 DB (PostgreSQL mode) — no
  Docker or dev Postgres needed. Docker is only for browser/dev-server verification.

## Commit messages

GitLab enforces this by regex — don't freestyle it.

```
<category>: otp-<issue-number>: <short header>

<optional body>

Release note
<optional: what must happen and when — before stop / while down / after restart>
```

- Category keywords needing an `otp-NNNN` ref: `change`/`chg`/`add`/`new`,
  `bug`/`bugfix`/`fix`, `refactor`, `scripts`, `minor`.
- No ref needed: `codenarc`, `update`.
- Excluded from changelog: `WIP`, `ignore`, `review`.
- Git-generated only: `revert`, `merge`.
- `otp-` must be lowercase.

## Branch naming

House convention (not GitLab-enforced): `otp-<issue-number>-<short-kebab-description>`
e.g. `otp-4821-fix-sample-swap-validation`.

## Migrations

- Groovy changesets only, in `migrations/changelogs/$YEAR/`, registered in
  `migrations/changelog.groovy`.
- Avoid raw SQL files — a known bug resolving the SQL migration directory; embed
  SQL inside a Groovy changeset if unavoidable.
- Stage the new changelog file in the **same commit** as the `include` line that
  registers it — `git status --short migrations/` must show no `??` before committing.
- Verify with `dbmUpdate` before considering it done.

## Hard rules — don't do these unless explicitly asked

- No force-push (`-f`) or history rewrite on a branch someone else may be reviewing.
- No `git push` without an explicit go-ahead for that push.
- No destructive DB operations against anything but the disposable dev DB.
- No touching production config or credentials.
- Don't run the full `workflowTest` suite unprompted — scope it with `--tests`.
- Don't merge or auto-approve a merge request.

## Reference

- Dev login: `otp` / `otp`.
- Key dirs: `grails-app/{controllers,services,domain,views}`, `src/test/groovy/`
  (unit), `src/integration-test/groovy/` (integration), `cypress/e2e/` (E2E),
  `migrations/`.
- Test class mirrors source path:
  `grails-app/services/a/b/FooService.groovy` → `src/test/groovy/a/b/FooServiceSpec.groovy`.
