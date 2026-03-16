# Copilot Instructions — repo-health-checker

## Project Overview

A Java 21 CLI tool that analyzes GitHub repositories and generates a health score report (0–100+) with an AI readiness assessment for GitHub Copilot (0–6). Built with Picocli, Jackson, and SLF4J.

## Quick Reference

| Action | Command |
|---|---|
| Build | `mvn clean package` (from repo root) |
| Test | `mvn verify` (from repo root) |
| Run | `java -jar target/healthchecker-1.0-SNAPSHOT.jar --repo owner/name --token <token>` |
| CI | GitHub Actions on push/PR to `main` — runs `mvn verify` with Java 21 Temurin |

> **Note:** All Maven commands must run from the repository root.

## Architecture

```
App.java (CLI entry, Picocli @Command)
 ├─ GitHubApiClient      — HTTP calls to GitHub API, rate-limit handling
 ├─ HealthChecker         — Weighted scoring (README, license, CI, stars, issues, commits)
 ├─ AiReadinessChecker    — 6-indicator Copilot readiness check
 └─ ReportFormatter       — Text and JSON output rendering
```

**Package layout:** `com.demo.healthchecker.{checker, client, formatter, model}`

**Data flow:** `App` resolves auth → constructs `GitHubApiClient` → passes it to checkers → checkers return model records → `ReportFormatter` renders output.

## Code Conventions

> See also: [copilot-instructions.md](copilot-instructions.md) for the canonical convention list.

- **Logging:** SLF4J `LoggerFactory.getLogger()` — never `System.out.println`
- **DTOs:** Java records, not POJOs
- **Nulls:** `Optional<T>` over returning `null`
- **Testing:** JUnit 5 + Mockito + **AssertJ `assertThat()`** (not JUnit `assertEquals`)
- **Style:** Google Java Style Guide
- **Errors:** Descriptive exception messages including the failing input value
- **Constants:** Named constants for magic numbers / scoring weights
- **Javadoc:** All public methods must have `@param` and `@return`
- **Deprecation cleanup:** Remove deprecated code along with all associated tests, imports, and references

## Key Patterns

- **Token resolution order:** `--token` flag → `GITHUB_TOKEN` env → `gh auth token` CLI → unauthenticated
- **Scoring weights:** Named `private static final int WEIGHT_*` constants in `HealthChecker`
- **Recursive file search:** `AiReadinessChecker.hasFileWithSuffix()` walks directories for `.prompt.md` / `.instructions.md`
- **Rate-limit detection:** 403 + `X-RateLimit-Remaining: 0` header → descriptive `IOException`
- **GitHub API responses:** Deserialized as `Map<String, Object>` (not typed POJOs) using Jackson `TypeReference`
- **Test injection:** Package-private constructors accept `HttpClient` for mocking

## Testing Expectations

- Mockito for stubbing `GitHubApiClient` in checker tests
- `@CsvSource` parameterized tests for scoring boundaries
- Edge cases: zero issues, null license, missing commit dates, malformed JSON, network errors
- Target 90%+ code coverage (JaCoCo reports generated on `mvn verify`)

## Existing Customizations

- **Agents:** `reviewer-claude.agent.md`, `reviewer-gpt.agent.md` — dual-model code reviewers (not user-invocable)
- **Agents:** `tdd.agent.md` — user-invocable test-driven development specialist (Red-Green-Refactor workflow)
- **Prompts:** `code-review.prompt.md` — dispatches to both reviewer agents for a combined report
