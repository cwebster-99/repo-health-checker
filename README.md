# GitHub Repository Health Checker

[![CI](https://github.com/Sweekriti91/repo-health-checker/actions/workflows/ci.yml/badge.svg)](https://github.com/Sweekriti91/repo-health-checker/actions/workflows/ci.yml)

A CLI tool that analyzes GitHub repositories and generates a health score report with AI readiness assessment for GitHub Copilot.

## Requirements

- Java 21+
- Maven

## Building

```bash
mvn clean package
```

## Usage

```bash
# Using a personal access token
java -jar target/healthchecker-1.0-SNAPSHOT.jar \
  --repo owner/repo-name \
  --token ghp_yourGitHubToken

# Using the GITHUB_TOKEN environment variable
export GITHUB_TOKEN=ghp_yourGitHubToken
java -jar target/healthchecker-1.0-SNAPSHOT.jar --repo owner/repo-name

# JSON output
java -jar target/healthchecker-1.0-SNAPSHOT.jar \
  --repo owner/repo-name \
  --format json
```

| Flag       | Required | Description                                              |
|------------|----------|----------------------------------------------------------|
| `--repo`   | Yes      | GitHub repository in `owner/name` format                 |
| `--token`  | No       | GitHub API token (defaults to `GITHUB_TOKEN` env var)    |
| `--format` | No       | Output format: `text` (default) or `json`                |

## Example Output

```
=== Repository Health Report ===
  README:          yes
  License:         yes
  License type:    MIT
  CI:              yes (GitHub Actions)
  Description:     yes
  Topics:          yes
  CODEOWNERS:      no
  Security policy: no
  Stars:           ✓ 128 stars
  Open issues:     3 / 12
  Last commit:     2 days ago
  Health score:    87 / 100
  Rating:          Excellent

=== AI Readiness Report ===
  Copilot instructions:   yes
  Custom agents:          no
  Custom skills:          no
  Prompt files:           yes
  .gitignore:             yes
  Folder instructions:    no
  AI readiness score:     3 / 6
```

## Built with GitHub Copilot

This project was built entirely with GitHub Copilot across multiple interaction modes:

| Feature Used | Where | What It Did |
|---|---|---|
| 🏗️ Agent Mode | Project setup, API client, wiring | Created pom.xml, directory structure, custom instructions, CI/CD — all from natural language |
| ⌨️ Inline Completions | Domain models | Tab-completed Java records with 21+ fields |
| 🔌 MCP (GitHub) | API client design | Queried live repos via MCP to inform client design |
| 💬 Chat | Scoring logic, AI readiness checker | Generated business logic from requirements |
| 🧪 Test Generation | Test suite | Generated JUnit 5 tests with Mockito, parameterized tests, edge cases |
| 📈 Coverage Improvement | Test suite | Iteratively increased coverage from ~65% to 90%+ with targeted prompts |
| 🗑️ Deprecation Cleanup | API client refactor | Removed deprecated method + all references, imports, and tests in one pass |
| 🔧 /fix | Bug fix | Found off-by-one boundary bug from failing test |
| 📋 Custom Instructions | All generated code | Enforced SLF4J logging, Optional returns, records, AssertJ — from first commit |
| ⚙️ CI/CD | GitHub Actions | Generated workflow with JaCoCo coverage gate |
| 🤖 Coding Agent (IntelliJ) | Report Formatter | Created full class + tests via agent from inside the IDE |
| 🤖 Coding Agent (Issue) | Javadoc | Assigned GitHub issue, received working PR |
